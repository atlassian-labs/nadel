import com.bnorm.power.PowerAssertGradleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.bnorm.power.kotlin-power-assert") version "0.10.0"
}

dependencies {
    implementation(project(":nadel-api"))

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-progressive", "-java-parameters", "-Xopt-in=kotlin.RequiresOptIn")
    }
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

configure<PowerAssertGradleExtension> {
    // WARNING: do NOT touch this unless you have read https://github.com/bnorm/kotlin-power-assert/issues/55
    functions = listOf("kotlin.assert", "graphql.nadel.test.dbg")
}
