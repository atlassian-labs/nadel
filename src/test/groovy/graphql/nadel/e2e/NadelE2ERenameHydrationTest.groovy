package graphql.nadel.e2e


import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ERenameHydrationTest extends Specification {

    def 'mutation with nested renamed fields, field types and a hydration call'() {
        def simpleNDSL = '''
         service IssuesService {
            type Query {
                findIssueOwner(id: ID): SpecificIssueOwner
            }
           
            type SpecificIssueOwner => renamed from IssueOwner {
                identity: String
            }
 
            type SpecificIssue => renamed from Issue  {
                id: ID
                name: SpecificIssueOwner => hydrated from IssuesService.findIssueOwner(id: $source.id)
            }

            type UpdateSpecificIssuePayload => renamed from UpdateIssuePayload {
                specificIssue: SpecificIssue => renamed from issue
            }
 
            type Mutation {
                updateSpecificIssue: UpdateSpecificIssuePayload => renamed from updateIssue
            }
         }
        '''

        def underlyingSchema = typeDefinitions('''
            type Query {
                findIssueOwner(id: ID): IssueOwner
            }
 
            type IssueOwner {
                identity: String
            }
 
            type Issue {
                id: ID
            }
 
            type UpdateIssuePayload {
                issue: Issue
            }
 
            type Mutation {
                updateIssue: UpdateIssuePayload
            }
        ''')

        def delegatedExecution = Mock(ServiceExecution)
        def serviceFactory = TestUtil.serviceFactory(delegatedExecution, underlyingSchema)

        def query = '''
        mutation { 
            updateSpecificIssue { 
                specificIssue { 
                    id
                    name {
                        identity
                    }
                }
            } 
        }
        '''

        given:
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()

        def topLevelData = [updateIssue: [issue: [id: "1"]]]

        def hydrationData = [findIssueOwner: [identity: "Luna"]]

        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult = new ServiceExecutionResult(hydrationData)

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >>
                completedFuture(topLevelResult)
        1 * delegatedExecution.execute(_) >>
                completedFuture(hydrationResult)

        result.join().data == [updateSpecificIssue: [specificIssue: [name: [identity: "Luna"]]]]
    }

    def 'hydration works when an ancestor field has been renamed'() {
        // Note: this bug happens when the root field has been renamed, and then a hydration occurs further down the tree
        // i.e. here we rename relationships to devOpsRelationships and hydrate devOpsRelationships/nodes[0]/issue

        def simpleNDSL = '''
         service IssueService {
            type DevOpsIssue => renamed from Issue {
                id: ID
            }
 
            type DevOpsRelationship => renamed from Relationship {
                devOpsIssue: DevOpsIssue => hydrated from IssueService.issue(id: $source.issueId)
            }
 
            type DevOpsRelationshipConnection => renamed from RelationshipConnection {
                nodes: [DevOpsRelationship]
            }
 
            type Query {
                devOpsRelationships: DevOpsRelationshipConnection => renamed from relationships
                devOpsIssue(id: ID): DevOpsIssue => renamed from issue
            }
         }
        '''

        def underlyingSchema = typeDefinitions('''
            type Issue {
                id: ID
            }
 
            type Relationship {
                issueId: ID
            }
 
            type RelationshipConnection {
                nodes: [Relationship]
            }
 
            type Query {
                relationships: RelationshipConnection
                issue(id: ID): Issue
            }
        ''')

        def delegatedExecution = Mock(ServiceExecution)
        def serviceFactory = TestUtil.serviceFactory(delegatedExecution, underlyingSchema)

        def query = '''
        query { 
            devOpsRelationships {
                nodes {
                    devOpsIssue {
                        id
                    }
                }
            }
        }
        '''

        given:
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()

        def topLevelData = [relationships: [
                nodes: [
                        [issueId: "1"],
                ]
        ]]

        def hydrationData = [issue: [id: "1"]]

        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult = new ServiceExecutionResult(hydrationData)

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        delegatedExecution.execute(_) >>> [
                completedFuture(topLevelResult),
                completedFuture(hydrationResult)
        ]

        result.join().data == [
                devOpsRelationships: [
                        nodes: [
                                [
                                        devOpsIssue: [id: "1"]
                                ]
                        ]
                ]
        ]
    }

}
