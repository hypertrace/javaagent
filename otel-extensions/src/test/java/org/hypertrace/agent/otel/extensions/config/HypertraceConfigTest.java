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

package org.hypertrace.agent.otel.extensions.config;

import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.hypertrace.agent.config.v1.Config.AgentConfig;
import org.hypertrace.agent.config.v1.Config.MetricReporterType;
import org.hypertrace.agent.config.v1.Config.PropagationFormat;
import org.hypertrace.agent.config.v1.Config.TraceReporterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class HypertraceConfigTest {

  @Test
  public void defaultValues() throws IOException {
    URL resource = getClass().getClassLoader().getResource("emptyconfig.yaml");
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    Assertions.assertTrue(agentConfig.getEnabled().getValue());
    Assertions.assertEquals("unknown", agentConfig.getServiceName().getValue());
    Assertions.assertEquals(
        TraceReporterType.OTLP, agentConfig.getReporting().getTraceReporterType());
    Assertions.assertEquals(
        MetricReporterType.METRIC_REPORTER_TYPE_OTLP,
        agentConfig.getReporting().getMetricReporterType());
    Assertions.assertFalse(agentConfig.getReporting().hasCertFile());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_REPORTING_ENDPOINT,
        agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(
        HypertraceConfig.DEFAULT_REPORTING_ENDPOINT,
        agentConfig.getReporting().getMetricEndpoint().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.TRACECONTEXT), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(false, agentConfig.getReporting().getSecure().getValue());
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
    Assertions.assertTrue(agentConfig.hasJavaagent());
    Assertions.assertEquals(0, agentConfig.getJavaagent().getFilterJarPathsCount());
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
    Assertions.assertEquals(false, agentConfig.getEnabled().getValue());
    Assertions.assertEquals(
        Arrays.asList(PropagationFormat.B3), agentConfig.getPropagationFormatsList());
    Assertions.assertEquals(
        TraceReporterType.OTLP, agentConfig.getReporting().getTraceReporterType());
    Assertions.assertEquals(
        MetricReporterType.METRIC_REPORTER_TYPE_OTLP,
        agentConfig.getReporting().getMetricReporterType());
    Assertions.assertEquals(
        "/foo/bar/example.pem", agentConfig.getReporting().getCertFile().getValue());
    Assertions.assertEquals(
        "http://localhost:4317", agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(
        "http://localhost:4317", agentConfig.getReporting().getMetricEndpoint().getValue());
    Assertions.assertEquals(true, agentConfig.getReporting().getSecure().getValue());
    Assertions.assertEquals(16, agentConfig.getDataCapture().getBodyMaxSizeBytes().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue());
    Assertions.assertEquals(
        false, agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getHttpBody().getRequest().getValue());
    Assertions.assertEquals(
        true, agentConfig.getDataCapture().getRpcBody().getRequest().getValue());
    Assertions.assertEquals(2, agentConfig.getJavaagent().getFilterJarPathsCount());
    Assertions.assertEquals(
        StringValue.newBuilder().setValue("/path1.jar").build(),
        agentConfig.getJavaagent().getFilterJarPaths(0));
    Assertions.assertEquals(
        StringValue.newBuilder().setValue("/path/2/jar.jar").build(),
        agentConfig.getJavaagent().getFilterJarPaths(1));
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

  @Test
  @SetEnvironmentVariable(key = "HT_REPORTING_ENDPOINT", value = "http://oltp.hypertrace.org:4317")
  public void complexConfig() throws IOException {
    // GIVEN a config file with a non-default reporting endpoint and an env-var with a different
    // non-default otlp reporting endpoint
    URL resource = getClass().getClassLoader().getResource("config.yaml");
    // WHEN we load the config
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    // VERIFY the trace and metric endpoints are the both the value of the env var
    String expectedEndpoint = "http://oltp.hypertrace.org:4317";
    Assertions.assertEquals(expectedEndpoint, agentConfig.getReporting().getEndpoint().getValue());
    Assertions.assertEquals(
        expectedEndpoint, agentConfig.getReporting().getMetricEndpoint().getValue());
    Assertions.assertEquals(
        TraceReporterType.OTLP, agentConfig.getReporting().getTraceReporterType());
    Assertions.assertEquals(
        MetricReporterType.METRIC_REPORTER_TYPE_OTLP,
        agentConfig.getReporting().getMetricReporterType());
  }

  @Test
  public void zipkinExporter() throws IOException {
    // GIVEN a config file with a non-default zipkin reporting endpoint
    URL resource = getClass().getClassLoader().getResource("zipkinConfig.yaml");
    // WHEN we load the config
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    // VERIFY the trace reporting endpoint is the zipkin endpoint
    Assertions.assertEquals(
        "http://example.com:9411/api/v2/spans",
        agentConfig.getReporting().getEndpoint().getValue());
    // VERIFY the trace reporting type is ZIPKIN
    Assertions.assertEquals(
        TraceReporterType.ZIPKIN, agentConfig.getReporting().getTraceReporterType());
    // VERIFY the metric reporting type is none
    Assertions.assertEquals(
        MetricReporterType.METRIC_REPORTER_TYPE_NONE,
        agentConfig.getReporting().getMetricReporterType());
  }

  @Test
  public void noneTraceExporter() throws IOException {
    // GIVEN a config file with a non-default zipkin reporting endpoint
    URL resource = getClass().getClassLoader().getResource("noneTraceReportingConfig.yaml");
    // WHEN we load the config
    AgentConfig agentConfig = HypertraceConfig.load(resource.getPath());
    // VERIFY the trace reporting type is NONE
    Assertions.assertEquals(
            TraceReporterType.NONE, agentConfig.getReporting().getTraceReporterType());
    // VERIFY the metric reporting type is OTLP
    Assertions.assertEquals(
            MetricReporterType.METRIC_REPORTER_TYPE_OTLP,
            agentConfig.getReporting().getMetricReporterType());
    // VERIFY the metric reporting endpoint is default
    Assertions.assertEquals(
            HypertraceConfig.DEFAULT_REPORTING_ENDPOINT,
            agentConfig.getReporting().getMetricEndpoint().getValue());
  }
}
