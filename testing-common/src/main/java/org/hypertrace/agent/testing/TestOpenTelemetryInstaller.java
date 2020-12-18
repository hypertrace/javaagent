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

package org.hypertrace.agent.testing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.javaagent.tooling.OpenTelemetryInstaller;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class TestOpenTelemetryInstaller extends OpenTelemetryInstaller {

  private final SpanProcessor spanProcessor;

  public TestOpenTelemetryInstaller(SpanProcessor spanProcessor) {
    this.spanProcessor = spanProcessor;
  }

  @Override
  public void afterByteBuddyAgent() {
    // TODO this is probably temporary until default propagators are supplied by SDK
    //  https://github.com/open-telemetry/opentelemetry-java/issues/1742
    //  currently checking against no-op implementation so that it won't override aws-lambda
    //  propagator configuration
    if (OpenTelemetry.getGlobalPropagators()
        .getTextMapPropagator()
        .getClass()
        .getSimpleName()
        .equals("NoopTextMapPropagator")) {
      // Workaround https://github.com/open-telemetry/opentelemetry-java/pull/2096
      OpenTelemetry.setGlobalPropagators(
          ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
    }
    OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(spanProcessor);
  }
}
