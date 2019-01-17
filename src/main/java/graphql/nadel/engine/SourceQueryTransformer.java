package graphql.nadel.engine;

import graphql.analysis.QueryTraversal;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.execution.ExecutionContext;
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
import graphql.nadel.engine.transformation.CopyFieldTransformation;
import graphql.nadel.engine.transformation.FieldMappingTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.util.TreeTransformerUtil;

import java.util.HashMap;
import java.util.HashSet;
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
import static java.util.function.Function.identity;

public class SourceQueryTransformer {
    private final ExecutionContext executionContext;
    private final Map<OperationDefinition.Operation, OperationDefinition> operationDefinitions = new HashMap<>();
    private Set<String> referencedFragmentNames = new HashSet<>();
    private Map<String, FragmentDefinition> transformedFragments;

    public SourceQueryTransformer(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        //Field transformation may remove fragment reference all together if fragment is reduced to empty selection set
        //so it must be done first
        this.transformedFragments = transformFragments(executionContext.getFragmentsByName());
    }

    /**
     * Transforms fields into delegate query. Fields passed here needs to be top level fields in delegate service schema.
     * It can be called multiple times for different operationType. The result will be merged into document.
     *
     * @param fields        to transform
     * @param operationType
     */
    public void transform(List<Field> fields, OperationDefinition.Operation operationType) {
        List<Field> transformed = fields.stream().map(field -> (Field) transformTopLevelField(field, operationType))
                .collect(Collectors.toList());
        assertFalse(operationDefinitions.containsKey(operationType), "Transform was already called for opearation '%s'",
                operationType);

        OperationDefinition definition = newOperationDefinition()
                .operation(operationType)
                .selectionSet(newSelectionSet(transformed).build())
                .build();


        operationDefinitions.put(operationType, definition);
    }


    public Document delegateDocument() {
        Document.Builder newDocument = Document.newDocument();
        operationDefinitions.values().forEach(newDocument::definition);
        referencedFragmentNames.stream()
                .map(transformedFragments::get)
                .forEach(newDocument::definition);

        return newDocument.build();
    }

    private Map<String, FragmentDefinition> transformFragments(Map<String, FragmentDefinition> fragments) {
        return fragments.values().stream()
                .map(this::transformNamedFragment)
                .collect(Collectors.toMap(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformNamedFragment(FragmentDefinition fragmentDefinition) {
        QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) traversal.transform(new DelegateQueryTransformer());
    }

    private Node transformTopLevelField(Field topLevelField, OperationDefinition.Operation operation) {
        QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(topLevelField)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(getRootTypeFromOperation(operation, executionContext.getGraphQLSchema()))
                .schema(executionContext.getGraphQLSchema())
                .build();

        return traversal.transform(new DelegateQueryTransformer());
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

    private class DelegateQueryTransformer implements QueryVisitor {

        @Override
        public void visitField(QueryVisitorFieldEnvironment environment) {

            //TODO: add referenced variables
            environment.getField().getArguments().stream()
                    .filter(argument -> argument.getValue() instanceof VariableReference)
                    .map(argument -> (VariableReference) argument.getValue())
                    .forEach(varRef -> varRef.getName());
            transformationForFieldDefinition(environment.getFieldDefinition().getDefinition()).apply(environment);
        }

        @Override
        public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment environment) {
            InlineFragment fragment = environment.getInlineFragment();
            TypeName typeName = fragment.getTypeCondition();
            TypeTransformation typeTransformation = typeTransformationForFragment(typeName);
            if(typeTransformation != null) {
                InlineFragment changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeTransformation.getOriginalName()).build();
                    f.typeCondition(newTypeName);
                });
                TreeTransformerUtil.changeNode(environment.getTraverserContext(), changedFragment);
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
        ObjectTypeDefinition typeDefinition = ((GraphQLObjectType)type).getDefinition();
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
            return new CopyFieldTransformation();
        }
        if (definition.getFieldMappingDefinition() != null) {
            return new FieldMappingTransformation(definition.getFieldMappingDefinition());
        } else if (definition.getInnerServiceHydration() != null) {
            throw new UnsupportedOperationException("Hydration not implemented yet.");
        }
        throw new UnsupportedOperationException("Unsupported transformation.");
    }

}
