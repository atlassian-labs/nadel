import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.bnorm.power.kotlin-power-assert") version "0.10.0"
}

dependencies {
    implementation(project(":nadel-api"))
    implementation(project(":nadel-engine"))
    implementation(project(":nadel-engine-nextgen"))
    testImplementation("com.graphql-java:graphql-java-extended-scalars:2021-06-29T01-19-32-8e19827") {
        exclude(group = "com.graphql-java", module = "com.graphql-java")
    }
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.6")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.6")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    testImplementation("io.strikt:strikt-jvm:0.31.0")
    testImplementation("com.graphql-java:graphql-java-extended-scalars:2021-06-29T01-19-32-8e19827")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-progressive", "-java-parameters")
    }
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}
