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

public class VertxWebServer extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {
    int port = config().getInteger(VertxBodyInstrumentationModuleTest.CONFIG_HTTP_SERVER_PORT);
    Router router = Router.router(vertx);

    router
        .route("/success")
        .handler(
            ctx ->
                ctx.request()
                    .bodyHandler(
                        h -> {
                          System.out.printf("vertx received: %s\n", new String(h.getBytes()));
                          // TODO test chunked
                          //                          ctx.response().setChunked(true);
                          ////                          ctx.response().setWriteQueueMaxSize()
                          //                          ctx.response().write("chunk1");
                          //                          ctx.response().write("chunk2");
                          ctx.response().setStatusCode(200).end("success");
                        }));
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(port, it -> startFuture.complete());
  }
}
