package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

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
            errors.map { it.message }.shouldBeEmpty()
        }

        it("fails if a type is implicitly renamed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            echo: String
                        }
                    """.trimIndent(),
                    "delta" to """
                        type Query {
                            delta: Echo
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            echo: String
                        }
                    """.trimIndent(),
                    "delta" to """
                        type Query {
                            delta: Delta
                        }
                        type Delta {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val missingTypeError = errors.assertSingleOfType<MissingUnderlyingType>()
            missingTypeError.service.name.shouldBe("delta")
            missingTypeError.overallType.name.shouldBe("Echo")

            val fieldTypeError = errors.assertSingleOfType<IncompatibleFieldOutputType>()
            fieldTypeError.service.name.shouldBe("delta")
            fieldTypeError.overallField.name.shouldBe("delta")
            fieldTypeError.parentType.overall.name.shouldBe("Query")
        }

        it("does not crash on schema definition") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        schema {
                            query: Query
                        }
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
            errors.map { it.message }.shouldBeEmpty()
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

        it("allows synthetic union if exclusively used for @hydrated fields") {
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
                            hello: Something @hydrated(
                                service: "test"
                                field: "echo.world"
                                arguments: []
                            )
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
            errors.map { it.message }.shouldBeEmpty()
        }

        it("prohibits synthetic union if not exclusively used for @hydrated fields") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: Echo
                        }
                        type Echo {
                            world: World
                            test: Something
                        }
                        type World {
                            hello: Something @hydrated(
                                service: "test"
                                field: "echo.world"
                                arguments: []
                            )
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

            val error = errors.assertSingleOfType<MissingUnderlyingType>()
            error.service.name.shouldBe("test")
            error.overallType.name.shouldBe("Something")
        }

        it("tracks visited types to avoid stack overflow").config(timeout = 1.seconds) {
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
            errors.map { it.message }.shouldBeEmpty()
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
            error.service.name.shouldBe("test")
            error.duplicates.map { it.overall.name }.toSet().shouldBe(setOf("World", "Microcosm"))
            error.duplicates.map { it.underlying.name }.toSet().shouldBe(setOf("World"))
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
            errors.map { it.message }.shouldBeEmpty()
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
            error.service.name.shouldBe("users")
            error.overallField.name.shouldBe("nextgenSpecific")
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
            error.service.name.shouldBe("users")
            error.overallField.name.shouldBe("nextgenSpecific")
        }

        it("validates extension union member if defined by own service") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "users" to """
                        type Query {
                            stuff: Stuff
                        }
                        union Stuff = User
                        type User {
                            id: ID!
                        }
                        extend union Stuff = | Issue
                    """.trimIndent(),
                    "issues" to """
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "users" to """
                        type Query {
                            stuff: Stuff
                        }
                        union Stuff = User
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            echo: String
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())
            val error = errors.assertSingleOfType<MissingUnderlyingType>()
            error.service.name.shouldBe("users")
            error.overallType.name.shouldBe("Issue")
        }

        it("validates external member if service defines it") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "users" to """
                        type Query {
                            stuff: Stuff
                        }
                        union Stuff = User
                        type User {
                            id: ID!
                        }
                    """.trimIndent(),
                    "issues" to """
                        extend union Stuff = | Issue
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "users" to """
                        type Query {
                            stuff: Stuff
                        }
                        union Stuff = User | Issue
                        type User {
                            id: ID!
                        }
                        type Issue {
                            key: ID!
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            echo: String
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())
            val error = errors.assertSingleOfType<MissingUnderlyingField>()
            error.service.name.shouldBe("users")
            error.parentType.overall.name.shouldBe("Issue")
            error.overallField.name.shouldBe("id")
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
            error.service.name.shouldBe("users")
            error.parentType.overall.name.shouldBe("UserFilter")
            error.overallField.name.shouldBe("a")
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
            errors.map { it.message }.size.shouldBe(3)

            val missingField = errors.assertSingleOfType<MissingUnderlyingField>()
            missingField.service.name.shouldBe("users")
            missingField.parentType.overall.name.shouldBe("Issue")
            missingField.overallField.name.shouldBe("epic")

            val missingIssueFilter = errors.assertSingleOfType<MissingUnderlyingType> {
                it.overallType.name == "IssueFilter"
            }
            missingIssueFilter.service.name.shouldBe("users")

            val missingEpic = errors.assertSingleOfType<MissingUnderlyingType> { it.overallType.name == "Epic" }
            missingEpic.service.name.shouldBe("users")
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
            error.schemaElement.service.name.shouldBe("test")
            error.schemaElement.overall.name.shouldBe("World")
            error.schemaElement.underlying.name.shouldBe("World")
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
            error.schemaElement.service.name.shouldBe("test")
            error.schemaElement.overall.name.shouldBe("World")
            error.schemaElement.underlying.name.shouldBe("Pluto")
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
            missingNextPage.service.name.shouldBe("jira")
            missingNextPage.parentType.overall.name.shouldBe("PageInfo")

            val missingPrevPage = errors.assertSingleOfType<MissingUnderlyingField> {
                it.overallField.name == "hasPreviousPage"
            }
            missingPrevPage.service.name.shouldBe("jira")
            missingPrevPage.parentType.overall.name.shouldBe("PageInfo")

            val missingStartCursor = errors.assertSingleOfType<MissingUnderlyingField> {
                it.overallField.name == "startCursor"
            }
            missingStartCursor.service.name.shouldBe("users")
            missingStartCursor.parentType.overall.name.shouldBe("PageInfo")
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
            error.parentType.overall.name.shouldBe("Echo")
            error.parentType.underlying.name.shouldBe("Echo")
            error.overallField.name.shouldBe("world")
            error.underlyingField.name.shouldBe("world")
            error.subject.shouldBe(error.overallField)
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
            errors.map { it.message }.shouldBeEmpty()
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
                error.parentType.overall.name.shouldBe("Query")
                error.parentType.underlying.name.shouldBe("Query")
                error.overallField.name.shouldBe("worlds")
                error.underlyingField.name.shouldBe("worlds")
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
            errors.firstOrNull { it !is IncompatibleFieldOutputType }.shouldBe(null)
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
            error.parentType.overall.name.shouldBe("Echo")
            error.parentType.underlying.name.shouldBe("Echo")
            error.overallField.name.shouldBe("worlds")
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
            errors.map { it.message }.shouldBeEmpty()
        }
    }
})
