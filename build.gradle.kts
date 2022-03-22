import org.jetbrains.kotlin.gradle.plugin.statistics.ReportStatisticsToElasticSearch.password
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import java.text.SimpleDateFormat
import java.util.Date
import javax.xml.catalog.CatalogFeatures.defaults

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("com.jfrog.artifactory") version "4.28.0"
    id("biz.aQute.bnd.builder") version "5.3.0"
    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"
}

if (JavaVersion.current() != JavaVersion.VERSION_11) {
    val current = JavaVersion.current()
    val jdkHome = System.getenv("JAVA_HOME")
    val msg =
        "This build must be run with Java 11 but you are running $current - Gradle finds the JDK via JAVA_HOME=$jdkHome"
    throw GradleException(msg)
}

fun getDevelopmentVersion(): String {
    val gitProcess = ProcessBuilder("git", "-C", projectDir.absolutePath, "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()

    val gitHash = gitProcess.inputStream.bufferedReader()
        .readText()
        .trim()

    if (gitHash.isBlank()) {
        System.err.println(gitHash)
        throw IllegalStateException("Git hash could not be determined")
    }

    val version = "${SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(Date())}-$gitHash"
    println("Created development version: $version")
    return version
}

val releaseVersion = System.getenv("RELEASE_VERSION")
version = releaseVersion ?: getDevelopmentVersion()
group = "com.atlassian"

allprojects {
    description = "Nadel is a Java library that combines multiple GraphQL services together into one API."

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

gradle.buildFinished(
    @Suppress("ObjectLiteralToLambda") // Gradle dies trying to determine which method to invoke without this
    object : Action<BuildResult> {
        override fun execute(result: BuildResult) {
            println("*******************************")
            println("*")
            if (result.failure != null) {
                println("* FAILURE - ${result.failure}")
            } else {
                println("* SUCCESS")
            }
            println("* Version: $version")
            println("*")
            println("*******************************")
        }
    },
)
