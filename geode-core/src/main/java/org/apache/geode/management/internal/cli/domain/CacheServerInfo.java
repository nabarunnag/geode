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
package org.apache.geode.management.internal.cli.domain;

import java.io.Serializable;

public class CacheServerInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String bindAddress;
  private int port;
  private boolean isRunning;

  public CacheServerInfo(String bindAddress, int port, boolean isRunning) {
    this.setBindAddress(bindAddress);
    this.setPort(port);
    this.setRunning(isRunning);
  }

  public String getBindAddress() {
    return bindAddress;
  }

  public void setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean isRunning() {
    return isRunning;
  }

  public void setRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }

  public String toString() {
    String sb = "Bind Address :"
        + this.bindAddress
        + '\n'
        + "Port :"
        + this.port
        + '\n'
        + "Running :"
        + this.isRunning;
    return sb;
  }

}
