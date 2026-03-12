package graphql.nadel.engine.blueprint

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.UnionTypeDefinition
import graphql.nadel.definition.coordinates.NadelDirectiveCoordinates
import graphql.nadel.definition.coordinates.NadelEnumCoordinates
import graphql.nadel.definition.coordinates.NadelInputObjectCoordinates
import graphql.nadel.definition.coordinates.NadelInterfaceCoordinates
import graphql.nadel.definition.coordinates.NadelObjectCoordinates
import graphql.nadel.definition.coordinates.NadelScalarCoordinates
import graphql.nadel.definition.coordinates.NadelUnionCoordinates
import graphql.parser.Parser
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelSchemaDefinitionTraverserElementTest {
    @Test
    fun `ObjectType coordinates returns correct NadelObjectCoordinates`() {
        val document = Parser().parseDocument(
            """
                type User {
                    id: ID!
                    name: String
                }
            """.trimIndent()
        )

        val objectTypeDefinition = document.definitions[0] as ObjectTypeDefinition
        val objectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ObjectType(objectTypeDefinition)

        // When
        val coordinates = objectTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelObjectCoordinates("User"))
    }

    @Test
    fun `InterfaceType coordinates returns correct NadelInterfaceCoordinates`() {
        val document = Parser().parseDocument(
            """
                interface Node {
                    id: ID!
                    title: String
                }
            """.trimIndent()
        )

        val interfaceTypeDefinition = document.definitions[0] as InterfaceTypeDefinition
        val interfaceTypeTraverserElement = NadelSchemaDefinitionTraverserElement.InterfaceType(interfaceTypeDefinition)

        // When
        val coordinates = interfaceTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelInterfaceCoordinates("Node"))
    }

    @Test
    fun `UnionType coordinates returns correct NadelUnionCoordinates`() {
        val document = Parser().parseDocument(
            """
                union SearchResult = User | Post | Comment
            """.trimIndent()
        )

        val unionTypeDefinition = document.definitions[0] as UnionTypeDefinition
        val unionTypeTraverserElement = NadelSchemaDefinitionTraverserElement.UnionType(unionTypeDefinition)

        // When
        val coordinates = unionTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelUnionCoordinates("SearchResult"))
    }

    @Test
    fun `EnumType coordinates returns correct NadelEnumCoordinates`() {
        val document = Parser().parseDocument(
            """
                enum Status {
                    ACTIVE
                    INACTIVE
                    PENDING
                }
            """.trimIndent()
        )

        val enumTypeDefinition = document.definitions[0] as EnumTypeDefinition
        val enumTypeTraverserElement = NadelSchemaDefinitionTraverserElement.EnumType(enumTypeDefinition)

        // When
        val coordinates = enumTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelEnumCoordinates("Status"))
    }

    @Test
    fun `EnumValueDefinition coordinates returns correct NadelEnumValueCoordinates`() {
        val document = Parser().parseDocument(
            """
                enum Status {
                    ACTIVE
                    INACTIVE
                    PENDING
                }
            """.trimIndent()
        )

        val enumTypeDefinition = document.definitions[0] as EnumTypeDefinition
        val enumTypeTraverserElement = NadelSchemaDefinitionTraverserElement.EnumType(enumTypeDefinition)
        val enumValueDefinition = enumTypeDefinition.enumValueDefinitions[0]
        val enumValueTraverserElement = NadelSchemaDefinitionTraverserElement.EnumValueDefinition(enumTypeTraverserElement, enumValueDefinition)

        // When
        val coordinates = enumValueTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelEnumCoordinates("Status").enumValue("ACTIVE"))
    }

    @Test
    fun `InputObjectType coordinates returns correct NadelInputObjectCoordinates`() {
        val document = Parser().parseDocument(
            """
                input UserInput {
                    name: String
                    email: String
                }
            """.trimIndent()
        )

        val inputObjectTypeDefinition = document.definitions[0] as InputObjectTypeDefinition
        val inputObjectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.InputObjectType(inputObjectTypeDefinition)

        // When
        val coordinates = inputObjectTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelInputObjectCoordinates("UserInput"))
    }

    @Test
    fun `InputObjectField coordinates returns correct NadelInputObjectFieldCoordinates`() {
        val document = Parser().parseDocument(
            """
                input UserInput {
                    name: String
                    email: String
                }
            """.trimIndent()
        )

        val inputObjectTypeDefinition = document.definitions[0] as InputObjectTypeDefinition
        val inputObjectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.InputObjectType(inputObjectTypeDefinition)
        val inputObjectFieldDefinition = inputObjectTypeDefinition.inputValueDefinitions[0]
        val inputObjectFieldTraverserElement = NadelSchemaDefinitionTraverserElement.InputObjectField(inputObjectTypeTraverserElement, inputObjectFieldDefinition)

        // When
        val coordinates = inputObjectFieldTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelInputObjectCoordinates("UserInput").field("name"))
    }

    @Test
    fun `ScalarType coordinates returns correct NadelScalarCoordinates`() {
        val document = Parser().parseDocument(
            """
                scalar DateTime
            """.trimIndent()
        )

        val scalarTypeDefinition = document.definitions[0] as ScalarTypeDefinition
        val scalarTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ScalarType(scalarTypeDefinition)

        // When
        val coordinates = scalarTypeTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelScalarCoordinates("DateTime"))
    }

    @Test
    fun `Directive coordinates returns correct NadelDirectiveCoordinates`() {
        val document = Parser().parseDocument(
            """
                directive @auth(requires: String!) on FIELD_DEFINITION
            """.trimIndent()
        )

        val directiveDefinition = document.definitions[0] as graphql.language.DirectiveDefinition
        val directiveTraverserElement = NadelSchemaDefinitionTraverserElement.Directive(directiveDefinition)

        // When
        val coordinates = directiveTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelDirectiveCoordinates("auth"))
    }

    @Test
    fun `FieldDefinition coordinates returns correct NadelFieldCoordinates for ObjectType`() {
        val document = Parser().parseDocument(
            """
                type User {
                    id: ID!
                    name: String
                    email: String
                }
            """.trimIndent()
        )

        val objectTypeDefinition = document.definitions[0] as ObjectTypeDefinition
        val objectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ObjectType(objectTypeDefinition)
        val nameFieldDefinition = objectTypeDefinition.fieldDefinitions[1]
        val nameFieldTraverserElement = NadelSchemaDefinitionTraverserElement.FieldDefinition(objectTypeTraverserElement, nameFieldDefinition)

        // When
        val coordinates = nameFieldTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelObjectCoordinates("User").field("name"))
    }

    @Test
    fun `FieldDefinition coordinates returns correct NadelFieldCoordinates for InterfaceType`() {
        val document = Parser().parseDocument(
            """
                interface Node {
                    id: ID!
                    title: String
                }
            """.trimIndent()
        )

        val interfaceTypeDefinition = document.definitions[0] as InterfaceTypeDefinition
        val interfaceTypeTraverserElement = NadelSchemaDefinitionTraverserElement.InterfaceType(interfaceTypeDefinition)
        val idFieldDefinition = interfaceTypeDefinition.fieldDefinitions[0]
        val idFieldTraverserElement = NadelSchemaDefinitionTraverserElement.FieldDefinition(interfaceTypeTraverserElement, idFieldDefinition)

        // When
        val coordinates = idFieldTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelInterfaceCoordinates("Node").field("id"))
    }

    @Test
    fun `FieldArgument coordinates returns correct NadelArgumentCoordinates`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    user(id: ID!, limit: Int): User
                }
            """.trimIndent()
        )

        val queryTypeDefinition = document.definitions[0] as ObjectTypeDefinition
        val queryTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ObjectType(queryTypeDefinition)
        val userFieldDefinition = queryTypeDefinition.fieldDefinitions[0]
        val userFieldTraverserElement = NadelSchemaDefinitionTraverserElement.FieldDefinition(queryTypeTraverserElement, userFieldDefinition)
        val idArgDefinition = userFieldDefinition.inputValueDefinitions[0]
        val idArgTraverserElement = NadelSchemaDefinitionTraverserElement.FieldArgument(userFieldTraverserElement, idArgDefinition)

        // When
        val coordinates = idArgTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelObjectCoordinates("Query").field("user").argument("id"))
    }

    @Test
    fun `DirectiveArgument coordinates returns correct NadelArgumentCoordinates`() {
        val document = Parser().parseDocument(
            """
                directive @auth(requires: String!, scope: String) on FIELD_DEFINITION
            """.trimIndent()
        )

        val directiveDefinition = document.definitions[0] as graphql.language.DirectiveDefinition
        val directiveTraverserElement = NadelSchemaDefinitionTraverserElement.Directive(directiveDefinition)
        val requiresArgDefinition = directiveDefinition.inputValueDefinitions[0]
        val requiresArgTraverserElement = NadelSchemaDefinitionTraverserElement.DirectiveArgument(directiveTraverserElement, requiresArgDefinition)

        // When
        val coordinates = requiresArgTraverserElement.coordinates()

        // Then
        assertTrue(coordinates == NadelDirectiveCoordinates("auth").argument("requires"))
    }

    @Test
    fun `multiple enum value coordinates are correct`() {
        val document = Parser().parseDocument(
            """
                enum Status {
                    ACTIVE
                    INACTIVE
                    PENDING
                }
            """.trimIndent()
        )

        val enumTypeDefinition = document.definitions[0] as EnumTypeDefinition
        val enumTypeTraverserElement = NadelSchemaDefinitionTraverserElement.EnumType(enumTypeDefinition)
        val enumTypeCoords = NadelEnumCoordinates("Status")

        enumTypeDefinition.enumValueDefinitions.forEach { enumValueDefinition ->
            val enumValueTraverserElement = NadelSchemaDefinitionTraverserElement.EnumValueDefinition(enumTypeTraverserElement, enumValueDefinition)

            // When
            val coordinates = enumValueTraverserElement.coordinates()

            // Then
            assertTrue(coordinates == enumTypeCoords.enumValue(enumValueDefinition.name))
        }
    }

    @Test
    fun `multiple input object field coordinates are correct`() {
        val document = Parser().parseDocument(
            """
                input CreateUserInput {
                    name: String!
                    email: String!
                    age: Int
                }
            """.trimIndent()
        )

        val inputObjectTypeDefinition = document.definitions[0] as InputObjectTypeDefinition
        val inputObjectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.InputObjectType(inputObjectTypeDefinition)
        val inputTypeCoords = NadelInputObjectCoordinates("CreateUserInput")

        inputObjectTypeDefinition.inputValueDefinitions.forEach { inputObjectFieldDefinition ->
            val inputObjectFieldTraverserElement = NadelSchemaDefinitionTraverserElement.InputObjectField(inputObjectTypeTraverserElement, inputObjectFieldDefinition)

            // When
            val coordinates = inputObjectFieldTraverserElement.coordinates()

            // Then
            assertTrue(coordinates == inputTypeCoords.field(inputObjectFieldDefinition.name))
        }
    }

    @Test
    fun `multiple field coordinates are correct`() {
        val document = Parser().parseDocument(
            """
                type User {
                    id: ID!
                    name: String
                    email: String
                    age: Int
                }
            """.trimIndent()
        )

        val objectTypeDefinition = document.definitions[0] as ObjectTypeDefinition
        val objectTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ObjectType(objectTypeDefinition)
        val objectTypeCoords = NadelObjectCoordinates("User")

        objectTypeDefinition.fieldDefinitions.forEach { fieldDefinition ->
            val fieldTraverserElement = NadelSchemaDefinitionTraverserElement.FieldDefinition(objectTypeTraverserElement, fieldDefinition)

            // When
            val coordinates = fieldTraverserElement.coordinates()

            // Then
            assertTrue(coordinates == objectTypeCoords.field(fieldDefinition.name))
        }
    }

    @Test
    fun `multiple field argument coordinates are correct`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    users(first: Int, after: String, filter: String): [User]
                }
            """.trimIndent()
        )

        val queryTypeDefinition = document.definitions[0] as ObjectTypeDefinition
        val queryTypeTraverserElement = NadelSchemaDefinitionTraverserElement.ObjectType(queryTypeDefinition)
        val usersFieldDefinition = queryTypeDefinition.fieldDefinitions[0]
        val usersFieldTraverserElement = NadelSchemaDefinitionTraverserElement.FieldDefinition(queryTypeTraverserElement, usersFieldDefinition)
        val queryUsersFieldCoords = NadelObjectCoordinates("Query").field("users")

        usersFieldDefinition.inputValueDefinitions.forEach { argDefinition ->
            val argTraverserElement = NadelSchemaDefinitionTraverserElement.FieldArgument(usersFieldTraverserElement, argDefinition)

            // When
            val coordinates = argTraverserElement.coordinates()

            // Then
            assertTrue(coordinates == queryUsersFieldCoords.argument(argDefinition.name))
        }
    }
}
