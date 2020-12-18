plugins {
    `java-library`
}

dependencies {
    api(project(":filter-api"))
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.squareup.okhttp3:okhttp:3.14.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
}
