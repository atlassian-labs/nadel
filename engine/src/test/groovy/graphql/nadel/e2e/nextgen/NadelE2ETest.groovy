package graphql.nadel.e2e.nextgen

import graphql.GraphQLError
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.result.ResultComplexityAggregator
import graphql.nadel.engine.testutils.TestUtil
import graphql.nadel.hooks.ServiceExecutionHooks
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.engine.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {


    def "simple deep rename"() {

        def nsdl = [IssueService: '''
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue {
                name: String => renamed from detail.detailName
            }
         }
        ''']
        def underlyingSchema = '''
            type Query {
                issue: Issue 
            } 
            type Issue {
                detail: IssueDetails
            }
            type IssueDetails {
                detailName: String
            }
        '''
        def query = '''
        { issue { name } } 
        '''
        def expectedQuery = '''query {... on Query {issue {... on Issue {my_uuid:detail {... on IssueDetails {detailName}}}}}}'''
        def overallResponse = [issue: [name: "My Issue"]]
        def serviceResponse = [issue: [my_uuid: [detailName: "My Issue"]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "deep rename with interfaces"() {

        def serviceName = 'PetService'
        def nsdl = [(serviceName): '''
         service PetService {
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                name: String 
            }
            type Dog implements Pet {
                name: String  => renamed from detail.petName
            }
            type Cat implements Pet {
                name: String  => renamed from detail.petName
            }
         }
        ''']
        def underlyingSchema = '''
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                detail: PetDetails
            }
            type Dog implements Pet {
                detail: PetDetails
            }
            type Cat implements Pet {
                detail: PetDetails
            }
            type PetDetails {
                petName: String 
            }
        '''
        def query = '''
        { pets { name } } 
        '''
        def expectedQuery = '''query {... on Query {pets {... on Cat { detail {... on PetDetail {petName}}} ... on Dog { detail {... on PetDetail {petName}}}}}'''
        def serviceResponse = [pets: [[detail: [petName: "Tiger"]], [detail: [petName: "Luna"]]]]

        def overallResponse = [pets: [[name: "Tiger"], [name: "Luna"]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                serviceName,
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    Object[] test1Service(Map<String, String> overallSchema,
                          String serviceOneName,
                          String underlyingSchema,
                          String query,
                          String expectedQuery,
                          Map serviceResponse,
                          ServiceExecutionHooks serviceExecutionHooks = new ServiceExecutionHooks() {},
                          Map variables = [:],
                          ResultComplexityAggregator resultComplexityAggregator = null
    ) {
        def response1ServiceResult = new ServiceExecutionResult(serviceResponse)


        boolean calledService1 = false
        ServiceExecution serviceExecution = { ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            calledService1 = true
            assert printAstCompact(sep.query) == expectedQuery
            return completedFuture(response1ServiceResult)
        }

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                (serviceOneName): new Tuple2(serviceExecution, typeDefinitions(underlyingSchema))]
        )
        Nadel nadel = NextgenEngine.newNadel()
                .dsl(overallSchema)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("uuid")
                .build()


        def response = nadel.execute(nadelExecutionInput)

        def executionResult = response.get()
        assert calledService1

        return [executionResult.getData(), executionResult.getErrors()]
    }


}
