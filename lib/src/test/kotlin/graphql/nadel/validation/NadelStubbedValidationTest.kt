package graphql.nadel.validation

import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelStubbedValidationTest {
    private val source = "$" + "source"

    @Test
    fun `can stub type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        address: Address
                    }
                    type Address @stubbed {
                        street: String!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `can apply stubbed on type and field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        address: Address @stubbed
                    }
                    type Address @stubbed {
                        street: String!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `stubbed type must not implement any interfaces`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Location {
                        coordinates: String
                    }
                    type Person {
                        address: Address
                    }
                    type Address implements Location @stubbed {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Location {
                        coordinates: String
                    }
                    type GeoLocation implements Location {
                        coordinates: String
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedTypeMustNotImplementError>()
        assertTrue(error.type.overall.name == "Address")
    }

    @Test
    fun `stubbed field output type can implement interfaces`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Location {
                        coordinates: String
                    }
                    type Person {
                        address: Address @stubbed
                    }
                    type Address implements Location {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Location {
                        coordinates: String
                    }
                    type GeoLocation implements Location {
                        coordinates: String
                    }
                    type Person {
                        name: String
                    }
                    type Address implements Location {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `field using stubbed type must be nullable`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        address: Address!
                    }
                    type Address @stubbed {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMustBeNullableError>()
        assertTrue(error.parent.overall.name == "Person")
        assertTrue(error.field.name == "address")
    }

    @Test
    fun `renamed field cannot use stubbed type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        address: Address! @renamed(from: "wtf")
                    }
                    type Address @stubbed {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMustBeUsedExclusively>()
        assertTrue(error.parent.overall.name == "Person")
        assertTrue(error.field.name == "address")
    }

    @Test
    fun `stubbed field cannot be renamed`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        address: Address! @renamed(from: "wtf") @stubbed
                    }
                    type Address {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMustBeUsedExclusively>()
        assertTrue(error.parent.overall.name == "Person")
        assertTrue(error.field.name == "address")
    }

    @Test
    fun `hydrated field cannot use stubbed type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                        address(id: ID!): Address
                    }
                    type Person {
                        id: ID!
                        address: Address!
                            @hydrated(
                                field: "address"
                                arguments: [{ name: "id", value: "$source.id" }]
                            )
                    }
                    type Address @stubbed {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        id: ID!
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMustBeUsedExclusively>()
        assertTrue(error.parent.overall.name == "Person")
        assertTrue(error.field.name == "address")
    }

    @Test
    fun `stubbed field cannot be hydrated`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                        address(id: ID!): Address
                    }
                    type Person {
                        id: ID!
                        address: Address!
                            @stubbed
                            @hydrated(
                                field: "address"
                                arguments: [{ name: "id", value: "$source.id" }]
                            )
                    }
                    type Address {
                        coordinates: String
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMustBeUsedExclusively>()
        assertTrue(error.parent.overall.name == "Person")
        assertTrue(error.field.name == "address")
    }

    @Test
    fun `can stub root level field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                        address(id: ID!): String @stubbed
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }
    @Test
    fun `stubbed type can be used as output type for root level field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person @stubbed {
                        name: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }
}
