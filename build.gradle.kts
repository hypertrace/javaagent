import org.hypertrace.gradle.publishing.License.APACHE_2_0;

plugins {
    `java-library`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.publish-maven-central-plugin") version "1.0.4" apply false
    id("org.hypertrace.ci-utils-plugin") version "0.3.0"
    id("org.gradle.test-retry") version "1.2.0" apply false
}

allprojects {
    apply(plugin="java-library")
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
            "opentelemetry" to "1.11.0",
            "opentelemetry_proto" to "0.11.0-alpha",
            "opentelemetry_java_agent" to "1.11.0-alpha",
            "opentelemetry_java_agent_all" to "1.11.0",
            "opentelemetry_gradle_plugin" to "1.11.0-alpha",
            "byte_buddy" to "1.12.6",
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
