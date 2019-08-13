package graphql.nadel.hooks

import graphql.Scalars
import graphql.execution.ExecutionStepInfo
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

class ModifiedArgumentsTest extends Specification {

    def field = TestUtil.parseMergedField('field(arg1 : "abc", arg2 : "xyz")')
    def esi = ExecutionStepInfo.newExecutionStepInfo()
            .field(field)
            .type(Scalars.GraphQLString)
            .build()


    def "can modify itself"() {

        when:
        def modifiedArguments = ModifiedArguments.newModifiedArguments(esi).build()
        then:
        modifiedArguments.getFieldArgs().collect({ arg -> arg.getName() }) == ["arg1", "arg2"]
        modifiedArguments.getVariables() == [:]

    }

    def "can transform itself"() {
        when:
        def modifiedArguments = ModifiedArguments.newModifiedArguments(esi).build()
        modifiedArguments = modifiedArguments.transform({ b -> b.variables(["c": "z"]) })
        then:
        modifiedArguments.getFieldArgs().collect({ arg -> arg.getName() }) == ["arg1", "arg2"]
        modifiedArguments.getVariables() == ["c": "z"]
    }
}
