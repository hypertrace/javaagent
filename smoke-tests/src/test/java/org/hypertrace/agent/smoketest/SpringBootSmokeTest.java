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

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.resources.ResourceAttributes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

// @org.junitpioneer.jupiter.SetEnvironmentVariable(
//    key = "SMOKETEST_JAVAAGENT_PATH",
//    value =
//
// "/Users/ploffay/projects/hypertrace/javaagent/javaagent/build/libs/hypertrace-agent-0.3.3-SNAPSHOT-all.jar")
public class SpringBootSmokeTest extends AbstractSmokeTest {

  @Override
  protected String getTargetImage(int jdk) {
    return "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk" + jdk + "-20210209.550405798";
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
  public void get() throws IOException, InterruptedException {
    String url = String.format("http://localhost:%d/greeting", app.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(response.body().string(), "Hi!");
    }
    ArrayList<ExportTraceServiceRequest> traces = new ArrayList<>(waitForTraces());

    Object currentAgentVersion =
        new JarFile(agentPath)
            .getManifest()
            .getMainAttributes()
            .get(Attributes.Name.IMPLEMENTATION_VERSION);

    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(
        ResourceAttributes.SERVICE_NAME.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(0).getKey());
    Assertions.assertEquals(
        ResourceAttributes.CONTAINER_ID.getKey(),
        traces.get(0).getResourceSpans(0).getResource().getAttributes(1).getKey());
    // value is specified in resources/ht-config.yaml
    Assertions.assertEquals(
        "app_under_test",
        traces
            .get(0)
            .getResourceSpans(0)
            .getResource()
            .getAttributes(0)
            .getValue()
            .getStringValue());

    Assertions.assertEquals(1, countSpansByName(traces, "/greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.greeting"));
    Assertions.assertTrue(
        getSpanStream(traces)
                .flatMap(span -> span.getAttributesList().stream())
                .filter(attribute -> attribute.getKey().equals(OTEL_LIBRARY_VERSION_ATTRIBUTE))
                .map(attribute -> attribute.getValue().getStringValue())
                .filter(value -> value.equals(currentAgentVersion))
                .count()
            > 0);
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

    // OTEL BS smoke test app does not have an endpoint that uses content type what we capture
    // enable once we add smoke tests apps to our build.
    //    List<String> responseBodyAttributes =
    //        getSpanStream(traces)
    //            .flatMap(span -> span.getAttributesList().stream())
    //            .filter(
    //                attribute ->
    //                    attribute
    //                        .getKey()
    //                        .contains(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey()))
    //            .map(attribute -> attribute.getValue().getStringValue())
    //            .collect(Collectors.toList());
    //    Assertions.assertEquals(1, responseBodyAttributes.size());
    //    Assertions.assertEquals("Hi!", responseBodyAttributes.get(0));
  }

  @Test
  public void blocking() throws IOException {
    String url = String.format("http://localhost:%d/greeting", app.getMappedPort(8080));
    Request request = new Request.Builder().url(url).addHeader("mockblock", "true").get().build();
    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Assertions.assertEquals(403, response.code());
    Assertions.assertEquals(
        1,
        getSpanStream(traces)
            .flatMap(span -> span.getAttributesList().stream())
            .filter(attribute -> attribute.getKey().equals("hypertrace.mock.filter.result"))
            .count());
  }
}
