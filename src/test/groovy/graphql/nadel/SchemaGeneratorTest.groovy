package graphql.nadel

import graphql.nadel.dsl.StitchingDsl
import spock.lang.Specification

class SchemaGeneratorTest extends Specification {


    NadelTypeDefinitionRegistry createTypeRegistry(String dsl) {
        Parser parser = new Parser()
        StitchingDsl stitchingDsl = parser.parseDSL(dsl)
        NadelTypeDefinitionRegistry typeDefinitionRegistry = new NadelTypeDefinitionRegistry(stitchingDsl)
        return typeDefinitionRegistry
    }

    def "test schema generation"() {
        given:
        def typeRegistry = createTypeRegistry("""
        service X {
            url: "url"
            type Query {
                hello: String
                }
            }
        }
        """)
        SchemaGenerator schemaGenerator = new SchemaGenerator()


        when:
        def schema = schemaGenerator.makeExecutableSchema(typeRegistry)
        then:
        schema.getQueryType().name == "Query"
        schema.getQueryType().fieldDefinitions[0].name == "hello"

    }
}
