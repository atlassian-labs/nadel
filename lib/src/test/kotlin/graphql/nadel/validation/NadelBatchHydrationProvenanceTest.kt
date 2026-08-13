package graphql.nadel.validation

import graphql.nadel.NadelSchemas.Companion.newNadelSchemas
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val source = "$" + "source"

class NadelBatchHydrationProvenanceTest {
    @Test
    fun `id hydration preserves its default hydration declaration`() {
        val blueprint = generateBlueprint(
            NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            assigneeId: ID @hidden
                            assignee: User @idHydrated(idField: "assigneeId")
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User @defaultHydration(field: "users", idArgument: "ids") {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            assigneeId: ID
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            ),
        )

        val instruction = blueprint.getBatchHydrationInstruction("Issue", "assignee")

        assertEquals(setOf("User"), instruction.defaultHydrationKeys.mapTo(mutableSetOf()) { it.declaringTypeName })
    }

    @Test
    fun `field declared hydration has no default hydration provenance`() {
        val blueprint = generateBlueprint(
            NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            assigneeId: ID @hidden
                            assignee: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [{name: "ids", value: "$source.assigneeId"}]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            assigneeId: ID
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            ),
        )

        val instruction = blueprint.getBatchHydrationInstruction("Issue", "assignee")

        assertTrue(instruction.defaultHydrationKeys.isEmpty())
    }

    @Test
    fun `equal union member default hydrations retain every declaration`() {
        val blueprint = generateBlueprint(
            NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            comments(ids: [ID!]!): [Comment]
                            users(ids: [ID!]!): [User]
                        }
                        type Comment {
                            id: ID!
                            commenterId: ID @hidden
                            commenter: Commenter @idHydrated(idField: "commenterId")
                        }
                        union Commenter = AtlassianAccountUser | CustomerUser | AppUser
                        interface User {
                            id: ID!
                        }
                        type AtlassianAccountUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                            id: ID!
                        }
                        type CustomerUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                            id: ID!
                        }
                        type AppUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            comments(ids: [ID!]!): [Comment]
                            users(ids: [ID!]!): [User]
                        }
                        type Comment {
                            id: ID!
                            commenterId: ID
                        }
                        union Commenter = AtlassianAccountUser | CustomerUser | AppUser
                        interface User {
                            id: ID!
                        }
                        type AtlassianAccountUser implements User {
                            id: ID!
                        }
                        type CustomerUser implements User {
                            id: ID!
                        }
                        type AppUser implements User {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            ),
        )

        val instruction = blueprint.getBatchHydrationInstruction("Comment", "commenter")

        assertEquals(
            setOf("AtlassianAccountUser", "CustomerUser", "AppUser"),
            instruction.defaultHydrationKeys.mapTo(mutableSetOf()) { it.declaringTypeName },
        )
    }
}

private fun generateBlueprint(fixture: NadelValidationTestFixture): NadelOverallExecutionBlueprint {
    val schemas = newNadelSchemas()
        .overallSchemas(fixture.overallSchema)
        .underlyingSchemas(fixture.underlyingSchema)
        .stubServiceExecution()
        .build()

    return NadelSchemaValidationFactory.create().validateAndGenerateBlueprint(schemas)
}

private fun NadelOverallExecutionBlueprint.getBatchHydrationInstruction(
    parentTypeName: String,
    fieldName: String,
): NadelBatchHydrationFieldInstruction {
    return fieldInstructions
        .get(parentTypeName, fieldName)
        .orEmpty()
        .filterIsInstance<NadelBatchHydrationFieldInstruction>()
        .single()
}
