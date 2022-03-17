package graphql.nadel.hints;

import graphql.nadel.Service;

public interface LegacyOperationNamesHint {
    /**
     * Determines whether to use the old <code>nadel_2_{service}_{operationName}</code> operation names
     * for the given service.
     *
     * @param service the service we want to determine whether to send legacy operation names to.
     * @return true to use the old format
     */
    boolean invoke(Service service);
}
