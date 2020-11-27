package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import io.opentelemetry.api.trace.Span;
import java.util.WeakHashMap;

public class ApacheHttpClientObjectRegistry {

  private ApacheHttpClientObjectRegistry() {}

  public static final WeakHashMap<Object, Span> objectToSpanMap = new WeakHashMap<>();

}
