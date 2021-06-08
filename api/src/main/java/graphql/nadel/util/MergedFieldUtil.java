package graphql.nadel.util;

import graphql.Assert;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.FieldCollector;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.ResultPath;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static java.util.stream.Collectors.toList;

public class MergedFieldUtil {

    /**
     * This helper method will take a parent merged field and edit its (merged) sub selection so that only child merged fields
     * that meet the predicate will be included into that field
     *
     * @param parentField         the parent field to edit
     * @param parentFieldTypeName the name of the object type of the parent field
     * @param executionContext    the execution context (which may include fragments etc...)
     * @param fieldPredicate      the predicate to decide if a field should be included
     *
     * @return a new MergedField with the edited child sub selection
     */
    public static MergedField includeSubSelection(MergedField parentField, String parentFieldTypeName, ExecutionContext executionContext, Predicate<MergedField> fieldPredicate) {

        FieldSubSelection fieldSubSelection = getFieldSubSelection(parentField, parentFieldTypeName, executionContext);
        MergedSelectionSet mergedSelectionSet = fieldSubSelection.getMergedSelectionSet();
        List<MergedField> childFields = new ArrayList<>(mergedSelectionSet.getSubFieldsList());
        childFields.removeIf(childMergedField -> !fieldPredicate.test(childMergedField));

        List<Field> newFields = childFields.stream().map(MergedField::getSingleField).collect(toList());

        Assert.assertFalse(newFields.isEmpty(), () -> "You have not included any child sub selections which is invalid");

        SelectionSet selectionSet = SelectionSet.newSelectionSet(newFields).build();
        Field newParentField = parentField.getSingleField().transform(bld -> bld.selectionSet(selectionSet));
        return MergedField.newMergedField().addField(newParentField).build();
    }

    /**
     * This will build out the field sub selection (of merged fields) for a given parent merged fields.
     *
     * @param parentField         the parent field
     * @param parentFieldTypeName the name of the object type of the parent field
     * @param executionContext    the execution context (which may include fragments etc...)
     *
     * @return a field sub selection where the sub selection is in fact merged fields one level deep
     */
    public static FieldSubSelection getFieldSubSelection(MergedField parentField, String parentFieldTypeName, ExecutionContext executionContext) {
        FieldCollector fieldCollector = new FieldCollector();
        GraphQLObjectType parentFieldObjectType = executionContext.getGraphQLSchema().getObjectType(parentFieldTypeName);

        FieldCollectorParameters collectorParameters = FieldCollectorParameters.newParameters()
                .schema(executionContext.getGraphQLSchema())
                .objectType(parentFieldObjectType)
                .fragments(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .build();

        MergedSelectionSet mergedSelectionSet = fieldCollector.collectFields(collectorParameters, parentField.getSingleField().getSelectionSet());
        ExecutionStepInfo executionInfo = newExecutionStepInfo().type(parentFieldObjectType).path(ResultPath.rootPath()).build();

        return FieldSubSelection.newFieldSubSelection()
                .source(executionContext.getRoot())
                .localContext(executionContext.getLocalContext())
                .mergedSelectionSet(mergedSelectionSet)
                .executionInfo(executionInfo)
                .build();

    }
}
