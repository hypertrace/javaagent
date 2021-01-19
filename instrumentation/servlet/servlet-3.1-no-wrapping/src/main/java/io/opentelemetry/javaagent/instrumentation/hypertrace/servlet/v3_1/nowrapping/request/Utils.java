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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.ByteBufferMetadata;
import java.io.UnsupportedEncodingException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferAndSpan;

public class Utils {

  private Utils() {}

  private static final Tracer TRACER =
      GlobalOpenTelemetry.get().getTracer("org.hypertrace.java.servletinputstream");

  public static void captureBody(ByteBufferMetadata metadata) {
    System.out.println("Capturing request body");
    Span span = metadata.span;
    String requestBody = null;
    try {
      requestBody = metadata.buffer.toStringWithSuppliedCharset();
    } catch (UnsupportedEncodingException e) {
      // ignore charset has been parsed before
    }
    if (span.isRecording()) {
      span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody);
    } else {
      TRACER
          .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
          .setParent(Context.root().with(span))
          .setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody)
          .startSpan()
          .end();
    }
  }

  public static void captureBody(CharBufferAndSpan charBufferAndSpan) {
    System.out.println("Capturing request body - BufferedReader");
    if (charBufferAndSpan.isBufferCaptured()) {
      return;
    }
    System.out.println(charBufferAndSpan);
    Span span = charBufferAndSpan.span;
    String requestBody = charBufferAndSpan.buffer.toString();

    charBufferAndSpan.setBufferCaptured(true);
    if (span.isRecording()) {
      span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody);
    } else {
      TRACER
          .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
          .setParent(Context.root().with(span))
          .setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody)
          .startSpan()
          .end();
    }
  }
}
