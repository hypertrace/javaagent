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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
  public Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType) throws IOException {

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
    InputStream inputStream = response.getEntity().getContent();
    return new Response(readInputStream(inputStream), response.getStatusLine().getStatusCode());
  }

  @Override
  public Response doGetRequest(String uri, Map<String, String> headers) throws IOException {
    HttpGet request = new HttpGet();
    for (String key : headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }
    request.setURI(URI.create(uri));
    HttpResponse response = client.execute(request);
    if (response.getEntity() == null || response.getEntity().getContentLength() <= 0) {
      return new Response(null, response.getStatusLine().getStatusCode());
    }
    InputStream inputStream = response.getEntity().getContent();
    return new Response(readInputStream(inputStream), response.getStatusLine().getStatusCode());
  }
}
