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

package io.opentelemetry.javaagent.instrumentation.hypertrace.vertx;

import io.opentelemetry.proto.trace.v1.Span;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.testing.AbstractHttpClientTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class VertxClientInstrumentationTest extends AbstractHttpClientTest {

  private final Vertx vertx = Vertx.vertx(new VertxOptions());
  private final HttpClientOptions clientOptions = new HttpClientOptions();
  private final HttpClient httpClient = vertx.createHttpClient(clientOptions);

  public VertxClientInstrumentationTest() {
    super(true);
  }

  @Override
  public Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      request = request.putHeader(entry.getKey(), entry.getValue());
    }
    request = request.putHeader("Content-Type", contentType);
    BufferHandler bufferHandler = new BufferHandler(countDownLatch);
    ResponseHandler responseHandler = new ResponseHandler(bufferHandler);

    request.handler(responseHandler).end(body);

    countDownLatch.await();
    return new Response(bufferHandler.responseBody, responseHandler.responseStatus);
  }

  @Override
  public Response doGetRequest(String uri, Map<String, String> headers)
      throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    HttpClientRequest request = httpClient.requestAbs(HttpMethod.GET, uri);

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      request = request.putHeader(entry.getKey(), entry.getValue());
    }
    BufferHandler bufferHandler = new BufferHandler(countDownLatch);
    ResponseHandler responseHandler = new ResponseHandler(bufferHandler);

    request.handler(responseHandler).end();

    countDownLatch.await();
    return new Response(
        bufferHandler.responseBody == null || bufferHandler.responseBody.isEmpty()
            ? null
            : bufferHandler.responseBody,
        responseHandler.responseStatus);
  }

  static class ResponseHandler implements Handler<HttpClientResponse> {

    int responseStatus;
    final BufferHandler bufferHandler;

    ResponseHandler(BufferHandler bufferHandler) {
      this.bufferHandler = bufferHandler;
    }

    @Override
    public void handle(HttpClientResponse response) {
      response.bodyHandler(bufferHandler);
      responseStatus = response.statusCode();
    }
  }

  static class BufferHandler implements Handler<Buffer> {

    String responseBody;
    final CountDownLatch countDownLatch;

    BufferHandler(CountDownLatch countDownLatch) {
      this.countDownLatch = countDownLatch;
    }

    @Override
    public void handle(Buffer responseBodyBuffer) {
      responseBody = responseBodyBuffer.getString(0, responseBodyBuffer.length());
      countDownLatch.countDown();
    }
  }

  @Test
  public void postJson_write_end() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    BufferHandler bufferHandler = new BufferHandler(countDownLatch);
    ResponseHandler responseHandler = new ResponseHandler(bufferHandler);

    request
        .handler(responseHandler)
        .write("write")
        .write(Buffer.buffer().appendString(" buffer"))
        .write(" str_encoding ", "utf-8")
        .end();
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(1, traces.size());
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "write buffer str_encoding ",
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
  }

  @Test
  @Disabled("This is flaky")
  public void postJson_write_end_string() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    BufferHandler bufferHandler = new BufferHandler(countDownLatch);
    ResponseHandler responseHandler = new ResponseHandler(bufferHandler);

    request
        .handler(responseHandler)
        .write("write")
        .write(Buffer.buffer().appendString(" buffer"))
        .write(" str_encoding ", "utf-8")
        .end("end");
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(1, traces.size(), String.format("was: %d", traces.size()));
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "write buffer str_encoding end",
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
  }

  @Test
  @Disabled("This is flaky on github actions!!")
  public void postJson_write_end_buffer() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    BufferHandler bufferHandler = new BufferHandler(countDownLatch);
    ResponseHandler responseHandler = new ResponseHandler(bufferHandler);

    request
        .handler(responseHandler)
        .write("write")
        .write(Buffer.buffer().appendString(" buffer"))
        .write(" str_encoding ", "utf-8")
        .end(Buffer.buffer("end"));
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                !span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getAttributesList().stream()
                        .noneMatch(
                            keyValue ->
                                keyValue.getKey().equals("http.url")
                                    && keyValue.getValue().getStringValue().contains("/echo")));
    Assertions.assertEquals(1, traces.size(), String.format("was: %d", traces.size()));
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "write buffer str_encoding end",
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
  }
}
