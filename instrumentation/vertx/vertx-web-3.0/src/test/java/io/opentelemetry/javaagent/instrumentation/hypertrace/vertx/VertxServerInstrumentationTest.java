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
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VertxServerInstrumentationTest extends AbstractInstrumenterTest {

  static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  static final String REQUEST_BODY = "{\"foo\": \"bar\"}";

  private static final String REQUEST_HEADER_NAME = "reqheader";
  private static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static Vertx vertx;
  private static int port;

  @BeforeAll
  public static void startServer()
      throws InterruptedException, ExecutionException, TimeoutException, IOException {

    ServerSocket socket;
    socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    vertx =
        Vertx.vertx(
            new VertxOptions()
                // Useful for debugging:
                // .setBlockedThreadCheckInterval(Integer.MAX_VALUE)
                .setClusterPort(port));
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(
        VertxWebServer.class.getName(),
        new DeploymentOptions()
            .setConfig(new JsonObject().put(CONFIG_HTTP_SERVER_PORT, port))
            .setInstances(3),
        res -> {
          if (!res.succeeded()) {
            throw new RuntimeException("Cannot deploy server Verticle", res.cause());
          }
          future.complete(null);
        });

    future.get(20, TimeUnit.SECONDS);
  }

  @AfterAll
  public static void stopServer() {
    vertx.close();
  }

  @Test
  public void postJsonReturnChunked() throws IOException, TimeoutException, InterruptedException {
    postJson(String.format("http://localhost:%d/return_chunked", port));
  }

  @Test
  public void postJsonReturnNoChunked() throws IOException, TimeoutException, InterruptedException {
    postJson(String.format("http://localhost:%d/return_no_chunked", port));
  }

  @Test
  public void get() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertEquals(
        VertxWebServer.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    VertxWebServer.RESPONSE_HEADER_NAME)));
  }

  @Test
  public void blocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("mockblock", "true")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertNull(
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    VertxWebServer.RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(VertxWebServer.RESPONSE_BODY)));
  }

  public void postJson(String url) throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(url)
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .post(RequestBody.create(MediaType.get("application/json"), REQUEST_BODY))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(VertxWebServer.RESPONSE_BODY, response.body().string());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);
    Assertions.assertEquals(
        REQUEST_BODY, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        VertxWebServer.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertEquals(
        VertxWebServer.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    VertxWebServer.RESPONSE_HEADER_NAME)));
  }
}
