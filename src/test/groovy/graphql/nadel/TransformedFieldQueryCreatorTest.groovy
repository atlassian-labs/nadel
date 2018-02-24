package graphql.nadel

import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.nadel.dsl.StitchingDsl
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import spock.lang.Specification

class TransformedFieldQueryCreatorTest extends Specification {

    def "create query for target field"() {
        given:
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
        def (GraphQLSchema schema, StitchingDsl stitchingDsl) = createSchemaAndDsl(dsl)
        def fieldDefinition = (schema.getType("Foo") as GraphQLObjectType).fieldDefinitions[0].getDefinition()
        def transformedField = stitchingDsl.getTransformationsByFieldDefinition().get(fieldDefinition)

        TransformedFieldQueryCreator transformedFieldQueryCreator = new TransformedFieldQueryCreator(fieldDefinition, transformedField)

        def overallQuery = """ {foo{bar{name}}} """
        def (DataFetchingEnvironment dataFetchingEnvrionment) = mockDFEnvironment(overallQuery, schema)
        dataFetchingEnvrionment.getSource() >> [barId: "someBarId"]
        when:
        def createdQuery = transformedFieldQueryCreator.createQuery(dataFetchingEnvrionment)

        then:
        TestUtil.printAstCompact(createdQuery) == """query { bar(id: "someBarId") { id name } }"""

    }

    def mockDFEnvironment(String query, GraphQLSchema graphQLSchema) {
        def environment = Mock(DataFetchingEnvironment)
        def graphqlParser = new graphql.parser.Parser()
        def queryDocument = graphqlParser.parseDocument(query)

        Field fooField = (queryDocument.getDefinitions()[0] as OperationDefinition).getSelectionSet().getSelections()[0] as Field
        assert fooField.name == "foo"
        def barField = fooField.getSelectionSet().getSelections()[0] as Field
        assert barField.name == "bar"

        environment.getField() >> barField
        environment.getGraphQLSchema() >> graphQLSchema
        return [environment, barField]
    }

    def createSchemaAndDsl(String dsl) {
        Parser parser = new Parser()
        StitchingDsl stitchingDsl = parser.parseDSL(dsl)
        NadelTypeDefinitionRegistry typeRegistry = new NadelTypeDefinitionRegistry(stitchingDsl)
        SchemaGenerator schemaGenerator = new SchemaGenerator()
        def dummyCallerFactory = Mock(GraphqlCallerFactory)
        dummyCallerFactory.createGraphqlCaller(_) >> Mock(GraphqlCaller)
        return [schemaGenerator.makeExecutableSchema(typeRegistry, dummyCallerFactory), stitchingDsl]
    }
}
