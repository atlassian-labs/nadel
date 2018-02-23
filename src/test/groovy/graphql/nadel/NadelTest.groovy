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
        graphqlCallerService2.call(_) >> new GraphqlCallResult([hello2: 'world'])
        def callerFactory = mockCallerFactory([Service1: graphqlCallerService1, Service2: graphqlCallerService2])

        String query = "{hello}"
        Nadel nadel = new Nadel(dsl, callerFactory)
        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).build()
        when:
        def executionResult = nadel.executeAsync(executionInput).get()

        then:
        executionResult.data == [hello: 'world']
        1 * graphqlCallerService1.call(_) >> new GraphqlCallResult([hello: 'world'])
    }

}
