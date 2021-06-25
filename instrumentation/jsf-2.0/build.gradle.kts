plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}


dependencies {
    testImplementation("com.ocpsoft:prettyfaces-jsf2:3.3.3") {
        isTransitive = false
    }
//    testImplementation("com.sun.faces:jsf-api:2.2.14")
    testImplementation("org.jboss.spec.javax.faces:jboss-jsf-api_2.1_spec:2.0.1.Final")
    testImplementation("javax.xml.bind:jaxb-api:2.2.11")
    testImplementation("com.sun.xml.bind:jaxb-impl:2.2.11")


    testImplementation(testFixtures(project(":testing-common")) as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-smoderver")
    }

    val jettyVersion = "9.4.35.v20201120"
    testImplementation("org.eclipse.jetty:jetty-annotations:$jettyVersion")
    testImplementation("org.eclipse.jetty:apache-jsp:$jettyVersion")
    testRuntimeOnly(project(":instrumentation:servlet:servlet-3.0")) {

    }
//    testRuntimeOnly("com.sun.faces", "jsf-api", "2.1.7")
//    testRuntimeOnly("com.sun.faces", "jsf-impl", "2.1.7")

}
