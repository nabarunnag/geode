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
package org.apache.geode.cache;

import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;
import static org.apache.geode.cache.RegionShortcut.REPLICATE;
import static org.apache.geode.cache.client.ClientRegionShortcut.CACHING_PROXY;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.distributed.internal.DSClock;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.categories.ClientServerTest;

@Category({ClientServerTest.class})
public class ClientServerTimeSyncDUnitTest {

  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Ignore("Bug 52327")
  @Test
  public void testClientTimeAdvances() throws Exception {
    MemberVM server = clusterStartupRule.startServerVM(0);
    int serverPort = server.getPort();

    final String regionName = "testRegion";
    final long TEST_OFFSET = 20000;

    server.invoke(() -> {
      Cache cache = ClusterStartupRule.getCache();
      cache.createRegionFactory(REPLICATE).create(regionName);
      // now set an artificial time offset for the test
      ((InternalDistributedSystem) cache.getDistributedSystem()).getClock()
          .setCacheTimeOffset(null, TEST_OFFSET, true);
    });

    ClientVM client =
        clusterStartupRule.startClientVM(1,
            clientCacheRule -> clientCacheRule.withServerConnection(serverPort)
                .withPoolSubscription(true));
    client.invoke(() -> {
      ClientCache clientCache = ClusterStartupRule.getClientCache();
      Region proxyRegion = clientCache.createClientRegionFactory(CACHING_PROXY).create(regionName);
      proxyRegion.registerInterestRegex(".*");
      proxyRegion.put("testkey", "testvalue");

      final DSClock clock = ((GemFireCacheImpl) clientCache).getSystem().getClock();
      GeodeAwaitility.await().until(() -> {
        long clientTimeOffset = clock.getCacheTimeOffset();
        return clientTimeOffset >= TEST_OFFSET;
      });
    });

  }


  @Ignore("not yet implemented")
  @Test
  public void testClientTimeSlowsDown() throws Exception {
    MemberVM server = clusterStartupRule.startServerVM(0);
    int serverPort = server.getPort();

    final String regionName = "testRegion";
    final long TEST_OFFSET = 20000;


    server.invoke(() -> {
      Cache serverCache = ClusterStartupRule.getCache();
      serverCache.createRegionFactory(REPLICATE).create(regionName);
      ((InternalDistributedSystem) serverCache.getDistributedSystem()).getClock()
          .setCacheTimeOffset(null, -TEST_OFFSET, true);
    });

    ClientVM client =
        clusterStartupRule
            .startClientVM(1, c -> c.withServerConnection(serverPort).withPoolSubscription(true));

    client.invoke(() -> {
      ClientCache clientCache = ClusterStartupRule.getClientCache();
      Region proxyRegion = clientCache.createClientRegionFactory(CACHING_PROXY).create(regionName);
      proxyRegion.registerInterestRegex(".*");
      proxyRegion.put("testkey", "testvalue");

      final DSClock clock = ((GemFireCacheImpl) clientCache).getSystem().getClock();
      GeodeAwaitility.await().until(() -> {
        long clientTimeOffset = clock.getCacheTimeOffset();
        System.out.println("NABA::" + clientTimeOffset);
        if (clientTimeOffset >= 0) {
          return false;
        }
        long cacheTime = clock.cacheTimeMillis();
        return abs(currentTimeMillis() - (cacheTime - clientTimeOffset)) < 5;
      });
    });

  }

}
