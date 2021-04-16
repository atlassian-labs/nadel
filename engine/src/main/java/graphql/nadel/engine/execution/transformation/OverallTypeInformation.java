package graphql.nadel.engine.execution.transformation;

import graphql.Internal;
import graphql.language.Node;

import java.util.Map;

@Internal
public class OverallTypeInformation<T extends Node> {
    private final Map<String, OverallTypeInfo> overallInfoById;

    public OverallTypeInformation(Map<String, OverallTypeInfo> overallInfoById) {
        this.overallInfoById = overallInfoById;
    }

    public Map<String, OverallTypeInfo> getOverallInfoById() {
        return overallInfoById;
    }

    public OverallTypeInfo getOverallTypeInfo(String id) {
        return overallInfoById.get(id);
    }
}
