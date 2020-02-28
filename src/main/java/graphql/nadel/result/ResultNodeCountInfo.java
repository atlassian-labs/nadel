package graphql.nadel.result;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultNodeCountInfo {
    private AtomicInteger totalNodeCount = new AtomicInteger(0);
    private Map<String, AtomicInteger> serviceNodeCounts;


    /*public ResultNodeCountInfo(AtomicInteger totalNodeCount, Map<String, AtomicInteger> serviceNodeCounts) {
        this.totalNodeCount = totalNodeCount;
        this.serviceNodeCounts = serviceNodeCounts;
    }*/

    public AtomicInteger getTotalNodeCount() {
        return totalNodeCount;
    }

    public ResultNodeCountInfo incrementServiceNodeCount(String serviceFieldName) {
        serviceNodeCounts.putIfAbsent(serviceFieldName, new AtomicInteger(0));
        serviceNodeCounts.get(serviceFieldName).incrementAndGet();
        totalNodeCount.incrementAndGet();
        return this;
    }

    public ResultNodeCountInfo setServiceNodeCount(String serviceFieldName, Integer nodeCount) {
        serviceNodeCounts.putIfAbsent(serviceFieldName, new AtomicInteger());
        serviceNodeCounts.get(serviceFieldName).set(nodeCount);
        totalNodeCount.addAndGet(nodeCount);
        return this;
    }


}
