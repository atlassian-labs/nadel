package graphql.nadel.definition.coordinates

import kotlin.test.Test
import kotlin.test.assertTrue

class NadelSchemaMemberCoordinatesTest {
    @Test
    fun `compare level first`() {
        val inputObject = NadelInputObjectCoordinates("UserFilter")
        val field = inputObject.field("filter")

        val list = listOf(field, inputObject)

        // When
        val result = list.shuffled().sorted()

        // Then
        assertTrue(result == list.asReversed())
    }

    @Test
    fun `compares parents second`() {
        val list = listOf(
            NadelUnionCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("Account").field("cheese"),
            NadelObjectCoordinates("User").field("cheese"),
            NadelObjectCoordinates("User").field("cheese").argument("id"),
            NadelObjectCoordinates("Account").field("cheese").argument("id"),
            NadelObjectCoordinates("Account").field("cheese").argument("cheese"),
        )

        // Should basically be sorted alphabetically by level
        val expectedOrder = listOf(
            NadelObjectCoordinates("Account").field("cheese"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").field("cheese"),
            NadelUnionCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("Account").field("cheese").argument("cheese"),
            NadelObjectCoordinates("Account").field("cheese").argument("id"),
            NadelObjectCoordinates("User").field("cheese").argument("id"),
        )

        // When
        val result = list.shuffled().sorted()

        // Then
        assertTrue(result == expectedOrder)
    }

    @Test
    fun `compares kind third`() {
        val list = listOf(
            NadelDirectiveCoordinates("User"),
            NadelEnumCoordinates("User"),
            NadelInputObjectCoordinates("User"),
            NadelInterfaceCoordinates("User"),
            NadelObjectCoordinates("User"),
            NadelScalarCoordinates("User"),
            NadelUnionCoordinates("User"),
            NadelObjectCoordinates("User").field("beta"),
            NadelUnionCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelInterfaceCoordinates("User").field("beta"),
        )

        // Should basically be sorted alphabetically by level
        val expectedOrder = listOf(
            NadelDirectiveCoordinates("User"),
            NadelEnumCoordinates("User"),
            NadelInputObjectCoordinates("User"),
            NadelInterfaceCoordinates("User"),
            NadelObjectCoordinates("User"),
            NadelScalarCoordinates("User"),
            NadelUnionCoordinates("User"),
            NadelInterfaceCoordinates("User").field("beta"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").field("beta"),
            NadelUnionCoordinates("User").appliedDirective("beta"),
        )

        // When
        val result = list.sorted()

        // Then
        assertTrue(result == expectedOrder)
    }

    @Test
    fun `compares name fourth`() {
        val list = listOf(
            NadelUnionCoordinates("User"),
            NadelObjectCoordinates("User").field("beta"),
            NadelObjectCoordinates("User").appliedDirective("alpha"),
            NadelUnionCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").appliedDirective("gamma"),
            NadelScalarCoordinates("Account"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelInterfaceCoordinates("User").field("beta"),
            NadelInterfaceCoordinates("User").field("alpha"),
            NadelScalarCoordinates("User"),
        )

        // Should basically be sorted alphabetically by level
        val expectedOrder = listOf(
            NadelScalarCoordinates("Account"),
            NadelScalarCoordinates("User"),
            NadelUnionCoordinates("User"),
            NadelInterfaceCoordinates("User").field("alpha"),
            NadelInterfaceCoordinates("User").field("beta"),
            NadelObjectCoordinates("User").appliedDirective("alpha"),
            NadelObjectCoordinates("User").appliedDirective("beta"),
            NadelObjectCoordinates("User").appliedDirective("gamma"),
            NadelObjectCoordinates("User").field("beta"),
            NadelUnionCoordinates("User").appliedDirective("beta"),
        )

        // When
        val result = list.shuffled().sorted()

        // Then
        assertTrue(result == expectedOrder)
    }
}
