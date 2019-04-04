package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.util.Util;
import graphql.schema.GraphQLOutputType;

import java.util.ArrayList;
import java.util.List;

import static graphql.Assert.assertNotNull;

/**
 * Interfaces and unions require that __typename be put on queries so we can work out what type they are on he other side
 */
class UnderscoreTypeNameUtils {

    static Field maybeAddUnderscoreTypeName(NadelContext nadelContext, Field field, GraphQLOutputType fieldType) {
        if (!Util.isInterfaceOrUnionField(fieldType)) {
            return field;
        }
        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        assertNotNull(underscoreTypeNameAlias, "We MUST have a generated __typename alias in the request context");

        SelectionSet selectionSet = field.getSelectionSet();
        Field underscoreTypeNameAliasField = Field.newField("__typename").alias(underscoreTypeNameAlias).build();
        if (selectionSet == null) {
            selectionSet = SelectionSet.newSelectionSet().selection(underscoreTypeNameAliasField).build();
        } else {
            selectionSet = selectionSet.transform(builder -> builder.selection(underscoreTypeNameAliasField));
        }
        SelectionSet newSelectionSet = selectionSet;
        field = field.transform(builder -> builder.selectionSet(newSelectionSet));
        return field;
    }

    static ExecutionResultNode maybeRemoveUnderscoreTypeName(NadelContext nadelContext, ExecutionResultNode resultNode) {
        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        return maybeRemoveUnderscoreTypeNameImpl(underscoreTypeNameAlias, resultNode);
    }

    private static ExecutionResultNode maybeRemoveUnderscoreTypeNameImpl(String underscoreTypeNameAlias, ExecutionResultNode resultNode) {
        // leaves never have children
        if (resultNode instanceof LeafExecutionResultNode) {
            return resultNode;
        }
        List<ExecutionResultNode> currentChildren = resultNode.getChildren();
        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode childNode : currentChildren) {
            if (childNode instanceof LeafExecutionResultNode) {
                LeafExecutionResultNode leaf = (LeafExecutionResultNode) childNode;
                MergedField mergedField = leaf.getFetchedValueAnalysis().getField();
                if (!isAliasedUnderscoreTpeNameField(underscoreTypeNameAlias, mergedField)) {
                    newChildren.add(childNode);
                }
            } else {
                childNode = maybeRemoveUnderscoreTypeNameImpl(underscoreTypeNameAlias, childNode);
                newChildren.add(childNode);
            }
        }
        return resultNode.withNewChildren(newChildren);
    }

    private static boolean isAliasedUnderscoreTpeNameField(String underscoreTypeNameAlias, MergedField mergedField) {
        List<Field> fields = mergedField.getFields();
        if (fields.size() == 1) {
            Field singleField = mergedField.getSingleField();
            String alias = singleField.getAlias();
            return underscoreTypeNameAlias.equals(alias);
        }
        return false;
    }
}
