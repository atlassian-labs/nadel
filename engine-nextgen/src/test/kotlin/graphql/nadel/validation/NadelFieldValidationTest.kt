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
    }
})
