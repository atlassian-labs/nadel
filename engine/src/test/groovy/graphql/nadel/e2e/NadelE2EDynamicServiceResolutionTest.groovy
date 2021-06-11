package graphql.nadel.e2e


import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.ExecutionStepInfo
import graphql.nadel.Service
import graphql.nadel.engine.StrategyTestHelper
import graphql.nadel.engine.result.ResultComplexityAggregator
import graphql.nadel.engine.testutils.TestUtil
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.hooks.ServiceOrError

class NadelE2EDynamicServiceResolutionTest extends StrategyTestHelper {
    def commonTypes = '''
            directive @dynamicServiceResolution on FIELD_DEFINITION
            
            type Query {
                node(id: ID!): Node @dynamicServiceResolution
            } 
            
            interface Node {
                id: ID!
            }
        '''

    def repoSchema = TestUtil.schema("""
            type Query {
                node(id: ID): Node
            }
           
            interface Node {
                id: ID!
            }
            
            type PullRequest implements Node {
                id: ID!
                description: String
            }
                
            type Commit implements Node {
                id: ID!
                hash: String
            }""")

    def issueSchema = TestUtil.schema("""
            type Query {
                node(id: ID): Node
            }
           
            interface Node {
                id: ID!
            }
            
            type Issue implements Node {
                id: ID!
                issueKey: String
            }""")

    def serviceHooks = new ServiceExecutionHooks() {
        @Override
        ServiceOrError resolveServiceForField(List<Service> services, ExecutionStepInfo executionStepInfo) {
            def idArgument = executionStepInfo.getArguments().get("id")

            if(idArgument.toString().contains("pull-request")) {
                return new ServiceOrError(services.stream().filter({ service -> (service.getName() == "RepoService") }).findFirst().get(), null)
            }

            if(idArgument.toString().contains("issue")) {
                return new ServiceOrError(services.stream().filter({ service -> (service.getName() == "IssueService") }).findFirst().get(), null)
            }

            return new ServiceOrError(
                    null,
                    GraphqlErrorBuilder.newError()
                            .message("Could not resolve service for field: %s", executionStepInfo.path)
                            .errorType(ErrorType.ExecutionAborted)
                            .path(executionStepInfo.getPath())
                            .build()
            )
        }
    }

    def 'simple success case'() {
        def overallSchema = TestUtil.schemaFromNdsl([
                RepoService: '''   
                    service RepoService {
                       type PullRequest implements Node {
                            id: ID!
                            description: String
                       }
                    }
                    '''
        ], commonTypes)

        def query = '''
            {
                node(id: "pull-request:id-123") {
                   ... on PullRequest {
                        id
                        description
                   }
                }
            }
        '''

        def expectedQuery1 = 'query nadel_2_RepoService {node(id:"pull-request:id-123") {... on PullRequest {id description} type_hint_typename__UUID:__typename}}'
        Map response1 = [node: [id: "123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                overallSchema,
                "RepoService",
                repoSchema,
                query,
                ["pullRequest"],
                expectedQuery1,
                response1,
                serviceHooks,
                Mock(ResultComplexityAggregator)
        )
        then:
        response == [node: [id: "123", description: "this is a pull request"]]
        errors.size() == 0

    }

    def 'multiple services'() {
        def overallSchema = TestUtil.schemaFromNdsl([
                RepoService: '''   
                    service RepoService {
                       type PullRequest implements Node {
                            id: ID!
                            description: String
                       }
                    }
                   ''',
                IssueService: '''   
                    service IssueService {
                       type Issue implements Node {
                            id: ID!
                            issueKey: String
                       }
                    }
                    '''
        ], commonTypes)

        def query = '''
            {
                pr:node(id: "pull-request:id-123") {
                   ... on PullRequest {
                        id
                        description
                   }
                }
                issue:node(id: "issue/id-123") {
                   ... on Issue {
                        id
                        issueKey
                   }
                }
            }
        '''
        def repoTestService = new TestService(
                name: "RepoService",
                schema: repoSchema,
                topLevelFields: ["pullRequest"],
                expectedQuery: 'query nadel_2_RepoService {pr:node(id:"pull-request:id-123") {... on PullRequest {id description} type_hint_typename__UUID:__typename}}',
                response: [pr: [id: "pull-request:id-123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]
        )

        def issueTestService = new TestService(
                name: "IssueService",
                schema: issueSchema,
                topLevelFields: ["issue"],
                expectedQuery: 'query nadel_2_IssueService {issue:node(id:"issue/id-123") {... on Issue {id issueKey} type_hint_typename__UUID:__typename}}',
                response: [issue: [id: "issue/id-123", issueKey: "ISSUE-1", type_hint_typename__UUID: "Issue"]]
        )

        Map response
        List<GraphQLError> errors
        when:

        (response, errors) = testServices(
                overallSchema,
                query,
                [repoTestService, issueTestService],
                serviceHooks,
                Mock(ResultComplexityAggregator)
        )
        then:
        response == [
                pr: [id: "pull-request:id-123", description: "this is a pull request"],
                issue: [id: "issue/id-123", issueKey: "ISSUE-1"]
        ]
        errors.size() == 0
    }

    def 'multiple services with one unmapped node lookup'() {
        def overallSchema = TestUtil.schemaFromNdsl([
                RepoService: '''
                    service RepoService {
                       type PullRequest implements Node {
                            id: ID!
                            description: String
                       }
                       type Commit implements Node {
                            id: ID!
                            hash: String
                       }
                    }
                   ''',
                IssueService: '''
                    service IssueService {
                       type Issue implements Node {
                            id: ID!
                            issueKey: String
                       }
                    }
                    '''
        ], commonTypes)

        def query = '''
            {
                commit:node(id: "commit:id-123") {
                   ... on Commit {
                        id
                        hash
                   }
                }
                issue:node(id: "issue/id-123") {
                   ... on Issue {
                        id
                        issueKey
                   }
                }
            }
        '''

        def issueTestService = new TestService(
                name: "IssueService",
                schema: issueSchema,
                topLevelFields: ["issue"],
                expectedQuery: 'query nadel_2_IssueService {issue:node(id:"issue/id-123") {... on Issue {id issueKey} type_hint_typename__UUID:__typename}}',
                response: [issue: [id: "issue/id-123", issueKey: "ISSUE-1", type_hint_typename__UUID: "Issue"]]
        )

        Map response
        List<GraphQLError> errors
        when:

        (response, errors) = testServices(
                overallSchema,
                query,
                [issueTestService],
                serviceHooks,
                Mock(ResultComplexityAggregator)
        )
        then:
        response == [
                commit: null,
                issue: [id: "issue/id-123", issueKey: "ISSUE-1"]
        ]
        errors.size() == 1
        errors.get(0).message == "Could not resolve service for field: /commit"
    }
}
