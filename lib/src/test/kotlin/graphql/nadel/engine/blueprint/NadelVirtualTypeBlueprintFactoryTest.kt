package graphql.nadel.engine.blueprint

import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelVirtualTypeBlueprintFactoryTest {
    @Test
    fun `creates a mapping for one scalar`() {
        val schema = SchemaGenerator.createdMockedSchema(
            // language=GraphQL
            """
                directive @hydrated(field: String!) on FIELD_DEFINITION

                type Query {
                    virtualEcho: VirtualEcho @hydrated(field: "echo")
                    echo: BackingEcho
                }
                type VirtualEcho {
                    echo: String
                    info: VirtualEchoInfo
                }
                type VirtualEchoInfo {
                    cursor: String
                }
                type BackingEcho {
                    echo: String
                    info: BackingEchoInfo
                }
                type BackingEchoInfo {
                    cursor: String
                    something: Boolean
                }
            """.trimIndent()
        )

        val virtualTypeContext = NadelVirtualTypeBlueprintFactory()
            .makeVirtualTypeContext(
                schema,
                containerType = schema.queryType,
                virtualFieldDef = schema.queryType.getField("virtualEcho"),
            )

        assertTrue(virtualTypeContext != null)

        assertTrue(virtualTypeContext.virtualFieldContainer.name == "Query")
        assertTrue(virtualTypeContext.virtualField.name == "virtualEcho")

        assertTrue(
            virtualTypeContext.virtualTypeToBackingType == mapOf(
                "VirtualEcho" to "BackingEcho",
                "VirtualEchoInfo" to "BackingEchoInfo",
            ),
        )
        assertTrue(
            virtualTypeContext.backingTypeToVirtualType == mapOf(
                "BackingEcho" to "VirtualEcho",
                "BackingEchoInfo" to "VirtualEchoInfo",
            ),
        )
    }
}
