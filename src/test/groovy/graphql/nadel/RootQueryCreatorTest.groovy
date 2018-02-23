package graphql.nadel

import graphql.language.AstPrinter
import graphql.language.Field
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.dsl.StitchingDsl
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

class RootQueryCreatorTest extends Specification {


    def "create query"() {
        given:
        ServiceDefinition serviceDefinition = new ServiceDefinition("name", "url", null)
        StitchingDsl stitchingDsl = new StitchingDsl()
        def queryCreator = new RootQueryCreator(serviceDefinition, stitchingDsl)
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

    def "create query respecting the field transformation"() {

    }
}
