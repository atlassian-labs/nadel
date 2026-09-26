package graphql.nadel.engine.document

import graphql.normalized.VariablePredicate

class DocumentPredicates {

    companion object {
        /**
         * A predicate that causes ALL arguments to be compiled as variables
         */
        val allVariablesPredicate =
            VariablePredicate { _, _, _ ->
                true
            }
    }
}
