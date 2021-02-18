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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import org.hypertrace.agent.testing.AbstractHttpClientTest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class SpringWebfluxClientTest extends AbstractHttpClientTest {

  public SpringWebfluxClientTest() {
    super(false);
  }

  @Override
  public Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType) {
    WebClient.Builder clientBuilder = WebClient.builder().baseUrl(uri);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      clientBuilder = clientBuilder.defaultHeader(entry.getKey(), entry.getValue());
    }
    clientBuilder = clientBuilder.defaultHeader("Content-Type", contentType);
    WebClient client = clientBuilder.build();

    ClientResponse clientResponse =
        client.post().body(Mono.just(body), String.class).exchange().block();
    if (clientResponse == null) fail();
    int responseStatus = clientResponse.statusCode().value();
    String responseBody = clientResponse.bodyToMono(String.class).block();
    return new Response(responseBody, responseStatus);
  }

  @Override
  public Response doGetRequest(String uri, Map<String, String> headers) {
    WebClient.Builder clientBuilder = WebClient.builder().baseUrl(uri);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      clientBuilder = clientBuilder.defaultHeader(entry.getKey(), entry.getValue());
    }
    WebClient client = clientBuilder.build();

    ClientResponse clientResponse = client.get().exchange().block();
    if (clientResponse == null) fail();
    int responseStatus = clientResponse.statusCode().value();
    String responseBody = clientResponse.bodyToMono(String.class).block();
    return new Response(responseBody, responseStatus);
  }
}
