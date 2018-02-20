package graphql.nadel

import graphql.nadel.dsl.StitchingDsl
import graphql.schema.GraphQLSchema
import spock.lang.Specification

class SchemaGeneratorTest extends Specification {


    NadelTypeDefinitionRegistry createTypeRegistry(String dsl) {
        Parser parser = new Parser()
        StitchingDsl stitchingDsl = parser.parseDSL(dsl)
        NadelTypeDefinitionRegistry typeDefinitionRegistry = new NadelTypeDefinitionRegistry(stitchingDsl)
        return typeDefinitionRegistry
    }

    GraphQLSchema createSchema(String dsl) {
        def typeRegistry = createTypeRegistry(dsl)
        SchemaGenerator schemaGenerator = new SchemaGenerator()
        schemaGenerator.makeExecutableSchema(typeRegistry)
    }

    def "simple service"() {
        given:
        def dsl = """
        service X {
            url: "url"
            type Query {
                hello: String
                }
            }
        }
        """
        when:
        def schema = createSchema(dsl)
        then:
        schema.getQueryType().name == "Query"
        schema.getQueryType().fieldDefinitions[0].name == "hello"

    }

    def "two services"() {
        given:
        def dsl = """
        service Foo {
            url: "url1"
            type Query {
                    hello1: String
            }
        }
        service Bar {
            url: "url2"
            type Query {
                hello2: String
            }
        }
        """
        when:
        def schema = createSchema(dsl)
        then:
        schema.getQueryType().name == "Query"
        schema.getQueryType().fieldDefinitions.size() == 2
        schema.getQueryType().fieldDefinitions[0].name == "hello1"
        schema.getQueryType().fieldDefinitions[1].name == "hello2"
    }
}
