package graphql.nadel.util

import graphql.language.SDLDefinition
import graphql.parser.Parser
import graphql.parser.ParserEnvironment
import graphql.parser.ParserOptions
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.Reader

object SchemaUtil {
    private val defaultParserOptions = ParserOptions.newParserOptions()
        .maxTokens(Int.MAX_VALUE)
        .captureSourceLocation(false)
        .build()

    private val parser = Parser()
    private val schemaParser = SchemaParser()

    fun parseDefinitions(schema: String): List<AnySDLDefinition> {
        return parser
            .parseDocument(
                ParserEnvironment.newParserEnvironment()
                    .document(schema)
                    .parserOptions(defaultParserOptions)
                    .build(),
            )
            .getDefinitionsOfType(SDLDefinition::class.java)
    }

    fun parseDefinitions(schema: Reader): List<AnySDLDefinition> {
        return parser
            .parseDocument(
                ParserEnvironment.newParserEnvironment()
                    .document(schema)
                    .parserOptions(defaultParserOptions)
                    .build(),
            )
            .getDefinitionsOfType(SDLDefinition::class.java)
    }

    fun parseTypeDefinitionRegistry(schema: Reader): TypeDefinitionRegistry {
        return schemaParser.parse(schema, defaultParserOptions)
    }
}
