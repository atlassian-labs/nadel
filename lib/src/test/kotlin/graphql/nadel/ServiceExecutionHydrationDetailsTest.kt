package graphql.nadel

import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.test.mock
import graphql.schema.FieldCoordinates
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ServiceExecutionHydrationDetailsTest {
    @Test
    fun `generated copy retains only legacy singular consumer details`() {
        val details = newDetails(
            virtualField = FieldCoordinates.coordinates("Issue", "assignee"),
            fieldPath = listOf("issues", "assignee"),
        )
        val reporter = newDetails(
            virtualField = FieldCoordinates.coordinates("Issue", "reporter"),
            fieldPath = listOf("issues", "reporter"),
        )
        details.withConsumers(
            listOf(
                details.toConsumerDetails(),
                reporter.toConsumerDetails(),
            ),
        )

        assertEquals(2, details.consumerDetails.size)

        val copiedDetails = details.copy()

        assertEquals(
            listOf(details.toConsumerDetails()),
            copiedDetails.consumerDetails,
        )
    }

    private fun newDetails(
        virtualField: FieldCoordinates,
        fieldPath: List<String>,
    ): ServiceExecutionHydrationDetails {
        return ServiceExecutionHydrationDetails(
            instruction = mock<NadelGenericHydrationInstruction>(),
            timeout = 1_000,
            batchSize = 100,
            hydrationSourceService = mock<Service>(),
            hydrationVirtualField = virtualField,
            hydrationBackingField = FieldCoordinates.coordinates("Query", "usersByIds"),
            fieldPath = fieldPath,
        )
    }
}
