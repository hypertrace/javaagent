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

    SamplingResult samplingResult =
        modeSampler.shouldSample(
            Context.root(),
            "dsadasd",
            "GET",
            SpanKind.SERVER,
            Attributes.of(SemanticAttributes.HTTP_URL, "http://unicorn.foo/foo/bar"),
            Collections.emptyList());

    TraceState traceState = samplingResult.getUpdatedTraceState(TraceState.getDefault());
    String hypertraceTraceState =
        traceState.get(ProtectionModeSampler.HYPERTRACE_TRACE_STATE_VENDOR);
    Assertions.assertEquals("isCore-false", hypertraceTraceState);
  }
}
