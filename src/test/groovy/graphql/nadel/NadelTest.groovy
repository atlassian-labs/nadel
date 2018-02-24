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

        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query1).build()).get()

        then:
        executionResult.data == [hello: 'world']
        1 * graphqlCallerService1.call(_) >> new GraphqlCallResult([hello: 'world'])

        when:
        executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query2).build()).get()

        then:
        executionResult.data == [hello2: 'world']
        1 * graphqlCallerService2.call(_) >> new GraphqlCallResult([hello2: 'world'])
    }


    def "stitching with transformation"() {
        def dsl = """
        service FooService {
            url: "url1"
            type Query {
                foo: Foo
            }
            type Foo {
                barId: ID => bar: Bar
            }
        }
        service BarService {
            url: "url2"
            type Query {
                bar(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        """
        def graphqlCallerService1 = Mock(GraphqlCaller)
        def graphqlCallerService2 = Mock(GraphqlCaller)
        def callerFactory = mockCallerFactory([FooService: graphqlCallerService1, BarService: graphqlCallerService2])

        String query = "{foo{bar{name}}}"
        Nadel nadel = new Nadel(dsl, callerFactory)
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [bar: [name: 'barName']]]
        1 * graphqlCallerService1.call(_) >> new GraphqlCallResult([foo: [barId: 'someBarId']])
        1 * graphqlCallerService1.call(_) >> new GraphqlCallResult([bar: [id: 'someBarId', name: 'barName']])
    }

}
