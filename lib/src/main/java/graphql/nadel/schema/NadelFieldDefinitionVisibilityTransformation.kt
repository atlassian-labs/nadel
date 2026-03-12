package graphql.nadel.schema

import graphql.language.FieldDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.nadel.NadelOperationKind
import graphql.nadel.engine.blueprint.NadelSchemaDefinitionTraverser
import graphql.nadel.engine.blueprint.NadelSchemaDefinitionTraverserElement
import graphql.nadel.engine.blueprint.NadelSchemaDefinitionTraverserVisitor
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.util.AnyImplementingTypeDefinition
import graphql.nadel.util.AnyNamedNode
import graphql.nadel.util.AnySDLDefinition
import graphql.schema.idl.DirectiveInfo
import graphql.schema.idl.ScalarInfo

fun interface NadelFieldDefinitionVisibilityTransformationPredicate {
    operator fun invoke(
        parent: ImplementingTypeDefinition<*>,
        field: FieldDefinition,
    ): Boolean
}

class NadelFieldDefinitionVisibilityTransformation(
    val fieldPredicate: NadelFieldDefinitionVisibilityTransformationPredicate,
) : NadelSchemaDefinitionTransformationHook {
    override fun apply(definitions: List<AnySDLDefinition>): List<AnySDLDefinition> {
        return deleteFields(definitions)
    }

    fun deleteFields(definitions: List<AnySDLDefinition>): List<AnySDLDefinition> {
        val newDefinitions = definitions
            .map { definition ->
                when (definition) {
                    is InterfaceTypeExtensionDefinition -> transformExtensionType(definition)
                    is InterfaceTypeDefinition -> transformType(definition)
                    is ObjectTypeExtensionDefinition -> transformExtensionType(definition)
                    is ObjectTypeDefinition -> transformType(definition)
                    else -> definition
                }
            }

        // val observedBeforeTransform = getStronglyReferencedTypes(definitions)
        // val observedAfterTransform = getStronglyReferencedTypes(newDefinitions)

        val observedBeforeTransform = getStronglyReferencedTypes(definitions)
        val observedAfterTransform = getStronglyReferencedTypes(newDefinitions)

        // if (observedBeforeTransform != observedBeforeTransform2) {
        //     println("observedBeforeTransform mismatch")
        // }
        // if (observedAfterTransform != observedAfterTransform2) {
        //     println("observedAfterTransform mismatch")
        // }

        return newDefinitions
            .filter { definition ->
                if (definition is AnyNamedNode) {
                    if (definition.name in observedBeforeTransform) {
                        definition.name in observedAfterTransform
                    } else {
                        true
                    }
                } else {
                    true
                }
            }
    }

    fun transformExtensionType(definition: InterfaceTypeExtensionDefinition): InterfaceTypeExtensionDefinition {
        val newFields = filterFields(definition)
        if (newFields.size == definition.fieldDefinitions.size) {
            return definition
        }

        return definition.transformExtension { builder ->
            builder.definitions(newFields)
        }
    }

    fun transformType(definition: InterfaceTypeDefinition): InterfaceTypeDefinition {
        val newFields = filterFields(definition)
        if (newFields.size == definition.fieldDefinitions.size) {
            return definition
        }

        return definition.transform { builder ->
            builder.definitions(newFields)
        }
    }

    fun transformExtensionType(definition: ObjectTypeExtensionDefinition): ObjectTypeExtensionDefinition {
        val newFields = filterFields(definition)
        if (newFields.size == definition.fieldDefinitions.size) {
            return definition
        }

        return definition.transformExtension { builder ->
            builder.fieldDefinitions(newFields)
        }
    }

    fun transformType(definition: ObjectTypeDefinition): ObjectTypeDefinition {
        val newFields = filterFields(definition)
        if (newFields.size == definition.fieldDefinitions.size) {
            return definition
        }

        return definition.transform { builder ->
            builder.fieldDefinitions(newFields)
        }
    }

    fun filterFields(
        parent: ImplementingTypeDefinition<*>,
    ): List<FieldDefinition> {
        return parent.fieldDefinitions
            .filter { field ->
                fieldPredicate(parent, field)
            }
    }

    fun getStronglyReferencedTypes(types: List<AnySDLDefinition>): Set<String> {
        val typesByName = types
            .groupBy {
                (it as AnyNamedNode).name
            }

        val typeQueue = NadelOperationKind.entries
            .asSequence()
            .filter { it.name in typesByName }
            .mapTo(mutableListOf()) { it.name }

        val typeReferences = mutableSetOf<String>()

        while (typeQueue.isNotEmpty()) {
            // Exhaust queue
            while (typeQueue.isNotEmpty()) {
                val typeName = typeQueue.removeLast()
                if (ScalarInfo.isGraphqlSpecifiedScalar(typeName) || DirectiveInfo.isGraphqlSpecifiedDirective(typeName)) {
                    continue
                }

                val types = typesByName[typeName] ?: throw NullPointerException(typeName)
                collectTypeReferences(types) { typeReference ->
                    if (typeReference !in typeReferences) {
                        typeReferences.add(typeReference)
                        typeQueue.add(typeReference)
                    }
                }
            }

            // Populate queue up with interface implementations
            types.forEach { type ->
                if (type is AnyImplementingTypeDefinition) {
                    if (type.name !in typeReferences) {
                        if (type.implements.any { it.unwrapAll().name in typeReferences }) {
                            typeReferences.add(type.name)
                            typeQueue.add(type.name)
                        }
                    }
                }
            }
        }

        return typeReferences
    }

    private fun collectTypeReferences(
        type: List<AnySDLDefinition>,
        onTypeReferenced: (String) -> Unit,
    ) {
        val roots = type
            .asSequence()
            .map { type ->
                NadelSchemaDefinitionTraverserElement.from(type)
            }
            .asIterable()

        NadelSchemaDefinitionTraverser()
            .traverse(
                roots,
                object : NadelSchemaDefinitionTraverserVisitor {
                    override fun visitGraphQLArgument(element: NadelSchemaDefinitionTraverserElement.Argument): Boolean {
                        return true
                    }

                    override fun visitGraphQLUnionType(element: NadelSchemaDefinitionTraverserElement.UnionType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLInterfaceType(element: NadelSchemaDefinitionTraverserElement.InterfaceType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLEnumType(element: NadelSchemaDefinitionTraverserElement.EnumType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLEnumValueDefinition(element: NadelSchemaDefinitionTraverserElement.EnumValueDefinition): Boolean {
                        return true
                    }

                    override fun visitGraphQLFieldDefinition(element: NadelSchemaDefinitionTraverserElement.FieldDefinition): Boolean {
                        return true
                    }

                    override fun visitGraphQLInputObjectField(element: NadelSchemaDefinitionTraverserElement.InputObjectField): Boolean {
                        return true
                    }

                    override fun visitGraphQLInputObjectType(element: NadelSchemaDefinitionTraverserElement.InputObjectType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLObjectType(element: NadelSchemaDefinitionTraverserElement.ObjectType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLScalarType(element: NadelSchemaDefinitionTraverserElement.ScalarType): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLDirective(element: NadelSchemaDefinitionTraverserElement.Directive): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLAppliedDirective(element: NadelSchemaDefinitionTraverserElement.AppliedDirective): Boolean {
                        onTypeReferenced(element.node.name)
                        return true
                    }

                    override fun visitGraphQLAppliedDirectiveArgument(element: NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument): Boolean {
                        return true
                    }

                    override fun visitTypeReference(element: NadelSchemaDefinitionTraverserElement.TypeReference): Boolean {
                        onTypeReferenced(element.node.unwrapAll().name)
                        return true
                    }

                    override fun visitSchemaDefinition(element: NadelSchemaDefinitionTraverserElement.SchemaDefinition): Boolean {
                        return true
                    }
                }
            )
    }
}
