package graphql.nadel

import com.atlassian.braid.source.GraphQLRemoteRetriever
import graphql.ExecutionInput
import graphql.nadel.dsl.ServiceDefinition
import graphql.schema.DataFetcher
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
            type Query {
                hello: String
            }
        }
        service Service2 {
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
        Nadel nadel = Nadel.newBuilder()
                .withDsl(dsl)
                .withGraphQLRemoteRetrieverFactory(callerFactory)
                .build()

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

    def "stitching with service hydration"() {
        def dsl = """
        service FooService {
            type Query {
                foo: Foo
            }

            type Foo {
                id: ID 
                title : String 
                barId: ID
                bar : String <= \$innerQueries.BarService.bar(id: \$source.barId)
            }
        }
        
        service BarService {
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

        String query = "{foo { id bar { id name }}}"
        Nadel nadel = Nadel.newBuilder()
                .withDsl(dsl)
                .withGraphQLRemoteRetrieverFactory(callerFactory)
                .build()
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [id: 'foo1', bar: [id: 'someBarId', name: 'barName']]]
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
            completedFuture([data: [foo100: [id: 'foo1', barId: 'someBarId']]])
        }
        1 * graphqlRemoteRetriever2.queryGraphQL(*_) >> { it ->
            completedFuture([data: [bar100: [id: 'someBarId', name: 'barName']]])
        }
    }


    def "register special DataFetcher"() {
        def dsl = """
        service base {
            type Query {
                example(id: ID!): Example <= \$dataFetcher.exampleFetcher
            }
            
            type Example {
                id: ID!
                specialField: String <= \$dataFetcher.specialField
            }
        }
        
        service Foo {
            type Query {
                foo: Foo
            }

            type Foo {
                id: ID 
                value: Int <= \$dataFetcher.timesTwo
            }
        }
        """

        Map<String, DataFetcher<Object>> fetchers = [
                "exampleFetcher": (DataFetcher) { env ->
                    return ["id": env.getArgument("id")]
                },
                "specialField"  : (DataFetcher) { env ->
                    return "special value from fetcher"
                },
                "timesTwo"  : (DataFetcher) { env ->
                    return "special value from fetcher"
                }
        ]

        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
        def callerFactory = mockCallerFactory([Foo: graphqlRemoteRetriever1])

        Nadel nadel = Nadel.newBuilder()
                .withDsl(dsl)
                .withGraphQLRemoteRetrieverFactory(callerFactory)
                .withDataFetcherFactory(DataFetcherFactory.fromMap(fetchers))
                .build()

        when:
        String query = """
        {
          example(id: "id2") { 
                id
                specialField 
          }
          
          foo {
               id
               value
          }
        }
        """

        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [
                'example': ['id': 'id2', 'specialField': 'special value from fetcher']
        ]
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
            completedFuture([data: [foo100: [id: 'foo1', 'value': 5]]])
        }
    }
}
