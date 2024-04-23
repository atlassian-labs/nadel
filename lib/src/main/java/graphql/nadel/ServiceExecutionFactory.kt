package graphql.nadel

fun interface ServiceExecutionFactory {
    /**
     * Called to to get a function that can be called to get data for the named service
     *
     * @param serviceName the name of the service
     *
     * @return a function that can be called to get data from that service
     */
    fun getServiceExecution(serviceName: String): ServiceExecution
}
