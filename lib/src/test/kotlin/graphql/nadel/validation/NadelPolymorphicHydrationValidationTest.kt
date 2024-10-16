package graphql.nadel.validation

import graphql.nadel.engine.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.FieldWithPolymorphicHydrationMustReturnAUnion
import graphql.nadel.validation.NadelSchemaValidationError.HydrationIncompatibleOutputType
import graphql.nadel.validation.NadelSchemaValidationError.HydrationsMismatch
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.assertSingleOfType
import graphql.schema.GraphQLNamedType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

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
            errors.map { it.message }.shouldBeEmpty()
        }

        it("fails if one of polymorphic hydrations references non existent field") {
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
                                field: "internalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )    
                        }
                        union AbstractUser = User | ExternalUser
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingHydrationActorField>()
            error.service.name.shouldBe("issues")
            error.parentType.overall.name.shouldBe("Issue")
            error.overallField.name.shouldBe("creator")
            error.hydration.pathToActorField.shouldBe(listOf("internalUser"))
        }

        it("fails if a mix of batched and non-batched hydrations is used") {
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
                                field: "users"
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
                        type Query {
                            users(id: [ID!]!): [User]
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
                            users(id: [ID!]!): [User]
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
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<HydrationsMismatch>()
            error.parentType.overall.name.shouldBe("Issue")
            error.overallField.name.shouldBe("creator")
        }

        it("can detect if polymorphic hydration from the same service returns a type that does not exist in the underlying schema") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue(id: ID!): String 
                        }
                        type Issue {
                            id: ID!
                            reference: ReferenceObject
                            @hydrated(
                                service: "issues"
                                field: "issue"
                                arguments: [
                                    {name: "id", value: "$source.referenceId"}
                                ]
                            )
                            @hydrated(
                                service: "pages"
                                field: "page"
                                arguments: [
                                    {name: "id", value: "$source.referenceId"}
                                ]
                            )    
                        }
                        union ReferenceObject = Issue | Page
                    """.trimIndent(),
                    "pages" to """
                        type Query {
                            page(id: ID!): Page
                        }
                        type Page {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue(id: ID!): String
                        }
                    """.trimIndent(),
                    "pages" to """
                        type Query {
                            page(id: ID!): Page
                        }
                        type Page {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            val error = errors.assertSingleOfType<MissingUnderlyingType>()
            error.overallType.name.shouldBe("Issue")
            error.service.name.shouldBe("issues")
        }

        it("can detect if polymorphic hydration from the same service references a type that does not exist in the underlying schema") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue(id: ID!): String
                        }
                        type Comment {
                            id: ID
                            text: String
                        }
                        type Issue {
                            id: ID!
                            comment: Comment
                            reference: ReferenceObject
                            @hydrated(
                                service: "issues"
                                field: "issue"
                                arguments: [
                                    {name: "id", value: "$source.referenceId"}
                                ]
                            )
                            @hydrated(
                                service: "pages"
                                field: "page"
                                arguments: [
                                    {name: "id", value: "$source.referenceId"}
                                ]
                            )    
                        }
                        union ReferenceObject = Issue | Page
                    """.trimIndent(),
                    "pages" to """
                        type Query {
                            page(id: ID!): Page
                        }
                        type Page {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            referenceId: ID
                        }
                    """.trimIndent(),
                    "pages" to """
                        type Query {
                            page(id: ID!): Page
                        }
                        type Page {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            val error = errors.assertSingleOfType<MissingUnderlyingType>()
            error.overallType.name.shouldBe("Comment")
            error.service.name.shouldBe("issues")
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
                        type Query {
                            user(id: ID!): InternalUser
                            externalUser(id: ID!): ExternalUser
                        }
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
            errors.map { it.message }.shouldBeEmpty()
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
            assert(errors.isNotEmpty())
            val error = errors.filterIsInstance<FieldWithPolymorphicHydrationMustReturnAUnion>().single()
            error.parentType.overall.name.shouldBe("Issue")
            error.overallField.name.shouldBe("creator")
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
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<HydrationIncompatibleOutputType>()
            error.parentType.overall.name.shouldBe("Issue")
            error.actorField.name.shouldBe("externalUser")
            (error.actorField.type as GraphQLNamedType).name.shouldBe("ExternalUser")
            error.incompatibleOutputType.name.shouldBe("ExternalUser")
        }
    }
})

