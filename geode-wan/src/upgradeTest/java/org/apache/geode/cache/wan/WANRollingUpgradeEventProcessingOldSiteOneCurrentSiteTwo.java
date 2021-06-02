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
import static org.apache.geode.internal.AvailablePortHelper.getRandomAvailableTCPPorts;
import static org.apache.geode.test.awaitility.GeodeAwaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.internal.cache.wan.parallel.ParallelGatewaySenderQueue;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.DistributedTestUtils;
import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.NetworkUtils;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.version.VersionManager;

public class WANRollingUpgradeEventProcessingOldSiteOneCurrentSiteTwo
    extends WANRollingUpgradeDUnitTest2 {

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Test
  public void testEventProcessingOldSiteOneCurrentSiteTwo() throws Exception {

    // Get old site members
    MemberVM
        site1Locator1 =
        clusterStartupRule.startLocatorVM(0, 0, oldVersion,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0"));
    int site1Locator1Port = site1Locator1.getPort();
    MemberVM
        site1Locator2 =
        clusterStartupRule.startLocatorVM(1, 0, oldVersion,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0")
                .withConnectionToLocator(site1Locator1Port));
    int site1Locator2Port = site1Locator2.getPort();
    MemberVM
        site1Server1 =
        clusterStartupRule.startServerVM(2, oldVersion,
            s -> s.withConnectionToLocator(site1Locator1Port, site1Locator2Port));
    MemberVM site1Server2 = clusterStartupRule.startServerVM(3, oldVersion,
        s -> s.withConnectionToLocator(site1Locator1Port, site1Locator2Port));
//    VM site1Client = host.getVM(oldVersion, 3);

    // Get current site members
    MemberVM
        site2Locator1 =
        clusterStartupRule.startLocatorVM(4,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0")
                .withProperty(REMOTE_LOCATORS, "localhost[" + site1Locator1Port +
                    "],localhost[" + site1Locator2Port + "]"));

    int site2Locator1Port = site1Locator1.getPort();
    MemberVM
        site2Locator2 =
        clusterStartupRule.startLocatorVM(5,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0")
                .withConnectionToLocator(site1Locator1Port)
                .withProperty(REMOTE_LOCATORS, "localhost[" + site1Locator1Port +
                    "],localhost[" + site1Locator2Port + "]"));
    int site2Locator2Port = site2Locator2.getPort();
    MemberVM
        site2Server1 =
        clusterStartupRule
            .startServerVM(6, s -> s.withConnectionToLocator(site2Locator1Port, site2Locator2Port));

    MemberVM
        site2Server2 =
        clusterStartupRule.startServerVM(7,
            s -> s.withConnectionToLocator(site2Locator1Port, site2Locator2Port));

//    VM site2Client = host.getVM(VersionManager.CURRENT_VERSION, 7);


    // Start and configure old site servers
    String regionName = "region";

    String site1SenderId = "gatewaysender_to_site2";
    site1Server1.invoke(()->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site1SenderId,2);
    });
    site1Server2.invoke(()->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site1SenderId,2);
    });



    // Start and configure current site servers
    String site2SenderId = "gatewaysender_to_site1";
    site2Server1.invoke(() ->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site2SenderId,1);
    });

    // Do puts from old site client and verify events on current site
    ClientVM
        client =
        clusterStartupRule
            .startClientVM(3, c -> c.withLocatorConnection(site1Locator1Port, site1Locator2Port));
    int numPuts = 100;
    client.invoke(()->{
      createClientRegionAndDoPuts(regionName, numPuts);
    });

    //Verify that the remote site received all events
    site2Server1.invoke(()->{
      Region region = ClusterStartupRule.getCache().getRegion(regionName);
      GeodeAwaitility.await()
          .untilAsserted(() -> assertThat(region.keySet().size()).isEqualTo(numPuts));
    });

    // Verify that the cache listeners were triggered
    int numEventsAtRemoteServer1 = site2Server1.invoke(() -> getEventsReceived(regionName));
    int numEventsAtRemoteServer2 = site2Server2.invoke(() -> getEventsReceived(regionName));
    assertThat(numEventsAtRemoteServer1 + numEventsAtRemoteServer2).isEqualTo(numPuts);

    ClientVM
        clientConnectedToCurrentVersionSite2 =
        clusterStartupRule
            .startClientVM(8, c -> c.withLocatorConnection(site2Locator1Port, site2Locator2Port));
    // Do puts from current site client and verify events on old site
    clientConnectedToCurrentVersionSite2.invoke(()->{

    });
    doClientPutsAndVerifyEvents(site2Client, site2Server1, site2Server2, site1Server1, site1Server2,
        hostName, site2LocatorPort, regionName, numPuts, site2SenderId, false);

    // Do puts from old client in the current site and verify events on old site
    site1Client.invoke(() -> closeCache());
    doClientPutsAndVerifyEvents(site1Client, site2Server1, site2Server2, site1Server1, site1Server2,
        hostName, site2LocatorPort, regionName, numPuts, site2SenderId, false);
  }
}
