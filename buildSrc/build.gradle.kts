plugins {
  groovy
  `java-gradle-plugin`
  id("com.diffplug.gradle.spotless") version "4.3.0"
}

gradlePlugin {
  plugins {
    create("muzzle-plugin") {
      id = "muzzle"
      implementationClass = "MuzzlePlugin"
    }
    create("auto-instrumentation-plugin") {
      id = "io.opentelemetry.instrumentation.auto-instrumentation"
      implementationClass = "io.opentelemetry.instrumentation.gradle.AutoInstrumentationPlugin"
    }
  }
}

repositories {
  mavenLocal()
  jcenter()
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  val otelInstrumentationVersion = "1.24.0-alpha"
  implementation("io.opentelemetry.javaagent:opentelemetry-muzzle:$otelInstrumentationVersion")
  implementation("io.opentelemetry.instrumentation.muzzle-generation:io.opentelemetry.instrumentation.muzzle-generation.gradle.plugin:$otelInstrumentationVersion")
  implementation("io.opentelemetry.instrumentation.muzzle-check:io.opentelemetry.instrumentation.muzzle-check.gradle.plugin:$otelInstrumentationVersion")
  implementation("com.github.jengelman.gradle.plugins:shadow:6.0.0")
  implementation("org.eclipse.aether", "aether-connector-basic", "1.1.0")
  implementation("org.eclipse.aether", "aether-transport-http", "1.1.0")
  implementation("org.apache.maven", "maven-aether-provider", "3.3.9")

  implementation("com.google.guava", "guava", "20.0")
  implementation("org.ow2.asm", "asm", "9.1")
  implementation("org.ow2.asm", "asm-tree", "9.1")
  implementation("org.apache.httpcomponents:httpclient:4.5.10")
  implementation("net.bytebuddy:byte-buddy-gradle-plugin:1.12.6") {
    exclude(group = "net.bytebuddy", module = "byte-buddy")
  }
  implementation("net.bytebuddy:byte-buddy-dep:1.12.13")

  testImplementation("org.spockframework", "spock-core", "1.3-groovy-2.5")
  testImplementation("org.codehaus.groovy", "groovy-all", "2.5.8")
}
