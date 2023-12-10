import com.bnorm.power.PowerAssertGradleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    groovy
    id("com.bnorm.power.kotlin-power-assert")
}

val graphqlJavaVersion = "0.0.0-2023-12-05T22-54-46-39d2155"
val slf4jVersion = "1.7.25"

dependencies {
    api("com.graphql-java:graphql-java:$graphqlJavaVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("com.graphql-java:graphql-java-extended-scalars:21.0") {
        exclude("com.graphql-java", "graphql-java")
    }

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-framework-datatest:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
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
            "-Xcontext-receivers",
        )
    }
}

configure<PowerAssertGradleExtension> {
    // WARNING: do NOT touch this unless you have read https://github.com/bnorm/kotlin-power-assert/issues/55
    functions = listOf("kotlin.assert", "graphql.nadel.test.dbg")
}
