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
import org.hypertrace.agent.core.propagation.HypertraceTracestate;
import org.hypertrace.agent.core.propagation.HypertraceTracestate.CaptureMode;

public class AdditionalCaptureSampler implements Sampler {

  private static final HypertraceSamplingResult allSamplingResult =
      new HypertraceSamplingResult(CaptureMode.ALL);
  private static final HypertraceSamplingResult defaultSamplingResult =
      new HypertraceSamplingResult(CaptureMode.DEFAULT);

  private List<SampledEndpoint> sampledEndpoints;

  public AdditionalCaptureSampler(List<String> urlPatterns) {
    sampledEndpoints = new ArrayList<>(urlPatterns.size());
    for (String pattern : urlPatterns) {
      sampledEndpoints.add(
          new SampledEndpoint(new RateLimitingSampler(1), Pattern.compile(pattern)));
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

    CaptureMode captureMode = HypertraceTracestate.getCaptureMode(spanContext.getTraceState());
    if (captureMode == CaptureMode.ALL) {
      // core mode already defer to the default sampler
      return allSamplingResult;
    } else if (captureMode == CaptureMode.DEFAULT) {
      return defaultSamplingResult;
    }

    String urlAttr = attributes.get(SemanticAttributes.HTTP_URL);
    if (urlAttr != null && !urlAttr.isEmpty()) {
      String path;
      try {
        URL url = new URL(urlAttr);
        path = url.getPath();
      } catch (MalformedURLException e) {
        return defaultSamplingResult;
      }

      if (path == null || path.isEmpty()) {
        return allSamplingResult;
      }

      for (SampledEndpoint sampledEndpoint : this.sampledEndpoints) {
        if (sampledEndpoint.pattern.matcher(path).matches()) {
          SamplingResult samplingResult =
              sampledEndpoint.sampler.shouldSample(
                  parentContext, traceId, name, spanKind, attributes, parentLinks);
          if (samplingResult.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
            // set the advanced mode - record all
            return allSamplingResult;
          } else {
            // set the core mode
            return defaultSamplingResult;
          }
        }
      }
    }
    // default use the advanced mode
    return allSamplingResult;
  }

  @Override
  public String getDescription() {
    return "Samples additional";
  }

  static class HypertraceSamplingResult implements SamplingResult {

    final CaptureMode captureMode;

    public HypertraceSamplingResult(CaptureMode captureMode) {
      this.captureMode = captureMode;
    }

    @Override
    public SamplingDecision getDecision() {
      return SamplingDecision.RECORD_AND_SAMPLE;
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.empty();
    }

    @Override
    public TraceState getUpdatedTraceState(TraceState parentTraceState) {
      return HypertraceTracestate.create(parentTraceState, captureMode);
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
