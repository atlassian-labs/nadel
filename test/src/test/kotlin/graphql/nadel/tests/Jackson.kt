package graphql.nadel.tests

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.SPLIT_LINES
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER

val jsonObjectMapper: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()

val yamlObjectMapper: ObjectMapper = YAMLFactory()
    .enable(LITERAL_BLOCK_STYLE)
    .disable(WRITE_DOC_START_MARKER)
    .disable(SPLIT_LINES)
    .let(::ObjectMapper)
    .findAndRegisterModules()

val prettierPrinter = DefaultPrettyPrinter()
    // Wtf is this half mutable, half immutable API?
    .apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    }
    .withSeparators(
        Separators()
            .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
            .withArrayEmptySeparator("")
            .withObjectEmptySeparator("")
    )

fun ObjectMapper.withPrettierPrinter(): ObjectWriter {
    return writer(prettierPrinter)
}
