package org.hypertrace.agent.otel.extensions.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProtectionModeSamplerTest {

  @Test
  public void traceStateIsNotCore() {
    ProtectionModeSampler modeSampler = new ProtectionModeSampler(Arrays.asList("/foo/bar"));

    SamplingResult samplingResult = modeSampler
        .shouldSample(Context.root(), "dsadasd", "GET", SpanKind.SERVER, Attributes.of(
            SemanticAttributes.HTTP_URL, "http://unicorn.foo/foo/bar"), Collections.emptyList());

    TraceState traceState = samplingResult.getUpdatedTraceState(TraceState.getDefault());
    String hypertraceTraceState = traceState.get(ProtectionModeSampler.HYPERTRACE_TRACE_STATE_VENDOR);
    Assertions.assertEquals("isCore-false", hypertraceTraceState);
  }
}
