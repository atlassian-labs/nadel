import graphql.nadel.Deps.kotestRunnerJunit5
import graphql.nadel.Deps.mockk
import graphql.nadel.Deps.striktJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":nadel-api"))

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")

    testImplementation(kotestRunnerJunit5)
    testImplementation(striktJvm)
    testImplementation(mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}
