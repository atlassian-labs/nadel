package graphql.nadel;

/**
 * This contains details about a service hydration call when a {@link ServiceExecution} is invoked.
 */
public class ServiceExecutionHydrationDetails {
    private final int timeout;
    private final int batchSize;

    public ServiceExecutionHydrationDetails(Integer timeout, Integer batchSize) {
        this.timeout = timeout == null ? -1 : timeout;
        this.batchSize = batchSize == null ? 1 : batchSize;
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
}
