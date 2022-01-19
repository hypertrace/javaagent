plugins {
    `java-library`
}

val versions: Map<String, String> by extra

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent"]}") {
        constraints {
            implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling-java9:1.7.2-alpha") {
                attributes {
                    attribute(Attribute.of("org.gradle.jvm.version", Integer::class.java), 9 as Integer)
                }
            }
        }
    }
}