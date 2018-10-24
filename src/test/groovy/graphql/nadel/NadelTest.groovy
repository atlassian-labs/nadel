package graphql.nadel

import com.atlassian.braid.source.GraphQLRemoteRetriever
import com.atlassian.braid.transformation.SchemaTransformation
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.nadel.dsl.ServiceDefinition
import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import spock.lang.Specification
import spock.lang.Unroll

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring
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

    def "stitching with service hydration"() {
        def dsl = """
        service FooService {
            schema {
               query: Query
            }
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
            schema {
                    query: Query
            }
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
        Nadel nadel = new Nadel(dsl, callerFactory)
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [id: 'foo1', bar: [id: 'b2', name: 'bar2']]]
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
            completedFuture([data: [foo100: [id: 'foo1', barId: 'b2']]])
        }

    }

    @Unroll
    def "stitching with #fragment field rename"(String fragment, String query) {
        def dsl = """
            service FooService {
                schema {
                    query: Query
                }
                
                type Query {
                    foo: [Foo!]
                }
    
                type Foo {
                    newName: ID <= \$source.id
                    barId: ID
                    newTitle : String <=\$source.title
                    name: String 
                }
            }
        """

        def fooService = fooService([new Foo("foo1", "name1", "title1", "someBarId1"),
                                     new Foo("foo2", "name2", "title2", "someBarId2")])
        GraphQLRemoteRetriever graphqlRemoteRetrieverFoo = { input, ctx ->
            return completedFuture([data: (Map<String, Object>) fooService.execute(input).getData()])
        }
        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetrieverFoo])

        Nadel nadel = new Nadel(dsl, callerFactory)
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [[newName: 'foo1', barId: 'someBarId1', newTitle: 'title1'],
                                       [newName: 'foo2', barId: 'someBarId2', newTitle: 'title2']]]

        where:
        fragment          | query                                                            | _
        "simple"          | "{foo { newName newTitle barId }}"                               | _
        "inline fragment" | "{foo {... on Foo { newName  barId newTitle} }} "                | _
        "named fragment"  | "fragment cf on Foo { newName  barId newTitle} {foo { ... cf}} " | _
    }

    @Unroll
    def "stitching with #fragment type rename"(String fragment, String query) {
        def dsl = """
            service FooService {
                schema {
                    query: Query
                }
                type Query {
                    foo: [Foo2!]
                }
    
                type Foo2 <= \$innerTypes.Foo {
                    newName: ID <= \$source.id
                    barId: ID
                    newTitle : String <=\$source.title
                    name: String   
                }
            }
        """

        def fooService = fooService([new Foo("foo1", "name1", "title1", "someBarId1"),
                                     new Foo("foo2", "name2", "title2", "someBarId2")])
        GraphQLRemoteRetriever graphqlRemoteRetrieverFoo = { input, ctx ->
            return completedFuture([data: (Map<String, Object>) fooService.execute(input).getData()])
        }
        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetrieverFoo])

        Nadel nadel = new Nadel(dsl, callerFactory)
        when:
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [foo: [[newName: 'foo1', barId: 'someBarId1', newTitle: 'title1'],
                                       [newName: 'foo2', barId: 'someBarId2', newTitle: 'title2']]]

        where:
        fragment          | query                                                             | _
        "simple"          | "{foo { newName newTitle barId }}"                                | _
//      "inline fragment" | " {f1: foo {... on Foo2 { newName  barId newTitle} } } "         | _
        "named fragment"  | "fragment cf on Foo2 { newName  barId newTitle} {foo { ... cf}} " | _
    }

    def "additional runtime wiring provided programmatically"() {
        given:
        def dsl = """
        service Service1 {
            type Query {
                hello: String
                additionalField: String
            }
        }
        """
        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
        def callerFactory = mockCallerFactory([Service1: graphqlRemoteRetriever1])

        def fieldWiring = newTypeWiring("Query")
                .dataFetcher("additionalField", new StaticDataFetcher("myValue"))
                .build()


        SchemaTransformation transformation = { ctx ->
            ctx.runtimeWiringBuilder.type(fieldWiring)
            return [:]
        }
        Nadel nadel = new Nadel(dsl, new GraphQLRemoteSchemaSourceFactory<>(callerFactory), { it ->
            [transformation]
        })

        when:
        def query = " { hello additionalField }"
        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()

        then:
        executionResult.data == [hello: 'world', additionalField: 'myValue']
        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: [hello100: 'world']])
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

    /**
     * Creates foo service that returns values from provided foos
     */
    GraphQL fooService(List<Foo> foos) {
        def schema = """
        type Query {
            foo:[Foo!]
        }

        type Foo {
              id: ID
              barId: ID
              title : String
              name: String 
        }
        """

        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schema)
        DataFetcher<Bar> fetcher = {
            return foos
        }

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", { it.dataFetcher("foo", fetcher) })
                .build()

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)

        return GraphQL.newGraphQL(graphQLSchema).build()
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

    static class Foo {
        private String id
        private String name
        private String title
        private String barId

        Foo(String id, String name, String title, barId) {
            this.id = id
            this.name = name
            this.title = title
            this.barId = barId
        }

        String getId() {
            return id
        }

        String getName() {
            return name
        }

        String getTitle() {
            return title
        }

        String getBarId() {
            return barId
        }
    }
}
