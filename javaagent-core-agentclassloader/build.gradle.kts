
// This artifact is located in the agent classloader so completelly isolated from the user application
// hence it can pull in any 3rd party libraries (even not shaded!). The classes in agent classloader
// are located in /inst folder and named as .classdata.

plugins {
    `java-library`
}

dependencies {
    api(platform("com.google.protobuf:protobuf-bom:$protobufVersion"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")
}
