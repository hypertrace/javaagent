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

package io.opentelemetry.javaagent.instrumentation.hypertrace.spring.webflux;

import io.opentelemetry.javaagent.instrumentation.hypertrace.spring.webflux.SpringWebFluxTestApplication.GreetingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.spring.webflux.SpringWebfluxServerTest.ForceNettyAutoConfiguration;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

@ExtendWith(SpringExtension.class) // enables junit5
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {SpringWebFluxTestApplication.class, ForceNettyAutoConfiguration.class})
public class SpringWebfluxServerTest extends AbstractInstrumenterTest {

  static final String REQUEST_HEADER_NAME = "reqheader";
  static final String REQUEST_HEADER_VALUE = "reqheadervalue";
  static final String REQUEST_BODY = "foobar";

  @TestConfiguration
  static class ForceNettyAutoConfiguration {
    @Bean
    NettyReactiveWebServerFactory nettyFactory() {
      return new NettyReactiveWebServerFactory();
    }
  }

  @LocalServerPort private int port;

  @Test
  public void get() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        SpringWebFluxTestApplication.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + SpringWebFluxTestApplication.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.request.body"));
    Assertions.assertNull(TEST_WRITER.getAttributesMap(span).get("http.response.body"));
  }

  @Test
  public void getStream() throws IOException, TimeoutException, InterruptedException {
    Flux<FooModel> modelFlux = SpringWebFluxTestApplication.finiteStream();
    StringBuilder responseBodyStr = new StringBuilder();
    modelFlux
        .flatMap(
            fooModel -> {
              responseBodyStr.append(fooModel.toString()).append("\n");
              return Flux.empty();
            })
        .then()
        .block();

    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/stream", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    ServerResponse serverResponse = SpringWebFluxTestApplication.finiteStreamResponse().block();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(responseBodyStr.toString(), response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);
    Assertions.assertEquals(
        responseBodyStr.toString(),
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
  }

  @Test
  public void post() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .post(RequestBody.create(REQUEST_BODY, MediaType.parse("application/json")))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(202, response.code());
      Assertions.assertEquals(REQUEST_BODY, response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        SpringWebFluxTestApplication.RESPONSE_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + SpringWebFluxTestApplication.RESPONSE_HEADER_NAME)
            .getStringValue());
    Assertions.assertEquals(
        REQUEST_BODY, TEST_WRITER.getAttributesMap(span).get("http.request.body").getStringValue());
    Assertions.assertEquals(
        REQUEST_BODY,
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
  }

  @Test
  public void blocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("mockblock", "true")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertEquals("Hypertrace Blocked Request", response.body().string());
    }

    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.request.header." + REQUEST_HEADER_NAME)
            .getStringValue());
    Assertions.assertNull(
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + SpringWebFluxTestApplication.RESPONSE_HEADER_NAME));
    Assertions.assertNull(
        TEST_WRITER
            .getAttributesMap(span)
            .get("http.response.header." + GreetingHandler.DEFAULT_RESPONSE));
  }
}
