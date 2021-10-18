package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.NadelEngineType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLTypeVisitorStub
import graphql.schema.SchemaTransformer
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@UseHook
class `schema-transformation-is-applied` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.schemaTransformationHook { originalSchema, services ->
            expectThat(services).single().get { name }.isEqualTo("MyService")

            SchemaTransformer.transformSchema(originalSchema, object : GraphQLTypeVisitorStub() {
                override fun visitGraphQLFieldDefinition(
                    node: GraphQLFieldDefinition,
                    context: TraverserContext<GraphQLSchemaElement>,
                ): TraversalControl {
                    if ((context.parentNode as GraphQLObjectType).name == "World" && node.name == "name") {
                        return changeNode(
                            context,
                            node.transform { builder ->
                                builder.name("nameChanged")
                            },
                        )
                    }

                    return TraversalControl.CONTINUE
                }
            })
        }
    }
}
