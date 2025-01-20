package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.definition.virtualType.hasVirtualTypeDefinition
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelVirtualTypeValidationContext.TraversalOutcome.DUPLICATED_BACKING_TYPE
import graphql.nadel.validation.NadelVirtualTypeValidationContext.TraversalOutcome.SKIP
import graphql.nadel.validation.NadelVirtualTypeValidationContext.TraversalOutcome.VISIT
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

private class NadelVirtualTypeValidationContext {
    /**
     * The previously visited elements by backing type name.
     */
    private val visitedByBacking: MutableMap<String, NadelServiceSchemaElement.VirtualType> = mutableMapOf()

    fun visit(element: NadelServiceSchemaElement.VirtualType): TraversalOutcome {
        val existingTraversal = visitedByBacking[element.underlying.name]

        @Suppress("CascadeIf") // Not as easy to understand
        return if (existingTraversal == null) {
            visitedByBacking[element.underlying.name] = element
            VISIT
        } else if (existingTraversal == element) {
            SKIP
        } else {
            DUPLICATED_BACKING_TYPE
        }
    }

    enum class TraversalOutcome {
        VISIT,
        DUPLICATED_BACKING_TYPE,
        SKIP,
        ;
    }
}

class NadelVirtualTypeValidation internal constructor(
    private val hydrationValidation: NadelHydrationValidation,
    private val assignableTypeValidation: NadelAssignableTypeValidation,
) {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.VirtualType,
    ): NadelSchemaValidationResult {
        with(NadelVirtualTypeValidationContext()) {
            return validate(schemaElement)
        }
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validate(
        schemaElement: NadelServiceSchemaElement.VirtualType,
    ): NadelSchemaValidationResult {
        when (visit(schemaElement)) {
            VISIT -> {}
            DUPLICATED_BACKING_TYPE -> return NadelVirtualTypeDuplicationError(schemaElement)
            SKIP -> return ok()
        }

        if (instructionDefinitions.isRenamed(schemaElement)) {
            return NadelVirtualTypeIllegalRenameError(schemaElement)
        }

        if (schemaElement.overall is GraphQLObjectType && schemaElement.underlying is GraphQLObjectType) {
            return validateType(
                service = schemaElement.service,
                virtualType = schemaElement.overall,
                backingType = schemaElement.underlying,
            )
        }

        return NadelVirtualTypeIllegalTypeError(schemaElement)
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateType(
        service: Service,
        virtualType: GraphQLObjectType,
        backingType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        return results(
            validateFields(service, virtualType, backingType),
            validateInterfaces(service, virtualType, backingType),
        )
    }

    /**
     * Renamed fields forbidden.
     *
     * Hydration fields must be valid.
     */
    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateFields(
        service: Service,
        virtualType: GraphQLObjectType,
        backingType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        return virtualType.fields.map { virtualField ->
            if (instructionDefinitions.isRenamed(virtualType, virtualField)) {
                NadelVirtualTypeRenameFieldError(
                    type = NadelServiceSchemaElement.VirtualType(
                        service = service,
                        overall = virtualType,
                        underlying = backingType,
                    ),
                    virtualField = virtualField,
                )
            } else if (instructionDefinitions.isHydrated(virtualType, virtualField)) {
                hydrationValidation.validate(
                    parent = NadelServiceSchemaElement.Object(
                        service = service,
                        overall = virtualType,
                        underlying = backingType,
                    ),
                    virtualField = virtualField,
                )
            } else {
                val backingField = backingType.getField(virtualField.name)
                if (backingField == null) {
                    NadelVirtualTypeMissingBackingFieldError(
                        type = NadelServiceSchemaElement.VirtualType(
                            service = service,
                            overall = virtualType,
                            underlying = backingType,
                        ),
                        virtualField = virtualField,
                    )
                } else {
                    validateVirtualField(
                        parent = NadelServiceSchemaElement.VirtualType(
                            service = service,
                            overall = virtualType,
                            underlying = backingType,
                        ),
                        virtualField = virtualField,
                        backingField = backingField,
                    )
                }
            }
        }.toResult()
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateVirtualField(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        return results(
            validateFieldArguments(
                parent = parent,
                virtualField = virtualField,
                backingField = backingField,
            ),
            validateFieldOutputType(
                parent = parent,
                virtualField = virtualField,
                backingField = backingField,
            ),
        )
    }

    /**
     * Output type must be another valid virtual type, otherwise exactly the same.
     */
    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateFieldOutputType(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val virtualFieldUnwrappedOutputType = virtualField.type.unwrapAll()
        val backingFieldUnwrappedOutputType = backingField.type.unwrapAll()

        if (virtualFieldUnwrappedOutputType.hasVirtualTypeDefinition()) {
            return validate(
                NadelServiceSchemaElement.VirtualType(
                    service = parent.service,
                    overall = virtualFieldUnwrappedOutputType,
                    underlying = backingFieldUnwrappedOutputType,
                ),
            )
        }

        val isOutputTypeAssignable = isOutputTypeAssignable(
            backingField = backingField,
            virtualField = virtualField,
        )

        return if (isOutputTypeAssignable) {
            ok()
        } else {
            NadelVirtualTypeIncompatibleFieldOutputTypeError(
                parent = parent,
                virtualField = virtualField,
                backingField = backingField,
            )
        }
    }

    /**
     * Arguments can be omitted, but otherwise exactly the same.
     */
    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateFieldArguments(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        return virtualField.arguments
            .asSequence()
            .map { virtualFieldArgument ->
                val backingFieldArgument = backingField.getArgument(virtualFieldArgument.name)
                if (backingFieldArgument == null) {
                    NadelVirtualTypeMissingBackingFieldArgumentError(
                        type = parent,
                        virtualField = virtualField,
                        backingField = backingField,
                        virtualFieldArgument = virtualFieldArgument,
                    )
                } else {
                    validateVirtualFieldArgument(
                        parent,
                        virtualField,
                        backingField,
                        virtualFieldArgument,
                        backingFieldArgument
                    )
                }
            }
            .toResult()
    }

    /**
     * Argument must be exactly the same.
     */
    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateVirtualFieldArgument(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        virtualFieldArgument: GraphQLArgument,
        backingFieldArgument: GraphQLArgument,
    ): NadelSchemaValidationResult {
        val isInputTypeAssignable = isInputTypeAssignable(virtualFieldArgument, backingFieldArgument)

        return if (isInputTypeAssignable) {
            ok()
        } else {
            NadelVirtualTypeIncompatibleFieldArgumentError(
                type = parent,
                virtualField = virtualField,
                backingField = backingField,
                virtualFieldArgument = virtualFieldArgument,
                backingFieldArgument = backingFieldArgument,
            )
        }
    }

    /**
     * Interfaces can be omitted, but otherwise exactly the same.
     */
    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun validateInterfaces(
        service: Service,
        virtualType: GraphQLObjectType,
        backingType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        return virtualType.interfaces.asSequence().map { virtualTypeInterface ->
            if (backingType.interfaces.contains(virtualTypeInterface)) {
                ok()
            } else {
                NadelVirtualTypeMissingInterfaceError(
                    type = NadelServiceSchemaElement.VirtualType(
                        service = service,
                        overall = virtualType,
                        underlying = backingType,
                    ),
                    virtualFieldInterface = virtualTypeInterface,
                )
            }
        }.toResult()
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun isInputTypeAssignable(
        virtualFieldArgument: GraphQLArgument,
        backingFieldArgument: GraphQLArgument,
    ): Boolean {
        val suppliedType = virtualFieldArgument.type
        val requiredType = backingFieldArgument.type

        return assignableTypeValidation.isTypeAssignable(
            suppliedType = suppliedType,
            requiredType = requiredType,
            // Note: we do not check for renames here, types must be used 1-1
            suppliedTypeName = suppliedType.unwrapAll().name,
            requiredTypeName = requiredType.unwrapAll().name,
        )
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    private fun isOutputTypeAssignable(
        backingField: GraphQLFieldDefinition,
        virtualField: GraphQLFieldDefinition,
    ): Boolean {
        // Note: the (supplied) value comes from the backing service, and that value needs to fit the (required) virtual field's type
        val suppliedType = backingField.type
        val requiredType = virtualField.type

        return assignableTypeValidation.isTypeAssignable(
            suppliedType = suppliedType,
            requiredType = requiredType,
            // Note: we do not check for renames here, types must be used 1-1
            suppliedTypeName = suppliedType.unwrapAll().name,
            requiredTypeName = requiredType.unwrapAll().name,
        )
    }
}
