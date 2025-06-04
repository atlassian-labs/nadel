package graphql.nadel.definition.coordinates

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelSchemaMemberCoordinatesTest {
    @Test
    fun `can get parents`() {
        val coords = NadelObjectCoordinates("UserFilter").field("test").appliedDirective("hello").argument("wow")

        // When
        val parentNames = coords.parents.map { it.name }.toList()

        // Then
        assertTrue(parentNames == listOf("hello", "test", "UserFilter"))
    }

    @TestFactory
    fun toHumanReadableString(): List<DynamicTest> {
        data class TestCase(
            val coordinates: NadelSchemaMemberCoordinates,
            val expectedToString: String,
        )

        fun execute(testCase: TestCase) {
            // When
            val actual = NadelSchemaMemberCoordinates.toHumanReadableString(testCase.coordinates)

            // Then
            assertTrue(actual == testCase.expectedToString)
        }

        return listOf(
            TestCase(
                coordinates = NadelObjectCoordinates("UserFilter")
                    .field("test")
                    .appliedDirective("hello")
                    .argument("wow"),
                expectedToString = "UserFilter (Object).test (Field).hello (AppliedDirective).wow (AppliedDirectiveArgument)",
            ),
            TestCase(
                NadelUnionCoordinates("User").appliedDirective("beta"),
                "User (Union).beta (AppliedDirective)",
            ),
            TestCase(
                NadelObjectCoordinates("User").appliedDirective("beta"),
                "User (Object).beta (AppliedDirective)",
            ),
            TestCase(
                NadelObjectCoordinates("Account").field("cheese"),
                "Account (Object).cheese (Field)",
            ),
            TestCase(
                NadelObjectCoordinates("User").field("cheese"),
                "User (Object).cheese (Field)",
            ),
            TestCase(
                NadelObjectCoordinates("User").field("cheese").argument("id"),
                "User (Object).cheese (Field).id (Argument)",
            ),
            TestCase(
                NadelObjectCoordinates("Account").field("cheese").argument("id"),
                "Account (Object).cheese (Field).id (Argument)",
            ),
            TestCase(
                NadelObjectCoordinates("Account").field("cheese").argument("cheese"),
                "Account (Object).cheese (Field).cheese (Argument)",
            ),
        ).map {
            DynamicTest.dynamicTest(it.toString()) {
                execute(it)
            }
        }
    }

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
