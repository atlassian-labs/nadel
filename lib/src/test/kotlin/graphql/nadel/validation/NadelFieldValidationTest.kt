package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import kotlin.test.assertTrue

val namespaceDirectiveDef = """
    directive @namespaced on FIELD_DEFINITION
""".trimIndent()

class NadelFieldValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if schema is valid") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isEmpty())
        }

        it("fails if argument value is missing") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingArgumentOnUnderlying>()
            assertTrue(error.parentType.overall.name == "Query")
            assertTrue(error.parentType.underlying.name == "Query")
            assertTrue(error.overallField.name == "echo")
            assertTrue(error.subject == error.overallField)
            assertTrue(error.argument.name == "world")
        }

        it("passes if overall argument value is stricter") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean!): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean): String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isEmpty())
        }

        it("passes if overall argument value is stricter with more wrappings") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: [[Boolean]!]): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: [[Boolean]]): String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isEmpty())
        }

        it("passes if overall argument value is stricter with more wrappings 2") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: [Boolean]): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: [Boolean]!): String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            assertTrue(error.parentType.overall.name == "Query")
            assertTrue(error.parentType.underlying.name == "Query")
            assertTrue(error.overallInputArg.type.unwrapAll().name == "Boolean")
            assertTrue(error.subject == error.overallInputArg)
            assertTrue(error.overallField.name == "echo")
            assertTrue(error.overallInputArg.name == "world")
        }

        context("fails if argument type list wrappings are not equal") {
            withData(
                nameFn = { (underlying, overall) -> "Underlying=$underlying, overall=$overall" },
                "Boolean" to "[Boolean]",
                "Boolean!" to "[Boolean]",
                "[Boolean]" to "Boolean",
                "[Boolean]" to "Boolean!",
                "[[Boolean]]" to "[Boolean]",
                "[Boolean]" to "[[Boolean]]",
                "[Boolean!]" to "[[Boolean]]",
                "[[Boolean]]" to "[Boolean!]",
            ) { (underlying, overall) ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                        type Query {
                            echo(world: $overall): String
                        }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                        type Query {
                            echo(world: $underlying): String
                        }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)
                assertTrue(errors.map { it.message }.isNotEmpty())

                val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
                assertTrue(error.parentType.overall.name == "Query")
                assertTrue(error.parentType.underlying.name == "Query")
                assertTrue(error.subject == error.overallInputArg)
                assertTrue(error.overallField.name == "echo")
                assertTrue(error.overallInputArg.name == "world")
                assertTrue(error.overallInputArg.type.unwrapAll().name == error.overallInputArg.type.unwrapAll().name)
            }
        }

        it("fails if underlying argument value is stricter") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean!): String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            assertTrue(error.parentType.overall.name == "Query")
            assertTrue(error.parentType.underlying.name == "Query")
            assertTrue(error.overallInputArg.type.unwrapAll().name == "Boolean")
            assertTrue(error.subject == error.overallInputArg)
        }

        it("fails if argument value is not matching") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: Boolean): String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo(world: String): String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            assertTrue(error.parentType.overall.name == "Query")
            assertTrue(error.parentType.underlying.name == "Query")
            assertTrue(error.overallInputArg.type.unwrapAll().name == "Boolean")
            assertTrue(error.underlyingInputArg.type.unwrapAll().name == "String")
            assertTrue(error.subject == error.overallInputArg)
        }

        it("checks the output type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Echo {
                            hello: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            ignore: String
                        }
                        type Echo {
                            hello: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                        }
                        type Echo {
                            world: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            assertTrue(error.parentType.overall.name == "Echo")
            assertTrue(error.parentType.underlying.name == "Echo")
            assertTrue(error.overallField.name == "hello")
            assertTrue(error.subject == error.overallField)
        }

        it("handles types whose fields are contributed from multiple services") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to namespaceDirectiveDef,
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery @namespaced
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                        type Echo {
                            hello: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                        }
                        extend type TestQuery {
                            worlds: [String]
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            shared: String
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                            testing: TestQuery
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            worlds: [String]
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isEmpty())
        }

        it("fails if field in namespace does not exist") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to namespaceDirectiveDef,
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery @namespaced
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                        type Echo {
                            hello: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                        }
                        extend type TestQuery {
                            worlds: [String]
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            shared: String
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                            testing: TestQuery
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            world: [String]
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            assertTrue(error.service.name == "echo")
            assertTrue(error.parentType.overall.name == "TestQuery")
            assertTrue(error.parentType.underlying.name == "TestQuery")
            assertTrue(error.overallField.name == "worlds")
            assertTrue(error.subject == error.overallField)
        }

        it("checks mutation and subscription namespaced fields") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to namespaceDirectiveDef,
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery @namespaced
                        }
                        type Mutation {
                            testing: TestMutation @namespaced
                        }
                        type TestMutation {
                            setUserAgent(agent: String): String
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                        type Echo {
                            hello: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        extend type TestSubscription {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            description: String!
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                        }
                        extend type TestMutation {
                            active(active: Boolean): Boolean
                        }
                        extend type TestQuery {
                            worlds: [String]
                        }
                        type Subscription {
                            testing: TestSubscription @namespaced
                        }
                        type TestSubscription {
                            echoes: [String]
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            shared: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        scalar Nothing
                        type Query {
                            nothing: Nothing
                        }
                        type Subscription {
                            testing: TestSubscription
                        }
                        type TestSubscription {
                            issueUpdates(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            description: String!
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            test: String
                            testing: TestQuery
                        }
                        type Mutation {
                            testing: TestMutation
                        }
                        type TestMutation {
                            setUserAgent(agent: String): String
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            diagnostic: String
                        }
                    """.trimIndent(),
                    "echo" to """
                        type Query {
                            echo(world: Boolean): Echo
                            testing: TestQuery
                        }
                        type Mutation {
                            testing: TestMutation
                        }
                        type Subscription {
                            testing: TestSubscription
                        }
                        type TestSubscription {
                            echoes: [String]
                        }
                        type TestMutation {
                            setActive(active: Boolean): Boolean
                        }
                        type Echo {
                            hello: String
                        }
                        type TestQuery {
                            world: [String]
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val queryError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestQuery"
            }
            assertTrue(queryError.service.name == "echo")
            assertTrue(queryError.parentType.underlying.name == "TestQuery")
            assertTrue(queryError.overallField.name == "worlds")
            assertTrue(queryError.subject == queryError.overallField)

            val mutationError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestMutation"
            }
            assertTrue(mutationError.service.name == "echo")
            assertTrue(mutationError.parentType.underlying.name == "TestMutation")
            assertTrue(mutationError.overallField.name == "active")
            assertTrue(mutationError.subject == mutationError.overallField)

            val subscriptionError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestSubscription"
            }
            assertTrue(subscriptionError.service.name == "issues")
            assertTrue(subscriptionError.parentType.underlying.name == "TestSubscription")
            assertTrue(subscriptionError.overallField.name == "issue")
            assertTrue(subscriptionError.subject == subscriptionError.overallField)

            assertTrue(errors.count { it is MissingUnderlyingField } == 3)
        }

        it("passes if hydrated type is not present in underlying") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            fieldA(id: ID!): TypeA
                        }

                        type TypeA {
                            echo: String
                        }
                    """.trimIndent(),
                    "test-2" to """
                        type Query {
                            fieldB: TypeB
                        }

                        type TypeB {
                            fieldA: TypeA @hydrated(
                                service: "test",
                                field: "fieldA",
                                arguments : [{ name:"id", value:"${'$'}source.aId"}],
                            )
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            fieldA(id: ID!): TypeA
                        }

                        type TypeA {
                            echo: String
                        }
                    """.trimIndent(),
                    "test-2" to """
                        type Query {
                            fieldB: TypeB
                        }

                        type TypeB {
                            aId: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.isEmpty())
        }

        it("fails if fields declared in extensions are missing in underlying") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            fieldA(id: ID!): TypeA
                        }

                        type TypeA {
                            echo: String
                        }

                        extend type TypeA {
                            fieldFromExtension: String
                        }

                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            fieldA(id: ID!): TypeA
                        }

                        type TypeA {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.size == 1)

            val error = errors.assertSingleOfType<NadelSchemaValidationError>()
            assertTrue(error.message == "Could not find overall field TypeA.fieldFromExtension on the underlying type TypeA on service test")
        }

        it("fails if fields in extended Query are missing in underlying") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        extend type Query {
                            fieldA(id: ID!): String
                        }

                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            fieldA: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            assertTrue(errors.size == 1)

            val error = errors.assertSingleOfType<NadelSchemaValidationError>()

            assertTrue(error.message == "The overall field Query.fieldA defines argument id which does not exist in service test field Query.fieldA")
        }

        it("fails if schema contains all fields as hidden") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String @hidden
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<NadelSchemaValidationError>()
            assertTrue(error.message == "Must have at least one field without @hidden on type Query")
        }

        it("passes if schema contains at least one field as not hidden") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String @hidden
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.isEmpty())
        }

        it("passes if all fields are hidden and their container type is deleted") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            hello: String
                            other: Other @hidden
                        }
                        type Other {
                            id: ID! @hidden
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            hello: String
                            other: Other
                        }
                        type Other {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assertTrue(errors.isEmpty())
        }
    }
})
