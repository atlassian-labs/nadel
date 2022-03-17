package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.ScalarWiringEnvironment

class NeverWiringFactoryWithExtendedJsonScalar : NeverWiringFactory() {

    override fun providesScalar(environment: ScalarWiringEnvironment): Boolean {
        val scalarName: String? = environment.scalarTypeDefinition?.name
        return scalarName == ExtendedScalars.Json.name
    }

    override fun getScalar(environment: ScalarWiringEnvironment): GraphQLScalarType? {
        return when (environment.scalarTypeDefinition?.name) {
            ExtendedScalars.Json.name -> ExtendedScalars.Json
            else -> null
        }
    }
}

@UseHook
class `inlined-json-arguments` : EngineTestHook {
    override val wiringFactory = NeverWiringFactoryWithExtendedJsonScalar()
}

@UseHook
class `input-object-with-json-field` : EngineTestHook {
    override val wiringFactory = NeverWiringFactoryWithExtendedJsonScalar()
}

@UseHook
class `primitive-json-arguments` : EngineTestHook {
    override val wiringFactory = NeverWiringFactoryWithExtendedJsonScalar()
}

@UseHook
class `primitive-json-arguments-variables` : EngineTestHook {
    override val wiringFactory = NeverWiringFactoryWithExtendedJsonScalar()
}

open class AllDocumentVariablesHintHook : EngineTestHook {
    override val wiringFactory = NeverWiringFactoryWithExtendedJsonScalar()

    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder
    ): NadelExecutionInput.Builder {
        return builder.transformExecutionHints {
            it.allDocumentVariablesHint {
                true
            }
        }
    }
}

@UseHook
class `inlined-all-arguments` : AllDocumentVariablesHintHook() {
}

@UseHook
class `inlined-all-arguments-with-mixed-literals-and-variables` : AllDocumentVariablesHintHook() {
}

@UseHook
class `inlined-all-arguments-with-renamed-field` : AllDocumentVariablesHintHook() {
}

@UseHook
class `complex-identified-by-with-rename` : AllDocumentVariablesHintHook() {
}
