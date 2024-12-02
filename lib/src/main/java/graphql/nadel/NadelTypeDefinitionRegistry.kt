package graphql.nadel

import graphql.language.DirectiveDefinition
import graphql.language.NamedNode
import graphql.language.ObjectTypeDefinition
import graphql.language.SchemaDefinition
import graphql.language.TypeDefinition
import graphql.nadel.util.AnySDLDefinition
import java.util.Collections

/**
 * Alternative to [graphql.schema.idl.TypeDefinitionRegistry] but is more generic
 * and tailored to Nadel specific operations to build the overall schema.
 */
class NadelTypeDefinitionRegistry {
    private val _definitions: MutableList<AnySDLDefinition> = ArrayList()
    private val definitionsByClass = LinkedHashMap<Class<out AnySDLDefinition>, MutableList<AnySDLDefinition>>()
    private val definitionsByName = LinkedHashMap<String, MutableList<AnySDLDefinition>>()

    val definitions: List<AnySDLDefinition>
        get() = Collections.unmodifiableList(_definitions)

    fun add(sdlDefinition: AnySDLDefinition) {
        _definitions.add(sdlDefinition)

        definitionsByClass.computeIfAbsent(sdlDefinition.javaClass) {
            ArrayList()
        }.add(sdlDefinition)

        if (sdlDefinition is TypeDefinition<*> || sdlDefinition is DirectiveDefinition) {
            val name = (sdlDefinition as NamedNode<*>).name

            definitionsByName.computeIfAbsent(name) {
                ArrayList()
            }.add(sdlDefinition)
        }
    }

    val schemaDefinition: SchemaDefinition?
        get() = if (!definitionsByClass.containsKey(SchemaDefinition::class.java)) {
            null
        } else {
            definitionsByClass[SchemaDefinition::class.java]!![0] as SchemaDefinition?
        }

    val operationMap: Map<NadelOperationKind, List<ObjectTypeDefinition>>
        get() {
            return NadelOperationKind.values().associateWith(::getOpsDefinitions)
        }

    private fun getOpsDefinitions(operationKind: NadelOperationKind): List<ObjectTypeDefinition> {
        val type = getOperationTypeName(operationKind)
        return getDefinitionsOfType(type)
    }

    fun getOperationTypeName(operationKind: NadelOperationKind): String {
        val operationName = operationKind.name // e.g. query, mutation etc.

        // Check the schema definition for the operation type
        // i.e. we are trying to find MyOwnQueryType in: schema { query: MyOwnQueryType }
        val schemaDefinition = schemaDefinition
        if (schemaDefinition != null) {
            for (opTypeDef in schemaDefinition.operationTypeDefinitions) {
                if (opTypeDef.name.equals(operationName, ignoreCase = true)) {
                    return opTypeDef.typeName.name
                }
            }
        }

        // This is the default name if there is no schema definition
        return operationKind.defaultTypeName
    }

    private inline fun <reified T : AnySDLDefinition> getDefinitionsOfType(name: String): List<T> {
        val sdlDefinitions = definitionsByName[name]
            ?: return emptyList()

        return sdlDefinitions.filterIsInstance<T>()
    }

    fun getDefinitions(name: String): List<AnySDLDefinition> {
        return definitionsByName[name] ?: return emptyList()
    }

    inline fun <reified T : AnySDLDefinition> getDefinitionsOfType(): List<T> {
        return getDefinitionsOfType(T::class.java)
    }

    fun <T : AnySDLDefinition> getDefinitionsOfType(klass: Class<T>): List<T> {
        return _definitions
            .asSequence()
            .filter {
                klass.isInstance(it)
            }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as T
            }
            .toList()
    }

    companion object {
        @JvmStatic
        fun from(definitions: List<AnySDLDefinition>): NadelTypeDefinitionRegistry {
            val registry = NadelTypeDefinitionRegistry()
            definitions.forEach(registry::add)
            return registry
        }
    }
}
