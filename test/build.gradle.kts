import com.bnorm.power.PowerAssertGradleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.bnorm.power.kotlin-power-assert")
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test"))
    testImplementation("com.graphql-java:graphql-java-extended-scalars:18.1") {
        exclude(group = "com.graphql-java", module = "graphql-java")
    }
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("io.strikt:strikt-jvm:0.31.0")
    testImplementation("org.yaml:snakeyaml:1.30")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("com.google.guava:guava:33.1.0-jre")
    testImplementation("com.squareup:kotlinpoet:1.16.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_17.toString()
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
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertFalse")
}
