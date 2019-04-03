package graphql.nadel.engine.transformation


import spock.lang.Specification
import spock.lang.Unroll

import static graphql.nadel.testutils.TestUtil.mkField
import static graphql.nadel.testutils.TestUtil.mkFragments

class FieldUtilsTest extends Specification {


    @Unroll
    def "can find sub fields at an index for #fieldName"() {

        def parentField = mkField('''
            parent {
                targetField1
                targetField2
                targetField3
            }
        ''')

        expect:

        expectedResult == FieldUtils.hasFieldSubSelectionAtIndex(fieldName, parentField, desiredIndex)

        where:
        fieldName      | desiredIndex | expectedResult
        "targetField1" | 0            | true
        "targetField1" | 1            | false
        "targetField1" | 2            | false
        "targetField1" | 3            | false

        "targetField2" | 0            | false
        "targetField2" | 1            | true

        "notPresent"   | 0            | false
        "notPresent"   | 1            | false
    }

    @Unroll
    def "can find un-aliased field selections for #fieldName"() {

        def fragments = mkFragments('''
            fragment Frag1 on Type {
                fragField1
                aliasedAsWell : fragField2
            }
        ''')

        def parentField = mkField('''
            parent {
                targetField1
                targetField2
                
                aliased : targetAliased
                
                ... Frag1
                
                ... on TypeCondition {
                    inlineField1
                    aliasedInline : inlineField2
                }
            }
        ''')

        expect:

        expectedResult == FieldUtils.hasUnAliasedFieldSubSelection(fieldName, parentField, fragments)

        where:
        fieldName       | expectedResult
        "targetField1"  | true
        "targetField2"  | true
        "targetField3"  | false
        "targetAliased" | false

        "fragField1"    | true
        "fragField2"    | false
        "fragField2"    | false

        "inlineField1"  | true
        "inlineField2"  | false
    }

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
