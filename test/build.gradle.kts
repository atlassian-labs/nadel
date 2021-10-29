import graphql.nadel.Deps.jacksonDataformatYaml
import graphql.nadel.Deps.jacksonModuleKotlin
import graphql.nadel.Deps.kotestRunnerJunit5
import graphql.nadel.Deps.mockk
import graphql.nadel.Deps.reflections
import graphql.nadel.Deps.striktJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":nadel-api"))
    implementation(project(":nadel-engine"))
    implementation(project(":nadel-engine-nextgen"))
    testImplementation(reflections)
    testImplementation(jacksonDataformatYaml)
    testImplementation(jacksonModuleKotlin)
    testImplementation(kotestRunnerJunit5)
    testImplementation(striktJvm)
    testImplementation(mockk)
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
