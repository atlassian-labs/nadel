package graphql.nadel.util

import graphql.language.SDLDefinition
import graphql.parser.Parser
import graphql.parser.ParserOptions
import java.io.Reader

object SchemaUtil {
    private val defaultParserOptions = ParserOptions.newParserOptions()
        .maxTokens(Int.MAX_VALUE)
        .build()

    private val parser = Parser()

    fun parseDefinitions(schema: String): List<AnySDLDefinition> {
        return parser
            .parseDocument(schema, defaultParserOptions)
            .getDefinitionsOfType(SDLDefinition::class.java)
    }

    fun parseDefinitions(schema: Reader): List<AnySDLDefinition> {
        return parser
            .parseDocument(schema, defaultParserOptions)
            .getDefinitionsOfType(SDLDefinition::class.java)
    }
}
