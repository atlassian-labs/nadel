package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLTypeVisitorStub
import graphql.schema.SchemaTransformer.transformSchema
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.none
import strikt.assertions.one

@UseHook
class `can-delete-fields-and-types` : EngineTestHook {
    override val schemaTransformationHook = SchemaTransformationHook { originalSchema, _ ->
        transformSchema(originalSchema, object : GraphQLTypeVisitorStub() {
            override fun visitGraphQLFieldDefinition(
                node: GraphQLFieldDefinition,
                context: TraverserContext<GraphQLSchemaElement>,
            ): TraversalControl {
                if (node.name == "foo") {
                    return deleteNode(context)
                }
                return super.visitGraphQLFieldDefinition(node, context)
            }

            override fun visitGraphQLObjectType(
                node: GraphQLObjectType,
                context: TraverserContext<GraphQLSchemaElement>,
            ): TraversalControl {
                if (node.name == "Foo") {
                    return deleteNode(context)
                }
                return super.visitGraphQLObjectType(node, context)
            }
        })
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result)
            .data
            .isNotNull()
            .assertJsonKeys()["__schema"]
            .isNotNull()
            .isAJsonMap()["types"]
            .isA<AnyList>()
            .none {
                isNotNull().isAJsonMap()["name"].isEqualTo("Foo")
            }
            .one {
                isNotNull().isAJsonMap()["name"].isEqualTo("String")
            }
            .one {
                isNotNull().isAJsonMap()["name"].isEqualTo("Query")
            }
    }
}
