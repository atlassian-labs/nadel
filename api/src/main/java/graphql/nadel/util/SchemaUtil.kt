package graphql.nadel.util;

import graphql.language.SDLDefinition;
import graphql.parser.Parser;
import graphql.parser.ParserOptions;

import java.io.Reader;
import java.util.List;

import static graphql.parser.ParserOptions.newParserOptions;
import static java.lang.Integer.MAX_VALUE;

public class SchemaUtil {
    private static final ParserOptions defaultParserOptions = newParserOptions()
        .maxTokens(MAX_VALUE)
        .build();

    public static List<SDLDefinition> parseDefinitions(String schema) {
        return new Parser().parseDocument(schema, defaultParserOptions)
            .getDefinitionsOfType(SDLDefinition.class);
    }

    public static List<SDLDefinition> parseDefinitions(Reader schema) {
        return new Parser().parseDocument(schema, defaultParserOptions)
            .getDefinitionsOfType(SDLDefinition.class);
    }
}
