package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import io.kotest.core.spec.style.DescribeSpec

val renameDirectiveDef = """
    directive @renamed(
        "The type to be renamed"
        from: String!
    ) on SCALAR | OBJECT | FIELD_DEFINITION | INTERFACE | UNION | ENUM | INPUT_OBJECT
""".trimIndent()

class NadelRenameValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if rename references existing underlying field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            test: String @renamed(from: "echo")
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
            assert(errors.isEmpty())
        }

        it("passes if rename references existing underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            echo: String
                        }
                        type User @renamed(from: "Account") {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Account {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isEmpty())
        }

        it("raises error if type is renamed twice") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            echo: String
                        }
 
                        type User @renamed(from: "Account") {
                            id: ID!
                        }

                        type Profile @renamed(from: "Account") {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Account {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<DuplicatedUnderlyingType>()
            assert(error.service.name == "test")
            assert(error.duplicates.map { it.overall.name }.toSet() == setOf("User", "Profile"))
            assert(error.duplicates.map { it.underlying.name }.toSet().singleOrNull() == "Account")
        }

        it("allows fields to be duplicated") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                            greet: String @renamed(from: "echo")
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            key: ID! @renamed(from: "id")
                            link: URL!
                        }
                        scalar URL
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            link: URL!
                        }
                        scalar URL
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        // When we say implicitly renamed, we refer to the fact that the output type of the field
        // is not the same as the underlying type and that the type is shared
        // i.e. the service does not explicitly define a rename
        it("raises error if type is implicitly renamed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            echo: String
                        }
 
                        type Test {
                            id: ID!
                        }
                    """.trimIndent(),
                    "accounts" to """
                        type Query {
                            account: Test
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Test {
                            id: ID!
                        }
                    """.trimIndent(),
                    "accounts" to """
                        type Query {
                            account: Account
                        }
                        type Account {
                            id: ID!
                        }
                        type Test {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val incompatibleTypeName = errors.singleOfType<IncompatibleFieldOutputType>()
            assert(incompatibleTypeName.parentType.overall.name == "Query")
            assert(incompatibleTypeName.parentType.underlying.name == "Query")
            assert(incompatibleTypeName.overallField.type.unwrapAll().name == "Test")
            assert(incompatibleTypeName.underlyingField.type.unwrapAll().name == "Account")
        }

        xit("passes if field uses renamed shared type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Test {
                          me: Account
                        }
                        extend type Account @renamed(from: "Profile")
                    """.trimIndent(),
                    "accounts" to """
                        type Account @renamed(from: "User") {
                            name: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Test {
                            me: Profile 
                        }
                        type Profile {
                            name: String
                        }
                    """.trimIndent(),
                    "accounts" to """
                        type Query {
                            echo: String
                        }
                        type User {
                            name: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())
        }

        it("raises error if renamed field references non existent underlying field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            test: String @renamed(from: "junk")
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

            val error = errors.singleOfType<MissingRename>()
            assert(error.service.name == "test")
            assert(error.subject.name == "test")
            assert(error.overallField.name == "test")
            assert(error.parentType.overall.name == "Query")
            assert(error.parentType.underlying.name == "Query")
        }

        it("raises error if renamed type references non existent underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            echo: String
                        }
 
                        type User @renamed(from: "Test") {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                        type Account {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingUnderlyingType>()
            assert(error.subject.name == "User")
            assert(error.overallType.name == "User")
        }
    }
})
