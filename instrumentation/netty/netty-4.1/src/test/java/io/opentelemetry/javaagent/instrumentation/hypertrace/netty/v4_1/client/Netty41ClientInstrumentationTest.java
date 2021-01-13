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

import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Netty41ClientInstrumentationTest extends AbstractInstrumenterTest {

  private static final String REQUEST_BODY = "hello_foo_bar";
  private static final String REQUEST_HEADER_NAME = "reqheadername";
  private static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  private final DefaultAsyncHttpClientConfig clientConfig =
      new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(30000).build();
  private final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient(clientConfig);

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void getJson() throws ExecutionException, InterruptedException, TimeoutException {
    ListenableFuture<Object> response =
        asyncHttpClient
            .prepareGet(String.format("http://localhost:%d/get_json", testHttpServer.port()))
            .addHeader(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .execute(
                new AsyncCompletionHandler<Object>() {
                  @Override
                  public Object onCompleted(Response response) throws Exception {
                    return null;
                  }
                });

    // wait for the result
    response.get();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void post() throws ExecutionException, InterruptedException, TimeoutException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(REQUEST_BODY.getBytes());

    ListenableFuture<Object> response =
        asyncHttpClient
            .preparePost(String.format("http://localhost:%d/post", testHttpServer.port()))
            .setBody(inputStream)
            .addHeader("Content-Type", "application/json")
            .addHeader(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .execute(
                new AsyncCompletionHandler<Object>() {
                  @Override
                  public Object onCompleted(Response response) throws Exception {
                    return null;
                  }
                });

    // wait for the result
    response.get();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        clientSpan
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
    Assertions.assertEquals(
        REQUEST_BODY,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }
}
