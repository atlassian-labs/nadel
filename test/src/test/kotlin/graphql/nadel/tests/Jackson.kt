package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.SPLIT_LINES
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER

val jsonObjectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

val yamlObjectMapper: ObjectMapper = YAMLFactory()
    .enable(LITERAL_BLOCK_STYLE)
    .disable(WRITE_DOC_START_MARKER)
    .disable(SPLIT_LINES)
    .let(::ObjectMapper)
    .findAndRegisterModules()
