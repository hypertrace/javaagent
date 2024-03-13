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

package io.opentelemetry.javaagent.instrumentation.hypertrace.sparkjava;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spark.Spark;

public class SparkJavaInstrumentationTest extends AbstractInstrumenterTest {
  private static final int PORT = 8099;

  private static final String RESPONSE_BODY = "{\"key\": \"val\"}";
  private static final String RESPONSE_HEADER = "responseheader";
  private static final String RESPONSE_HEADER_VALUE = "responseHeaderValue";
  private static final String REQUEST_BODY = "Hi!";
  private static final String REQUEST_HEADER = "requestheader";
  private static final String REQUEST_HEADER_VALUE = "responseHeader";

  @BeforeAll
  public static void postJson() throws Exception {
    AbstractInstrumenterTest.beforeAll();
    Spark.port(PORT);
    Spark.post(
        "/",
        (req, res) -> {
          System.out.printf("Spark received: %s\n", req.body());

          res.header(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
          res.type("application/json");
          return RESPONSE_BODY;
        });
    Spark.get(
        "/exception",
        (req, res) -> {
          res.header(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
          res.type("application/json");
          res.body(RESPONSE_BODY);
          throw new RuntimeException();
        });
    Spark.awaitInitialization();
  }

  @AfterAll
  public static void afterAll() {
    Spark.stop();
  }

  @Test
  public void postRequest() throws IOException, InterruptedException, TimeoutException {
    Request request =
        new Builder()
            .post(RequestBody.create(REQUEST_BODY, MediaType.get("application/json")))
            .url(String.format("http://localhost:%d/", PORT))
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(RESPONSE_BODY, response.body().string());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces = TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    Assertions.assertEquals(1, traces.size());
    List<Span> spans = traces.get(0);
    Assertions.assertEquals(1, spans.size());
    Span span = spans.get(0);
    Assertions.assertEquals(
        REQUEST_BODY, TEST_WRITER.getAttributesMap(span).get("http.request.body").getStringValue());
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER.getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER).getStringValue());

    Assertions.assertEquals(
        RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
    Assertions.assertEquals(
        RESPONSE_HEADER_VALUE,
        TEST_WRITER.getAttributesMap(span)
            .get("http.response.header." + RESPONSE_HEADER).getStringValue());
  }

  @Test
  public void exceptionInHandler() throws IOException, InterruptedException, TimeoutException {
    Request request =
        new Builder()
            .get()
            .url(String.format("http://localhost:%d/exception", PORT))
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {}

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces = TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    Assertions.assertEquals(1, traces.size());
    List<Span> spans = traces.get(0);
    Assertions.assertEquals(1, spans.size());
    Span span = spans.get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER.getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER).getStringValue());

    Assertions.assertEquals(
        "<html><body><h2>500 Internal Error</h2></body></html>",
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
    Assertions.assertEquals(
        RESPONSE_HEADER_VALUE,
        TEST_WRITER.getAttributesMap(span)
            .get("http.response.header." + RESPONSE_HEADER).getStringValue());
  }
}
