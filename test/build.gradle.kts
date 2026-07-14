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

// Generates/updates the *Snapshot.kt files used by NadelIntegrationTest tests.
// This is the CLI equivalent of the "Update Test Snapshots" IntelliJ run config.
//
// Usage:
//   ./gradlew :test:updateTestSnapshots                       # generate only missing snapshots
//   ./gradlew :test:updateTestSnapshots --args="graphql.nadel.tests.next.fixtures.hydration.HydrationTest"
//                                                             # (re)generate snapshots for the given test FQN(s)
//   ./gradlew :test:updateTestSnapshots --args="graphql.nadel.tests.next.fixtures.execution"
//                                                             # (re)generate every snapshot under a package/folder, recursively
//                                                             # (a filesystem path to the folder works too)
tasks.register<JavaExec>("updateTestSnapshots") {
    group = "verification"
    description = "Generates/updates test snapshots (pass test FQNs via --args to regenerate specific ones)."

    mainClass.set("graphql.nadel.tests.next.UpdateTestSnapshotsKt")
    classpath = sourceSets["test"].runtimeClasspath

    // The generator resolves the source root via the relative path "test/src/test/kotlin/",
    // so it must run from the repository root.
    workingDir = rootProject.projectDir

    // Ensure test sources are compiled before generating snapshots.
    dependsOn(tasks.named("testClasses"))
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
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertFalse")
}
