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

package io.opentelemetry.instrumentation.hypertrace.apachehttpclient.v4_0;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApacheHttpClientInstrumentationTest extends AbstractInstrumenterTest {

  private static final String JSON = "{\"id\":1,\"name\":\"John\"}";
  private static final TestHttpServer testHttpServer = new TestHttpServer();

  private final HttpClient client = new DefaultHttpClient();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void getJson() throws IOException, TimeoutException, InterruptedException {
    HttpGet getRequest = new HttpGet();
    getRequest.addHeader("foo", "bar");
    getRequest.setURI(
        URI.create(String.format("http://localhost:%d/get_json", testHttpServer.port())));
    HttpResponse response = client.execute(getRequest);
    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
    InputStream inputStream = response.getEntity().getContent();
    Assertions.assertEquals(GetJsonHandler.RESPONSE_BODY, readInputStream(inputStream));

    Assertions.assertEquals(false, Span.current().isRecording());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(2, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertEquals(
        "bar",
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.httpRequestHeader("foo")));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    SpanData responseBodySpan = traces.get(0).get(1);
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postUrlEncoded() throws IOException, TimeoutException, InterruptedException {
    List<NameValuePair> nvps = new ArrayList<>();
    nvps.add(new BasicNameValuePair("code", "22"));

    HttpPost postRequest = new HttpPost();
    postRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    postRequest.setURI(
        URI.create(String.format("http://localhost:%d/post", testHttpServer.port())));
    HttpResponse response = client.execute(postRequest);
    Assertions.assertEquals(204, response.getStatusLine().getStatusCode());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertEquals(
        "code=22", clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postJson() throws IOException, TimeoutException, InterruptedException {
    StringEntity entity = new StringEntity(JSON);
    postJsonEntity(entity);
  }

  // TODO enable the NonRepeatableStringEntity causes the HttpEntity.writeTo instrumentation fail
  //  @Test
  //  public void postJsonNonRepeatableEntity()
  //      throws IOException, TimeoutException, InterruptedException {
  //    StringEntity entity = new NonRepeatableStringEntity(JSON);
  //    postJsonEntity(entity);
  //  }

  public void postJsonEntity(HttpEntity entity)
      throws TimeoutException, InterruptedException, IOException {
    HttpPost postRequest = new HttpPost();
    postRequest.setEntity(entity);
    postRequest.setHeader("Content-type", "application/json");
    postRequest.setURI(
        URI.create(String.format("http://localhost:%d/post", testHttpServer.port())));
    HttpResponse response = client.execute(postRequest);
    Assertions.assertEquals(204, response.getStatusLine().getStatusCode());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertEquals(
        readInputStream(entity.getContent()),
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void getContent_throws_exception() throws IOException {
    HttpClient client = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet();
    getRequest.setURI(
        URI.create(String.format("http://localhost:%d/get_json", testHttpServer.port())));
    HttpResponse response = client.execute(getRequest);
    HttpEntity entity = response.getEntity();
    Assertions.assertNotNull(entity.getContent());
    try {
      entity.getContent();
    } catch (Exception ex) {
      Assertions.assertEquals(IllegalStateException.class, ex.getClass());
    }
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

  //  static class NonRepeatableStringEntity extends StringEntity {
  //
  //    public NonRepeatableStringEntity(String s) throws UnsupportedEncodingException {
  //      super(s);
  //    }
  //
  //    @Override
  //    public boolean isRepeatable() {
  //      return false;
  //    }
  //
  //    @Override
  //    public InputStream getContent() throws IOException {
  //      return super.getContent();
  //    }
  //
  //    @Override
  //    public void writeTo(OutputStream outstream) throws IOException {
  //      System.out.println("writeTo in:");
  //      System.out.println(this.getClass().getName());
  //      super.writeTo(outstream);
  //    }
  //  }
}
