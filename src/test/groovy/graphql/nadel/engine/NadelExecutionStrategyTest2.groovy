package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLError
import graphql.execution.ExecutionId
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.DefinitionRegistry
import graphql.nadel.FieldInfo
import graphql.nadel.FieldInfos
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.StrategyTestHelper
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.result.ResultNodesUtil
import graphql.nadel.result.RootExecutionResultNode
import graphql.nadel.schema.UnderlyingWiringFactory
import graphql.nadel.testutils.MockedWiringFactory
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.testutils.TestUtil.createNormalizedQuery
import static graphql.nadel.testutils.TestUtil.parseQuery
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelExecutionStrategyTest2 extends StrategyTestHelper {

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
        errors[0].message.contains("Cannot return null for non-nullable")

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
        errors[0].message.contains("Cannot return null for non-nullable")


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
        errors[0].message.contains("Cannot return null for non-nullable")

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
        errors[0].message.contains("Cannot return null for non-nullable")

    }
}

