plugins {
    java
    id("com.diffplug.spotless") version "5.2.0" apply false
}

description = "Traceable OpenTelemetry Java agent"
group = "io.traceable.opentelemetry"


allprojects {
    apply(plugin="java-library")
    group = "ai.traceable.agent.javaagent"
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

subprojects {
    version = rootProject.version

  //  apply<JavaPlugin>()
  //  apply(plugin = "com.diffplug.spotless")
  //  apply(from = "$rootDir/gradle/spotless.gradle")

    repositories {
        jcenter()
        maven {
            url = uri("https://dl.bintray.com/open-telemetry/maven")
        }
        maven {
            url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local")
        }
        mavenCentral()
    }

    dependencies {
        testImplementation("org.mockito:mockito-core:3.3.3")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
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
