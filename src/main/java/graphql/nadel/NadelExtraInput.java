package graphql.nadel;

import graphql.Internal;

import java.util.concurrent.ForkJoinPool;

@Internal
public class NadelExtraInput {

    private final String artificialFieldsUUID;
    private final ForkJoinPool forkJoinPool;

    public NadelExtraInput(String artificialFieldsUUID, ForkJoinPool forkJoinPool) {
        this.artificialFieldsUUID = artificialFieldsUUID;
        this.forkJoinPool = forkJoinPool;
    }

    public String getArtificialFieldsUUID() {
        return artificialFieldsUUID;
    }

    public ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }
}
