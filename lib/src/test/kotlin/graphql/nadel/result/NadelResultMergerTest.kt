package graphql.nadel.result

import graphql.execution.RawVariables
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.engine.util.JsonMap
import graphql.normalized.ExecutableNormalizedOperationFactory
import graphql.normalized.ExecutableNormalizedOperationFactory.Options.defaultOptions
import graphql.parser.Parser
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class NadelResultMergerTest {
    private val schema = SchemaGenerator.createdMockedSchema(
        """
            directive @namespaced on FIELD_DEFINITION

            type Query {
                optional: String
                required: String!
                namespace: Namespace @namespaced
            }
            type Namespace {
                optional: String
                required: String!
            }
        """.trimIndent(),
    )

    @TestFactory
    fun `merges initial results without nulling omitted deferred fields`(): List<DynamicTest> {
        data class TestCase(
            val name: String,
            val query: String,
            val data: JsonMap,
            val expectedData: JsonMap?,
            val deferSupport: Boolean = true,
        )

        fun execute(testCase: TestCase) {
            // Given
            val operation = ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
                schema,
                Parser().parseDocument(testCase.query),
                null,
                RawVariables.emptyVariables(),
                defaultOptions().deferSupport(testCase.deferSupport),
            )
            val serviceResult = NadelServiceExecutionResultImpl(data = testCase.data.toMutableMap())

            // When
            val result = NadelResultMerger.mergeResults(operation.topLevelFields, schema, listOf(serviceResult))

            // Then
            assertEquals(testCase.expectedData, result.getData<JsonMap?>())
        }

        return listOf(
            TestCase(
                name = "omitted deferred non-null root",
                query = "{ optional ... @defer { required } }",
                data = mapOf("optional" to "value"),
                expectedData = mapOf("optional" to "value"),
            ),
            TestCase(
                name = "omitted deferred nullable root",
                query = "{ required ... @defer { optional } }",
                data = mapOf("required" to "value"),
                expectedData = mapOf("required" to "value"),
            ),
            TestCase(
                name = "omitted deferred aliased root",
                query = "{ optional ... @defer { result: required } }",
                data = mapOf("optional" to "value"),
                expectedData = mapOf("optional" to "value"),
            ),
            TestCase(
                name = "explicit null deferred non-null root",
                query = "{ optional ... @defer { required } }",
                data = mapOf("optional" to "value", "required" to null),
                expectedData = null,
            ),
            TestCase(
                name = "explicit null deferred nullable root",
                query = "{ required ... @defer { optional } }",
                data = mapOf("required" to "value", "optional" to null),
                expectedData = mapOf("required" to "value", "optional" to null),
            ),
            TestCase(
                name = "defer if false still requires non-null root",
                query = "{ optional ... @defer(if: false) { required } }",
                data = mapOf("optional" to "value"),
                expectedData = null,
            ),
            TestCase(
                name = "disabled defer support still requires non-null root",
                query = "{ optional ... @defer { required } }",
                data = mapOf("optional" to "value"),
                expectedData = null,
                deferSupport = false,
            ),
            TestCase(
                name = "missing non-deferred nullable root is null",
                query = "{ optional required }",
                data = mapOf("required" to "value"),
                expectedData = mapOf("optional" to null, "required" to "value"),
            ),
            TestCase(
                name = "omitted deferred namespaced child",
                query = "{ namespace { optional ... @defer { required } } }",
                data = mapOf("namespace" to mutableMapOf("optional" to "value")),
                expectedData = mapOf("namespace" to mapOf("optional" to "value")),
            ),
            TestCase(
                name = "typename is preserved when other namespaced children are deferred",
                query = "{ namespace { __typename ... @defer { required } } }",
                data = mapOf("namespace" to mutableMapOf("__typename" to "Namespace")),
                expectedData = mapOf("namespace" to mapOf("__typename" to "Namespace")),
            ),
            TestCase(
                name = "empty namespace is preserved when all children are deferred",
                query = "{ namespace { ... @defer { required } } }",
                data = mapOf("namespace" to mutableMapOf<String, Any?>()),
                expectedData = mapOf("namespace" to emptyMap<String, Any?>()),
            ),
            TestCase(
                name = "omitted deferred namespace",
                query = "{ optional ... @defer { namespace { required } } }",
                data = mapOf("optional" to "value"),
                expectedData = mapOf("optional" to "value"),
            ),
            TestCase(
                name = "explicit null deferred non-null namespaced child",
                query = "{ namespace { optional ... @defer { required } } }",
                data = mapOf("namespace" to mutableMapOf("optional" to "value", "required" to null)),
                expectedData = mapOf("namespace" to null),
            ),
            TestCase(
                name = "defer if false still requires non-null namespaced child",
                query = "{ namespace { optional ... @defer(if: false) { required } } }",
                data = mapOf("namespace" to mutableMapOf("optional" to "value")),
                expectedData = mapOf("namespace" to null),
            ),
            // These commented out tests describe expected behavior but they are bugged by the query representation
            // TestCase(
            //     name = "non-deferred selection of deferred root is still required",
            //     query = "{ optional required ... @defer { required } }",
            //     data = mapOf("optional" to "value"),
            //     expectedData = null,
            // ),
            // TestCase(
            //     name = "non-deferred selection after deferred root is still required",
            //     query = "{ optional ... @defer { required } required }",
            //     data = mapOf("optional" to "value"),
            //     expectedData = null,
            // ),
            // TestCase(
            //     name = "non-deferred selection of deferred namespaced child is still required",
            //     query = "{ namespace { optional required ... @defer { required } } }",
            //     data = mapOf("namespace" to mutableMapOf("optional" to "value")),
            //     expectedData = mapOf("namespace" to null),
            // ),
        ).map { testCase ->
            DynamicTest.dynamicTest(testCase.name) {
                execute(testCase)
            }
        }
    }
}
