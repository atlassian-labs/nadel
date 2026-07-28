package graphql.nadel.tests.next.fixtures.execution.servicetypefilter

import graphql.nadel.Nadel
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.util.NadelTransformAdapter
import graphql.normalized.ExecutableNormalizedField

/**
 * Verifies that service type filtering composes monotonically with a transform on an abstract parent field.
 *
 * Nadel plans every field's transform state before query transformations start. A parent transform can subsequently
 * narrow its child fields' [ExecutableNormalizedField.objectTypeNames] before Nadel recursively executes those
 * children's already-planned service type filters. The service filter must therefore intersect its planned
 * service-owned types with each child's current types. Replacing the current types with the planned set would
 * resurrect implementations deliberately removed by the parent and could expose their fields.
 *
 * The concrete tests cover both outcomes of that intersection: no service-owned types remain, so Nadel substitutes an
 * artificial `__typename`, or one service-owned type remains and must not be widened to its removed sibling.
 */
abstract class ServiceTypeFilterNarrowedSelectionTest(
    private val narrowedTypeNames: List<String>,
) : NadelIntegrationTest(
    query = """
        query {
          crossServiceItems {
            value
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "shared",
            overallSchema = """
                interface CrossServiceItem {
                  value: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = {},
        ),
        Service(
            name = "comments",
            overallSchema = """
                type CommentsItem implements CrossServiceItem {
                  value: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                interface CrossServiceItem {
                  value: String
                }
                type CommentsItem implements CrossServiceItem {
                  value: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("CrossServiceItem") { type ->
                    type.typeResolver { env ->
                        env.schema.getObjectType("CommentsItem")
                    }
                }
            },
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  crossServiceItems: [CrossServiceItem]
                }
                type AllowedIssuesItem implements CrossServiceItem {
                  value: String
                }
                type DeniedIssuesItem implements CrossServiceItem {
                  value: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  crossServiceItems: [CrossServiceItem]
                }
                interface CrossServiceItem {
                  value: String
                }
                type AllowedIssuesItem implements CrossServiceItem {
                  value: String
                }
                type DeniedIssuesItem implements CrossServiceItem {
                  value: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class AllowedIssuesItem(val value: String)
                data class DeniedIssuesItem(val value: String)

                wiring
                    .type("CrossServiceItem") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("crossServiceItems") {
                            listOf(
                                AllowedIssuesItem(value = "allowed issues data"),
                                DeniedIssuesItem(value = "denied issues data"),
                            )
                        }
                    }
            },
        ),
    ),
) {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .transforms(listOf(NarrowChildSelectionsTransform(narrowedTypeNames)))
    }
}

class ServiceTypeFilterDoesNotWidenNarrowedSelectionTest : ServiceTypeFilterNarrowedSelectionTest(
    narrowedTypeNames = listOf("CommentsItem"),
)

class ServiceTypeFilterDoesNotRestoreRemovedOwnedTypeTest : ServiceTypeFilterNarrowedSelectionTest(
    narrowedTypeNames = listOf("AllowedIssuesItem", "CommentsItem"),
)

/** Simulates a parent transform, such as AGG scope narrowing, restricting its child selections. */
private class NarrowChildSelectionsTransform(
    private val narrowedTypeNames: List<String>,
) : NadelTransformAdapter {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Unit? {
        return Unit.takeIf { overallField.name == "crossServiceItems" }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Unit,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): NadelTransformFieldResult {
        field.children.forEach { child ->
            check(
                child.objectTypeNames.toSet() ==
                    setOf("AllowedIssuesItem", "CommentsItem", "DeniedIssuesItem")
            )
            child.setObjectTypeNames(narrowedTypeNames)
        }
        return NadelTransformFieldResult.unmodified(field)
    }
}
