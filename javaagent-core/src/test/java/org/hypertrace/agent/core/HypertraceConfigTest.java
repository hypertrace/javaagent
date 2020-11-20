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

package org.hypertrace.agent.core;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

public class HypertraceConfigTest {

  @Test
  public void defaultValues() throws IOException {
    URL resource = getClass().getClassLoader().getResource("emptyconfig.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    Assertions.assertEquals("unknown", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_REPORTING_ADDRESS,
        agentConfig.getReporting().getAddress().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.TRACE_CONTEXT), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(false, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_OPA_ADDRESS,
        agentConfig.getReporting().getOpa().getAddress().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_OPA_POLL_PERIOD_SECONDS,
        agentConfig.getReporting().getOpa().getPollPeriod().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpBody().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpBody().getResponse().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcMetadata().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcMetadata().getResponse().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcBody().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcBody().getResponse().getValue());
  }

  @Test
  public void config() throws IOException {
    URL resource = getClass().getClassLoader().getResource("config.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    assertConfig(agentConfig);
  }

  @Test
  public void jsonConfig() throws IOException {
    URL resource = getClass().getClassLoader().getResource("config.jsOn");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    assertConfig(agentConfig);
  }

  private void assertConfig(AgentConfig agentConfig) {
    Assertions.assertEquals("service", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.B3), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(
        "http://localhost:9411", agentConfig.getReporting().getAddress().getValue());
    Assertions.assertEquals(true, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(
        "http://opa.localhost:8181/", agentConfig.getReporting().getOpa().getAddress().getValue());
    Assertions.assertEquals(12, agentConfig.getReporting().getOpa().getPollPeriod().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue());
    Assertions.assertEquals(
        false, agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpBody().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcBody().getRequest().getValue());
  }

  @Test
  @ClearSystemProperty(key = EnvironmentConfig.REPORTING_ADDRESS)
  public void configWithSystemProps() throws IOException {
    System.setProperty(EnvironmentConfig.REPORTING_ADDRESS, "http://nowhere.here");

    URL resource = getClass().getClassLoader().getResource("config.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    Assertions.assertEquals(
        "http://nowhere.here", agentConfig.getReporting().getAddress().getValue());
    Assertions.assertEquals("service", agentConfig.getServiceName().getValue());
  }
}
