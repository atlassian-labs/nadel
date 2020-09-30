package graphql.nadel.engine

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.*
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.testutils.TestUtil

import static graphql.language.AstPrinter.printAstCompact
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelExecutionStrategyTest2 extends StrategyTestHelper {


    ExecutionHelper executionHelper
    def service1Execution
    def service2Execution
    def serviceDefinition
    def definitionRegistry
    def instrumentation
    def serviceExecutionHooks
    def resultComplexityAggregator

    void setup() {
        executionHelper = new ExecutionHelper()
        service1Execution = Mock(ServiceExecution)
        service2Execution = Mock(ServiceExecution)
        serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        definitionRegistry = Mock(DefinitionRegistry)
        instrumentation = new NadelInstrumentation() {}
        serviceExecutionHooks = new ServiceExecutionHooks() {}
        resultComplexityAggregator = new ResultComplexityAggregator()
    }

    def "underlying service returns null for non-nullable field"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue
            }
            type Issue {
                id: ID!
            }
        }
        ''')
        def issueSchema = TestUtil.schema("""
            type Query {
                issue: Issue
            }
            type Issue {
                id: ID!
            }
        """)
        def query = "{issue {id}}"

        def expectedQuery1 = "query nadel_2_Issues {issue {id}}"
        def response1 = [issue: [id: null]]

        def overallResponse = [issue: null]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Issues",
                issueSchema,
                query,
                ["issue"],
                expectedQuery1,
                response1,
        )
        then:
        response == overallResponse
        errors.size() == 1
        errors[0].errorType == ErrorType.NullValueInNonNullableField

    }

    def "non-nullable field error bubbles up"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue!
            }
            type Issue {
                id: ID!
            }
        }
        ''')
        def issueSchema = TestUtil.schema("""
            type Query {
                issue: Issue!
            }
            type Issue {
                id: ID!
            }
        """)
        def query = "{issue {id}}"

        def expectedQuery1 = "query nadel_2_Issues {issue {id}}"
        def response1 = [issue: [id: null]]

        def overallResponse = null


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Issues",
                issueSchema,
                query,
                ["issue"],
                expectedQuery1,
                response1,
        )
        then:
        response == overallResponse
        errors.size() == 1
        errors[0].errorType == ErrorType.NullValueInNonNullableField


    }

    def "non-nullable field error in lists bubbles up to the top"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue!]!
            }
            type Issue {
                id: ID!
            }
        }
        ''')
        def issueSchema = TestUtil.schema("""
            type Query {
                issues: [Issue!]!
            }
            type Issue {
                id: ID!
            }
        """)
        def query = "{issues {id}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id}}"
        def response1 = [issues: [[id: null]]]

        def overallResponse = null


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Issues",
                issueSchema,
                query,
                ["issues"],
                expectedQuery1,
                response1,
        )
        then:
        response == overallResponse
        errors.size() == 1
        errors[0].errorType == ErrorType.NullValueInNonNullableField
    }

    def "non-nullable field error in lists bubbles up"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [[[Issue!]!]]
            }
            type Issue {
                id: ID!
            }
        }
        ''')
        def issueSchema = TestUtil.schema("""
            type Query {
                issues: [[[Issue!]!]]
            }
            type Issue {
                id: ID!
            }
        """)
        def query = "{issues {id}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id}}"
        def response1 = [issues: [[[[id: null], [id: "will be discarded"]]]]]

        def overallResponse = [issues: [null]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Issues",
                issueSchema,
                query,
                ["issues"],
                expectedQuery1,
                response1,
        )
        then:
        response == overallResponse
        errors.size() == 1
        errors[0].errorType == ErrorType.NullValueInNonNullableField

    }

    def "a lot of renames"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Boards {
            type Query {
                boardScope: BoardScope
            }
            type BoardScope {
                 cardParents: [CardParent]! => renamed from issueParents
            }
            type CardParent => renamed from IssueParent {
                 cardType: CardType! => renamed from issueType
            }
             type CardType => renamed from IssueType {
                id: ID
                inlineCardCreate: InlineCardCreateConfig => renamed from inlineIssueCreate
            }
            
            type InlineCardCreateConfig => renamed from InlineIssueCreateConfig {
                enabled: Boolean!
            }
        }
        ''')
        def boardSchema = TestUtil.schema("""
            type Query {
                boardScope: BoardScope
            }
            type BoardScope {
                issueParents: [IssueParent]!
            }
            type IssueParent {
                issueType: IssueType!
            }
            type IssueType {
                id: ID
                inlineIssueCreate: InlineIssueCreateConfig
            }
            type InlineIssueCreateConfig {
                enabled: Boolean!
            }
        """)
        def query = "{boardScope{ cardParents { cardType {id inlineCardCreate {enabled}}}}}"

        def expectedQuery1 = "query nadel_2_Boards {boardScope {issueParents {issueType {id inlineIssueCreate {enabled}}}}}"
        def response1 = [boardScope: [issueParents: [
                [issueType: [id: "ID-1", inlineIssueCreate: [enabled: true]]]
        ]]]

        def overallResponse = [boardScope: [cardParents: [
                [cardType: [id: "ID-1", inlineCardCreate: [enabled: true]]]
        ]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Boards",
                boardSchema,
                query,
                ["boardScope"],
                expectedQuery1,
                response1,
        )
        then:
        errors.size() == 0
        response == overallResponse

    }

    def "fragment referenced twice from inside Query and inside another Fragment"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar 
              } 
              type Bar {
                 id: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
              } 
              type Bar {
                id: String
              }
        """)
        def query = """{foo {id ...F2 ...F1}} fragment F2 on Bar {id} fragment F1 on Bar {id ...F2} """

        def expectedQuery1 = "query nadel_2_Foo {foo {id ...F2 ...F1}} fragment F2 on Bar {id} fragment F1 on Bar {id ...F2}"
        def response1 = [foo: [id: "ID"]]
        def overallResponse = response1


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    def "synthetic hydration call with two argument values from original field arguments "() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorId: ID
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersQuery: UsersQuery
        }
        type UsersQuery {
           usersByIds(extraArg1: String, extraArg2: Int, id: [ID]): [User]       
        }
        type User {
            id: ID
            name: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                author(extraArg: String): User => hydrated from UserService.usersQuery.usersByIds(extraArg1: $argument.extraArg1, extraArg2: $argument.extraArg2, id: $source.authorId) object identified by id, batch size 2
            }
        }
        service UserService {
            type Query {
                usersQuery: UsersQuery
            }
            type UsersQuery {
               usersByIds(extraArg1: String, extraArg2: Int, id: [ID]): [User]       
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("UserService", userServiceSchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = '{issues {id author(extraArg1: "extraArg1", extraArg2: 10) {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorId}}"
        def issue1 = [id: "ISSUE-1", authorId: "USER-1"]
        def response1 = new ServiceExecutionResult([issues: [issue1]])


        def expectedQuery2 = "query nadel_2_UserService {usersQuery {usersByIds(id:[\"USER-1\"],extraArg1:\"extraArg1\",extraArg2:10) {name object_identifier__UUID:id}}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"]]
        def response2 = new ServiceExecutionResult([usersQuery: [usersByIds: batchResponse1]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        def issue1Result = [id: "ISSUE-1", author: [name: "User 1"]]
        resultData(response) == [issues: [issue1Result]]
        resultComplexityAggregator.getTotalNodeCount() == 7
        resultComplexityAggregator.getNodeCountsForService("Issues") == 5
        resultComplexityAggregator.getNodeCountsForService("UserService") == 2

    }

    def "one synthetic hydration call with longer path and same named overall field"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorDetails: [AuthorDetail]
        }
        type AuthorDetail {
            authorId: ID
            name: String
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersQuery: UserQuery
        }
        type UserQuery {
            usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
            name: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authorDetails: [AuthorDetail]
                authors: [User] => hydrated from UserService.usersQuery.usersByIds(id: $source.authorDetails.authorId) object identified by id, batch size 2
            }
            type AuthorDetail {
                name: String
            }
        }
        service UserService {
            type Query {
                usersQuery: UserQuery
            }
            type UserQuery {
                usersByIds(id: [ID]): [User]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("UserService", userServiceSchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authors {id} authorDetails {name}}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id authorDetails {authorId} authorDetails {name}}}"
        def issue1 = [id: "ISSUE-1", authorDetails: [[authorId: "USER-1", name: "User 1"], [authorId: "USER-2", name: "User 2"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1]])


        def expectedQuery2 = "query nadel_2_UserService {usersQuery {usersByIds(id:[\"USER-1\",\"USER-2\"]) {id object_identifier__UUID:id}}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"], [id: "USER-2", name: "User 2", object_identifier__UUID: "USER-2"]]
        def response2 = new ServiceExecutionResult([usersQuery: [usersByIds: batchResponse1]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        def issue1Result = [id: "ISSUE-1", authorDetails: [[name: "User 1"], [name: "User 2"]], authors: [[id: "USER-1"], [id: "USER-2"]]]
        resultData(response) == [issues: [issue1Result]]

        resultComplexityAggregator.getTotalNodeCount() == 13
        resultComplexityAggregator.getNodeCountsForService("Issues") == 9
        resultComplexityAggregator.getNodeCountsForService("UserService") == 4

    }


    def "one synthetic hydration call with longer path arguments and merged fields"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
            authors: [IssueUser]
        }
        type IssueUser {
            authorId: ID
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersQuery: UserQuery
        }
        type UserQuery {
           usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
            name: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authors: [User] => hydrated from UserService.usersQuery.usersByIds(id: $source.authors.authorId) object identified by id, batch size 2
            }
        }
        service UserService {
            type Query {
                usersQuery: UserQuery
            }
            type UserQuery {
               usersByIds(id: [ID]): [User]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("UserService", userServiceSchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authors {name id}}}"
        def expectedQuery1 = "query nadel_2_Issues {issues {id authors {authorId}}}"
        def issue1 = [id: "ISSUE-1", authors: [[authorId: "USER-1"], [authorId: "USER-2"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1]])

        def expectedQuery2 = "query nadel_2_UserService {usersQuery {usersByIds(id:[\"USER-1\",\"USER-2\"]) {name id object_identifier__UUID:id}}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"], [id: "USER-2", name: "User 2", object_identifier__UUID: "USER-2"]]
        def response2 = new ServiceExecutionResult([usersQuery: [usersByIds: batchResponse1]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        def issue1Result = [id: "ISSUE-1", authors: [[id: "USER-1", name: "User 1"], [id: "USER-2", name: "User 2"]]]
        resultData(response) == [issues: [issue1Result]]

        resultComplexityAggregator.getTotalNodeCount() == 11
        resultComplexityAggregator.getNodeCountsForService("Issues") == 5
        resultComplexityAggregator.getNodeCountsForService("UserService") == 6
    }

    def "Expecting one child Error on extensive field argument passed to synthetic hydration"() {
        given:
        def boardSchema = TestUtil.schema("""
        type Query {
            board(id: ID) : Board
        }
        type Board {
            id: ID
            issueChildren: [Card]
        }
        type Card {
            id: ID
            issue: Issue
        }
        
        type Issue {
            id: ID
            assignee: TestUser
        }
        
        type TestUser {
            accountId: String
        }
        """)

        def identitySchema = TestUtil.schema("""
        type Query {
            usersQuery: UserQuery
        }
        type UserQuery {
            users(accountIds: [ID]): [User]
        }
        type User {
            accountId: ID
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service TestBoard {
            type Query {
                board(id: ID) : SoftwareBoard
            }
            
            type SoftwareBoard => renamed from Board {
                id: ID
                cardChildren: [SoftwareCard] => renamed from issueChildren
            }
            
            type SoftwareCard => renamed from Card {
                id: ID
                assignee: User => hydrated from Users.usersQuery.users(accountIds: $source.issue.assignee.accountId) object identified by accountId, batch size 3
            }
        }
       
        service Users {
            type Query {
                usersQuery: UserQuery
            }
            type UserQuery {
                users(accountIds: [ID]): [User]
            }
            type User {
                accountId: ID
            }
        }
        ''')

        def query = '''{
                        board(id:1) {
                            id 
                            cardChildren { 
                                assignee { 
                                    accountId
                                 } 
                            }
                        }
                        }'''

        def expectedQuery1 = "query nadel_2_TestBoard {board(id:1) {id issueChildren {issue {assignee {accountId}}}}}"
        def data1 = [board: [id: "1", issueChildren: [[issue: [assignee: [accountId: "1"]]], [issue: [assignee: [accountId: "2"]]], [issue: [assignee: [accountId: "3"]]]]]]
        def response1 = new ServiceExecutionResult(data1)

        def expectedQuery2 = "query nadel_2_Users {usersQuery {users(accountIds:[\"1\",\"2\",\"3\"]) {accountId object_identifier__UUID:accountId}}}"
        def response2 = new ServiceExecutionResult([usersQuery: [users: [[accountId: "1", object_identifier__UUID: "1"], [accountId: "2", object_identifier__UUID: "2"], [accountId: "3", object_identifier__UUID: "3"]]]])

        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("board")
        def service1 = new Service("TestBoard", boardSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("Users", identitySchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)

        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [board: [id: "1", cardChildren: [[assignee: [accountId: "1"]], [assignee: [accountId: "2"]], [assignee: [accountId: "3"]]]]]

    }


    def "extending types via hydration with arguments passed on"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issue: Issue
        }
        type Issue  {
            id: ID
        }
        """)
        def associationSchema = TestUtil.schema("""
        type Query {
            association(id: ID, filter: Filter): Association
        }
        
        input Filter  {
            name: String
        }
        
        type Association {
            id: ID
            nameOfAssociation: String
        }
        """)


        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issue {
            type Query {
                issue: Issue
            }
            type Issue  {
                id: ID
            }
        }
            
        service Association {
            type Query {
                association(id: ID, filter: Filter): Association
            }
            
            input Filter  {
                name: String
            }
            
            type Association {
                id: ID
                nameOfAssociation: String
            }
            extend type Issue {
                association(filter:Filter): Association => hydrated from Association.association(id: \$source.id, filter: \$argument.filter)
            } 
       
        }
        ''')

        def query = '''{
                        issue {
                            association(filter: {name: "value"}){
                                nameOfAssociation
                            }
                        }
                        }'''

        def expectedQuery1 = "query nadel_2_Issue {issue {id}}"
        def response1 = [issue: [id: "ISSUE-1"]]


        def expectedQuery2 = """query nadel_2_Association {association(id:"ISSUE-1",filter:{name:"value"}) {nameOfAssociation}}"""
        def response2 = [association: [nameOfAssociation: "ASSOC NAME"]]
        def overallResponse = [issue: [association: [nameOfAssociation: "ASSOC NAME"]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2Services(
                overallSchema,
                "Issue",
                issueSchema,
                "Association",
                associationSchema,
                query,
                ["issue"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2
        )

        then:
        response == overallResponse
        errors.size() == 0
    }


}

