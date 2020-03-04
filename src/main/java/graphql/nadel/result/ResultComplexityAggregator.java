package graphql.nadel.result;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultComplexityAggregator {
    private AtomicInteger totalNodeCount;
    private LinkedHashMap<String, AtomicInteger> serviceNodeCountsMap;


    public ResultComplexityAggregator(AtomicInteger totalNodeCount, LinkedHashMap<String, AtomicInteger> serviceNodeCountsMap) {
        this.totalNodeCount = totalNodeCount;
        this.serviceNodeCountsMap = serviceNodeCountsMap;
    }

    public Integer getTotalNodeCount() {
        return totalNodeCount.get();
    }

    public LinkedHashMap<String, AtomicInteger> getServiceNodeCountsMap() {
        return serviceNodeCountsMap;
    }

    public void setServiceNodeCount(String serviceFieldName, Integer nodeCount) {
        serviceNodeCountsMap.putIfAbsent(serviceFieldName, new AtomicInteger());
        serviceNodeCountsMap.get(serviceFieldName).set(nodeCount);
        totalNodeCount.addAndGet(nodeCount);
    }

}
