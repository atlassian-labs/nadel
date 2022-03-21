package graphql.nadel.testutils

import graphql.nadel.NadelDefinitionRegistry
import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.schema.OverallSchemaGenerator
import graphql.nadel.util.SchemaUtil
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory
import graphql.schema.idl.errors.SchemaProblem

class TestUtil {

    static TypeDefinitionRegistry typeDefinitions(String spec) {
        try {
            def registry = new SchemaParser().parse(spec)
            return registry
        } catch (SchemaProblem e) {
            assert false: "The underlying schema could not be compiled : ${e}"
            return null
        }
    }

    static GraphQLSchema schema(String spec) {
        schema(spec, mockRuntimeWiring)
    }

    static GraphQLSchema schema(Reader specReader) {
        schema(specReader, mockRuntimeWiring)
    }

    static GraphQLSchema schema(String spec, RuntimeWiring runtimeWiring) {
        schema(new StringReader(spec), runtimeWiring)
    }

    static GraphQLSchema schema(InputStream specStream, RuntimeWiring runtimeWiring) {
        schema(new InputStreamReader(specStream), runtimeWiring)
    }

    static GraphQLSchema schema(Reader specReader, RuntimeWiring runtimeWiring) {
        try {
            def registry = new SchemaParser().parse(specReader)
            def options = SchemaGenerator.Options.defaultOptions()
            return new SchemaGenerator().makeExecutableSchema(options, registry, runtimeWiring)
        } catch (SchemaProblem e) {
            assert false: "The schema could not be compiled : ${e}"
            return null
        }
    }

    static GraphQLSchema schema(SchemaGenerator.Options options, String spec, RuntimeWiring runtimeWiring) {
        try {
            def registry = new SchemaParser().parse(spec)
            return new SchemaGenerator().makeExecutableSchema(options, registry, runtimeWiring)
        } catch (SchemaProblem e) {
            assert false: "The schema could not be compiled : ${e}"
            return null
        }
    }

    static GraphQLSchema schemaFromNdsl(Map<String, String> serviceDSLs) {
        def defRegistries = []

        for (Map.Entry<String, String> e : serviceDSLs.entrySet()) {
            def schemaDocument = SchemaUtil.INSTANCE.parseDefinitions(e.value)
            def definitionRegistry = NadelDefinitionRegistry.from(schemaDocument)
            defRegistries.add(definitionRegistry)
        }

        return new OverallSchemaGenerator().buildOverallSchema(defRegistries, new NeverWiringFactory())
    }

    static WiringFactory mockWiringFactory = new MockedWiringFactory()

    static RuntimeWiring mockRuntimeWiring = RuntimeWiring.newRuntimeWiring().wiringFactory(mockWiringFactory).build()

}
