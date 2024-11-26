package graphql.nadel.schema

import graphql.GraphQLException
import graphql.nadel.NadelOperationKind
import graphql.nadel.NadelOperationKind.Mutation
import graphql.nadel.NadelOperationKind.Query
import graphql.nadel.NadelOperationKind.Subscription
import graphql.nadel.NadelSchemas
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLObjectType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class OverallSchemaGeneratorTest {
    private val fooType = """
        type Foo {
            name: String
        }
    """.trimIndent()

    private val barType = """
        type Bar {
            id: String
        }
    """.trimIndent()

    private val fooServiceDefaultQuery = """
        type Query {
            foo: Foo
        }
    """.trimIndent()

    private val fooServiceQueryInSchema = """
        schema {
            query: fooQuery
        }
        type fooQuery {
            foo: Foo
        }
    """.trimIndent()

    private val fooQueryExtension = """
        extend type Query {
            foo2: Foo
        }
    """.trimIndent()

    private val fooServiceDefaultMutation = """
        type Mutation {
            setFoo(name: String): Foo
        }
        type Query {
            hello: String
        }
    """.trimIndent()

    private val fooServiceMutationInSchema = """
        schema {
            mutation: fooMutation
            query: Query
        }
        type Query {
            foo: String
        }
        type fooMutation {
            setFoo(name: String): Foo
        }
    """.trimIndent()

    private val fooMutationExtension = """
        extend type Mutation {
            setFoo2(name: String): Foo
        }
    """.trimIndent()

    private val fooServiceDefaultSubscription = """
        type Subscription {
            subFoo: Foo
        }
        type Query {
            foo: String
        }
    """.trimIndent()

    private val fooServiceSubscriptionInSchema = """
        schema {
            subscription: fooSubscription
            query: MyQuery
        }
        type MyQuery {
            foo: String
        }
        type fooSubscription {
            subFoo: Foo
        }
    """.trimIndent()

    private val fooSubscriptionExtension = """
        extend type Subscription {
            subFoo2: Foo
        }
    """.trimIndent()

    private val barServiceDefaultQuery = """
        type Query {
            bar: Bar
        }
    """.trimIndent()

    private val barServiceWithDirectives = """
        directive @cloudId2(type: String) on ARGUMENT_DEFINITION
        type Query {
            bar(id: ID! @cloudId2(type: "ari")): Bar
        }
    """.trimIndent()

    private val barServiceQueryInSchema = """
        schema {
            query: barQuery
        }
        type barQuery {
            bar: Bar
        }
    """.trimIndent()

    private val barQueryExtension = """
        extend type Query {
            bar2: Bar
        }
    """.trimIndent()

    private val barServiceDefaultMutation = """
        type Mutation {
            setBar(id: String): Bar
        }
    """.trimIndent()

    private val barServiceMutationInSchema = """
        schema {
            mutation: barMutation
        }
        type barMutation {
            setBar(id: String): Bar
        }
    """.trimIndent()

    private val barMutationExtension = """
        extend type Mutation {
            setBar2(id: String): Bar
        }
    """.trimIndent()

    private val barServiceDefaultSubscription = """
        type Subscription {
            subBar: Bar
        }
    """.trimIndent()

    private val barServiceSubscriptionInSchema = """
        schema {
            subscription: barSubscription
        }
        type barSubscription {
            subBar: Bar
        }
    """.trimIndent()

    private val barSubscriptionExtension = """
        extend type Subscription {
            subBar2: Bar
        }
    """.trimIndent()

    @TestFactory
    fun `can merge operation types`(): List<DynamicTest> {
        data class TestCase(
            val operationKind: NadelOperationKind,
            val display: String,
            val fooService: String,
            val barService: String,
            val expectedOperationFields: Set<String>,
        )

        return listOf(
            TestCase(
                Query,
                display = "both services with default definition",
                fooService = "$fooServiceDefaultQuery $fooType",
                barService = "$barServiceDefaultQuery $barType",
                expectedOperationFields = setOf("foo", "bar"),
            ),
            TestCase(
                Query,
                display = "one service with default definition and one service defined in schema",
                fooService = "$fooServiceQueryInSchema $fooType",
                barService = "$barServiceDefaultQuery $barType",
                expectedOperationFields = setOf("foo", "bar"),
            ),
            TestCase(
                Query,
                display = "one service with default definition and one service defined in schema and extension",
                fooService = "$fooServiceQueryInSchema $fooType $fooQueryExtension",
                barService = "$barServiceDefaultQuery $barType",
                expectedOperationFields = setOf("foo", "foo2", "bar"),
            ),
            TestCase(
                Query,
                display = "both services with definition in schema",
                fooService = "$fooServiceQueryInSchema $fooType",
                barService = "$barServiceQueryInSchema $barType",
                expectedOperationFields = setOf("foo", "bar"),
            ),
            TestCase(
                Query,
                display = "both services with definition in schema and extension",
                fooService = "$fooServiceQueryInSchema $fooType $fooQueryExtension",
                barService = "$barServiceQueryInSchema $barType, $barQueryExtension",
                expectedOperationFields = setOf("foo", "foo2", "bar", "bar2"),
            ),
            TestCase(
                Mutation,
                display = "both services with default definition",
                fooService = "$fooServiceDefaultMutation $fooType",
                barService = "$barServiceDefaultMutation $barType",
                expectedOperationFields = setOf("setFoo", "setBar"),
            ),
            TestCase(
                Mutation,
                display = "one service with default definition and one service defined in schema",
                fooService = "$fooServiceMutationInSchema $fooType",
                barService = "$barServiceDefaultMutation $barType",
                expectedOperationFields = setOf("setFoo", "setBar"),
            ),
            TestCase(
                Mutation,
                display = "one service with default definition and one service defined in schema and extension",
                fooService = "$fooServiceMutationInSchema $fooType $fooMutationExtension",
                barService = "$barServiceDefaultMutation $barType",
                expectedOperationFields = setOf("setFoo", "setFoo2", "setBar"),
            ),
            TestCase(
                Mutation,
                display = "both services with definition in schema",
                fooService = "$fooServiceMutationInSchema $fooType",
                barService = "$barServiceMutationInSchema $barType",
                expectedOperationFields = setOf("setFoo", "setBar"),
            ),
            TestCase(
                Mutation,
                display = "both services with definition in schema and extension",
                fooService = "$fooServiceMutationInSchema $fooType $fooMutationExtension",
                barService = "$barServiceMutationInSchema $barType $barMutationExtension",
                expectedOperationFields = setOf("setFoo", "setFoo2", "setBar", "setBar2"),
            ),
            TestCase(
                Subscription,
                display = "both services with default definition",
                fooService = "$fooServiceDefaultSubscription $fooType",
                barService = "$barServiceDefaultSubscription $barType",
                expectedOperationFields = setOf("subFoo", "subBar"),
            ),
            TestCase(
                Subscription,
                display = "one service with default definition and one service defined in schema",
                fooService = "$fooServiceSubscriptionInSchema $fooType",
                barService = "$barServiceDefaultSubscription $barType",
                expectedOperationFields = setOf("subFoo", "subBar"),
            ),
            TestCase(
                Subscription,
                display = "one service with default definition and one service defined in schema and extension",
                fooService = "$fooServiceSubscriptionInSchema $fooType $fooSubscriptionExtension",
                barService = "$barServiceDefaultSubscription $barType",
                expectedOperationFields = setOf("subFoo", "subFoo2", "subBar"),
            ),
            TestCase(
                Subscription,
                display = "both services with definition in schema",
                fooService = "$fooServiceSubscriptionInSchema $fooType",
                barService = "$barServiceSubscriptionInSchema $barType",
                expectedOperationFields = setOf("subFoo", "subBar"),
            ),
            TestCase(
                Subscription,
                display = "both services with definition in schema and extension",
                fooService = "$fooServiceSubscriptionInSchema $fooType $fooSubscriptionExtension",
                barService = "$barServiceSubscriptionInSchema $barType $barSubscriptionExtension",
                expectedOperationFields = setOf("subFoo", "subFoo2", "subBar", "subBar2"),
            ),
        ).map { testCase ->
            DynamicTest.dynamicTest(testCase.display) {
                // when
                val schemas = NadelSchemas.newNadelSchemas()
                    .overallSchema("Foo", testCase.fooService)
                    .overallSchema("Bar", testCase.barService)
                    .underlyingSchema("Foo", "type Query {echo: String}")
                    .underlyingSchema("Bar", "type Query {echo: String}")
                    .stubServiceExecution()
                    .build()
                val operationKind = testCase.operationKind.getType(schemas.engineSchema)

                // then
                assert(operationKind != null)

                val operationTypeFields = operationKind!!
                    .children
                    .asSequence()
                    .map { it as GraphQLNamedSchemaElement }
                    .map { it.name }
                    .toSet()
                val expectedFields = testCase.expectedOperationFields

                assertTrue(operationTypeFields == expectedFields)
            }
        }
    }

    @Test
    fun `does not generate absent subscription operation type`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema("Foo", "type Query {hello: String} type Mutation {hello: String}")
            .underlyingSchema("Foo", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.mutationType != null)
        assert(schema.subscriptionType == null)
    }

    @Test
    fun `does not generate absent mutation and subscription operation types`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema("Foo", "type Query {hello: String}")
            .underlyingSchema("Foo", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.mutationType == null)
        assert(schema.subscriptionType == null)
    }

    @Test
    fun `extending types created merged type with all fields`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema(
                "S1",
                """
                    type Query {
                        a: String
                    }
                    extend type Query {
                        b: String
                    }
                    type A {
                        x: String
                    }
                    extend type A {
                        y: String
                    }
                """.trimIndent(),
            )
            .overallSchema(
                "S2",
                """
                    type Query {
                        c: String
                    }
                    extend type Query {
                        d: String
                    }
                    extend type A {
                        z: String
                    }
                """.trimIndent(),
            )
            .underlyingSchema("S1", "type Query {echo: String}")
            .underlyingSchema("S2", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.queryType.fields.mapTo(LinkedHashSet()) { it.name } == setOf("a", "b", "c", "d"))
        val aType = schema.getType("A") as GraphQLObjectType
        assert(aType.fields.mapTo(LinkedHashSet()) { it.name } == setOf("x", "y", "z"))
    }

    @Test
    fun `can extend Query type without explicit Query definition`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema(
                "S1",
                """
                    extend type Query {
                        a: String
                    }
                    extend type Query {
                        b: String
                    }
                """.trimIndent(),
            )
            .overallSchema(
                "S2",
                """
                    extend type Query {
                        c: String
                    }
                """.trimIndent(),
            )
            .underlyingSchema("S1", "type Query {echo: String}")
            .underlyingSchema("S2", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.queryType.fields.mapTo(LinkedHashSet()) { it.name } == setOf("a", "b", "c"))
    }

    @Test
    fun `forbids type redefinition`() {
        // when
        val ex = assertThrows<GraphQLException> {
            NadelSchemas.newNadelSchemas()
                .overallSchema(
                    "S1",
                    """
                        type Query {
                            a: String
                        }
                        type A {
                        }
                    """.trimIndent()
                )
                .overallSchema(
                    "S2",
                    """
                        type Query {
                            c: String
                        }
                        type A {
                            x: String
                        }
                    """.trimIndent()
                )
                .underlyingSchema("S1", "type Query {echo: String}")
                .underlyingSchema("S2", "type Query {echo: String}")
                .stubServiceExecution()
                .build()
                .engineSchema
        }

        // then
        assert(ex.message?.contains("tried to redefine existing 'A' type") == true)
    }

    @Test
    fun `allows services to declare operation types with same name`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema(
                "S1",
                """
                    schema {
                        query: MyQuery
                    }
                    type MyQuery {
                        a: String
                    }
                    type A {
                        x: String
                    }
                """.trimIndent(),
            )
            .overallSchema(
                "S2",
                """
                    schema {
                        query: MyQuery
                    }
                    type MyQuery {
                        c: String
                    }
                """.trimIndent(),
            )
            .underlyingSchema("S1", "type Query {echo: String}")
            .underlyingSchema("S2", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.queryType.fields.mapTo(LinkedHashSet()) { it.name } == setOf("a", "c"))
        val aType = schema.typeMap["A"] as GraphQLObjectType
        assert(aType.fields.mapTo(LinkedHashSet()) { it.name } == setOf("x"))
    }

    @Test
    fun `adds built-in directives to schema if absent`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema(
                "S1",
                """
                    type Query {
                        a: String
                    }
                    type A {
                        x: String
                    }
                """.trimIndent(),
            )
            .overallSchema(
                "S2",
                """
                    type Query {
                        c: String
                    }
                    extend type A {
                        y: String
                    }
                """.trimIndent(),
            )
            .underlyingSchema("S1", "type Query {echo: String}")
            .underlyingSchema("S2", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.getDirective(NadelDirectives.hydratedDirectiveDefinition.name) != null)
        assert(schema.getDirective(NadelDirectives.renamedDirectiveDefinition.name) != null)
        assert(schema.getType(NadelDirectives.nadelHydrationArgumentDefinition.name) != null)
    }

    @Test
    fun `permits explicit definitions of built-in directives`() {
        // when
        val schema = NadelSchemas.newNadelSchemas()
            .overallSchema(
                "S1",
                """
                    type Query {
                        a: String
                    }
                    type A {
                        x: String
                    }
                    input NadelHydrationArgument {
                        name: String!
                        value: String!
                    }

                    directive @hydrated(
                        arguments: [NadelHydrationArgument!]
                        batchSize: Int = 200
                        field: String!
                        identifiedBy: String! = "id"
                        indexed: Boolean = false
                        service: String!
                    ) repeatable on FIELD_DEFINITION

                    directive @renamed(
                        from: String!
                    ) on SCALAR | OBJECT | FIELD_DEFINITION | INTERFACE | UNION | ENUM | INPUT_OBJECT
                """.trimIndent(),
            )
            .overallSchema(
                "S2",
                """
                    type Query {
                        c: String
                    }
                    extend type A {
                        y: String
                    }
                """.trimIndent(),
            )
            .underlyingSchema("S1", "type Query {echo: String}")
            .underlyingSchema("S2", "type Query {echo: String}")
            .stubServiceExecution()
            .build()
            .engineSchema

        // then
        assert(schema.getDirective(NadelDirectives.hydratedDirectiveDefinition.name) != null)
        assert(schema.getDirective(NadelDirectives.renamedDirectiveDefinition.name) != null)
        assert(schema.getType(NadelDirectives.nadelHydrationArgumentDefinition.name) != null)
    }
}
