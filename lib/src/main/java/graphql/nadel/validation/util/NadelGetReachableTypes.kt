package graphql.nadel.validation.util

import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.util.AnySDLNamedDefinition
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
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLModifiedType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLNullableType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLTypeReference
import graphql.schema.GraphQLTypeVisitorStub
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType
import graphql.schema.SchemaTraverser
import graphql.util.TraversalControl
import graphql.util.TraversalControl.ABORT
import graphql.util.TraversalControl.CONTINUE
import graphql.util.TraverserContext

context(NadelValidationContext)
internal fun getReachableTypeNames(
    service: Service,
    definitionNames: List<String>,
): Set<String> {
    // We need a mutable Set so we can keep track of what types we've visited to avoid StackOverflows
    val reachableTypeNames = mutableSetOf<String>()

    // We keep these so we can add breakpoints to see where a type came from
    fun add(name: String) = reachableTypeNames.add(name)
    fun addAll(elements: Collection<String>) = reachableTypeNames.addAll(elements)

    val serviceDefinitions = engineSchema.typeMap.values.filter { it.name in definitionNames }
    val traverser = object : GraphQLTypeVisitorStub() {
        override fun visitGraphQLArgument(
            node: GraphQLArgument,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.type.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLUnionType(
            node: GraphQLUnionType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLInterfaceType(
            node: GraphQLInterfaceType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLEnumType(
            node: GraphQLEnumType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLFieldDefinition(
            node: GraphQLFieldDefinition,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            val parentNode = context.parentNode as GraphQLFieldsContainer

            // Don't look at fields contributed by other services
            if (parentNode.name in combinedTypeNames) {
                if (service.name != fieldContributor[makeFieldCoordinates(parentNode.name, node.name)]!!.name) {
                    return ABORT
                }
            }

            return if (node.isHydrated()) {
                // Do not collect output type, hydrations do not require the type to be defined in the service
                ABORT
            } else {
                // Will visit the output type
                CONTINUE
            }
        }

        override fun visitGraphQLInputObjectField(
            node: GraphQLInputObjectField,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.type.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLInputObjectType(
            node: GraphQLInputObjectType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLList(
            node: GraphQLList,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLNonNull(
            node: GraphQLNonNull,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLObjectType(
            node: GraphQLObjectType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }

            // Don't look at union members defined by external services
            val parentNode = context.parentNode
            if (parentNode is GraphQLUnionType && isUnionMemberExempt(service, parentNode, node)) {
                return ABORT
            }

            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLScalarType(
            node: GraphQLScalarType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            visitTypeGuard(node) { return ABORT }
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLTypeReference(
            node: GraphQLTypeReference,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLModifiedType(
            node: GraphQLModifiedType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLCompositeType(
            node: GraphQLCompositeType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLDirective(
            node: GraphQLDirective,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            // We don't care about directives, not a Nadel runtime concern
            // GraphQL Java will do validation on them for us
            return ABORT
        }

        override fun visitGraphQLDirectiveContainer(
            node: GraphQLDirectiveContainer,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLFieldsContainer(
            node: GraphQLFieldsContainer,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLInputFieldsContainer(
            node: GraphQLInputFieldsContainer,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLNullableType(
            node: GraphQLNullableType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLOutputType(
            node: GraphQLOutputType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.unwrapAll().name)
            return CONTINUE
        }

        override fun visitGraphQLUnmodifiedType(
            node: GraphQLUnmodifiedType,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            add(node.name)
            return CONTINUE
        }

        override fun visitGraphQLAppliedDirective(
            node: GraphQLAppliedDirective,
            context: TraverserContext<GraphQLSchemaElement>,
        ): TraversalControl {
            // Don't look into applied directives. Could be a shared directive.
            // As long as the schema compiled then we don't care.
            return ABORT
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

    SchemaTraverser { element ->
        element.children
    }.depthFirst(traverser, serviceDefinitions)

    return reachableTypeNames
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
