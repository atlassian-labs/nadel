package graphql.nadel

import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

class ServiceExecutionParametersTest extends Specification {

    def "builder works as expected"() {
        given:
        Document document = TestUtil.parseQuery("query { foo }")
        def opDef = document.getChildren()[0] as OperationDefinition
        def context = [some: "Context"]
        def variables = [variables: "okPresent"]

        when:
        def parameters = ServiceExecutionParameters.newServiceExecutionParameters()
                .query(document)
                .context(context)
                .operationDefinition(opDef)
                .variables(variables)
                .build()

        then:
        parameters.query == document
        parameters.context == context
        parameters.variables == variables
    }
}
