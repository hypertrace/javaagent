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

package io.opentelemetry.instrumentation.hypertrace.jaxrs.v2_0;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JaxrsClientBodyInstrumentationTest extends AbstractInstrumenterTest {

  private static final String JSON = "{\"id\":1,\"name\":\"John\"}";
  private static final TestHttpServer testHttpServer = new TestHttpServer();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void getJson() throws TimeoutException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.build();

    Response response =
        client
            .target(String.format("http://localhost:%d/get_json", testHttpServer.port()))
            .request()
            .header("test-request-header", "test-header-value")
            .get();
    Assertions.assertEquals(200, response.getStatus());
    // read entity has to happen before response.close()
    String entity = response.readEntity(String.class);
    Assertions.assertEquals(GetJsonHandler.RESPONSE_BODY, entity);
    Assertions.assertEquals(false, Span.current().isRecording());
    response.close();

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
        "test-header-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader("test-request-header")));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    SpanData responseBodySpan = traces.get(0).get(1);
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postJson() throws TimeoutException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.build();

    MyDto myDto = new MyDto();
    myDto.name = "foo";

    Response response =
        client
            .target(String.format("http://localhost:%d/post", testHttpServer.port()))
            .request()
            .header("test-request-header", "test-header-value")
            .post(Entity.entity(JSON, MediaType.APPLICATION_JSON_TYPE));
    Assertions.assertEquals(204, response.getStatus());

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
        JSON, clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postJsonDto() throws TimeoutException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.register(MyDtoMessageBodyWriter.class).build();

    MyDto myDto = new MyDto();
    myDto.name = "name";

    Response response =
        client
            .target(String.format("http://localhost:%d/post", testHttpServer.port()))
            .request()
            .header("test-request-header", "test-header-value")
            .post(Entity.json(myDto));
    Assertions.assertEquals(204, response.getStatus());

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
        JSON, clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postUrlEncoded() throws TimeoutException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.build();

    WebTarget webTarget =
        client.target(String.format("http://localhost:%d/post", testHttpServer.port()));
    MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
    formData.add("key1", "value1");
    formData.add("key2", "value2");
    Response response = webTarget.request().post(Entity.form(formData));
    Assertions.assertEquals(204, response.getStatus());

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
        "key1=value1&key2=value2",
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  public static class MyDto {
    public String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class MyDtoMessageBodyWriter implements MessageBodyWriter<MyDto> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
      return true;
    }

    @Override
    public void writeTo(MyDto myDto, Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {
      entityStream.write(myDto.name.getBytes());
    }
  }
}
