package graphql.nadel

import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.nadel.dsl.FieldTransformation
import graphql.nadel.dsl.ServiceDefinition
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

    def "parse transformation"() {
        given:
        def dsl = """
        service FooService {
            type Query {
                foo: Foo
            }

            type Foo {
                id: ID <= \$source.fooId
                title : String <= \$source.name
                category : String <= \$innerQueries.foo.category(id: \$source.fooId)
            }
        }
        
        service BarService {
            type Query {
                bar(id: ID): Bar
            }

            type Bar <= \$innerTypes.FooBar {
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
        def barIdDefinition = fooType.fieldDefinitions[0]
        FieldTransformation barTransformation = stitchingDSL.getTransformationsByFieldDefinition().get(barIdDefinition)
        barTransformation != null
        barTransformation.targetName == "fooId"

        def titleDefinition = fooType.fieldDefinitions[1]
        def titleTransformation = stitchingDSL.getTransformationsByFieldDefinition().get(titleDefinition)
        titleTransformation != null
        titleTransformation.targetName == "name"

        def categoryDefinition = fooType.fieldDefinitions[2]
        def categoryTransformation = stitchingDSL.getTransformationsByFieldDefinition().get(categoryDefinition)
        categoryTransformation != null
        categoryTransformation.targetName == "category"
        categoryTransformation.serviceName == "foo"


        ObjectTypeDefinition barType = stitchingDSL.getServiceDefinitions()[1].getTypeDefinitions()[1]
        barType.name == "Bar"
        def barTypeTransformation = stitchingDSL.getTransformationsByTypeDefinition().get(barType)
        barTypeTransformation != null
    }
}


