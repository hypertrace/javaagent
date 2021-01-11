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

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
class SpringWebFluxTestApplication {

  static final String RESPONSE_HEADER_NAME = "resphadername";
  static final String RESPONSE_HEADER_VALUE = "respheaderval";

  @Bean
  RouterFunction<ServerResponse> echoRouterFunction(EchoHandler echoHandler) {
    return org.springframework.web.reactive.function.server.RouterFunctions.route(
        org.springframework.web.reactive.function.server.RequestPredicates.POST("/post"),
        new EchoHandlerFunction(echoHandler));
  }

  class EchoHandlerFunction implements HandlerFunction<ServerResponse> {
    private final EchoHandler echoHandler;

    EchoHandlerFunction(EchoHandler echoHandler) {
      this.echoHandler = echoHandler;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
      return echoHandler.echo(request);
    }
  }

  @Bean
  RouterFunction<ServerResponse> greetRouterFunction(GreetingHandler greetingHandler) {
    return org.springframework.web.reactive.function.server.RouterFunctions.route(
        org.springframework.web.reactive.function.server.RequestPredicates.GET("/get"),
        new HandlerFunction<ServerResponse>() {
          @Override
          public Mono<ServerResponse> handle(ServerRequest request) {
            return greetingHandler.defaultGreet();
          }
        });
  }

  @Bean
  RouterFunction<ServerResponse> finiteStream() {
    return org.springframework.web.reactive.function.server.RouterFunctions.route(
        org.springframework.web.reactive.function.server.RequestPredicates.GET("/stream"),
        new HandlerFunction<ServerResponse>() {
          @Override
          public Mono<ServerResponse> handle(ServerRequest request) {
            return finiteStream(request);
          }
        });
  }

  public Mono<ServerResponse> finiteStream(ServerRequest req) {
    String[] array = {"a", "b", "c", "d", "e"};

    // Arrays.stream
    Stream<String> stream = Arrays.stream(array);

    Flux<FooModel> mapFlux =
        Flux.fromStream(stream)
            .zipWith(Flux.interval(Duration.ofSeconds(1)))
            .map(
                i -> {
                  return new FooModel(i.getT1(), "name");
                });

    return ok().contentType(MediaType.APPLICATION_STREAM_JSON).body(mapFlux, FooModel.class);
  }

  @Component
  static class GreetingHandler {
    static final String DEFAULT_RESPONSE = "HELLO";

    Mono<ServerResponse> defaultGreet() {
      return ServerResponse.ok()
          .contentType(MediaType.TEXT_PLAIN)
          .header(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE)
          .body(BodyInserters.fromObject(DEFAULT_RESPONSE));
    }
  }
}
