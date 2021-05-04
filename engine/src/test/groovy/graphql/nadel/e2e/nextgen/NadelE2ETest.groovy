package graphql.nadel.e2e.nextgen

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.testutils.TestUtil
import spock.lang.Specification

import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.engine.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {

    def simpleNDSL = [MyService: '''
         service MyService {
            type Query {
                hello: World
            }
            type World {
                id: ID
                name: String
            }
            type Mutation {
                hello: String  
            }
            type Subscription {
                onWorldUpdate: World
                onAnotherUpdate: World
            }
         }
        ''']

    def simpleUnderlyingSchema = typeDefinitions('''
            type Query {
                hello: World
            }
            type World {
                id: ID
                name: String
            }
            type Mutation {
                hello: String
            }
            type Subscription {
                onWorldUpdate: World
                onAnotherUpdate: World
            }
        ''')

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)

    def "deep rename works"() {

        def nsdl = [IssueService: '''
         service IssueService {
            type Query{
                issue: Issue
            } 
            type Issue {
                name: String => renamed from detail.detailName
            }
         }
        ''']
        def underlyingSchema = typeDefinitions('''
            type Query{
                issue: Issue 
                
            } 
            type Issue {
                detail: IssueDetails
            }
            type IssueDetails {
                detailName: String
            }
        ''')
        ServiceExecution serviceExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                IssueService: new Tuple2(serviceExecution, underlyingSchema)]
        )

        given:
        def query = '''
        { issue { name } } 
        '''
        Nadel nadel = NextgenEngine.newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("uuid")
                .build()

        def data1 = [issue: [detail: [detailName: "My Issue"]]]
        // TODO: assert query equality
        1 * serviceExecution.execute(_) >> completedFuture(new ServiceExecutionResult(data1))

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        result.join().data == [issue: [name: "My Issue"]]
    }
}
