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

package io.opentelemetry.javaagent.instrumentation.hypertrace.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Assertions;

public class VertxWebServer extends AbstractVerticle {

  private static final String CHUNK1 = "chunk1";
  private static final String CHUNK2 = "chunk2";
  private static final String CHUNK3 = "chunk3";
  public static final String RESPONSE_BODY = CHUNK1 + CHUNK2 + CHUNK3;

  public static final String RESPONSE_HEADER_NAME = "vertxheader";
  public static final String RESPONSE_HEADER_VALUE = "vertxheader";

  @Override
  public void start(Future<Void> startFuture) {
    int port = config().getInteger(VertxInstrumentationTest.CONFIG_HTTP_SERVER_PORT);
    Router router = Router.router(vertx);

    router
        .route("/return_chunked")
        .handler(
            ctx ->
                ctx.request()
                    .bodyHandler(
                        h -> {
                          Assertions.assertEquals(
                              VertxInstrumentationTest.REQUEST_BODY, new String(h.getBytes()));
                          ctx.response()
                              .putHeader("content-Type", "application/json; charset=utf-8");
                          ctx.response().putHeader(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
                          ctx.response().setChunked(true);
                          ctx.response().write(CHUNK1);
                          ctx.response().write(CHUNK2);
                          ctx.response().setStatusCode(200).end(CHUNK3);
                        }));

    router
        .route("/return_no_chunked")
        .handler(
            ctx ->
                ctx.request()
                    .bodyHandler(
                        h -> {
                          Assertions.assertEquals(
                              VertxInstrumentationTest.REQUEST_BODY, new String(h.getBytes()));
                          ctx.response()
                              .putHeader("content-Type", "application/json; charset=utf-8");
                          ctx.response().putHeader(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
                          ctx.response().setStatusCode(200).end(RESPONSE_BODY);
                        }));

    router
        .route("/get")
        .handler(
            ctx -> {
              ctx.response().setStatusCode(200);
              ctx.response().putHeader(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
              ctx.response().end();
            });

    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(port, it -> startFuture.complete());
  }
}
