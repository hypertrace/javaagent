package io.opentelemetry.instrumentation.gradle;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;

/**
 * Plugin to encapsulate our rather odd practice of depending on a shaded instrumentation JAR
 * published by OpenTelemetry, un-shading the OTEL public API by relocating the packages to their
 * original location so we can use the utilities exposed in the not-shaded OTEL
 * instrumentation library classes.
 */
public final class OpenTelemetryUnShadePlugin implements Plugin<Project> {

  @Override
  public void apply(final Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply("com.github.johnrengelman.shadow");
    project.getTasks().named("shadowJar", ShadowJar.class).configure(shadowJar -> {
      shadowJar.relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.api",
          "io.opentelemetry.api");
      shadowJar.relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.context",
          "io.opentelemetry.context");
      shadowJar.relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv",
          "io.opentelemetry.semconv");
      shadowJar.relocate("io.opentelemetry.javaagent.shaded.instrumentation.api",
          "io.opentelemetry.instrumentation.api");
    });

  }
}
