package graphql.nadel.schema

import graphql.nadel.testutils.MockedWiringFactory
import graphql.schema.idl.SchemaParser
import spock.lang.Specification

class UnderlyingSchemaGeneratorTest extends Specification {

    def "exceptions are thrown in he name of the service"() {

        def sdl = '''
            type QueryMissing {
                field : String
            }
        '''

        def registry = new SchemaParser().parse(sdl)

        when:
        new UnderlyingSchemaGenerator().buildUnderlyingSchema("serviceX", registry, new MockedWiringFactory())
        then:
        def serviceSchemaProblem = thrown(ServiceSchemaProblem)

        serviceSchemaProblem.getServiceName() == 'serviceX'
        serviceSchemaProblem.getMessage().contains("There was a problem building the schema for 'serviceX'")
        serviceSchemaProblem.getMessage().contains("A schema MUST have a 'query' operation defined")
    }
}
