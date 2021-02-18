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

import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import org.hypertrace.agent.testing.AbstractHttpClientTest;

public class JaxrsClientBodyInstrumentationTest extends AbstractHttpClientTest {

  private static final Client client = ClientBuilder.newBuilder().build();

  public JaxrsClientBodyInstrumentationTest() {
    super(true);
  }

  @Override
  public AbstractHttpClientTest.Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType) {

    Invocation.Builder builder = client.target(uri).request();

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder = builder.header(entry.getKey(), entry.getValue());
    }

    javax.ws.rs.core.Response response =
        builder.post(Entity.entity(body, MediaType.valueOf(contentType)));

    return new Response(response.readEntity(String.class), response.getStatus());
  }

  @Override
  public AbstractHttpClientTest.Response doGetRequest(String uri, Map<String, String> headers) {

    Invocation.Builder builder = client.target(uri).request();

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder = builder.header(entry.getKey(), entry.getValue());
    }

    javax.ws.rs.core.Response response = builder.get();

    String responseBody = response.readEntity(String.class);

    return new Response(
        responseBody == null || responseBody.isEmpty() ? null : responseBody, response.getStatus());
  }
}
