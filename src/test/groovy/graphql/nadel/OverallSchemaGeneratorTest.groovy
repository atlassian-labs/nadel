package graphql.nadel


import spock.lang.Specification
import spock.lang.Unroll

class OverallSchemaGeneratorTest extends Specification {
    static def fooType = """
           type Foo {
                name: String
            }
    """
    static def barType = """
           type Bar {
                id: String
            }
    """
    static def fooService_default_query = """
           type Query{
                foo: Foo
           } 
    """
    static def fooService_query_in_schema = """
           schema {
              query: fooQuery
           }
           type fooQuery{
                foo: Foo
           } 
    """

    static def fooQueryExtension = """
           extend type Query { 
                foo2: Foo
           }
    """
    static def fooService_default_mutation = """
           type Mutation{
                setFoo(name:String): Foo
           } 
    """
    static def fooService_mutation_in_schema = """
           schema {
              mutation: fooMutation
           }
           type fooMutation{
                setFoo(name:String): Foo
           } 
    """
    static def fooMutationExtension = """
           extend type Mutation { 
                foo2(name:String): Foo
           }
    """
    static def barService_default_query = """
           type Query{
                bar: Bar
           } 
    """

    static def barService_query_in_schema = """
           schema {
              query: barQuery
           }
           type barQuery{
                bar: Bar
           } 
    """

    static def barQueryExtension = """
           extend type Query { 
                bar2: Bar
           }
    """
    static def barService_default_mutation = """
           type Mutation{
                setBar(id:String): Bar
           } 
    """
    static def barService_mutation_in_schema = """
           schema {
              mutation: barMutation
           }
           type barMutation{
                setBar(id:String): Bar
           } 
    """
    static def barMutationExtension = """
           extend type Mutation { 
                bar2(id:String): Bar
           }
    """

    @Unroll
    def "#opsType definition could be merged for #caseName"(String opsType, String caseName, String fooService, String barService, List<String> expectedList) {
        given:
        def schema = TestUtil.schemaFromNdsl(fooService + barService)
        when:
        def resultList =  (opsType =="query")
                             ? schema.getQueryType().children.stream().map({ gtype -> gtype.getName() }).collect()
                             : (opsType =="mutation")
                                     ? schema.getMutationType().children.stream().map({ gtype -> gtype.getName() }).collect()
                                     : null

        then:
        resultList as Set == expectedList as Set

        where:
        opsType          | caseName                                                                             | fooService                                                                    | barService                                                                    | expectedList                   | _
        "query"          |"both services with default definition"                                               | "service Foo {$fooService_default_query $fooType}"                            | "service Bar {$barService_default_query $barType}"                            | ["foo", "bar"]                 | _
        "query"          |"one service with default definition and one service defined in schema"               | "service Foo {$fooService_query_in_schema $fooType}"                          | "service Bar {$barService_default_query $barType}"                            | ["foo", "bar"]                 | _
        "query"          |"one service with default definition and one service defined in schema and extension" | "service Foo {$fooService_query_in_schema $fooType $fooQueryExtension}"       | "service Bar {$barService_default_query $barType}"                            | ["foo", "foo2", "bar"]         | _
        "query"          |"both services with definition in schema"                                             | "service Foo {$fooService_query_in_schema $fooType}"                          | "service Bar {$barService_query_in_schema $barType}"                          | ["foo", "bar"]                 | _
        "query"          |"both services with definition in schema and extension"                               | "service Foo {$fooService_query_in_schema $fooType $fooQueryExtension}"       | "service Bar {$barService_query_in_schema $barType, $barQueryExtension}"      | ["foo", "foo2", "bar", "bar2"] | _
        "mutation"       |"both services with default definition"                                               | "service Foo {$fooService_default_mutation $fooType}"                         | "service Bar {$barService_default_mutation $barType}"                         | ["foo", "bar"]                 | _
        "mutation"       |"one service with default definition and one service defined in schema"               | "service Foo {$fooService_mutation_in_schema $fooType}"                       | "service Bar {$barService_default_mutation $barType}"                         | ["foo", "bar"]                 | _
        "mutation"       |"one service with default definition and one service defined in schema and extension" | "service Foo {$fooService_mutation_in_schema $fooType $fooMutationExtension}" | "service Bar {$barService_default_mutation $barType}"                         | ["foo", "foo2", "bar"]         | _
        "mutation"       |"both services with definition in schema"                                             | "service Foo {$fooService_mutation_in_schema $fooType}"                       | "service Bar {$barService_mutation_in_schema $barType}"                       | ["foo", "bar"]                 | _
        "mutation"       |"both services with definition in schema and extension"                               | "service Foo {$fooService_mutation_in_schema $fooType $fooMutationExtension}" | "service Bar {$barService_mutation_in_schema $barType $barMutationExtension}" | ["foo", "foo2", "bar", "bar2"] | _

    }
}
