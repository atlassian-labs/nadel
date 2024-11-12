package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.CannotRenamePartitionedField
import graphql.nadel.validation.NadelSchemaValidationError.InvalidPartitionArgument
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToFieldWithUnsupportedOutputType
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToSubscriptionField
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToUnsupportedField
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize

class NadelPartitionValidationTest : DescribeSpec({
    describe("validate") {
        context("passes if partition directive is applied in top-level field") {
            withData("type Query", "type Query { echo: String }  type Mutation") { operationPrepText ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         $operationPrepText {
                            users(ids: [ID!]!): [User!]! @partition(pathToPartitionArg: ["ids"])
                         }

                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         $operationPrepText {
                            users(ids: [ID!]!): [User!]!
                         }

                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.shouldBeEmpty()
            }

        }

        context("passes if partition directive is applied in namespaced field") {
            withData("type Query", "type Query { echo: String }  type Mutation") { operationPrepText ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                        $namespaceDirectiveDef
                         $operationPrepText {
                            userApi: UserApi @namespaced
                         }

                         type UserApi {
                             users(ids: [ID!]!): [User!]! @partition(pathToPartitionArg: ["ids"])
                         }
                         
                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         $operationPrepText {
                            userApi: UserApi 
                         }

                         type UserApi {
                             users(ids: [ID!]!): [User!]! 
                         }
                         
                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.shouldBeEmpty()
            }

        }

        it("fails when partition is applied to field in wrong location") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                         type Query {
                           user(id: ID!): User
                         }
                         
                         type User {
                            id: ID!
                            friends(ids: [ID!]!): [User!]! @partition(pathToPartitionArg: ["ids"])
                         }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                         type Query {
                           user(id: ID!): User
                         }
                         
                         type User {
                            id: ID!
                            friends(ids: [ID!]!): [User!]!
                         }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            errors.assertSingleOfType<PartitionAppliedToUnsupportedField>()
        }

        it("fails when partition is applied to Subscription type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                         type Query {
                            echo: String
                         }
                         
                         type Subscription {
                            users(ids: [ID!]!): [User!]! @partition(pathToPartitionArg: ["ids"])
                         }
                         
                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                         type Query {
                            echo: String
                         }
                         
                         type Subscription {
                            users(ids: [ID!]!): [User!]!
                         }
                         
                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            errors.assertSingleOfType<PartitionAppliedToSubscriptionField>()
        }

        it("passes when a nested split point is used") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                         type Query {
                            users(filter: UserFilter!): [User!]! @partition(pathToPartitionArg: ["filter", "ids"])
                         }
                         
                         input UserFilter {
                            ids: [ID!]!
                         }

                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                         type Query {
                            users(filter: UserFilter!): [User!]!
                         }
                         
                         input UserFilter {
                            ids: [ID!]!
                         }

                         type User {
                            id: ID!
                         }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            errors.shouldBeEmpty()

        }

        context("fails when output type is not a mutation payload") {
            withData(
                """
                type CreateUserRelationshipsPayload {
                    success: String # should be Boolean
                    users: [User!]!
                }
                """.trimIndent(),
                """
                type CreateUserRelationshipsPayload {
                    success: Boolean!
                    statusCode: String # should not be present 
                }
                """.trimIndent(),
                """
                type CreateUserRelationshipsPayload {
                    success: Boolean!
                    errors: String # should be a list 
                }
                """.trimIndent()
            ) { outputTypeDef ->

                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         type Query {
                             echo: String
                         }
                         
                         type Mutation {
                             createUserRelationships(input: UserRelationshipInput!): CreateUserRelationshipsPayload! 
                             @partition(pathToPartitionArg: ["input", "ids"])
                         }
                         
                         input UserRelationshipInput {
                             ids: [ID!]!
                         }
                         $outputTypeDef
                         type User {
                             id: ID!
                         }
                         
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         type Query {
                             echo: String
                         }
                         
                         type Mutation {
                             createUserRelationships(input: UserRelationshipInput!): CreateUserRelationshipsPayload! 
                         }
                         
                         input UserRelationshipInput {
                             ids: [ID!]!
                         }
                         $outputTypeDef
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.assertSingleOfType<PartitionAppliedToFieldWithUnsupportedOutputType>()
            }
        }

        context("passes when output type is a mutation payload") {
            withData(
                """
                type CreateUserRelationshipsPayload {
                    success: Boolean!
                    errors: [String!]!
                    users: [User!]!
                }
                """.trimIndent(),
                """
                type CreateUserRelationshipsPayload {
                    success: Boolean
                    users: [User]
                }
                """.trimIndent(),
                """
                type CreateUserRelationshipsPayload {
                    success: Boolean!
                }
                """.trimIndent(),
            ) { outputTypeDef ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         type Query {
                             echo: String
                         }
                         
                         type Mutation {
                             createUserRelationships(input: UserRelationshipInput!): CreateUserRelationshipsPayload! 
                             @partition(pathToPartitionArg: ["input", "ids"])
                         }
                         
                         input UserRelationshipInput {
                             ids: [ID!]!
                         }
                         $outputTypeDef
                         type User {
                             id: ID!
                         }
                         
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         type Query {
                             echo: String
                         }
                         
                         type Mutation {
                             createUserRelationships(input: UserRelationshipInput!): CreateUserRelationshipsPayload! 
                         }
                         
                         input UserRelationshipInput {
                             ids: [ID!]!
                         }
                         $outputTypeDef
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.isEmpty()
            }
        }

        context("fails when return type is incorrect") {
            withData("String", "String!", "User") { returnType ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         type Query {
                             users(ids: [ID!]!): $returnType @partition(pathToPartitionArg: ["ids"])
                         }

                         type User {
                             id: ID!
                             name: String
                         }
                         
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         type Query {
                             users(ids: [ID!]!): $returnType
                         }

                         type User {
                             id: ID!
                             name: String
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.assertSingleOfType<PartitionAppliedToFieldWithUnsupportedOutputType>()
            }
        }

        it("fails when partition is applied to renamed field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                         type Query {
                             usersRenamed(ids: [ID!]!): [User!]! 
                             @partition(pathToPartitionArg: ["ids"])
                             @renamed(from: "users")
                         }

                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                         type Query {
                             users(ids: [ID!]!): [User!]!
                         }

                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            errors.assertSingleOfType<CannotRenamePartitionedField>()
        }

        context("fails when pathToPartitionArg value is invalid") {
            withData("[]", """["extra", "ids"]""", """["ids", "extra"]""", """["ids_wrong"]""") { argValue ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         type Query {
                             users(ids: [ID!]!): [User!]! @partition(pathToPartitionArg: $argValue)
                         }
                         
                         input UsersInput {
                             ids: [ID!]!
                         }
                         
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         type Query {
                             users(ids: [ID!]!): [User!]!
                         }
                         
                         input UsersInput {
                             ids: [ID!]!
                         }
                         
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.assertSingleOfType<InvalidPartitionArgument>()
            }
        }

        context("fails when partition argument is not a list") {
            withData("String", "String!", "ID", "UsersInput") { argumentDef ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                         type Query {
                             users(arg: $argumentDef): [User!]! @partition(pathToPartitionArg: ["arg"])
                         }
                         
                         input UsersInput {
                             ids: [ID!]!
                         }
                         
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                         type Query {
                             users(arg: $argumentDef): [User!]!
                         }
                         
                         input UsersInput {
                             ids: [ID!]!
                         }
                         
                         type User {
                             id: ID!
                         }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                errors.assertSingleOfType<InvalidPartitionArgument>()
            }
        }

        it("only returns first error if there are multiple") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                         type Query {
                             user(id: ID!): User
                         }

                         type User {
                             id: ID!
                             name: String
                             bestFriend(id: ID!): User! @partition(pathToPartitionArg: ["id"])
                         }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                         type Query {
                             user(id: ID!): User
                         }

                         type User {
                             id: ID!
                             name: String
                             bestFriend(id: ID!): User! 
                             partner(id: ID!): User! 
                         }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)

            errors.shouldHaveSize(1)
            errors.shouldExist { it is PartitionAppliedToUnsupportedField }
        }
    }
})
