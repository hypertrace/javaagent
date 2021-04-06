package org.hypertrace.agent.otel.extensions.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProtectionModeSampler implements Sampler {

  static final String HYPERTRACE_TRACE_STATE_VENDOR = "hypertrace";
  private final SamplingResult onSamplingResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty());

  private List<SampledEndpoint> sampledEndpoints;

  public ProtectionModeSampler(List<String> urlPatterns) {
    sampledEndpoints = new ArrayList<>(urlPatterns.size());
    for (String pattern: urlPatterns) {
      sampledEndpoints.add(new SampledEndpoint(new RateLimitingSampler(1), Pattern.compile(pattern)));
    }
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    Span span = Span.fromContext(parentContext);
    SpanContext spanContext = span.getSpanContext();

    String htTraceState = spanContext.getTraceState().get(HYPERTRACE_TRACE_STATE_VENDOR);
    if ("isCore-true".equals(htTraceState)) {
      // core mode already defer to the default sampler
      return onSamplingResult;
    }

    // sampling didn't happen - need to sample
    if (htTraceState == null) {
      String urlAttr = attributes.get(SemanticAttributes.HTTP_URL);
      if (urlAttr != null && !urlAttr.isEmpty()) {

        String path = "";
        try {
          URL url = new URL(urlAttr);
          path = url.getPath();
        } catch (MalformedURLException e) {
          return new HypertraceSamplingResult(SamplingDecision.RECORD_AND_SAMPLE, false);
        }

        if (path == null || path.isEmpty()) {
          return new HypertraceSamplingResult(SamplingDecision.RECORD_AND_SAMPLE, false);
        }

        for (SampledEndpoint sampledEndpoint: this.sampledEndpoints) {
          if (sampledEndpoint.pattern.matcher(path).matches()) {
            SamplingResult samplingResult = sampledEndpoint.sampler
                .shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
            if (samplingResult.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
              // set the advanced mode - record all
              return new HypertraceSamplingResult(SamplingDecision.RECORD_AND_SAMPLE, false);
            } else {
              // set the core mode
              return new HypertraceSamplingResult(SamplingDecision.RECORD_AND_SAMPLE, true);
            }
          }
        }
      }
    }
    // default use the advanced mode
    return new HypertraceSamplingResult(SamplingDecision.RECORD_AND_SAMPLE, false);
  }

  @Override
  public String getDescription() {
    return "Samples additional";
  }

  static class HypertraceSamplingResult implements SamplingResult {

    final SamplingDecision samplingDecision;
    final boolean isCoreMode;

    public HypertraceSamplingResult(
        SamplingDecision samplingDecision,
        boolean coreMode
    ) {
      this.samplingDecision = samplingDecision;
      this.isCoreMode = coreMode;
    }

    @Override
    public SamplingDecision getDecision() {
      return samplingDecision;
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.empty();
    }

    @Override
    public TraceState getUpdatedTraceState(TraceState parentTraceState) {
      return parentTraceState.toBuilder()
          .put(HYPERTRACE_TRACE_STATE_VENDOR, String.format("isCore-%s", isCoreMode))
          .build();
    }
  }

  static class SampledEndpoint {
    private final RateLimitingSampler sampler;
    private final Pattern pattern;

    public SampledEndpoint(RateLimitingSampler sampler, Pattern pattern) {
      this.sampler = sampler;
      this.pattern = pattern;
    }
  }
}

