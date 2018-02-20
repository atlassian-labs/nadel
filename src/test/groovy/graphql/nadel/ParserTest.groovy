package graphql.nadel

import graphql.language.ObjectTypeDefinition
import org.antlr.v4.runtime.misc.ParseCancellationException
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

    def "two services"() {
        given:
        def simpleDSL = """
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
        def stitchingDSL
        when:
        Parser parser = new Parser()
        stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        stitchingDSL.getServiceDefinitions().size() == 2

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

    def "not all tokens are parsed"() {
        given:
        def simpleDSL = """
        service Foo {
            url: "someUrl"
        }
        someFoo
       """
        when:
        Parser parser = new Parser()
        parser.parseDSL(simpleDSL)

        then:
        thrown(ParseCancellationException)


    }
}


