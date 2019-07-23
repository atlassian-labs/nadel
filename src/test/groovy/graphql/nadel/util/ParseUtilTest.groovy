package graphql.nadel.util

import graphql.nadel.schema.ServiceSchemaProblem
import graphql.parser.MultiSourceReader
import spock.lang.Specification

class ParseUtilTest extends Specification {

    def "parse exceptions are thrown in the name of the service"() {

        def sdl = '''
            type QueryMissing {
                field : String
            }
            
            type Bad {
        '''


        when:
        def multiSourceReader = MultiSourceReader.newMultiSourceReader().string(sdl, "serviceXSDL").build()
        ParseUtil.parseServiceSDL("serviceX", multiSourceReader)
        then:
        def serviceSchemaProblem = thrown(ServiceSchemaProblem)

        serviceSchemaProblem.getServiceName() == 'serviceX'
        serviceSchemaProblem.getMessage().contains("There was a problem parsing the schema SDL for 'serviceX'")
    }
}
