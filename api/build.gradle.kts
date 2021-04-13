plugins {
    antlr
    kotlin("jvm")
}

val graphqlJavaVersion = "2021-03-22T20-02-28-3a701a0f"
val slf4jVersion = "1.7.25"

configurations {
    create("graphqlJavaSource")
}

task("extractGraphqlGrammar", type = Copy::class) {
    from({
        zipTree({
            configurations.compileClasspath.get().files.first {
                it.path.contains("graphql-java/graphql-java/")
            }
        })
    }) {
        include("*.g4")
    }
    into("src/main/antlr")
}

task("generateAntrlToJavaSource") {
    doLast {
        copy {
            from({ zipTree(configurations.getByName("graphqlJavaSource").singleFile).files }) {
                include("GraphqlAntlrToLanguage.java")
            }
            into("build/generated-src/antlr/main/graphql/nadel/parser")
        }
        val replacePatterns = listOf(
                "package graphql.parser;" to "package graphql.nadel.parser;\nimport graphql.parser.MultiSourceReader;\nimport graphql.parser.AntlrHelper;",
                "import graphql.parser.antlr.GraphqlParser;" to "import graphql.nadel.parser.antlr.StitchingDSLParser;",
                "import graphql.parser.antlr.GraphqlLexer;" to "import graphql.nadel.parser.antlr.StitchingDSLLexer;",
                "import graphql.parser.antlr.GraphqlBaseVisitor;" to "import graphql.nadel.parser.antlr.StitchingDSLBaseVisitor;",
                "import com.google.common.collect.ImmutableList;" to "import graphql.com.google.common.collect.ImmutableList;",
                "GraphqlBaseVisitor<Void>" to "StitchingDSLBaseVisitor<Void>",
                "GraphqlParser" to "StitchingDSLParser",
                "GraphqlLexer" to "StitchingDSLLexer"
        )
        val sourceFile = File(project.projectDir, "build/generated-src/antlr/main/graphql/nadel/parser/GraphqlAntlrToLanguage.java")

        val content = sourceFile.readText()
                .let {
                    replacePatterns.fold(it) { accumulator, (find, replace) ->
                        accumulator.replace(find, replace)
                    }
                }
                .let {
                    val startIndex = it.indexOf("//MARKER START")
                    val endIndex = it.indexOf("//MARKER END")
                    require(startIndex > 0)
                    require(endIndex > 0)
                    val endIndexLine = it.indexOf("\n", endIndex)
                    require(endIndexLine > 0)
                    it.removeRange(startIndex, endIndexLine + 1)
                }

        sourceFile.writeText(content)
    }
}

dependencies {
    "graphqlJavaSource"("com.graphql-java:graphql-java:$graphqlJavaVersion:sources") {
        isTransitive = false
    }
    api("com.graphql-java:graphql-java:$graphqlJavaVersion")
    implementation("org.antlr:antlr4-runtime:4.8")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    "antlr"("org.antlr:antlr4:4.8")
}

tasks.generateGrammarSource {
    setIncludes(listOf("StitchingDSL.g4"))
    maxHeapSize = "64m"
    arguments = arguments + "-visitor"
    outputDirectory = file("${project.buildDir}/generated-src/antlr/main/graphql/nadel/parser/antlr")

    inputs.dir("src/main/antlr")
    dependsOn(tasks.getByName("extractGraphqlGrammar"), tasks.getByName("generateAntrlToJavaSource"))
}

// compileJava.source file("build/generated-src"), sourceSets.main.java
tasks.compileJava {
    source(file("build/generated-src"))
}
