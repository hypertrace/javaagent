plugins {
    `java-library`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.repository-plugin") version "0.2.3"
    id("org.hypertrace.publish-plugin") version "0.3.3" apply false
}

description = "Hypertrace OpenTelemetry Java agent"
group = "org.hypertrace.agent"

allprojects {
    apply(plugin="java-library")
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

subprojects {
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
        maven {
            url = uri(extra.properties["artifactory_contextUrl"] as String + "/gradle")
            credentials {
                username = extra.properties["artifactory_user"] as String
                password = extra.properties["artifactory_password"] as String
            }
        }
    }

    pluginManager.withPlugin("org.hypertrace.publish-plugin") {
        configure<org.hypertrace.gradle.publishing.HypertracePublishExtension> {
            license.set(org.hypertrace.gradle.publishing.License.APACHE_2_0)
        }
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
