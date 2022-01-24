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
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import net.bytebuddy.agent.ByteBuddyAgent;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

/**
 * Abstract test class that tests {@link
 * io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule} on the classpath.
 */
public abstract class AbstractInstrumenterTest {

  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(AbstractInstrumenterTest.class);

  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final InMemoryExporter TEST_WRITER = new InMemoryExporter();;

  protected static Tracer TEST_TRACER;
  private static final Instrumentation INSTRUMENTATION;

  static {
    // always run with the thread propagation debugger to help track down sporadic test failures
    System.setProperty("io.opentelemetry.context.contextStorageProvider", "default");
    System.setProperty("otel.threadPropagationDebugger", "true");
    System.setProperty("otel.internal.failOnContextLeak", "true");
    System.setProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.log.muzzleMatcher", "warn");

    INSTRUMENTATION = ByteBuddyAgent.install();
    InstrumentationHolder.setInstrumentation(INSTRUMENTATION);

    // TODO causes Caused by: java.lang.ClassCastException
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("io.opentelemetry")).setLevel(Level.DEBUG);
  }

  private static ClassFileTransformer classFileTransformer;

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;
  protected OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .callTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .build();

  @BeforeAll
  public static void beforeAll() {
    /*
     * OpenTelemetry moved the initialization of some agent primitives to the OTEL class
     * OpenTelemetryAgent which does not get used in the scope of these tests. To remove this
     * workaround, we should adopt their testing pattern leveraging the agent-for-teseting artifact
     * and the AgentInstrumentationExtension for JUnit.
     */
    TestAgentStarter testAgentStarter = new TestAgentStarter();
    try {
      Class<?> agentInitializerClass =
          ClassLoader.getSystemClassLoader()
              .loadClass("io.opentelemetry.javaagent.bootstrap.AgentInitializer");
      Field agentClassLoaderField = agentInitializerClass.getDeclaredField("agentClassLoader");
      agentClassLoaderField.setAccessible(true);
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      agentClassLoaderField.set(null, systemClassLoader);
      Field agentStarterField = agentInitializerClass.getDeclaredField("agentStarter");
      agentStarterField.setAccessible(true);
      agentStarterField.set(null, testAgentStarter);
    } catch (Throwable t) {
      throw new AssertionError("Could not access agent classLoader", t);
    }
    if (classFileTransformer == null) {
      classFileTransformer =
          AgentInstaller.installBytebuddyAgent(INSTRUMENTATION, Collections.emptyList());
    }
    if (TEST_TRACER == null) {
      TEST_TRACER = GlobalOpenTelemetry.getTracer("io.opentelemetry.auto");
    }
  }

  @BeforeEach
  public void beforeEach() {
    TEST_WRITER.clear();
  }

  public static RequestBody requestBody(
      final boolean chunked, final long size, final int writeSize) {
    final byte[] buffer = new byte[writeSize];
    Arrays.fill(buffer, (byte) 'x');

    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return MediaType.get("application/json; charset=utf-8");
      }

      @Override
      public long contentLength() throws IOException {
        return chunked ? -1L : size;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        sink.write("{\"body\":\"".getBytes());
        for (int count = 0; count < size; count += writeSize) {
          sink.write(buffer, 0, (int) Math.min(size - count, writeSize));
        }
        sink.write("\"}".getBytes());
      }
    };
  }
}
