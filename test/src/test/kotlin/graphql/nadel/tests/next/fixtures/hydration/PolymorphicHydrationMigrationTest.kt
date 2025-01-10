package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField

class PolymorphicHydrationMigrationTest : NadelIntegrationTest(
    query = """
        {
          nothing: reference {
            object {
              __typename
            }
          }
          old_query: reference {
            object {
              __typename
              ... on OldObject {
                id
              }
            }
          }
          new_query: reference {
            object {
              __typename
              ... on NewObject {
                id
              }
            }
          }
          both_query: reference {
            object {
              __typename
              ... on OldObject {
                id
              }
              ... on NewObject {
                id
              }
            }
          }
          migrate_off: reference {
            object {
              __typename
              ... on OldObject {
                id
              }
              ... on NewObject @include(if: false) {
                id
              }
            }
          }
          migrate_on: reference {
            object {
              __typename
              ... on OldObject {
                id
              }
              ... on NewObject @include(if: true) {
                id
              }
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  reference: Reference
                  oldObjects(ids: [ID!]!): [OldObject]
                  newObjects(ids: [ID!]!): [NewObject]
                }
                union Object = OldObject | NewObject
                type Reference {
                  objectId: ID!
                  object: Object
                    @idHydrated(idField: "objectId")
                }
                type NewObject @defaultHydration(field: "newObjects", idArgument: "ids") {
                  id: ID!
                }
                type OldObject @defaultHydration(field: "oldObjects", idArgument: "ids") {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Reference(val objectId: String)
                data class NewObject(val id: String)
                data class OldObject(val id: String)

                val newObjectsById = listOf(
                    NewObject("ari:cloud:owner::type/1")
                ).associateBy { it.id }
                val oldObjectsById = listOf(
                    OldObject("ari:cloud:owner::type/1")
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("reference") { env ->
                                Reference(objectId = "ari:cloud:owner::type/1")
                            }
                            .dataFetcher("oldObjects") { env ->
                                env.getArgument<List<String>>("ids")?.map(oldObjectsById::get)
                            }
                            .dataFetcher("newObjects") { env ->
                                env.getArgument<List<String>>("ids")?.map(newObjectsById::get)
                            }
                    }
                    .type("Object") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any?>().javaClass.simpleName)
                        }
                    }
            },
        ),
    ),
) {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        virtualField: ExecutableNormalizedField,
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        val hasNewObject = virtualField.children.any { child ->
                            child.objectTypeNames.size == 1
                                && child.objectTypeNames.first() == "NewObject"
                                && !child.fieldName.startsWith("__")
                        }
                        val hasOldObject = virtualField.children.any { child ->
                            child.objectTypeNames.contains("OldObject") && !child.fieldName.startsWith("__")
                        }

                        return if (hasNewObject || !hasOldObject) {
                            instructions.first {
                                it.backingFieldDef.name == "newObjects"
                            }
                        } else {
                            instructions.first {
                                it.backingFieldDef.name == "oldObjects"
                            }
                        }
                    }
                }
            )
    }
}
