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

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.tooling.OpenTelemetryInstaller;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.lang.reflect.Field;

public class TestOpenTelemetryInstaller extends OpenTelemetryInstaller {

  private final SpanProcessor spanProcessor;

  public TestOpenTelemetryInstaller(SpanProcessor spanProcessor) {
    this.spanProcessor = spanProcessor;
  }

  @Override
  public void beforeAgent(Config config) {
    OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build())
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();

    /*
     * OpenTelemetry moved the initialization of some agent primitives to the OTEL class
     * OpenTelemetryAgent which does not get used in the scope of these tests. To remove this
     * workaround, we should adopt their testing pattern leveraging the agent-for-teseting artifact
     * and the AgentInstrumentationExtension for JUnit.
     */
    try {
      Class<?> agentInitializerClass =
          ClassLoader.getSystemClassLoader()
              .loadClass("io.opentelemetry.javaagent.bootstrap.AgentInitializer");
      Field agentClassLoaderField = agentInitializerClass.getDeclaredField("agentClassLoader");
      agentClassLoaderField.setAccessible(true);
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      agentClassLoaderField.set(null, systemClassLoader);
    } catch (Throwable t) {
      throw new AssertionError("Could not access agent classLoader", t);
    }
  }

  @Override
  public void afterAgent(Config config) {}
}
