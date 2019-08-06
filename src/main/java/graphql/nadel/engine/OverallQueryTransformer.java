package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.MergedField;
import graphql.language.AstNodeAdapter;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.nadel.Operation;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.schema.idl.TypeInfo;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformer;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.util.FpKit.toMapCollector;
import static graphql.nadel.util.Util.getTypeMappingDefinitionFor;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.util.FpKit.map;
import static graphql.util.TreeTransformerUtil.changeNode;
import static java.util.function.Function.identity;

public class OverallQueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(OverallQueryTransformer.class);

    QueryTransformationResult transformHydratedTopLevelField(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName,
            Operation operation,
            Field topLevelField,
            GraphQLCompositeType topLevelFieldType
    ) {
        long startTime = System.currentTimeMillis();
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<String, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();

        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        SelectionSet topLevelFieldSelectionSet = transformNode(
                executionContext,
                underlyingSchema,
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
                underlyingSchema,
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
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformHydratedTopLevelField time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
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
            GraphQLSchema underlyingSchema,
            String operationName, Operation operation,
            List<MergedField> mergedFields
    ) {
        long startTime = System.currentTimeMillis();
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
                        underlyingSchema,
                        field,
                        rootType,
                        transformationByResultField,
                        typeRenameMappings,
                        referencedFragmentNames,
                        referencedVariables,
                        nadelContext);

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
                underlyingSchema,
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
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformMergedFields time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
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
                                                               GraphQLSchema underlyingSchema,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<String, FieldTransformation> transformationByResultField,
                                                               Map<String, String> typeRenameMappings,
                                                               Set<String> referencedFragmentNames,
                                                               Map<String, VariableDefinition> referencedVariables) {
        return fragments.values().stream()
                .map(fragment -> transformFragmentDefinition(executionContext, underlyingSchema, fragment, transformationByResultField, typeRenameMappings, referencedFragmentNames, referencedVariables))
                .collect(toMapCollector(FragmentDefinition::getName, identity()));
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           GraphQLSchema underlyingSchema,
                                                           FragmentDefinition fragmentDefinition,
                                                           Map<String, FieldTransformation> transformationByResultField,
                                                           Map<String, String> typeRenameMappings,
                                                           Set<String> referencedFragmentNames,
                                                           Map<String, VariableDefinition> referencedVariables) {
        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        Transformer transformer = new Transformer(executionContext, underlyingSchema, transformationByResultField, typeRenameMappings, referencedFragmentNames, referencedVariables, nadelContext);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(NodeTypeContext.class, new NodeTypeContext(null, null));
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(fragmentDefinition, new TraverserVisitorStub<Node>() {
                    @Override
                    public TraversalControl enter(TraverserContext<Node> context) {
                        return context.thisNode().accept(context, transformer);
                    }
                },
                rootVars
        );
        //noinspection unchecked
        return (FragmentDefinition) newNode;
    }

    private List<VariableDefinition> buildReferencedVariableDefinitions(Map<String, VariableDefinition> referencedVariables,
                                                                        GraphQLSchema graphQLSchema,
                                                                        Map<String, String> typeRenameMappings) {
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
                                             GraphQLSchema underlyingSchema,
                                             T node,
                                             GraphQLCompositeType parentType,
                                             Map<String, FieldTransformation> transformationByResultField,
                                             Map<String, String> typeRenameMappings,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables,
                                             NadelContext nadelContext) {
        Transformer transformer = new Transformer(executionContext,
                underlyingSchema,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(NodeTypeContext.class, new NodeTypeContext(parentType, null));
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(node, new TraverserVisitorStub<Node>() {
                    @Override
                    public TraversalControl enter(TraverserContext<Node> context) {
                        return context.thisNode().accept(context, transformer);
                    }
                },
                rootVars
        );

        //noinspection unchecked
        return (T) newNode;
    }

    private class Transformer extends NodeVisitorStub {

        final ExecutionContext executionContext;
        final GraphQLSchema underlyingSchema;
        final Map<String, FieldTransformation> transformationByResultField;
        final Map<String, String> typeRenameMappings;
        final Set<String> referencedFragmentNames;
        final Map<String, VariableDefinition> referencedVariables;
        final NadelContext nadelContext;
        private final Map<String, VariableDefinition> variableDefinitions;

        Transformer(ExecutionContext executionContext,
                    GraphQLSchema underlyingSchema,
                    Map<String, FieldTransformation> transformationByResultField,
                    Map<String, String> typeRenameMappings,
                    Set<String> referencedFragmentNames,
                    Map<String, VariableDefinition> referencedVariables, NadelContext nadelContext) {
            this.executionContext = executionContext;
            this.underlyingSchema = underlyingSchema;
            this.transformationByResultField = transformationByResultField;
            this.typeRenameMappings = typeRenameMappings;
            this.referencedFragmentNames = referencedFragmentNames;
            this.referencedVariables = referencedVariables;
            this.nadelContext = nadelContext;
            OperationDefinition operationDefinition = executionContext.getOperationDefinition();
            this.variableDefinitions = FpKit.getByName(operationDefinition.getVariableDefinitions(), VariableDefinition::getName);
        }

        @Override
        public TraversalControl visitVariableReference(VariableReference variableReference, TraverserContext<Node> context) {
            VariableDefinition variableDefinition = variableDefinitions.get(variableReference.getName());
            referencedVariables.put(variableDefinition.getName(), variableDefinition);
            return TraversalControl.CONTINUE;
        }

        @Override
        public TraversalControl visitField(Field field, TraverserContext<Node> context) {
            if (FieldMetadataUtil.skipTraversing(field)) {
                return TraversalControl.CONTINUE;
            }
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
                return TraversalControl.CONTINUE;
            }

            FieldTransformation.ApplyEnvironment applyEnvironment = createApplyEnvironment(field, context);

            GraphQLUnmodifiedType unwrappedType = unwrapAll(applyEnvironment.getFieldDefinition().getType());
            if (unwrappedType instanceof GraphQLCompositeType) {
                context.setVar(NodeTypeContext.class, new NodeTypeContext(applyEnvironment.getFieldDefinition().getType(), typeContext));
            }



            GraphQLOutputType fieldOutputType = applyEnvironment.getFieldDefinition().getType();
            GraphQLNamedOutputType fieldType = (GraphQLNamedOutputType) GraphQLTypeUtil.unwrapAll(fieldOutputType);

            TypeMappingDefinition typeMappingDefinition = typeTransformation(executionContext, fieldType.getName());
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
            }


            FieldTransformation transformation = createTransformation(applyEnvironment);
            if (transformation != null) {
                //
                // major side effect alert - we are relying on transformation to call TreeTransformerUtil.changeNode
                // inside itself here
                //
                TraversalControl traversalControl = transformation.apply(applyEnvironment);
                Field changedField = (Field) applyEnvironment.getTraverserContext().thisNode();


                String fieldId = FieldMetadataUtil.getUniqueRootFieldId(changedField);
                transformationByResultField.put(fieldId, transformation);

                if (transformation instanceof FieldRenameTransformation) {
                    maybeAddUnderscoreTypeName(applyEnvironment, changedField, fieldType);
                }
                return traversalControl;

            } else {
                maybeAddUnderscoreTypeName(applyEnvironment, field, fieldType);
            }
            return TraversalControl.CONTINUE;
        }

        FieldTransformation.ApplyEnvironment createApplyEnvironment(Field field, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLOutputType parentFieldType = typeContext.getOutputType();
            String fieldName = field.getName();
            GraphQLFieldsContainer fieldsContainer = (GraphQLFieldsContainer) unwrapAll(parentFieldType);
            GraphQLFieldDefinition fieldDefinition = fieldsContainer.getFieldDefinition(fieldName);
            return new FieldTransformation.ApplyEnvironment(field, fieldDefinition, fieldsContainer, context);
        }


        private Field maybeAddUnderscoreTypeName(FieldTransformation.ApplyEnvironment environment, Field field, GraphQLOutputType fieldType) {
            Field changedNode = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, field, fieldType);
            if (changedNode != field) {
                TreeTransformerUtil.changeNode(environment.getTraverserContext(), changedNode);
                return changedNode;
            }
            return field;
        }


        @Override
        public TraversalControl visitInlineFragment(InlineFragment inlineFragment, TraverserContext<Node> context) {
            // inline fragments are allowed not have type conditions, if so the parent type counts
            updateTypeContextForInlineFragment(inlineFragment, context);

            TypeName typeCondition = inlineFragment.getTypeCondition();

            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeCondition);
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                InlineFragment changedFragment = inlineFragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
                    f.typeCondition(newTypeName);
                });
                changeNode(context, changedFragment);
            }
            //TODO: what if all fields inside inline fragment get deleted? we should recheck it on LEAVING the node
            //(after transformations are applied); So we can see what happened. Alternative would be  to do second pass
            return TraversalControl.CONTINUE;
        }


        private void updateTypeContextForInlineFragment(InlineFragment inlineFragment, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLCompositeType fragmentCondition;
            if (inlineFragment.getTypeCondition() != null) {
                TypeName typeCondition = inlineFragment.getTypeCondition();
                fragmentCondition = (GraphQLCompositeType) executionContext.getGraphQLSchema().getType(typeCondition.getName());
            } else {
                fragmentCondition = (GraphQLCompositeType) unwrapAll(typeContext.getOutputType());
            }
            context.setVar(NodeTypeContext.class, new NodeTypeContext(fragmentCondition, typeContext));
        }

        @Override
        public TraversalControl visitFragmentDefinition(FragmentDefinition fragment, TraverserContext<Node> context) {
            TypeName typeName = fragment.getTypeCondition();
            updateTypeContextForFragmentDefinition(fragment, context);
            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeName);
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                FragmentDefinition changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
                    f.typeCondition(newTypeName);
                });
                changeNode(context, changedFragment);
            }
            return TraversalControl.CONTINUE;
        }

        private void updateTypeContextForFragmentDefinition(FragmentDefinition fragmentDefinition, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            TypeName typeCondition = fragmentDefinition.getTypeCondition();
            GraphQLCompositeType fragmentCondition = (GraphQLCompositeType) executionContext.getGraphQLSchema().getType(typeCondition.getName());
            context.setVar(NodeTypeContext.class, new NodeTypeContext(fragmentCondition, typeContext));
        }


        @Override
        public TraversalControl visitFragmentSpread(FragmentSpread fragmentSpread, TraverserContext<Node> context) {
            referencedFragmentNames.add(fragmentSpread.getName());
            return TraversalControl.CONTINUE;
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

    private FieldTransformation createTransformation(FieldTransformation.ApplyEnvironment environment) {
        FieldDefinition fieldDefinition = environment.getFieldDefinition().getDefinition();
        graphql.nadel.dsl.FieldTransformation definition = transformationDefinitionForField(fieldDefinition);

        if (definition == null) {
            return null;
        }
        if (definition.getFieldMappingDefinition() != null) {
            return new FieldRenameTransformation(definition.getFieldMappingDefinition());
        } else if (definition.getUnderlyingServiceHydration() != null) {
            return new HydrationTransformation(definition.getUnderlyingServiceHydration());
        } else {
            return assertShouldNeverHappen();
        }
    }

}
