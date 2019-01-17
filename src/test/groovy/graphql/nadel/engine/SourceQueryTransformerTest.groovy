package graphql.nadel.engine

import graphql.execution.ExecutionContext
import graphql.execution.nextgen.FieldSubSelection
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.nadel.TestUtil
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import java.util.stream.Collectors

class SourceQueryTransformerTest extends Specification {
    def schema = TestUtil.schemaFromNdsl('''
        service example {
            type Query { 
                hello: String <= $source.helloWorld 
                foo(id: ID!): Foo
            }
            
            type Foo {
                id: ID!
                barId: String <= $source.bazId
                qux: String!
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

    def "referenced named fragments are transformed and included"() {

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
                'query {f1:foo(id:"1") {...frag1} f2:foo(id:"2") {...frag2}} fragment frag2 on Foo {id bazId} fragment frag1 on Foo {id}'
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
                'query {foo(id:"1") {...frag1}} fragment frag2 on Foo {bazId ...frag3} fragment frag3 on Foo {qux} ' +
                'fragment frag1 on Foo {id ...frag2}'
    }


    private Document doTransform(GraphQLSchema schema, Document query) {
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)
        List<Field> fields = fieldSubSelection.getSubFields().values().stream()
                .map({ it.getSingleField() })
                .collect(Collectors.toList())

        def transformer = new SourceQueryTransformer(executionContext)
        transformer.transform(fields, OperationDefinition.Operation.QUERY)
        return transformer.delegateDocument()
    }
}
