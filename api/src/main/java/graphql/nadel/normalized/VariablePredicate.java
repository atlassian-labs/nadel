package graphql.nadel.normalized;

import graphql.Internal;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;

/**
 * This predicate indicates whether a variable should be made for this field argument
 */
@Internal
public interface VariablePredicate {
    /**
     * Return true if a variable should be made for this field argument
     *
     * @param executableNormalizedField the field in question
     * @param argName                   the argument on the field
     * @param normalizedInputValue      the input value for that argument
     * @return true if a variable should be made
     */
    boolean shouldMakeVariable(ExecutableNormalizedField executableNormalizedField, String argName, NormalizedInputValue normalizedInputValue);
}
