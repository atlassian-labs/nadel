package graphql.nadel.result;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResultComplexityAggregator {
    private int totalNodeCount = 0;
    private LinkedHashMap serviceNodeCounts = new LinkedHashMap<String, Integer>();

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public LinkedHashMap getServiceNodeCounts() {
        return serviceNodeCounts;
    }

    public int getNodeCountsForService(String serviceName) {
        return (int) serviceNodeCounts.get(serviceName);
    }

    public void incrementServiceNodeCount(String serviceFieldName, Integer nodeCount) {
        serviceNodeCounts.compute(serviceFieldName, (k,v) -> (v == null) ? nodeCount : (int) v + nodeCount);
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
