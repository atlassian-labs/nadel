package graphql.nadel.engine.transformation;

import graphql.language.Node;

import java.util.Map;

public class OverallTypeInformation<T extends Node> {
    private final T node;
    private final Map<String, FieldTypeInfo> fieldInfoById;

    public OverallTypeInformation(T node, Map<String, FieldTypeInfo> fieldInfoById) {
        this.node = node;
        this.fieldInfoById = fieldInfoById;
    }

    public T getNode() {
        return node;
    }

    public Map<String, FieldTypeInfo> getFieldInfoById() {
        return fieldInfoById;
    }
}