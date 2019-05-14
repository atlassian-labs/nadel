package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.util.Util;
import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.List;

import static graphql.Assert.assertNotNull;
import static graphql.util.FpKit.map;

/**
 * Interfaces and unions require that __typename be put on queries so we can work out what type they are on he other side
 */
public class ArtificialFieldUtils {

    private static final String UNDERSCORE_TYPENAME = "__typename";

    public static Field maybeAddUnderscoreTypeName(NadelContext nadelContext, Field field, GraphQLOutputType fieldType) {
        if (!Util.isInterfaceOrUnionField(fieldType)) {
            return field;
        }
        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        assertNotNull(underscoreTypeNameAlias, "We MUST have a generated __typename alias in the request context");

        // check if we have already added it
        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null) {
            for (Field fld : selectionSet.getSelectionsOfType(Field.class)) {
                if (underscoreTypeNameAlias.equals(fld.getAlias())) {
                    return field;
                }
            }
        }

        Field underscoreTypeNameAliasField = Field.newField(UNDERSCORE_TYPENAME).alias(underscoreTypeNameAlias).build();
        if (selectionSet == null) {
            selectionSet = SelectionSet.newSelectionSet().selection(underscoreTypeNameAliasField).build();
        } else {
            selectionSet = selectionSet.transform(builder -> builder.selection(underscoreTypeNameAliasField));
        }
        SelectionSet newSelectionSet = selectionSet;
        field = field.transform(builder -> builder.selectionSet(newSelectionSet));
        return field;
    }

    public static Field addObjectIdentifier(NadelContext nadelContext, Field field, String objectIdentifier) {
        Field idField = Field.newField().alias(nadelContext.getObjectIdentifierAlias()).name(objectIdentifier).build();
        SelectionSet selectionSet = field.getSelectionSet().transform(builder -> builder.selection(idField));
        return field.transform(builder -> builder.selectionSet(selectionSet));
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static List<ExecutionResultNode> removeArtificialFields(NadelContext nadelContext, List<ExecutionResultNode> resultNodes) {
        return map(resultNodes, resultNode -> removeArtificialFields(nadelContext, resultNode));

    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static ExecutionResultNode removeArtificialFields(NadelContext nadelContext, ExecutionResultNode resultNode) {
        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();
        ExecutionResultNode newNode = resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (node instanceof LeafExecutionResultNode) {
                    LeafExecutionResultNode leaf = (LeafExecutionResultNode) node;
                    MergedField mergedField = leaf.getFetchedValueAnalysis().getField();

                    if (isArtificialField(nadelContext, mergedField)) {
                        return TreeTransformerUtil.deleteNode(context);
                    }
                }
                return TraversalControl.CONTINUE;
            }
        });
        return newNode;
    }

    public static boolean isArtificialField(NadelContext nadelContext, MergedField mergedField) {
        List<Field> fields = mergedField.getFields();
        // we KNOW we put the field in as a single field with alias (not merged) and hence we can assume that on the reverse
        if (fields.size() == 1) {
            Field singleField = mergedField.getSingleField();
            String alias = singleField.getAlias();
            return nadelContext.getUnderscoreTypeNameAlias().equals(alias) || nadelContext.getObjectIdentifierAlias().equals(alias);
        }
        return false;
    }

}
