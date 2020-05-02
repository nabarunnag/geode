
package org.apache.geode.cache.query.dunit;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.data.Portfolio;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.GfshCommandRule;

public class SubregionIndexQueryDUnitTest {
  @Rule
  public ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Rule
  public GfshCommandRule gfshCommandRule = new GfshCommandRule();


  @Test
  public void queryPerformanceOnSubregions() throws Exception {
    MemberVM locator = clusterStartupRule.startLocatorVM(0);
    int locatorPort = locator.getPort();
    MemberVM server = clusterStartupRule.startServerVM(1, serverStarterRule ->
        serverStarterRule.withConnectionToLocator(locatorPort)
            .withProperty("cache-xml-file", "/home/nabarun/Downloads/server.xml"));
    ClientVM
        client =
        clusterStartupRule.startClientVM(2,
            clientCacheRule -> clientCacheRule.withLocatorConnection(locatorPort));

    server.invoke(() ->{
      Region<Integer,Portfolio> a = ClusterStartupRule.getCache().getRegion("/root/loginAccounts/C");
      Region<Integer, Portfolio> m = ClusterStartupRule.getCache().getRegion("/root/intradayMemoTrades/C");
      for (int i = 0; i < 10; i++) {
        a.put(i, new Portfolio(i));
        m.put(i, new Portfolio(i));
      }
    });

    client.invoke(() ->{
      QueryService qs = ClusterStartupRule.getClientCache().getQueryService();
      Query query = qs.newQuery("<trace>SELECT count(DISTINCT m) FROM /root/intradayMemoTrades/C m , /root/loginAccounts/C a WHERE a.login = 'active' AND a.accountBase = m.acctBase");
      SelectResults results = (SelectResults) query.execute();
      results.stream().forEach(result ->{
        System.out.println("NABA: " + result);
      });
//      query =
//          qs.newQuery(
//              "<trace>SELECT count(DISTINCT m) FROM /root/intradayMemoTrades/C m WHERE m.acctBase in (select a.accountBase from /root/loginAccounts/C a where a.login = 'active')");
//      results = (SelectResults) query.execute();
//      results.stream().forEach(result ->{
//        System.out.println("NABA: " + result);
//      });
    });


  }


}
