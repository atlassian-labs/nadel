package graphql.nadel.result;

import java.util.LinkedHashMap;
import java.util.Map;
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

    public void addAndSetServiceNodeCount(String serviceFieldName, Integer nodeCount) {
        serviceNodeCountsMap.putIfAbsent(serviceFieldName, new AtomicInteger());
        serviceNodeCountsMap.get(serviceFieldName).addAndGet(nodeCount);
        totalNodeCount.addAndGet(nodeCount);
    }

    public Map<String, Object> snapshotResultComplexityData() {

        Map<String, Object> resultComplexityMap = new LinkedHashMap<>();
        resultComplexityMap.put("totalNodeCount", totalNodeCount.get());
        resultComplexityMap.put("serviceNodeCounts", serviceNodeCountsMap);

        return resultComplexityMap;
    }

    @Override
    public String toString() {
        return "ResultComplexityAggregator{" +
                "totalNodeCount=" + totalNodeCount +
                ", serviceNodeCountsMap=" + serviceNodeCountsMap +
                '}';
    }
}
