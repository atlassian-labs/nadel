package graphql.nadel.engine.execution.transformation.variables;

import graphql.PublicApi;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.schema.GraphQLTypeUtil;

/**
 * Complex input types such as lists and inout object types form a tree of input types
 * as you descend their values
 */
@PublicApi
public class InputValueTree {
    private final String name;
    private final GraphQLInputType inputType;
    private final GraphQLInputValueDefinition valueDefinition;
    private final InputValueTree parent;

    public InputValueTree(InputValueTree parent, String name, GraphQLInputType inputType, GraphQLInputValueDefinition valueDefinition) {
        this.parent = parent;
        this.valueDefinition = valueDefinition;
        this.inputType = inputType;
        this.name = name;
    }

    public InputValueTree unwrapOne() {
        return new InputValueTree(parent, name, (GraphQLInputType) GraphQLTypeUtil.unwrapOne(inputType), valueDefinition);
    }

    /**
     * @return the top level tree item, that is the one with no parent aka the root of the tree
     */
    public InputValueTree getTopLevel() {
        InputValueTree tree = this;
        while (tree.parent != null) {
            tree = tree.parent;
        }
        return tree;
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

    public GraphQLInputValueDefinition getValueDefinition() {
        return valueDefinition;
    }

    @Override
    public String toString() {
        return "InputValueTree{" +
                "name='" + name + '\'' +
                ", inputType=" + inputType +
                ", valueDefinition=" + valueDefinition +
                ", parent=" + parent +
                '}';
    }
}
