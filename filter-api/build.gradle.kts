plugins {
   `java-library`
   id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
   api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
   api(project(":javaagent-core"))
   compileOnly("com.google.auto.service:auto-service-annotations:1.0")
   implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
   annotationProcessor("com.google.auto.service:auto-service:1.0")
   implementation(project(":otel-extensions"))
}
