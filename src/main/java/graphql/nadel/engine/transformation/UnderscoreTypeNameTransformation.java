package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLUnionType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interfaces and Unions require the __typename to be put on the query and then removed in the final results
 */
public class UnderscoreTypeNameTransformation implements FieldTransformation {
    private static final String __TYPENAME = Introspection.TypeNameMetaFieldDef.getName();

    private final Map<String, FragmentDefinition> fragmentsByName;

    public UnderscoreTypeNameTransformation(Map<String, FragmentDefinition> fragmentsByName) {
        this.fragmentsByName = fragmentsByName;
    }

    public static boolean isInterfaceOrUnionField(QueryVisitorFieldEnvironment environment) {
        GraphQLOutputType fieldOutputType = environment.getFieldDefinition().getType();
        return fieldOutputType instanceof GraphQLInterfaceType || fieldOutputType instanceof GraphQLUnionType;
    }


    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        TraverserContext<Node> context = environment.getTraverserContext();
        Field field = environment.getField();
        SelectionSet selectionSet = field.getSelectionSet();
        SelectionSet newSelectionSet = selectionSet.transform(builder -> {
            Field underscoreTypeName = Field.newField(__TYPENAME).build();

            List<Selection> newSelections = new ArrayList<>(selectionSet.getSelections());
            // always first here
            newSelections.add(0, underscoreTypeName);
            builder.selections(newSelections);
        });
        Field addedUnderScoreTypeNameField = field.transform(builder -> builder.selectionSet(newSelectionSet));

        return TreeTransformerUtil.changeNode(context, addedUnderScoreTypeNameField);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ExecutionResultNode> T unapplyResultNode(T executionResultNode) {
        List<ExecutionResultNode> children = new ArrayList<>(executionResultNode.getChildren());
        children.removeIf(node -> isUnderscoreTypeNameNode(node, executionResultNode.getFetchedValueAnalysis()));
        return (T) executionResultNode.withNewChildren(children);
    }

    private boolean isUnderscoreTypeNameNode(ExecutionResultNode childNode, FetchedValueAnalysis fetchedValueAnalysis) {
        MergedField nonConcreteField = fetchedValueAnalysis.getField();
        //
        // we need to remove the __typename node value UNLESS they have __typename already in their query somewhere else
        //
        // remember while the field might be present N times there is only 1 merged field
        //
        // eg
        //      interfaceOrUnionField {
        //          __typename # we put this here
        //          __typename # this is valid but they put it there
        //          ... on Dog {
        //              __typename # this will also result in a single value to be merged back
        //          }
        //
        if (!FieldUtils.hasUnAliasedFieldSubSelection(__TYPENAME, nonConcreteField, fragmentsByName)) {
            if (childNode instanceof LeafExecutionResultNode) {
                LeafExecutionResultNode leaf = (LeafExecutionResultNode) childNode;
                String childFieldName = FieldUtils.resultKeyForField(leaf.getFetchedValueAnalysis().getField().getSingleField());
                return childFieldName.equals(__TYPENAME);
            }
        }
        return false;
    }

    @Override
    public MergedField unapplyMergedField(MergedField mergedField) {
        List<Field> fields = new ArrayList<>(mergedField.getFields());
        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            if (FieldUtils.hasFieldSubSelectionAtIndex(__TYPENAME, field, 0)) {
                Field newField = field.transform(builder -> {
                    SelectionSet selectionSet = field.getSelectionSet();
                    List<Selection> selections = new ArrayList<>(selectionSet.getSelections());
                    selections.remove(0);

                    SelectionSet newSelectionSet = selectionSet.transform(builderSS -> builderSS.selections(selections));
                    builder.selectionSet(newSelectionSet);

                });
                fields.set(i, newField);
            }
        }
        return MergedField.newMergedField(fields).build();
    }
}
