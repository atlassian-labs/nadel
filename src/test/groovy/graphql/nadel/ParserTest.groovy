package graphql.nadel

import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

import static graphql.nadel.TestUtil.astAsMap
import static graphql.nadel.TestUtil.getExpectedData

class ParserTest extends Specification {

    def "simple service definition"() {
        given:
        def simpleDSL = """
        service Foo {
            type Query {
                hello(arg1: String, arg2: Integer): String
            }
        }
       """
        Parser parser = new Parser()
        when:
        def stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        astAsMap(stitchingDSL) == getExpectedData("service-definition")

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
        Parser parser = new Parser()
        when:
        def stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        astAsMap(stitchingDSL) == getExpectedData("two-services")

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

    def "parse field mapping"() {
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
        astAsMap(stitchingDSL) == getExpectedData("field-mapping")

    }


    def "parse hydration"() {
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
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == getExpectedData("hydration")
    }


    def "parse type transformation"() {
        given:

        def dsl = """
            service FooService {
                type Query {
                    foo: Foo
                }

                type Foo <= \$innerTypes.OriginalFooName {
                    id: ID
                }
            }
        """
        when:
        Parser parser = new Parser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == getExpectedData("type-transformation")
    }

    def "parse directives to wrong place for field transformation"() {
        given:

        def dsl = """
            service FooService {
                directive @testdirective on FIELD_DEFINITION
                type Query {
                    foo: Foo
                }

                type Foo {
                    newId: ID <= \$source.id @testdirective 
                }
            }
        """
        when:
        Parser parser = new Parser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        thrown(Exception)
    }

    def "parse directives for field transformation should not break"() {
        given:

        def dsl = """
            service FooService {
                directive @testdirective on FIELD_DEFINITION
                type Query {
                    foo: Foo
                }

                type Foo {
                    newId: ID @testdirective  <= \$source.id 
                }
            }
        """

        when:
        Parser parser = new Parser()
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        astAsMap(stitchingDSL).size() > 0
    }
}


