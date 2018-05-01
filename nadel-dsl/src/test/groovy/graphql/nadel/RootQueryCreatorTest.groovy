//package graphql.nadel
//
//import graphql.language.AstPrinter
//import graphql.language.Field
//import graphql.language.OperationDefinition
//import graphql.nadel.dsl.ServiceDefinition
//import graphql.nadel.dsl.StitchingDsl
//import graphql.schema.DataFetchingEnvironment
//import graphql.schema.GraphQLSchema
//import spock.lang.Specification
//
//class RootQueryCreatorTest extends Specification {
//
//
//    def "simple query"() {
//        given:
//        def dsl = """
//        service FooService {
//            url: "url1"
//            type Query {
//                hello: String
//            }
//        }
//        """
//        def (GraphQLSchema schema, StitchingDsl stitchingDsl) = createSchema(dsl)
//        ServiceDefinition fooService = stitchingDsl.getServiceDefinition("FooService")
//        def query = "{hello}"
//        def (DataFetchingEnvironment environment, Field field) = mockDFEnvironment(query, schema)
//        def queryCreator = new RootQueryCreator(fooService, stitchingDsl)
//        given:
//        when:
//        def document = queryCreator.createQuery(environment)
//
//        then:
//        TestUtil.printAstCompact(document) == """query { hello }"""
//    }
//
//
//    def "create query respecting the field transformation"() {
//        given:
//        def dsl = """
//        service FooService {
//            url: "url1"
//            type Query {
//                foo: Foo
//            }
//            type Foo {
//                barId: ID => bar: Bar
//            }
//        }
//        service BarService {
//            url: "url2"
//            type Query {
//                bar(id: ID): Bar
//            }
//            type Bar {
//                id: ID
//            }
//        }
//        """
//        def (GraphQLSchema schema, StitchingDsl stitchingDsl) = createSchema(dsl)
//        ServiceDefinition fooService = stitchingDsl.getServiceDefinition("FooService")
//        def query = "{foo{bar{id}}}"
//        def (DataFetchingEnvironment environment, Field field) = mockDFEnvironment(query, schema)
//        def queryCreator = new RootQueryCreator(fooService, stitchingDsl)
//
//        when:
//        def document = queryCreator.createQuery(environment)
//
//        then:
//        TestUtil.printAstCompact(document) == """query { foo { barId } }"""
//
//    }
//
//
//    def "more complex query with multiple transformations"() {
//        given:
//        def dsl = """
//        service FooService {
//            url: "url1"
//            type Query {
//                foo: Foo
//            }
//            type Foo {
//                subFoo: SubFoo
//                someValue: String
//            }
//            type SubFoo{
//                barId: ID => bar: Bar
//                subSub: SubSubFoo
//            }
//            type SubSubFoo {
//                blaId: ID => bla: Bla
//            }
//
//        }
//        service BarService {
//            url: "url2"
//            type Query {
//                bar(id: ID): Bar
//            }
//            type Bar {
//                id: ID
//            }
//        }
//        service BlaService {
//            url: "url3"
//            type Query {
//                bla(id: ID): Bar
//            }
//            type Bla {
//               id: ID
//            }
//        }
//        """
//        def (GraphQLSchema schema, StitchingDsl stitchingDsl) = createSchema(dsl)
//        ServiceDefinition fooService = stitchingDsl.getServiceDefinition("FooService")
//        def query = """ {
//            foo {
//                someValue
//                subFoo{
//                    bar {
//                        id
//                    }
//                    subSub {
//                        bla {
//                            id
//                        }
//                    }
//                }
//            }
//        }
//
//        """
//        def (DataFetchingEnvironment environment, Field field) = mockDFEnvironment(query, schema)
//        def queryCreator = new RootQueryCreator(fooService, stitchingDsl)
//
//        when:
//        def document = queryCreator.createQuery(environment)
//
//        then:
//        AstPrinter.printAst(document) ==
//                """query {
//  foo {
//    someValue
//    subFoo {
//      barId
//      subSub {
//        blaId
//      }
//    }
//  }
//}
//"""
//
//    }
//
//    def createSchema(String dsl) {
//        Parser parser = new Parser()
//        StitchingDsl stitchingDsl = parser.parseDSL(dsl)
//        NadelTypeDefinitionRegistry typeRegistry = new NadelTypeDefinitionRegistry(stitchingDsl)
//        SchemaGenerator schemaGenerator = new SchemaGenerator()
//        def dummyCallerFactory = Mock(GraphqlCallerFactory)
//        dummyCallerFactory.createGraphqlCaller(_) >> Mock(GraphqlCaller)
//        return [schemaGenerator.makeExecutableSchema(typeRegistry, dummyCallerFactory), stitchingDsl]
//    }
//
//    def mockDFEnvironment(String query, GraphQLSchema graphQLSchema) {
//        def environment = Mock(DataFetchingEnvironment)
//        def graphqlParser = new graphql.parser.Parser()
//        def queryDocument = graphqlParser.parseDocument(query)
//        Field field = (queryDocument.getDefinitions()[0] as OperationDefinition).getSelectionSet().getSelections()[0] as Field
//        environment.getFields() >> [field]
//        environment.getGraphQLSchema() >> graphQLSchema
//        return [environment, field]
//    }
//}
