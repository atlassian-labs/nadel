package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.InvalidPathToPartitionArg
import graphql.nadel.validation.NadelSchemaValidationError.PathToPartitionArgMustBeSourceField
import graphql.nadel.validation.NadelSchemaValidationError.PathToPartitionArgNotList
import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test

private const val source = "$" + "source"

class NadelHydrationPartitionValidationTest {

    @Test
    fun `passes when pathToPartitionArg references a valid list argument`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                        authors: [User]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [{name: "id", value: "$source.authorIds"}]
                            identifiedBy: "id"
                            pathToPartitionArg: "id"
                        )
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID]): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID]): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assert(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `fails when pathToPartitionArg references a non-existent argument`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                        authors: [User]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [{name: "id", value: "$source.authorIds"}]
                            identifiedBy: "id"
                            pathToPartitionArg: "nonexistent"
                        )
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID]): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID]): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        val error = errors.assertSingleOfType<InvalidPathToPartitionArg>()
        assert(error.path == "nonexistent")
        assert(error.validArgs == listOf("id"))
    }

    @Test
    fun `fails when pathToPartitionArg references a static argument`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                        authors: [User]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "id", value: "$source.authorIds"}
                                {name: "type", value: "USER"}
                            ]
                            identifiedBy: "id"
                            pathToPartitionArg: "type"
                        )
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID], type: String): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorIds: [ID]
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        usersByIds(id: [ID], type: String): [User]
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        val error = errors.assertSingleOfType<PathToPartitionArgMustBeSourceField>()
        assert(error.path == "type")
    }

    @Test
    fun `fails when pathToPartitionArg references a non-list argument`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorId: ID
                        author: User
                        @hydrated(
                            service: "users"
                            field: "userById"
                            arguments: [{name: "id", value: "$source.authorId"}]
                            pathToPartitionArg: "id"
                        )
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                    type Query {
                        issues: [Issue]
                    }
                    type Issue {
                        id: ID
                        authorId: ID
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        val error = errors.assertSingleOfType<PathToPartitionArgNotList>()
        assert(error.path == "id")
    }
}
