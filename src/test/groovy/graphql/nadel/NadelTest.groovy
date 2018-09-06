package graphql.nadel

import com.atlassian.braid.source.GraphQLRemoteRetriever
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.nadel.dsl.ServiceDefinition
import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import spock.lang.Specification

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring
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
                bar : String <= \$innerQueries.BarService.bar(barId: \$source.barId)
            }
        }
        
        service BarService {
            type Query {
                bar(barId: ID, id: ID): Bar
            }

            type Bar {
                id: ID
                name: String
            }
        }
        """
        def barService = barService([new Bar("b1", "bar1"), new Bar("b2", "bar2")])
        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
        GraphQLRemoteRetriever graphqlRemoteRetriever2 = { input, ctx ->
            return completedFuture([data: (Map<String, Object>) barService.execute(input).getData()])
        }
        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetriever1, BarService: graphqlRemoteRetriever2])

        String query = "{foo { id bar { id name }}}"
        Nadel nadel = Nadel.newBuilder()
                .withDsl(dsl)
                .withGraphQLRemoteRetrieverFactory(callerFactory)
                .build()
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [id: 'foo1', bar: [id: 'b2', name: 'bar2']]]
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
            completedFuture([data: [foo100: [id: 'foo1', barId: 'b2']]])
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
        """

        Map<String, DataFetcher<Object>> fetchers = [
                "exampleFetcher": (DataFetcher) { env ->
                    return ["id": env.getArgument("id")]
                },
                "specialField"  : (DataFetcher) { env ->
                    return "special value from fetcher"
                }
        ]



        Nadel nadel = Nadel.newBuilder()
                .withDsl(dsl)
                .withDataFetcherFactory(DataFetcherFactory.fromMap(fetchers))
                .build()

        when:
        String query = """
        {
          example(id: "id2") { 
                id
                specialField 
          }
        }
        """

        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [
                'example': ['id': 'id2', 'specialField': 'special value from fetcher']
        ]
    }


    static class Bar {
        private String id
        private String name

        Bar(String id, String name) {
            this.id = id
            this.name = name
        }

        String getId() {
            return id
        }

        String getName() {
            return name
        }
    }


    /**
     * Creates bar service that returns values from provided bars
     */
    GraphQL barService(List<Bar> bars) {
        def schema = """
        type Query {
            bar(barId: ID, otherArg: String): Bar
        }

        type Bar {
            id: ID
            name: String
        }
        """

        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schema)
        DataFetcher<Bar> fetcher = {
            def barId = it.arguments["barId"]
            if (barId == null) {
                throw new IllegalArgumentException("BarId is required")
            }
            return bars.find { it.id == barId }
        }

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", { it.dataFetcher("bar", fetcher) })
                .build()

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)

        return GraphQL.newGraphQL(graphQLSchema).build()
    }
}
