package graphql.nadel.engine.transformation.variables;

import graphql.PublicApi;
import graphql.schema.GraphQLDirectiveContainer;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeUtil;

/**
 * Complex input types such as lists and inout object types form a tree of input types
 * as you descend their values
 */
@PublicApi
public class InputValueTree {
    private final InputValueTree parent;
    private final String name;
    private final GraphQLInputType inputType;
    private final GraphQLDirectiveContainer directivesContainer;

    public InputValueTree(InputValueTree parent, String name, GraphQLInputType inputType, GraphQLDirectiveContainer directivesContainer) {
        this.parent = parent;
        this.directivesContainer = directivesContainer;
        this.inputType = inputType;
        this.name = name;
    }

    public InputValueTree unwrapOne() {
        return new InputValueTree(parent, name, (GraphQLInputType) GraphQLTypeUtil.unwrapOne(inputType), directivesContainer);
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

    public GraphQLDirectiveContainer getDirectivesContainer() {
        return directivesContainer;
    }

    @Override
    public String toString() {
        return "InputValueTree{" +
                "parent=" + parent +
                ", name='" + name + '\'' +
                ", inputType=" + inputType +
                '}';
    }
}
