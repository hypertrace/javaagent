plugins {
   `java-library`
   id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
   implementation(project(":javaagent-core"))

   api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
   compileOnly("com.google.auto.service:auto-service-annotations:1.0")
   implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
   annotationProcessor("com.google.auto.service:auto-service:1.0")
}
