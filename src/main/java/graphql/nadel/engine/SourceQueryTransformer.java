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
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.engine.transformation.CopyFieldTransformation;
import graphql.nadel.engine.transformation.FieldMappingTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.Assert.assertFalse;
import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static java.util.Collections.emptyMap;

public class SourceQueryTransformer {
    private final ExecutionContext executionContext;
    private final Map<OperationDefinition.Operation, OperationDefinition> operationDefinitions = new HashMap<>();
    private Set<String> referencedFragmentNames = new HashSet<>();
    private Map<String, FragmentDefinition> transformedFragments;

    public SourceQueryTransformer(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        //TODO: transform each fragment before any field transformation
        //field transformation may remove fragment reference all together if fragment is reduced to empty selection set
        this.transformedFragments = executionContext.getFragmentsByName();
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
        operationDefinitions.compute(operationType, (ignored, existingOperation) -> {
            if (existingOperation == null) {
                existingOperation = newOperationDefinition().build();
            }
            return appendSelectionsToOperation(existingOperation, transformed);
        });
        OperationDefinition opDefinition = operationDefinitions.getOrDefault(operationType, newOperationDefinition().build());

        operationDefinitions.put(operationType, opDefinition);
        //TODO: check if operation definition of given operation type exists if yes, then merge
//        operationDefinitions.add(operationDefinition);
    }

    private OperationDefinition appendSelectionsToOperation(OperationDefinition operation, List<? extends Selection> newSelections) {
        return operation.transform(op -> {
            List<Selection> selections = new ArrayList<>(operation.getSelectionSet().getSelections());
            selections.addAll(newSelections);
            op.selectionSet(SelectionSet.newSelectionSet(selections).build());
        });
    }

    public Document delegateDocument() {
        Document.Builder newDocument = Document.newDocument();
        operationDefinitions.values().forEach(newDocument::definition);
        referencedFragmentNames.stream()
                .map(transformedFragments::get)
                .forEach(newDocument::definition);

        return newDocument.build();

    }

    private Node transformTopLevelField(Field topLevelField, OperationDefinition.Operation operation) {
        QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                //Fragments by name are not used in transformation since we are not traversing fragments
                .fragmentsByName(emptyMap())
                .variables(executionContext.getVariables())
                .root(topLevelField)
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
        private List<VariableDefinition> variableDefinitions = new LinkedList<>();
        private Map<String, Object> variables = new HashMap<>();

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
            throw new UnsupportedOperationException("Inline fragments are not supported");
            //TODO: what if all fields inside inline fragment get deleted? we should recheck it on LEAVING the node
            //(after transformations are applied); So we can see what happened. Alternative would be  to do second pass
        }

        @Override
        public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment environment) {
            referencedFragmentNames.add(environment.getFragmentSpread().getName());
        }
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
