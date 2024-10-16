package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

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
            errors.map { it.message }.shouldBeEmpty()
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingArgumentOnUnderlying>()
            error.parentType.overall.name.shouldBe("Query")
            error.parentType.underlying.name.shouldBe("Query")
            error.overallField.name.shouldBe("echo")
            error.subject.shouldBe(error.overallField)
            error.argument.name.shouldBe("world")
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
            errors.map { it.message }.shouldBeEmpty()
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
            errors.map { it.message }.shouldBeEmpty()
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            error.parentType.overall.name.shouldBe("Query")
            error.parentType.underlying.name.shouldBe("Query")
            error.overallInputArg.type.unwrapAll().name.shouldBe("Boolean")
            error.subject.shouldBe(error.overallInputArg)
            error.overallField.name.shouldBe("echo")
            error.overallInputArg.name.shouldBe("world")
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
                assert(errors.map { it.message }.isNotEmpty())

                val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
                error.parentType.overall.name.shouldBe("Query")
                error.parentType.underlying.name.shouldBe("Query")
                error.subject.shouldBe(error.overallInputArg)
                error.overallField.name.shouldBe("echo")
                error.overallInputArg.name.shouldBe("world")
                error.overallInputArg.type.unwrapAll().name.shouldBe(error.overallInputArg.type.unwrapAll().name)
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            error.parentType.overall.name.shouldBe("Query")
            error.parentType.underlying.name.shouldBe("Query")
            error.overallInputArg.type.unwrapAll().name.shouldBe("Boolean")
            error.subject.shouldBe(error.overallInputArg)
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentInputType>()
            error.parentType.overall.name.shouldBe("Query")
            error.parentType.underlying.name.shouldBe("Query")
            error.overallInputArg.type.unwrapAll().name.shouldBe("Boolean")
            error.underlyingInputArg.type.unwrapAll().name.shouldBe("String")
            error.subject.shouldBe(error.overallInputArg)
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            error.parentType.overall.name.shouldBe("Echo")
            error.parentType.underlying.name.shouldBe("Echo")
            error.overallField.name.shouldBe("hello")
            error.subject.shouldBe(error.overallField)
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
            errors.map { it.message }.shouldBeEmpty()
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            error.service.name.shouldBe("echo")
            error.parentType.overall.name.shouldBe("TestQuery")
            error.parentType.underlying.name.shouldBe("TestQuery")
            error.overallField.name.shouldBe("worlds")
            error.subject.shouldBe(error.overallField)
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
            assert(errors.map { it.message }.isNotEmpty())

            val queryError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestQuery"
            }
            queryError.service.name.shouldBe("echo")
            queryError.parentType.underlying.name.shouldBe("TestQuery")
            queryError.overallField.name.shouldBe("worlds")
            queryError.subject.shouldBe(queryError.overallField)

            val mutationError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestMutation"
            }
            mutationError.service.name.shouldBe("echo")
            mutationError.parentType.underlying.name.shouldBe("TestMutation")
            mutationError.overallField.name.shouldBe("active")
            mutationError.subject.shouldBe(mutationError.overallField)

            val subscriptionError = errors.assertSingleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestSubscription"
            }
            subscriptionError.service.name.shouldBe("issues")
            subscriptionError.parentType.underlying.name.shouldBe("TestSubscription")
            subscriptionError.overallField.name.shouldBe("issue")
            subscriptionError.subject.shouldBe(subscriptionError.overallField)

            errors.count { it is MissingUnderlyingField }.shouldBe(3)
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
            errors.shouldBeEmpty()
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
            errors.size.shouldBe(1)

            val error = errors.assertSingleOfType<NadelSchemaValidationError>()
            error.message.shouldBe("Could not find overall field TypeA.fieldFromExtension on the underlying type TypeA on service test")
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

            errors.size.shouldBe(1)

            val error = errors.assertSingleOfType<NadelSchemaValidationError>()

            error.message.shouldBe("The overall field Query.fieldA defines argument id which does not exist in service test field Query.fieldA")
        }
    }
})
