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

package org.hypertrace.agent.core.config;

import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.ClearSystemProperty;

public class HypertraceConfigTest {

  @Test
  public void defaultValues() throws IOException {
    URL resource = getClass().getClassLoader().getResource("emptyconfig.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    Assertions.assertEquals("unknown", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_REPORTING_ENDPOINT,
        agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.TRACECONTEXT), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(false, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_OPA_ENDPOINT,
        agentConfig.getReporting().getOpa().getEndpoint().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_OPA_POLL_PERIOD_SECONDS,
        agentConfig.getReporting().getOpa().getPollPeriodSeconds().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_BODY_MAX_SIZE_BYTES,
        agentConfig.getDataCapture().getBodyMaxSizeBytes().getValue());
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
  public void jsonConfig(@TempDir File tempFolder) throws IOException {
    URL resource = getClass().getClassLoader().getResource("config.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());

    String jsonConfig = JsonFormat.printer().print(agentConfig);
    Assertions.assertTrue(!jsonConfig.contains("value"));
    File jsonFile = new File(tempFolder, "config.jSon");
    FileOutputStream fileOutputStream = new FileOutputStream(jsonFile);
    fileOutputStream.write(jsonConfig.getBytes());

    agentConfig = HypertraceConfig.load(jsonFile.getAbsolutePath());
    assertConfig(agentConfig);
  }

  private void assertConfig(AgentConfig agentConfig) {
    Assertions.assertEquals("service", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.B3), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(
        "http://localhost:9411", agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(true, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(
        "http://opa.localhost:8181/", agentConfig.getReporting().getOpa().getEndpoint().getValue());
    Assertions.assertEquals(
        12, agentConfig.getReporting().getOpa().getPollPeriodSeconds().getValue());
    Assertions.assertEquals(16, agentConfig.getDataCapture().getBodyMaxSizeBytes().getValue());
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
  @ClearSystemProperty(key = EnvironmentConfig.REPORTING_ENDPOINT)
  public void configWithSystemProps() throws IOException {
    System.setProperty(EnvironmentConfig.REPORTING_ENDPOINT, "http://nowhere.here");

    URL resource = getClass().getClassLoader().getResource("config.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    Assertions.assertEquals(
        "http://nowhere.here", agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals("service", agentConfig.getServiceName().getValue());
  }
}
