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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.HttpTraceContext;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.config.ConfigInitializer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.ByteBuddyAgent;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

/**
 * Abstract test class that tests {@link io.opentelemetry.javaagent.tooling.Instrumenter} on the
 * classpath.
 */
public abstract class AbstractInstrumenterTest {

  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final InMemoryExporter TEST_WRITER;

  protected static final Tracer TEST_TRACER;
  private static final Instrumentation INSTRUMENTATION;

  static {
    ConfigInitializer.initialize();
    // always run with the thread propagation debugger to help track down sporadic test failures
    System.setProperty("otel.threadPropagationDebugger", "true");
    System.setProperty("otel.internal.failOnContextLeak", "true");

    INSTRUMENTATION = ByteBuddyAgent.install();

    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("io.opentelemetry")).setLevel(Level.DEBUG);

    TEST_WRITER = new InMemoryExporter();

    // TODO this is probably temporary until default propagators are supplied by SDK
    //  https://github.com/open-telemetry/opentelemetry-java/issues/1742
    //  currently checking against no-op implementation so that it won't override aws-lambda
    //  propagator configuration
    if (OpenTelemetry.getGlobalPropagators()
        .getTextMapPropagator()
        .getClass()
        .getSimpleName()
        .equals("NoopTextMapPropagator")) {
      OpenTelemetry.setGlobalPropagators(
          DefaultContextPropagators.builder()
              .addTextMapPropagator(HttpTraceContext.getInstance())
              .build());
    }
    OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(TEST_WRITER);
    TEST_TRACER = OpenTelemetry.getGlobalTracer("io.opentelemetry.auto");
  }

  private static ClassFileTransformer classFileTransformer;
  protected OkHttpClient httpClient = new OkHttpClient.Builder().build();

  @BeforeAll
  public static void beforeAll() {
    if (classFileTransformer == null) {
      classFileTransformer = AgentInstaller.installBytebuddyAgent(INSTRUMENTATION, true);
    }
  }

  @BeforeEach
  public void beforeEach() {
    TEST_WRITER.clear();
  }
}
