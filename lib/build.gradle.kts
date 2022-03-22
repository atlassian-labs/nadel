import com.bnorm.power.PowerAssertGradleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

plugins {
    kotlin("jvm")
    groovy
    id("com.bnorm.power.kotlin-power-assert")

    // Publishing
    id("java")
    id("java-library")
    id("signing")
    id("maven-publish")
    id("com.jfrog.artifactory")
}

val graphqlJavaVersion = "0.0.0-2022-03-01T04-16-14-e973c9a1"
val slf4jVersion = "1.7.25"

dependencies {
    api("com.graphql-java:graphql-java:$graphqlJavaVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")

    testImplementation("org.antlr:antlr4-runtime:4.8")
    testImplementation("junit:junit:4.11")
    testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
    testImplementation("org.codehaus.groovy:groovy:3.0.8")
    testImplementation("org.codehaus.groovy:groovy-json:3.0.8")
    testImplementation("cglib:cglib-nodep:3.1")
    testImplementation("org.objenesis:objenesis:2.1")
    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    testImplementation("org.openjdk.jmh:jmh-core:1.21")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.21")
    testImplementation("com.google.guava:guava:28.0-jre")
    testImplementation("com.graphql-java:graphql-java-extended-scalars:2021-06-29T01-19-32-8e19827")
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-framework-datatest:5.1.0")
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("nadel") {
            from(components["java"])

            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/atlassian-labs/nadel")

                scm {
                    url.set("https://github.com/atlassian-labs/nadel")
                    connection.set("https://github.com/atlassian-labs/nadel")
                    developerConnection.set("https://github.com/atlassian-labs/nadel")
                }

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("andimarek")
                        name.set("Andreas Marek")
                    }
                }
            }
        }
    }
}

artifactory {
    publish {
        // This should be a PublisherConfig but the fucking Artifactory thing wraps the Closure, so we don't have access
        // See https://github.com/jfrog/build-info/blob/0d88631f9a116155360b5754ff7b01dc79bbbe02/build-info-extractor-gradle/src/main/groovy/org/jfrog/gradle/plugin/artifactory/dsl/PublisherConfig.groovy
        withGroovyBuilder {
            "setContextUrl"("https://packages.atlassian.com/")

            repository {
                // This should be a PublisherConfig.Repository
                withGroovyBuilder {
                    "setRepoKey"("maven-central")
                    "setUsername"(System.getenv("ARTIFACTORY_USERNAME"))
                    "setPassword"(System.getenv("ARTIFACTORY_API_KEY"))
                }
            }

            // This should be a PublisherConfig but note that PublisherConfig seems to delegate missing methods to PublisherHandler
            defaults(
                @Suppress("ObjectLiteralToLambda") // Gradle can't compile unless we use object literal
                object : Action<ArtifactoryTask> {
                    override fun execute(task: ArtifactoryTask) {
                        task.setPublishIvy(false)
                        task.publications("nadel")

                        // This needs to be "false", otherwise, the artifactory plugin will try to publish
                        // a build info file to Artifactory and fail because we don't have the permissions to do that.
                        clientConfig.publisher.isPublishBuildInfo = false
                    }
                }
            )
        }
    }
}

tasks.withType<PublishToMavenRepository> {
    dependsOn(tasks["build"])
}

signing {
    if (System.getenv("SIGNING_KEY") != null) {
        useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        sign(publishing.publications)
    }
}
