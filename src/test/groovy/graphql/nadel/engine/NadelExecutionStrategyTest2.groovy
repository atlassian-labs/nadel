package graphql.nadel.engine

import graphql.AssertException
import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.*
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLSchema

import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

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

        def executionData = createExecutionData(query, [:], overallSchema)

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

        def executionData = createExecutionData(query, [:], overallSchema)

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

        def executionData = createExecutionData(query, [:], overallSchema)

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

        def executionData = createExecutionData(query, [:], overallSchema)

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

    def "extending types via hydration with variables arguments"() {
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

        def query = '''query MyQuery($filter: Filter){
                        issue {
                            association(filter: $filter){
                                nameOfAssociation
                            }
                        }
                        }'''

        def expectedQuery1 = "query nadel_2_Issue {issue {id}}"
        def response1 = [issue: [id: "ISSUE-1"]]


        def expectedQuery2 = '''query nadel_2_Association($filter:Filter) {association(id:"ISSUE-1",filter:$filter) {nameOfAssociation}}'''
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
                response2,
                new ServiceExecutionHooks() {},
                [filter: [name: ["value"]]]
        )

        then:
        response == overallResponse
        errors.size() == 0
    }

    def "extending types via hydration returning a connection"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            synth: Synth
        }
        type Synth {
            issue: Issue
        }
        type Issue  {
            id: ID
        }
        """)
        def associationSchema = TestUtil.schema("""
        type Query {
            association(id: ID, filter: Filter): AssociationConnection
            pages: Pages
        }
        type Pages {
            page(id:ID): Page
        }
        type Page {
            id: ID
        }
        
        type AssociationConnection {
            nodes: [Association]
        }

        input Filter  {
            name: String
        }

        type Association {
            id: ID
            nameOfAssociation: String
            pageId: ID
        }
        """)


        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issue {
            type Query {
                synth: Synth
            }
            type Synth {
                issue: Issue
            }
            type Issue  {
                id: ID
            }
        }

        service Association {
            type Query {
                association(id: ID, filter: Filter): AssociationConnection
            }
            
            type AssociationConnection {
                nodes: [Association]
            }

            input Filter  {
                name: String
            }

            type Association {
                id: ID
                nameOfAssociation: String
                page: Page => hydrated from Association.pages.page(id: $source.pageId)
            }
            type Page {
                id: ID
            }
            extend type Issue {
                association(filter:Filter): AssociationConnection => hydrated from Association.association(id: $source.id, filter: $argument.filter)
            }

        }
        ''')

        def query = '''{
                        synth {
                            issue {
                                association(filter: {name: "value"}){
                                    nodes {
                                        page {
                                            id
                                        }
                                    }
                                }
                            }
                        }
                        }'''

        def expectedQuery1 = "query nadel_2_Issue {synth {issue {id}}}"
        def response1 = [synth: [issue: [id: "ISSUE-1"]]]


        def expectedQuery2 = """query nadel_2_Association {association(id:"ISSUE-1",filter:{name:"value"}) {nodes {pageId}}}"""
        def response2 = [association: [nodes: [[pageId: "1"]]]]

        def expectedQuery3 = """query nadel_2_Association {pages {page(id:"1") {id}}}"""
        def response3 = [pages: [page: [id: "1"]]]


        def overallResponse = [synth: [issue: [association: [nodes: [[page: [id: "1"]]]]]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2ServicesWith3Calls(
                overallSchema,
                "Issue",
                issueSchema,
                "Association",
                associationSchema,
                query,
                ["synth"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                expectedQuery3,
                response3

        )

        then:
        response == overallResponse
        errors.size() == 0
    }

    def "hydration matching using index"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIds(ids: [ID]): [User]
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
                authors: [User] => hydrated from UserService.usersByIds(ids: $source.authorIds) object indexed, batch size 5
            }
        }
        service UserService {
            type Query {
                usersByIds(ids: [ID]): [User]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')

        def query = '{issues {id authors {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds}}"
        def issue1 = [id: "ISSUE-1", authorIds: ['1']]
        def issue2 = [id: "ISSUE-2", authorIds: ['1', '2']]

        def expectedQuery2 = "query nadel_2_UserService {usersByIds(ids:[\"1\",\"1\",\"2\"]) {name}}"
        def user1 = [id: "USER-1", name: 'Name']
        def user2 = [id: "USER-2", name: 'Name 2']

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2Services(
                overallSchema,
                "Issues",
                issueSchema,
                "UserService",
                userServiceSchema,
                query,
                ["issues"],
                expectedQuery1,
                [issues: [issue1, issue2]],
                expectedQuery2,
                [usersByIds: [user1, user1, user2]]
        )


        then:
        def user1Result = [name: 'Name']
        def user2Result = [name: 'Name 2']
        def issue1Result = [id: "ISSUE-1", authors: [user1Result]]
        def issue2Result = [id: "ISSUE-2", authors: [user1Result, user2Result]]
        response == [issues: [issue1Result, issue2Result]]
        errors.size() == 0
    }

    def "hydration matching using index returning null"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIds(ids: [ID]): [User]
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
                authors: [User] => hydrated from UserService.usersByIds(ids: $source.authorIds) object indexed, batch size 5
            }
        }
        service UserService {
            type Query {
                usersByIds(ids: [ID]): [User]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')

        def query = '{issues {id authors {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds}}"
        def issue1 = [id: "ISSUE-1", authorIds: ['1']]
        def issue2 = [id: "ISSUE-2", authorIds: ['1', '2']]

        def expectedQuery2 = "query nadel_2_UserService {usersByIds(ids:[\"1\",\"1\",\"2\"]) {name}}"
        def user1 = [id: "USER-1", name: 'Name']
        def user2 = [id: "USER-2", name: 'Name 2']

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2Services(
                overallSchema,
                "Issues",
                issueSchema,
                "UserService",
                userServiceSchema,
                query,
                ["issues"],
                expectedQuery1,
                [issues: [issue1, issue2]],
                expectedQuery2,
                [usersByIds: [user1, null, user2]]
        )


        then:
        def user1Result = [name: 'Name']
        def user2Result = [name: 'Name 2']
        def issue1Result = [id: "ISSUE-1", authors: [user1Result]]
        def issue2Result = [id: "ISSUE-2", authors: [null, user2Result]]
        response == [issues: [issue1Result, issue2Result]]
        errors.size() == 0
    }

    def "hydration matching using index result size invariant mismatch"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIds(ids: [ID]): [User]
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
                authors: [User] => hydrated from UserService.usersByIds(ids: $source.authorIds) object indexed, batch size 5
            }
        }
        service UserService {
            type Query {
                usersByIds(ids: [ID]): [User]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')

        def query = '{issues {id authors {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds}}"
        def issue1 = [id: "ISSUE-1", authorIds: ['1']]
        def issue2 = [id: "ISSUE-2", authorIds: ['1', '2']]

        def expectedQuery2 = "query nadel_2_UserService {usersByIds(ids:[\"1\",\"1\",\"2\"]) {name}}"
        def user1 = [id: "USER-1", name: 'Name']

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2Services(
                overallSchema,
                "Issues",
                issueSchema,
                "UserService",
                userServiceSchema,
                query,
                ["issues"],
                expectedQuery1,
                [issues: [issue1, issue2]],
                expectedQuery2,
                [usersByIds: [user1, null]]
        )


        then:
        ExecutionException ex = thrown()

        ex.cause.getClass() in AssertException
        ex.cause.message == "If you use indexed hydration then you MUST follow a contract where the resolved nodes matches the size of the input arguments. We expected 3 returned nodes but only got 2"
    }

    def "hydration matching using index with arrays"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIssueIds(issueIds: [ID]): [[User]]
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
                authors: [User] => hydrated from UserService.usersByIssueIds(issueIds: $source.id) object indexed, batch size 5
            }
        }
        service UserService {
            type Query {
                usersByIssueIds(issueIds: [ID]): [[User]]
            }
            type User {
                id: ID
                name: String
            }
        }
        ''')

        def query = '{issues {id authors {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id id}}"
        def issue1 = [id: "ISSUE-1"]
        def issue2 = [id: "ISSUE-2"]

        def expectedQuery2 = "query nadel_2_UserService {usersByIssueIds(issueIds:[\"ISSUE-1\",\"ISSUE-2\"]) {name}}"
        def user1 = [name: 'Name']
        def user2 = [name: 'Name 2']

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test2Services(
                overallSchema,
                "Issues",
                issueSchema,
                "UserService",
                userServiceSchema,
                query,
                ["issues"],
                expectedQuery1,
                [issues: [issue1, issue2]],
                expectedQuery2,
                [usersByIssueIds: [[user1], [user1, user2]]]
        )


        then:
        def user1Result = [name: 'Name']
        def user2Result = [name: 'Name 2']
        def issue1Result = [id: "ISSUE-1", authors: [user1Result]]
        def issue2Result = [id: "ISSUE-2", authors: [user1Result, user2Result]]
        response == [issues: [issue1Result, issue2Result]]
        errors.size() == 0
    }

    Object[] test2ServicesWith3Calls(GraphQLSchema overallSchema,
                                     String serviceOneName,
                                     GraphQLSchema underlyingOne,
                                     String serviceTwoName,
                                     GraphQLSchema underlyingTwo,
                                     String query,
                                     List<String> topLevelFields,
                                     String expectedQuery1,
                                     Map response1,
                                     String expectedQuery2,
                                     Map response2,
                                     String expectedQuery3,
                                     Map response3,
                                     ServiceExecutionHooks serviceExecutionHooks = new ServiceExecutionHooks() {
                                     },
                                     Map variables = [:]
    ) {

        def response1ServiceResult = new ServiceExecutionResult(response1)
        def response2ServiceResult = new ServiceExecutionResult(response2)
        def response3ServiceResult = new ServiceExecutionResult(response3)

        boolean calledService1 = false
        ServiceExecution service1Execution = { ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            assert printAstCompact(sep.query) == expectedQuery1
            calledService1 = true
            return completedFuture(response1ServiceResult)
        }
        int calledService2Count = 0;
        ServiceExecution service2Execution = { ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            calledService2Count++
            if (calledService2Count == 1) {
                assert printAstCompact(sep.query) == expectedQuery2
                return completedFuture(response2ServiceResult)
            } else {
                assert printAstCompact(sep.query) == expectedQuery3
                return completedFuture(response3ServiceResult)
            }
        }
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def instrumentation = new NadelInstrumentation() {}

        def service1 = new Service(serviceOneName, underlyingOne, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service(serviceTwoName, underlyingTwo, service2Execution, serviceDefinition, definitionRegistry)

        Map fieldInfoByDefinition = [:]
        topLevelFields.forEach({ it ->
            def fd = overallSchema.getQueryType().getFieldDefinition(it)
            FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service1, fd)
            fieldInfoByDefinition.put(fd, fieldInfo)
        })
        FieldInfos fieldInfos = new FieldInfos(fieldInfoByDefinition)

        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def executionData = createExecutionData(query, variables, overallSchema)

        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, Mock(ResultComplexityAggregator))

        assert calledService1
        assert calledService2Count == 2

        return [resultData(response), resultErrors(response)]
    }
}

