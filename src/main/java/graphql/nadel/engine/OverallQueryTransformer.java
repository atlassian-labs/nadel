package graphql.nadel.engine;

import graphql.analysis.QueryTransformer;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentDefinitionEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.execution.ExecutionContext;
import graphql.execution.MergedField;
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
import graphql.language.VariableReference;
import graphql.nadel.Operation;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.TypeTransformation;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertTrue;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.FpKit.toMapCollector;
import static graphql.util.FpKit.map;
import static graphql.util.TreeTransformerUtil.changeNode;
import static java.util.function.Function.identity;

public class OverallQueryTransformer {


    public QueryTransformationResult transformSelectionSetInField(
            ExecutionContext executionContext,
            Field field,
            GraphQLOutputType graphQLOutputType) {
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();


        SelectionSet transformedSelectionSet = (SelectionSet) transformNode(
                executionContext,
                field.getSelectionSet(),
                (GraphQLObjectType) graphQLOutputType,
                transformationByResultField,
                referencedFragmentNames);
        Field transformedField = field.transform(builder -> builder.selectionSet(transformedSelectionSet));

        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(OperationDefinition.Operation.QUERY)
                .selectionSet(newSelectionSet().selection(transformedField).build())
                .build();


        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                referencedFragmentNames);

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        referencedFragmentNames.stream()
                .map(transformedFragments::get)
                .forEach(newDocumentBuilder::definition);

        QueryTransformationResult result = new QueryTransformationResult(
                newDocumentBuilder.build(),
                null,
                transformedField,
                transformationByResultField
        );
        return result;

    }

    public QueryTransformationResult transformMergedFields(
            ExecutionContext executionContext,
            List<MergedField> mergedFields,
            Operation operation,
            String operationName
    ) {
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();

        List<MergedField> transformedMergedFields = new ArrayList<>();
        List<Field> transformedFields = new ArrayList<>();

        for (MergedField mergedField : mergedFields) {
            List<Field> fields = mergedField.getFields();

            List<Field> transformed = map(fields, field -> (Field) transformNode(
                    executionContext,
                    field,
                    operation.getRootType(executionContext.getGraphQLSchema()),
                    transformationByResultField,
                    referencedFragmentNames));
            transformedFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(operation.getAstOperation())
                .name(operationName)
                .selectionSet(newSelectionSet(transformedFields).build())
                .build();


        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                referencedFragmentNames);

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        referencedFragmentNames.stream()
                .map(transformedFragments::get)
                .forEach(newDocumentBuilder::definition);

        QueryTransformationResult result = new QueryTransformationResult(
                newDocumentBuilder.build(),
                transformedMergedFields,
                null,
                transformationByResultField
        );

        return result;
    }


    private Map<String, FragmentDefinition> transformFragments(ExecutionContext executionContext,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<Field, FieldTransformation> transformationByResultField,
                                                               Set<String> referencedFragmentNames
    ) {
        return fragments.values().stream()
                .map(fragment -> transformFragmentDefinition(executionContext, fragment, transformationByResultField, referencedFragmentNames))
                .collect(toMapCollector(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           FragmentDefinition fragmentDefinition,
                                                           Map<Field, FieldTransformation> transformationByResultField,
                                                           Set<String> referencedFragmentNames
    ) {
        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) transformer.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames));
    }

    private Node transformNode(ExecutionContext executionContext,
                               Node node,
                               GraphQLObjectType parentType,
                               Map<Field, FieldTransformation> transformationByResultField,
                               Set<String> referencedFragmentNames
    ) {
        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(node)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(parentType)
                .schema(executionContext.getGraphQLSchema())
                .build();

        return transformer.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames));
    }

    private class Transformer implements QueryVisitor {

        final ExecutionContext executionContext;
        final Map<Field, FieldTransformation> transformationByResultField;
        final Set<String> referencedFragmentNames;

        public Transformer(ExecutionContext executionContext,
                           Map<Field, FieldTransformation> transformationByResultField,
                           Set<String> referencedFragmentNames) {
            this.executionContext = executionContext;
            this.transformationByResultField = transformationByResultField;
            this.referencedFragmentNames = referencedFragmentNames;
        }

        @Override
        public void visitField(QueryVisitorFieldEnvironment environment) {

            //TODO: add referenced variables
            environment.getField().getArguments().stream()
                    .filter(argument -> argument.getValue() instanceof VariableReference)
                    .map(argument -> (VariableReference) argument.getValue())
                    .forEach(VariableReference::getName);
            FieldTransformation fieldTransformation = transformationForFieldDefinition(environment.getFieldDefinition().getDefinition());
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

    private FieldTransformation transformationForFieldDefinition(FieldDefinition fieldDefinition) {
        graphql.nadel.dsl.FieldTransformation definition = transformationDefinitionForField(fieldDefinition);
        if (definition == null) {
            return null;
        }
        if (definition.getFieldMappingDefinition() != null) {
            return new FieldRenameTransformation(definition.getFieldMappingDefinition());
        } else if (definition.getInnerServiceHydration() != null) {
            return new HydrationTransformation(definition.getInnerServiceHydration());
        }
        throw new UnsupportedOperationException("Unsupported transformation.");
    }

}
