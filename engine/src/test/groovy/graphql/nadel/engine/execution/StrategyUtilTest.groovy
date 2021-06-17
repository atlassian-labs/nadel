package graphql.nadel.engine.execution

import graphql.AssertException
import graphql.nadel.engine.result.ExecutionResultNode
import graphql.nadel.engine.testutils.ExecutionResultNodeUtil
import spock.lang.Specification

import static graphql.nadel.engine.result.ObjectExecutionResultNode.newObjectExecutionResultNode
import static graphql.nadel.engine.result.RootExecutionResultNode.newRootExecutionResultNode

class StrategyUtilTest extends Specification {

    def mkObjectERN(String name, String childPrefix, Integer childCount) {
        def children = []
        def childrenValues = [:]
        for (int i = 0; i < childCount; i++) {
            def childName = childPrefix + i
            def childVal = childPrefix + i + "_val"
            children.add(ExecutionResultNodeUtil.leaf(childName))
            childrenValues.put(childName, childVal)
        }
        def info = ExecutionResultNodeUtil.esi(name)
        newObjectExecutionResultNode().fieldDefinition(ExecutionResultNodeUtil.fieldDefinition(name)).resultPath(info.path).completedValue(childrenValues).children(children).build()
    }

    def mkRootERN(List<ExecutionResultNode> resultNodes) {
        newRootExecutionResultNode().children(resultNodes).build()
    }

    def "can merge multiple fields that group on the same path"() {


        def root1 = mkRootERN([mkObjectERN("single", "s", 4)])
        def root2 = mkRootERN([
                mkObjectERN("namespaced", "a", 2),
                mkObjectERN("namespaced", "b", 3)
        ])
        when:
        def combinedRoot = StrategyUtil.mergeTrees([root1, root2])
        then:
        combinedRoot.children.size() == 2

        combinedRoot.children[0].fieldName == "single"
        combinedRoot.children[0].children.size() == 4

        def ern = combinedRoot.children[1]
        ern.fieldName == "namespaced"
        ern.children.size() == 5
        ern.children.collect { it.fieldName } == ["a0", "a1", "b0", "b1", "b2"]

        def completedValue = ern.completedValue as Map
        completedValue.size() == 5
        completedValue == [a0: "a0_val", a1: "a1_val", b0: "b0_val", b1: "b1_val", b2: "b2_val"]
    }

    def "can merge multiple fields that DO NOT group on the same path"() {

        def root1 = mkRootERN([mkObjectERN("single", "s", 4)])
        def root2 = mkRootERN([mkObjectERN("notnamespaced", "a", 2)])

        when:
        def combinedRoot = StrategyUtil.mergeTrees([root1, root2])
        then:
        combinedRoot.children.size() == 2

        combinedRoot.children[0].fieldName == "single"
        combinedRoot.children[0].children.size() == 4

        def ern = combinedRoot.children[1]
        ern.fieldName == "notnamespaced"
        ern.children.size() == 2
        ern.children.collect { it.fieldName } == ["a0", "a1"]

        def completedValue = ern.completedValue as Map
        completedValue.size() == 2
        completedValue == [a0: "a0_val", a1: "a1_val"]
    }

    def "can NOT HANDLE merging non object things"() {

        def root1 = mkRootERN([mkObjectERN("notnamespaced", "s", 4)])
        def root2 = mkRootERN([ExecutionResultNodeUtil.list("namespaced", [mkObjectERN("inner", "s", 4)])])
        def root3 = mkRootERN([ExecutionResultNodeUtil.list("namespaced", [mkObjectERN("outer", "t", 2)])])

        when:
        StrategyUtil.mergeTrees([root1, root2, root3])
        then:
        thrown(AssertException)
    }
}
