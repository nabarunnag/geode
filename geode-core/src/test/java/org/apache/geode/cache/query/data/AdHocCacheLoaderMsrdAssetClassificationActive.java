package org.apache.geode.cache.query.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;


public class AdHocCacheLoaderMsrdAssetClassificationActive implements
    CacheLoader<String, Asset>, Declarable, Serializable {

  private static final long serialVersionUID = 4550027690575408149L;

  public void close() {
    // TODO Auto-generated method stub

  }

  public Asset load(LoaderHelper<String, Asset> helper)
      throws CacheLoaderException {
    // TODO Auto-generated method stub
    System.out.println("NABA:::: loading the loader");
    Region<String, Asset> region = helper.getRegion();
    Asset asset = getAsset();
    region.put("999", asset);
    return asset;
  }

  private Asset getAsset() {
    // TODO Auto-generated method stub
    Asset asset = new Asset();

    asset.setCountry(null);
    asset.setCreatedBy("loader");
    Map<String, String> crossReferenceMap = new HashMap<String, String>();
    crossReferenceMap.put("5", "five");
    crossReferenceMap.put("6", "six");
    asset.setCrossReferences(crossReferenceMap);
    asset.setEffectiveEndDate(LocalDate.now().plusDays(100));
    asset.setEffectiveStartDate(LocalDate.now());
    asset.setId("999");
    asset.setJpmcAssetGroup("Fund");
    asset.setJpmcCategoryType("mutualFund");
    asset.setJpmcSecurityType("unsecured");
    asset.setJpmcSubSecurityType("unsecured");
    asset.setModifiedBy("loader");
    asset.setProductMappings(getProductMappings());
    asset.setStatus("inactive");
    asset.setVersion(1);
    return asset;
  }

  private List<CacheProductMapping> getProductMappings() {

    List<CacheProductMapping> mappingList = new ArrayList<CacheProductMapping>();

    CacheProductMapping mapping1 = new CacheProductMapping();
    mapping1.setLob("USA"); //
    mapping1.setProcess("FRONTENT");
    mappingList.add(mapping1);
    return mappingList;

  }

  public void init(Properties arg0) {
    // TODO Auto-generated method stub

  }

}
