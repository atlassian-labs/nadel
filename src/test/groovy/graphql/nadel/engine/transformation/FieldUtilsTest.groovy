package graphql.nadel.engine.transformation

import spock.lang.Specification
import spock.lang.Unroll

import static graphql.nadel.testutils.TestUtil.mkField

class FieldUtilsTest extends Specification {

    @Unroll
    def "can find aliases on field for #expectedResult"() {

        expect:

        expectedResult == FieldUtils.resultKeyForField(field)

        where:
        field                             | expectedResult
        mkField("targetField")            | "targetField"
        mkField("aliased : targetField1") | "aliased"
    }
}
