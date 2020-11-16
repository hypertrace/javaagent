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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import org.hypertrace.agent.filter.opa.custom.data.BlockingData;
import org.hypertrace.agent.filter.opa.custom.data.IpAddressWithExpiry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class IpAddressPolicyEvaluatorTest {

  private final String ipAddr = "127.0.1.1";
  private final String ipAddr2 = "127.0.1.2";
  private final IpAddressPolicyEvaluator policyEvaluator = new IpAddressPolicyEvaluator();

  @Test
  public void testExtractXRealIps() {
    BiConsumer<Map, Set> method = (map, set) -> policyEvaluator.extractXRealIps(map, set);
    String key = IpAddressPolicyEvaluator.X_REAL_IP_KEY;
    assertIps(method, "xyz", ipAddr);
    assertIps(method, key, "  ");
    assertIps(method, key, " " + ipAddr + " ", ipAddr);
  }

  @Test
  public void testExtractXForwardedForIps() {
    BiConsumer<Map, Set> method = (map, set) -> policyEvaluator.extractXForwardedForIps(map, set);
    String key = IpAddressPolicyEvaluator.X_FORWARDED_FOR_KEY;
    assertIps(method, "xyz", ipAddr);
    assertIps(method, key, "  ");
    assertIps(method, key, " , 0.1 ");
    assertIps(method, key, " " + ipAddr + " ", ipAddr);
    assertIps(method, key, " " + ipAddr + " , 0.1", ipAddr);
  }

  @Test
  public void testExtractXProxyUserIps() {
    BiConsumer<Map, Set> method = (map, set) -> policyEvaluator.extractXProxyUserIps(map, set);
    String key = IpAddressPolicyEvaluator.X_PROXYUSER_IP_KEY;
    assertIps(method, "xyz", ipAddr);
    assertIps(method, key, "  ");
    assertIps(method, key, " " + ipAddr + " ", ipAddr);
  }

  @Test
  public void testExtractProxyClientIps() {
    BiConsumer<Map, Set> method = (map, set) -> policyEvaluator.extractProxyClientIps(map, set);
    String key = IpAddressPolicyEvaluator.PROXY_CLIENT_KEY;
    assertIps(method, "xyz", ipAddr);
    assertIps(method, key, "  ");
    assertIps(method, key, " " + ipAddr + " ", ipAddr);
  }

  @Test
  public void testExtractHttpForwardedIps() {
    BiConsumer<Map, Set> method = (map, set) -> policyEvaluator.extractHttpForwardedIps(map, set);
    String key = IpAddressPolicyEvaluator.HTTP_FORWARDED_KEY;
    assertIps(method, "xyz", ipAddr);
    assertIps(method, key, "  ");
    assertIps(method, key, " " + ipAddr + " ");
    assertIps(method, key, "f=" + ipAddr + " ");
    assertIps(method, key, "for= " + ipAddr + " ", ipAddr);
    assertIps(method, key, "for= ");
    assertIps(method, key, "for= ; 0.1");
    assertIps(method, key, "for= " + ipAddr + " ;f=" + ipAddr2, ipAddr);
    assertIps(method, key, "for= " + ipAddr + " ;for= " + ipAddr2, ipAddr, ipAddr2);
  }

  @Test
  public void testGetSnoozedSuspendedIpData() {
    IpAddressWithExpiry activeIp = new IpAddressWithExpiry();
    activeIp.setExpiry(System.currentTimeMillis() + 60 * 60 * 1000);
    activeIp.setIpAddress(Collections.singleton(ipAddr));
    IpAddressWithExpiry expiredIp = new IpAddressWithExpiry();
    expiredIp.setExpiry(System.currentTimeMillis() - 60 * 60 * 1000);
    expiredIp.setIpAddress(Collections.singleton(ipAddr2));

    List<IpAddressWithExpiry> ipAddresses =
        new ArrayList<IpAddressWithExpiry>() {
          {
            add(activeIp);
            add(expiredIp);
          }
        };

    {
      BlockingData blockingData = new BlockingData();
      blockingData.setSnoozedlist(ipAddresses);
      Set<String> resultIps =
          policyEvaluator.getSnoozedIpData(blockingData, System.currentTimeMillis());
      Assertions.assertEquals(1, resultIps.size());
      Assertions.assertTrue(ipAddresses.contains(activeIp));
    }
    {
      BlockingData blockingData = new BlockingData();
      blockingData.setSuspendedlist(ipAddresses);
      Set<String> resultIps =
          policyEvaluator.getSuspendedIpData(blockingData, System.currentTimeMillis());
      Assertions.assertEquals(1, resultIps.size());
      Assertions.assertTrue(ipAddresses.contains(activeIp));
    }
  }

  @Test
  public void testAllow() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode =
        objectMapper.readTree(getClass().getClassLoader().getResource("data/data.json"));
    BlockingData blockingData =
        objectMapper.treeToValue(jsonNode.at("/result/traceable/http/request"), BlockingData.class);
    assertAllow(objectMapper, blockingData, null, true);
    assertAllow(
        objectMapper,
        blockingData,
        getClass().getClassLoader().getResource("input/input-allowed-non-empty.json"),
        true);
    assertAllow(
        objectMapper,
        blockingData,
        getClass().getClassLoader().getResource("input/input-allowed-empty-violations-empty.json"),
        true);
    assertAllow(
        objectMapper,
        blockingData,
        getClass()
            .getClassLoader()
            .getResource("input/input-allowed-empty-violations-non-empty.json"),
        false);
  }

  private void assertIps(
      BiConsumer<Map, Set> function, String key, String value, String... resultIps) {
    Set<String> ipAddresses = new HashSet<>();
    Map<String, String> attrMap = new HashMap<>();
    attrMap.put(key, value);
    function.accept(attrMap, ipAddresses);

    Assertions.assertEquals(resultIps.length, ipAddresses.size());
    for (String ip : resultIps) {
      Assertions.assertTrue(ipAddresses.contains(ip));
    }
  }

  private void assertAllow(
      ObjectMapper objectMapper, BlockingData blockingData, URL fileName, boolean allow)
      throws IOException {
    Map<String, String> attributesMap =
        fileName == null ? new HashMap<>() : objectMapper.readValue(fileName, HashMap.class);
    Assertions.assertEquals(policyEvaluator.allow(blockingData, attributesMap), allow);
  }
}
