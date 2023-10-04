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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseBodyWrappingHandler implements Handler<Buffer> {

  private static final Tracer tracer =
      GlobalOpenTelemetry.getTracer("io.opentelemetry.javaagent.vertx-core-3.0");

  private static final Logger log = LoggerFactory.getLogger(ResponseBodyWrappingHandler.class);

  private static Method getAttribute = null;

  static {
    try {
      getAttribute = Class.forName("io.opentelemetry.sdk.trace.SdkSpan").getDeclaredMethod("getAttribute", AttributeKey.class);
    } catch (NoSuchMethodException e) {
      log.error("getAttribute method not found in SdkSpan class", e);
    } catch (ClassNotFoundException e) {
      log.error("SdkSpan class not found", e);
    }
    if (getAttribute != null) {
      getAttribute.setAccessible(true);
    }
  }

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
      SpanBuilder spanBuilder =
          tracer
              .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
              .setParent(Context.root().with(span))
              .setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, responseBody);

      // Also add content type if present
      if (getAttribute != null && span.getClass().getName().equals("io.opentelemetry.sdk.trace.SdkSpan")) {
        try {
          Object resContentType =
              getAttribute.invoke(
                  span, HypertraceSemanticAttributes.HTTP_RESPONSE_HEADER_CONTENT_TYPE);
          if (resContentType != null) {
            spanBuilder.setAttribute("http.response.header.content-type", (String) resContentType);
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
          // ignore and continue
          log.debug("Could not invoke getAttribute on SdkSpan", e);
        }
      }

      spanBuilder.startSpan().end();
    }

    wrapped.handle(event);
  }
}
