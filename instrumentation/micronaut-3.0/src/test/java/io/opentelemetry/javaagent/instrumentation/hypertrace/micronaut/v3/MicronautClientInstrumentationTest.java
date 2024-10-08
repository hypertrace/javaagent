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

package io.opentelemetry.javaagent.instrumentation.hypertrace.micronaut.v3;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@MicronautTest
public class MicronautClientInstrumentationTest extends AbstractInstrumenterTest {

  private static final String REQUEST_BODY = "hello_foo_bar";
  private static final String REQUEST_HEADER_NAME = "reqheadername";
  private static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Inject
  @Client("/")
  private HttpClient client;

  @Test
  public void getJson() throws InterruptedException, TimeoutException {
    String retrieve =
        client
            .toBlocking()
            .retrieve(
                HttpRequest.GET(
                        String.format("http://localhost:%d/get_json", testHttpServer.port()))
                    .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE));
    Assertions.assertEquals(GetJsonHandler.RESPONSE_BODY, retrieve);

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.response.header." + TestHttpServer.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body"));
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        TEST_WRITER.getAttributesMap(clientSpan).get("http.response.body").getStringValue());
  }

  @Test
  public void post() throws InterruptedException, TimeoutException {
    HttpResponse<Object> response =
        client
            .toBlocking()
            .exchange(
                HttpRequest.POST(
                        String.format("http://localhost:%d/post", testHttpServer.port()),
                        REQUEST_BODY)
                    .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
                    .header("Content-Type", "application/json"));
    Assertions.assertEquals(204, response.getStatus().getCode());

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(1, span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.response.header." + TestHttpServer.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        REQUEST_BODY,
        TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body").getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.response.body"));
  }

  @Test
  public void getGzipResponse() throws TimeoutException, InterruptedException, IOException {
    String retrieve =
        client
            .toBlocking()
            .retrieve(
                HttpRequest.GET(String.format("http://localhost:%d/gzip", testHttpServer.port()))
                    .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE));
    Assertions.assertEquals("{\"message\": \"hello\"}", retrieve);

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                !span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getAttributesList().stream()
                        .noneMatch(
                            keyValue ->
                                keyValue.getKey().equals("http.url")
                                    && keyValue.getValue().getStringValue().contains("/gzip")));
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    Span clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(clientSpan)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(clientSpan).get("http.request.body"));

    String respBodyCapturedInSpan =
        TEST_WRITER.getAttributesMap(clientSpan).get("http.response.body").getStringValue();
    Assertions.assertEquals("{\"message\": \"hello\"}", respBodyCapturedInSpan);
  }
}
