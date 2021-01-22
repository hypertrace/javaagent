plugins {
   `java-library`
   id("com.athaydes.wasm")
}

wasm {
   // this is used as the top-level package name for this project.
   packageName = "org.hypertrace.agent.filter.wasm.generated"


   classNameByFile = mapOf("add.wasm" to "Adder")
}

val versions: Map<String, String> by extra

dependencies {
   api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
   api(project(":javaagent-core"))
   implementation("com.google.auto.service:auto-service:1.0-rc7")
   annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
}
