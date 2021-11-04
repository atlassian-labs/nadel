package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import io.kotest.core.spec.style.DescribeSpec
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NadelTypeValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if types are valid") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("tracks visited types to avoid stack overflow").config(timeout = Duration.seconds(1)) {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                            world: World
                        }
                        type Echo {
                            self: Echo
                            world: World
                        }
                        type World {
                            echo: Echo
                            self: World
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                            world: World
                        }
                        type Echo {
                            self: Echo
                            world: World
                        }
                        type World {
                            echo: Echo
                            self: World
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails if underlying type is duplicated via renames") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm @renamed(from: "World") {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<DuplicatedUnderlyingType>()
            assert(error.service.name == "test")
            assert(error.duplicates.map { it.overall.name }.toSet() == setOf("World", "Microcosm"))
            assert(error.duplicates.map { it.underlying.name }.toSet() == setOf("World"))
        }

        it("treats service named shared as fake service for holding types") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            a: String 
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("picks up types only used at output type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "users" to """
                        type Query {
                            users: [User!]
                        }
                    """.trimIndent(),
                    "nextgenUsers" to """
                        type Query {
                            user(id: ID!): User
                        }
                        type User {
                            id: ID
                            nextgenSpecific: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "users" to """
                        type Query {
                            users: [User!]
                        }
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                    "nextgenUsers" to """
                        type Query {
                            user(id: ID!): User!
                            users: [User!]
                        }
                        type User {
                            id: ID!
                            nextgenSpecific: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingUnderlyingField>()
            assert(error.service.name == "users")
            assert(error.overallField.name == "nextgenSpecific")
        }

        it("fails if type kinds are incompatible") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        interface World {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<IncompatibleType>()
            assert(error.schemaElement.service.name == "test")
            assert(error.schemaElement.overall.name == "World")
            assert(error.schemaElement.underlying.name == "World")
        }

        it("fails if renamed type kinds are incompatible") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World @renamed(from: "Pluto") {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: Pluto 
                        }
                        interface Pluto {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<IncompatibleType>()
            assert(error.schemaElement.service.name == "test")
            assert(error.schemaElement.overall.name == "World")
            assert(error.schemaElement.underlying.name == "Pluto")
        }

        it("can validate shared types") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        type PageInfo {
                            hasNextPage: Boolean!
                            hasPreviousPage: Boolean!
                            startCursor: String
                            endCursor: String
                        }
                    """.trimIndent(),
                    "jira" to """
                        type Query {
                            issues: IssueConnection
                        }
                        type IssueConnection {
                            nodes: [Issue]
                            pageInfo: PageInfo!
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type UserConnection {
                            nodes: [User!]
                            pageInfo: PageInfo!
                        }
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "jira" to """
                        type Query {
                            issues: IssueConnection
                        }
                        type IssueConnection {
                            nodes: [Issue]
                            pageInfo: PageInfo!
                        }
                        type Issue {
                            id: ID!
                        }
                        type PageInfo {
                            startCursor: String
                            endCursor: String
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            echo: String
                        }
                        type UserConnection {
                            nodes: [User!]
                            pageInfo: PageInfo!
                        }
                        type User {
                            id: ID!
                        }
                        type PageInfo {
                            hasNextPage: Boolean!
                            hasPreviousPage: Boolean!
                            endCursor: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val missingNextPage = errors.singleOfType<MissingUnderlyingField> {
                it.overallField.name == "hasNextPage"
            }
            assert(missingNextPage.service.name == "jira")
            assert(missingNextPage.parentType.overall.name == "PageInfo")

            val missingPrevPage = errors.singleOfType<MissingUnderlyingField> {
                it.overallField.name == "hasPreviousPage"
            }
            assert(missingPrevPage.service.name == "jira")
            assert(missingPrevPage.parentType.overall.name == "PageInfo")

            val missingStartCursor = errors.singleOfType<MissingUnderlyingField> {
                it.overallField.name == "startCursor"
            }
            assert(missingStartCursor.service.name == "users")
            assert(missingStartCursor.parentType.overall.name == "PageInfo")
        }
    }

    describe("validateOutputType") {
        it("fails if output type is wrong") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: Microcosm
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<IncompatibleFieldOutputType>()
            assert(error.parentType.overall.name == "Echo")
            assert(error.parentType.underlying.name == "Echo")
            assert(error.overallField.name == "world")
            assert(error.underlyingField.name == "world")
            assert(error.subject == error.overallField)
        }

        it("passes if underlying output type is non null but overall output type is nullable") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            worlds: [World]
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo!
                        }
                        type Echo {
                            worlds: [World!]!
                        }
                        type World {
                            hello: String!
                        }
                        type Microcosm {
                            hello: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails if array dimensions are wrong") {
            val fixtures = listOf(
                NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                        type Query {
                            worlds: String 
                        }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                        type Query {
                            worlds: [String]
                        }
                    """.trimIndent(),
                    ),
                ),
                NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                        type Query {
                            worlds: [String]
                        }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                        type Query {
                            worlds: String 
                        }
                    """.trimIndent(),
                    ),
                ),
            )

            for (fixture in fixtures) {
                val errors = validate(fixture)
                assert(errors.map { it.message }.isNotEmpty())

                val error = errors.singleOfType<IncompatibleFieldOutputType>()
                assert(error.parentType.overall.name == "Query")
                assert(error.parentType.underlying.name == "Query")
                assert(error.overallField.name == "worlds")
                assert(error.underlyingField.name == "worlds")
            }
        }

        it("fails if underlying output type is nullable but overall output type is not null") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo!
                        }
                        type Echo {
                            world: World!
                        }
                        type World {
                            hello: String!
                        }
                        type Microcosm {
                            hello: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            // We could say .none but this will print out the perpetrator
            assert(errors.firstOrNull { it !is IncompatibleFieldOutputType } == null)
        }

        it("fails if underlying output array type is nullable but overall array type is not null") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            worlds: [World!]
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            worlds: [World]
                        }
                        type World {
                            hello: String
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<IncompatibleFieldOutputType>()
            assert(error.parentType.overall.name == "Echo")
            assert(error.parentType.underlying.name == "Echo")
            assert(error.overallField.name == "worlds")
        }

        it("passes even if output type is renamed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            worlds: [[World]]
                        }
                        type World @renamed(from: "Microcosm") {
                            hello: String
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
                            echo: Echo
                        }
                        type Echo {
                            worlds: [[Microcosm!]]
                        }
                        type Microcosm {
                            hello: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }
    }
})
