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
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class VertxClientInstrumentationPostTests extends AbstractInstrumenterTest {

  private static final TestHttpServer testHttpServer = new TestHttpServer();
  private final Vertx vertx = Vertx.vertx(new VertxOptions());
  private final HttpClientOptions clientOptions = new HttpClientOptions();
  private final HttpClient httpClient = vertx.createHttpClient(clientOptions);

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void postJson_write_end() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    VertxClientInstrumentationTest.BufferHandler bufferHandler =
        new VertxClientInstrumentationTest.BufferHandler(countDownLatch);
    VertxClientInstrumentationTest.ResponseHandler responseHandler =
        new VertxClientInstrumentationTest.ResponseHandler(bufferHandler);

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
  public void postJson_write_end_string() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    VertxClientInstrumentationTest.BufferHandler bufferHandler =
        new VertxClientInstrumentationTest.BufferHandler(countDownLatch);
    VertxClientInstrumentationTest.ResponseHandler responseHandler =
        new VertxClientInstrumentationTest.ResponseHandler(bufferHandler);

    request
        .handler(responseHandler)
        .write("write")
        .write(Buffer.buffer().appendString(" buffer"))
        .write(" str_encoding ", "utf-8")
        .end("end");
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER));
    Assertions.assertEquals(1, traces.size(), String.format("was: %d", traces.size()));
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "write buffer str_encoding end",
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
  }

  @Test
  public void postJson_write_end_buffer() throws TimeoutException, InterruptedException {
    String uri = String.format("http://localhost:%d/echo", testHttpServer.port());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, uri);
    request = request.putHeader("Content-Type", "application/json");
    request.setChunked(true);
    VertxClientInstrumentationTest.BufferHandler bufferHandler =
        new VertxClientInstrumentationTest.BufferHandler(countDownLatch);
    VertxClientInstrumentationTest.ResponseHandler responseHandler =
        new VertxClientInstrumentationTest.ResponseHandler(bufferHandler);

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
