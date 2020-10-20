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

package io.opentelemetry.instrymentation.hypertrace.servlet.v2_3;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.Sets;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.config.ConfigInitializer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;

public abstract class AbstractAgentTest {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(AbstractAgentTest.class);

  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final InMemoryExporter TEST_WRITER;

  protected static final Set<String> TRANSFORMED_CLASSES_NAMES = Sets.newConcurrentHashSet();
  protected static final Set<TypeDescription> TRANSFORMED_CLASSES_TYPES =
      Sets.newConcurrentHashSet();
  private static final AtomicInteger INSTRUMENTATION_ERROR_COUNT = new AtomicInteger(0);

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
    if (OpenTelemetry.getPropagators()
        .getTextMapPropagator()
        .getClass()
        .getSimpleName()
        .equals("NoopTextMapPropagator")) {
      OpenTelemetry.setPropagators(
          DefaultContextPropagators.builder()
              .addTextMapPropagator(HttpTraceContext.getInstance())
              .build());
    }
    OpenTelemetrySdk.getTracerManagement().addSpanProcessor(TEST_WRITER);
    TEST_TRACER = OpenTelemetry.getTracer("io.opentelemetry.auto");
  }

  private static ClassFileTransformer classFileTransformer;

  @BeforeAll
  public static void beforeAll() {
    if (classFileTransformer == null) {
      classFileTransformer = AgentInstaller.installBytebuddyAgent(INSTRUMENTATION, false);
    }
  }

  /**
   * Invoked when Bytebuddy encounters an instrumentation error. Fails the test by default.
   *
   * <p>Override to skip specific expected errors.
   *
   * @return true if the test should fail because of this error.
   */
  protected boolean onInstrumentationError(
      String typeName,
      ClassLoader classLoader,
      JavaModule module,
      boolean loaded,
      Throwable throwable) {
    log.error(
        "Unexpected instrumentation error when instrumenting {} on {}",
        typeName,
        classLoader,
        throwable);
    return true;
  }

  public static class TestRunnerListener implements AgentBuilder.Listener {
    private static final List<AbstractAgentTest> activeTests = new CopyOnWriteArrayList<>();

    public void activateTest(AbstractAgentTest testRunner) {
      activeTests.add(testRunner);
    }

    public void deactivateTest(AbstractAgentTest testRunner) {
      activeTests.remove(testRunner);
    }

    @Override
    public void onDiscovery(
        String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
      //      for (AbstractAgentTest testRunner : activeTests) {
      //        if (!testRunner.shouldTransformClass(typeName, classLoader)) {
      //          throw new AbortTransformationException(
      //              "Aborting transform for class name = " + typeName + ", loader = " +
      // classLoader);
      //        }
      //      }
    }

    @Override
    public void onTransformation(
        TypeDescription typeDescription,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded,
        DynamicType dynamicType) {
      TRANSFORMED_CLASSES_NAMES.add(typeDescription.getActualName());
      TRANSFORMED_CLASSES_TYPES.add(typeDescription);
    }

    @Override
    public void onIgnored(
        TypeDescription typeDescription,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded) {}

    @Override
    public void onError(
        String typeName,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded,
        Throwable throwable) {
      if (!(throwable instanceof AbortTransformationException)) {
        for (AbstractAgentTest testRunner : activeTests) {
          if (testRunner.onInstrumentationError(typeName, classLoader, module, loaded, throwable)) {
            INSTRUMENTATION_ERROR_COUNT.incrementAndGet();
            break;
          }
        }
      }
    }

    @Override
    public void onComplete(
        String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}

    /** Used to signal that a transformation was intentionally aborted and is not an error. */
    public static class AbortTransformationException extends RuntimeException {
      public AbortTransformationException() {
        super();
      }

      public AbortTransformationException(String message) {
        super(message);
      }
    }
  }
}
