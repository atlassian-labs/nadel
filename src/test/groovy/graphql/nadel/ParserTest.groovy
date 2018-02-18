package graphql.nadel

import graphql.language.ObjectTypeDefinition
import spock.lang.Specification

class ParserTest extends Specification {


    def "simple service definition"() {
        given:
        def simpleDSL = """
        service Foo {
            url: "someUrl"
            type Query {
                hello: String
            }
        }
       """
        def stitchingDSL
        when:
        Parser parser = new Parser()
        stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        stitchingDSL.getServiceDefinitions().size() == 1
        stitchingDSL.getServiceDefinitions()[0].url == 'someUrl'
        stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions().size() == 1
        stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0] instanceof ObjectTypeDefinition
        ((ObjectTypeDefinition) stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0]).name == 'Query'
        ((ObjectTypeDefinition) stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0]).fieldDefinitions[0].name == 'hello'

    }

    def "parse error"() {
        given:
        def simpleDSL = """
        service Foo {
            urlX: "someUrl"
        }
       """
        when:
        Parser parser = new Parser()
        parser.parseDSL(simpleDSL)

        then:
        thrown(Exception)

    }
}
