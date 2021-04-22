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

import static io.opentelemetry.javaagent.instrumentation.vertx.client.VertxClientTracer.tracer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class ResponseBodyWrappingHandler implements Handler<Buffer> {

  private static final Tracer tracer =
      GlobalOpenTelemetry.getTracer("io.opentelemetry.javaagent.vertx-core-3.0");

  private final Handler<Buffer> wrapped;
  private final Span span;

  public ResponseBodyWrappingHandler(Handler<Buffer> wrapped, Span span) {
    this.wrapped = wrapped;
    this.span = span;
  }

  @Override
  public void handle(Buffer event) {
    String responseBody = event.getString(0, event.length());
    if (span.isRecording()) {
      span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, responseBody);
    } else {
      tracer
          .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
          .setParent(Context.root().with(span))
          .setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, responseBody)
          .startSpan()
          .end();
    }

    System.out.println("response body");
    System.out.println(responseBody);
    wrapped.handle(event);
  }
}
