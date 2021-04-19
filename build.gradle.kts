import org.hypertrace.gradle.publishing.License.APACHE_2_0;

plugins {
    `java-library`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.publish-maven-central-plugin") version "1.0.2" apply false
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
            "opentelemetry" to "1.1.0",
            "opentelemetry_java_agent" to "1.1.0-alpha",
            "opentelemetry_java_agent_all" to "1.1.0",
            "byte_buddy" to "1.10.18",
                "slf4j" to "1.7.30"
    ))

    apply<JavaPlugin>()
    apply(plugin = "com.diffplug.spotless")
    apply(from = "$rootDir/gradle/spotless.gradle")

    repositories {
        mavenCentral()
        jcenter()
        maven {
            url = uri("https://dl.bintray.com/open-telemetry/maven")
        }
        maven {
            url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local")
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
