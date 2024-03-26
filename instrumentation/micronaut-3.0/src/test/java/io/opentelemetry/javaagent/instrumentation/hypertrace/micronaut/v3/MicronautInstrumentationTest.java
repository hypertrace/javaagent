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

package io.opentelemetry.javaagent.instrumentation.hypertrace.micronaut.v3;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class MicronautInstrumentationTest extends AbstractInstrumenterTest {

  public static final String REQUEST_HEADER_NAME = "reqheader";
  public static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  @Inject EmbeddedServer server;

  @Test
  public void get() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get_no_content", server.getPort()))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(204, response.code());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        TestController.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + TestController.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.request.body"));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.response.body"));
  }

  @Test
  public void postJson() throws IOException, TimeoutException, InterruptedException {
    RequestBody requestBody = requestBody(true, 3000, 75);
    Buffer requestBodyBuffer = new Buffer();
    requestBody.writeTo(requestBodyBuffer);
    String requestBodyStr = new String(requestBodyBuffer.readByteArray());

    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", server.getPort()))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("Transfer-Encoding", "chunked")
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(TestController.RESPONSE_BODY, response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        TestController.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + TestController.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        requestBodyStr,
        TEST_WRITER.getAttributesMap(span).get("http.request.body").getStringValue());
    Assertions.assertEquals(
        TestController.RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
  }

  @Test
  public void stream() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/stream", server.getPort()))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    StringBuilder responseBody = new StringBuilder();
    for (String body : TestController.streamBody()) {
      responseBody.append(body);
    }

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(responseBody.toString(), response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.request.body"));
    Assertions.assertEquals(
        responseBody.toString(),
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
  }

  @Test
  public void blocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", server.getPort()))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("mockblock", "true")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + TestController.RESPONSE_HEADER_NAME));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.response.body"));
  }
}
