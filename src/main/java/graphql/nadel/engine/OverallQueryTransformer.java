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
import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.nadel.Operation;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.engine.transformation.CollapseTransformation;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.TypeInfo;
import graphql.util.TraversalControl;
import graphql.util.TreeTransformerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.util.FpKit.toMapCollector;
import static graphql.nadel.util.Util.getTypeMappingDefinitionFor;
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
        Map<String, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();

        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        SelectionSet topLevelFieldSelectionSet = transformNode(
                executionContext,
                topLevelField.getSelectionSet(),
                topLevelFieldType,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext);

        Field transformedTopLevelField = topLevelField.transform(builder -> builder.selectionSet(topLevelFieldSelectionSet));

        transformedTopLevelField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, transformedTopLevelField, topLevelFieldType);

        List<VariableDefinition> variableDefinitions = buildReferencedVariableDefinitions(referencedVariables, executionContext.getGraphQLSchema(), typeRenameMappings);
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                typeRenameMappings,
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
                typeRenameMappings,
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
        Map<String, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
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
                        typeRenameMappings,
                        referencedFragmentNames,
                        referencedVariables, nadelContext);

                GraphQLOutputType fieldType = rootType.getFieldDefinition(field.getName()).getType();
                newField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, newField, fieldType);
                return newField;
            });
            transformedFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        List<VariableDefinition> variableDefinitions = buildReferencedVariableDefinitions(referencedVariables, executionContext.getGraphQLSchema(), typeRenameMappings);
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
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables);

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (String referencedFragmentName : referencedFragmentNames) {
            FragmentDefinition fragmentDefinition = transformedFragments.get(referencedFragmentName);
            newDocumentBuilder.definition(fragmentDefinition);
        }

        Document newDocument = newDocumentBuilder.build();
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                transformedMergedFields,
                typeRenameMappings, referencedVariableNames,
                transformationByResultField,
                transformedFragments
        );
    }


    private Map<String, FragmentDefinition> transformFragments(ExecutionContext executionContext,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<String, FieldTransformation> transformationByResultField,
                                                               Map<String, String> typeRenameMappings,
                                                               Set<String> referencedFragmentNames,
                                                               Map<String, VariableDefinition> referencedVariables) {
        return fragments.values().stream()
                .map(fragment -> transformFragmentDefinition(executionContext, fragment, transformationByResultField, typeRenameMappings, referencedFragmentNames, referencedVariables))
                .collect(toMapCollector(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           FragmentDefinition fragmentDefinition,
                                                           Map<String, FieldTransformation> transformationByResultField,
                                                           Map<String, String> typeRenameMappings,
                                                           Set<String> referencedFragmentNames,
                                                           Map<String, VariableDefinition> referencedVariables) {
        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(fragmentDefinition)
                //TODO: fragment definition does not need a parent at all, this will be refactored in graphql-java
                .rootParentType(executionContext.getGraphQLSchema().getQueryType())
                .schema(executionContext.getGraphQLSchema())
                .build();
        return (FragmentDefinition) transformer.transform(new Transformer(executionContext, transformationByResultField, typeRenameMappings, referencedFragmentNames, referencedVariables, nadelContext));
    }

    private List<VariableDefinition> buildReferencedVariableDefinitions(Map<String, VariableDefinition> referencedVariables, GraphQLSchema graphQLSchema, Map<String, String> typeRenameMappings) {
        List<VariableDefinition> variableDefinitions = new ArrayList<>();
        for (VariableDefinition vd : referencedVariables.values()) {
            TypeInfo typeInfo = TypeInfo.typeInfo(vd.getType());

            GraphQLType type = graphQLSchema.getType(typeInfo.getName());
            TypeMappingDefinition mappingDefinition = getTypeMappingDefinitionFor(type);
            if (mappingDefinition != null) {
                typeRenameMappings.put(mappingDefinition.getUnderlyingName(), mappingDefinition.getOverallName());

                String newName = mappingDefinition.getUnderlyingName();
                TypeInfo newTypeInfo = typeInfo.renameAs(newName);
                vd = vd.transform(builder -> builder.type(newTypeInfo.getRawType()));
            }
            variableDefinitions.add(vd);
        }
        return variableDefinitions;
    }

    private <T extends Node> T transformNode(ExecutionContext executionContext,
                                             T node,
                                             GraphQLCompositeType parentType,
                                             Map<String, FieldTransformation> transformationByResultField,
                                             Map<String, String> typeRenameMappings,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables,
                                             NadelContext nadelContext) {
        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .root(node)
                //TODO: Root parent type needs to be passed to this function, especially important for hydration
                .rootParentType(parentType)
                .schema(executionContext.getGraphQLSchema())
                .build();

        System.out.println(AstPrinter.printAst(node));
        Node newNode = transformer.transform(new Transformer(executionContext, transformationByResultField, typeRenameMappings, referencedFragmentNames, referencedVariables, nadelContext));
        //noinspection unchecked
        return (T) newNode;
    }

    private class Transformer implements QueryVisitor {

        final ExecutionContext executionContext;
        final Map<String, FieldTransformation> transformationByResultField;
        final Map<String, String> typeRenameMappings;
        final Set<String> referencedFragmentNames;
        final Map<String, VariableDefinition> referencedVariables;
        final NadelContext nadelContext;

        Transformer(ExecutionContext executionContext,
                    Map<String, FieldTransformation> transformationByResultField,
                    Map<String, String> typeRenameMappings,
                    Set<String> referencedFragmentNames,
                    Map<String, VariableDefinition> referencedVariables, NadelContext nadelContext) {
            this.executionContext = executionContext;
            this.transformationByResultField = transformationByResultField;
            this.typeRenameMappings = typeRenameMappings;
            this.referencedFragmentNames = referencedFragmentNames;
            this.referencedVariables = referencedVariables;
            this.nadelContext = nadelContext;
        }

        @Override
        public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
            assertShouldNeverHappen();
        }

        @Override
        public TraversalControl visitFieldWithControl(QueryVisitorFieldEnvironment environment) {

            OperationDefinition operationDefinition = executionContext.getOperationDefinition();
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

            GraphQLOutputType fieldOutputType = environment.getFieldDefinition().getType();
            GraphQLOutputType fieldType = (GraphQLOutputType) GraphQLTypeUtil.unwrapAll(fieldOutputType);
            TypeMappingDefinition typeMappingDefinition = typeTransformation(executionContext, fieldType.getName());
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
            }


            FieldTransformation transformation = createTransformation(environment);
            if (transformation != null) {
                //
                // major side effect alert - we are relying on transformation to call TreeTransformerUtil.changeNode
                // inside itself here
                //
                TraversalControl traversalControl = transformation.apply(environment);
                Field changedField = (Field) environment.getTraverserContext().thisNode();

                String fieldId = changedField.getAdditionalData().get(FieldTransformation.NADEL_FIELD_ID);
                assertNotNull(fieldId, "nadel field metadata it is null after transformation");
                transformationByResultField.put(fieldId, transformation);

                if (transformation instanceof FieldRenameTransformation) {
                    maybeAddUnderscoreTypeName(environment, changedField, fieldType);
                }
                return traversalControl;

            } else {
                maybeAddUnderscoreTypeName(environment, field, fieldType);
            }
            return TraversalControl.CONTINUE;
        }

        private Field maybeAddUnderscoreTypeName(QueryVisitorFieldEnvironment environment, Field field, GraphQLOutputType fieldType) {
            Field changedNode = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, field, fieldType);
            if (changedNode != field) {
                TreeTransformerUtil.changeNode(environment.getTraverserContext(), changedNode);
                return changedNode;
            }
            return field;
        }

        @Override
        public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment environment) {
            InlineFragment fragment = environment.getInlineFragment();
            TypeName typeName = fragment.getTypeCondition();

            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeName);
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                InlineFragment changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
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
            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeName);
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                FragmentDefinition changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
                    f.typeCondition(newTypeName);
                });
                changeNode(environment.getTraverserContext(), changedFragment);
            }
        }

        @Override
        public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment environment) {
            referencedFragmentNames.add(environment.getFragmentSpread().getName());
        }

        private TypeMappingDefinition recordTypeRename(TypeMappingDefinition typeMappingDefinition) {
            if (typeMappingDefinition != null) {
                typeRenameMappings.put(typeMappingDefinition.getUnderlyingName(), typeMappingDefinition.getOverallName());
            }
            return typeMappingDefinition;
        }

        @SuppressWarnings("ConstantConditions")
        private TypeMappingDefinition typeTransformationForFragment(ExecutionContext executionContext, TypeName typeName) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeName.getName());
            assertTrue(type instanceof GraphQLFieldsContainer, "Expected type '%s' to be an field container type", typeName);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }


        @SuppressWarnings("ConstantConditions")
        private TypeMappingDefinition typeTransformation(ExecutionContext executionContext, String typeName) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeName);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        private TypeMappingDefinition extractAndRecordTypeMappingDefinition(GraphQLSchema graphQLSchema, GraphQLType type) {

            TypeMappingDefinition typeMappingDefinition = getTypeMappingDefinitionFor(type);
            recordTypeRename(typeMappingDefinition);

            if (type instanceof GraphQLInterfaceType) {
                GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) type;

                graphQLSchema.getImplementations(interfaceType).forEach(objectType -> {
                    TypeMappingDefinition definition = extractAndRecordTypeMappingDefinition(graphQLSchema, objectType);
                    recordTypeRename(definition);
                });
            }
            if (type instanceof GraphQLUnionType) {
                GraphQLUnionType unionType = (GraphQLUnionType) type;
                unionType.getTypes().forEach(typeMember -> {
                    TypeMappingDefinition definition = extractAndRecordTypeMappingDefinition(graphQLSchema, typeMember);
                    recordTypeRename(definition);
                });
            }
            return typeMappingDefinition;
        }
    }


    private graphql.nadel.dsl.FieldTransformation transformationDefinitionForField(FieldDefinition definition) {
        if (definition instanceof FieldDefinitionWithTransformation) {
            return ((FieldDefinitionWithTransformation) definition).getFieldTransformation();
        }
        return null;
    }

    private FieldTransformation createTransformation(QueryVisitorFieldEnvironment environment) {
        FieldDefinition fieldDefinition = environment.getFieldDefinition().getDefinition();
        graphql.nadel.dsl.FieldTransformation definition = transformationDefinitionForField(fieldDefinition);

        if (definition == null) {
            return null;
        }
        if (definition.getFieldMappingDefinition() != null) {
            return new FieldRenameTransformation(definition.getFieldMappingDefinition());
        } else if (definition.getInnerServiceHydration() != null) {
            return new HydrationTransformation(definition.getInnerServiceHydration());
        } else if (definition.getCollapseDefinition() != null) {
            return new CollapseTransformation(definition.getCollapseDefinition());
        } else {
            return assertShouldNeverHappen();
        }
    }

}
