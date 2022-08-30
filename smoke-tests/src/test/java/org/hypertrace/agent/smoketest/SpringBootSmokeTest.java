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

package org.hypertrace.agent.smoketest;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

// @org.junitpioneer.jupiter.SetEnvironmentVariable(
//    key = "SMOKETEST_JAVAAGENT_PATH",
//    value =
//
// "/home/ploffay/projects/hypertrace/javaagent/javaagent/build/libs/hypertrace-agent-1.1.1-SNAPSHOT-all.jar")
public class SpringBootSmokeTest extends AbstractSmokeTest {

  private static final int DEFAULT_MAX_PAYLOAD_CAPTURE_SIZE = 128 * 1024;

  @Override
  protected String getTargetImage(int jdk) {
    return "hypertrace/java-agent-test-containers:smoke-springboot-jdk"
        + jdk
        + "-20210706.1005057969";
  }

  private static GenericContainer app;

  @BeforeEach
  synchronized void beforeEach() {
    if (app == null) {
      // TODO test with JDK (11, 14)
      app = createAppUnderTest(8);
      app.start();
    }
  }

  @AfterAll
  static synchronized void afterEach() {
    if (app != null) {
      app.stop();
    }
  }

  @Test
  public void postJson() throws IOException, InterruptedException {
    String url = String.format("http://localhost:%d/echo", app.getMappedPort(8080));
    String requestBody = "request_body";
    Request request =
        new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(response.body().string(), requestBody);
    }
    ArrayList<ExportTraceServiceRequest> traces = new ArrayList<>(waitForTraces());

    Object currentAgentVersion =
        new JarFile(agentPath)
            .getManifest()
            .getMainAttributes()
            .get(OTEL_INSTRUMENTATION_VERSION_MANIFEST_PROP);

    Assertions.assertEquals(
        ResourceAttributes.CONTAINER_ID.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(0).getKey());
    Assertions.assertEquals(
        ResourceAttributes.SERVICE_NAME.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(11).getKey());
    Assertions.assertEquals(
        ResourceAttributes.TELEMETRY_AUTO_VERSION.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(12).getKey());
    Assertions.assertEquals(
        ResourceAttributes.TELEMETRY_SDK_LANGUAGE.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(13).getKey());
    Assertions.assertEquals(
        ResourceAttributes.TELEMETRY_SDK_NAME.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(14).getKey());
    Assertions.assertEquals(
        ResourceAttributes.TELEMETRY_SDK_VERSION.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(15).getKey());
    // value is specified in resources/ht-config.yaml
    Assertions.assertEquals(
        "app_under_test",
        traces
            .get(0)
            .getResourceSpans(0)
            .getResource()
            .getAttributes(11)
            .getValue()
            .getStringValue());

    Assertions.assertEquals(1, countSpansByName(traces, "/echo"));
    Assertions.assertEquals(1, countSpansByName(traces, "WebController.echo"));
    Assertions.assertTrue(
        getInstrumentationLibSpanStream(traces)
            .anyMatch(
                instLibSpan ->
                    instLibSpan
                        .getInstrumentationLibrary()
                        .getVersion()
                        .equals(currentAgentVersion)));
    Assertions.assertTrue(
        getSpanStream(traces)
                .flatMap(span -> span.getAttributesList().stream())
                .filter(attribute -> attribute.getKey().contains("request.header."))
                .map(attribute -> attribute.getValue().getStringValue())
                .count()
            > 0);
    Assertions.assertTrue(
        getSpanStream(traces)
                .flatMap(span -> span.getAttributesList().stream())
                .filter(attribute -> attribute.getKey().contains("response.header."))
                .map(attribute -> attribute.getValue().getStringValue())
                .count()
            > 0);

    List<String> responseBodyAttributes =
        getSpanStream(traces)
            .flatMap(span -> span.getAttributesList().stream())
            .filter(
                attribute ->
                    attribute
                        .getKey()
                        .contains(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey()))
            .map(attribute -> attribute.getValue().getStringValue())
            .collect(Collectors.toList());
    Assertions.assertEquals(1, responseBodyAttributes.size());
    Assertions.assertEquals(requestBody, responseBodyAttributes.get(0));
    List<String> requestBodyAttributes =
        getSpanStream(traces)
            .flatMap(span -> span.getAttributesList().stream())
            .filter(
                attribute ->
                    attribute
                        .getKey()
                        .contains(HypertraceSemanticAttributes.HTTP_REQUEST_BODY.getKey()))
            .map(attribute -> attribute.getValue().getStringValue())
            .collect(Collectors.toList());
    Assertions.assertEquals(1, requestBodyAttributes.size());
    Assertions.assertEquals(requestBody, requestBodyAttributes.get(0));

    ArrayList<ExportMetricsServiceRequest> metrics = new ArrayList<>(waitForMetrics());

    Assertions.assertTrue(hasMetricNamed("otlp.exporter.seen", metrics));
    Assertions.assertTrue(hasMetricNamed("otlp.exporter.exported", metrics));
    Assertions.assertTrue(hasMetricNamed("processedSpans", metrics));
    Assertions.assertTrue(hasMetricNamed("queueSize", metrics));
    Assertions.assertTrue(hasMetricNamed("runtime.jvm.gc.count", metrics));
    Assertions.assertTrue(hasMetricNamed("runtime.jvm.gc.time", metrics));
    Assertions.assertTrue(hasMetricNamed("process.runtime.jvm.memory.usage", metrics));
    Assertions.assertTrue(hasMetricNamed("process.runtime.jvm.memory.init", metrics));
    Assertions.assertTrue(hasMetricNamed("process.runtime.jvm.memory.committed", metrics));

    //    The following metrics are no longer produced by the OTEL Java agent (as of the 1.13.1
    // release)
    //    Assertions.assertTrue(hasMetricNamed("runtime.jvm.memory.pool", metrics));
    //    Assertions.assertTrue(hasMetricNamed("runtime.jvm.memory.area", metrics));
  }

  @Test
  public void postJson_payload_truncation() throws IOException {
    String url = String.format("http://localhost:%d/echo", app.getMappedPort(8080));
    String requestBody = createRequestBody(DEFAULT_MAX_PAYLOAD_CAPTURE_SIZE + 150, 'a');
    Request request =
        new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(response.body().string(), requestBody);
    }

    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    List<String> responseBodyAttributes =
        getSpanStream(traces)
            .flatMap(span -> span.getAttributesList().stream())
            .filter(
                attribute ->
                    attribute
                        .getKey()
                        .contains(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey()))
            .map(attribute -> attribute.getValue().getStringValue())
            .collect(Collectors.toList());
    Assertions.assertEquals(1, responseBodyAttributes.size());
    Assertions.assertEquals(
        DEFAULT_MAX_PAYLOAD_CAPTURE_SIZE, responseBodyAttributes.get(0).length());
    List<String> requestBodyAttributes =
        getSpanStream(traces)
            .flatMap(span -> span.getAttributesList().stream())
            .filter(
                attribute ->
                    attribute
                        .getKey()
                        .contains(HypertraceSemanticAttributes.HTTP_REQUEST_BODY.getKey()))
            .map(attribute -> attribute.getValue().getStringValue())
            .collect(Collectors.toList());
    Assertions.assertEquals(1, requestBodyAttributes.size());
    Assertions.assertEquals(
        DEFAULT_MAX_PAYLOAD_CAPTURE_SIZE, requestBodyAttributes.get(0).length());
  }

  private String createRequestBody(int size, char item) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < size; i++) {
      stringBuilder.append(item);
    }
    return stringBuilder.toString();
  }
}
