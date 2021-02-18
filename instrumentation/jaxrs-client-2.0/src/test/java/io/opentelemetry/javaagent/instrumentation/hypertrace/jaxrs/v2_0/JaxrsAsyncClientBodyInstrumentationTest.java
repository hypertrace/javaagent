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

package io.opentelemetry.javaagent.instrumentation.hypertrace.jaxrs.v2_0;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.hypertrace.agent.testing.AbstractHttpClientTest;

public class JaxrsAsyncClientBodyInstrumentationTest extends AbstractHttpClientTest {

  public JaxrsAsyncClientBodyInstrumentationTest() {
    super(true);
  }

  @Override
  public Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws ExecutionException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.register(MyDtoMessageBodyWriter.class).build();

    Invocation.Builder builder = client.target(uri).request();

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder = builder.header(entry.getKey(), entry.getValue());
    }

    MyDto myDto = new MyDto();
    myDto.data = body;

    Future<javax.ws.rs.core.Response> post =
        builder.async().post(Entity.entity(myDto, MediaType.valueOf(contentType)));
    javax.ws.rs.core.Response response = post.get();

    return new Response(response.readEntity(String.class), response.getStatus());
  }

  @Override
  public Response doGetRequest(String uri, Map<String, String> headers)
      throws ExecutionException, InterruptedException {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    Client client = clientBuilder.build();

    Invocation.Builder builder = client.target(uri).request();

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder = builder.header(entry.getKey(), entry.getValue());
    }

    Future<javax.ws.rs.core.Response> responseFuture = builder.async().get();

    javax.ws.rs.core.Response response = responseFuture.get();

    String responseBody = response.readEntity(String.class);

    return new Response(
        responseBody == null || responseBody.isEmpty() ? null : responseBody, response.getStatus());
  }

  public static class MyDto {
    public String data;
  }

  public static class MyDtoMessageBodyWriter implements MessageBodyWriter<MyDto> {

    @Override
    public void writeTo(
        MyDto myDto,
        Class<?> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream)
        throws IOException, WebApplicationException {
      entityStream.write((myDto.data).getBytes());
    }

    @Override
    public boolean isWriteable(
        Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
      return true;
    }
  }
}
