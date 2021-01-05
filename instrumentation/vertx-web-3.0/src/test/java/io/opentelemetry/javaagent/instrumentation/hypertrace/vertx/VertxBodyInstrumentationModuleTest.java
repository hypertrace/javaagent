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
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VertxBodyInstrumentationModuleTest extends AbstractInstrumenterTest {

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";

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
                // TODO use random port
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
    System.out.println("Closing vertx");
    vertx.close();
    System.out.println("vertx closed");
  }

  @Test
  public void test() throws IOException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/success", port))
            .post(RequestBody.create("{\"foo\": \"bar\"}", MediaType.get("application/json")))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals("success", response.body().string());
      // TODO test
      //      Assertions.assertEquals("chunk1chunk2success", response.body().string());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
  }
}
