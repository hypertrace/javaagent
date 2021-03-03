plugins {
    `java-library`
    `maven`
    id("com.diffplug.spotless") version "5.2.0" apply false
    id("org.hypertrace.publish-plugin") version "0.3.3" apply false
    id("org.hypertrace.ci-utils-plugin") version "0.1.4"
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
    apply(plugin = "maven")
    group = "org.hypertrace.agent"
    description = "Hypertrace OpenTelemetry Javaagent"

    extra.set("versions", mapOf(
            "opentelemetry" to "0.15.0",
            "opentelemetry_java_agent" to "0.15.1",
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
        "uploadArchives"(Upload::class) {
            val ossrhUsername: String? by project
            val ossrhPassword: String? by project
            repositories {
                withConvention(MavenRepositoryHandlerConvention::class) {
                    mavenDeployer {
                        withGroovyBuilder {
                            "repository"("url" to "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                                "authentication"("userName" to ossrhUsername, "password" to ossrhPassword)
                            }
                            "snapshotRepository"("url" to "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
                                "authentication"("userName" to ossrhUsername, "password" to ossrhPassword)
                            }
                        }
                        pom.project {
                            withGroovyBuilder {
                                "licenses" {
                                    "license" {
                                        "name"("The Apache Software License, Version 2.0")
                                        "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                        "distribution"("repo")
                                    }
                                }
                                "scm" {
                                    "connection"("scm:git:git://github.com/hypertrace/javaagent.git")
                                    "developerConnection"("scm:git:ssh://github.com:hypertrace/javaagent.git")
                                    "url"("https://github.com/hypertrace/javaagent/tree/main")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
