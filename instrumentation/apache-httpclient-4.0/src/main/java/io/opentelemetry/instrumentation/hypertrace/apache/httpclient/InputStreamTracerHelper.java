package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public class InputStreamTracerHelper {

  private InputStreamTracerHelper() {}

  private static final Tracer TRACER = OpenTelemetry.getGlobalTracer("org.hypertrace.java.inputstream");

  /**
   * Adds an attribute to span. If the span is ended it adds the attributed to a newly created child.
   */
  public static void addAttribute(Span span, AttributeKey<String> attributeKey, String value) {
    System.out.printf("Captured body is %s\n", value);
    if (span.isRecording()) {
      span.setAttribute(attributeKey, value);
    } else {
      System.out.println("parent is");
      System.out.println(span);
      TRACER.spanBuilder("additional-data")
          .setParent(Context.current().with(span))
          .setAttribute(attributeKey, value)
          .startSpan()
          .end();
    }
  }
}
