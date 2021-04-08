package graphql.nadel.engine.transformation.variables;

import graphql.PublicSpi;

/**
 * Called to transform input type values from {@link InputValueTransformer}
 */
@PublicSpi
public interface InputValueTransform {
    /**
     * The callback method given a coercedValue and its input type information
     *
     * @param coercedValue   the coercedValue to transform
     * @param inputValueTree the type information
     * @return a possible new coercedValue
     */
    Object transformValue(Object coercedValue, InputValueTree inputValueTree);
}
