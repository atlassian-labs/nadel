package graphql.nadel.hints;

import graphql.nadel.Service;

public interface RunCoerceTransform {
    /**
     * Determines whether to run graphql.nadel.enginekt.transform.NadelCoerceTransform
     *
     * @param service the service in question
     * @return true to run the coerce transform
     */
    boolean invoke(Service service);
}
