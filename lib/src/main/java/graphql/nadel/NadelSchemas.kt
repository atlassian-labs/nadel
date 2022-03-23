package graphql.nadel

import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.schema.OverallSchemaGenerator
import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.schema.UnderlyingSchemaGenerator
import graphql.nadel.util.SchemaUtil
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.WiringFactory
import java.io.Reader
import java.io.StringReader
import graphql.schema.idl.ScalarInfo.GRAPHQL_SPECIFICATION_SCALARS as graphQLSpecScalars

data class NadelSchemas internal constructor(
    val engineSchema: GraphQLSchema,
    val services: List<Service>,
) {
    companion object {
        @JvmStatic
        fun newNadelSchemas() = Builder()
    }

    class Builder {
        internal var schemaTransformationHook: SchemaTransformationHook = SchemaTransformationHook.Identity

        internal var overallWiringFactory: WiringFactory = NeverWiringFactory()
        internal var underlyingWiringFactory: WiringFactory = NeverWiringFactory()

        internal var serviceExecutionFactory: ServiceExecutionFactory? = null

        // .nadel files
        internal var overallSchemas = mutableMapOf<String, Reader>()

        // .graphqls files
        internal var underlyingSchemas = mutableMapOf<String, Reader>()

        fun schemaTransformationHook(value: SchemaTransformationHook): Builder = also {
            schemaTransformationHook = value
        }

        fun overallWiringFactory(value: WiringFactory): Builder = also {
            overallWiringFactory = value
        }

        fun underlyingWiringFactory(value: WiringFactory): Builder = also {
            underlyingWiringFactory = value
        }

        fun serviceExecutionFactory(value: ServiceExecutionFactory): Builder = also {
            serviceExecutionFactory = value
        }

        fun overallSchema(serviceName: String, schema: Reader): Builder = also {
            overallSchemas[serviceName] = schema
        }

        fun underlyingSchema(serviceName: String, schema: Reader): Builder = also {
            underlyingSchemas[serviceName] = schema
        }

        fun overallSchema(serviceName: String, schema: String): Builder = also {
            overallSchemas[serviceName] = schema.reader()
        }

        fun underlyingSchema(serviceName: String, schema: String): Builder = also {
            underlyingSchemas[serviceName] = schema.reader()
        }

        @JvmName("overallSchemasReader")
        fun overallSchemas(value: Map<String, Reader>): Builder = also {
            overallSchemas = value.toMutableMap() // copy
        }

        @JvmName("underlyingSchemasReader")
        fun underlyingSchemas(value: Map<String, Reader>): Builder = also {
            underlyingSchemas = value.toMutableMap() // copy
        }

        @JvmName("overallSchemasString")
        fun overallSchemas(value: Map<String, String>): Builder = also {
            overallSchemas = value
                .mapValuesTo(LinkedHashMap()) { (_, schema) ->
                    StringReader(schema)
                }
        }

        @JvmName("underlyingSchemasString")
        fun underlyingSchemas(value: Map<String, String>): Builder = also {
            underlyingSchemas = value
                .mapValuesTo(LinkedHashMap()) { (_, schema) ->
                    StringReader(schema)
                }
        }

        fun build(): NadelSchemas {
            require(overallSchemas.isNotEmpty()) { "Nadel schemas must not be empty" }
            require(underlyingSchemas.isNotEmpty()) { "Underlying schemas must not be empty" }

            require(underlyingSchemas.keys.containsAll(overallSchemas.keys)) {
                val missing = overallSchemas.keys - underlyingSchemas.keys
                "Each Nadel schema must have an equivalent underlying schema. Missing $missing"
            }
            val serviceExecutionFactory = requireNotNull(serviceExecutionFactory) {
                "serviceExecutionFactory must be set"
            }

            return Factory(builder = this, serviceExecutionFactory).create()
        }
    }

    internal class Factory(
        private val builder: Builder,
        private var serviceExecutionFactory: ServiceExecutionFactory,
    ) {
        fun create(): NadelSchemas {
            val services = createServices()

            return NadelSchemas(
                engineSchema = createEngineSchema(services),
                services = services,
            )
        }

        private fun createServices(): List<Service> {
            val underlyingSchemaGenerator = UnderlyingSchemaGenerator()

            return builder.overallSchemas.map { (serviceName, reader) ->
                val nadelDefinitions = SchemaUtil.parseDefinitions(reader)
                val nadelDefinitionRegistry = NadelDefinitionRegistry.from(nadelDefinitions)

                // Builder should enforce non-null entry
                val underlyingSchemaReader = requireNotNull(builder.underlyingSchemas[serviceName])
                val underlyingTypeDefinitions = SchemaParser().parse(underlyingSchemaReader)
                val underlyingSchema = underlyingSchemaGenerator.buildUnderlyingSchema(
                    serviceName,
                    underlyingTypeDefinitions,
                    builder.underlyingWiringFactory,
                )

                val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)

                Service(serviceName, underlyingSchema, serviceExecution, nadelDefinitionRegistry)
            }
        }

        private fun createEngineSchema(services: List<Service>): GraphQLSchema {
            val overallSchemaGenerator = OverallSchemaGenerator()
            val serviceRegistries = services.map(Service::definitionRegistry)
            val schema = overallSchemaGenerator.buildOverallSchema(serviceRegistries, builder.overallWiringFactory)
            val newSchema = builder.schemaTransformationHook.apply(schema, services)

            // make sure that the overall schema has the standard scalars in
            // it since the underlying may use them EVEN if the overall does
            // not make direct use of them, we still have to map between them
            return newSchema.transform { builder: GraphQLSchema.Builder ->
                graphQLSpecScalars.forEach(builder::additionalType)
            }
        }
    }
}

