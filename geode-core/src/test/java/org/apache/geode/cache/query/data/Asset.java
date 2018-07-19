package org.apache.geode.cache.query.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset implements Serializable {

  private static final long serialVersionUID = -6800352145713062375L;

  private String id;
  private String jpmcAssetGroup;
  private String jpmcSecurityType;
  private String jpmcSubSecurityType;
  private String jpmcCategoryType;
  private String country;
  private Integer version;
  private String createdBy;
  private String modifiedBy;
  private LocalDate effectiveStartDate;
  private LocalDate effectiveEndDate;
  private String status;
  private List<CacheProductMapping> productMappings = new ArrayList<>();
  private Map<String, String> crossReferences = new HashMap<>();

  public String getKey() {
    return id;
  }

  public String getId() {
    return id;
  }

  public List<CacheProductMapping> getProductMapping() {
    return productMappings;
  }

  public String getJpmcAssetGroup() {
    return jpmcAssetGroup;
  }

  public String getJpmcSecurityType() {
    return jpmcSecurityType;
  }

  public String getJpmcSubSecurityType() {
    return jpmcSubSecurityType;
  }

  public String getJpmcCategoryType() {
    return jpmcCategoryType;
  }

  public Integer getVersion() {
    return version;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public LocalDate getEffectiveStartDate() {
    return effectiveStartDate;
  }

  public LocalDate getEffectiveEndDate() {
    return effectiveEndDate;
  }

  public String getStatus() {
    return status;
  }

  public Map<String, String> getCrossReferences() {
    return crossReferences;
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  public String getCountry() {
    return country;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setProductMappings(List<CacheProductMapping> productMappings) {
    this.productMappings = productMappings;
  }

  public void setJpmcAssetGroup(String jpmcAssetGroup) {
    this.jpmcAssetGroup = jpmcAssetGroup;
  }

  public void setJpmcSecurityType(String jpmcSecurityType) {
    this.jpmcSecurityType = jpmcSecurityType;
  }

  public void setJpmcSubSecurityType(String jpmcSubSecurityType) {
    this.jpmcSubSecurityType = jpmcSubSecurityType;
  }

  public void setJpmcCategoryType(String jpmcCategoryType) {
    this.jpmcCategoryType = jpmcCategoryType;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public void setEffectiveStartDate(LocalDate effectiveStartDate) {
    this.effectiveStartDate = effectiveStartDate;
  }

  public void setEffectiveEndDate(LocalDate effectiveEndDate) {
    this.effectiveEndDate = effectiveEndDate;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCrossReferences(Map<String, String> crossReferences) {
    this.crossReferences = crossReferences;
  }

  public void setCountry(String country) {
    this.country = country;
  }

}
