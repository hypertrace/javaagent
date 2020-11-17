/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.gradle;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * {@link Plugin} to initialize projects that implement auto instrumentation using bytecode
 * manipulation. Currently builds the special bootstrap classpath that is needed by bytecode tests.
 */
// TODO(anuraaga): Migrate more build logic into this plugin to avoid having two places for it.
public class AutoInstrumentationPlugin implements Plugin<Project> {

  /**
   * An exact copy of {@code
   * io.opentelemetry.javaagent.tooling.Constants#BOOTSTRAP_PACKAGE_PREFIXES}. We can't reference it
   * directly since this file needs to be compiled before the other packages.
   */
  public static final String[] BOOTSTRAP_PACKAGE_PREFIXES_COPY = {
    "io.opentelemetry.javaagent.common.exec",
    "io.opentelemetry.javaagent.slf4j",
    "io.opentelemetry.javaagent.bootstrap",
    "io.opentelemetry.javaagent.shaded",
    "io.opentelemetry.javaagent.instrumentation.api",
  };

  // Aditional classes we need only for tests and aren't shared with the agent business logic.
  private static final String[] TEST_BOOTSTRAP_PREFIXES;

  static {
    String[] testBS = {
      "io.opentelemetry.instrumentation.api",
      "io.opentelemetry.api", // OpenTelemetry API
      "io.opentelemetry.context", // OpenTelemetry API
      "org.slf4j",
      "ch.qos.logback",
      // Tomcat's servlet classes must be on boostrap
      // when running tomcat test
      "javax.servlet.ServletContainerInitializer",
      "javax.servlet.ServletContext"
    };
    TEST_BOOTSTRAP_PREFIXES =
        Arrays.copyOf(
            BOOTSTRAP_PACKAGE_PREFIXES_COPY,
            BOOTSTRAP_PACKAGE_PREFIXES_COPY.length + testBS.length);
    System.arraycopy(
        testBS, 0, TEST_BOOTSTRAP_PREFIXES, BOOTSTRAP_PACKAGE_PREFIXES_COPY.length, testBS.length);
    for (int i = 0; i < TEST_BOOTSTRAP_PREFIXES.length; i++) {
      TEST_BOOTSTRAP_PREFIXES[i] = TEST_BOOTSTRAP_PREFIXES[i].replace('.', '/');
    }
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);

    project
        .getTasks()
        .withType(
            Test.class,
            task -> {
              TaskProvider<Jar> bootstrapJar =
                  project.getTasks().register(task.getName() + "BootstrapJar", Jar.class);

              Configuration testClasspath =
                  project.getConfigurations().findByName(task.getName() + "RuntimeClasspath");
              if (testClasspath == null) {
                // Same classpath as default test task
                testClasspath =
                    project
                        .getConfigurations()
                        .findByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
              }

              String bootstrapJarName = task.getName() + "-bootstrap.jar";

              Configuration testClasspath0 = testClasspath;
              bootstrapJar.configure(
                  jar -> {
                    jar.dependsOn(testClasspath0.getBuildDependencies());
                    jar.getArchiveFileName().set(bootstrapJarName);
                    jar.setIncludeEmptyDirs(false);
                    // Classpath is ordered in priority, but later writes into the JAR would take
                    // priority, so we exclude the later ones (we need this to make sure logback is
                    // picked up).
                    jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
                    jar.from(
                        project.files(
                            // Needs to be a Callable so it's executed lazily at runtime, instead of
                            // configuration time where the classpath may still be getting built up.
                            (Callable<?>)
                                () ->
                                    testClasspath0.resolve().stream()
                                        .filter(
                                            file ->
                                                !file.isDirectory()
                                                    && file.getName().endsWith(".jar"))
                                        .map(project::zipTree)
                                        .collect(toList())));

                    jar.eachFile(
                        file -> {
                          if (!isBootstrapClass(file.getPath())) {
                            file.exclude();
                          }
                        });
                  });

              task.dependsOn(bootstrapJar);
              task.getJvmArgumentProviders()
                  .add(
                      new InstrumentationTestArgs(
                          new File(project.getBuildDir(), "libs/" + bootstrapJarName)));
            });
  }

  private static class InstrumentationTestArgs implements CommandLineArgumentProvider {
    private final File bootstrapJar;

    @Internal
    public File getBootstrapJar() {
      return bootstrapJar;
    }

    public InstrumentationTestArgs(File bootstrapJar) {
      this.bootstrapJar = bootstrapJar;
    }

    @Override
    public Iterable<String> asArguments() {
      return Arrays.asList(
          "-Xbootclasspath/a:" + bootstrapJar.getAbsolutePath(), "-Dnet.bytebuddy.raw=true");
    }
  }

  private static boolean isBootstrapClass(String filePath) {
    for (String testBootstrapPrefix : TEST_BOOTSTRAP_PREFIXES) {
      if (filePath.startsWith(testBootstrapPrefix)) {
        return true;
      }
    }
    return false;
  }
}
