package graphql.nadel.engine

import graphql.language.SourceLocation
import graphql.nadel.util.ErrorUtil
import spock.lang.Specification

class ErrorUtilTest extends Specification {

    def "test that an error can be converted"() {

        def rawError = [message: "M", path: ["a", "b", "c"], locations: [[line: 5, column: 2]], extensions: [ext: "val"]]

        when:
        def graphlError = ErrorUtil.createGraphqlErrorFromRawError(rawError)

        then:
        graphlError.message == "M"
        graphlError.path == ["a", "b", "c"]
        graphlError.locations == [new SourceLocation(5, 2)]
        graphlError.extensions == [ext: "val"]
    }

    def "test that errors with missing attributes can be converted"() {

        def rawError = [message: "M"]

        when:
        def graphlError = ErrorUtil.createGraphqlErrorFromRawError(rawError)

        then:
        graphlError.message == "M"
        graphlError.path == null
        graphlError.extensions == null
        graphlError.locations == [] // graphl-java is returning an empty list - debatable
    }

    def "test that errors can be converted"() {

        def rawError = [message: "M", path: ["a", "b", "c"], locations: [[line: 5, column: 2]], extensions: [ext: "val"]]

        when:
        def graphlErrors = ErrorUtil.createGraphQlErrorsFromRawErrors([rawError])

        then:
        graphlErrors[0].message == "M"
        graphlErrors[0].path == ["a", "b", "c"]
        graphlErrors[0].locations == [new SourceLocation(5, 2)]
        graphlErrors[0].extensions == [ext: "val"]
    }

    def "test that an bogus errors can be converted"() {

        def rawError = [:]

        when:
        def graphlError = ErrorUtil.createGraphqlErrorFromRawError(rawError)

        then:
        graphlError.message == "null"
    }
}
