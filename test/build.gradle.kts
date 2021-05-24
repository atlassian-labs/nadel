plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":nadel-api"))
    implementation(project(":nadel-engine"))
    implementation(project(":nadel-engine-nextgen"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.6")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    testImplementation("io.strikt:strikt-jvm:0.31.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
