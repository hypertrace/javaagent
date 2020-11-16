/*
 * Copyright The Hypertrace Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hypertrace.agent.filter.opa.custom.evaluator;

import java.util.*;
import org.hypertrace.agent.filter.opa.custom.data.BlockingData;
import org.hypertrace.agent.filter.opa.custom.data.IpAddressWithExpiry;

public class IpAddressPolicyEvaluator implements ICustomPolicyEvaluator {

  public static final String X_REAL_IP_KEY = "http.request.header.x-real-ip";
  public static String X_FORWARDED_FOR_KEY = "http.request.header.x-forwarded-for";
  public static String X_PROXYUSER_IP_KEY = "http.request.header.x-proxyuser-ip";
  public static String HTTP_FORWARDED_KEY = "http.request.header.forwarded";
  public static String PROXY_CLIENT_KEY = "proxy.client.addr";

  @Override
  public boolean allow(BlockingData blockingData, Map<String, String> attrMap) {
    long currentTimeInMillis = System.currentTimeMillis();

    if (blockingData == null) {
      return true;
    }

    Set<String> ipAddresses = new HashSet<>();
    extractXRealIps(attrMap, ipAddresses);
    extractXForwardedForIps(attrMap, ipAddresses);
    extractXProxyUserIps(attrMap, ipAddresses);
    extractHttpForwardedIps(attrMap, ipAddresses);
    extractProxyClientIps(attrMap, ipAddresses);

    Set<String> snoozedIpData = getSnoozedIpData(blockingData, currentTimeInMillis);
    Set<String> suspendedIpData = getSuspendedIpData(blockingData, currentTimeInMillis);
    Set<String> deniedIpData = blockingData.getDenylist().getIpAddress();
    Set<String> allowedIpData = blockingData.getAllowlist().getIpAddress();

    Set<String> allowed = new HashSet<>();
    Set<String> violations = new HashSet<>();

    ipAddresses.forEach(
        ipAddr -> {
          if (allowedIpData.contains(ipAddr) || snoozedIpData.contains(ipAddr)) {
            allowed.add(ipAddr);
          }
          if (deniedIpData.contains(ipAddr) || suspendedIpData.contains(ipAddr)) {
            violations.add(ipAddr);
          }
        });

    if (allowed.isEmpty() && !violations.isEmpty()) {
      return false;
    }

    return true;
  }

  void extractXRealIps(Map<String, String> attrMap, Set<String> ipAddresses) {
    String ipAddr = attrMap.get(X_REAL_IP_KEY);
    if (ipAddr != null && !(ipAddr = ipAddr.trim()).isEmpty()) {
      ipAddresses.add(ipAddr);
    }
  }

  void extractXForwardedForIps(Map<String, String> attrMap, Set<String> ipAddresses) {
    String ipAddrStr = attrMap.get(X_FORWARDED_FOR_KEY);
    String ipAddr =
        (ipAddrStr != null && !ipAddrStr.isEmpty()) ? ipAddrStr.split(",")[0].trim() : "";
    if (ipAddr != null && !ipAddr.isEmpty()) {
      ipAddresses.add(ipAddr);
    }
  }

  void extractXProxyUserIps(Map<String, String> attrMap, Set<String> ipAddresses) {
    String ipAddr = attrMap.get(X_PROXYUSER_IP_KEY);
    if (ipAddr != null && !(ipAddr = ipAddr.trim()).isEmpty()) {
      ipAddresses.add(ipAddr);
    }
  }

  void extractHttpForwardedIps(Map<String, String> attrMap, Set<String> ipAddresses) {
    String val = attrMap.get(HTTP_FORWARDED_KEY);
    if (val != null && !val.isEmpty()) {
      String[] forwardedIpHeaderValues = val.split(";");
      for (String headerValue : forwardedIpHeaderValues) {
        String[] keyValuePair = headerValue.split("=");
        if (keyValuePair.length > 1 && keyValuePair[0].equals("for")) {
          String ipAddr = keyValuePair[1].trim();
          if (ipAddr != null && !ipAddr.isEmpty()) {
            ipAddresses.add(ipAddr);
          }
        }
      }
    }
  }

  void extractProxyClientIps(Map<String, String> attrMap, Set<String> ipAddresses) {
    String ipAddr = attrMap.get(PROXY_CLIENT_KEY);
    if (ipAddr != null && !(ipAddr = ipAddr.trim()).isEmpty()) {
      ipAddresses.add(ipAddr);
    }
  }

  Set<String> getSnoozedIpData(BlockingData blockingData, long currentTimeInMillis) {
    Set<String> snoozedIps = new HashSet<>();
    for (IpAddressWithExpiry ipAddr : blockingData.getSnoozedlist()) {
      if (ipAddr.getExpiry() > currentTimeInMillis) {
        snoozedIps.addAll(ipAddr.getIpAddress());
      }
    }
    return snoozedIps;
  }

  Set<String> getSuspendedIpData(BlockingData blockingData, long currentTimeInMillis) {
    Set<String> suspendedIps = new HashSet<>();
    for (IpAddressWithExpiry ipAddr : blockingData.getSuspendedlist()) {
      if (ipAddr.getExpiry() > currentTimeInMillis) {
        suspendedIps.addAll(ipAddr.getIpAddress());
      }
    }
    return suspendedIps;
  }
}
