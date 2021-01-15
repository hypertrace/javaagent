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
