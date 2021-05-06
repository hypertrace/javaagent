/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.gradle;

import java.io.File;
import java.util.Arrays;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * {@link Plugin} to initialize projects that implement auto instrumentation using bytecode
 * manipulation. Currently builds the special bootstrap classpath that is needed by bytecode tests.
 */
// TODO(anuraaga): Migrate more build logic into this plugin to avoid having two places for it.
public class AutoInstrumentationPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);
    createLibraryConfiguration(project);
    project
        .getTasks()
        .withType(
            Test.class,
            task -> {
              task.dependsOn(":testing-bootstrap:shadowJar");
              File testingBootstrapJar =
                  new File(
                      project.project(":testing-bootstrap").getBuildDir(),
                      "libs/testing-bootstrap.jar");
              // Make sure tests get rerun if the contents of the testing-bootstrap.jar change
              task.getInputs().property("testing-bootstrap-jar", testingBootstrapJar);
              task.getJvmArgumentProviders().add(new InstrumentationTestArgs(testingBootstrapJar));
            });
  }

  /**
   * Creates a custom dependency configuration called {@code library} inspired by the <a
   * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/0b0516bd1a0599c8f536cc6b2151782000681f39/gradle/instrumentation-common.gradle#L8">opentelemetry-java-instrumentation</a>.
   * Namely, this instructs Gradle to include the classes from the dependency on the {@code
   * compileClasspath} and the {@code testRuntimeClasspath} but not the {@code runtimeClasspath}.
   * This makes the scope of the dependency clear to all consumers of this project and gives
   * stronger guarantees that we won't accidentally include libraries targeted for instrumentation
   * in the published Hypertrace javaagent JAR.
   *
   * <p> Example usage:
   * <pre>
   *  dependencies {
   *    library("org.apache.httpcomponents:httpclient:4.0")
   *  }
   * </pre>
   *
   * <p> This above snippet is functionally equivalent to:
   * <pre>
   *  dependencies {
   *    compileOnly("org.apache.httpcomponents:httpclient:4.0")
   *    testImplementation("org.apache.httpcomponents:httpclient:4.0")
   *  }
   * </pre>
   *
   *
   * <p>This change was first introduced to help our dependency scanning tool, Snyk, understand
   * that certain dependencies are not going to be included in the published artifact and therefore
   * do not represent security vulnerabilities
   *
   * @param project to create the configuration in
   */
  private void createLibraryConfiguration(Project project) {
    final Configuration libraryConfiguration = project.getConfigurations()
        .create("library", files -> {
          files.setCanBeConsumed(false);
          files.setCanBeResolved(false);
        });
    // here, we manually copy dependencies added with the library configuration and add them to the
    // testImplementation DependencySet as well. Inspired by https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/0b0516bd1a0599c8f536cc6b2151782000681f39/gradle/instrumentation-common.gradle#L46
    libraryConfiguration.getDependencies().whenObjectAdded(
        dependency -> {
          final Dependency copy = dependency.copy();
          project.getConfigurations().named("testImplementation").get().getDependencies().add(copy);
        }
    );
    project.getConfigurations().named("compileOnly").get().extendsFrom(libraryConfiguration);
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
}
