plugins {
    `java-library`
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3") {
        constraints {
            implementation("org.yaml:snakeyaml:1.31") {
                because(
                    "SNYK error SNYK-JAVA-ORGYAML-2806360"
                )
            }
        }
    }
}
