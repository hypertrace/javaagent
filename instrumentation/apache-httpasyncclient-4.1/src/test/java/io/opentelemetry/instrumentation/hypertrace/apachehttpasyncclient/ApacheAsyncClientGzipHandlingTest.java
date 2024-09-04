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

import io.opentelemetry.proto.trace.v1.Span;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApacheAsyncClientGzipHandlingTest extends AbstractInstrumenterTest {

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  private static final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
    client.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void getGzipResponse()
      throws ExecutionException, InterruptedException, TimeoutException, IOException {
    HttpGet getRequest =
        new HttpGet(String.format("http://localhost:%s/gzip", testHttpServer.port()));
    getRequest.addHeader("foo", "bar");
    Future<HttpResponse> futureResponse =
        client.execute(
            getRequest, new ApacheAsyncClientInstrumentationModuleTest.NoopFutureCallback());

    HttpResponse response = futureResponse.get();
    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
    try (InputStream gzipStream = new GZIPInputStream(response.getEntity().getContent())) {
      String responseBody = readInputStream(gzipStream);
      Assertions.assertEquals(TestHttpServer.GzipHandler.RESPONSE_BODY, responseBody);
    }

    TEST_WRITER.waitForTraces(1);
    // exclude server spans
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            2,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER)
                    || span.getAttributesList().stream()
                        .noneMatch(
                            keyValue ->
                                keyValue.getKey().equals("http.response.header.content-encoding")
                                    && keyValue.getValue().getStringValue().contains("gzip")));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(2, traces.get(0).size());
    Span clientSpan = traces.get(0).get(1);
    Span responseBodySpan = traces.get(0).get(0);
    if (traces.get(0).get(0).getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)) {
      clientSpan = traces.get(0).get(0);
      responseBodySpan = traces.get(0).get(1);
    }

    Assertions.assertEquals(
        "test-value",
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.response.header.test-response-header")
            .getStringValue());
    Assertions.assertEquals(
        "bar",
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.header.foo").getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body"));

    Assertions.assertEquals(
        TestHttpServer.GzipHandler.RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(responseBodySpan).get("http.response.body").getStringValue());
  }

  private String readInputStream(InputStream inputStream) throws IOException {
    StringBuilder textBuilder = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    return textBuilder.toString();
  }
}
