package graphql.nadel

import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.schema.OverallSchemaGenerator
import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.schema.UnderlyingSchemaGenerator
import graphql.nadel.util.SchemaUtil
import graphql.parser.MultiSourceReader
import graphql.schema.GraphQLSchema
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory
import java.io.Reader
import graphql.schema.idl.ScalarInfo.GRAPHQL_SPECIFICATION_SCALARS as graphQLSpecScalars

data class NadelSchemas constructor(
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
        internal var overallSchemaReaders = mutableMapOf<String, Reader>()

        // .graphqls files
        internal var underlyingSchemaReaders = mutableMapOf<String, Reader>()
        internal var underlyingTypeDefs = mutableMapOf<String, TypeDefinitionRegistry>()

        private var captureSourceLocation = false

        fun captureSourceLocation(value: Boolean): Builder = also {
            captureSourceLocation = value
        }

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
            overallSchemaReaders[serviceName] = schema
        }

        fun underlyingSchema(serviceName: String, schema: Reader): Builder = also {
            underlyingTypeDefs.remove(serviceName) // remove from other Map
            underlyingSchemaReaders[serviceName] = schema
        }

        fun underlyingSchema(serviceName: String, schema: TypeDefinitionRegistry): Builder = also {
            underlyingSchemaReaders.remove(serviceName) // remove from other Map
            underlyingTypeDefs[serviceName] = schema
        }

        fun overallSchema(serviceName: String, schema: String): Builder = also {
            overallSchemaReaders[serviceName] = schema.reader()
        }

        fun underlyingSchema(serviceName: String, schema: String): Builder = also {
            underlyingSchema(
                serviceName,
                MultiSourceReader.newMultiSourceReader()
                    .string(schema, serviceName)
                    .build(),
            )
        }

        @JvmName("overallSchemasReader")
        fun overallSchemas(value: Map<String, Reader>): Builder = also {
            overallSchemaReaders = value.toMutableMap() // copy
        }

        @JvmName("underlyingSchemasReader")
        fun underlyingSchemas(value: Map<String, Reader>): Builder = also {
            underlyingSchemaReaders = value.toMutableMap() // copy
            value.keys.forEach(underlyingTypeDefs::remove) // remove from other Map
        }

        @JvmName("overallSchemasString")
        fun overallSchemas(value: Map<String, String>): Builder = also {
            overallSchemaReaders = value
                .mapValuesTo(LinkedHashMap()) { (serviceName, schema) ->
                    MultiSourceReader.newMultiSourceReader()
                        .string(schema, serviceName)
                        .build()
                }
        }

        @JvmName("underlyingSchemasString")
        fun underlyingSchemas(value: Map<String, String>): Builder = also {
            val readers = value
                .mapValuesTo(LinkedHashMap()) { (serviceName, schema) ->
                    MultiSourceReader.newMultiSourceReader()
                        .string(schema, serviceName)
                        .build()
                }

            underlyingSchemas(readers)
        }

        @JvmName("underlyingTypeDefs")
        fun underlyingSchemas(value: Map<String, TypeDefinitionRegistry>): Builder = also {
            underlyingTypeDefs = value.toMutableMap() // copy
            value.keys.forEach(underlyingSchemaReaders::remove) // remove from other Map
        }

        /**
         * Use this if you want just want the service schema without the "full" [Service].
         */
        fun stubServiceExecution(): Builder = also {
            serviceExecutionFactory(
                object : ServiceExecutionFactory {
                    override fun getServiceExecution(serviceName: String): ServiceExecution {
                        return ServiceExecution {
                            throw UnsupportedOperationException("no-op")
                        }
                    }
                },
            )
        }

        fun build(): NadelSchemas {
            require(overallSchemaReaders.isNotEmpty()) { "Nadel schemas must not be empty" }
            require(underlyingSchemaReaders.isNotEmpty() || underlyingTypeDefs.isNotEmpty()) { "Underlying schemas must not be empty" }

            val underlyingServiceNames = underlyingSchemaReaders.keys + underlyingTypeDefs.keys
            require(overallSchemaReaders.keys == underlyingServiceNames) {
                val extraOverallKeys = overallSchemaReaders.keys - underlyingServiceNames
                if (extraOverallKeys.isNotEmpty()) {
                    "There are services in the overall schemas $extraOverallKeys that are not present in the underlying schemas"
                } else {
                    val extraUnderlyingKeys = underlyingServiceNames - overallSchemaReaders.keys
                    "There are extra services in the underlying schemas $extraUnderlyingKeys that are not present in the overall schemas"
                }
            }

            val serviceExecutionFactory = requireNotNull(serviceExecutionFactory) {
                "serviceExecutionFactory must be set"
            }

            // Combine readers & type defs
            val readersToTypeDefs = underlyingSchemaReaders
                .mapValues { (_, reader) ->
                    SchemaUtil.parseTypeDefinitionRegistry(
                        reader,
                        captureSourceLocation = captureSourceLocation,
                    )
                }
            val resolvedUnderlyingTypeDefs = readersToTypeDefs + underlyingTypeDefs

            // Ensure we didn't have dupes i.e. we didn't merge and ignore a value
            require(resolvedUnderlyingTypeDefs.size == underlyingTypeDefs.size + underlyingSchemaReaders.size) {
                val intersection = underlyingTypeDefs.keys.intersect(underlyingSchemaReaders.keys)
                "There is an illegal intersection of underlying schema keys $intersection"
            }

            return Factory(
                builder = this,
                serviceExecutionFactory = serviceExecutionFactory,
                underlyingTypeDefs = resolvedUnderlyingTypeDefs,
                captureSourceLocation = captureSourceLocation,
            ).create()
        }
    }

    internal class Factory(
        private val builder: Builder,
        private val serviceExecutionFactory: ServiceExecutionFactory,
        private val underlyingTypeDefs: Map<String, TypeDefinitionRegistry>,
        private val captureSourceLocation: Boolean,
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

            return builder.overallSchemaReaders.map { (serviceName, reader) ->
                val nadelDefinitions = SchemaUtil.parseSchemaDefinitions(
                    reader,
                    captureSourceLocation = captureSourceLocation,
                )
                val nadelDefinitionRegistry = NadelDefinitionRegistry.from(nadelDefinitions)

                // Builder should enforce non-null entry
                val underlyingTypeDefinitions = underlyingTypeDefs[serviceName]!!
                val underlyingSchema = underlyingSchemaGenerator.buildUnderlyingSchema(
                    serviceName,
                    underlyingTypeDefinitions,
                    builder.underlyingWiringFactory,
                )

                val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)

                Service(
                    name = serviceName,
                    underlyingSchema = underlyingSchema,
                    serviceExecution = serviceExecution,
                    definitionRegistry = nadelDefinitionRegistry,
                    underlyingTypeDefinitions = underlyingTypeDefinitions,
                )
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

