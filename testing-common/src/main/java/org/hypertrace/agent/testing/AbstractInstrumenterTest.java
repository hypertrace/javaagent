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
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.config.ConfigInitializer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import net.bytebuddy.agent.ByteBuddyAgent;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

/**
 * Abstract test class that tests {@link io.opentelemetry.javaagent.tooling.InstrumentationModule}
 * on the classpath.
 */
public abstract class AbstractInstrumenterTest {

  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(AbstractInstrumenterTest.class);

  private static final ComponentInstaller COMPONENT_INSTALLER;

  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final InMemoryExporter TEST_WRITER = new InMemoryExporter();;

  protected static final Tracer TEST_TRACER;
  private static final Instrumentation INSTRUMENTATION;

  static {
    ConfigInitializer.initialize();
    // always run with the thread propagation debugger to help track down sporadic test failures
    System.setProperty("otel.threadPropagationDebugger", "true");
    System.setProperty("otel.internal.failOnContextLeak", "true");
    System.setProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.log.muzzleMatcher", "warn");

    INSTRUMENTATION = ByteBuddyAgent.install();

    // TODO causes Caused by: java.lang.ClassCastException
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("io.opentelemetry")).setLevel(Level.DEBUG);

    COMPONENT_INSTALLER = new TestOpenTelemetryInstaller(TEST_WRITER);
    OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(TEST_WRITER);
    TEST_TRACER = OpenTelemetry.getGlobalTracer("io.opentelemetry.auto");
  }

  private static ClassFileTransformer classFileTransformer;
  protected OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .callTimeout(30, TimeUnit.SECONDS)
          .connectTimeout(30, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build();

  @BeforeAll
  public static void beforeAll() {
    if (classFileTransformer == null) {
      classFileTransformer =
          AgentInstaller.installBytebuddyAgent(
              INSTRUMENTATION, true, Collections.singleton(COMPONENT_INSTALLER));
    }
  }

  @BeforeEach
  public void beforeEach() {
    TEST_WRITER.clear();
  }
}
