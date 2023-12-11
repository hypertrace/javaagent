import org.hypertrace.gradle.publishing.License.APACHE_2_0;

plugins {
  id("com.diffplug.spotless") version "5.2.0" apply false
  id("org.hypertrace.publish-maven-central-plugin") version "1.0.6"
  id("org.hypertrace.ci-utils-plugin") version "0.3.0"
  id("org.gradle.test-retry") version "1.2.0" apply false
  id("org.owasp.dependencycheck") version "7.1.1"
}

subprojects {
  group = "org.hypertrace.agent"
  description = "Hypertrace OpenTelemetry Javaagent"

  apply(plugin = "java-library")
  apply(plugin = "com.diffplug.spotless")
  apply(from = "$rootDir/gradle/spotless.gradle")
  pluginManager.withPlugin("java-library") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
      mavenCentral()
      maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      }
    }

    configurations {
      all {
        exclude(group = "asm", module = "asm")
        exclude(group = "asm", module = "asm-commons")
        exclude(group = "asm", module = "asm-tree")
        exclude(group = "gradle.plugin.com.github.johnrengelman", module = "shadow")
      }
    }

    tasks.named<JavaCompile>("compileJava") {
      options.compilerArgs.add("-Werror")
    }

    tasks.withType<Jar> {
      duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    tasks.withType<JavaCompile> {
      options.compilerArgs.add("-Xlint:unchecked")
      options.isDeprecation = true
      options.release.set(8)
    }

    dependencies {
      add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-api:5.7.0")
      add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-engine:5.7.0")
      add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.junit-pioneer:junit-pioneer:1.0.0")
    }

    tasks.named<Test>("test") {
      useJUnitPlatform()
      reports {
        junitXml.isOutputPerTestCase = true
      }
    }

    extra.set("versions", mapOf(
        // when updating these values, some values must also be updated in buildSrc as this map
        // cannot be accessed there
        "opentelemetry" to "1.24.0",
        "opentelemetry_semconv" to "1.24.0-alpha",
        "opentelemetry_proto" to "0.11.0-alpha",
        "opentelemetry_java_agent" to "1.24.0-alpha",
        "opentelemetry_java_agent_all" to "1.24.0",
        "opentelemetry_java_agent-tooling" to "1.24.0-alpha",

        "opentelemetry_gradle_plugin" to "1.24.0-alpha",
        "byte_buddy" to "1.12.10",
        "slf4j" to "2.0.7"
    ))
  }

  pluginManager.withPlugin("org.hypertrace.publish-maven-central-plugin") {
    configure<org.hypertrace.gradle.publishing.HypertracePublishMavenCentralExtension> {
      repoName.set("javaagent")
      license.set(APACHE_2_0)
    }
  }
}

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.valueOf("ALL")
//    suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 7.0F
}
