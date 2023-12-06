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

tasks {
  processResources {
    /*
    Make Jar build fail on duplicate files

    By default Gradle Jar task can put multiple files with the same name
    into a Jar. This may lead to confusion. For example if auto-service
    annotation processing creates files with same name in `scala` and
    `java` directory this would result in Jar having two files with the
    same name in it. Which in turn would result in only one of those
    files being actually considered when that Jar is used leading to very
    confusing failures.

    Instead we should 'fail early' and avoid building such Jars.
    */
    duplicatesStrategy = DuplicatesStrategy.WARN
  }
}

//java {
//  sourceCompatibility = JavaVersion.VERSION_1_8
//  targetCompatibility = JavaVersion.VERSION_1_8
//}

configurations {
  all {
    exclude(group = "asm", module = "asm")
  }
}

//configurations.compile.get().dependencies.remove(dependencies.gradleApi())

dependencies {
  compileOnly(gradleApi())
  implementation(localGroovy())
  val otelInstrumentationVersion = "1.24.0-alpha"
  implementation("io.opentelemetry.javaagent:opentelemetry-muzzle:$otelInstrumentationVersion")
  implementation("io.opentelemetry.instrumentation.muzzle-generation:io.opentelemetry.instrumentation.muzzle-generation.gradle.plugin:$otelInstrumentationVersion") {
    exclude(group = "gradle.plugin.com.github.johnrengelman", module = "shadow")
  }
  implementation("io.opentelemetry.instrumentation.muzzle-check:io.opentelemetry.instrumentation.muzzle-check.gradle.plugin:$otelInstrumentationVersion") {
    exclude(group = "gradle.plugin.com.github.johnrengelman", module = "shadow")
  }
  implementation("com.github.johnrengelman", "shadow","8.1.1"){
    exclude(group = "asm", module = "asm")
  }
  implementation("org.eclipse.aether", "aether-connector-basic", "1.1.0")
  implementation("org.eclipse.aether", "aether-transport-http", "1.1.0")
  implementation("org.apache.maven", "maven-aether-provider", "3.3.9")

  implementation("com.google.guava", "guava", "32.0.0-android")
  implementation("org.ow2.asm", "asm", "9.1")
  implementation("org.ow2.asm", "asm-tree", "9.1")
  implementation("org.apache.httpcomponents:httpclient:4.5.10")
  implementation("net.bytebuddy:byte-buddy-gradle-plugin:1.14.2") {
    exclude(group = "net.bytebuddy", module = "byte-buddy")
  }
  implementation("net.bytebuddy:byte-buddy-dep:1.14.2")

  testImplementation("org.spockframework", "spock-core", "1.3-groovy-2.5")
  testImplementation("org.codehaus.groovy", "groovy-all", "2.5.8")
}
