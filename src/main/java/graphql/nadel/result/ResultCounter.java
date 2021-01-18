package graphql.nadel.result;

import java.util.concurrent.atomic.AtomicInteger;

public class ResultCounter {
    private final AtomicInteger nodeCount;
    private final AtomicInteger fieldRenameCount;
    private final AtomicInteger typeRenameCount;

    public ResultCounter() {
        nodeCount = new AtomicInteger();
        fieldRenameCount = new AtomicInteger();
        typeRenameCount = new AtomicInteger();
    }

    public void incrementNodeCount() {
        incrementNodeCount(1);
    }

    public void incrementFieldRenameCount() {
        incrementFieldRenameCount(1);
    }

    public void incrementTypeRenameCount() {
        incrementTypeRenameCount(1);
    }

    public void decrementNodeCount() {
        incrementNodeCount(-1);
    }

    public void decrementFieldRenameCount() {
        incrementFieldRenameCount(-1);
    }

    public void decrementTypeRenameCount() {
        incrementTypeRenameCount(-1);
    }

    public void incrementNodeCount(int count) {
        nodeCount.getAndAdd(count);
    }

    public void incrementFieldRenameCount(int count) {
        fieldRenameCount.getAndAdd(count);
    }

    public void incrementTypeRenameCount(int count) {
        typeRenameCount.getAndAdd(count);
    }

    public int getNodeCount() {
        return nodeCount.get();
    }

    public int getFieldRenameCount() {
        return fieldRenameCount.get();
    }

    public int getTypeRenameCount() {
        return typeRenameCount.get();
    }

}
