package graphql.nadel.validation

import graphql.nadel.validation.hydration.NadelDefaultHydrationDefinitionValidation
import graphql.nadel.validation.hydration.NadelHydrationArgumentTypeValidation
import graphql.nadel.validation.hydration.NadelHydrationArgumentValidation
import graphql.nadel.validation.hydration.NadelHydrationConditionValidation
import graphql.nadel.validation.hydration.NadelHydrationSourceFieldValidation
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.nadel.validation.hydration.NadelHydrationVirtualTypeValidation

abstract class NadelSchemaValidationFactory {
    fun create(): NadelSchemaValidation {
        val definitionParser = NadelInstructionDefinitionParser(
            hook = hook,
            idHydrationDefinitionParser = idHydrationDefinitionParser,
        )

        return NadelSchemaValidation(
            typeValidation = NadelTypeValidation(
                fieldValidation = fieldValidation,
                inputObjectValidation = inputObjectValidation,
                unionValidation = unionValidation,
                enumValidation = enumValidation,
                interfaceValidation = interfaceValidation,
                namespaceValidation = namespaceValidation,
                virtualTypeValidation = virtualTypeValidation,
                defaultHydrationDefinitionValidation = defaultHydrationDefinitionValidation,
            ),
            instructionDefinitionParser = definitionParser,
            hook = hook,
        )
    }

    private val idHydrationDefinitionParser = NadelIdHydrationDefinitionParser()

    private val partitionValidation = NadelPartitionValidation()

    private val typeWrappingValidation = NadelTypeWrappingValidation()

    private val assignableTypeValidation = NadelAssignableTypeValidation(typeWrappingValidation)

    private val inputObjectValidation = NadelInputObjectValidation(assignableTypeValidation)

    private val hydrationArgumentTypeValidation = NadelHydrationArgumentTypeValidation(typeWrappingValidation)
    private val hydrationArgumentValidation = NadelHydrationArgumentValidation(hydrationArgumentTypeValidation)
    private val hydrationConditionValidation = NadelHydrationConditionValidation()
    private val hydrationSourceFieldValidation = NadelHydrationSourceFieldValidation()
    private val hydrationVirtualTypeValidation = NadelHydrationVirtualTypeValidation()

    private val defaultHydrationDefinitionValidation = NadelDefaultHydrationDefinitionValidation()

    private val hydrationValidation = NadelHydrationValidation(
        argumentValidation = hydrationArgumentValidation,
        conditionValidation = hydrationConditionValidation,
        sourceFieldValidation = hydrationSourceFieldValidation,
        virtualTypeValidation = hydrationVirtualTypeValidation,
    )

    private val fieldValidation = NadelFieldValidation(
        hydrationValidation = hydrationValidation,
        partitionValidation = partitionValidation,
        assignableTypeValidation = assignableTypeValidation,
    )

    private val virtualTypeValidation = NadelVirtualTypeValidation(
        hydrationValidation = hydrationValidation,
        assignableTypeValidation = assignableTypeValidation,
    )

    private val unionValidation = NadelUnionValidation()

    private val enumValidation = NadelEnumValidation()

    private val interfaceValidation = NadelInterfaceValidation()

    private val namespaceValidation = NadelNamespaceValidation()

    open val hook: NadelSchemaValidationHook
        get() = object : NadelSchemaValidationHook() {
        }

    companion object : NadelSchemaValidationFactory()
}
