package graphql.nadel.dsl;


import graphql.language.AbstractNode;
import graphql.language.Node;

import java.util.ArrayList;
import java.util.List;

public class StitchingDsl extends AbstractNode<StitchingDsl> {

    private List<ServiceDefinition> serviceDefinitions = new ArrayList<>();


    public List<ServiceDefinition> getServiceDefinitions() {
        return serviceDefinitions;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(serviceDefinitions);
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
