plugins {
    id("io.opentelemetry.instrumentation.un-shade")
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}:all")
}

tasks.shadowJar {
    // a special case exists here. There does exist a standalone instrumentation library for
    // servlet-3.0, however it does not contain all the classes we need from the netty-4.1
    // instrumentation project (some are located in the shaded javaagent JAR). Therefore, we exclude
    // the shaded instrumentation library from this un-shaded project so that our netty-4.1 project
    // can simply use the standalone netty-4.1 library where possible (and hopefully, eventually get
    // rid of this library un-shade hack).
    relocate("io.opentelemetry.javaagent.shaded.instrumentation.servlet", "io.opentelemetry.instrumentation.servlet")
}