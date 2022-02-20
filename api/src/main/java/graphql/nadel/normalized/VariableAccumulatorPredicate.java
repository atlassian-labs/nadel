package graphql.nadel.normalized;

import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;

/**
 * This predicate indicates whether a variable should be made for this field argument
 */
public interface VariableAccumulatorPredicate {
    boolean shouldMakeVariable(ExecutableNormalizedField executableNormalizedField, String argName, NormalizedInputValue normalizedInputValue);
}
