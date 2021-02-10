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

import com.google.protobuf.StringValue;
import java.util.Arrays;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

class EnvironmentConfigTest {

  @Test
  @ClearSystemProperty(key = EnvironmentConfig.REPORTING_ENDPOINT)
  @ClearSystemProperty(key = EnvironmentConfig.REPORTING_SECURE)
  @ClearSystemProperty(key = EnvironmentConfig.OPA_ENDPOINT)
  @ClearSystemProperty(key = EnvironmentConfig.OPA_POLL_PERIOD)
  @ClearSystemProperty(key = EnvironmentConfig.OPA_ENABLED)
  @ClearSystemProperty(key = EnvironmentConfig.PROPAGATION_FORMATS)
  @ClearSystemProperty(key = EnvironmentConfig.CAPTURE_HTTP_BODY_PREFIX + "request")
  @ClearSystemProperty(key = EnvironmentConfig.CAPTURE_BODY_MAX_SIZE_BYTES)
  @ClearSystemProperty(key = EnvironmentConfig.JAVAAGENT_FILTER_JAR_PATHS)
  public void systemProperties() {
    // when tests are run in parallel the env vars/sys props set it junit-pioneer are visible to
    // parallel tests
    System.setProperty(EnvironmentConfig.REPORTING_ENDPOINT, "http://:-)");
    System.setProperty(EnvironmentConfig.REPORTING_SECURE, "true");
    System.setProperty(EnvironmentConfig.CAPTURE_HTTP_BODY_PREFIX + "request", "true");
    System.setProperty(EnvironmentConfig.OPA_ENDPOINT, "http://azkaban:9090");
    System.setProperty(EnvironmentConfig.OPA_POLL_PERIOD, "10");
    System.setProperty(EnvironmentConfig.OPA_ENABLED, "true");
    System.setProperty(EnvironmentConfig.PROPAGATION_FORMATS, "B3,TRACECONTEXT");
    System.setProperty(EnvironmentConfig.CAPTURE_BODY_MAX_SIZE_BYTES, "512");
    System.setProperty(EnvironmentConfig.JAVAAGENT_FILTER_JAR_PATHS, "/path1.jar,/path/2/jar.jar");

    AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
    configBuilder.setServiceName(StringValue.newBuilder().setValue("foo"));

    AgentConfig agentConfig = EnvironmentConfig.applyPropertiesAndEnvVars(configBuilder).build();
    Assertions.assertEquals("foo", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.B3, PropagationFormat.TRACECONTEXT),
        agentConfig.getPropagationFormatsList());
    Assertions.assertEquals("http://:-)", agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(
        "http://azkaban:9090", agentConfig.getReporting().getOpa().getEndpoint().getValue());
    Assertions.assertEquals(true, agentConfig.getReporting().getOpa().getEnabled().getValue());
    Assertions.assertEquals(
        10, agentConfig.getReporting().getOpa().getPollPeriodSeconds().getValue());
    Assertions.assertEquals(512, agentConfig.getDataCapture().getBodyMaxSizeBytes().getValue());
    Assertions.assertEquals(true, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpBody().getRequest().getValue());
    Assertions.assertEquals(2, agentConfig.getJavaagent().getFilterJarPathsCount());
    Assertions.assertEquals(
        StringValue.newBuilder().setValue("/path1.jar").build(),
        agentConfig.getJavaagent().getFilterJarPaths(0));
    Assertions.assertEquals(
        StringValue.newBuilder().setValue("/path/2/jar.jar").build(),
        agentConfig.getJavaagent().getFilterJarPaths(1));
  }
}
