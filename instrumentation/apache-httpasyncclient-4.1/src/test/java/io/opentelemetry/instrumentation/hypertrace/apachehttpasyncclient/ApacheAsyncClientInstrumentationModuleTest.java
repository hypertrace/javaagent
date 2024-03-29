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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ApacheAsyncClientInstrumentationModuleTest extends AbstractInstrumenterTest {

  private static final String JSON = "{\"id\":1,\"name\":\"John\"}";
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

  @Disabled("This is flaky !!")
  @Test
  public void getJson()
      throws ExecutionException, InterruptedException, TimeoutException, IOException {
    HttpGet getRequest =
        new HttpGet(String.format("http://localhost:%s/get_json", testHttpServer.port()));
    getRequest.addHeader("foo", "bar");
    Future<HttpResponse> futureResponse = client.execute(getRequest, new NoopFutureCallback());

    HttpResponse response = futureResponse.get();
    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
    String responseBody = readInputStream(response.getEntity().getContent());
    Assertions.assertEquals(GetJsonHandler.RESPONSE_BODY, responseBody);

    TEST_WRITER.waitForTraces(1);
    // exclude server spans
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            2, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER), 123);
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
        GetJsonHandler.RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(responseBodySpan).get("http.response.body").getStringValue());
  }

  @Test
  public void postJson()
      throws IOException, TimeoutException, InterruptedException, ExecutionException {
    StringEntity entity =
        new StringEntity(JSON, ContentType.create(ContentType.APPLICATION_JSON.getMimeType()));
    postJsonEntity(entity);
  }

  @Test
  public void postJsonNonRepeatableEntity()
      throws IOException, TimeoutException, InterruptedException, ExecutionException {
    StringEntity entity = new NonRepeatableStringEntity(JSON);
    postJsonEntity(entity);
  }

  public void postJsonEntity(HttpEntity entity)
      throws TimeoutException, InterruptedException, IOException, ExecutionException {
    HttpPost postRequest = new HttpPost();
    postRequest.setEntity(entity);
    postRequest.setHeader("Content-type", "application/json");
    postRequest.setURI(
        URI.create(String.format("http://localhost:%d/post", testHttpServer.port())));

    Future<HttpResponse> responseFuture = client.execute(postRequest, new NoopFutureCallback());

    HttpResponse response = responseFuture.get();
    Assertions.assertEquals(204, response.getStatusLine().getStatusCode());

    TEST_WRITER.waitForTraces(1);
    // exclude server spans
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    Span clientSpan = traces.get(0).get(0);

    String requestBody = readInputStream(entity.getContent());
    Assertions.assertEquals(
        "test-value",
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.response.header.test-response-header")
            .getStringValue());
    Assertions.assertEquals(
        requestBody,
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.response.body"));
  }

  private static String readInputStream(InputStream inputStream) throws IOException {
    StringBuilder textBuilder = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    return textBuilder.toString();
  }

  static class NonRepeatableStringEntity extends StringEntity {

    public NonRepeatableStringEntity(String s) throws UnsupportedEncodingException {
      super(s);
    }

    @Override
    public Header getContentType() {
      return new BasicHeader("Content-Type", "json");
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public InputStream getContent() {
      return new ByteArrayInputStream(this.content);
    }
  }

  static class NoopFutureCallback implements FutureCallback<HttpResponse> {
    @Override
    public void completed(HttpResponse result) {}

    @Override
    public void failed(Exception ex) {}

    @Override
    public void cancelled() {}
  }
}
