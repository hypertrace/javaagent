import org.hypertrace.gradle.publishing.License.APACHE_2_0;

plugins {
    `java-library`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.publish-maven-central-plugin") version "1.0.4" apply false
    id("org.hypertrace.ci-utils-plugin") version "0.3.0"
    id("org.gradle.test-retry") version "1.2.0" apply false
    id("org.owasp.dependencycheck") version "7.1.1"
}

allprojects {
    apply(plugin="java-library")
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.compileJava {
        options.compilerArgs.add("-Werror")
    }


    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.isDeprecation = true
        options.release.set(8)
    }
}

val testDependencies by configurations.creating {
    extendsFrom(configurations.testImplementation.get())
    extendsFrom(configurations.testRuntimeOnly.get())
}

subprojects {
    group = "org.hypertrace.agent"
    description = "Hypertrace OpenTelemetry Javaagent"

    extra.set("versions", mapOf(
            // when updating these values, some values must also be updated in buildSrc as this map
            // cannot be accessed there
            // version 1.11.1-alpha is specified for opentelemetry_java_agent_jaxrs
            // because jaxrs-client-2.0 instrumentation was removed from opentelemetry after this version
            // because it didn't add any value in instrumentation, since they have
            // http-url-connection and apache-httpclient instrumentations that already create the client spans
            "opentelemetry" to "1.24.0",
            "opentelemetry_semconv" to "1.24.0-alpha",
            "opentelemetry_proto" to "0.11.0-alpha",
            "opentelemetry_java_agent" to "1.24.0-alpha",
            "opentelemetry_java_agent_all" to "1.24.0",
            "opentelemetry_java_agent_jaxrs" to "1.11.1-alpha",
            "opentelemetry_java_agent-tooling" to "1.24.0-alpha",

            "opentelemetry_gradle_plugin" to "1.24.0-alpha",
            "byte_buddy" to "1.12.10",
                "slf4j" to "1.7.32"
    ))

    apply<JavaPlugin>()
    apply(plugin = "com.diffplug.spotless")
    apply(from = "$rootDir/gradle/spotless.gradle")

    repositories {
        mavenCentral()
        jcenter()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    pluginManager.withPlugin("org.hypertrace.publish-maven-central-plugin") {
        configure<org.hypertrace.gradle.publishing.HypertracePublishMavenCentralExtension> {
            repoName.set("javaagent")
            license.set(APACHE_2_0)
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
        testImplementation("org.junit-pioneer:junit-pioneer:1.0.0")
    }

    tasks {
        test {
            useJUnitPlatform()
            reports {
                junitXml.isOutputPerTestCase = true
            }
        }
    }
}

dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.valueOf("ALL")
//    suppressionFile = "owasp-suppressions.xml"
    scanConfigurations.add("runtimeClasspath")
    failBuildOnCVSS = 7.0F
}
