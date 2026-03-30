package graphql.nadel.validation

import graphql.nadel.validation.util.assertSingleOfType
import kotlin.test.Test
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationValidationTest2 {
    @Test
    fun `error on mixing index hydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issue: JiraIssue
                    }
                    union Person = User | Account
                    type JiraIssue @renamed(from: "Issue") {
                        id: ID!
                        creator: Person
                        @hydrated(
                            service: "users"
                            field: "user"
                            arguments: [
                                {name: "id", value: "$source.creator"}
                            ]
                        )
                        @hydrated(
                            service: "users"
                            field: "account"
                            arguments: [
                                {name: "id", value: "$source.creator"}
                            ]
                            indexed: true
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        user(id: ID!): User
                        account(id: ID!): Account
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                        creator: ID!
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        user(id: ID!): User
                        account(id: ID!): Account
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelHydrationMustUseIndexExclusivelyError)
        assertTrue(errors.single().subject.name == "creator")
    }

    @Test
    fun `prohibit multiple source input fields if they are list types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userIds"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueIds"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userIds: [ID]
                        issueIds: [ID]
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelPolymorphicHydrationIncompatibleSourceFieldsError)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `prohibit multiple source fields where list not the leaf`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.contexts.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.contexts.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type ActivityContext {
                        issueId: ID
                        userId: ID
                    }
                    type Activity {
                        id: ID!
                        contexts: [ActivityContext]
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelPolymorphicHydrationIncompatibleSourceFieldsError)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `prohibit mixing list and non-list source input fields`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userIds"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userIds: [ID]
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelPolymorphicHydrationIncompatibleSourceFieldsError)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `permit multiple source fields if source input field is not list type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userId: ID
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `permit multiple source fields non batched hydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "users"
                            field: "userById"
                            arguments: [
                                {name: "id", value: "$source.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issueById"
                            arguments: [
                                {name: "id", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issueById(id: ID): Issue
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userId: ID
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issueById(id: ID): Issue
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `object identifier follows source input field path`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = Comment | Page 
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "issues"
                            field: "comments"
                            arguments: [
                                {name: "queries", value: "$source.jiraComment"}
                            ]
                            inputIdentifiedBy: [
                                {sourceId: "jiraComment.commentId", resultId: "id"}
                                {sourceId: "jiraComment.issueId", resultId: "issueId"}
                            ]
                        )
                        @hydrated(
                            service: "pages"
                            field: "pages"
                            arguments: [
                                {name: "queries", value: "$source.confluencePage"}
                            ]
                            inputIdentifiedBy: [
                                {sourceId: "confluencePage.pageId", resultId: "id"}
                                {sourceId: "confluencePage.pageStatus", resultId: "status"}
                            ]
                        )
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(queries: [CommentQuery]!): [Comment]
                    }
                    input CommentQuery {
                        issueId: ID!
                        commentId: ID!
                    }
                    type Comment {
                        id: ID!
                        issueId: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(queries: [PageQuery]!): [Page]
                    }
                    input PageQuery {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                    type Page {
                        id: ID!
                        status: PageStatus
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        jiraComment: ActivityJiraCommentLink
                        confluencePage: ActivityConfluencePageLink
                    }
                    type ActivityJiraCommentLink {
                        issueId: ID!
                        commentId: ID!
                    }
                    type ActivityConfluencePageLink {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(queries: [CommentQuery]!): [Comment]
                    }
                    input CommentQuery {
                        issueId: ID!
                        commentId: ID!
                    }
                    type Comment {
                        id: ID!
                        issueId: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(queries: [PageQuery]!): [Page]
                    }
                    input PageQuery {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                    type Page {
                        id: ID!
                        status: PageStatus
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `object identifier is outside of source input field path`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = Comment | Page 
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "issues"
                            field: "comments"
                            arguments: [
                                {name: "queries", value: "$source.jiraComment"}
                            ]
                            inputIdentifiedBy: [
                                {sourceId: "id", resultId: "id"}
                                {sourceId: "jiraComment.issueId", resultId: "issueId"}
                            ]
                        )
                        @hydrated(
                            service: "pages"
                            field: "pages"
                            arguments: [
                                {name: "queries", value: "$source.confluencePage"}
                            ]
                            inputIdentifiedBy: [
                                {sourceId: "confluencePage.pageId", resultId: "id"}
                                {sourceId: "confluencePage.pageStatus", resultId: "status"}
                            ]
                        )
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(queries: [CommentQuery]!): [Comment]
                    }
                    input CommentQuery {
                        issueId: ID!
                        commentId: ID!
                    }
                    type Comment {
                        id: ID!
                        issueId: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(queries: [PageQuery]!): [Page]
                    }
                    input PageQuery {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                    type Page {
                        id: ID!
                        status: PageStatus
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        jiraComment: ActivityJiraCommentLink
                        confluencePage: ActivityConfluencePageLink
                    }
                    type ActivityJiraCommentLink {
                        issueId: ID!
                        commentId: ID!
                    }
                    type ActivityConfluencePageLink {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(queries: [CommentQuery]!): [Comment]
                    }
                    input CommentQuery {
                        issueId: ID!
                        commentId: ID!
                    }
                    type Comment {
                        id: ID!
                        issueId: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(queries: [PageQuery]!): [Page]
                    }
                    input PageQuery {
                        pageId: ID!
                        pageStatus: PageStatus
                    }
                    enum PageStatus {
                        CURRENT
                        DRAFT
                    }
                    type Page {
                        id: ID!
                        status: PageStatus
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelBatchHydrationMatchingStrategyInvalidSourceIdError)
        val error = errors.single() as NadelBatchHydrationMatchingStrategyInvalidSourceIdError
        assertTrue(error.subject.name == "data")
        assertTrue(error.offendingObjectIdentifier.sourceId == "id")
        assertTrue(error.offendingObjectIdentifier.resultId == "id")
    }

    @Test
    fun `union hydration uses @idHydrated for all member types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = Comment | Page 
                    type Activity {
                        id: ID!
                        data: ActivityContent
                          @idHydrated(idField: "id")
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment @defaultHydration(field: "comments", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page @defaultHydration(field: "pages", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `union hydration is missing default hydrations for some members`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = Comment | Page 
                    type Activity {
                        id: ID!
                        data: ActivityContent
                          @idHydrated(idField: "id")
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment @defaultHydration(field: "comments", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        assertTrue(errors.singleOrNull() is NadelHydrationUnionMemberNoBackingError)
        val error = errors.single() as NadelHydrationUnionMemberNoBackingError
        assertTrue(error.virtualField.name == "data")
        assertTrue(error.membersNoBacking.map { it.name } == listOf("Page"))
    }

    @Test
    fun `union hydration can use a mix of @idHydrated and @hydrated`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = Comment | Page 
                    type Activity {
                        id: ID!
                        data: ActivityContent
                          @idHydrated(idField: "id")
                          @hydrated(
                            field: "pages",
                            arguments: [{name: "ids", value: "$source.id"}]
                          )
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment @defaultHydration(field: "comments", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `fails if requested absent batch size argument exceeds maximum`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        data: Page
                          @hydrated(
                            field: "pages",
                            arguments: [{name: "ids", value: "$source.id"}]
                          )
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        assertTrue(errors.singleOrNull() is NadelHydrationExceedsMaxBatchSizeError)
        val error = errors.single() as NadelHydrationExceedsMaxBatchSizeError
        assertTrue(error.virtualField.name == "data")
        assertTrue(error.requestedBatchSize == 200)
        assertTrue(error.maxBatchSize == 25)
    }

    @Test
    fun `fails if explicit batch size argument exceeds default`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        data: Page
                          @hydrated(
                            field: "pages",
                            arguments: [{name: "ids", value: "$source.id"}]
                            batchSize: 50
                          )
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        assertTrue(errors.singleOrNull() is NadelHydrationExceedsMaxBatchSizeError)
        val error = errors.single() as NadelHydrationExceedsMaxBatchSizeError
        assertTrue(error.virtualField.name == "data")
        assertTrue(error.requestedBatchSize == 50)
        assertTrue(error.maxBatchSize == 25)
    }

    @Test
    fun `max batch size limit works with idHydrated`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        data: Page @idHydrated(idField: "id")
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page @defaultHydration(field: "pages", idArgument: "ids", batchSize: 30) {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val hydratedError = errors.assertSingleOfType<NadelHydrationExceedsMaxBatchSizeError>()
        assertTrue(hydratedError.parentType.overall.name == "Activity")
        assertTrue(hydratedError.virtualField.name == "data")
        assertTrue(hydratedError.requestedBatchSize == 30)
        assertTrue(hydratedError.maxBatchSize == 25)

        val defaultHydrationError = errors.assertSingleOfType<NadelDefaultHydrationExceedsMaxBatchSizeError>()
        assertTrue(defaultHydrationError.type.overall.name == "Page")
        assertTrue(defaultHydrationError.backingField.name == "pages")
        assertTrue(defaultHydrationError.requestedBatchSize == 30)
        assertTrue(defaultHydrationError.maxBatchSize == 25)
    }

    @Test
    fun `max batch size limit works with absent batchSize on idHydrated`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        data: Page @idHydrated(idField: "id")
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page @defaultHydration(field: "pages", idArgument: "ids") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                    }
                """.trimIndent(),
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val hydratedError = errors.assertSingleOfType<NadelHydrationExceedsMaxBatchSizeError>()
        assertTrue(hydratedError.parentType.overall.name == "Activity")
        assertTrue(hydratedError.virtualField.name == "data")
        assertTrue(hydratedError.requestedBatchSize == 200)
        assertTrue(hydratedError.maxBatchSize == 25)

        val defaultHydrationError = errors.assertSingleOfType<NadelDefaultHydrationExceedsMaxBatchSizeError>()
        assertTrue(defaultHydrationError.type.overall.name == "Page")
        assertTrue(defaultHydrationError.backingField.name == "pages")
        assertTrue(defaultHydrationError.requestedBatchSize == 200)
        assertTrue(defaultHydrationError.maxBatchSize == 25)
    }

    @Test
    fun `max batch size limit works with defaultHydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page @defaultHydration(field: "pages", idArgument: "ids", batchSize: 40) {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val defaultHydrationError = errors.assertSingleOfType<NadelDefaultHydrationExceedsMaxBatchSizeError>()
        assertTrue(defaultHydrationError.type.overall.name == "Page")
        assertTrue(defaultHydrationError.backingField.name == "pages")
        assertTrue(defaultHydrationError.requestedBatchSize == 40)
        assertTrue(defaultHydrationError.maxBatchSize == 25)
    }

    @Test
    fun `max batch size limit works with absent batchSize on defaultHydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page] @maxBatchSize(size: 25)
                    }
                    type Page @defaultHydration(field: "pages", idArgument: "ids") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "pages" to /* language=GraphQL*/ """
                    type Query {
                        pages(ids: [ID!]!): [Page]
                    }
                    type Page {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val defaultHydrationError = errors.assertSingleOfType<NadelDefaultHydrationExceedsMaxBatchSizeError>()
        assertTrue(defaultHydrationError.type.overall.name == "Page")
        assertTrue(defaultHydrationError.backingField.name == "pages")
        assertTrue(defaultHydrationError.requestedBatchSize == 200)
        assertTrue(defaultHydrationError.maxBatchSize == 25)
    }
}
