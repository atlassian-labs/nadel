package graphql.nadel

import com.atlassian.braid.source.GraphQLRemoteRetriever
import graphql.ExecutionInput
import graphql.nadel.dsl.ServiceDefinition
import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

import static java.util.concurrent.CompletableFuture.completedFuture

class NadelTest extends Specification {


    GraphQLRemoteRetrieverFactory mockCallerFactory(Map callerMocks) {
        return new GraphQLRemoteRetrieverFactory() {
            @Override
            GraphQLRemoteRetriever createRemoteRetriever(ServiceDefinition serviceDefinition) {
                assert callerMocks[serviceDefinition.name] != null
                return callerMocks[serviceDefinition.name]
            }
        }
    }

    def "simple stitching: just two services merged at top level"() {
        given:
        def dsl = """
        service Service1 {
            serviceUrl: "url1"
            type Query {
                hello: String
            }
        }
        service Service2 {
            serviceUrl: "url2"
            type Query {
                hello2: String
            }
        }
        """
        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
        def graphqlRemoteRetriever2 = Mock(GraphQLRemoteRetriever)
        def callerFactory = mockCallerFactory([Service1: graphqlRemoteRetriever1, Service2: graphqlRemoteRetriever2])

        String query1 = "{hello}"
        String query2 = "{hello2}"
        Nadel nadel = new Nadel(dsl, callerFactory)

        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query1).build()).get()

        then:
        executionResult.data == [hello: 'world']
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: [hello100: 'world']])
        0 * graphqlRemoteRetriever2.queryGraphQL(*_) >> completedFuture([data: []])


        when:
        executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query2).build()).get()


        then:
        executionResult.data == [hello2: 'world']
        1 * graphqlRemoteRetriever2.queryGraphQL(*_) >> completedFuture([data: [hello2100: 'world']])
        0 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: []])
    }


    def "stitching with transformation"() {
        def dsl = """
        service FooService {
            serviceUrl: "url1"
            type Query {
                foo: Foo
            }
            type Foo {
                barId: ID => from bar(id) with input barId as realBarValue
            }
        }
        service BarService {
            serviceUrl: "url2"
            type Query {
                bar(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        """

        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
        def graphqlRemoteRetriever2 = Mock(GraphQLRemoteRetriever)
        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetriever1, BarService: graphqlRemoteRetriever2])

        String query = "{foo{realBarValue{name}}}"
        Nadel nadel = new Nadel(dsl, callerFactory)


        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()


        then:
        executionResult.data == [foo: [realBarValue: [name: 'barName']]]
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
            completedFuture([data: [foo100: [barId: 'someBarId']]])
        }
        1 * graphqlRemoteRetriever2.queryGraphQL(*_) >> { it ->
            completedFuture([data: [realBarValue100: [id: 'someBarId', name: 'barName']]])
        }
    }

}
