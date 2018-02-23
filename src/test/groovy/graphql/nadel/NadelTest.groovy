package graphql.nadel

import graphql.ExecutionInput
import graphql.nadel.dsl.ServiceDefinition
import spock.lang.Specification

class NadelTest extends Specification {


    GraphqlCallerFactory mockCallerFactory(Map callerMocks) {
        return new GraphqlCallerFactory() {
            @Override
            GraphqlCaller createGraphqlCaller(ServiceDefinition serviceDefinition) {
                assert callerMocks[serviceDefinition.name] != null
                return callerMocks[serviceDefinition.name]
            }
        }

    }

    def "simple stitching"() {
        given:
        def dsl = """
        service Service1 {
            url: "url1"
            type Query {
                hello: String
            }
        }
        service Service2 {
            url: "url2"
            type Query {
                hello2: String
            }
        }
        """
        def graphqlCallerService1 = Mock(GraphqlCaller)
        def graphqlCallerService2 = Mock(GraphqlCaller)
        def callerFactory = mockCallerFactory([Service1: graphqlCallerService1, Service2: graphqlCallerService2])

        String query1 = "{hello}"
        String query2 = "{hello2}"
        Nadel nadel = new Nadel(dsl, callerFactory)
        def executionResult

        when:
        executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query1).build()).get()

        then:
        executionResult.data == [hello: 'world']
        1 * graphqlCallerService1.call(_) >> new GraphqlCallResult([hello: 'world'])

        when:
        executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query2).build()).get()

        then:
        executionResult.data == [hello2: 'world']
        1 * graphqlCallerService2.call(_) >> new GraphqlCallResult([hello2: 'world'])
    }

}
