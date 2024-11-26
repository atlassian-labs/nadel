package graphql.nadel.validation

import graphql.nadel.validation.hydration.NadelHydrationValidation

abstract class NadelSchemaValidationFactory {
    fun create(): NadelSchemaValidation {
        return NadelSchemaValidation(
            fieldValidation = fieldValidation,
            inputValidation = inputValidation,
            unionValidation = unionValidation,
            enumValidation = enumValidation,
            interfaceValidation = interfaceValidation,
            namespaceValidation = namespaceValidation,
            virtualTypeValidation = virtualTypeValidation,
            definitionParser = NadelDefinitionParser(
                hook = hook,
                idHydrationDefinitionParser = NadelIdHydrationDefinitionParser(),
            ),
            hook = hook,
        )
    }

    protected val hydrationValidation: NadelHydrationValidation by lazy {
        NadelHydrationValidation()
    }

    protected val fieldValidation: NadelFieldValidation by lazy {
        NadelFieldValidation(hydrationValidation)
    }

    protected val virtualTypeValidation: NadelVirtualTypeValidation by lazy {
        NadelVirtualTypeValidation(hydrationValidation)
    }

    protected val inputValidation: NadelInputValidation by lazy {
        NadelInputValidation()
    }

    protected val unionValidation: NadelUnionValidation by lazy {
        NadelUnionValidation()
    }

    protected val enumValidation: NadelEnumValidation by lazy {
        NadelEnumValidation()
    }

    protected val interfaceValidation: NadelInterfaceValidation by lazy {
        NadelInterfaceValidation()
    }

    protected val namespaceValidation: NadelNamespaceValidation by lazy {
        NadelNamespaceValidation()
    }

    protected open val hook: NadelSchemaValidationHook
        get() = object : NadelSchemaValidationHook() {
        }

    companion object : NadelSchemaValidationFactory()
}
