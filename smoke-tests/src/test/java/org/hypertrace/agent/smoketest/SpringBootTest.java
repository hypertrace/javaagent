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
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

public class SpringBootTest extends AbstractSmokeTest {

  @Override
  protected String getTargetImage(int jdk) {
    return "open-telemetry-docker-dev.bintray.io/java/smoke-springboot-jdk" + jdk + ":latest";
  }

  private static GenericContainer app;

  @BeforeEach
  void beforeEach() {
    if (app == null) {
      // TODO test with JDK (11, 14)
      app = createAppUnderTest(8);
      app.start();
    }
  }

  @AfterAll
  static void afterEach() {
    if (app != null) {
      app.stop();
    }
  }

  @Test
  public void springBootSmokeTest() throws IOException {
    String url = String.format("http://localhost:%d/greeting", app.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Object currentAgentVersion =
        new JarFile(agentPath)
            .getManifest()
            .getMainAttributes()
            .get(Attributes.Name.IMPLEMENTATION_VERSION);

    Assertions.assertEquals(response.body().string(), "Hi!");
    Assertions.assertEquals(1, countSpansByName(traces, "/greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.withspan"));
    Assertions.assertEquals(
        3,
        getSpanStream(traces)
            .flatMap(s -> s.getAttributesList().stream())
            .filter(a -> a.getKey().equals(OTEL_LIBRARY_VERSION_ATTRIBUTE))
            .map(a -> a.getValue().getStringValue())
            .filter(s -> s.equals(currentAgentVersion))
            .count());
    Assertions.assertTrue(
        getSpanStream(traces)
                .flatMap(s -> s.getAttributesList().stream())
                .filter(a -> a.getKey().contains("request.header."))
                .map(a -> a.getValue().getStringValue())
                .count()
            > 0);
    Assertions.assertTrue(
        getSpanStream(traces)
                .flatMap(s -> s.getAttributesList().stream())
                .filter(a -> a.getKey().contains("response.header."))
                .map(a -> a.getValue().getStringValue())
                .count()
            > 0);
    List<String> responseBodyAttributes =
        getSpanStream(traces)
            .flatMap(s -> s.getAttributesList().stream())
            .filter(a -> a.getKey().contains("response.body"))
            .map(a -> a.getValue().getStringValue())
            .collect(Collectors.toList());
    Assertions.assertEquals(1, responseBodyAttributes.size());
    Assertions.assertEquals("Hi!", responseBodyAttributes.get(0));
  }

  @Test
  public void springBootMockBlockingTest() throws IOException {
    String url = String.format("http://localhost:%d/greeting", app.getMappedPort(8080));
    Request request = new Request.Builder().url(url).addHeader("block", "true").get().build();
    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Assertions.assertEquals(response.code(), 403);
    Assertions.assertEquals(
        1,
        getSpanStream(traces)
            .flatMap(s -> s.getAttributesList().stream())
            .filter(a -> a.getKey().equals("hypertrace.opa.result"))
            .count());
  }
}
