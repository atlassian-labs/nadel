package graphql.nadel

import graphql.language.ObjectTypeDefinition
import graphql.nadel.dsl.FieldDefinitionWithTransformation
import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

class ParserTest extends Specification {


    def "simple service definition"() {
        given:
        def simpleDSL = """
        service Foo {
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
        stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions().size() == 1
        stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0] instanceof ObjectTypeDefinition
        ((ObjectTypeDefinition) stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0]).name == 'Query'
        ((ObjectTypeDefinition) stitchingDSL.getServiceDefinitions()[0].getTypeDefinitions()[0]).fieldDefinitions[0].name == 'hello'

    }

    def "two services"() {
        given:
        def simpleDSL = """
         service Foo {
            type Query {
                hello1: String
            }
        }
        service Bar {
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
        }
        someFoo
       """
        when:
        Parser parser = new Parser()
        parser.parseDSL(simpleDSL)

        then:
        thrown(ParseCancellationException)
    }

    def "parse input mapping"() {
        given:
        def dsl = """
        service FooService {
            type Query {
                foo: Foo
            }

            type Foo {
                id: ID <= \$source.fooId
            }
        }
        """
        when:
        Parser parser = new Parser()
        then:
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        def fooTypDefinition = stitchingDSL.serviceDefinitions[0].typeDefinitions[1]
        fooTypDefinition instanceof ObjectTypeDefinition
        def fieldDefinition = (fooTypDefinition as ObjectTypeDefinition).fieldDefinitions[0]
        fieldDefinition instanceof FieldDefinitionWithTransformation
        (fieldDefinition as FieldDefinitionWithTransformation).fieldTransformation.inputMappingDefinition.inputName == "fooId"

    }

    def "parse inner service transformation"() {
        given:
        def dsl = """
        service FooService {
            type Query {
                foo: Foo
            }

            type Foo {
                id: ID <= \$innerQueries.OtherService.resolveId(otherId: \$source.id)
            }
        }
        """
        when:
        Parser parser = new Parser()
        then:
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        def fooTypDefinition = stitchingDSL.serviceDefinitions[0].typeDefinitions[1]
        fooTypDefinition instanceof ObjectTypeDefinition
        def fieldDefinition = (fooTypDefinition as ObjectTypeDefinition).fieldDefinitions[0]
        fieldDefinition instanceof FieldDefinitionWithTransformation
        def innerServiceTransformation = (fieldDefinition as FieldDefinitionWithTransformation).fieldTransformation.innerServiceTransformation
        innerServiceTransformation.serviceName == "OtherService"
        innerServiceTransformation.topLevelField == "resolveId"
        innerServiceTransformation.arguments.containsKey("otherId")
        innerServiceTransformation.arguments["otherId"].inputName == "id"

    }

//    @Ignore
//    def "parse transformation"() {
//        given:
//        def dsl = """
//        service FooService {
//            type Query {
//                foo: Foo
//            }
//
//            type Foo {
//                id: ID <= \$source.fooId
//                title : String <= \$source.name
//                category : String <= \$innerQueries.foo.category(id: \$source.fooId, secondId: \$source.barId)
//            }
//        }
//
//        service BarService {
//            type Query {
//                bar(id: ID): Bar
//            }
//
//            type Bar <= \$innerTypes.FooBar {
//                id: ID
//            }
//        }
//        """
//        then:
//        true
//        // TODO: check the resulting AST here
//    }
}


