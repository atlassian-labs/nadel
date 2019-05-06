package graphql.nadel.engine;

import graphql.analysis.QueryTransformer;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentDefinitionEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.execution.ExecutionContext;
import graphql.execution.MergedField;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.nadel.Operation;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.TypeTransformation;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertTrue;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.util.FpKit.toMapCollector;
import static graphql.util.FpKit.map;
import static graphql.util.TreeTransformerUtil.changeNode;
import static java.util.function.Function.identity;

public class OverallQueryTransformer {


    QueryTransformationResult transformHydratedTopLevelField(
            ExecutionContext executionContext,
            String operationName, Operation operation,
            Field topLevelField, GraphQLCompositeType topLevelFieldType
    ) {
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();

        SelectionSet topLevelFieldSelectionSet = transformNode(
                executionContext,
                topLevelField.getSelectionSet(),
                topLevelFieldType,
                transformationByResultField,
                referencedFragmentNames,
                referencedVariables);

        Field transformedTopLevelField = topLevelField.transform(builder -> builder.selectionSet(topLevelFieldSelectionSet));

        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        transformedTopLevelField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, transformedTopLevelField, topLevelFieldType);

        List<VariableDefinition> variableDefinitions = new ArrayList<>(referencedVariables.values());
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                referencedFragmentNames,
                referencedVariables);

        SelectionSet newOperationSelectionSet = newSelectionSet().selection(transformedTopLevelField).build();
        OperationDefinition operationDefinition = newOperationDefinition()
                .name(operationName)
                .operation(operation.getAstOperation())
                .selectionSet(newOperationSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (String referencedFragmentName : referencedFragmentNames) {
            FragmentDefinition fragmentDefinition = transformedFragments.get(referencedFragmentName);
            newDocumentBuilder.definition(fragmentDefinition);
        }
        Document newDocument = newDocumentBuilder.build();

        MergedField transformedMergedField = MergedField.newMergedField(transformedTopLevelField).build();
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                Collections.singletonList(transformedMergedField),
                referencedVariableNames,
                transformationByResultField,
                transformedFragments);

    }

    QueryTransformationResult transformMergedFields(
            ExecutionContext executionContext,
            String operationName, Operation operation,
            List<MergedField> mergedFields
    ) {
        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();

        List<MergedField> transformedMergedFields = new ArrayList<>();
        List<Field> transformedFields = new ArrayList<>();

        for (MergedField mergedField : mergedFields) {
            List<Field> fields = mergedField.getFields();

            List<Field> transformed = map(fields, field -> {
                GraphQLObjectType rootType = operation.getRootType(executionContext.getGraphQLSchema());
                Field newField = transformNode(
                        executionContext,
                        field,
                        rootType,
                        transformationByResultField,
                        referencedFragmentNames,
                        referencedVariables);

                GraphQLOutputType fieldType = rootType.getFieldDefinition(field.getName()).getType();
                newField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, newField, fieldType);
                return newField;
            });
            transformedFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        List<VariableDefinition> variableDefinitions = new ArrayList<>(referencedVariables.values());
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        SelectionSet newSelectionSet = newSelectionSet(transformedFields).build();

        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(operation.getAstOperation())
                .name(operationName)
                .selectionSet(newSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Map<String, FragmentDefinition> transformedFragments = transformFragments(
                executionContext,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                referencedFragmentNames,
                referencedVariables);

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (String referencedFragmentName : referencedFragmentNames) {
            FragmentDefinition fragmentDefinition = transformedFragments.get(referencedFragmentName);
            newDocumentBuilder.definition(fragmentDefinition);
        }

        Document newdocument = newDocumentBuilder.build();
        return new QueryTransformationResult(
                newdocument,
                operationDefinition,
                transformedMergedFields,
                referencedVariableNames,
                transformationByResultField,
                transformedFragments);
    }


    private Map<String, FragmentDefinition> transformFragments(ExecutionContext executionContext,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<Field, FieldTransformation> transformationByResultField,
                                                               Set<String> referencedFragmentNames,
                                                               Map<String, VariableDefinition> referencedVariables) {
        return fragments.values().stream()
                .map(fragment -> transformFragmentDefinition(executionContext, fragment, transformationByResultField, referencedFragmentNames, referencedVariables))
                .collect(toMapCollector(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           FragmentDefinition fragmentDefinition,
                                                           Map<Field, FieldTransformation> transformationByResultField,
                                                           Set<String> referencedFragmentNames,
                                                           Map<String, VariableDefinition> referencedVariables) {
        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) transformer.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames, referencedVariables));
    }

    private <T extends Node> T transformNode(ExecutionContext executionContext,
                                             T node,
                                             GraphQLCompositeType parentType,
                                             Map<Field, FieldTransformation> transformationByResultField,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables) {
        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(node)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(parentType)
                .schema(executionContext.getGraphQLSchema())
                .build();

        Node newNode = transformer.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames, referencedVariables));
        //noinspection unchecked
        return (T) newNode;
    }

    private class Transformer implements QueryVisitor {

        final ExecutionContext executionContext;
        final Map<Field, FieldTransformation> transformationByResultField;
        final Set<String> referencedFragmentNames;
        final Map<String, VariableDefinition> referencedVariables;

        Transformer(ExecutionContext executionContext,
                    Map<Field, FieldTransformation> transformationByResultField,
                    Set<String> referencedFragmentNames,
                    Map<String, VariableDefinition> referencedVariables) {
            this.executionContext = executionContext;
            this.transformationByResultField = transformationByResultField;
            this.referencedFragmentNames = referencedFragmentNames;
            this.referencedVariables = referencedVariables;
        }

        @Override
        public void visitField(QueryVisitorFieldEnvironment environment) {

            OperationDefinition operationDefinition = executionContext.getOperationDefinition();
            Map<String, FragmentDefinition> fragmentsByName = executionContext.getFragmentsByName();
            Map<String, VariableDefinition> variableDefinitions = FpKit.getByName(operationDefinition.getVariableDefinitions(), VariableDefinition::getName);

            // capture the variables that are referenced by fields
            Field field = environment.getField();
            field.getArguments().stream()
                    .map(Argument::getValue)
                    .filter(value -> value instanceof VariableReference)
                    .map(VariableReference.class::cast)
                    .forEach(variableReference -> {
                        VariableDefinition variableDefinition = variableDefinitions.get(variableReference.getName());
                        referencedVariables.put(variableDefinition.getName(), variableDefinition);
                    });

            FieldTransformation fieldTransformation = transformationForFieldDefinition(environment, fragmentsByName);
            if (fieldTransformation != null) {
                fieldTransformation.apply(environment);
                Field changedNode = (Field) environment.getTraverserContext().thisNode();

                transformationByResultField.put(changedNode, fieldTransformation);
            }
        }

        @Override
        public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment environment) {
            InlineFragment fragment = environment.getInlineFragment();
            TypeName typeName = fragment.getTypeCondition();

            TypeTransformation typeTransformation = typeTransformationForFragment(executionContext, typeName);
            if (typeTransformation != null) {
                InlineFragment changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeTransformation.getOriginalName()).build();
                    f.typeCondition(newTypeName);
                });
                changeNode(environment.getTraverserContext(), changedFragment);
            }
            //TODO: what if all fields inside inline fragment get deleted? we should recheck it on LEAVING the node
            //(after transformations are applied); So we can see what happened. Alternative would be  to do second pass
        }

        @Override
        public void visitFragmentDefinition(QueryVisitorFragmentDefinitionEnvironment environment) {
            FragmentDefinition fragment = environment.getFragmentDefinition();
            TypeName typeName = fragment.getTypeCondition();
            TypeTransformation typeTransformation = typeTransformationForFragment(executionContext, typeName);
            if (typeTransformation != null) {
                FragmentDefinition changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeTransformation.getOriginalName()).build();
                    f.typeCondition(newTypeName);
                });
                changeNode(environment.getTraverserContext(), changedFragment);
            }
        }

        @Override
        public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment environment) {
            referencedFragmentNames.add(environment.getFragmentSpread().getName());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private TypeTransformation typeTransformationForFragment(ExecutionContext executionContext, TypeName typeName) {
        GraphQLType type = executionContext.getGraphQLSchema().getType(typeName.getName());
        assertTrue(type instanceof GraphQLObjectType, "Expected type '%s' to be an object type", typeName);
        ObjectTypeDefinition typeDefinition = ((GraphQLObjectType) type).getDefinition();
        if (typeDefinition instanceof ObjectTypeDefinitionWithTransformation) {
            return ((ObjectTypeDefinitionWithTransformation) typeDefinition).getTypeTransformation();
        }
        return null;
    }

    private graphql.nadel.dsl.FieldTransformation transformationDefinitionForField(FieldDefinition definition) {
        if (definition instanceof FieldDefinitionWithTransformation) {
            return ((FieldDefinitionWithTransformation) definition).getFieldTransformation();
        }
        return null;
    }

    private FieldTransformation transformationForFieldDefinition(QueryVisitorFieldEnvironment environment, Map<String, FragmentDefinition> fragmentsByName) {
        FieldDefinition fieldDefinition = environment.getFieldDefinition().getDefinition();
        FieldTransformation fieldTransformation = null;
        graphql.nadel.dsl.FieldTransformation definition = transformationDefinitionForField(fieldDefinition);
        if (definition != null) {
            if (definition.getFieldMappingDefinition() != null) {
                fieldTransformation = new FieldRenameTransformation(definition.getFieldMappingDefinition());
            } else if (definition.getInnerServiceHydration() != null) {
                fieldTransformation = new HydrationTransformation(definition.getInnerServiceHydration());
            }
        }
        return fieldTransformation;
    }

}
