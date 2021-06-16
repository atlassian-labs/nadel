package graphql.nadel.e2e


import graphql.ErrorType
import graphql.GraphqlErrorBuilder
import graphql.execution.ExecutionStepInfo
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.testutils.TestUtil
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.hooks.ServiceOrError
import spock.lang.Specification

import static graphql.nadel.NadelEngine.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.engine.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2EDynamicServiceResolutionTest extends Specification {
    def nsdl = [
            common: '''
                    common {
                        directive @dynamicServiceResolution on FIELD_DEFINITION
                        
                        type Query {
                            node(id: ID!): Node @dynamicServiceResolution
                        } 
                        
                        interface Node {
                            id: ID!
                        }
                    }
            ''',
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
    ]

    def repoSchema = typeDefinitions('''
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
        ''')

    def issueSchema = typeDefinitions("""
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

    def "simple success case"() {
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
        ServiceExecution repoExecution = Mock(ServiceExecution)
        ServiceExecution issueExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                RepoService: new Tuple2(repoExecution, repoSchema),
                IssueService: new Tuple2(issueExecution, issueSchema)
        ])
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .serviceExecutionHooks(serviceHooks)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        Map response1 = [node: [id: "123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(response1)

        repoExecution.execute(_) >>
                completedFuture(topLevelResult)

        when:
        def result = nadel.execute(nadelExecutionInput).get()

        then:
        result.data == [node: [id: "123", description: "this is a pull request"]]
        result.errors.size() == 0
    }

    def "multiple services"() {
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
        ServiceExecution repoServiceExecution = Mock(ServiceExecution)
        ServiceExecution issueServiceExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                RepoService: new Tuple2(repoServiceExecution, repoSchema),
                IssueService: new Tuple2(issueServiceExecution, issueSchema)
        ])
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .serviceExecutionHooks(serviceHooks)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        Map repoResponse = [pr: [id: "pull-request:id-123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]
        Map issueResponse = [issue: [id: "issue/id-123", issueKey: "ISSUE-1", type_hint_typename__UUID: "Issue"]]
        ServiceExecutionResult repoExecutionResult = new ServiceExecutionResult(repoResponse)
        ServiceExecutionResult issueExecutionResult = new ServiceExecutionResult(issueResponse)

        repoServiceExecution.execute(_) >> completedFuture(repoExecutionResult)
        issueServiceExecution.execute(_) >> completedFuture(issueExecutionResult)

        when:
        def result = nadel.execute(nadelExecutionInput).get()

        then:
        result.data == [
                pr: [id: "pull-request:id-123", description: "this is a pull request"],
                issue: [id: "issue/id-123", issueKey: "ISSUE-1"]
        ]
        result.errors.size() == 0
    }

    def "multiple services with one unmapped node lookup"() {
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
        ServiceExecution issueServiceExecution = Mock(ServiceExecution)
        ServiceExecution repoServiceExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                RepoService: new Tuple2(repoServiceExecution, repoSchema),
                IssueService: new Tuple2(issueServiceExecution, issueSchema)
        ])

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .serviceExecutionHooks(serviceHooks)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        Map issueResponse = [issue: [id: "issue/id-123", issueKey: "ISSUE-1", type_hint_typename__UUID: "Issue"]]
        ServiceExecutionResult issueExecutionResult = new ServiceExecutionResult(issueResponse)

        issueServiceExecution.execute(_) >> completedFuture(issueExecutionResult)

        when:
        def result = nadel.execute(nadelExecutionInput).get()
        then:

        result.data == [
                commit: null,
                issue: [id: "issue/id-123", issueKey: "ISSUE-1"]
        ]
        result.errors.size() == 1
        result.errors.get(0).message == "Could not resolve service for field: /commit"
    }

    def "handles multiple inline fragments"() {
        def query = '''
            {
                node(id: "pull-request:id-123") {
                   ... on PullRequest {
                        id
                        description
                   }
                   ... on Issue {
                        id
                        issueKey
                   }
                }
            }
        '''

        // Have a look at NormalizedQueryToAstCompiler
        def expectedQuery = '''
        {
                node(id: "pull-request:id-123") {
                   ... on PullRequest {
                        id
                   }
                   ... on PullRequest {
                        description
                   }
                   ... on PullRequest {
                        author {
                            name
                        }
                   }
                }
        }
        '''
        ServiceExecution repoExecution = Mock(ServiceExecution)
        ServiceExecution issueExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                RepoService: new Tuple2(repoExecution, repoSchema),
                IssueService: new Tuple2(issueExecution, issueSchema)
        ])
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .serviceExecutionHooks(serviceHooks)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        Map response1 = [node: [id: "123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(response1)

        repoExecution.execute(_) >>
                completedFuture(topLevelResult)

        when:
        def result = nadel.execute(nadelExecutionInput).get()

        then:
        result.data == [node: [id: "123", description: "this is a pull request"]]
        result.errors.size() == 0
    }

    def "handles complex fragments"() {
        def query = '''
            {
                node(id: "pull-request:id-123") {
                   ... {
                       ... on PullRequest {
                            id
                       }
                   }
                   ... on PullRequest {
                        description
                   }
                }
            }
        '''
        ServiceExecution repoExecution = Mock(ServiceExecution)
        ServiceExecution issueExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                RepoService: new Tuple2(repoExecution, repoSchema),
                IssueService: new Tuple2(issueExecution, issueSchema)
        ])
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .serviceExecutionHooks(serviceHooks)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        Map response1 = [node: [id: "123", description: "this is a pull request", type_hint_typename__UUID: "PullRequest"]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(response1)

        repoExecution.execute(_) >>
                completedFuture(topLevelResult)

        when:
        def result = nadel.execute(nadelExecutionInput).get()

        then:
        result.data == [node: [id: "123", description: "this is a pull request"]]
        result.errors.size() == 0
    }
}
