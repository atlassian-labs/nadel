package graphql.nadel.engine

import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.nextgen.result.ResolvedValue
import graphql.introspection.Introspection
import graphql.nadel.result.ExecutionResultNode
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import java.util.function.Consumer

class ResolvedValueMapperTest extends Specification {
    def mapper = new ResolvedValueMapper()

    def "changes __typename field value accordingly"(String given, String expected) {
        given:
        def mapping = ["Underlying": "Exposed", "A": "B", "C": "D"]
        def unapplyEnvironment = new UnapplyEnvironment(Mock(ExecutionStepInfo), false, false, mapping, Mock(GraphQLSchema))
        def node = newNode { builder -> builder.completedValue(given) }

        when:
        def mapped = mapper.mapResolvedValue(node, unapplyEnvironment)

        then:
        mapped.completedValue == expected

        where:
        given        | expected
        "Underlying" | "Exposed"
        "A"          | "B"
        "B"          | "B"
        "C"          | "D"
        "D"          | "D"
        "Unmapped"   | "Unmapped"
    }

    def "leaves non __typename fields alone"() {
        given:
        def mapping = ["Underlying": "Exposed", "A": "B", "C": "D"]
        def unapplyEnvironment = new UnapplyEnvironment(Mock(ExecutionStepInfo), false, false, mapping, Mock(GraphQLSchema))
        def node = newNode({ builder -> builder.completedValue("Underlying") }, "__someField")

        when:
        def mapped = mapper.mapResolvedValue(node, unapplyEnvironment)

        then:
        mapped.is(node.resolvedValue)
        mapped.completedValue == "Underlying"
    }

    def newNode(Consumer<ResolvedValue.Builder> valueBuilder, String name = Introspection.TypeNameMetaFieldDef.name) {
        def node = Mock(ExecutionResultNode)
        def field = Mock(MergedField)
        def value = ResolvedValue.newResolvedValue().with({ valueBuilder.accept(it); it }).build()

        (1.._) * node.resolvedValue >> value
        (1.._) * node.mergedField >> field
        (1.._) * field.name >> name

        return node
    }
}
