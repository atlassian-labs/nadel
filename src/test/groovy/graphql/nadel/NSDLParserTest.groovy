package graphql.nadel

import graphql.parser.InvalidSyntaxException
import org.antlr.v4.runtime.misc.ParseCancellationException
import spock.lang.Specification

import static graphql.nadel.testutils.TestUtil.astAsMap
import static graphql.nadel.testutils.TestUtil.expectedJson

class NSDLParserTest extends Specification {


    def "rename with more than one level is not allowed"() {
        given:
        def simpleDSL = """
        service Foo {
            type Query {
                hello: String => renamed from detail.subDetail.hello
            }
        }
       """
        NSDLParser parser = new NSDLParser()
        when:
        parser.parseDSL(simpleDSL)

        then:
        thrown(InvalidSyntaxException)

    }

    def "simple service definition"() {
        given:
        def simpleDSL = """
        service Foo {
            type Query {
                hello(arg1: String, arg2: Integer): String
            }
        }
       """
        NSDLParser parser = new NSDLParser()
        when:
        def stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        astAsMap(stitchingDSL) == expectedJson("service-definition.json")

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
        NSDLParser parser = new NSDLParser()
        when:
        def stitchingDSL = parser.parseDSL(simpleDSL)

        then:
        astAsMap(stitchingDSL) == expectedJson("two-services.json")

    }

    def "parse error"() {
        given:
        def simpleDSL = """
        service Foo {
            urlX: "someUrl"
        }
       """
        when:
        NSDLParser parser = new NSDLParser()
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
        NSDLParser parser = new NSDLParser()
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
                id: ID => renamed from fooId
            }
        }
        """
        when:
        NSDLParser parser = new NSDLParser()
        then:
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        astAsMap(stitchingDSL) == expectedJson("field-mapping.json")

    }


    def "parse hydration"() {
        given:

        def dsl = '''
        service FooService {
            type Query {
                foo: Foo
            }

            type Foo {
                id(inputArg: ID!): ID => hydrated from OtherService.resolveId(otherId: $source.id, 
                arg1: $context.ctxParam, arg2: $argument.inputArg)
            }
        }
        '''
        when:
        NSDLParser parser = new NSDLParser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == expectedJson("hydration.json")
    }


    def "parse object type transformation"() {
        given:

        def dsl = """
            service FooService {
                type Query {
                    foo: Foo
                }

                type Foo  @directiveFirst  => renamed from UnderlyingFooName {
                    id: ID
                }
            }
        """
        when:
        NSDLParser parser = new NSDLParser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == expectedJson("object-type-transformation.json")
    }

    def "parse interface type transformation"() {
        given:

        def dsl = """
            service FooService {
                type Query {
                    foo: Foo
                }

                interface FooInterface @directiveFirst  => renamed from UnderlyingFooName {
                    id: ID
                }
            }
        """
        when:
        NSDLParser parser = new NSDLParser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == expectedJson("interface-type-transformation.json")
    }

    def "parse union type transformation"() {
        given:

        def dsl = """
            service FooService {
                type Query {
                    foo: Foo
                }

                union FooUnion  @directiveFirst  => renamed from UnderlyingFooName = Photo | Person
            }
        """
        when:
        NSDLParser parser = new NSDLParser()
        def stitchingDSL = parser.parseDSL(dsl)
        then:
        astAsMap(stitchingDSL) == expectedJson("union-type-transformation.json")
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
                    newId: ID > renamed from id @testdirective 
                }
            }
        """
        when:
        NSDLParser parser = new NSDLParser()
        parser.parseDSL(dsl)
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
                    newId: ID @testdirective  => renamed from id 
                }
            }
        """

        when:
        NSDLParser parser = new NSDLParser()
        def stitchingDSL = parser.parseDSL(dsl)

        then:
        astAsMap(stitchingDSL).size() > 0
    }
}


