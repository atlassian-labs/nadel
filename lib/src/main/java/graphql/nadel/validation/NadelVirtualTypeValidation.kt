package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

private class NadelVirtualTypeValidationContext {
    private val visited: MutableSet<Pair<String, String>> = mutableSetOf()

    /**
     * @return true to visit the element, false to abort
     */
    fun visit(element: NadelServiceSchemaElement.VirtualType): Boolean {
        return visited.add(element.overall.name to element.underlying.name)
    }
}

internal class NadelVirtualTypeValidation(
    private val hydrationValidation: NadelHydrationValidation,
) {
    private val typeWrappingValidation = NadelTypeWrappingValidation()

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.VirtualType,
    ): NadelSchemaValidationResult {
        with(NadelVirtualTypeValidationContext()) {
            return validate(schemaElement)
        }
    }

    context(NadelValidationContext, NadelVirtualTypeValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.VirtualType,
    ): NadelSchemaValidationResult {
        if (!visit(schemaElement)) {
            return ok()
        }

        if (schemaElement.overall is GraphQLObjectType && schemaElement.underlying is GraphQLObjectType) {
            return validateType(
                service = schemaElement.service,
                virtualType = schemaElement.overall,
                backingType = schemaElement.underlying,
            )
        }

        return NadelInvalidVirtualTypeError(schemaElement)
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
            if (virtualField.isRenamed()) {
                NadelVirtualTypeRenameFieldError(
                    type = NadelServiceSchemaElement.VirtualType(
                        service = service,
                        overall = virtualType,
                        underlying = backingType,
                    ),
                    virtualField = virtualField,
                )
            } else if (virtualField.isHydrated()) {
                hydrationValidation.validate(
                    parent = NadelServiceSchemaElement.Object(
                        service = service,
                        overall = virtualType,
                        backingType,
                    ),
                    overallField = virtualField,
                )
            } else {
                val backingField = backingType.getField(virtualField.name)
                return if (backingField == null) {
                    NadelVirtualTypeUnexpectedFieldError(
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
    context(NadelValidationContext)
    private fun validateFieldOutputType(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val virtualFieldOutputType = virtualField.type.unwrapAll()
        val backingFieldOutputType = backingField.type.unwrapAll()

        if (virtualFieldOutputType.isVirtualType()) {
            return validate(
                NadelServiceSchemaElement.VirtualType(
                    service = parent.service,
                    overall = virtualFieldOutputType,
                    underlying = backingFieldOutputType,
                ),
            )
        }

        return if (virtualFieldOutputType.name == backingFieldOutputType.name) {
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
    context(NadelValidationContext)
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
                    NadelVirtualTypeUnexpectedFieldArgumentError(
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
    context(NadelValidationContext)
    private fun validateVirtualFieldArgument(
        parent: NadelServiceSchemaElement.VirtualType,
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        virtualFieldArgument: GraphQLArgument,
        backingFieldArgument: GraphQLArgument,
    ): NadelSchemaValidationResult {
        if (virtualFieldArgument.type.unwrapAll().name == backingFieldArgument.type.unwrapAll().name) {
            val isTypeWrappingValid = typeWrappingValidation.isTypeWrappingValid(
                lhs = virtualFieldArgument.type,
                rhs = backingFieldArgument.type,
                rule = LHS_MUST_BE_STRICTER_OR_SAME,
            )

            if (isTypeWrappingValid) {
                return ok()
            }
        }

        return NadelVirtualTypeIncompatibleFieldArgumentError(
            type = parent,
            virtualField = virtualField,
            backingField = backingField,
            virtualFieldArgument = virtualFieldArgument,
            backingFieldArgument = backingFieldArgument,
        )
    }

    /**
     * Interfaces can be omitted, but otherwise exactly the same.
     */
    context(NadelValidationContext)
    private fun validateInterfaces(
        service: Service,
        virtualType: GraphQLObjectType,
        backingType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        return virtualType.interfaces.asSequence().map { virtualTypeInterface ->
            if (backingType.interfaces.contains(virtualTypeInterface)) {
                ok()
            } else {
                NadelVirtualTypeUnexpectedInterfaceError(
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
}
