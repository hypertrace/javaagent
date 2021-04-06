package org.hypertrace.agent.otel.extensions.sampler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.internal.SystemClock;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/**
 * {@link RateLimitingSampler} sampler uses a leaky bucket rate limiter to ensure that traces are
 * sampled with a certain constant rate.
 */
class RateLimitingSampler implements Sampler {
  static final String TYPE = "ratelimiting";
  static final AttributeKey<String> SAMPLER_TYPE = AttributeKey.stringKey("sampler.type");
  static final AttributeKey<Double> SAMPLER_PARAM = AttributeKey.doubleKey("sampler.param");

  private final double maxTracesPerSecond;
  private final RateLimiter rateLimiter;
  private final SamplingResult onSamplingResult;
  private final SamplingResult offSamplingResult;

  /**
   * Creates rate limiting sampler.
   *
   * @param maxTracesPerSecond the maximum number of sampled traces per second.
   */
  RateLimitingSampler(int maxTracesPerSecond) {
    this.maxTracesPerSecond = maxTracesPerSecond;
    double maxBalance = maxTracesPerSecond < 1.0 ? 1.0 : maxTracesPerSecond;
    this.rateLimiter = new RateLimiter(maxTracesPerSecond, maxBalance, SystemClock.getInstance());
    Attributes attributes =
        Attributes.of(SAMPLER_TYPE, TYPE, SAMPLER_PARAM, (double) maxTracesPerSecond);
    this.onSamplingResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, attributes);
    this.offSamplingResult = SamplingResult.create(SamplingDecision.DROP, attributes);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    return this.rateLimiter.trySpend(1.0) ? onSamplingResult : offSamplingResult;
  }

  @Override
  public String getDescription() {
    return String.format("RateLimitingSampler{%.2f}", maxTracesPerSecond);
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
