package graphql.nadel.validation.util

import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.hydration.isIdHydrated
import graphql.nadel.definition.renamed.getRenamedOrNull
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.blueprint.NadelFastSchemaTraverser
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLModifiedType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLNullableType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeReference
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

internal sealed class NadelReferencedType {
    abstract val name: String

    data class OrdinaryType(
        override val name: String,
    ) : NadelReferencedType()

    data class VirtualType(
        override val name: String,
        val backingType: String,
    ) : NadelReferencedType()
}

context(NadelValidationContext)
internal fun getReferencedTypeNames(
    service: Service,
    definitionNames: List<String>,
): Set<NadelReferencedType> {
    val referencedTypes = mutableSetOf<NadelReferencedType>()

    val traverser = NadelReferencedTypeVisitor(service) { reference ->
        referencedTypes.add(reference)
    }

    NadelFastSchemaTraverser().traverse(
        schema = engineSchema,
        roots = definitionNames,
        visitor = traverser,
    )

    return referencedTypes
}

context(NadelValidationContext)
private class NadelReferencedTypeVisitor(
    private val service: Service,
    private val onTypeReferenced: (NadelReferencedType) -> Unit,
) : NadelFastSchemaTraverser.Visitor {
    fun onTypeReferenced(name: String) {
        onTypeReferenced(NadelReferencedType.OrdinaryType(name))
    }

    fun onVirtualTypeReferenced(virtualType: String, backingType: String) {
        onTypeReferenced(NadelReferencedType.VirtualType(virtualType, backingType))
    }

    override fun visitGraphQLArgument(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLArgument,
    ): Boolean {
        onTypeReferenced(node.type.unwrapAll().name)
        return true
    }

    override fun visitGraphQLUnionType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLUnionType,
    ): Boolean {
        visitTypeGuard(node) { return false }
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLInterfaceType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLInterfaceType,
    ): Boolean {
        visitTypeGuard(node) { return false }
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLEnumType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLEnumType,
    ): Boolean {
        visitTypeGuard(node) { return false }
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLEnumValueDefinition(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLEnumValueDefinition,
    ): Boolean {
        return true
    }

    // todo: does this validate type extensions properly?
    override fun visitGraphQLFieldDefinition(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLFieldDefinition,
    ): Boolean {
        val parentNode = parent as GraphQLFieldsContainer

        // Don't look at fields contributed by other services
        if (parentNode.name in combinedTypeNames) {
            if (service.name != fieldContributor[makeFieldCoordinates(parentNode.name, node.name)]!!.name) {
                return false
            }
        }

         if (node.isHydrated() || node.isIdHydrated()) {
            visitHydratedFieldDefinition(node)
            // Never continue traversing on a hydrated field, we have special handling for that in visitHydratedFieldDefinition
            return false
        }

        val unwrappedOutputType = node.type.unwrapAll()
        if (unwrappedOutputType is GraphQLInterfaceType) {
            getObjectTypes(unwrappedOutputType)
                .forEach {
                    onTypeReferenced(it.name)
                }
        }

        return true
    }

    /**
     * For every interface referenced, we need to try match the object types the service implements.
     */
    private fun getObjectTypes(interfaceType: GraphQLInterfaceType): Sequence<GraphQLObjectType> {
        val impls = engineSchema.getImplementations(interfaceType)

        return impls
            .asSequence()
            .filter { impl ->
                val underlyingTypeName = impl.getRenamedOrNull()?.from ?: impl.name
                service.underlyingSchema.typeMap[underlyingTypeName] is GraphQLObjectType
            }
            .filterNot {
                it.isVirtualType() // Not sure if this is needed…
            }
    }

    private fun visitHydratedFieldDefinition(field: GraphQLFieldDefinition) {
        val outputType = field.type.unwrapAll()
        if (outputType.isVirtualType()) {
            for (hydration in field.getHydrationDefinitions()) {
                val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)
                    ?: continue // Error will be handled elsewhere down the line
                onVirtualTypeReferenced(
                    virtualType = outputType.name,
                    backingType = backingField.type.unwrapAll().name,
                )
            }
        }
    }

    override fun visitGraphQLInputObjectField(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLInputObjectField,
    ): Boolean {
        onTypeReferenced(node.type.unwrapAll().name)
        return true
    }

    override fun visitGraphQLInputObjectType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLInputObjectType,
    ): Boolean {
        visitTypeGuard(node) { return false }
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLList(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLList,
    ): Boolean {
        onTypeReferenced(node.unwrapAll().name)
        return true
    }

    override fun visitGraphQLNonNull(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLNonNull,
    ): Boolean {
        onTypeReferenced(node.unwrapAll().name)
        return true
    }

    override fun visitGraphQLObjectType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLObjectType,
    ): Boolean {
        visitTypeGuard(node) { return false }

        // Don't look at union members defined by external services
        if (parent is GraphQLUnionType && isUnionMemberExempt(service, parent, node)) {
            return false
        }

        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLScalarType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLScalarType,
    ): Boolean {
        visitTypeGuard(node) { return false }
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLTypeReference(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLTypeReference,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLModifiedType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLModifiedType,
    ): Boolean {
        onTypeReferenced(node.unwrapAll().name)
        return true
    }

    override fun visitGraphQLCompositeType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLCompositeType,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLDirective(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLDirective,
    ): Boolean {
        // We don't care about directives, not a Nadel runtime concern
        // GraphQL Java will do validation on them for us
        return false
    }

    override fun visitGraphQLDirectiveContainer(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLDirectiveContainer,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLFieldsContainer(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLFieldsContainer,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLInputFieldsContainer(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLInputFieldsContainer,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLNullableType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLNullableType,
    ): Boolean {
        onTypeReferenced(node.unwrapAll().name)
        return true
    }

    override fun visitGraphQLOutputType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLOutputType,
    ): Boolean {
        onTypeReferenced(node.unwrapAll().name)
        return true
    }

    override fun visitGraphQLUnmodifiedType(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLUnmodifiedType,
    ): Boolean {
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLAppliedDirective(
        parent: GraphQLNamedSchemaElement?,
        node: GraphQLAppliedDirective,
    ): Boolean {
        // Don't look into applied directives. Could be a shared directive.
        // As long as the schema compiled then we don't care.
        return false
    }

    /**
     * Call to ensure the given [type] is not traversed if it shouldn't be.
     *
     * The [onExit] lambda is not intended to return, so it is typed to [Nothing]
     * i.e. use [onExit] to actually exit the outer function to escape the lambda
     */
    private inline fun visitTypeGuard(type: GraphQLNamedType, onExit: () -> Nothing) {
        if (type is GraphQLDirectiveContainer) {
            if (type.isVirtualType()) {
                onExit()
            }
        }
    }
}

internal fun isUnionMemberExempt(
    service: Service,
    unionType: GraphQLUnionType,
    memberInQuestion: GraphQLObjectType,
): Boolean {
    return isMemberMissingFromUnderlyingSchema(service, memberInQuestion)
        && isUnionMemberExternal(service, unionType, memberInQuestion)
}

/**
 * Checks if the [unionType] definitions inside [service] actually declare [memberInQuestion].
 *
 * @return true if the [memberInQuestion] was NOT declared inside [service]
 */
internal fun isUnionMemberExternal(
    service: Service,
    unionType: GraphQLUnionType,
    memberInQuestion: GraphQLObjectType,
): Boolean {
    return service.definitionRegistry
        .getDefinitions(unionType.name)
        .asSequence()
        .flatMap {
            (it as UnionTypeDefinition).memberTypes
        }
        .none {
            it.unwrapAll().name == memberInQuestion.name
        }
}

internal fun isMemberMissingFromUnderlyingSchema(
    service: Service,
    overallType: GraphQLNamedType,
): Boolean {
    return getUnderlyingType(overallType, service) == null
}
