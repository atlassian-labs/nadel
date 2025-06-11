package graphql.nadel.validation.util

import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.stubbed.hasStubbedDefinition
import graphql.nadel.definition.virtualType.hasVirtualTypeDefinition
import graphql.nadel.engine.blueprint.NadelSchemaTraverser
import graphql.nadel.engine.blueprint.NadelSchemaTraverserElement
import graphql.nadel.engine.blueprint.NadelSchemaTraverserVisitor
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal sealed class NadelReferencedType {
    abstract val name: String

    data class OrdinaryType(
        override val name: String,
    ) : NadelReferencedType()

    data class StubbedType(
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

    NadelSchemaTraverser().traverse(
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
) : NadelSchemaTraverserVisitor {
    fun onTypeReferenced(name: String) {
        onTypeReferenced(NadelReferencedType.OrdinaryType(name))
    }

    fun onStubbedTypeReferenced(stubbedType: String) {
        onTypeReferenced(NadelReferencedType.StubbedType(stubbedType))
    }

    fun onVirtualTypeReferenced(virtualType: String, backingType: String) {
        onTypeReferenced(NadelReferencedType.VirtualType(virtualType, backingType))
    }

    override fun visitGraphQLArgument(
        element: NadelSchemaTraverserElement.Argument,
    ): Boolean {
        return true
    }

    override fun visitGraphQLUnionType(
        element: NadelSchemaTraverserElement.UnionType,
    ): Boolean {
        visitTypeGuard(element) { return false }
        val node = element.node
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLUnionMemberType(element: NadelSchemaTraverserElement.UnionMemberType): Boolean {
        val union = element.parent
        val memberType = element.node
        // Don't look at union members defined by external services
        return !isUnionMemberExempt(service, union, memberType)
    }

    override fun visitGraphQLInterfaceType(
        element: NadelSchemaTraverserElement.InterfaceType,
    ): Boolean {
        visitTypeGuard(element) { return false }
        val node = element.node
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLEnumType(
        element: NadelSchemaTraverserElement.EnumType,
    ): Boolean {
        visitTypeGuard(element) { return false }
        val node = element.node
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLEnumValueDefinition(
        element: NadelSchemaTraverserElement.EnumValueDefinition,
    ): Boolean {
        return true
    }

    // todo: does this validate type extensions properly?
    override fun visitGraphQLFieldDefinition(
        element: NadelSchemaTraverserElement.FieldDefinition,
    ): Boolean {
        val parent = element.parent
        val node = element.node

        // Don't look at fields contributed by other services
        if (parent.name in combinedTypeNames) {
            if (service.name != fieldContributor[makeFieldCoordinates(parent.name, node.name)]!!.name) {
                return false
            }
        }

        val hydrations = instructionDefinitions.getHydrationDefinitions(parent, node)
        if (hydrations.any()) {
            visitHydratedFieldDefinition(node, hydrations)
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
                getUnderlyingType(impl, service) is GraphQLObjectType
            }
            .filterNot {
                it.hasVirtualTypeDefinition() // Not sure if this is needed…
            }
    }

    private fun visitHydratedFieldDefinition(
        field: GraphQLFieldDefinition,
        definitions: Sequence<NadelHydrationDefinition>,
    ) {
        val outputType = field.type.unwrapAll()
        if (outputType.hasVirtualTypeDefinition()) {
            definitions
                .forEach { hydration ->
                    val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)
                    if (backingField != null) {
                        onVirtualTypeReferenced(
                            virtualType = outputType.name,
                            backingType = backingField.type.unwrapAll().name,
                        )
                    }
                }
        }
    }

    override fun visitGraphQLInputObjectField(
        element: NadelSchemaTraverserElement.InputObjectField,
    ): Boolean {
        return true
    }

    override fun visitGraphQLInputObjectType(
        element: NadelSchemaTraverserElement.InputObjectType,
    ): Boolean {
        visitTypeGuard(element) { return false }
        val node = element.node
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLObjectType(
        element: NadelSchemaTraverserElement.ObjectType,
    ): Boolean {
        visitTypeGuard(element, exitOnStubbedType = false) { return false }
        val node = element.node
        if (node.hasStubbedDefinition()) {
            onStubbedTypeReferenced(node.name)
        } else {
            onTypeReferenced(node.name)
        }
        return true
    }

    override fun visitGraphQLScalarType(
        element: NadelSchemaTraverserElement.ScalarType,
    ): Boolean {
        visitTypeGuard(element) { return false }
        val node = element.node
        onTypeReferenced(node.name)
        return true
    }

    override fun visitGraphQLDirective(
        element: NadelSchemaTraverserElement.Directive,
    ): Boolean {
        // We don't care about directives, not a Nadel runtime concern
        // GraphQL Java will do validation on them for us
        return false
    }

    override fun visitGraphQLAppliedDirective(
        element: NadelSchemaTraverserElement.AppliedDirective,
    ): Boolean {
        // Don't look into applied directives. Could be a shared directive.
        // As long as the schema compiled then we don't care.
        return false
    }

    override fun visitGraphQLAppliedDirectiveArgument(
        element: NadelSchemaTraverserElement.AppliedDirectiveArgument,
    ): Boolean {
        return false
    }

    /**
     * Call to ensure the given [element] is not traversed if it shouldn't be.
     *
     * The [onExit] lambda is not intended to return, so it is typed to [Nothing]
     * i.e. use [onExit] to actually exit the outer function to escape the lambda
     */
    private inline fun visitTypeGuard(
        element: NadelSchemaTraverserElement.Type,
        exitOnVirtualType: Boolean = true,
        exitOnStubbedType: Boolean = true,
        onExit: () -> Nothing,
    ) {
        val type = element.node
        if (type is GraphQLDirectiveContainer) {
            if (exitOnVirtualType && type.hasVirtualTypeDefinition()) {
                onExit()
            }
            if (exitOnStubbedType && type.hasStubbedDefinition()) {
                onExit()
            }
        }
    }
}

context(NadelValidationContext)
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
context(NadelValidationContext)
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

context(NadelValidationContext)
internal fun isMemberMissingFromUnderlyingSchema(
    service: Service,
    overallType: GraphQLNamedType,
): Boolean {
    return getUnderlyingType(overallType, service) == null
}
