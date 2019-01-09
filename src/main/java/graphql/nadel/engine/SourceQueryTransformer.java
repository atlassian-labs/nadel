package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldTransformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static graphql.execution.MergedField.newMergedField;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static java.util.Collections.singletonList;

public class SourceQueryTransformer {
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private OperationDefinition.Builder operationDefinition = newOperationDefinition();

    private SourceQueryTransformer() {
    }

    public static Document transform(List<MergedField> fields, ExecutionContext executionContext, ExecutionStepInfo executionStepInfo) {
        return new SourceQueryTransformer().transformInternal(fields, executionContext, executionStepInfo);
    }

    private Document transformInternal(List<MergedField> fields, ExecutionContext executionContext, ExecutionStepInfo executionStepInfo) {
        List<Field> topTargetSelections = fields.stream()
                .map(sourceTopField -> this.transformTopField(sourceTopField, executionContext, executionStepInfo))
                .collect(Collectors.toList());
        operationDefinition
                .operation(OperationDefinition.Operation.QUERY)
                .selectionSet(newSelectionSet(topTargetSelections).build());

        return Document.newDocument()
                .definition(operationDefinition.build())
                .build();
    }

    private Field transformTopField(MergedField mergedField, ExecutionContext executionContext, ExecutionStepInfo parentStep) {
        ExecutionStepInfo stepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(executionContext, mergedField, parentStep);
        FieldTransformation fieldTransformation = transformationForField(stepInfo);
        Field field = mergedField.getSingleField();

        Field.Builder topLevelField = Field.newField()
                .alias(field.getAlias());

        if (fieldTransformation == null || fieldTransformation.getInnerServiceHydration() != null) {
            // Copy the field as is, this is either top level field or hydrated field treated as top level (since it is an entry point)
            topLevelField.name(field.getName());
        } else if (fieldTransformation.getFieldMappingDefinition() != null) {
            // Field rename
            topLevelField.name(fieldTransformation.getFieldMappingDefinition().getInputName());
        } else {
            throw new IllegalStateException("Unsupported field transformation.");
        }
        SelectionSet selectionSet = transformSelectionSetInternal(field.getSelectionSet(), executionContext, stepInfo);
        topLevelField.selectionSet(selectionSet);
        return topLevelField.build();
    }

    private SelectionSet transformSelectionSetInternal(SelectionSet sourceSelectionSet, ExecutionContext executionContext,
                                                       ExecutionStepInfo parentStep) {
        if (sourceSelectionSet != null) {
            List<Selection> targetSelections = new ArrayList<>();
            for (Selection selection : sourceSelectionSet.getSelections()) {
                targetSelections.addAll(transformInternal(selection, executionContext, parentStep));
            }
            if (!targetSelections.isEmpty()) {
                return newSelectionSet(targetSelections).build();
            }
        }
        return null;
    }

    private List<Selection> transformInternal(Selection sourceSelection, ExecutionContext executionContext,
                                              ExecutionStepInfo parentStep) {
        if (sourceSelection instanceof Field) {
            return transformFieldInternal((Field) sourceSelection, executionContext, parentStep);
        } else {
            //either fragment or inlinefragment
            throw new UnsupportedOperationException("Fragments and inline fragments are not supported yet.");
        }
    }

    private List<Selection> transformFieldInternal(Field sourceField, ExecutionContext executionContext,
                                                   ExecutionStepInfo parentStep) {
        ExecutionStepInfo stepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(executionContext,
                newMergedField(sourceField).build(), parentStep);
        FieldTransformation fieldTransformation = transformationForField(stepInfo);
        Field.Builder targetField = Field.newField()
                .alias(sourceField.getAlias());
        if (fieldTransformation == null) {
            targetField.name(sourceField.getName());
        } else if (fieldTransformation.getInnerServiceHydration() != null) {
            throw new UnsupportedOperationException("Hydration not supported yet.");
        } else if (fieldTransformation.getFieldMappingDefinition() != null) {
            targetField.name(fieldTransformation.getFieldMappingDefinition().getInputName());
        } else {
            throw new UnsupportedOperationException("Unsupported field transformation.");
        }
        SelectionSet selectionSet = transformSelectionSetInternal(sourceField.getSelectionSet(), executionContext,
                stepInfo);
        targetField.selectionSet(selectionSet);
        return singletonList(targetField.build());
    }

    private FieldTransformation transformationForField(ExecutionStepInfo stepInfo) {
        FieldDefinition definition = stepInfo.getFieldDefinition().getDefinition();
        if (definition instanceof FieldDefinitionWithTransformation) {
            return ((FieldDefinitionWithTransformation) definition).getFieldTransformation();
        }
        return null;
    }
}
