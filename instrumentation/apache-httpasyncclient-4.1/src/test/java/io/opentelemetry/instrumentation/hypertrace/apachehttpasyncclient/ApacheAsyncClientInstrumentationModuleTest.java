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

package io.opentelemetry.instrumentation.hypertrace.apachehttpasyncclient;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApacheAsyncClientInstrumentationModuleTest extends AbstractInstrumenterTest {

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void test() throws ExecutionException, InterruptedException, TimeoutException {
    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
    client.start();

    Future<HttpResponse> futureResponse =
        client.execute(
            new HttpGet(String.format("http://localhost:%s/get_json", testHttpServer.port())),
            new FutureCallback<HttpResponse>() {
              @Override
              public void completed(HttpResponse result) {
                System.out.println("commpleted");
              }

              @Override
              public void failed(Exception ex) {}

              @Override
              public void cancelled() {}
            });

    HttpResponse response = futureResponse.get();
    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
  }
}
