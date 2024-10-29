package graphql.nadel.validation.util

import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.engine.blueprint.FastSchemaTraverser
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

    val traverser = object : FastSchemaTraverser.Visitor {
        override fun visitGraphQLArgument(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLArgument,
        ): Boolean {
            add(node.type.unwrapAll().name)
            return true
        }

        override fun visitGraphQLUnionType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLUnionType,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLInterfaceType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInterfaceType,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLEnumType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLEnumType,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLEnumValueDefinition(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLEnumValueDefinition,
        ): Boolean {
            return false
        }

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

            return !node.isHydrated()
        }

        override fun visitGraphQLInputObjectField(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputObjectField,
        ): Boolean {
            add(node.type.unwrapAll().name)
            return true
        }

        override fun visitGraphQLInputObjectType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputObjectType,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLList(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLList,
        ): Boolean {
            add(node.unwrapAll().name)
            return true
        }

        override fun visitGraphQLNonNull(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLNonNull,
        ): Boolean {
            add(node.unwrapAll().name)
            return true
        }

        override fun visitGraphQLObjectType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLObjectType,
        ): Boolean {
            // Don't look at union members defined by external services
            if (parent is GraphQLUnionType && isUnionMemberExempt(service, parent, node)) {
                return false
            }

            add(node.name)
            return true
        }

        override fun visitGraphQLScalarType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLScalarType,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLTypeReference(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLTypeReference,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLModifiedType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLModifiedType,
        ): Boolean {
            add(node.unwrapAll().name)
            return true
        }

        override fun visitGraphQLCompositeType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLCompositeType,
        ): Boolean {
            add(node.name)
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
            add(node.name)
            return true
        }

        override fun visitGraphQLFieldsContainer(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLFieldsContainer,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLInputFieldsContainer(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputFieldsContainer,
        ): Boolean {
            add(node.name)
            return true
        }

        override fun visitGraphQLNullableType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLNullableType,
        ): Boolean {
            add(node.unwrapAll().name)
            return true
        }

        override fun visitGraphQLOutputType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLOutputType,
        ): Boolean {
            add(node.unwrapAll().name)
            return true
        }

        override fun visitGraphQLUnmodifiedType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLUnmodifiedType,
        ): Boolean {
            add(node.name)
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
    }

    FastSchemaTraverser().traverse(
        schema = engineSchema,
        roots = definitionNames,
        visitor = traverser,
    )

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
