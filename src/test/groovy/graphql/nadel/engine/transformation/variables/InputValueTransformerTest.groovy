package graphql.nadel.engine.transformation.variables

import graphql.Scalars
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLEnumType
import spock.lang.Specification

class InputValueTransformerTest extends Specification {

    def sdl = '''
        
        directive @directive1 on ARGUMENT_DEFINITION | FIELD_DEFINITION

        enum RGB {
            red, blue,green
        }
        
        input ComplexInputType {
            stringField : String @directive1
            intField : Int
            enumField : RGB
            nonNullField : Boolean!
            listField : [ComplexInputType]
            objectField : ComplexInputType     
        }

        type Query {
            field( arg : ComplexInputType) : String
            fieldSimple( simpleArg : String) : String
        }
    '''

    InputValueTransform twizzleTransformFunction = new InputValueTransform() {
        @Override
        Object transformValue(Object value, InputValueTree inputTypeTree) {
            if (inputTypeTree.inputType == Scalars.GraphQLString) {
                return String.valueOf(value).toUpperCase()
            }
            if (inputTypeTree.inputType == Scalars.GraphQLInt) {
                return 999
            }
            if (inputTypeTree.inputType == Scalars.GraphQLBoolean) {
                return !((Boolean) value)
            }
            if (inputTypeTree.inputType instanceof GraphQLEnumType) {
                return String.valueOf(value).reverse()
            }

            if (inputTypeTree.name == "listField") {
                return ["list", "changed"]
            }
            return value
        }
    }

    def schema = TestUtil.schema(sdl)

    def arg = schema.getObjectType("Query").getFieldDefinition("field").getArgument("arg")
    def simpleArg = schema.getObjectType("Query").getFieldDefinition("fieldSimple").getArgument("simpleArg")

    def "can modify variables"() {

        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                return value
            }
        }

        def complexFieldObj1 = [nonNullField: true, listField: [], stringField: "abc"]
        def complexFieldObj2 = [nonNullField: false, listField: [], stringField: "def"]
        def complexFieldObj3 = [nonNullField: true, listField: [], stringField: "xyz"]

        def coercedValue = [
                stringField: "abc", intField: 666, nonNullField: true, enumField: "red",
                listField  : [complexFieldObj1, complexFieldObj2],
                objectField: complexFieldObj3
        ]

        when:
        def newValue = InputValueTransformer.transform(arg, coercedValue, transformFunction)

        then:
        !newValue.is(coercedValue) // not the same object
        newValue == coercedValue // but build out the sames
    }

    def "can change variables"() {


        def complexFieldObj1 = [nonNullField: true, listField: [], stringField: "abc"]
        def complexFieldObj2 = [nonNullField: false, listField: [], stringField: "def"]
        def complexFieldObj3 = [nonNullField: true, listField: [], stringField: "xyz"]

        def coercedValue = [
                stringField: "abc", intField: 666, nonNullField: true, enumField: "red",
                listField  : [complexFieldObj1, complexFieldObj2],
                objectField: complexFieldObj3
        ]

        when:
        def newValue = InputValueTransformer.transform(arg, coercedValue, twizzleTransformFunction)

        then:
        !newValue.is(coercedValue) // not the same object
        newValue["stringField"] == "ABC"
        newValue["intField"] == 999
        newValue["enumField"] == "der"
        newValue["nonNullField"] == false
        newValue["listField"] == ["list", "changed"]
    }

    def "can change simple"() {
        def variable = "abc"

        when:
        def newValue = InputValueTransformer.transform(simpleArg, variable, twizzleTransformFunction)

        then:
        !newValue.is(variable) // not the same object
        newValue == "ABC"
    }

    def "grabs directive containers as they whizz past"() {
        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                if (inputTypeTree.name == "stringField") {
                    assert inputTypeTree.valueDefinition.getDirective("directive1") != null
                }
                return value
            }
        }

        def coercedValue = [stringField: "abc", intField: 666, nonNullField: true, enumField: "red",]

        when:
        def newValue = InputValueTransformer.transform(arg, coercedValue, transformFunction)

        then:
        newValue["stringField"] == "abc"
    }
}

