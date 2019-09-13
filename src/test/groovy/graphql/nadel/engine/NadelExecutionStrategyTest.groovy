package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.execution.ExecutionId
import graphql.execution.nextgen.ExecutionHelper
import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.execution.nextgen.result.RootExecutionResultNode
import graphql.nadel.DefinitionRegistry
import graphql.nadel.FieldInfo
import graphql.nadel.FieldInfos
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.testutils.TestUtil.parseQuery
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelExecutionStrategyTest extends Specification {

    ExecutionHelper executionHelper
    def service1Execution
    def service2Execution
    def serviceDefinition
    def definitionRegistry
    def instrumentation
    def serviceExecutionHooks

    void setup() {
        executionHelper = new ExecutionHelper()
        service1Execution = Mock(ServiceExecution)
        service2Execution = Mock(ServiceExecution)
        serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        definitionRegistry = Mock(DefinitionRegistry)
        instrumentation = new NadelInstrumentation() {}
        serviceExecutionHooks = new ServiceExecutionHooks() {}
    }

    def "one call to one service"() {
        given:
        def underlyingSchema = TestUtil.schema("""
        type Query {
            foo: String  
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo: String
        }
        """)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo}"
        def executionData = createExecutionData(query, overallSchema)

        def expectedQuery = "query nadel_2_service {foo}"

        when:
        nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery
        } as ServiceExecutionParameters) >> completedFuture(new ServiceExecutionResult(null))
    }

    def "one call to one service with list result"() {
        given:
        def underlyingSchema = TestUtil.schema("""
        type Query {
            foo: [String]
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo: [String]
        }
        """)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo}"
        def executionData = createExecutionData(query, overallSchema)

        def expectedQuery = "query nadel_2_service {foo}"

        def serviceResultData = [foo: ["foo1", "foo2"]]

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery
        } as ServiceExecutionParameters) >> completedFuture(new ServiceExecutionResult(serviceResultData))
        resultData(response) == [foo: ["foo1", "foo2"]]
    }


    def underlyingHydrationSchema1 = TestUtil.schema("""
        type Query {
            foo(id : ID) : Foo
        }
        type Foo {
            id: ID
            barId: ID
            fooDetails: FooDetails
        }
        type FooDetails {
            externalBarId: ID
        }
        """)

    def underlyingHydrationSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

    def overallHydrationSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo(id : ID): Foo
            }
            type Foo {
                id: ID
                bar: Bar => hydrated from service2.barById(id: $source.barId)
                barLongerInput: Bar => hydrated from service2.barById(id: $source.fooDetails.externalBarId)
            }
        }
        service service2 {
            type Query {
                barById(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')


    def "one hydration call with variables defined"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query($var : ID, $unusedVar2 : ID) {foo(id : $var) {bar{id name}}}
        '''
        def expectedQuery1 = 'query nadel_2_service1($var:ID) {foo(id:$var) {barId}}'
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def executionData = createExecutionData(query, overallHydrationSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery1
        } as ServiceExecutionParameters) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({
            printAstCompact(it.query) == expectedQuery2
        } as ServiceExecutionParameters) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [id: "barId", name: "Bar1"]]]
    }


    def "one hydration call with fragments defined"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query { foo { ... frag1 } } 
            
            fragment frag1 on Foo {
                bar { id name }
            }
            fragment unusedFrag2 on Foo {
                bar { id name }
            }

        '''
        def expectedQuery1 = 'query nadel_2_service1 {foo {...frag1}} fragment frag1 on Foo {barId}'
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def document = parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).context(NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build()) build()
        def executionData = executionHelper.createExecutionData(document, overallHydrationSchema, ExecutionId.generate(), executionInput, null)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [id: "barId", name: "Bar1"]]]
    }

    def "basic hydration"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query { foo { bar { name } } } 
        '''
        def expectedQuery1 = 'query nadel_2_service1 {foo {barId}}'
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def document = parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).context(NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build()) build()
        def executionData = executionHelper.createExecutionData(document, overallHydrationSchema, ExecutionId.generate(), executionInput, null)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [name: "Bar1"]]]
    }

    def "one hydration call with input value having longer path"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query { foo { barLongerInput  { name } } } 
        '''
        def expectedQuery1 = 'query nadel_2_service1 {foo {fooDetails {externalBarId}}}'
        def response1 = new ServiceExecutionResult([foo: [fooDetails: [externalBarId: "barId"]]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def document = parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).context(NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build()) build()
        def executionData = executionHelper.createExecutionData(document, overallHydrationSchema, ExecutionId.generate(), executionInput, null)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [barLongerInput: [name: "Bar1"]]]
    }


    def "one hydration call with longer path and same named overall field"() {
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
                authors: [User] => hydrated from UserService.usersByIds(id: $source.authorDetails.authorId) object identified by id, batch size 2
            }
            type AuthorDetail {
                name: String
            }
        }
        service UserService {
            type Query {
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


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\"]) {id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"], [id: "USER-2", name: "User 2", object_identifier__UUID: "USER-2"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


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

    }

    def "one hydration call with longer path arguments and merged fields"() {
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
                authors: [User] => hydrated from UserService.usersByIds(id: $source.authors.authorId) object identified by id, batch size 2
            }
        }
        service UserService {
            type Query {
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


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\"]) {name id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"], [id: "USER-2", name: "User 2", object_identifier__UUID: "USER-2"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


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

    }

    def "batching of hydration list"() {
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
            usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authors: [User] => hydrated from UserService.usersByIds(id: $source.authorIds) object identified by id, batch size 3
            }
        }
        service UserService {
            type Query {
                usersByIds(id: [ID]): [User]
            }
            type User {
                id: ID
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("UserService", userServiceSchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authors {id}}}"
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds}}"
        def issue1 = [id: "ISSUE-1", authorIds: ["USER-1", "USER-2"]]
        def issue2 = [id: "ISSUE-2", authorIds: ["USER-3"]]
        def issue3 = [id: "ISSUE-3", authorIds: ["USER-2", "USER-4", "USER-5",]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2, issue3]])


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\",\"USER-3\"]) {id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-3", object_identifier__UUID: "USER-3"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def expectedQuery3 = "query nadel_2_UserService {usersByIds(id:[\"USER-2\",\"USER-4\",\"USER-5\"]) {id object_identifier__UUID:id}}"
        def batchResponse2 = [[id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-4", object_identifier__UUID: "USER-4"], [id: "USER-5", object_identifier__UUID: "USER-5"]]
        def response3 = new ServiceExecutionResult([usersByIds: batchResponse2])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery3
        }) >> completedFuture(response3)


        def issue1Result = [id: "ISSUE-1", authors: [[id: "USER-1"], [id: "USER-2"]]]
        def issue2Result = [id: "ISSUE-2", authors: [[id: "USER-3"]]]
        def issue3Result = [id: "ISSUE-3", authors: [[id: "USER-2"], [id: "USER-4"], [id: "USER-5"]]]
        resultData(response) == [issues: [issue1Result, issue2Result, issue3Result]]

    }

    def "batching of hydration list with flattened arguments"() {
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
            usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authors: [User] => hydrated from UserService.usersByIds(id: $source.authors.authorId) object identified by id, batch size 3
            }
        }
        service UserService {
            type Query {
                usersByIds(id: [ID]): [User]
            }
            type User {
                id: ID
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("UserService", userServiceSchema, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authors {id}}}"
        def expectedQuery1 = "query nadel_2_Issues {issues {id authors {authorId}}}"
        def issue1 = [id: "ISSUE-1", authors: [[authorId: "USER-1"], [authorId: "USER-2"]]]
        def issue2 = [id: "ISSUE-2", authors: [[authorId: "USER-3"]]]
        def issue3 = [id: "ISSUE-3", authors: [[authorId: "USER-2"], [authorId: "USER-4"], [authorId: "USER-5"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2, issue3]])


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\",\"USER-3\"]) {id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-3", object_identifier__UUID: "USER-3"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def expectedQuery3 = "query nadel_2_UserService {usersByIds(id:[\"USER-2\",\"USER-4\",\"USER-5\"]) {id object_identifier__UUID:id}}"
        def batchResponse2 = [[id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-4", object_identifier__UUID: "USER-4"], [id: "USER-5", object_identifier__UUID: "USER-5"]]
        def response3 = new ServiceExecutionResult([usersByIds: batchResponse2])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


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
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery3
        }) >> completedFuture(response3)


        def issue1Result = [id: "ISSUE-1", authors: [[id: "USER-1"], [id: "USER-2"]]]
        def issue2Result = [id: "ISSUE-2", authors: [[id: "USER-3"]]]
        def issue3Result = [id: "ISSUE-3", authors: [[id: "USER-2"], [id: "USER-4"], [id: "USER-5"]]]
        resultData(response) == [issues: [issue1Result, issue2Result, issue3Result]]

    }


    def "hydration list"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: [ID]
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barById(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId1\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId1", name: "Bar1"]])

        def expectedQuery3 = "query nadel_2_service2 {barById(id:\"barId2\") {id name}}"
        def response3 = new ServiceExecutionResult([barById: [id: "barId2", name: "Bar3"]])

        def expectedQuery4 = "query nadel_2_service2 {barById(id:\"barId3\") {id name}}"
        def response4 = new ServiceExecutionResult([barById: [id: "barId3", name: "Bar4"]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery3
        }) >> completedFuture(response3)
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery4
        }) >> completedFuture(response4)

        resultData(response) == [foo: [bar: [[id: "barId1", name: "Bar1"], [id: "barId2", name: "Bar3"], [id: "barId3", name: "Bar4"]]]]
    }

    def "rename with first path element returning null"() {

        def issueSchema = TestUtil.schema("""
        type Query {
            issue : Issue
        }
        type Issue {
            details: IssueDetails
        }
        type IssueDetails {
            name: String
        }
        """)
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue
            }
            type Issue {
                name: String => renamed from details.name
            }
        }
        ''')
        def issueFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issue")
        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issueFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issue {name}}"
        def expectedQuery1 = "query nadel_2_Issues {issue {details {name}}}"
        def response1 = new ServiceExecutionResult([issue: [details: null]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        resultData(response) == [issue: [name: null]]
    }

    def "rename with first last path element returning null"() {

        def issueSchema = TestUtil.schema("""
        type Query {
            issue : Issue
        }
        type Issue {
            details: IssueDetails
        }
        type IssueDetails {
            name: String
        }
        """)
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue
            }
            type Issue {
                name: String => renamed from details.name
            }
        }
        ''')
        def issueFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issue")
        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issueFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issue {name}}"
        def expectedQuery1 = "query nadel_2_Issues {issue {details {name}}}"
        def response1 = new ServiceExecutionResult([issue: [details: [name: null]]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        resultData(response) == [issue: [name: null]]
    }

    def "hydration list with batching"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: [ID]
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barsById(id: [ID]): [Bar]
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barsById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barsById(id: [ID]): [Bar]
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{ name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barsById(id:[\"barId1\",\"barId2\",\"barId3\"]) {name object_identifier__UUID:id}}"
        def response2 = new ServiceExecutionResult([barsById: [[object_identifier__UUID: "barId1", name: "Bar1"], [object_identifier__UUID: "barId2", name: "Bar2"], [object_identifier__UUID: "barId3", name: "Bar3"]]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [[name: "Bar1"], [name: "Bar2"], [name: "Bar3"]]]]
    }

    def "hydration batching returns null"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: [ID]
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barsById(id: [ID]): [Bar]
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barsById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barsById(id: [ID]): [Bar]
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{ name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barsById(id:[\"barId1\",\"barId2\",\"barId3\"]) {name object_identifier__UUID:id}}"
        def response2 = new ServiceExecutionResult(null)

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [null, null, null]]]
    }

    def "hydration list with one element"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: [ID]
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barById(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1"]]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId1\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId1", name: "Bar1"]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> completedFuture(response2)

        resultData(response) == [foo: [bar: [[id: "barId1", name: "Bar1"]]]]
    }

    FieldInfos topLevelFieldInfo(GraphQLFieldDefinition fieldDefinition, Service service) {
        FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, fieldDefinition)
        return new FieldInfos([(fieldDefinition): fieldInfo])
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, GraphQLSchema overallSchema) {
        createExecutionData(query, [:], overallSchema)
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, Map<String, Object> variables, GraphQLSchema overallSchema) {
        def document = parseQuery(query)

        def nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).artificialFieldsUUID("UUID").build()
        def executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .context(nadelContext)
                .build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null)
        executionData
    }

    Object resultData(CompletableFuture<RootExecutionResultNode> response) {
        ResultNodesUtil.toExecutionResult(response.get()).data
    }


    def "two deep renames"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorDetails: AuthorDetail
        }
        type AuthorDetail {
            authorId: ID
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
                authorId: ID => renamed from authorDetails.authorId
                authorName: ID => renamed from authorDetails.name
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authorId authorName}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id authorDetails {authorId} authorDetails {name}}}"
        def issue1 = [id: "ISSUE-1", authorDetails: [authorId: "USER-1", name: "User 1"]]
        def issue2 = [id: "ISSUE-2", authorDetails: [authorId: "USER-2", name: "User 2"]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2]])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def issue1Result = [id: "ISSUE-1", authorId: "USER-1", authorName: "User 1"]
        def issue2Result = [id: "ISSUE-2", authorId: "USER-2", authorName: "User 2"]
        resultData(response) == [issues: [issue1Result, issue2Result]]

    }

    def "two deep renames, merged fields with same path and field rename"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issue : Issue
        }
        type Issue {
            id: ID
            authorDetails: AuthorDetail
        }
        type AuthorDetail {
            authorId: ID
            name: String
            extraInfo: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue
            }
            type Issue {
                id: ID
                authorId: ID => renamed from authorDetails.authorId
                authorName: ID => renamed from authorDetails.name
                details: AuthorDetail  => renamed from authorDetails
            }
            type AuthorDetail {
                extra: String => renamed from extraInfo
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issue")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issue {id authorId authorName details { extra }}}"

        def expectedQuery1 = "query nadel_2_Issues {issue {id authorDetails {authorId} authorDetails {name} authorDetails {extraInfo}}}"
        def issue1 = [id: "ISSUE-1", authorDetails: [authorId: "USER-1", name: "User 1", extraInfo: "extra 1"]]
        def response1 = new ServiceExecutionResult([issue: issue1])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def issue1Result = [id: "ISSUE-1", authorId: "USER-1", authorName: "User 1", details: [extra: "extra 1"]]
        resultData(response) == [issue: issue1Result]

    }

    def "deep rename of an object"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorDetails: AuthorDetail
        }
        type AuthorDetail {
            name: Name 
        }
        type Name {
            fName: String
            lName: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authorName: Name => renamed from authorDetails.name
            }
            type Name {
                firstName: String => renamed from fName
                lastName: String => renamed from lName
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authorName {firstName lastName}}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id authorDetails {name {fName lName}}}}"
        def issue1 = [id: "ISSUE-1", authorDetails: [name: [fName: "George", lName: "Smith"]]]
        def issue2 = [id: "ISSUE-2", authorDetails: [name: [fName: "Elizabeth", lName: "Windsor"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2]])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def issue1Result = [id: "ISSUE-1", authorName: [firstName: "George", lastName: "Smith"]]
        def issue2Result = [id: "ISSUE-2", authorName: [firstName: "Elizabeth", lastName: "Windsor"]]
        resultData(response) == [issues: [issue1Result, issue2Result]]
    }

    def "deep rename of list"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            details : [IssueDetail]
        }
        type IssueDetail {
            issue: Issue
        }
        type Issue {
            labels: [String] 
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                details: [IssueDetail]
            }
            type IssueDetail {
                labels: [String] => renamed from issue.labels
            }
        }
        ''')
        def detailsDefinition = overallSchema.getQueryType().getFieldDefinition("details")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(detailsDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{details {labels}}"

        def expectedQuery1 = "query nadel_2_Issues {details {issue {labels}}}"
        def detail1 = [issue: [labels: ["label1", "label2"]]]
        def response1 = new ServiceExecutionResult([details: [detail1]])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def detail1Result = [labels: ["label1", "label2"]]
        resultData(response) == [details: [detail1Result]]
    }

    def "deep rename of list of list"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            details : [IssueDetail]
        }
        type IssueDetail {
            issue: Issue
        }
        type Issue {
            labels: [[String]] 
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                details: [IssueDetail]
            }
            type IssueDetail {
                labels: [[String]] => renamed from issue.labels
            }
        }
        ''')
        def detailsDefinition = overallSchema.getQueryType().getFieldDefinition("details")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(detailsDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{details {labels}}"

        def expectedQuery1 = "query nadel_2_Issues {details {issue {labels}}}"
        def detail1 = [issue: [labels: [["label1", "label2"], ["label3"]]]]
        def response1 = new ServiceExecutionResult([details: [detail1]])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def detail1Result = [labels: [["label1", "label2"], ["label3"]]]
        resultData(response) == [details: [detail1Result]]
    }

    def "deep rename of an object with transformations inside object"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorDetails: AuthorDetail
        }
        type AuthorDetail {
            name: OriginalName 
        }
        type OriginalName {
            originalFirstName: String
            lastName: String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authorName: Name => renamed from authorDetails.name
            }
            type Name => renamed from OriginalName {
                firstName: String => renamed from originalFirstName
                lastName: String
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("issues")

        def service1 = new Service("Issues", issueSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{issues {id authorName {firstName lastName}}}"

        def expectedQuery1 = "query nadel_2_Issues {issues {id authorDetails {name {originalFirstName lastName}}}}"
        def issue1 = [id: "ISSUE-1", authorDetails: [name: [originalFirstName: "George", lastName: "Smith"]]]
        def issue2 = [id: "ISSUE-2", authorDetails: [name: [originalFirstName: "Elizabeth", lastName: "Windsor"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2]])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)

        def issue1Result = [id: "ISSUE-1", authorName: [firstName: "George", lastName: "Smith"]]
        def issue2Result = [id: "ISSUE-2", authorName: [firstName: "Elizabeth", lastName: "Windsor"]]
        resultData(response) == [issues: [issue1Result, issue2Result]]
    }

    def "hydration call with argument value from original field argument "() {
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
            usersByIds(extraArg: String, id: [ID]): [User]
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
                author(extraArg: String): User => hydrated from UserService.usersByIds(extraArg: $argument.extraArg,id: $source.authorId) object identified by id, batch size 2
            }
        }
        service UserService {
            type Query {
                usersByIds(extraArg: String, id: [ID]): [User]
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


        def query = '{issues {id author(extraArg: "extraArg") {name} }}'
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorId}}"
        def issue1 = [id: "ISSUE-1", authorId: "USER-1"]
        def response1 = new ServiceExecutionResult([issues: [issue1]])


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\"],extraArg:\"extraArg\") {name object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


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

    }

    def "hydration call with two argument values from original field arguments "() {
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
                author(extraArg: String): User => hydrated from UserService.usersByIds(extraArg1: $argument.extraArg1, extraArg2: $argument.extraArg2, id: $source.authorId) object identified by id, batch size 2
            }
        }
        service UserService {
            type Query {
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


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\"],extraArg1:\"extraArg1\",extraArg2:10) {name object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


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

    }

    def "call with variables inside input objects"() {
        given:
        def serviceSchema = TestUtil.schema("""
        type Query {
            hello(arg: Arg, otherArg: String): String
        }
        input Arg {
            ids: [ID]
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service  MyService{
            type Query {
                hello(arg: Arg, otherArg: String): String
            }
            input Arg {
                ids: [ID]
            }
        }
        ''')
        def issuesFieldDefinition = overallSchema.getQueryType().getFieldDefinition("hello")

        def service1 = new Service("MyService", serviceSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(issuesFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = 'query myQuery($varIds: [ID], $otherVar: String) { hello(arg: {ids: $varIds}, otherArg: $otherVar) }'
        def expectedQuery1 = 'query nadel_2_MyService($varIds:[ID],$otherVar:String) {hello(arg:{ids:$varIds},otherArg:$otherVar)}'
        def response1 = new ServiceExecutionResult([hello: "world"])


        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            printAstCompact(sep.query) == expectedQuery1
        }) >> completedFuture(response1)


        resultData(response) == [hello: "world"]

    }

}
