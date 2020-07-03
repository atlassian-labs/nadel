package graphql.nadel.result;

import graphql.Internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


@Internal
public class ResultComplexityAggregator {
    private int totalNodeCount = 0;
    private Map<String, Integer> serviceNodeCounts = new LinkedHashMap<>();

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public Map<String, Integer> getServiceNodeCounts() {
        return serviceNodeCounts;
    }

    public int getNodeCountsForService(String serviceName) {
        return serviceNodeCounts.get(serviceName);
    }

    public void incrementServiceNodeCount(String serviceFieldName, int nodeCount) {
        serviceNodeCounts.compute(serviceFieldName, (k, v) -> (v == null) ? nodeCount : v + nodeCount);
        totalNodeCount += nodeCount;
    }

    public Map<String, Object> snapshotResultComplexityData() {

        Map<String, Object> resultComplexityMap = new LinkedHashMap<>();
        resultComplexityMap.put("totalNodeCount", totalNodeCount);
        resultComplexityMap.put("serviceNodeCounts", serviceNodeCounts);

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
