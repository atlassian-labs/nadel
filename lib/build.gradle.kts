import com.bnorm.power.PowerAssertGradleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    groovy
    id("com.bnorm.power.kotlin-power-assert")
}

val graphqlJavaVersion = "0.0.0-2023-06-07T00-38-50-5b19375"
val slf4jVersion = "1.7.25"

dependencies {
    api("com.graphql-java:graphql-java:$graphqlJavaVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")

    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testImplementation("org.openjdk.jmh:jmh-core:1.21")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.21")
    testImplementation("com.google.guava:guava:28.0-jre")
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-framework-datatest:5.1.0")
    testImplementation("io.mockk:mockk:1.12.3")
}

// compileJava.source file("build/generated-src"), sourceSets.main.java
tasks.compileJava {
    source(file("build/generated-src"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf(
            "-progressive",
            "-java-parameters",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
        )
    }
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

configure<PowerAssertGradleExtension> {
    // WARNING: do NOT touch this unless you have read https://github.com/bnorm/kotlin-power-assert/issues/55
    functions = listOf("kotlin.assert", "graphql.nadel.test.dbg")
}
