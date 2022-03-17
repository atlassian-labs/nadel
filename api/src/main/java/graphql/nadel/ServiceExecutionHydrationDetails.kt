package graphql.nadel;

import graphql.schema.FieldCoordinates;

/**
 * This contains details about a service hydration call when a {@link ServiceExecution} is invoked.
 */
public class ServiceExecutionHydrationDetails {
    private final int timeout;
    private final int batchSize;
    private final FieldCoordinates hydrationSourceField;
    private final Service hydrationSourceService;

    public ServiceExecutionHydrationDetails(Integer timeout, Integer batchSize, Service hydrationSourceService, FieldCoordinates hydrationSourceField) {
        this.timeout = timeout == null ? -1 : timeout;
        this.batchSize = batchSize == null ? 1 : batchSize;
        this.hydrationSourceService = hydrationSourceService;
        this.hydrationSourceField = hydrationSourceField;
    }

    /**
     * @return The timeout in MS to use - a value of -1 indicates no specific timeout was set
     */
    public int getTimeout() {
        return timeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return the field that was the source of this hydration call.
     */
    public FieldCoordinates getHydrationSourceField() {
        return hydrationSourceField;
    }

    /**
     * @return the service that was the source of this hydration call.
     */
    public Service getHydrationSourceService() {
        return hydrationSourceService;
    }
}
