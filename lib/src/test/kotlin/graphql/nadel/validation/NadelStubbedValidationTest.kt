package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelStubbedValidationTest {
    private val source = "$" + "source"

    @Test
    fun `can stub object type`() {
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
    fun `can stub union type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        location: Location
                    }
                    union Location @stubbed = Address | Street
                    type Address {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
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
                    type Address {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
                        street: String!
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
    fun `stubbed union type can use stubbed type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        location: Location
                    }
                    union Location @stubbed = Address | Street
                    type Address @stubbed {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
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
                    type Street {
                        street: String!
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
    fun `real union type can not use stubbed type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        location: Location
                    }
                    union Location = Address | Street
                    type Address @stubbed {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
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
                    union Location = Street
                    type Street {
                        street: String!
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelUnionMustNotReferenceStubbedObjectTypeError>()
        assertTrue(error.union.overall.name == "Location")
        assertTrue(error.stubbedObjectType.name == "Address")
    }

    @Test
    fun `real union type can not use stubbed type even if it exists`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        location: Location
                    }
                    union Location = Address | Street
                    type Address @stubbed {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
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
                    union Location = Address | Street
                    type Address {
                        number: Int
                        street: String
                        suburb: String
                    }
                    type Street {
                        street: String!
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelUnionMustNotReferenceStubbedObjectTypeError>()
        assertTrue(error.union.overall.name == "Location")
        assertTrue(error.stubbedObjectType.name == "Address")
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

    @Test
    fun `stubbed fields on interface type must also be stubbed on implementing types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        address: Address @stubbed
                    }
                    type Human implements Person {
                        address: Address @stubbed
                    }
                    type Monkey implements Person {
                        address: Address @stubbed
                    }
                    type Address {
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        name: String
                    }
                    type Human implements Person {
                        name: String
                    }
                    type Monkey implements Person {
                        name: String
                    }
                    type Address {
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
    fun `error if stubbed interface field is not stubbed on an implementation`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        address: Address @stubbed
                    }
                    type Human implements Person {
                        address: Address
                    }
                    type Monkey implements Person {
                        address: Address @stubbed
                    }
                    type Address {
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        name: String
                    }
                    type Human implements Person {
                        name: String
                        address: Address
                    }
                    type Monkey implements Person {
                        name: String
                    }
                    type Address {
                        street: String
                    }
                """.trimIndent(),
            )
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedMissingOnConcreteTypeError>()
        assertTrue(error.interfaceType.overall.name == "Person")
        assertTrue(error.objectType.name == "Human")
        assertTrue(error.objectField.name == "address")
    }

    @Test
    fun `stubbed type can be used as output for interface field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        address: Address
                    }
                    type Human implements Person {
                        address: Address
                    }
                    type Monkey implements Person {
                        address: Address
                    }
                    type Address @stubbed {
                        street: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    interface Person {
                        name: String
                    }
                    type Human implements Person {
                        name: String
                    }
                    type Monkey implements Person {
                        name: String
                    }
                    type Address {
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
    fun `can stub input types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        stub(input: StubInput): StubPayload
                    }
                    input StubInput @stubbed {
                        field: String
                    }
                    type StubPayload @stubbed {
                        success: Boolean
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        _nothing: String
                    }
                """.trimIndent(),
            )
        )

        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `stubbed fields can use not-stubbed input types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        stub(input: ExistingInput): StubPayload
                    }
                    input ExistingInput {
                        field: String
                    }
                    type StubPayload @stubbed {
                        success: Boolean
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        _nothing(input: ExistingInput): String
                    }
                    input ExistingInput {
                        field: String
                    }
                """.trimIndent(),
            )
        )

        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `non stubbed fields cannot use stubbed input types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        stub(input: StubInput!): StubPayload
                    }
                    input StubInput @stubbed {
                        field: String
                    }
                    type StubPayload {
                        success: Boolean
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Mutation {
                        stub(input: StubInput!): StubPayload
                    }
                    input StubInput {
                        field: String
                    }
                    type StubPayload {
                        success: Boolean
                    }
                """.trimIndent(),
            )
        )

        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelStubbedInputTypeUsedByNotStubbedFieldError>()
        assertTrue(error.parent.overall.name == "Mutation")
        assertTrue(error.field.name == "stub")
        assertTrue(error.stubbedInputType.name == "StubInput")
    }

    @Test
    fun `can stub enum type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        name: String
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
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
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `can reference stubbed enum type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(id: ID!): Person
                    }
                    type Person {
                        alphabet: Alphabet
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
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
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `cannot use stubbed enum as input in non stubbed field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(alphabet: Alphabet): Person
                    }
                    type Person {
                        name: String
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
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
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<MissingArgumentOnUnderlying>()
        assertTrue(error.overallField.name == "person")
        assertTrue(error.argument.name == "alphabet")
    }

    @Test
    fun `can use stubbed enum as input on stubbed field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                        person(alphabet: Alphabet): Person @stubbed
                    }
                    type Person {
                        name: String
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `can use stubbed enum in stubbed input type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                        person(filter: PersonFilter): Person @stubbed
                    }
                    type Person {
                        name: String
                    }
                    input PersonFilter @stubbed {
                        alphabet: Alphabet
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                    type Person {
                        name: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `cannot use stubbed enum in non stubbed input type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person(filter: PersonFilter): Person
                    }
                    type Person {
                        name: String
                    }
                    input PersonFilter {
                        alphabet: Alphabet
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        person(filter: PersonFilter): Person
                    }
                    type Person {
                        name: String
                    }
                    input PersonFilter {
                        secret: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())

        val error = errors.assertSingleOfType<NadelSchemaValidationError.MissingUnderlyingInputField>()
        assertTrue(error.parentType.overall.name == "PersonFilter")
        assertTrue(error.overallField.name == "alphabet")
    }

    @Test
    fun `can use stubbed enum in stubbed type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to """
                    type Query {
                        person: Person
                    }
                    type Person @stubbed {
                        id: ID!
                        alphabet: Alphabet
                    }
                    enum Alphabet @stubbed {
                        Abc
                        Def
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "jira" to """
                    type Query {
                        echo: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }
}
