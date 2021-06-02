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
import static org.apache.geode.test.awaitility.GeodeAwaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.internal.AvailablePortHelper;
import org.apache.geode.internal.cache.wan.parallel.ParallelGatewaySenderQueue;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.DistributedTestUtils;
import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.NetworkUtils;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;

public class WANRollingUpgradeEventProcessingMixedSiteOneOldSiteTwo
    extends WANRollingUpgradeDUnitTest2 {

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Test
  public void EventProcessingMixedSiteOneOldSiteTwo() throws Exception {
    final Host host = Host.getHost(0);

    //Start site 1 with old version
    MemberVM
        site1Locator1 =
        clusterStartupRule.startLocatorVM(0, 0, oldVersion,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0"));
    MemberVM
        site1Locator2 =
        clusterStartupRule.startLocatorVM(1, 0, oldVersion,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0"));
    int site1Locator1Port = site1Locator1.getPort();
    int site1Locator2Port = site1Locator2.getPort();
    MemberVM
        site1Server1 =
        clusterStartupRule.startServerVM(2, oldVersion,
            s -> s.withConnectionToLocator(site1Locator1Port, site1Locator2Port));
    MemberVM
        site1Server2 =
        clusterStartupRule.startServerVM(3, oldVersion,
            s -> s.withConnectionToLocator(site1Locator1Port, site1Locator2Port));

    // Start site 2 with old version
    MemberVM
        site2Locator1 =
        clusterStartupRule.startLocatorVM(5, 0, oldVersion,
            l -> l.withProperty(DISTRIBUTED_SYSTEM_ID, "2").withProperty(MCAST_PORT, "0")
                .withProperty(REMOTE_LOCATORS,
                    "localhost[" + site1Locator1Port + "],localhost[" + site1Locator2Port + "]"));

    int site2Locator1Port = site2Locator1.getPort();
    MemberVM
        site2Server1 =
        clusterStartupRule.startServerVM(6, oldVersion,
            s -> s.withConnectionToLocator(site2Locator1Port));
    MemberVM
        site2Server2 =
        clusterStartupRule.startServerVM(7, oldVersion,
            s -> s.withConnectionToLocator(site2Locator1Port));

    // Get mixed site locator properties
    // Start and configure mixed site servers
    String regionName = "region";
    String site1SenderId = "gatewaysender_to_site2";
    site1Server1.invoke(() ->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site1SenderId,2);
    });
    site1Server2.invoke(() ->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site1SenderId,2);
    });

    // Roll mixed site locator to current
    clusterStartupRule.stop(0);
    site1Locator1 =
        clusterStartupRule.startLocatorVM(0, l -> l.withConnectionToLocator(site1Locator2Port)
            .withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0"));
    int newSite1Locator1Port = site1Locator1.getPort();
    clusterStartupRule.stop(1);
    site1Locator2 =
        clusterStartupRule.startLocatorVM(1, l -> l.withConnectionToLocator(newSite1Locator1Port)
            .withProperty(DISTRIBUTED_SYSTEM_ID, "1").withProperty(MCAST_PORT, "0"));
    int newSite1Locator2Port = site1Locator2.getPort();

    // Roll one mixed site server to current
    clusterStartupRule.stop(2);
    site1Server1 =
        clusterStartupRule.startServerVM(2,
            s -> s.withConnectionToLocator(newSite1Locator1Port, newSite1Locator2Port));
    site1Server1.invoke(()->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site1SenderId,2);
    });

    // Start and configure old site servers
    String site2SenderId = "gatewaysender_to_site1";
    site2Server1.invoke(()->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site2SenderId,1);
    });
    site2Server2.invoke(()->{
      createRegionAndConfigureGatwaysAndReceivers(regionName,site2SenderId,1);
    });

    // Do puts from mixed site client and verify events on old site
    int numPuts = 100;
    ClientVM
        clientVMConnectedToSite1 =
        clusterStartupRule.startClientVM(4, oldVersion,
            c -> c.withLocatorConnection(newSite1Locator1Port, newSite1Locator2Port));
    clientVMConnectedToSite1.invoke(()->{
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

    // clear the event listener counters
    site1Server1.invoke(() -> clearEventsReceived(regionName));
    site1Server2.invoke(() -> clearEventsReceived(regionName));
    site2Server1.invoke(() -> clearEventsReceived(regionName));
    site2Server2.invoke(() -> clearEventsReceived(regionName));
  }
}
