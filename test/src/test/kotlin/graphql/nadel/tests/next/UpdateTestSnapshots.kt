package graphql.nadel.tests.next

import com.google.common.reflect.ClassPath
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
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

// For navigation so you can search up UpdateTestSnapshots
private typealias UpdateTestSnapshots = Unit

suspend inline fun <reified T : NadelIntegrationTest> update() {
    return update(T::class)
}

suspend fun <T : NadelIntegrationTest> update(klass: KClass<T>) {
    main(klass.qualifiedName!!)
}

private suspend fun main(vararg args: String) {
    val sourceRoot = File("test/src/test/kotlin/")
    require(sourceRoot.exists() && sourceRoot.isDirectory)

    getTestClassSequence()
        .filter {
            args.isEmpty() || args.contains(it.qualifiedName)
        }
        // Only process non-existent by default
        .filter { klass ->
            classForNameOrNull(getSnapshotClassName(klass.asClassName()).reflectionName()) == null
                || (args.isNotEmpty() && klass.qualifiedName in args)
        }
        .onEach { klass ->
            println("Loading ${klass.qualifiedName}")
        }
        .map {
            it to it.newInstanceNoArgConstructor()
        }
        .toList() // Construct all tests before running them, plus we need to be in a suspending context
        .forEach { (klass, test) ->
            println("Recording ${klass.qualifiedName}")

            try {
                val captured = test.capture()

                writeTestSnapshotClass(
                    testClassName = klass.asClassName(),
                    captured = captured,
                    sourceRoot = sourceRoot,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
}

fun writeTestSnapshotClass(
    testClassName: ClassName,
    captured: TestExecutionCapture,
    sourceRoot: File,
) {
    val snapshotClassName = getSnapshotClassName(testClassName)
    val outputFile = FileSpec.builder(snapshotClassName)
        .indent(' '.toString().repeat(4))
        .addFileComment(FORMATTER_OFF)
        .addFunction(makeUpdateSnapshotFunction(testClassName))
        .addType(makeTestSnapshotClass(testClassName, captured))
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

fun getSnapshotClassName(className: ClassName): ClassName {
    val suffix = if (className.simpleName.contains(" ") || className.simpleName.none(Char::isUpperCase)) {
        " snapshot"
    } else {
        "Snapshot"
    }

    return ClassName(className.packageName, className.simpleName + suffix)
}

fun makeUpdateSnapshotFunction(testClassName: ClassName): FunSpec {
    return FunSpec.builder("main")
        .addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)
        .addCode("graphql.nadel.tests.next.update<%T>()", testClassName)
        .build()
}

private fun getTestClassSequence(): Sequence<KClass<out NadelIntegrationTest>> {
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

private fun makeTestSnapshotClass(
    testClassName: ClassName,
    captured: TestExecutionCapture,
): TypeSpec {
    return TypeSpec.classBuilder(getSnapshotClassName(testClassName))
        .superclass(TestSnapshot::class)
        .addKdoc("This class is generated. Do NOT modify.\n\nRefer to [graphql.nadel.tests.next.UpdateTestSnapshots]")
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())
        .addProperty(makeServiceCallsProperty(captured))
        .addProperty(makeNadelResultProperty(captured))
        .build()
}

private fun makeServiceCallsProperty(captured: TestExecutionCapture): PropertySpec {
    val listOf = MemberName("kotlin.collections", "listOf")
    val callsType = List::class.asClassName().parameterizedBy(ExpectedServiceCall::class.asTypeName())

    // override val calls: List<ExpectedServiceCall> = listOf(…)
    return PropertySpec.builder(TestSnapshot::calls.name, callsType)
        .addModifiers(KModifier.OVERRIDE)
        .initializer(
            buildCodeBlock {
                add("%M", listOf)
                add("(\n")
                captured.calls
                    // Calls can appear out of order (e.g. parallel calls) so sort it here to ensure a consistent output
                    .sortedWith(
                        compareBy(
                            { it.service },
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
                add(")")
            },
        )
        .build()
}

private fun makeNadelResultProperty(captured: TestExecutionCapture): PropertySpec {
    val listOfJsonStringsMember = MemberName("graphql.nadel.tests.next", "listOfJsonStrings")

    val combinedResult = combineExecutionResults(
        result = captured.result?.toSpecification() as JsonMap,
        incrementalResults = captured.delayedResults
            .map {
                it.toSpecification()
            },
    )

    // override val result: ExpectedNadelResult = …
    return PropertySpec.builder(TestSnapshot::result.name, ExpectedNadelResult::class)
        .addModifiers(KModifier.OVERRIDE)
        .initializer(
            buildCodeBlock {
                // Invoke constructor
                add("%T", ExpectedNadelResult::class)
                add("(\n")

                add("result = %S,\n", captured.result?.let(::writeResultJson))

                add("delayedResults = %M", listOfJsonStringsMember)
                add("(\n")
                indented {
                    captured.delayedResults
                        .map(::writeResultJson)
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
        .addKdoc("```json\n%L\n```", jsonObjectMapper.withPrettierPrinter().writeValueAsString(combinedResult))
        .build()
}

private fun makeConstructorInvocationToExpectedServiceCall(call: TestExecutionCapture.Call): CodeBlock {
    val listOfJsonStringsMember = MemberName("graphql.nadel.tests.next", "listOfJsonStrings")

    return buildCodeBlock {
        add("%T", ExpectedServiceCall::class.asClassName())
        add("(\n")
        indented {
            ExpectedServiceCall::query.name
            add("%L = %S,\n", ExpectedServiceCall::service.name, call.service)
            add("%L = %S,\n", ExpectedServiceCall::query.name, call.query.replaceIndent(" "))
            add("%L = %S,\n", ExpectedServiceCall::variables.name, writeResultJson(call.variables))
            add("%L = %S,\n", ExpectedServiceCall::result.name, writeResultJson(call.result))

            add("delayedResults = %M", listOfJsonStringsMember)
            add("(\n")
            indented {
                call.delayedResults
                    .map(::writeResultJson)
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
    if (result.isEmpty()) {
        return "{}"
    }

    return jsonObjectMapper
        .withPrettierPrinter()
        .writeValueAsString(result)
        .replaceIndent(" ")
}

private fun writeResultJson(result: ExecutionResult): String {
    return writeResultJson(result.toSpecification())
}

private fun writeResultJson(result: DelayedIncrementalPartialResult): String {
    return writeResultJson(result.toSpecification())
}

private fun classForNameOrNull(name: String): Class<*>? {
    return try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        null
    }
}

/**
 * Don't declare this as one string, it will turn off the formatter.
 *
 * Just don't touch it.
 */
private const val FORMATTER_OFF = "@formatter" + ":off"
