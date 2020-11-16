plugins {
    java
}

dependencies {
    implementation(project(":filter"))
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.squareup.okhttp3:okhttp:3.14.9")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
}
