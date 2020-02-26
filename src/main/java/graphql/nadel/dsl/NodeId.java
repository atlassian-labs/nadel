package graphql.nadel.dsl;

import graphql.language.Node;

import static graphql.Assert.assertNotNull;
import static java.lang.String.format;

public class NodeId {
    /**
     * Every AST node is given an id as additional data
     */
    public static final String ID = "id";

    public static String getId(Node<?> node) {
        return assertNotNull(node.getAdditionalData().get(ID), format("expected node %s to have an id", node));
    }
}
