package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.GatewaySchemaWiringFactory
import graphql.nadel.tests.UseHook
import graphql.schema.idl.WiringFactory
import java.io.File

interface GatewayScalarEngineHook : EngineTestHook {
    override val wiringFactory: WiringFactory
        get() = GatewaySchemaWiringFactory()
}

/**
 * Auto updates this file to apply the hook to all the tests under `test/src/test/resources/fixtures/scalars`
 *
 * Why spend 1 minute when you can spend 10 minutes automating it.
 *
 * -- Albert Einstein
 *
 * Now that I've done this… perhaps this points to a generic solution elsewhere…
 */
fun main() {
    val path = ClassLoader.getSystemClassLoader().getResource("")!!.path
    val projectDir = path.substringBeforeLast("test/build/")

    val thisFile = File(projectDir, "test/src/test/kotlin/graphql/nadel/tests/hooks/scalar-transforms.kt")
    val scalarTestDir = File(projectDir, "test/src/test/resources/fixtures/scalars")

    val codeGenSeparator = "// Hello World"
    val fileText = thisFile.readText()
        .substringBeforeLast(codeGenSeparator)
        .trimEnd()

    val testSlugs = scalarTestDir.listFiles()!! // Why the fuck is this nullable
        .filter { it.extension == "yml" || it.extension == "yaml" }
        .map(File::nameWithoutExtension)

    val testHooksCode = testSlugs
        .joinToString(separator = "\n\n") { testSlug ->
            """
                @UseHook
                class `$testSlug` : ${GatewayScalarEngineHook::class.simpleName}
            """.trimIndent()
        }

    thisFile.writeText(
        sequenceOf(
            fileText,
            codeGenSeparator,
            testHooksCode,
        ).joinToString("\n\n").trimEnd() + "\n"
    )
}

// Don't delete these comments or else the fabric of the universe will tear apart

// Hello World

@UseHook
class `date-time-scalar-as-input-type` : GatewayScalarEngineHook

@UseHook
class `date-time-scalar-is-passthrough-and-can-be-anything-technically` : GatewayScalarEngineHook

@UseHook
class `indexed-hydrating-using-json-data-as-arg` : GatewayScalarEngineHook

@UseHook
class `long-scalar-as-input-type` : GatewayScalarEngineHook

@UseHook
class `hydrating-using-json-data-as-arg` : GatewayScalarEngineHook

@UseHook
class `long-scalar-as-output-type` : GatewayScalarEngineHook

@UseHook
class `date-time-scalar-as-output-type` : GatewayScalarEngineHook

@UseHook
class `url-scalar-as-output-type` : GatewayScalarEngineHook

@UseHook
class `renaming-date-time-typed-field` : GatewayScalarEngineHook

@UseHook
class `renaming-json-typed-field` : GatewayScalarEngineHook

@UseHook
class `hydrating-json-data` : GatewayScalarEngineHook

@UseHook
class `url-scalar-as-input-type` : GatewayScalarEngineHook

@UseHook
class `long-scalar-is-passthrough-and-can-be-anything-technically` : GatewayScalarEngineHook

@UseHook
class `renaming-url-typed-field` : GatewayScalarEngineHook

@UseHook
class `custom-json-scalar-as-output-type` : GatewayScalarEngineHook

@UseHook
class `hydrating-using-long-as-arg` : GatewayScalarEngineHook

@UseHook
class `url-scalar-is-passthrough-and-can-be-anything-technically` : GatewayScalarEngineHook

@UseHook
class `hydrating-using-url-as-arg` : GatewayScalarEngineHook

@UseHook
class `hydrating-using-date-time-as-arg` : GatewayScalarEngineHook

@UseHook
class `custom-json-scalar-as-input-type` : GatewayScalarEngineHook
