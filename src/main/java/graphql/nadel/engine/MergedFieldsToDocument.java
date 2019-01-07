package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.util.FpKit;

import java.util.List;

import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.util.FpKit.map;

public class MergedFieldsToDocument {

    public Document mergedSelectionSetToDocument(List<MergedField> mergedFields) {
        //TODO: we need to handle variable later
        List<Field> fields = FpKit.flatList(map(mergedFields, MergedField::getFields));

        OperationDefinition operationDefinition = newOperationDefinition()
                .selectionSet(SelectionSet.newSelectionSet((List) fields).build())
                .build();
        return Document.newDocument()
                .definition(operationDefinition)
                .build();
    }

    public Document mergedSelectionSetToDocument(MergedSelectionSet mergedSelectionSet) {
        List<MergedField> mergedFields = mergedSelectionSet.getSubFieldsList();
        return mergedSelectionSetToDocument(mergedFields);
    }

}
