package graphql.nadel.engine

import graphql.execution.ExecutionContext
import graphql.execution.MergedField
import graphql.execution.nextgen.FieldSubSelection
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.nadel.Operation
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLSchema
import spock.lang.Specification

class OverallQueryTransformerTest extends Specification {
    def schema = TestUtil.schemaFromNdsl('''
        service example {
            type Query { 
                hello: String => renamed as helloWorld 
                foo(id: ID!): Foo
                bar(id: ID!): Bar
            }
            type Mutation {
                hello: String
            }
            
            type Foo {
                id: ID!
                barId: String => renamed as bazId
                qux: String!
                anotherFoo: AnotherFoo => hydrated as AnotherService.topLevel(id: $source.anotherSourceId)
            }
            
            type Bar => renamed as Baz {
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

    def "used variables are included and not used ones left out"() {
        def query = TestUtil.parseQuery(
                '''query( $usedVariable : String, $unusedVariable : String )
            {
               foo(id : $usedVariable) {
                 id
               }
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) ==
                'query ($usedVariable:String) {foo(id:$usedVariable) {id}}'
    }

    def "nested fragments are transformed and included"() {
        def query = TestUtil.parseQuery(
                '''
            {
             foo(id: "1") {
                ...frag1
             }
             
             bar(id: "1") {
                ...barFrag
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
            
            fragment barFrag on Bar {
                barId: id
            }
            ''')

        when:
        def delegateQuery = doTransform(schema, query)

        then:
        AstPrinter.printAstCompact(delegateQuery) ==
                'query {foo(id:"1") {...frag1} bar(id:"1") {...barFrag}} ' +
                'fragment frag1 on Foo {id ...frag2} ' +
                'fragment barFrag on Baz {barId:id} ' +
                'fragment frag2 on Foo {bazId ...frag3} ' +
                'fragment frag3 on Foo {qux}'
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
        def transformationResult = transformer.transformMergedFields(executionContext, null, Operation.QUERY, fields)
        when:
        def document = transformationResult.document


        then:
        AstPrinter.printAstCompact(document) == 'query {foo(id:"12") {anotherSourceId}}'
    }


    private static Document doTransform(GraphQLSchema schema, Document query, Operation operation = Operation.QUERY, String operationName = null) {
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        List<MergedField> fields = new ArrayList<>(fieldSubSelection.getSubFields().values())

        def transformer = new OverallQueryTransformer()
        def transformationResult = transformer.transformMergedFields(executionContext, operationName, operation, fields)
        return transformationResult.document
    }
}
