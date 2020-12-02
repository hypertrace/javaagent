plugins {
    `java-library`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.publish-plugin") version "0.3.3" apply false
    id("org.hypertrace.ci-utils-plugin") version "0.1.4"
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
            "opentelemetry" to "0.11.0",
            "opentelemetry_java_agent" to "0.11.0"
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

    pluginManager.withPlugin("org.hypertrace.publish-plugin") {
        configure<org.hypertrace.gradle.publishing.HypertracePublishExtension> {
            license.set(org.hypertrace.gradle.publishing.License.APACHE_2_0)
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
