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
import io.opentelemetry.javaagent.OpenTelemetryAgent;
import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.bootstrap.JavaagentFileHolder;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
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
    System.setProperty("otel.traces.exporter", "none");
    System.setProperty("otel.metrics.exporter", "none");

    //    System.setProperty("otel.instrumentation.common.default-enabled", "false");
    //
    //    System.setProperty("otel.instrumentation.netty.enabled", "true");
    //    System.setProperty("otel.instrumentation.servlet.enabled", "true");
    //    System.setProperty("otel.instrumentation.vertx-web.enabled", "true");
    //    System.setProperty("otel.instrumentation.undertow.enabled", "true");
    //    System.setProperty("otel.instrumentation.grpc.enabled", "true");
    //
    //    System.setProperty("otel.instrumentation.apache-httpasyncclient.enabled", "true");
    //    System.setProperty("otel.instrumentation.apache-httpclient.enabled", "true");
    //    System.setProperty("otel.instrumentation.okhttp.enabled", "true");
    //    System.setProperty("otel.instrumentation.http-url-connection.enabled", "true");
    //    System.setProperty("otel.instrumentation.vertx.enabled", "true");
    //
    //    System.setProperty("otel.instrumentation.inputstream.enabled", "true");
    //    System.setProperty("otel.instrumentation.outputstream.enabled", "true");
    //    System.setProperty("otel.instrumentation.ht.enabled", "true");
    //
    //    System.setProperty("otel.instrumentation.methods.enabled", "true");
    //    System.setProperty("otel.instrumentation.external-annotations.enabled", "true");
    //    System.setProperty("otel.instrumentation.opentelemetry-extension-annotations.enabled",
    // "true");
    //    System.setProperty(
    //        "otel.instrumentation.opentelemetry-instrumentation-annotations.enabled", "true");
    //    System.setProperty("otel.instrumentation.opentelemetry-api.enabled", "true");

    INSTRUMENTATION = ByteBuddyAgent.install();
    InstrumentationHolder.setInstrumentation(INSTRUMENTATION);

    // TODO causes Caused by: java.lang.ClassCastException
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("io.opentelemetry")).setLevel(Level.DEBUG);
  }

  private static boolean INSTRUMENTED = false;
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
//      Thread.currentThread().setContextClassLoader(null);

//      File javaagentFile = installBootstrapJar(INSTRUMENTATION);
//      InstrumentationHolder.setInstrumentation(INSTRUMENTATION);
//      JavaagentFileHolder.setJavaagentFile(javaagentFile);
//      Class<?> agentInitializerClass =
//          ClassLoader.getSystemClassLoader()
//              .loadClass("io.opentelemetry.javaagent.bootstrap.AgentInitializer");

      Class<?> agentInitialiserClass = Class.forName("io.opentelemetry.javaagent.bootstrap.AgentInitializer");


      ClassLoader agentInitializerClassLoader = agentInitialiserClass.getClassLoader();
      Field agentClassLoaderField = agentInitialiserClass.getDeclaredField("agentClassLoader");
      agentClassLoaderField.setAccessible(true);
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      agentClassLoaderField.set(null, systemClassLoader);
      Field agentStarterField = agentInitialiserClass.getDeclaredField("agentStarter");
      agentStarterField.setAccessible(true);
      agentStarterField.set(null, testAgentStarter);
      Class<?> autoConfiguredOpenTelemetrySdkClass =
              ClassLoader.getSystemClassLoader()
                      .loadClass("io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk");
//      AgentInitializer.initialize(INSTRUMENTATION, javaagentFile, false);
      ClassLoader autoConfiguredOpenTelemetrySdkClassClassLoader = autoConfiguredOpenTelemetrySdkClass.getClassLoader();
      Class<?> autoConfiguredOpenTelemetrySdkClass1 = Class.forName("io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk");
      ClassLoader autoConfiguredOpenTelemetrySdkClassClassLoader1 = autoConfiguredOpenTelemetrySdkClass1.getClassLoader();
      System.out.println(autoConfiguredOpenTelemetrySdkClassClassLoader1);
    } catch (Throwable t) {
      throw new AssertionError("Could not access agent classLoader", t);
    }
    if (!INSTRUMENTED) {
      // ---------------------------------------------------------------------
      //  Set the otel.instrumentation.internal-reflection.enabled property to
      //  'false'.  This causes the OTEL agent to filter out virtual field
      //  accessor interfaces when it is verifying that a class can be
      //  transformed.  Some of our unit tests will fail if this property
      //  is not set.
      // ---------------------------------------------------------------------
      System.setProperty("otel.instrumentation.internal-reflection.enabled", "false");
      AgentInstaller.installBytebuddyAgent(
          INSTRUMENTATION,
          ClassLoader.getSystemClassLoader(),
          EarlyInitAgentConfig.create());
      INSTRUMENTED = true;
    }
    if (TEST_TRACER == null) {
      TEST_TRACER = GlobalOpenTelemetry.getTracer("io.opentelemetry.auto");
    }
  }

  private static synchronized File installBootstrapJar(Instrumentation inst)
      throws IOException, URISyntaxException {
    ClassLoader classLoader = OpenTelemetryAgent.class.getClassLoader();
    if (classLoader == null) {
      classLoader = ClassLoader.getSystemClassLoader();
    }

    URL url =
        classLoader.getResource(OpenTelemetryAgent.class.getName().replace('.', '/') + ".class");
    if (url != null && "jar".equals(url.getProtocol())) {
      String resourcePath = url.toURI().getSchemeSpecificPart();
      int protocolSeparatorIndex = resourcePath.indexOf(":");
      int resourceSeparatorIndex = resourcePath.indexOf("!/");
      if (protocolSeparatorIndex != -1 && resourceSeparatorIndex != -1) {
        String agentPath =
            resourcePath.substring(protocolSeparatorIndex + 1, resourceSeparatorIndex);
        File javaagentFile = new File(agentPath);
        if (!javaagentFile.isFile()) {
          throw new IllegalStateException(
              "agent jar location doesn't appear to be a file: " + javaagentFile.getAbsolutePath());
        } else {
          JarFile agentJar = new JarFile(javaagentFile, false);
          //          verifyJarManifestMainClassIsThis(javaagentFile, agentJar);
          inst.appendToBootstrapClassLoaderSearch(agentJar);
          return javaagentFile;
        }
      } else {
        throw new IllegalStateException("could not get agent location from url " + url);
      }
    } else {
      throw new IllegalStateException("could not get agent jar location from url " + url);
    }
  }

  //  private static void verifyJarManifestMainClassIsThis(File jarFile, JarFile agentJar) throws
  // IOException {
  //    Manifest manifest = agentJar.getManifest();
  //    if (manifest.getMainAttributes().getValue("Premain-Class") == null) {
  //      throw new IllegalStateException("The agent was not installed, because the agent was found
  // in '" + jarFile + "', which doesn't contain a Premain-Class manifest attribute. Make sure that
  // you haven't included the agent jar file inside of an application uber jar.");
  //    }
  //  }

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

  public static RequestBody blockedRequestBody(
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
        sink.write("{\"block=true\":\"".getBytes());
        for (int count = 0; count < size; count += writeSize) {
          sink.write(buffer, 0, (int) Math.min(size - count, writeSize));
        }
        sink.write("\"}".getBytes());
      }
    };
  }
}
