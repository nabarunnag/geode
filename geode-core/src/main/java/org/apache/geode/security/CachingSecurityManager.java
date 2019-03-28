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
package org.apache.geode.security;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CachingSecurityManager implements SecurityManager {

  SecurityManager securityManager;
  private Cache<Object, ConcurrentHashMap<ResourcePermission, Boolean>> credentialCache = null;

  public CachingSecurityManager(SecurityManager securityManager) {
    Class<SecurityManager> securityManagerAnnotated =
        (Class<SecurityManager>) securityManager.getClass();
    Annotation cacheCredentialsAnnotation =
        securityManagerAnnotated.getAnnotation(CacheAuthorization.class);
    CacheAuthorization cacheAuthorization = (CacheAuthorization) cacheCredentialsAnnotation;
    credentialCache = Caffeine.newBuilder()
        .expireAfterWrite(cacheAuthorization.time(), cacheAuthorization.timeUnit())
        .build();

    this.securityManager = securityManager;
  }

  @Override
  public void init(Properties securityProps) {
    this.securityManager.init(securityProps);
  }

  @Override
  public Object authenticate(Properties credentials) throws AuthenticationFailedException {
    return this.securityManager.authenticate(credentials);
  }

  @Override
  public boolean authorize(Object principal, ResourcePermission permission) {
    return authorizeWithCacheCheck(principal, permission);
  }

  @Override
  public void close() {
    this.securityManager.close();
  }

  private boolean authorizeWithCacheCheck(Object principal, ResourcePermission context) {
    ConcurrentHashMap<ResourcePermission, Boolean> contextMap = new ConcurrentHashMap<>();
    Map credentialsMap = credentialCache.getIfPresent(principal);
    // Not using the original map just in case it gets invalidated while using it.
    try {
      contextMap.putAll(credentialsMap);
    } catch (Exception e) {
      ConcurrentHashMap<ResourcePermission, Boolean> contextAuthorizationMap =
          new ConcurrentHashMap();
      return updateAuthorizationResults(principal, context, contextAuthorizationMap);
    }
    Boolean permission = (Boolean) contextMap.get(context);
    if (permission != null) {
      return permission;
    } else {
      return updateAuthorizationResults(principal, context, contextMap);
    }
  }

  private boolean updateAuthorizationResults(Object principal, ResourcePermission context,
      ConcurrentHashMap<ResourcePermission, Boolean> contextAuthorizationMap) {
    boolean authorizationResult = securityManager.authorize(principal, context);
    contextAuthorizationMap.put(context, authorizationResult);
    credentialCache.put(principal, contextAuthorizationMap);
    return authorizationResult;
  }
}
