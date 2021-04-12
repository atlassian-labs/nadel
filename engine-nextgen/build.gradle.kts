plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":nadel-api"))

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")
}
