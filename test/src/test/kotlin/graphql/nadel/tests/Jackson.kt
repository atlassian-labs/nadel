package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

val jsonObjectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

val yamlObjectMapper: ObjectMapper = YAMLFactory().let(::ObjectMapper).findAndRegisterModules()
