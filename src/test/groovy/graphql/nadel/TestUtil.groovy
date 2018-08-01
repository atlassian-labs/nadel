package graphql.nadel

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.language.Node
import groovy.json.JsonSlurper


class TestUtil {

    static String printAstCompact(Document document) {
        AstPrinter.printAst(document).replaceAll("\\s+", " ").trim()
    }

    private static String printAstAsJson(Node node) {
        SimpleBeanPropertyFilter theFilter = SimpleBeanPropertyFilter
                .serializeAllExcept("sourceLocation", "children") as SimpleBeanPropertyFilter
        FilterProvider filters = new SimpleFilterProvider()
        filters.addFilter("myFilter" as String, theFilter as SimpleBeanPropertyFilter)

        ObjectMapper mapper = new ObjectMapper()
        mapper.addMixIn(Object.class, FilterMixin.class)
        mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setFilterProvider(filters)
        return mapper.writeValueAsString(node)
    }

    static Map astAsMap(Node node) {
        def json = printAstAsJson(node)
        return new JsonSlurper().parseText(json)
    }

    static Map getExpectedData(String name) {
        String json = getClass().getResourceAsStream("/" + name + ".json").text
        return new JsonSlurper().parseText(json)
    }

}
