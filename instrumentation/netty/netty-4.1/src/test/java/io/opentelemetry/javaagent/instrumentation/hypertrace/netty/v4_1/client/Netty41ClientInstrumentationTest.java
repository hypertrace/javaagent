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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.client;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.hypertrace.agent.testing.AbstractHttpClientTest;

public class Netty41ClientInstrumentationTest extends AbstractHttpClientTest {

  private final DefaultAsyncHttpClientConfig clientConfig =
      new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(30000).build();
  private final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient(clientConfig);

  public Netty41ClientInstrumentationTest() {
    super(false);
  }

  @Override
  public Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws ExecutionException, InterruptedException {

    ByteArrayInputStream inputStream = new ByteArrayInputStream(body.getBytes());
    BoundRequestBuilder requestBuilder = asyncHttpClient.preparePost(uri).setBody(inputStream);

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
    }

    requestBuilder = requestBuilder.addHeader("Content-Type", contentType);
    ListenableFuture<Response> response =
        requestBuilder.execute(
            new AsyncCompletionHandler<Response>() {
              @Override
              public Response onCompleted(org.asynchttpclient.Response response) {
                return new Response(
                    response.hasResponseBody() ? response.getResponseBody() : null,
                    response.getStatusCode());
              }
            });

    // wait for the result
    return response.get();
  }

  @Override
  public Response doGetRequest(String uri, Map<String, String> headers)
      throws ExecutionException, InterruptedException {

    BoundRequestBuilder requestBuilder = asyncHttpClient.prepareGet(uri);

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
    }

    ListenableFuture<Response> response =
        requestBuilder.execute(
            new AsyncCompletionHandler<Response>() {
              @Override
              public Response onCompleted(org.asynchttpclient.Response response) {
                return new Response(
                    response.hasResponseBody() ? response.getResponseBody() : null,
                    response.getStatusCode());
              }
            });

    // wait for the result
    return response.get();
  }
}
