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

    static def fooService_with_directives = """
           directive @cloudId(type:String) on ARGUMENT_DEFINITION
           type Query{
                foo(name: String @cloudId(type:"ari")): Foo
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
                setFoo2(name:String): Foo
           }
    """
    static def fooService_default_subscription = """
           type Subscription{
                subFoo: Foo
           } 
    """
    static def fooService_subscription_in_schema = """
           schema {
              subscription: fooSubscription
           }
           type fooSubscription{
                subFoo: Foo
           } 
    """
    static def fooSubscriptionExtension = """
           extend type Subscription { 
                subFoo2: Foo
           }
    """
    static def barService_default_query = """
           type Query{
                bar: Bar
           } 
    """
    static def barService_with_directives = """
           directive @cloudId2(type:String) on ARGUMENT_DEFINITION
           type Query{
                bar(id: ID! @cloudId2(type:"ari")): Bar
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
                setBar2(id:String): Bar
           }
    """
    static def barService_default_subscription = """
           type Subscription{
                subBar: Bar
           } 
    """
    static def barService_subscription_in_schema = """
           schema {
              subscription: barSubscription
           }
           type barSubscription{
                subBar: Bar
           } 
    """
    static def barSubscriptionExtension = """
           extend type Subscription { 
                subBar2: Bar
           }
    """
    @Unroll
    def "#opsType definition could be merged for #caseName"(String opsType, String caseName, String fooService, String barService, List<String> expectedList) {
        given:
        def schema = TestUtil.schemaFromNdsl(fooService + barService)
        when:
        def resultList
        switch (opsType){
            case Operation.QUERY.name:
                resultList = schema.getQueryType().children.stream().map({ gtype -> gtype.getName() }).collect()
                break
            case Operation.MUTATION.name:
                resultList = schema.getMutationType().children.stream().map({ gtype -> gtype.getName() }).collect()
                break
            case Operation.SUBSCRIPTION.name:
                resultList = schema.getSubscriptionType().children.stream().map({ gtype -> gtype.getName() }).collect()
                break
            case "directives":
                resultList = schema.getDirectives().stream().map({directive -> directive.getName()}).collect()
                break

        }
        then:
        resultList != null && resultList as Set == expectedList as Set

        where:
        opsType          | caseName                                                                             | fooService                                                                    | barService                                                                    | expectedList                   | _
        "query"          |"both services with default definition"                                               | "service Foo {$fooService_default_query $fooType}"                            | "service Bar {$barService_default_query $barType}"                            | ["foo", "bar"]                 | _
        "query"          |"one service with default definition and one service defined in schema"               | "service Foo {$fooService_query_in_schema $fooType}"                          | "service Bar {$barService_default_query $barType}"                            | ["foo", "bar"]                 | _
        "query"          |"one service with default definition and one service defined in schema and extension" | "service Foo {$fooService_query_in_schema $fooType $fooQueryExtension}"       | "service Bar {$barService_default_query $barType}"                            | ["foo", "foo2", "bar"]         | _
        "query"          |"both services with definition in schema"                                             | "service Foo {$fooService_query_in_schema $fooType}"                          | "service Bar {$barService_query_in_schema $barType}"                          | ["foo", "bar"]                 | _
        "query"          |"both services with definition in schema and extension"                               | "service Foo {$fooService_query_in_schema $fooType $fooQueryExtension}"       | "service Bar {$barService_query_in_schema $barType, $barQueryExtension}"      | ["foo", "foo2", "bar", "bar2"] | _
        "mutation"       |"both services with default definition"                                               | "service Foo {$fooService_default_mutation $fooType}"                         | "service Bar {$barService_default_mutation $barType}"                         | ["setFoo", "setBar"]                 | _
        "mutation"       |"one service with default definition and one service defined in schema"               | "service Foo {$fooService_mutation_in_schema $fooType}"                       | "service Bar {$barService_default_mutation $barType}"                         | ["setFoo", "setBar"]                 | _
        "mutation"       |"one service with default definition and one service defined in schema and extension" | "service Foo {$fooService_mutation_in_schema $fooType $fooMutationExtension}" | "service Bar {$barService_default_mutation $barType}"                         | ["setFoo", "setFoo2", "setBar"]      | _
        "mutation"       |"both services with definition in schema"                                             | "service Foo {$fooService_mutation_in_schema $fooType}"                       | "service Bar {$barService_mutation_in_schema $barType}"                       | ["setFoo", "setBar"]                 | _
        "mutation"       |"both services with definition in schema and extension"                               | "service Foo {$fooService_mutation_in_schema $fooType $fooMutationExtension}" | "service Bar {$barService_mutation_in_schema $barType $barMutationExtension}" | ["setFoo", "setFoo2", "setBar", "setBar2"] | _
        "subscription"   |"both services with default definition"                                               | "service Foo {$fooService_default_subscription $fooType}"                             | "service Bar {$barService_default_subscription $barType}"                         | ["subFoo", "subBar"]           | _
        "subscription"   |"one service with default definition and one service defined in schema"               | "service Foo {$fooService_subscription_in_schema $fooType}"                           | "service Bar {$barService_default_subscription $barType}"                         | ["subFoo", "subBar"]           | _
        "subscription"   |"one service with default definition and one service defined in schema and extension" | "service Foo {$fooService_subscription_in_schema $fooType $fooSubscriptionExtension}" | "service Bar {$barService_default_subscription $barType}"                         | ["subFoo", "subFoo2", "subBar"]| _
        "subscription"   |"both services with definition in schema"                                             | "service Foo {$fooService_subscription_in_schema $fooType}"                           | "service Bar {$barService_subscription_in_schema $barType}"                       | ["subFoo", "subBar"]           | _
        "subscription"   |"both services with definition in schema and extension"                               | "service Foo {$fooService_subscription_in_schema $fooType $fooSubscriptionExtension}" | "service Bar {$barService_subscription_in_schema $barType $barSubscriptionExtension}" | ["subFoo", "subFoo2", "subBar", "subBar2"] | _
        "directives"     |"both services"                                                                       | "service Foo {$fooService_with_directives $fooType }" | "service Bar {$barService_with_directives $barType}" | ["include", "skip", "defer", "deprecated", "cloudId","cloudId2"] | _
    }
}
