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
import static org.apache.geode.distributed.ConfigurationProperties.LOG_LEVEL;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.REMOTE_LOCATORS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.internal.cache.wan.parallel.ParallelGatewaySenderQueue;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;

public class WANRollingUpgradeEventProcessingMixedSiteOneCurrentSiteTwo
    extends WANRollingUpgradeDUnitTest2 {

  @Rule
  public transient ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Test
  public void EventProcessingMixedSiteOneCurrentSiteTwo() throws Exception {

    // Start site 1 with old version members
    MemberVM site1Locator1 = clusterStartupRule.startLocatorVM(0, 0, oldVersion, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0"));
    int site1Locator1Port = site1Locator1.getPort();
    MemberVM site1Locator2 = clusterStartupRule.startLocatorVM(1, 0, oldVersion, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator1Port));
    int site1Locator2Port = site1Locator2.getPort();
    MemberVM site1Server1 =
        clusterStartupRule.startServerVM(2, oldVersion,
            s -> s.withConnectionToLocator(site1Locator1Port));
    MemberVM site1Server2 =
        clusterStartupRule
            .startServerVM(3, oldVersion, s -> s.withConnectionToLocator(site1Locator1Port));


    // Start site 2 with current version members
    MemberVM site2Locator =
        clusterStartupRule.startLocatorVM(5, l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "2")
            .withProperty(MCAST_PORT, "0")
            .withProperty(REMOTE_LOCATORS, "localhost[+" + site1Locator1Port + "]")
            .withProperty(DISTRIBUTED_SYSTEM_ID, "2"));
    int site2LocatorPort = site2Locator.getPort();
    MemberVM site2Server1 =
        clusterStartupRule.startServerVM(6, s -> s.withConnectionToLocator(site2LocatorPort));
    MemberVM site2Server2 =
        clusterStartupRule.startServerVM(7, s -> s.withConnectionToLocator(site2LocatorPort));


    // Create region and configure WAN gateways and receivers at site 1
    String regionName = "EventProcessingMixedSiteOneCurrentSiteTwo_region";
    String site1SenderId = "toSite2";
    site1Server1.invoke(() -> {
      createRegionAndConfigureGatwaysAndReceivers(regionName, site1SenderId, 2);
    });

    site1Server2.invoke(() -> {
      createRegionAndConfigureGatwaysAndReceivers(regionName, site1SenderId, 2);
    });

    // Roll site 1 locators to current version
    clusterStartupRule.stop(0);
    site1Locator1 = clusterStartupRule.startLocatorVM(0, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator2Port));
    int site1Locator1PortV2 = site1Locator1.getPort();
    clusterStartupRule.stop(1);
    site1Locator2 = clusterStartupRule.startLocatorVM(1, l -> l
        .withProperty(DISTRIBUTED_SYSTEM_ID, "1")
        .withProperty(MCAST_PORT, "0")
        .withConnectionToLocator(site1Locator1PortV2));

    // Roll one server in site 1 to current version
    clusterStartupRule.stop(2);
    site1Server1 =
        clusterStartupRule
            .startServerVM(2, s -> s.withConnectionToLocator(site1Locator1PortV2));
    site1Server1.invoke(() -> {
      createRegionAndConfigureGatwaysAndReceivers(regionName, site1SenderId, 2);
    });

    // Create region and configure WAN gateways and receivers at site 2
    String site2SenderId = "toSite1";
    site2Server1.invoke(() -> {
      createRegionAndConfigureGatwaysAndReceivers(regionName, site2SenderId, 1);
    });
    site2Server2.invoke(() -> {
      createRegionAndConfigureGatwaysAndReceivers(regionName, site2SenderId, 1);
    });

    // Do puts from mixed version site client and verify events on current site
    ClientVM site1Client =
        clusterStartupRule
            .startClientVM(4, oldVersion, c -> c.withLocatorConnection(site1Locator1PortV2));
    int numPuts = 100;
    site1Client.invoke(() -> {
      createClientRegionAndDoPuts(regionName, numPuts);
    });
    site2Server2.invoke(() -> {
      Region region = ClusterStartupRule.getCache().getRegion(regionName);
      GeodeAwaitility.await()
          .untilAsserted(() -> assertThat(region.keySet().size()).isEqualTo(numPuts));
    });

    // Verify that the events have reached the remote site
    site2Server1.invoke(() -> {
      Region region = ClusterStartupRule.getCache().getRegion(regionName);
      GeodeAwaitility.await()
          .untilAsserted(() -> assertThat(region.keySet().size()).isEqualTo(numPuts));
    });

    // Verify that the cache listeners were triggered
    int numEventsAtRemoteServer1 = site2Server1.invoke(() -> getEventsReceived(regionName));
    int numEventsAtRemoteServer2 = site2Server2.invoke(() -> getEventsReceived(regionName));
    assertThat(numEventsAtRemoteServer1 + numEventsAtRemoteServer2).isEqualTo(numPuts);

    // clear the event listener counters
    site1Server1.invoke(() -> clearEventsReceived(regionName));
    site1Server2.invoke(() -> clearEventsReceived(regionName));
    site2Server1.invoke(() -> clearEventsReceived(regionName));
    site2Server2.invoke(() -> clearEventsReceived(regionName));
  }





}
