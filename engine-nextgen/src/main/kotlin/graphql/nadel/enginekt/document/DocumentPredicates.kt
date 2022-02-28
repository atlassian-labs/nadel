package graphql.nadel.enginekt.document

import graphql.normalized.VariablePredicate

class DocumentPredicates {

    companion object {
        /**
         * A predicate that causes JSON arguments to be compiled as variables
         */
        val jsonPredicate =
            VariablePredicate { _, _, normalizedInputValue ->
                "JSON" == normalizedInputValue.unwrappedTypeName && normalizedInputValue.value != null
            }

        /**
         * A predicate that causes ALL arguments to be compiled as variables
         */
        val allVariablesPredicate =
            VariablePredicate { _, _, _ ->
                true
            }
    }
}