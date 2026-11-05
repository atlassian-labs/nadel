package graphql.nadel.util

import graphql.schema.FieldCoordinates
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NadelFieldMapTest {
    @Test
    fun `from keeps colliding coordinates separate`() {
        // Given
        val milestone = FieldCoordinates.coordinates("MercuryMilestone", "linkedFocusAreas")
        val focusArea = FieldCoordinates.coordinates("MercuryFocusArea", "linkedMilestones")

        // When
        val map = NadelFieldMap.from(
            values = listOf(milestone, focusArea),
            getCoordinates = { it },
        )

        // Then
        assertEquals(milestone, map[milestone])
        assertEquals(focusArea, map[focusArea])
        assertEquals(milestone, map.get("MercuryMilestone", "linkedFocusAreas"))
        assertEquals(focusArea, map.get("MercuryFocusArea", "linkedMilestones"))
        assertEquals(2, map.size)
        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
    }

    @Test
    fun `from overwrites only matching coordinates`() {
        // Given
        val milestone = FieldCoordinates.coordinates("MercuryMilestone", "linkedFocusAreas")
        val focusArea = FieldCoordinates.coordinates("MercuryFocusArea", "linkedMilestones")
        val values = listOf(milestone to "old", focusArea to "hydration", milestone to "new")

        // When
        val map = NadelFieldMap.from(
            values = values,
            getCoordinates = { it.first },
            getValue = { it.second },
        )

        // Then
        assertEquals("new", map[milestone])
        assertEquals("hydration", map[focusArea])
        assertEquals(2, map.size)
    }

    @Test
    fun `from counts fields across types`() {
        // Given
        val values = listOf(
            FieldCoordinates.coordinates("MercuryMilestone", "id"),
            FieldCoordinates.coordinates("MercuryMilestone", "linkedFocusAreas"),
            FieldCoordinates.coordinates("MercuryFocusArea", "id"),
        )

        // When
        val map = NadelFieldMap.from(
            values = values,
            getTypeName = { it.typeName },
            getFieldName = { it.fieldName },
            getValue = { "${it.typeName}.${it.fieldName}" },
        )

        // Then
        assertEquals("MercuryMilestone.id", map.get("MercuryMilestone", "id"))
        assertEquals("MercuryMilestone.linkedFocusAreas", map.get("MercuryMilestone", "linkedFocusAreas"))
        assertEquals("MercuryFocusArea.id", map.get("MercuryFocusArea", "id"))
        assertEquals(3, map.size)
    }

    @Test
    fun `from returns null for absent colliding coordinates`() {
        // Given
        val focusArea = FieldCoordinates.coordinates("MercuryFocusArea", "linkedMilestones")

        // When
        val map = NadelFieldMap.from(
            values = listOf(focusArea),
            getTypeName = { it.typeName },
            getFieldName = { it.fieldName },
        )

        // Then
        assertNull(map.get("MercuryMilestone", "linkedFocusAreas"))
        assertNull(map.get("MercuryFocusArea", "id"))
        assertNull(map.get("UnknownType", "linkedMilestones"))
        assertEquals(focusArea, map[focusArea])
        assertEquals(1, map.size)
    }

    @Test
    fun `groupBy keeps colliding coordinates separate and preserves value order`() {
        // Given
        val milestone = FieldCoordinates.coordinates("MercuryMilestone", "linkedFocusAreas")
        val focusArea = FieldCoordinates.coordinates("MercuryFocusArea", "linkedMilestones")
        val values = listOf(milestone to "first", focusArea to "hydration", milestone to "second")

        // When
        val map = NadelFieldMap.groupBy(
            values = values,
            getCoordinates = { it.first },
            getValue = { it.second },
        )

        // Then
        assertEquals(listOf("first", "second"), map[milestone])
        assertEquals(listOf("hydration"), map[focusArea])
        assertEquals(2, map.size)
        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
    }

    @Test
    fun `groupBy counts fields across types`() {
        // Given
        val milestoneId = FieldCoordinates.coordinates("MercuryMilestone", "id")
        val linkedFocusAreas = FieldCoordinates.coordinates("MercuryMilestone", "linkedFocusAreas")
        val focusAreaId = FieldCoordinates.coordinates("MercuryFocusArea", "id")

        // When
        val map = NadelFieldMap.groupBy(
            values = listOf(milestoneId, linkedFocusAreas, milestoneId, focusAreaId),
            getTypeName = { it.typeName },
            getFieldName = { it.fieldName },
        )

        // Then
        assertEquals(listOf(milestoneId, milestoneId), map[milestoneId])
        assertEquals(listOf(linkedFocusAreas), map[linkedFocusAreas])
        assertEquals(listOf(focusAreaId), map[focusAreaId])
        assertEquals(3, map.size)
    }

    @Test
    fun `groupBy returns null for absent colliding coordinates`() {
        // Given
        val focusArea = FieldCoordinates.coordinates("MercuryFocusArea", "linkedMilestones")

        // When
        val map = NadelFieldMap.groupBy(
            values = listOf(focusArea),
            getCoordinates = { it },
        )

        // Then
        assertNull(map.get("MercuryMilestone", "linkedFocusAreas"))
        assertNull(map.get("MercuryFocusArea", "id"))
        assertNull(map.get("UnknownType", "linkedMilestones"))
        assertEquals(listOf(focusArea), map[focusArea])
        assertEquals(1, map.size)
    }

    @Test
    fun `empty maps have no fields`() {
        // Given
        val values = emptyList<FieldCoordinates>()

        // When
        val maps = listOf(
            NadelFieldMap.emptyMap<String>(),
            NadelFieldMap.from(values, getCoordinates = { it }),
            NadelFieldMap.groupBy(values, getCoordinates = { it }),
        )

        // Then
        maps.forEach { map ->
            assertEquals(0, map.size)
            assertTrue(map.isEmpty())
            assertFalse(map.isNotEmpty())
            assertNull(map.get("MercuryMilestone", "linkedFocusAreas"))
        }
    }
}
