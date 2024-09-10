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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server;

import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_BODY;
import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_HEADER_NAME;
import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_HEADER_VALUE;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractNetty41ServerInstrumentationTest extends AbstractInstrumenterTest {

  public static final String REQUEST_HEADER_NAME = "reqheader";
  public static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static int port;
  private static NettyTestServer nettyTestServer;

  @BeforeAll
  private void startServer() throws IOException, InterruptedException {
    nettyTestServer = createNetty();
    port = nettyTestServer.create();
  }

  @AfterAll
  private void stopServer() throws ExecutionException, InterruptedException {
    nettyTestServer.stopServer();
  }

  protected abstract NettyTestServer createNetty();

  @Test
  public void get() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get_no_content", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(204, response.code());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
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
        RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.request.body"));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.response.body"));
  }

  @Test
  public void postJson() throws IOException, TimeoutException, InterruptedException {
    RequestBody requestBody = requestBody(true, 3000, 75);
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("Transfer-Encoding", "chunked")
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(RESPONSE_BODY, response.body().string());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
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
        RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + RESPONSE_HEADER_NAME)
            .getStringValue());
    Buffer requestBodyBuffer = new Buffer();
    requestBody.writeTo(requestBodyBuffer);
    Assertions.assertEquals(
        new String(requestBodyBuffer.readByteArray()),
        TEST_WRITER.getAttributesMap(span).get("http.request.body").getStringValue());
    Assertions.assertEquals(
        RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
  }

  @Test
  public void blocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("mockblock", "true")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
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
        TEST_WRITER.getAttributesMap(span).get("http.response.header." + RESPONSE_HEADER_NAME));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.response.body"));

    RequestBody requestBody = blockedRequestBody(true, 3000, 75);
    Request request2 =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request2).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    TEST_WRITER.waitForTraces(2);
    List<List<Span>> traces2 =
        TEST_WRITER.waitForSpans(
            2, span1 -> span1.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    Assertions.assertEquals(2, traces2.size());
    List<Span> trace2 = traces2.get(1);
    Assertions.assertEquals(1, trace2.size());
    Span span2 = trace2.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span2)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(
        TEST_WRITER.getAttributesMap(span2).get("http.response.header." + RESPONSE_HEADER_NAME));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span2).get("http.response.body"));
  }

  @Test
  public void connectionKeepAlive() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("first", "1st")
            .header("connection", "keep-alive")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(RESPONSE_BODY, response.body().string());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
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
        "1st",
        TEST_WRITER.getAttributesMap(span).get("http.request.header.first").getStringValue());
    Assertions.assertEquals(
        "keep-alive",
        TEST_WRITER.getAttributesMap(span).get("http.request.header.connection").getStringValue());
    Assertions.assertEquals(
        RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.request.body"));
    Assertions.assertEquals(
        RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());

    RequestBody requestBody = blockedRequestBody(true, 3000, 75);
    Request request2 =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, "REQUEST_HEADER_VALUE")
            .header("second", "2nd")
            .header("connection", "keep-alive")
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request2).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    TEST_WRITER.waitForTraces(2);
    List<List<Span>> traces2 =
        TEST_WRITER.waitForSpans(
            2, span1 -> span1.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    Assertions.assertEquals(2, traces2.size());
    List<Span> trace2 = traces2.get(1);
    Assertions.assertEquals(1, trace2.size());
    Span span2 = trace2.get(0);

    Assertions.assertEquals(
        "REQUEST_HEADER_VALUE",
        TEST_WRITER
            .getAttributesMap(span2)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        "2nd",
        TEST_WRITER.getAttributesMap(span2).get("http.request.header.second").getStringValue());
    Assertions.assertEquals(
        "keep-alive",
        TEST_WRITER.getAttributesMap(span2).get("http.request.header.connection").getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span2).get("http.request.header.first"));
    Assertions.assertNull(
        TEST_WRITER.getAttributesMap(span2).get("http.response.header." + RESPONSE_HEADER_NAME));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span2).get("http.response.body"));
  }

  @Test
  public void getGzipResponse() throws TimeoutException, InterruptedException, IOException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get_gzip", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    Response response = httpClient.newCall(request).execute();
    Assertions.assertEquals(200, response.code());

    String responseBody = response.body().string();
    Assertions.assertEquals(RESPONSE_BODY, responseBody);

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body"));

    String respBodyCapturedInSpan =
        TEST_WRITER.getAttributesMap(clientSpan).get("http.response.body").getStringValue();
    Assertions.assertEquals(RESPONSE_BODY, respBodyCapturedInSpan);
  }
}
