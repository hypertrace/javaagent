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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletInputStream;
import org.hypertrace.agent.core.instrumentation.GlobalObjectRegistry;
import org.hypertrace.agent.core.instrumentation.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class ServletInputStreamUtils {

  private ServletInputStreamUtils() {}

  private static final Tracer TRACER =
      GlobalOpenTelemetry.get().getTracer("org.hypertrace.java.servletinputstream");

  public static void captureBody(Metadata metadata) {
    Span span = metadata.span;
    String requestBody = null;
    try {
      requestBody = metadata.boundedByteArrayOutputStream.toStringWithSuppliedCharset();
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

  public static SpanAndBuffer check(ServletInputStream inputStream) {
    SpanAndBuffer spanAndBuffer =
        GlobalObjectRegistry.inputStreamToSpanAndBufferMap.get(inputStream);
    if (spanAndBuffer == null) {
      return null;
    }

    int callDepth = CallDepthThreadLocalMap.incrementCallDepth(InputStream.class);
    if (callDepth > 0) {
      return null;
    }
    return spanAndBuffer;
  }
}
