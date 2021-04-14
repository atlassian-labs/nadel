package graphql.nadel.engine.execution

import graphql.nadel.engine.NadelContext
import graphql.nadel.engine.testutils.TestUtil
import spock.lang.Specification

class NadelContextTest extends Specification {

    def doc = TestUtil.parseQuery(''' query OpName { hello } ''')
    def multiOpDoc = TestUtil.parseQuery(''' 
        query OpName1 { hello1 } 
        query OpName2 { hello2 } 
        query OpName3 { hello3 } 
        ''')

    def "basic builder shape is ok"() {
        when:
        def nadelContext = NadelContext.newContext().userSuppliedContext("hello").originalOperationName(doc, null).build()
        then:
        nadelContext.userSuppliedContext == "hello"
        nadelContext.originalOperationName == "OpName"
        !nadelContext.underscoreTypeNameAlias.isEmpty()

    }

    def "can handle multiple operations"() {
        when:
        def nadelContext = NadelContext.newContext().userSuppliedContext("world").originalOperationName(multiOpDoc, "OpName1").build()
        then:
        nadelContext.userSuppliedContext == "world"
        nadelContext.originalOperationName == "OpName1"
        !nadelContext.underscoreTypeNameAlias.isEmpty()

    }
}
