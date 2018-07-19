package org.apache.geode.cache.query.data;

import java.io.Serializable;

public class CacheProductMapping implements Serializable {

  private static final long serialVersionUID = -5012087773264972161L;

  private String lob;
  private String process;


  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getProcess() {
    return process;
  }

  public void setProcess(String process) {
    this.process = process;
  }

}
