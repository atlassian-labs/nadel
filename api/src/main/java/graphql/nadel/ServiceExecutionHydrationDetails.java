package graphql.nadel;

import graphql.schema.FieldCoordinates;

/**
 * This contains details about a service hydration call when a {@link ServiceExecution} is invoked.
 */
public class ServiceExecutionHydrationDetails {
    private final int timeout;
    private final int batchSize;
    private final int totalObjectsToBeHydrated;
    private final int countOfObjectsToBeHydrated;
    private final FieldCoordinates hydrationSourceField;
    private final Service hydrationSourceService;

    public ServiceExecutionHydrationDetails(Integer timeout, Integer batchSize, int totalObjectsToBeHydrated, int countOfObjectsToBeHydrated, Service hydrationSourceService, FieldCoordinates hydrationSourceField) {
        this.timeout = timeout == null ? -1 : timeout;
        this.batchSize = batchSize == null ? 1 : batchSize;
        this.totalObjectsToBeHydrated = totalObjectsToBeHydrated;
        this.countOfObjectsToBeHydrated = countOfObjectsToBeHydrated;
        this.hydrationSourceService = hydrationSourceService;
        this.hydrationSourceField = hydrationSourceField;
    }

    /**
     * @return The timeout in MS to use - a value of -1 indicates no specific timeout was set
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @return the batch size of this hydration.  {@link #getCountOfObjectsToBeHydrated()} will always be equal
     * to or less than this value
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return this represents the total number of objects that this hydration call is part of, since
     * a set of hydrated objects can be split into batches and hence result in multiple service calls.
     */
    public int getTotalObjectsToBeHydrated() {
        return totalObjectsToBeHydrated;
    }

    /**
     * @return this represents the count number of objects that this hydration call is for.
     */
    public int getCountOfObjectsToBeHydrated() {
        return countOfObjectsToBeHydrated;
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
