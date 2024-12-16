package graphql.nadel

import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelTypeRenameInstructions
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

sealed interface ServiceLike {
    val name: String
    val underlyingSchema: GraphQLSchema
    val serviceExecution: ServiceExecution
}

@Deprecated("Move to NadelExecutableService")
open class Service(
    override val name: String,
    /**
     * These are the types as they are defined in the underlying service's schema.
     *
     *
     * There are no renames, hydrations etc.
     */
    override val underlyingSchema: GraphQLSchema,
    override val serviceExecution: ServiceExecution,
    /**
     * These are the GraphQL definitions that a service contributes to the OVERALL schema.
     */
    val definitionRegistry: NadelDefinitionRegistry,
) : ServiceLike {
    override fun toString(): String {
        return "Service{name='$name'}"
    }
}

class NadelExecutableService internal constructor(
    override val name: String,
    val fieldInstructions: NadelFieldInstructions,
    val typeInstructions: NadelTypeRenameInstructions,
    val declaredOverallTypeNames: Set<String>,
    val declaredUnderlyingTypeNames: Set<String>,
    override val serviceExecution: ServiceExecution,
    @Deprecated("Exists for migration purposes")
    internal val service: Service,
) : ServiceLike {
    override val underlyingSchema: GraphQLSchema
        get() = service.underlyingSchema

    inline fun <reified T : NadelFieldInstruction> getInstructionInsideVirtualType(
        hydrationDetails: ServiceExecutionHydrationDetails?,
        backingField: ExecutableNormalizedField,
    ): Map<String, List<T>> {
        hydrationDetails ?: return emptyMap() // Need hydration to provide virtual hydration context

        val backingFieldParentTypeName = backingField.objectTypeNames.singleOrNull()
            ?: return emptyMap() // Don't support abstract types for now

        val nadelHydrationContext = fieldInstructions[hydrationDetails.hydrationVirtualField]!!
            .asSequence()
            .filterIsInstance<NadelGenericHydrationInstruction>()
            .first() as? NadelHydrationFieldInstruction
            ?: return emptyMap() // Virtual types only come about from standard hydrations, not batched

        val virtualTypeContext = nadelHydrationContext.virtualTypeContext
            ?: return emptyMap() // Not all hydrations create virtual types

        val virtualType = virtualTypeContext.backingTypeToVirtualType[backingFieldParentTypeName]
            ?: return emptyMap() // Not a virtual type

        val fieldCoordinatesInVirtualType = makeFieldCoordinates(virtualType, backingField.name)

        val instructions = fieldInstructions[fieldCoordinatesInVirtualType]
            .filterIsInstance<T>()
            .takeIf {
                it.isNotEmpty()
            }
            ?: return emptyMap()

        return mapOf(
            backingField.objectTypeNames.single() to instructions,
        )
    }
}

data class NadelFieldInstructions internal constructor(
    val instructions: List<NadelFieldInstruction>,
) {
    private val instructionMap = NadelFieldMap.groupBy(instructions, NadelFieldInstruction::location)

    operator fun get(fieldCoordinates: FieldCoordinates): List<NadelFieldInstruction> {
        return instructionMap[fieldCoordinates] ?: emptyList()
    }

    fun getByCoordinates(objectTypeName: String, fieldName: String): List<NadelFieldInstruction> {
        return instructionMap.getByCoordinates(objectTypeName, fieldName) ?: emptyList()
    }

    companion object {
        internal val Empty = NadelFieldInstructions(emptyList())
    }
}

/**
 * Stores [values] that belong to a particular field.
 */
class NadelFieldMap<T> internal constructor(
    /**
     * ObjectTypeName -> FieldName -> List<T>
     */
    private val map: Map<String, Map<String, T>>,
) {
    operator fun get(fieldCoordinates: FieldCoordinates): T? {
        return getByCoordinates(fieldCoordinates.typeName, fieldCoordinates.fieldName)
    }

    fun getByCoordinates(objectTypeName: String, fieldName: String): T? {
        return map[objectTypeName]?.get(fieldName)
    }

    companion object {
        internal inline fun <T> from(
            values: List<T>,
            getCoordinates: (T) -> FieldCoordinates,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getCoordinates = getCoordinates,
                getValue = {
                    it
                },
            )
        }

        @JvmName("fromFieldCoordinates")
        internal inline fun <E, T> from(
            values: List<E>,
            getCoordinates: (E) -> FieldCoordinates,
            getValue: (E) -> T,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getTypeName = {
                    getCoordinates(it).typeName
                },
                getFieldName = {
                    getCoordinates(it).fieldName
                },
                getValue = getValue,
            )
        }

        internal inline fun <T> from(
            values: List<T>,
            getTypeName: (T) -> String,
            getFieldName: (T) -> String,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getTypeName = getTypeName,
                getFieldName = getFieldName,
                getValue = {
                    it
                },
            )
        }

        internal inline fun <E, T> from(
            values: List<E>,
            getTypeName: (E) -> String,
            getFieldName: (E) -> String,
            getValue: (E) -> T,
        ): NadelFieldMap<T> {
            val map: MutableMap<String, MutableMap<String, T>> = mutableMapOf()

            values.forEach { value ->
                val typeName = getTypeName(value)
                val fieldName = getFieldName(value)
                val typeMap = map.computeIfAbsent(typeName) {
                    mutableMapOf()
                }
                typeMap[fieldName] = getValue(value)
            }

            return NadelFieldMap(map)
        }

        internal inline fun <T> groupBy(
            values: List<T>,
            getCoordinates: (T) -> FieldCoordinates,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getCoordinates = getCoordinates,
                getValue = {
                    it
                },
            )
        }

        @JvmName("groupByFieldCoordinates")
        internal inline fun <E, T> groupBy(
            values: List<E>,
            getCoordinates: (E) -> FieldCoordinates,
            getValue: (E) -> T,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getTypeName = {
                    getCoordinates(it).typeName
                },
                getFieldName = {
                    getCoordinates(it).fieldName
                },
                getValue = getValue,
            )
        }

        internal inline fun <T> groupBy(
            values: List<T>,
            getTypeName: (T) -> String,
            getFieldName: (T) -> String,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getTypeName = getTypeName,
                getFieldName = getFieldName,
                getValue = {
                    it
                },
            )
        }

        internal inline fun <E, T> groupBy(
            values: List<E>,
            getTypeName: (E) -> String,
            getFieldName: (E) -> String,
            getValue: (E) -> T,
        ): NadelFieldMap<List<T>> {
            val map: MutableMap<String, MutableMap<String, MutableList<T>>> = mutableMapOf()

            values.forEach { value ->
                map
                    .computeIfAbsent(getTypeName(value)) {
                        mutableMapOf()
                    }
                    .computeIfAbsent(getFieldName(value)) {
                        mutableListOf()
                    }
                    .add(getValue(value))
            }

            return NadelFieldMap(map)
        }
    }
}
