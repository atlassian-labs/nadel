package graphql.nadel.engine.transformation.variables;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeUtil;

/**
 * Complex input types such as lists and inout object types form a tree of input types
 * as you descend their values
 */
public class InputValueTree {
    private final InputValueTree parent;
    private final GraphQLInputType inputType;
    private final String name;

    public InputValueTree(InputValueTree parent, GraphQLInputType inputType, String name) {
        this.parent = parent;
        this.inputType = inputType;
        this.name = name;
    }

    public InputValueTree unwrapOne() {
        return new InputValueTree(parent, (GraphQLInputType) GraphQLTypeUtil.unwrapOne(inputType), name);
    }

    public String getName() {
        return name;
    }

    public InputValueTree getParent() {
        return parent;
    }

    public GraphQLInputType getInputType() {
        return inputType;
    }

    @Override
    public String toString() {
        return "InputTypeTree{" +
                "name='" + name + '\'' +
                ", inputType=" + inputType +
                ", parent=" + parent +
                '}';
    }
}
