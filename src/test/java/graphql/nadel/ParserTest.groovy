package graphql.nadel

import spock.lang.Specification

class ParserTest extends Specification {


    def "parse test"() {
        given:
        def simpleDSL = """
        service Foo {
            url: 'someUrl'
            type Query {
                hello: String
            }
        }
       """
        when:
        Parser parser = new Parser()
        parser.parseDSL(simpleDSL)

        then:
        noExceptionThrown()

    }
}
