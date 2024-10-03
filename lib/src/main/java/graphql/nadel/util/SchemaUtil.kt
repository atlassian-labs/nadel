package graphql.nadel.util

import graphql.language.SDLDefinition
import graphql.parser.Parser
import graphql.parser.ParserEnvironment
import graphql.parser.ParserOptions
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.Reader

internal object SchemaUtil {
    private val parser = Parser()
    private val schemaParser = SchemaParser()

    fun parseSchemaDefinitions(
        schema: Reader,
        maxTokens: Int = Int.MAX_VALUE,
        captureSourceLocation: Boolean = false,
    ): List<AnySDLDefinition> {
        return parser
            .parseDocument(
                ParserEnvironment.newParserEnvironment()
                    .document(schema)
                    .parserOptions(
                        ParserOptions.getDefaultSdlParserOptions()
                            .transform {
                                it
                                    .maxTokens(maxTokens)
                                    .captureSourceLocation(captureSourceLocation)
                            }
                    )
                    .build(),
            )
            .getDefinitionsOfType(SDLDefinition::class.java)
    }

    fun parseTypeDefinitionRegistry(
        schema: Reader,
        maxTokens: Int = Int.MAX_VALUE,
        captureSourceLocation: Boolean = false,
    ): TypeDefinitionRegistry {
        return schemaParser.parse(
            schema,
            ParserOptions.getDefaultSdlParserOptions()
                .transform {
                    it
                        .maxTokens(maxTokens)
                        .captureSourceLocation(captureSourceLocation)
                }
        )
    }
}
