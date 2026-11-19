package graphql.nadel.validation.hydration

import graphql.nadel.NadelSchemas.Companion.newNadelSchemas
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.util.makeUnderlyingSchema
import graphql.nadel.validation.NadelSchemaValidation
import graphql.nadel.validation.NadelValidationTestFixture
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelHydrationVirtualTypeValidationTest {
    @Test
    fun `generates valid virtual type context`() {
        // Given
        val virtualFieldCoordinates = makeFieldCoordinates("Query", "virtualField")
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    type Query {
                        echo: String
                        virtualField: DataView
                            @hydrated(
                                field: "data"
                                arguments: [{name: "id", value: "1"}]
                            )
                    }
                    type DataView @virtualType {
                        id: ID
                        string: String
                        int: Int
                        other: OtherDataView
                    }
                    type OtherDataView @virtualType {
                        boolean: Boolean
                        data: DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string: String
                      bool: Boolean
                      int: Int!
                      other: OtherData
                    }
                    type OtherData {
                      boolean: Boolean!
                      data: Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val blueprintValidation = NadelSchemaValidation(
            newNadelSchemas()
                .overallSchemas(fixture.overallSchema)
                .underlyingSchemas(fixture.underlyingSchema)
                .stubServiceExecution()
                .build()
        ).validateAndGenerateBlueprint()

        // Then
        val fieldInstructions = blueprintValidation.fieldInstructions[virtualFieldCoordinates]
        val hydration = fieldInstructions!!.single() as NadelHydrationFieldInstruction

        val virtualTypeContext = hydration.virtualTypeContext
        assertTrue(virtualTypeContext != null)
        assertTrue(virtualTypeContext.virtualFieldContainer.name == virtualFieldCoordinates.typeName)
        assertTrue(virtualTypeContext.virtualField.name == virtualFieldCoordinates.fieldName)
        assertTrue(
            virtualTypeContext.virtualTypeToBackingType == mapOf(
                "DataView" to "Data",
                "OtherDataView" to "OtherData",
            ),
        )
        assertTrue(
            virtualTypeContext.backingTypeToVirtualType == mapOf(
                "Data" to "DataView",
                "OtherData" to "OtherDataView",
            ),
        )
    }

    private fun makeFixture(
        overallSchema: Map<String, String>,
        underlyingSchema: Map<String, String> = emptyMap(),
    ): NadelValidationTestFixture {
        return NadelValidationTestFixture(
            overallSchema = overallSchema,
            underlyingSchema = overallSchema
                .mapValues { (service, schema) ->
                    underlyingSchema[service] ?: makeUnderlyingSchema(schema)
                },
        )
    }
}
