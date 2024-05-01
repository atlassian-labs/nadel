package graphql.nadel.tests.next

import com.google.common.reflect.ClassPath
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import graphql.nadel.tests.withPrettierPrinter
import io.kotest.mpp.newInstanceNoArgConstructor
import java.io.File
import kotlin.reflect.KClass

// For autocomplete
private typealias CaptureTestData = Unit

suspend fun main() {
    val sourceRoot = File("test/src/test/kotlin/")
    require(sourceRoot.exists() && sourceRoot.isDirectory)

    getTestClassSequence()
        .onEach { klass ->
            println("Loading ${klass.qualifiedName}")
        }
        .map {
            it to it.newInstanceNoArgConstructor() as NadelIntegrationTest
        }
        .toList() // Construct all tests before running them, plus we need to be in a suspending context
        .forEach { (klass, test) ->
            println("Recording ${klass.qualifiedName}")

            val captured = test.capture()

            val outputFile = FileSpec.builder(ClassName.bestGuess(klass.qualifiedName!! + "Data"))
                .indent(' '.toString().repeat(4))
                .addFileComment("@formatter:off")
                .addType(makeTestDataClass(klass, captured))
                .build()
                .writeTo(sourceRoot)

            // Fixes shitty indentation
            outputFile
                .writeText(
                    outputFile.readText()
                        .replace(
                            "            ),\n            )\n",
                            "            ),\n        )\n",
                        )
                        .replace(
                            "            \"\"\".trimMargin(),\n            )\n",
                            "            \"\"\".trimMargin(),\n        )\n",
                        ),
                )
        }
}

private fun getTestClassSequence(): Sequence<KClass<NadelIntegrationTest>> {
    return ClassPath.from(ClassLoader.getSystemClassLoader())
        .getTopLevelClassesRecursive("graphql.nadel.tests")
        .asSequence()
        .map {
            it.load().kotlin
        }
        .filterNot {
            it.isAbstract
        }
        .mapNotNull {
            it.asSubclassOfOrNull()
        }
}

private fun makeTestDataClass(
    klass: KClass<out Any>,
    captured: TestExecutionCapture,
): TypeSpec {
    return TypeSpec.classBuilder(klass.simpleName + "Data")
        .superclass(TestData::class)
        .addKdoc("This class is generated. Do NOT modify.\n\nRefer to [graphql.nadel.tests.next.CaptureTestData]")
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S","unused").build())
        .addProperty(makeServiceCallsProperty(captured))
        .addProperty(makeNadelResultProperty(captured))
        .build()
}

private fun makeServiceCallsProperty(captured: TestExecutionCapture): PropertySpec {
    val listOf = MemberName("kotlin.collections", "listOf")
    val callsType = List::class.asClassName().parameterizedBy(ExpectedServiceCall::class.asTypeName())

    return PropertySpec.builder("calls", callsType)
        .addModifiers(KModifier.OVERRIDE)
        .initializer(
            buildCodeBlock {
                add("%M", listOf)
                add("(\n")
                indented {
                    captured.calls
                        // Calls can appear out of order (e.g. parallel calls) so sort it here to ensure a consistent output
                        .sortedWith(
                            compareBy(
                                { it.query },
                                {
                                    jsonObjectMapper.writeValueAsString(it.variables)
                                },
                            )
                        )
                        .forEach { call ->
                            add(makeConstructorInvocationToExpectedServiceCall(call))
                            add(",\n")
                        }
                }
                add(")")
            },
        )
        .build()
}

private fun makeNadelResultProperty(captured: TestExecutionCapture): PropertySpec {
    val listOfJsonStringsMember = MemberName("graphql.nadel.tests.next", ::listOfJsonStrings.name)

    return PropertySpec.builder("response", ExpectedNadelResponse::class)
        .addModifiers(KModifier.OVERRIDE)
        .initializer(
            buildCodeBlock {
                add("%T", ExpectedNadelResponse::class)
                add("(\n")

                add("response = %S,\n", captured.result?.let(::writeResultJson))

                add("delayedResponses = %M", listOfJsonStringsMember)
                add("(\n")
                indented {
                    captured.delayedResults
                        .map (::writeResultJson)
                        .sorted() // Delayed results are not in deterministic order, so we sort them so the output is consistent
                        .forEach { json ->
                            add("%S", json)
                            add(",\n")
                        }
                }
                add("),\n")

                add(")")
            },
        )
        .build()
}

private fun makeConstructorInvocationToExpectedServiceCall(call: TestExecutionCapture.Call): CodeBlock {
    val listOfJsonStringsMember = MemberName("graphql.nadel.tests.next", ::listOfJsonStrings.name)

    return buildCodeBlock {
        add("%T", ExpectedServiceCall::class.asClassName())
        add("(\n")
        indented {
            ExpectedServiceCall::query.name
            add("%L = %S,\n", ExpectedServiceCall::query.name, call.query.replaceIndent(" "))
            add("%L = %S,\n", ExpectedServiceCall::variables.name, call.variables)
            add("%L = %S,\n", ExpectedServiceCall::response.name, writeResultJson(call.result))

            add("delayedResponses = %M", listOfJsonStringsMember)
            add("(\n")
            indented {
                call.delayedResults
                    .map (::writeResultJson)
                    .sorted() // Delayed results are not in deterministic order, so we sort them so the output is consistent
                    .forEach { json ->
                        add("%S", json)
                        add(",\n")
                    }
            }
            add("),\n")
        }
        add(")")
    }
}

private fun writeResultJson(result: JsonMap): String {
    return jsonObjectMapper
        .withPrettierPrinter()
        .writeValueAsString(result)
        .replaceIndent(" ")
}

private fun writeResultJson(result: ExecutionResult): String {
    return jsonObjectMapper
        .withPrettierPrinter()
        .writeValueAsString(result.toSpecification())
        .replaceIndent(" ")
}

private fun writeResultJson(result: DelayedIncrementalPartialResult): String {
    return jsonObjectMapper
        .withPrettierPrinter()
        .writeValueAsString(result.toSpecification())
        .replaceIndent(" ")
}
