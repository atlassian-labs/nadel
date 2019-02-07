package graphql.nadel.engine;

import graphql.analysis.QueryTraversal;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldEnvironment;
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


    public OverallQueryTransformer() {
        //Field transformation may remove fragment reference all together if fragment is reduced to empty selection set
        //so it must be done first
    }

    public static class QueryTransformResult {

        public List<MergedField> transformedMergedFields;
        public SelectionSet transformedSelectionSet;
        public Document document;
        public Map<Field, FieldTransformation> transformationByResultField;
    }

    public QueryTransformResult transform(ExecutionContext executionContext, SelectionSet selectionSet, GraphQLOutputType graphQLOutputType) {
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();

        List<Field> transformedFields = new ArrayList<>();


        SelectionSet transformedSelectionSet = (SelectionSet) transformNode(
                executionContext,
                selectionSet,
                (GraphQLObjectType) graphQLOutputType,
                transformationByResultField,
                referencedFragmentNames);

        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(OperationDefinition.Operation.QUERY)
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

        QueryTransformResult result = new QueryTransformResult();
        result.transformedSelectionSet = transformedSelectionSet;
        result.document = newDocumentBuilder.build();
        result.transformationByResultField = transformationByResultField;
        return result;

    }

    public QueryTransformResult transform(ExecutionContext executionContext, List<MergedField> mergedFields) {
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();

        List<MergedField> transformedMergedFields = new ArrayList<>();
        List<Field> transformedFields = new ArrayList<>();

        for (MergedField mergedField : mergedFields) {
            List<Field> fields = mergedField.getFields();

            List<Field> transformed = map(fields, field -> (Field) transformNode(
                    executionContext,
                    field,
                    executionContext.getGraphQLSchema().getQueryType(),
                    transformationByResultField,
                    referencedFragmentNames));
            transformedFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(OperationDefinition.Operation.QUERY)
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

        QueryTransformResult result = new QueryTransformResult();
        result.transformedMergedFields = transformedMergedFields;
        result.document = newDocumentBuilder.build();
        result.transformationByResultField = transformationByResultField;

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
        QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) traversal.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames));
    }

    private Node transformNode(ExecutionContext executionContext,
                               Node node,
                               GraphQLObjectType parentType,
                               Map<Field, FieldTransformation> transformationByResultField,
                               Set<String> referencedFragmentNames
    ) {
        QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(node)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(parentType)
                .schema(executionContext.getGraphQLSchema())
                .build();

        return traversal.transform(new Transformer(executionContext, transformationByResultField, referencedFragmentNames));
    }

//    private GraphQLObjectType getRootTypeFromOperation(OperationDefinition.Operation operation, GraphQLSchema schema) {
//        switch (operation) {
//            case MUTATION:
//                return assertNotNull(schema.getMutationType());
//            case QUERY:
//                return assertNotNull(schema.getQueryType());
//            case SUBSCRIPTION:
//                return assertNotNull(schema.getSubscriptionType());
//            default:
//                return assertShouldNeverHappen();
//        }
//    }

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
