package graphql.nadel


import spock.lang.Specification

class NadelTest extends Specification {


    // convert tests later once ready

//    def twoSimpleServices = """
//        service Service1 {
//            type Query {
//                hello: String
//            }
//        }
//        service Service2 {
//            type Query {
//                hello2: String
//            }
//        }
//        """
//
//    def "builder works as expected with instrumentation"() {
//
//        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
//        def graphqlRemoteRetriever2 = Mock(GraphQLRemoteRetriever)
//        def callerFactory = mockCallerFactory([Service1: graphqlRemoteRetriever1, Service2: graphqlRemoteRetriever2])
//
//        def instrumentationCalled = false
//        def instrumentation = new SimpleInstrumentation() {
//            @Override
//            InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
//                instrumentationCalled = true
//                return super.beginExecution(parameters)
//            }
//        }
//
//        Nadel nadel = Nadel.newNadel().dsl(twoSimpleServices).remoteRetrieverFactory(callerFactory).useInstrumentation(instrumentation).build()
//
//        when:
//        String query1 = "{hello}"
//        nadel.executeAsync(ExecutionInput.newExecutionInput().query(query1).build()).get()
//
//        then:
//        instrumentationCalled
//        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: [hello100: 'world']])
//    }
//
//    def "simple stitching: just two services merged at top level"() {
//        given:
//
//        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
//        def graphqlRemoteRetriever2 = Mock(GraphQLRemoteRetriever)
//        def callerFactory = mockCallerFactory([Service1: graphqlRemoteRetriever1, Service2: graphqlRemoteRetriever2])
//
//        String query1 = "{hello}"
//        String query2 = "{hello2}"
//        Nadel nadel = Nadel.newNadel().dsl(twoSimpleServices).remoteRetrieverFactory(callerFactory).build()
//
//        when:
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query1).build()).get()
//
//        then:
//        executionResult.data == [hello: 'world']
//        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: [hello100: 'world']])
//        0 * graphqlRemoteRetriever2.queryGraphQL(*_) >> completedFuture([data: []])
//
//
//        when:
//        executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query2).build()).get()
//
//
//        then:
//        executionResult.data == [hello2: 'world']
//        1 * graphqlRemoteRetriever2.queryGraphQL(*_) >> completedFuture([data: [hello2100: 'world']])
//        0 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: []])
//    }
//
//    def "stitching with service hydration"() {
//        def dsl = """
//        service FooService {
//            schema {
//               query: Query
//            }
//            type Query {
//               foo: Foo
//            }
//
//            type Foo {
//                id: ID
//                title : String
//                barId: ID
//                bar : Bar <= \$innerQueries.BarService.bar(barId: \$source.barId)
//            }
//        }
//
//        service BarService {
//            schema {
//                    query: Query
//            }
//            type Query {
//                bar(barId: ID, id: ID): Bar
//            }
//
//            type Bar {
//                id: ID
//                name: String
//            }
//        }
//        """
//        def barService = barService([new Bar("b1", "bar1"), new Bar("b2", "bar2")])
//        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
//        GraphQLRemoteRetriever graphqlRemoteRetriever2 = { braidQuery, ctx ->
//            def executionInput = braidQuery.asExecutionInput()
//            return completedFuture([data: (Map<String, Object>) barService.execute(executionInput).getData()])
//        }
//        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetriever1, BarService: graphqlRemoteRetriever2])
//
//        String query = "{foo { id bar { id name }}}"
//        Nadel nadel = Nadel.newNadel().dsl(dsl).remoteRetrieverFactory(callerFactory).build()
//        when:
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
//
//        then:
//        executionResult.data == [foo: [id: 'foo1', bar: [id: 'b2', name: 'bar2']]]
//        then:
//        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
//            completedFuture([data: [foo100: [id: 'foo1', barId: 'b2']]])
//        }
//
//    }
//
//    def "hydration with multiple arguments"() {
//        def dsl = '''
//        service FooService {
//            schema {
//               query: Query
//            }
//            type Query {
//               foo: Foo
//            }
//
//            type Foo {
//                id: ID
//                title : String
//                barId: ID
//                baz(bazId: ID!) : Baz <= $innerQueries.BazService.baz(barId: $source.barId,
//                                                                      id: $argument.bazId,
//                                                                       userId: $context.userId)
//            }
//        }
//
//        service BazService {
//            schema {
//                    query: Query
//            }
//            type Query {
//                baz(id: ID!, userId: ID!, barId: ID): Baz
//            }
//
//            type Baz {
//                id: ID!
//                userId: ID!
//                barId: ID
//            }
//        }
//        '''
//        def bazService = bazService()
//        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
//        GraphQLRemoteRetriever graphqlRemoteRetriever2 = { braidQuery, ctx ->
//            def executionInput = braidQuery.asExecutionInput()
//            return completedFuture([data: (Map<String, Object>) bazService.execute(executionInput).getData()])
//        }
//        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetriever1, BazService: graphqlRemoteRetriever2])
//
//        String query = '{foo { id baz(bazId: "baz1") { id userId barId }}}'
//
//        Nadel nadel = Nadel.newNadel()
//                .dsl(dsl)
//                .remoteRetrieverFactory(callerFactory)
//                .argumentValueProvider(new ArgumentProviderWithContext(["userId": "brad"]))
//                .build()
//        when:
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
//
//        then:
//        executionResult.data == [foo: [id: 'foo1', baz: [id: 'baz1', userId: 'brad', barId: 'someBar']]]
//        then:
//        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> { it ->
//            completedFuture([data: [foo100: [id: 'foo1', barId: 'someBar']]])
//        }
//
//    }
//
//    @Unroll
//    def "stitching with #fragment field rename"(String fragment, String query) {
//        def dsl = """
//            service FooService {
//                schema {
//                    query: Query
//                }
//
//                type Query {
//                    foo: [Foo!]
//                }
//
//                type Foo {
//                    newName : ID <= \$source.id
//                    barId: ID
//                    newTitle : String <=\$source.title
//                    name: String
//                }
//            }
//        """
//
//        def fooService = fooService([new Foo("foo1", "name1", "title1", "someBarId1"),
//                                     new Foo("foo2", "name2", "title2", "someBarId2")])
//        GraphQLRemoteRetriever graphqlRemoteRetrieverFoo = { braidQuery, ctx ->
//            return completedFuture([data: (Map<String, Object>) fooService.execute(braidQuery.asExecutionInput()).getData()])
//        }
//        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetrieverFoo])
//
//        Nadel nadel = Nadel.newNadel().dsl(dsl).remoteRetrieverFactory(callerFactory).build()
//        when:
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
//
//        then:
//        executionResult.data == [foo: [[newName: 'foo1', barId: 'someBarId1', newTitle: 'title1'],
//                                       [newName: 'foo2', barId: 'someBarId2', newTitle: 'title2']]]
//
//        where:
//        fragment          | query                                                            | _
//        "simple"          | "{foo { newName newTitle barId }}"                               | _
//        "inline fragment" | "{foo {... on Foo { newName  barId newTitle} }} "                | _
//        "named fragment"  | "fragment cf on Foo { newName  barId newTitle} {foo { ... cf}} " | _
//    }
//
//    @Unroll
//    def "stitching with #fragment type rename"(String fragment, String query) {
//        def dsl = '''
//            service FooService {
//                schema {
//                    query: Query
//                }
//                type Query {
//                    foo: [Foo2!]
//                }
//
//                type Foo2 <= $innerTypes.Foo {
//                    newName: ID <= $source.id
//                    barId: ID
//                    newTitle : String <= $source.title
//                    name: String
//                }
//            }
//        '''
//
//        def fooService = fooService([new Foo("foo1", "name1", "title1", "someBarId1"),
//                                     new Foo("foo2", "name2", "title2", "someBarId2")])
//        GraphQLRemoteRetriever graphqlRemoteRetrieverFoo = { braidQuery, ctx ->
//            return completedFuture([data: (Map<String, Object>) fooService.execute(braidQuery.asExecutionInput()).getData()])
//        }
//        def callerFactory = mockCallerFactory([FooService: graphqlRemoteRetrieverFoo])
//
//        Nadel nadel = Nadel.newNadel().dsl(dsl).remoteRetrieverFactory(callerFactory).build()
//        when:
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
//
//        then:
//        executionResult.data == [foo: [[newName: 'foo1', barId: 'someBarId1', newTitle: 'title1'],
//                                       [newName: 'foo2', barId: 'someBarId2', newTitle: 'title2']]]
//
//        where:
//        fragment          | query                                                             | _
//        "simple"          | "{foo { newName newTitle barId }}"                                | _
//        "inline fragment" | "{foo {... on Foo2 { newName  barId newTitle} } } "               | _
//        "named fragment"  | "fragment cf on Foo2 { newName  barId newTitle} {foo { ... cf}} " | _
//    }
//
//    def "additional runtime wiring provided programmatically"() {
//        given:
//        def dsl = """
//        service Service1 {
//            type Query {
//                hello: String
//                additionalField: String
//            }
//        }
//        """
//        def graphqlRemoteRetriever1 = Mock(GraphQLRemoteRetriever)
//        def callerFactory = mockCallerFactory([Service1: graphqlRemoteRetriever1])
//
//        def fieldWiring = newTypeWiring("Query")
//                .dataFetcher("additionalField", new StaticDataFetcher("myValue"))
//                .build()
//
//
//        SchemaTransformation transformation = { ctx ->
//            ctx.runtimeWiringBuilder.type(fieldWiring)
//            return [:]
//        }
//
//        Nadel nadel = Nadel.newNadel().dsl(dsl).schemaSourceFactory(new GraphQLRemoteSchemaSourceFactory<>(callerFactory)).transformationsFactory({ it ->
//            [transformation]
//        }).build()
//
//        when:
//        def query = " { hello additionalField }"
//        def executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
//
//        then:
//        executionResult.data == [hello: 'world', additionalField: 'myValue']
//        1 * graphqlRemoteRetriever1.queryGraphQL(*_) >> completedFuture([data: [hello100: 'world']])
//    }
//
//    class ArgumentProviderWithContext extends DefaultArgumentValueProvider {
//        Map<String, Object> context
//
//        ArgumentProviderWithContext(Map<String, Object> context) {
//            this.context = context
//        }
//
//        @Override
//        CompletableFuture<Object> fetchValueForArgument(LinkArgument linkArgument, DataFetchingEnvironment environment) {
//            if (linkArgument.argumentSource == LinkArgument.ArgumentSource.CONTEXT) {
//                return completedFuture(context.get(linkArgument.sourceName))
//            }
//            return super.fetchValueForArgument(linkArgument, environment)
//        }
//    }
//    /**
//     * Creates bar service that returns values from provided bars
//     */
//    GraphQL barService(List<Bar> bars) {
//        def schema = """
//        type Query {
//            bar(barId: ID, otherArg: String): Bar
//        }
//
//        type Bar {
//            id: ID
//            name: String
//        }
//        """
//
//        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schema)
//        DataFetcher<Bar> fetcher = {
//            def barId = it.arguments["barId"]
//            if (barId == null) {
//                throw new IllegalArgumentException("BarId is required")
//            }
//            return bars.find { it.id == barId }
//        }
//
//        RuntimeWiring runtimeWiring = newRuntimeWiring()
//                .type("Query", { it.dataFetcher("bar", fetcher) })
//                .build()
//
//        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
//
//        return GraphQL.newGraphQL(graphQLSchema).build()
//    }
//
//    /**
//     * Creates baz service with query that  returns Baz object with provided arguments
//     */
//    GraphQL bazService() {
//        def schema = '''
//        type Query {
//            baz(id: ID!, userId: ID!, barId: ID,): Baz
//        }
//
//        type Baz {
//            id: ID!
//            userId: ID!
//            barId: ID
//        }
//        '''
//
//        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schema)
//        DataFetcher<Baz> fetcher = {
//            return new Baz(id: it.arguments["id"], barId: it.arguments["barId"], userId: it.arguments["userId"])
//        }
//
//        RuntimeWiring runtimeWiring = newRuntimeWiring()
//                .type("Query", { it.dataFetcher("baz", fetcher) })
//                .build()
//
//        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
//
//        return GraphQL.newGraphQL(graphQLSchema).build()
//    }
//
//    /**
//     * Creates foo service that returns values from provided foos
//     */
//    GraphQL fooService(List<Foo> foos) {
//        def schema = """
//        type Query {
//            foo:[Foo!]
//        }
//
//        type Foo {
//              id: ID
//              barId: ID
//              title : String
//              name: String
//        }
//        """
//
//        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schema)
//        DataFetcher<Bar> fetcher = {
//            return foos
//        }
//
//        RuntimeWiring runtimeWiring = newRuntimeWiring()
//                .type("Query", { it.dataFetcher("foo", fetcher) })
//                .build()
//
//        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
//
//        return GraphQL.newGraphQL(graphQLSchema).build()
//    }
//
//    static class Baz {
//        String id
//        String barId
//        String userId
//    }
//
//    static class Bar {
//        private String id
//        private String name
//
//        Bar(String id, String name) {
//            this.id = id
//            this.name = name
//        }
//
//        String getId() {
//            return id
//        }
//
//        String getName() {
//            return name
//        }
//    }
//
//    static class Foo {
//        private String id
//        private String name
//        private String title
//        private String barId
//
//        Foo(String id, String name, String title, barId) {
//            this.id = id
//            this.name = name
//            this.title = title
//            this.barId = barId
//        }
//
//        String getId() {
//            return id
//        }
//
//        String getName() {
//            return name
//        }
//
//        String getTitle() {
//            return title
//        }
//
//        String getBarId() {
//            return barId
//        }
//    }
}
