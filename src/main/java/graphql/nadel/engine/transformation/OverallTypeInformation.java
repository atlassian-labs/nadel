package graphql.nadel.engine.transformation;

import graphql.language.Node;

import java.util.Map;

public class OverallTypeInformation<T extends Node> {
    private final T node;
    private final Map<String, OverallTypeInfo> overallInfoById;

    public OverallTypeInformation(T node, Map<String, OverallTypeInfo> overallInfoById) {
        this.node = node;
        this.overallInfoById = overallInfoById;
    }

    public T getNode() {
        return node;
    }

    public Map<String, OverallTypeInfo> getOverallInfoById() {
        return overallInfoById;
    }
}