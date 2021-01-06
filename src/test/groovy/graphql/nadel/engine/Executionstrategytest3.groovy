package graphql.nadel.engine

import graphql.GraphQLError
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.*
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLSchema

import static graphql.language.AstPrinter.printAstCompact
import static java.util.concurrent.CompletableFuture.completedFuture

class Executionstrategytest3 extends StrategyTestHelper {

    ExecutionHelper executionHelper = new ExecutionHelper()
    def service1Execution = Mock(ServiceExecution)
    def service2Execution = Mock(ServiceExecution)
    def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
    def definitionRegistry = Mock(DefinitionRegistry)
    def instrumentation = new NadelInstrumentation() {}
    def serviceExecutionHooks = new ServiceExecutionHooks() {}
    def resultComplexityAggregator = new ResultComplexityAggregator()

    def "simple hydration"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.doo(id: $source.id)
              }
              type Foo {
                 id: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doo(id: ID): Foo
              } 
              type Bar {
                id: ID
              }
              type Foo {
                 id: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {id}}"
        def response1 = [foo: [id: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doo(id:\"ID\") {name}}"
        def response2 = [doo: [name: "name"]]
        def overallResponse = [foo: [cd: [name: "name"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "simple hydration returns single object for list input"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.foos(id: $source.id) object identified by id
              }
              type Foo {
                 id: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                foos(id: [ID]): [Foo]
              } 
              type Bar {
                id: ID
              }
              type Foo {
                 id: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {id}}"
        def response1 = [foo: [id: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {foos(id:[\"ID\"]) {name object_identifier__UUID:id}}"
        def response2 = [foos: [[name: "name", object_identifier__UUID:"ID"]]]
        def overallResponse = [foo: [cd: [name: "name"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "default batching hydration with two list sources"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
            names: [String]
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIds(id: [ID],names: [String]): [User] 
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
                authors: [User] => hydrated from UserService.usersByIds(id: $primarySource.authorIds, names: $source.names) object identified by id
            }
        }
        service UserService {
            type Query {
                usersByIds(id: [ID],names: [String]): [User] default batch size 3
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
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds extra_source_arg_names:names}}"
        def issue1 = [id: "ISSUE-1", authorIds: ["USER-1", "USER-2"], extra_source_arg_names: ["applebottom", "river"]]
        def issue2 = [id: "ISSUE-2", authorIds: ["USER-3"], extra_source_arg_names:["smithsondon"]]
        def issue3 = [id: "ISSUE-3", authorIds: ["USER-2", "USER-4", "USER-5"], extra_source_arg_names:["river", "hearth", "blast"]]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2, issue3]])


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\",\"USER-3\"],names:[\"applebottom\",\"river\",\"smithsondon\"]) {id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-3", object_identifier__UUID: "USER-3"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def expectedQuery3 = "query nadel_2_UserService {usersByIds(id:[\"USER-2\",\"USER-4\",\"USER-5\"],names:[\"river\",\"hearth\",\"blast\"]) {id object_identifier__UUID:id}}"
        def batchResponse2 = [[id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-4", object_identifier__UUID: "USER-4"], [id: "USER-5", object_identifier__UUID: "USER-5"]]
        def response3 = new ServiceExecutionResult([usersByIds: batchResponse2])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)


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

        resultComplexityAggregator.getTotalNodeCount() == 23
        resultComplexityAggregator.getFieldRenamesCount() == 0
        resultComplexityAggregator.getTypeRenamesCount() == 0
        resultComplexityAggregator.getNodeCountsForService("Issues") == 11
        resultComplexityAggregator.getNodeCountsForService("UserService") == 12
    }


    def "default batching hydration with two list sources and one source"() {
        given:
        def issueSchema = TestUtil.schema("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
            names: [String]
            admin: String
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersByIds(id: [ID],names: [String], admin: String): [User] 
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
                authors: [User] => hydrated from UserService.usersByIds(id: $primarySource.authorIds, names: $source.names, admin: $source.admin) object identified by id
            }
        }
        service UserService {
            type Query {
                usersByIds(id: [ID],names: [String], admin: String): [User] default batch size 3
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
        def expectedQuery1 = "query nadel_2_Issues {issues {id authorIds extra_source_arg_names:names extra_source_arg_admin:admin}}"
        def issue1 = [id: "ISSUE-1", authorIds: ["USER-1", "USER-2"], extra_source_arg_names: ["applebottom", "river"], extra_source_arg_admin:"George"]
        def issue2 = [id: "ISSUE-2", authorIds: ["USER-3"], extra_source_arg_names:["smithsondon"], extra_source_arg_admin:"George"]
        def issue3 = [id: "ISSUE-3", authorIds: ["USER-2", "USER-4", "USER-5"], extra_source_arg_names:["river", "hearth", "blast"], extra_source_arg_admin:"George"]
        def response1 = new ServiceExecutionResult([issues: [issue1, issue2, issue3]])


        def expectedQuery2 = "query nadel_2_UserService {usersByIds(id:[\"USER-1\",\"USER-2\",\"USER-3\"],names:[\"applebottom\",\"river\",\"smithsondon\"],admin:[\"George\",\"George\",\"George\"]) {id object_identifier__UUID:id}}"
        def batchResponse1 = [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-3", object_identifier__UUID: "USER-3"]]
        def response2 = new ServiceExecutionResult([usersByIds: batchResponse1])

        def expectedQuery3 = "query nadel_2_UserService {usersByIds(id:[\"USER-2\",\"USER-4\",\"USER-5\"],names:[\"river\",\"hearth\",\"blast\"],admin:[\"George\",\"George\",\"George\"]) {id object_identifier__UUID:id}}"
        def batchResponse2 = [[id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-4", object_identifier__UUID: "USER-4"], [id: "USER-5", object_identifier__UUID: "USER-5"]]
        def response3 = new ServiceExecutionResult([usersByIds: batchResponse2])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection, resultComplexityAggregator)


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

        resultComplexityAggregator.getTotalNodeCount() == 23
        resultComplexityAggregator.getFieldRenamesCount() == 0
        resultComplexityAggregator.getTypeRenamesCount() == 0
        resultComplexityAggregator.getNodeCountsForService("Issues") == 11
        resultComplexityAggregator.getNodeCountsForService("UserService") == 12
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, GraphQLSchema overallSchema) {
        createExecutionData(query, [:], overallSchema)
    }


}