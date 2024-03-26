plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronautTestVersion = "1.0.0"

dependencies {
    implementation(project(":instrumentation:netty:netty-4.1"))
    testImplementation(project(":testing-common"))
    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-http-client:${micronautVersion}")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")
}

val micronaut2Version = "2.2.3"

for (version in listOf(micronaut2Version)) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.runtimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(project(":testing-common"))
        versionedConfiguration("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
        versionedConfiguration("io.micronaut:micronaut-http-server-netty:${version}")
        versionedConfiguration("io.micronaut:micronaut-http-client:${version}")
        versionedConfiguration("io.micronaut:micronaut-runtime:${version}")
        versionedConfiguration("io.micronaut:micronaut-inject:${version}")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-api:5.7.0")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        classpath = versionedConfiguration + sourceSets.test.get().output
        // We do fine-grained filtering of the classpath of this codebase's sources since Gradle's
        // configurations will include transitive dependencies as well, which tests do often need.
        classpath = classpath.filter {
            if (file(layout.buildDirectory.dir("resources/main")).equals(it) || file(layout.buildDirectory.dir("classes/java/main")).equals(
                    it
                )
            ) {
                // The sources are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }

            val lib = it.absoluteFile
            if (lib.name.startsWith("opentelemetry-javaagent-")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("javaagent-core")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("filter-api")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("opentelemetry-") && lib.name.contains("-autoconfigure-")) {
                // These dependencies should not be on the test classpath, because they will auto-instrument
                // the library and the tests could pass even if the javaagent instrumentation fails to apply
                return@filter false
            }
            return@filter true
        }
        useJUnitPlatform()
    }
    tasks.check { dependsOn(versionedTest) }
}
