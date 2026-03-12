package graphql.nadel.definition.coordinates

import graphql.parser.Parser
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

abstract class NadelSchemaMemberCoordinatesFactoryTest {
    abstract fun extractCoordinates(schema: String): Set<NadelSchemaMemberCoordinates>

    class GraphQLSchemaExtractorTest : NadelSchemaMemberCoordinatesFactoryTest() {
        override fun extractCoordinates(schema: String): Set<NadelSchemaMemberCoordinates> {
            return NadelSchemaMemberCoordinatesFactory().create(SchemaGenerator.createdMockedSchema(schema))
        }
    }

    class DocumentDefinitionExtractorTest : NadelSchemaMemberCoordinatesFactoryTest() {
        override fun extractCoordinates(schema: String): Set<NadelSchemaMemberCoordinates> {
            return NadelSchemaMemberCoordinatesFactory().create(Parser().parseDocument(schema))
        }
    }

    @Test
    fun `generates schema map`() {
        // language=GraphQL
        val schema = """
            scalar URL
            scalar JSON
            scalar DateTime
            interface User {
                accountId: ID!
                canonicalAccountId: ID!
                accountStatus: AccountStatus!
                name: String!
                picture: URL!
            }
            union UserUnion = AtlassianAccountUser | CustomerUser
            interface LocalizationContext {
                zoneinfo: String
                locale: String
            }
            type AtlassianAccountUser implements User & LocalizationContext {
                accountId: ID!
                canonicalAccountId: ID!
                accountStatus: AccountStatus!
                name: String!
                picture: URL!
                email: String
                zoneinfo: String
                locale: String
                nickname: String
                orgId: ID
                extendedProfile: AtlassianAccountUserExtendedProfile
                characteristics: JSON
            }
            type AtlassianAccountUserExtendedProfile {
                jobTitle: String
                organization: String
                department: String
                location: String
                phoneNumbers: [String]
                closedDate: DateTime
                inactiveDate: DateTime
            }
            type CustomerUser implements User & LocalizationContext {
                accountId: ID!
                canonicalAccountId: ID!
                accountStatus: AccountStatus!
                name: String!
                picture: URL!
                email: String
                zoneinfo: String
                locale: String
            }
            type ThirdPartyUser implements LocalizationContext {
                accountId: ID!
                canonicalAccountId: ID!
                accountStatus: AccountStatus!
                name: String
                picture: URL
                email: String
                externalId: String!
                createdAt: DateTime!
                updatedAt: DateTime!
                zoneinfo: String
                locale: String
            }
            type AppUser implements User {
                accountId: ID!
                canonicalAccountId: ID!
                accountStatus: AccountStatus!
                name: String!
                picture: URL!
            }
            enum AccountStatus {
                active
                inactive
                closed
            }
            type AuthenticationContext {
                user: User
            }
            type Query {
                me: AuthenticationContext!
                authContext: AuthenticationContext @deprecated(reason: "Hello world")
                user(accountId: ID!): User
                users(accountIds: [ID!]!): [User!]
                thirdPartyUsers(ids: [ID!]!): [ThirdPartyUser!]
            }
        """.trimIndent()

        val expectedSet = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("me"),
            NadelObjectCoordinates("Query").field("authContext"),
            NadelObjectCoordinates("Query").field("authContext").appliedDirective("deprecated"),
            NadelObjectCoordinates("AuthenticationContext"),
            NadelObjectCoordinates("AuthenticationContext").field("user"),
            NadelInterfaceCoordinates("User"),
            NadelInterfaceCoordinates("User").field("accountId"),
            NadelInterfaceCoordinates("User").field("canonicalAccountId"),
            NadelInterfaceCoordinates("User").field("accountStatus"),
            NadelEnumCoordinates("AccountStatus"),
            NadelEnumCoordinates("AccountStatus").enumValue("active"),
            NadelEnumCoordinates("AccountStatus").enumValue("inactive"),
            NadelEnumCoordinates("AccountStatus").enumValue("closed"),
            NadelInterfaceCoordinates("User").field("name"),
            NadelInterfaceCoordinates("User").field("picture"),
            NadelScalarCoordinates("URL"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("accountId"),
            NadelObjectCoordinates("Query").field("users"),
            NadelObjectCoordinates("Query").field("users").argument("accountIds"),
            NadelObjectCoordinates("Query").field("thirdPartyUsers"),
            NadelObjectCoordinates("ThirdPartyUser"),
            NadelObjectCoordinates("ThirdPartyUser").field("accountId"),
            NadelObjectCoordinates("ThirdPartyUser").field("canonicalAccountId"),
            NadelObjectCoordinates("ThirdPartyUser").field("accountStatus"),
            NadelObjectCoordinates("ThirdPartyUser").field("name"),
            NadelObjectCoordinates("ThirdPartyUser").field("picture"),
            NadelObjectCoordinates("ThirdPartyUser").field("email"),
            NadelObjectCoordinates("ThirdPartyUser").field("externalId"),
            NadelObjectCoordinates("ThirdPartyUser").field("createdAt"),
            NadelScalarCoordinates("DateTime"),
            NadelObjectCoordinates("ThirdPartyUser").field("updatedAt"),
            NadelObjectCoordinates("ThirdPartyUser").field("zoneinfo"),
            NadelObjectCoordinates("ThirdPartyUser").field("locale"),
            NadelInterfaceCoordinates("LocalizationContext"),
            NadelInterfaceCoordinates("LocalizationContext").field("zoneinfo"),
            NadelInterfaceCoordinates("LocalizationContext").field("locale"),
            NadelObjectCoordinates("Query").field("thirdPartyUsers").argument("ids"),
            NadelObjectCoordinates("AtlassianAccountUser"),
            NadelObjectCoordinates("AtlassianAccountUser").field("accountId"),
            NadelObjectCoordinates("AtlassianAccountUser").field("canonicalAccountId"),
            NadelObjectCoordinates("AtlassianAccountUser").field("accountStatus"),
            NadelObjectCoordinates("AtlassianAccountUser").field("name"),
            NadelObjectCoordinates("AtlassianAccountUser").field("picture"),
            NadelObjectCoordinates("AtlassianAccountUser").field("email"),
            NadelObjectCoordinates("AtlassianAccountUser").field("zoneinfo"),
            NadelObjectCoordinates("AtlassianAccountUser").field("locale"),
            NadelObjectCoordinates("AtlassianAccountUser").field("nickname"),
            NadelObjectCoordinates("AtlassianAccountUser").field("orgId"),
            NadelObjectCoordinates("AtlassianAccountUser").field("extendedProfile"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("jobTitle"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("organization"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("department"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("location"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("phoneNumbers"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("closedDate"),
            NadelObjectCoordinates("AtlassianAccountUserExtendedProfile").field("inactiveDate"),
            NadelObjectCoordinates("AtlassianAccountUser").field("characteristics"),
            NadelScalarCoordinates("JSON"),
            NadelObjectCoordinates("CustomerUser"),
            NadelObjectCoordinates("CustomerUser").field("accountId"),
            NadelObjectCoordinates("CustomerUser").field("canonicalAccountId"),
            NadelObjectCoordinates("CustomerUser").field("accountStatus"),
            NadelObjectCoordinates("CustomerUser").field("name"),
            NadelObjectCoordinates("CustomerUser").field("picture"),
            NadelObjectCoordinates("CustomerUser").field("email"),
            NadelObjectCoordinates("CustomerUser").field("zoneinfo"),
            NadelObjectCoordinates("CustomerUser").field("locale"),
            NadelObjectCoordinates("AppUser"),
            NadelObjectCoordinates("AppUser").field("accountId"),
            NadelObjectCoordinates("AppUser").field("canonicalAccountId"),
            NadelObjectCoordinates("AppUser").field("accountStatus"),
            NadelObjectCoordinates("AppUser").field("name"),
            NadelObjectCoordinates("AppUser").field("picture"),
            NadelUnionCoordinates("UserUnion"),
        )

        // When
        val coordinates = extractCoordinates(schema)

        // Then
        for (expected in expectedSet) {
            assertTrue(coordinates.contains(expected))
        }
        for (actual in coordinates) {
            assertTrue(expectedSet.contains(actual))
        }
        assertTrue(coordinates.size == expectedSet.size)
    }

    @Test
    fun `generates coordinates for minimal schema with single query field`() {
        val schema = """
            type Query {
                hello: String
            }
        """.trimIndent()

        val expectedSet = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("hello"),
        )

        // When
        val coordinates = extractCoordinates(schema)

        // Then
        for (expected in expectedSet) {
            assertTrue(coordinates.contains(expected))
        }
        for (actual in coordinates) {
            assertTrue(expectedSet.contains(actual))
        }
        assertTrue(coordinates.size == expectedSet.size)
    }

    @Test
    fun `generates coordinates for schema with union type`() {
        val schema = """
            type Query {
                result: SearchResult
            }
            union SearchResult = User | Product
            type User {
                id: ID!
                name: String
            }
            type Product {
                id: ID!
                title: String
            }
        """.trimIndent()

        val expectedSet = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("result"),
            NadelUnionCoordinates("SearchResult"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelObjectCoordinates("Product"),
            NadelObjectCoordinates("Product").field("id"),
            NadelObjectCoordinates("Product").field("title"),
        )

        // When
        val coordinates = extractCoordinates(schema)

        // Then
        for (expected in expectedSet) {
            assertTrue(coordinates.contains(expected))
        }
        for (actual in coordinates) {
            assertTrue(expectedSet.contains(actual))
        }
        assertTrue(coordinates.size == expectedSet.size)
    }

    @Test
    fun `generates coordinates for schema with input type and mutation`() {
        val schema = """
            type Query {
                user(id: ID!): User
            }
            type Mutation {
                createUser(input: CreateUserInput!): User
            }
            input CreateUserInput {
                name: String!
                email: String
            }
            type User {
                id: ID!
                name: String
            }
        """.trimIndent()

        val expectedSet = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("Mutation"),
            NadelObjectCoordinates("Mutation").field("createUser"),
            NadelObjectCoordinates("Mutation").field("createUser").argument("input"),
            NadelInputObjectCoordinates("CreateUserInput"),
            NadelInputObjectCoordinates("CreateUserInput").field("name"),
            NadelInputObjectCoordinates("CreateUserInput").field("email"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
        )

        // When
        val coordinates = extractCoordinates(schema)

        // Then
        for (expected in expectedSet) {
            assertTrue(coordinates.contains(expected))
        }
        for (actual in coordinates) {
            assertTrue(expectedSet.contains(actual))
        }
        assertTrue(coordinates.size == expectedSet.size)
    }
}
