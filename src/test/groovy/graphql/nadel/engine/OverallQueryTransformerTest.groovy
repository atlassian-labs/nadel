package graphql.nadel.engine

import graphql.execution.ExecutionContext
import graphql.execution.MergedField
import graphql.execution.nextgen.FieldSubSelection
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.nadel.Operation
import graphql.nadel.TestUtil
import graphql.schema.GraphQLSchema
import spock.lang.Specification

class OverallQueryTransformerTest extends Specification {
    def schema = TestUtil.schemaFromNdsl('''
        service example {
            type Query { 
                hello: String <= $source.helloWorld 
                foo(id: ID!): Foo
                bar(id: ID!): Bar
            }
            type Mutation {
                hello: String
            }
            
            type Foo {
                id: ID!
                barId: String <= $source.bazId
                qux: String!
                anotherFoo: AnotherFoo <= $innerQueries.AnotherService.topLevel(id: $source.anotherSourceId)
            }
            
            type Bar <= $innerTypes.Baz {
                id: ID!
            }
        }
        service AnotherService {
            type Query { 
                topLevel(id:ID): AnotherFoo
            }
            type AnotherFoo {
                id: ID!
            }
        }
         ''')

    def "transforms query to delegate with field rename"() {
        def query = TestUtil.parseQuery(
                '''
            {
             hAlias: hello 
             foo(id: "1") {
                fooId: id
                barId
             }
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) == "query {hAlias:helloWorld foo(id:\"1\") {fooId:id bazId}}"
    }

    def "simple mutation"() {
        def query = TestUtil.parseQuery(
                '''
            mutation M {
             hAlias: hello 
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query, Operation.MUTATION, "M")

        then:
        AstPrinter.printAstCompact(delegateQuery) == "mutation M {hAlias:hello}"
    }

    def "used fragments are transformed and included and not used ones left out"() {
        def query = TestUtil.parseQuery(
                '''
            {
             f1: foo(id: "1") {
                ...frag1
             }
             f2: foo(id: "2") {
                ...frag2
             }
            }
            fragment frag1 on Foo {
                id
            }
            fragment frag2 on Foo {
                id
                barId
            }
            fragment unrefFrag on Foo {
                barId
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) ==
                'query {f1:foo(id:"1") {...frag1} f2:foo(id:"2") {...frag2}} fragment frag1 on Foo {id} fragment frag2 on Foo {id bazId}'
    }

    def "nested fragments are transformed and included"() {
        def query = TestUtil.parseQuery(
                '''
            {
             foo(id: "1") {
                ...frag1
             }
            }
            fragment frag1 on Foo {
                id
                ...frag2
            }
            fragment frag2 on Foo {
                barId
                ...frag3
            }
            fragment frag3 on Foo {
                qux
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) ==
                "query {foo(id:\"1\") {...frag1}} fragment frag1 on Foo {id ...frag2} fragment frag2 on Foo {bazId ...frag3} fragment frag3 on Foo {qux}"
    }

    def "inline fragments are transformed and types are renamed"() {
        def query = TestUtil.parseQuery(
                '''
            {
             b1: bar(id: "1") {
                ... on Bar {
                    barId: id
                }
             }
            }
            ''')
        //TODO: add test case without type condition, currently getting NPE due to AstPrinter bug

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) ==
                'query {b1:bar(id:"1") {... on Baz {barId:id}}}'
    }


    def "test hydration transformation"() {
        given:
        def query = TestUtil.parseQuery(
                '''
            {
                foo(id: "12") {
                    anotherFoo {
                        name
                    }
                }
            }
            ''')

        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        List<MergedField> fields = new ArrayList<>(fieldSubSelection.getSubFields().values())

        def transformer = new OverallQueryTransformer()
        def transformationResult = transformer.transformMergedFields(executionContext, fields, Operation.QUERY, null)
        when:
        def document = transformationResult.document


        then:
        AstPrinter.printAstCompact(document) == 'query {foo(id:"12") {anotherSourceId}}'
    }


    private Document doTransform(GraphQLSchema schema, Document query, Operation operation = Operation.QUERY, String operationName = null) {
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        List<MergedField> fields = new ArrayList<>(fieldSubSelection.getSubFields().values())

        def transformer = new OverallQueryTransformer()
        def transformationResult = transformer.transformMergedFields(executionContext, fields, operation, operationName)
        return transformationResult.document
    }
}
