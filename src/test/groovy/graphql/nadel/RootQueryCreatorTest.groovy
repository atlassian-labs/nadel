package graphql.nadel

import graphql.language.AstPrinter
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

class RootQueryCreatorTest extends Specification {


    def "create query"() {
        given:
        def queryCreator = new RootQueryCreator()
        def environment = Mock(DataFetchingEnvironment)
        Field field = new Field("hello")
        environment.getFields() >> [field]

        def document
        when:
        document = queryCreator.createQuery(environment)

        then:
        AstPrinter.printAst(document) == """query {
  hello
}
"""
    }
}
