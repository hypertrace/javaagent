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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0;

import io.opentelemetry.javaagent.instrumentation.api.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hypertrace.agent.testing.AbstractHttpClientTest;

public class ApacheHttpClientInstrumentationTest extends AbstractHttpClientTest {

  private final HttpClient client = new DefaultHttpClient();

  public ApacheHttpClientInstrumentationTest() {
    super(true);
  }

  @Override
  public Pair<Integer, String> doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws IOException, InterruptedException {

    HttpPost request = new HttpPost();
    for (String key : headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }
    request.setURI(URI.create(uri));
    StringEntity entity = new StringEntity(body);
    entity.setContentType(contentType);
    request.setEntity(entity);
    request.addHeader("Content-type", contentType);
    HttpResponse response = client.execute(request);
    Thread.sleep(200);
    InputStream inputStream = response.getEntity().getContent();
    return Pair.of(response.getStatusLine().getStatusCode(), readInputStream(inputStream));
  }

  @Override
  public Pair<Integer, String> doGetRequest(String uri, Map<String, String> headers)
      throws IOException, InterruptedException {
    HttpGet request = new HttpGet();
    for (String key : headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }
    request.setURI(URI.create(uri));
    HttpResponse response = client.execute(request);
    Thread.sleep(200);
    if (response.getEntity() == null || response.getEntity().getContentLength() <= 0) {
      return Pair.of(response.getStatusLine().getStatusCode(), null);
    }
    InputStream inputStream = response.getEntity().getContent();
    return Pair.of(response.getStatusLine().getStatusCode(), readInputStream(inputStream));
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
}
