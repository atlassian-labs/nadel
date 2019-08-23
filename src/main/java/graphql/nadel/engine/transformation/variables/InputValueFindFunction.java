package graphql.nadel.engine.transformation.variables;

import graphql.PublicSpi;

import java.util.Optional;

/**
 * Called to find values by inspecting input types when using {@link graphql.nadel.engine.transformation.variables.InputValueFinder}
 */
@PublicSpi
public interface InputValueFindFunction<T> {
    /**
     * The callback method its input type information
     *
     * @param coercedValue                 the value in play
     * @param inputValueTree               the type information
     * @return an optional object as being found
     */
    Optional<T> apply(Object coercedValue, InputValueTree inputValueTree);
}
