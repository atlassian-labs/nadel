package graphql.nadel.engine;

import graphql.analysis.QueryTransformer;
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
import graphql.language.TypeName;
import graphql.language.VariableReference;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.TypeTransformation;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.Assert.assertFalse;
import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.util.TreeTransformerUtil.changeNode;
import static java.util.function.Function.identity;

public class OverallQueryTransformer {
    private final ExecutionContext executionContext;
    private final Map<OperationDefinition.Operation, OperationDefinition> operationDefinitions = new LinkedHashMap<>();
    private Set<String> referencedFragmentNames = new LinkedHashSet<>();
    private Map<String, FragmentDefinition> transformedFragments;
    private Map<Field, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
    private List<MergedField> transformedMergedFields = new ArrayList<>();

    public OverallQueryTransformer(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        //Field transformation may remove fragment reference all together if fragment is reduced to empty selection set
        //so it must be done first
        this.transformedFragments = transformFragments(executionContext.getFragmentsByName());
    }

    public void transform(List<MergedField> mergedFields, OperationDefinition.Operation operationType) {

        List<Field> selectionSetFields = new ArrayList<>();
        for (MergedField mergedField : mergedFields) {
            List<Field> fields = mergedField.getFields();
            List<Field> transformed = fields.stream().map(field -> (Field) transformTopLevelField(field, operationType))
                    .collect(Collectors.toList());
            assertFalse(operationDefinitions.containsKey(operationType), "Transform was already called for operation '%s'",
                    operationType);
            selectionSetFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        OperationDefinition definition = newOperationDefinition()
                .operation(operationType)
                .selectionSet(newSelectionSet(selectionSetFields).build())
                .build();


        operationDefinitions.put(operationType, definition);
    }

    public List<MergedField> getTransformedMergedFields() {
        return transformedMergedFields;
    }

    public Document delegateDocument() {
        Document.Builder newDocument = Document.newDocument();
        operationDefinitions.values().forEach(newDocument::definition);
        referencedFragmentNames.stream()
                .map(transformedFragments::get)
                .forEach(newDocument::definition);

        return newDocument.build();
    }

    public Map<Field, FieldTransformation> getTransformationByResultField() {
        return transformationByResultField;
    }

    private Map<String, FragmentDefinition> transformFragments(Map<String, FragmentDefinition> fragments) {
        return fragments.values().stream()
                .map(this::transformNamedFragment)
                .collect(Collectors.toMap(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformNamedFragment(FragmentDefinition fragmentDefinition) {
        QueryTransformer traversal = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) traversal.transform(new Transformer());
    }

    private Node transformTopLevelField(Field topLevelField, OperationDefinition.Operation operation) {
        QueryTransformer traversal = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(topLevelField)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(getRootTypeFromOperation(operation, executionContext.getGraphQLSchema()))
                .schema(executionContext.getGraphQLSchema())
                .build();

        return traversal.transform(new Transformer());
    }

    private GraphQLObjectType getRootTypeFromOperation(OperationDefinition.Operation operation, GraphQLSchema schema) {
        switch (operation) {
            case MUTATION:
                return assertNotNull(schema.getMutationType());
            case QUERY:
                return assertNotNull(schema.getQueryType());
            case SUBSCRIPTION:
                return assertNotNull(schema.getSubscriptionType());
            default:
                return assertShouldNeverHappen();
        }
    }

    private class Transformer implements QueryVisitor {

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
            TypeTransformation typeTransformation = typeTransformationForFragment(typeName);
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

    private TypeTransformation typeTransformationForFragment(TypeName typeName) {
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
