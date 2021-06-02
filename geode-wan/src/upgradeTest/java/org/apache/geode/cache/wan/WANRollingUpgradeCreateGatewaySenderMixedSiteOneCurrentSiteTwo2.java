/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.cache.wan;

import static org.apache.geode.distributed.ConfigurationProperties.DISTRIBUTED_SYSTEM_ID;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.REMOTE_LOCATORS;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.serialization.KnownVersion;
import org.apache.geode.management.internal.i18n.CliStrings;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.assertions.CommandResultAssert;
import org.apache.geode.test.junit.categories.WanTest;
import org.apache.geode.test.junit.rules.GfshCommandRule;

@Category(WanTest.class)
public class WANRollingUpgradeCreateGatewaySenderMixedSiteOneCurrentSiteTwo2
    extends WANRollingUpgradeDUnitTest2 {

  @Rule
  public transient ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Rule
  public transient GfshCommandRule gfsh = new GfshCommandRule();

  @Test
  public void CreateGatewaySenderMixedSiteOneCurrentSiteTwo() throws Exception {

    // Site 1 with old version members
    MemberVM site1Locator1 = clusterStartupRule.startLocatorVM(0, 0, oldVersion, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0"));
    int site1Locator1Port = site1Locator1.getPort();
    MemberVM site1Locator2 = clusterStartupRule.startLocatorVM(1, 0, oldVersion, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator1Port));
    int site1Locator2Port = site1Locator2.getPort();
    MemberVM site1Server1 = clusterStartupRule.startServerVM(2, oldVersion,
        s -> s.withConnectionToLocator(site1Locator1Port));
    MemberVM site1Server2 = clusterStartupRule.startServerVM(3, oldVersion,
        s -> s.withConnectionToLocator(site1Locator1Port));


    // Site 2 with current version members
    MemberVM site2Locator1 = clusterStartupRule.startLocatorVM(4, l -> l
        .withProperty(MCAST_PORT, "0")
        .withProperty(DISTRIBUTED_SYSTEM_ID, "2")
        .withProperty(REMOTE_LOCATORS, "localhost[" + site1Locator1Port + "]"));
    int site2Locator1Port = site2Locator1.getPort();
    MemberVM site2Locator2 = clusterStartupRule.startLocatorVM(5, l -> l
        .withProperty(MCAST_PORT, "0")
        .withProperty(DISTRIBUTED_SYSTEM_ID, "2")
        .withProperty(REMOTE_LOCATORS, "localhost[" + site1Locator1Port + "]")
        .withConnectionToLocator(site2Locator1Port));
    int site2Locator2Port = site2Locator1.getPort();
    MemberVM site2Server1 = clusterStartupRule.startServerVM(6,
        s -> s.withConnectionToLocator(site2Locator2Port));
    MemberVM site2Server2 = clusterStartupRule.startServerVM(7,
        s -> s.withConnectionToLocator(site2Locator2Port));


    // Create receivers at the current version site
    site2Server1.invoke(() -> createGatewayReceiver());
    site2Server2.invoke(() -> createGatewayReceiver());



    // Roll old version locators at site 1 to current version
    clusterStartupRule.stop(0);
    site1Locator1 = clusterStartupRule.startLocatorVM(0, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator2Port));
    int site1Locator1PortV2 = site1Locator1.getPort();

    clusterStartupRule.stop(1);
    site1Locator1 = clusterStartupRule.startLocatorVM(1, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator1PortV2));
    // Roll one old version server at site 1 to current version
    clusterStartupRule.stop(2);
    site1Server2 = clusterStartupRule.startServerVM(2,
        s -> s.withConnectionToLocator(site1Locator1PortV2));

    // Use gfsh to attempt to create a gateway sender in the mixed site servers
    gfsh.connectAndVerify(site1Locator1);
    CommandResultAssert cmd = gfsh
        .executeAndAssertThat(getCreateGatewaySenderCommand("toSite2", 2));
    if (!majorMinor(oldVersion).equals(majorMinor(KnownVersion.CURRENT.getName()))) {
      cmd.statusIsError()
          .containsOutput(CliStrings.CREATE_GATEWAYSENDER__MSG__CAN_NOT_CREATE_DIFFERENT_VERSIONS);
    } else {
      // generally serialization version is unchanged between patch releases
      cmd.statusIsSuccess();
    }
  }

  /**
   * returns the major.minor prefix of a semver
   */
  private static String majorMinor(String version) {
    String[] parts = version.split("\\.");
    Assertions.assertThat(parts.length).isGreaterThanOrEqualTo(2);
    return parts[0] + "." + parts[1];
  }
}
