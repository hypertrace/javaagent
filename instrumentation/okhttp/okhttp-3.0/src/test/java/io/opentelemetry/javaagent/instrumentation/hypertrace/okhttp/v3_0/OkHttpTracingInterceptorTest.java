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

package io.opentelemetry.javaagent.instrumentation.hypertrace.okhttp.v3_0;

import io.opentelemetry.javaagent.instrumentation.api.Pair;
import java.io.IOException;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.testing.AbstractHttpClientTest;

public class OkHttpTracingInterceptorTest extends AbstractHttpClientTest {

  private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(true).build();

  public OkHttpTracingInterceptorTest() {
    super(false);
  }

  @Override
  public Pair<Integer, String> doPostRequest(
      String uri, Map<String, String> headersMap, String body, String contentType)
      throws IOException {

    Headers.Builder headers = new Headers.Builder();
    for (String key : headersMap.keySet()) {
      headers.add(key, headersMap.get(key));
    }

    Request request =
        new Request.Builder()
            .url(uri)
            .post(RequestBody.create(body, MediaType.get(contentType)))
            .headers(headers.build())
            .build();

    Response response = client.newCall(request).execute();

    return Pair.of(response.code(), response.body() != null ? response.body().string() : null);
  }

  @Override
  public Pair<Integer, String> doGetRequest(String uri, Map<String, String> headersMap)
      throws IOException {
    Headers.Builder headers = new Headers.Builder();
    for (String key : headersMap.keySet()) {
      headers.add(key, headersMap.get(key));
    }

    Request request = new Request.Builder().url(uri).headers(headers.build()).get().build();

    Response response = client.newCall(request).execute();

    return Pair.of(
        response.code(),
        (response.body() != null && response.body().contentLength() > 0)
            ? response.body().string()
            : null);
  }
}
