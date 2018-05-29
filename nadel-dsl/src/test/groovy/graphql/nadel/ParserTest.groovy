package graphql.nadel

import graphql.language.ObjectTypeDefinition
import graphql.nadel.dsl.FieldTransformation
import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

class ParserTest extends Specification {


    def "simple service definition"() {
        given:
        def simpleDSL = """
        service Foo {
            serviceUrl: "someUrl"
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
            serviceUrl: "url1"
            type Query {
                hello1: String
            }
        }
        service Bar {
            serviceUrl: "url2"
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

    def "parse transformation"() {
        given:
        def dsl = """
        service FooService {
            serviceUrl: "url1"
            type Query {
                foo: Foo
            }
            type Foo {
                barId: ID => from bar(id) with input barId as bar
            }
        }
        service BarService {
            serviceUrl: "url2"
            type Query {
                bar(id: ID): Bar
            }
            type Bar {
                id: ID
            }
        }
        """
        when:
        Parser parser = new Parser()
        then:
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions().size() == 2

        ObjectTypeDefinition fooType = stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[1]
        fooType.name == "Foo"
        def fieldDefinition = fooType.fieldDefinitions[0]
        FieldTransformation fieldTransformation = stitchingDSL.getTransformationsByFieldDefinition().get(fieldDefinition)
        fieldTransformation != null
        fieldTransformation.targetName == "bar"
    }

    def "empty arrow fails"() {
        def dsl = """
        service FooService {
            serviceUrl: "url1"

            type Foo {
                barId: ID => 
            }
        }
        """

        when:
        new Parser().parseDSL(dsl)

        then:
        ParseCancellationException ex = thrown()
        // Alternative syntax: def ex = thrown(InvalidDeviceException)
        ex.message.contains("expecting 'from'")

    }
}


