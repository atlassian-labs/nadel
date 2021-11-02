package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import io.kotest.core.spec.style.DescribeSpec

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
            assert(errors.map { it.message }.isEmpty())
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

            val error = errors.singleOfType<MissingArgumentOnUnderlying>()
            assert(error.parentType.overall.name == "Query")
            assert(error.parentType.underlying.name == "Query")
            assert(error.overallField.name == "echo")
            assert(error.subject == error.overallField)
            assert(error.argument.name == "world")
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

            val error = errors.singleOfType<MissingUnderlyingField>()
            assert(error.parentType.overall.name == "Echo")
            assert(error.parentType.underlying.name == "Echo")
            assert(error.overallField.name == "hello")
            assert(error.subject == error.overallField)
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
            assert(errors.map { it.message }.isEmpty())
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

            val error = errors.singleOfType<MissingUnderlyingField>()
            assert(error.service.name == "echo")
            assert(error.parentType.overall.name == "TestQuery")
            assert(error.parentType.underlying.name == "TestQuery")
            assert(error.overallField.name == "worlds")
            assert(error.subject == error.overallField)
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

            val queryError = errors.singleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestQuery"
            }
            assert(queryError.service.name == "echo")
            assert(queryError.parentType.underlying.name == "TestQuery")
            assert(queryError.overallField.name == "worlds")
            assert(queryError.subject == queryError.overallField)

            val mutationError = errors.singleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestMutation"
            }
            assert(mutationError.service.name == "echo")
            assert(mutationError.parentType.underlying.name == "TestMutation")
            assert(mutationError.overallField.name == "active")
            assert(mutationError.subject == mutationError.overallField)

            val subscriptionError = errors.singleOfType<MissingUnderlyingField> { error ->
                error.parentType.overall.name == "TestSubscription"
            }
            assert(subscriptionError.service.name == "issues")
            assert(subscriptionError.parentType.underlying.name == "TestSubscription")
            assert(subscriptionError.overallField.name == "issue")
            assert(subscriptionError.subject == subscriptionError.overallField)

            assert(errors.count { it is MissingUnderlyingField } == 3)
        }
    }
})
