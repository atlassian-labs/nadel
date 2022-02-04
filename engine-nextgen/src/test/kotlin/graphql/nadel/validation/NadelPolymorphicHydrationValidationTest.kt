package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.FieldWithPolymorphicHydrationMustReturnAUnion
import graphql.nadel.validation.NadelSchemaValidationError.PolymorphicHydrationReturnTypeMismatch
import graphql.schema.GraphQLNamedType
import io.kotest.core.spec.style.DescribeSpec

private const val source = "$" + "source"

class NadelPolymorphicHydrationValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if polymorphic hydration is valid") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )    
                        }
                        union AbstractUser = User | ExternalUser
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                        
                        type ExternalUser {
                            id: ID!
                            name: String!
                            metadata: UserMetadata
                        }
                        
                        type UserMetadata {
                            payload: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                            metadata: UserMetadata
                        }
                        
                        type UserMetadata {
                            payload: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("passes if polymorphic hydration is valid when actor field returns a renamed type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )    
                        }
                        union AbstractUser = InternalUser | ExternalUser
                    """.trimIndent(),
                    "users" to """
                        type InternalUser @renamed(from: "User") {
                            id: ID!
                            name: String!
                        }
                        
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails if return type is not a union") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: User
                            @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                        union AbstractUser = User | ExternalUser
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }

                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())
            val error = errors.filterIsInstance<FieldWithPolymorphicHydrationMustReturnAUnion>().single()
            assert(error.parentType.overall.name == "Issue")
            assert(error.overallField.name == "creator")
        }

        it("fails if one of the hydrations' return types is not in the union") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                        union AbstractUser = User
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }

                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())
            val error = errors.singleOfType<PolymorphicHydrationReturnTypeMismatch>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.actorField.name == "externalUser")
            assert(error.actorService.name == "users")
            assert((error.actorField.type as GraphQLNamedType).name == "ExternalUser")
        }
    }
})

