package graphql.nadel.dsl;


import graphql.language.AbstractNode;
import graphql.language.FieldDefinition;
import graphql.language.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StitchingDsl extends AbstractNode<StitchingDsl> {

    private List<ServiceDefinition> serviceDefinitions = new ArrayList<>();
    private Map<FieldDefinition, ServiceDefinition> serviceByField = new LinkedHashMap<>();
    private Map<FieldDefinition, FieldTransformation> transformationsByField = new LinkedHashMap<>();


    public List<ServiceDefinition> getServiceDefinitions() {
        return serviceDefinitions;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(serviceDefinitions);
    }

    public Map<FieldDefinition, ServiceDefinition> getServiceByField() {
        return serviceByField;
    }


    public Map<FieldDefinition, FieldTransformation> getTransformationsByField() {
        return transformationsByField;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public StitchingDsl deepCopy() {
        return null;
    }
}
