import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.bnorm.power.kotlin-power-assert")
}

dependencies {
    implementation(project(":lib"))
    testImplementation("com.graphql-java:graphql-java-extended-scalars:18.1") {
        exclude(group = "com.graphql-java", module = "graphql-java")
    }
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.6")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.6")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.strikt:strikt-jvm:0.31.0")
    testImplementation("org.yaml:snakeyaml:1.30")
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
}
