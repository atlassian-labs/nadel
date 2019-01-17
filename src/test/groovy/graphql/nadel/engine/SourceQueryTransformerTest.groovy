package graphql.nadel.engine

import graphql.execution.ExecutionContext
import graphql.execution.nextgen.FieldSubSelection
import graphql.nadel.TestUtil
import spock.lang.Specification

class SourceQueryTransformerTest extends Specification {
    def "simple query"() {

        def data = ["hello": "world"]
        def schema = TestUtil.schema("type Query{ hello: String }")
        def query = TestUtil.parseQuery(
                '''
            {hello { 
            mother father
            ... on Hello{
                field
            }
            }}
            ''')
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        def sourceQueryTransformer = new SourceQueryTransformer(new HashMap<String, Object>(), executionContext)

        when:
        sourceQueryTransformer.transform(fieldSubSelection.mergedSelectionSet.getSubFieldsList().first().singleField)

        then:
        1 == 1
    }


}
