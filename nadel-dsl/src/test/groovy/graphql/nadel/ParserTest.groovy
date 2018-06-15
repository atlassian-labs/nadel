package graphql.nadel

import graphql.language.ObjectTypeDefinition
import graphql.nadel.dsl.FieldTransformation
import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

class ParserTest extends Specification {


    def "simple service definition"() {
        given:
        def simpledsl = """
        service foo {
            serviceurl: "someurl"
            type query {
                hello: string
            }
        }
       """
        def stitchingdsl
        when:
        parser parser = new parser()
        stitchingdsl = parser.parsedsl(simpledsl)

        then:
        stitchingdsl.getservicedefinitions().size() == 1
        stitchingdsl.getservicedefinitions()[0].url == 'someurl'
        stitchingdsl.getservicedefinitions()[0].gettypedefinitions().size() == 1
        stitchingdsl.getservicedefinitions()[0].gettypedefinitions()[0] instanceof objecttypedefinition
        ((objecttypedefinition) stitchingdsl.getservicedefinitions()[0].gettypedefinitions()[0]).name == 'query'
        ((objecttypedefinition) stitchingdsl.getservicedefinitions()[0].gettypedefinitions()[0]).fielddefinitions[0].name == 'hello'

    }

    def "two services"() {
        given:
        def simpledsl = """
         service foo {
            serviceurl: "url1"
            type query {
                hello1: string
            }
        }
        service bar {
            serviceurl: "url2"
            type query {
                hello2: string
            }
        }
       """
        def stitchingdsl
        when:
        parser parser = new parser()
        stitchingdsl = parser.parsedsl(simpledsl)

        then:
        stitchingdsl.getservicedefinitions().size() == 2

    }

    def "parse error"() {
        given:
        def simpledsl = """
        service foo {
            urlx: "someurl"
        }
       """
        when:
        parser parser = new parser()
        parser.parsedsl(simpledsl)

        then:
        thrown(exception)

    }

    def "not all tokens are parsed"() {
        given:
        def simpledsl = """
        service foo {
            url: "someurl"
        }
        somefoo
       """
        when:
        parser parser = new parser()
        parser.parsedsl(simpledsl)

        then:
        thrown(parsecancellationexception)
    }

    def "parse transformation"() {
        given:
        def dsl = """
        service fooservice {
            serviceurl: "url1"
            type query {
                foo: foo
            }
            type foo {
                barid: id => from bar(id) with input barid as bar
            }
        }
        service barservice {
            serviceurl: "url2"
            type query {
                bar(id: id): bar
            }
            type bar {
                id: id
            }
        }
        """
        when:
        parser parser = new parser()
        then:
        def stitchingdsl = parser.parsedsl(dsl)

        then:
        stitchingdsl.getservicedefinitions()[0].gettypedefinitions().size() == 2

        objecttypedefinition footype = stitchingdsl.getservicedefinitions()[0].gettypedefinitions()[1]
        footype.name == "foo"
        def fielddefinition = footype.fielddefinitions[0]
        fieldtransformation fieldtransformation = stitchingdsl.gettransformationsbyfielddefinition().get(fielddefinition)
        fieldtransformation != null
        fieldtransformation.targetname == "bar"
    }

    def "empty arrow fails"() {
        def dsl = """
        service fooservice {
            serviceurl: "url1"

            type foo {
                barid: id => 
            }
        }
        """

        when:
        new parser().parsedsl(dsl)

        then:
        parsecancellationexception ex = thrown()
        // alternative syntax: def ex = thrown(invaliddeviceexception)
        ex.message.contains("expecting 'from'")

    }
}


