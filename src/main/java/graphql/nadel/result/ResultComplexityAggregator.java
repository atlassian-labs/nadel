package graphql.nadel.result;

import graphql.Internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Internal
public class ResultComplexityAggregator {
    private AtomicInteger totalNodeCount = new AtomicInteger(0);
    private Map<String, Integer> serviceNodeCounts = Collections.synchronizedMap(new LinkedHashMap<>());

    public int getTotalNodeCount() {
        return totalNodeCount.get();
    }

    public Map<String, Integer> getServiceNodeCounts() {
        return serviceNodeCounts;
    }

    public int getNodeCountsForService(String serviceName) {
        return serviceNodeCounts.get(serviceName);
    }

    public void incrementServiceNodeCount(String serviceFieldName, int nodeCount) {
        serviceNodeCounts.compute(serviceFieldName, (k, v) -> (v == null) ? nodeCount : v + nodeCount);
        totalNodeCount.getAndAdd(nodeCount);
    }

    public Map<String, Object> snapshotResultComplexityData() {

        Map<String, Object> resultComplexityMap = new LinkedHashMap<>();
        resultComplexityMap.put("totalNodeCount", getTotalNodeCount());
        resultComplexityMap.put("serviceNodeCounts", getServiceNodeCounts());

        return resultComplexityMap;
    }


    @Override
    public String toString() {
        return "ResultComplexityAggregator{" +
                "totalNodeCount=" + totalNodeCount +
                ", serviceNodeCountsMap=" + serviceNodeCounts +
                '}';
    }
}
