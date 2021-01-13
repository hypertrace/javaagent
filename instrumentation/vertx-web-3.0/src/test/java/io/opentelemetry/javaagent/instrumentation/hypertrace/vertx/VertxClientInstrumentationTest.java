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

import io.opentelemetry.sdk.trace.data.SpanData;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class VertxClientInstrumentationTest extends AbstractInstrumenterTest {

  private static final String REQUEST_BODY = "hello_foo_bar";
  private static final String REQUEST_HEADER_NAME = "reqheadername";
  private static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  private static final Vertx vertx = Vertx.vertx(new VertxOptions());
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
  public void getJson() throws InterruptedException, TimeoutException {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    httpClient
        .request(HttpMethod.GET, testHttpServer.port(), "localhost", "/get_json")
        .putHeader(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
        .handler(
            new Handler<HttpClientResponse>() {
              @Override
              public void handle(HttpClientResponse response) {
                Assertions.assertEquals(200, response.statusCode());
                countDownLatch.countDown();
              }
            })
        .end();
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void post() throws InterruptedException, TimeoutException {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    httpClient
        .request(HttpMethod.POST, testHttpServer.port(), "localhost", "/post")
        .putHeader(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
        .putHeader("Content-Type", "application/json")
        .handler(
            new Handler<HttpClientResponse>() {
              @Override
              public void handle(HttpClientResponse response) {
                Assertions.assertEquals(204, response.statusCode());
                countDownLatch.countDown();
              }
            })
        .end(REQUEST_BODY);
    countDownLatch.await();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
    Assertions.assertEquals(
        REQUEST_BODY,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }
}
