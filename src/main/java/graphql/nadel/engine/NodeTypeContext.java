package graphql.nadel.engine;

import graphql.schema.GraphQLOutputType;

public class NodeTypeContext {

    public NodeTypeContext(GraphQLOutputType outputType, NodeTypeContext parent) {
        this.outputType = outputType;
        this.parent = parent;
    }

    private GraphQLOutputType outputType;
    private NodeTypeContext parent;


    public GraphQLOutputType getOutputType() {
        return outputType;
    }

    public NodeTypeContext getParent() {
        return parent;
    }
}
