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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.hypertrace.agent.testing.AbstractHttpClientTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

class ApacheAsyncClientInstrumentationModuleTest extends AbstractHttpClientTest {

  private static final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

  public ApacheAsyncClientInstrumentationModuleTest() {
    super(true);
  }

  @BeforeAll
  public static void startClient() throws Exception {
    client.start();
  }

  @AfterAll
  public static void closeClient() throws IOException {
    client.close();
  }

  @Override
  public Pair<Integer, String> doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws IOException, ExecutionException, InterruptedException {
    HttpPost request = new HttpPost();
    for (String key : headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }
    request.setURI(URI.create(uri));
    StringEntity entity = new StringEntity(body);
    entity.setContentType(contentType);
    request.setEntity(entity);
    request.addHeader("Content-type", contentType);
    Future<HttpResponse> responseFuture = client.execute(request, new NoopFutureCallback());
    HttpResponse response = responseFuture.get();
    Thread.sleep(200);
    if (response.getEntity() == null || response.getEntity().getContentLength() <= 0) {
      return Pair.of(response.getStatusLine().getStatusCode(), null);
    }
    String responseBody = readInputStream(response.getEntity().getContent());
    Assertions.assertFalse(Span.current().isRecording());
    return Pair.of(response.getStatusLine().getStatusCode(), responseBody);
  }

  @Override
  public Pair<Integer, String> doGetRequest(String uri, Map<String, String> headers)
      throws IOException, ExecutionException, InterruptedException {
    HttpGet request = new HttpGet(uri);
    for (String key : headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }
    Future<HttpResponse> responseFuture = client.execute(request, new NoopFutureCallback());
    HttpResponse response = responseFuture.get();
    Thread.sleep(200);
    if (response.getEntity() == null || response.getEntity().getContentLength() <= 0) {
      return Pair.of(response.getStatusLine().getStatusCode(), null);
    }
    String responseBody = readInputStream(response.getEntity().getContent());
    Assertions.assertFalse(Span.current().isRecording());
    return Pair.of(response.getStatusLine().getStatusCode(), responseBody);
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

  static class NoopFutureCallback implements FutureCallback<HttpResponse> {
    @Override
    public void completed(HttpResponse result) {}

    @Override
    public void failed(Exception ex) {}

    @Override
    public void cancelled() {}
  }
}
