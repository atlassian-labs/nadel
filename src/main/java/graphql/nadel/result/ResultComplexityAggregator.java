package graphql.nadel.result;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResultComplexityAggregator {
    private Integer totalNodeCount;
    private LinkedHashMap<String, Integer> serviceNodeCountsMap;


    public ResultComplexityAggregator(Integer totalNodeCount, LinkedHashMap<String, Integer> serviceNodeCountsMap) {
        this.totalNodeCount = totalNodeCount;
        this.serviceNodeCountsMap = serviceNodeCountsMap;
    }

    public Integer getTotalNodeCount() {
        return totalNodeCount;
    }

    public LinkedHashMap<String, Integer> getServiceNodeCountsMap() {
        return serviceNodeCountsMap;
    }

    public void addAndSetServiceNodeCount(String serviceFieldName, Integer nodeCount) {
        serviceNodeCountsMap.putIfAbsent(serviceFieldName, 0);
        serviceNodeCountsMap.put(serviceFieldName, serviceNodeCountsMap.get(serviceFieldName) + nodeCount);
        totalNodeCount += nodeCount;
    }

    public Map<String, Object> snapshotResultComplexityData() {

        Map<String, Object> resultComplexityMap = new LinkedHashMap<>();
        resultComplexityMap.put("totalNodeCount", totalNodeCount);
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
