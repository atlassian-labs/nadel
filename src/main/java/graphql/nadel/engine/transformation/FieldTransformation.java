package graphql.nadel.engine.transformation;

import com.sun.istack.internal.Nullable;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.util.TraversalControl;

import java.util.List;

public interface FieldTransformation {
    TraversalControl apply(QueryVisitorFieldEnvironment environment);

    /**
     * The idea here is to convert back delegate result  for the field
     * and convert it back to what it should be on merged schema side.
     * Unapply should know which fields to fetch and return.
     * E.g. (this is query mixed with pseudo nadel for brevity)
     * Client query:
     * query {
     *     fieldA <= $source.originalName {
     *          alias: fieldB
     *          hydration <= uses fieldC as a parameter
     *     }
     * }
     * Delegate query:
     * query {
     *     originalName {
     *         alias: fieldB
     *         fieldC
     *     }
     * }
     *
     *Delegate result:
     * originalName {
     *     alias: "valueB"
     *     fieldC: "valueC"
     * }
     *
     * SourceQueryTransformer will return FieldTransformation for each field in delegate query.
     * Converting back:
     * unapply should be called postOrder (for each field in delegate query there is field transformation function that has
     * some state):
     * like so:
     * /orginalName/fieldC => unapply(resultNodeFor(originalName), null) = NamedResultNode(hydration: UnresolvedHydrationField(params))
     * /orginalName/alias => unapply(resultNodeFor(originalName), null) = NamedResultNode(alias: LeafNode("valueB"))
     * /orginalName => unapply(resultNodeFor(RootExecutionNode), (two children from above)) = NamedResultNode(fieldA: ObjectExecutionResultNode(children))
     */
    default List<NamedResultNode> unapply(ObjectExecutionResultNode resultNode, @Nullable List<NamedResultNode> children) {
        //This will be an abstract function
        throw new UnsupportedOperationException("not ready yet.");
    }
}
