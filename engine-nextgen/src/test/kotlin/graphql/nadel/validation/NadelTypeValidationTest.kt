package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.assertSingleOfType
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

        it("cannot have a synthetic union") {
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
                        union Something = Echo | World
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

            val error = errors.assertSingleOfType<DuplicatedUnderlyingType>()
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

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            assert(error.service.name == "users")
            assert(error.overallField.name == "nextgenSpecific")
        }

        it("picks up types defined in other services and used in unions") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        type QueryError {
                            id: ID
                        }
                    """.trimIndent(),
                    // This service's User type is missing the nextgenSpecific field
                    // But the User type is only referenced via the union UserResult
                    "users" to """
                        type Query {
                            users: [UserResult]
                        }
                    """.trimIndent(),
                    "nextgenUsers" to """
                        type User {
                            id: ID
                            nextgenSpecific: String
                        }
                        union UserResult = User | QueryError
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users: [UserResult]
                        }
                        type User {
                            id: ID!
                        }
                        type QueryError {
                            id: ID
                        }
                        union UserResult = User | QueryError
                    """.trimIndent(),
                    "nextgenUsers" to """
                        type Query {
                            echo: String
                        }
                        type User {
                            id: ID!
                            nextgenSpecific: String
                        }
                        type QueryError {
                            id: ID
                        }
                        union UserResult = User | QueryError
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            assert(error.service.name == "users")
            assert(error.overallField.name == "nextgenSpecific")
        }

        it("picks up types defined in other services used as input types") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        input UserFilter {
                            a: String
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(filter: UserFilter): [User]
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
                    "users" to """
                        type Query {
                            users(filter: UserFilter): [User]
                        }
                        type User {
                            id: ID!
                        }
                        input UserFilter {
                            x: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingInputField>()
            assert(error.service.name == "users")
            assert(error.parentType.overall.name == "UserFilter")
            assert(error.overallField.name == "a")
        }

        it("picks up types not directly referenced") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    // You use Issue? You have to define it as IS
                    "users" to """
                        type User {
                            id: ID!
                            issue: Issue
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: [Issue]
                        }
                        type Issue {
                            id: ID!
                            description: String
                            epic: Epic
                        }
                        type Epic {
                            children(input: IssueFilter): [Issue]
                        }
                        input IssueFilter {
                            keyword: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    // You can't just define a half assed Issue type without Issue.epic
                    // AND you need IssueFilter
                    // But you can define new fields on Issue if you want
                    "users" to """
                        type Query {
                            me: User
                        }
                        type User {
                            id: ID!
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            description: String
                            linked: [IssueRef]
                        }
                        type IssueRef {
                            key: ID
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: [Issue]
                        }
                        type Issue {
                            id: ID!
                            description: String
                            epic: Epic
                        }
                        type Epic {
                            children(input: IssueFilter): [Issue]
                        }
                        input IssueFilter {
                            keyword: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.size == 3)

            val missingField = errors.assertSingleOfType<MissingUnderlyingField>()
            assert(missingField.service.name == "users")
            assert(missingField.parentType.overall.name == "Issue")
            assert(missingField.overallField.name == "epic")

            val missingIssueFilter = errors.assertSingleOfType<MissingUnderlyingType> {
                it.overallType.name == "IssueFilter"
            }
            assert(missingIssueFilter.service.name == "users")

            val missingEpic = errors.assertSingleOfType<MissingUnderlyingType> { it.overallType.name == "Epic" }
            assert(missingEpic.service.name == "users")
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

            val error = errors.assertSingleOfType<IncompatibleType>()
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

            val error = errors.assertSingleOfType<IncompatibleType>()
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

            val missingNextPage = errors.assertSingleOfType<MissingUnderlyingField> {
                it.overallField.name == "hasNextPage"
            }
            assert(missingNextPage.service.name == "jira")
            assert(missingNextPage.parentType.overall.name == "PageInfo")

            val missingPrevPage = errors.assertSingleOfType<MissingUnderlyingField> {
                it.overallField.name == "hasPreviousPage"
            }
            assert(missingPrevPage.service.name == "jira")
            assert(missingPrevPage.parentType.overall.name == "PageInfo")

            val missingStartCursor = errors.assertSingleOfType<MissingUnderlyingField> {
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

            val error = errors.assertSingleOfType<IncompatibleFieldOutputType>()
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

                val error = errors.assertSingleOfType<IncompatibleFieldOutputType>()
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

            val error = errors.assertSingleOfType<IncompatibleFieldOutputType>()
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
