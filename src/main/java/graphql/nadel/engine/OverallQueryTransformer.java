package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.MergedField;
import graphql.language.AstNodeAdapter;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.VariableDefinition;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.Metadata;
import graphql.nadel.engine.transformation.OverallTypeInformation;
import graphql.nadel.engine.transformation.RecordOverallTypeInformation;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.TypeInfo;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.UnderlyingTypeContext.newUnderlyingTypeContext;
import static graphql.nadel.util.Util.getTypeMappingDefinitionFor;
import static graphql.util.FpKit.groupingByUniqueKey;
import static graphql.util.FpKit.map;

public class OverallQueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(OverallQueryTransformer.class);

    private final RecordOverallTypeInformation recordOverallTypeInformation = new RecordOverallTypeInformation();


    QueryTransformationResult transformHydratedTopLevelField(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName,
            Operation operation,
            Field topLevelField,
            GraphQLCompositeType topLevelFieldTypeOverall,
            ServiceExecutionHooks serviceExecutionHooks,
            Service service,
            Object serviceContext
    ) {
        long startTime = System.currentTimeMillis();
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<String, FieldTransformation> fieldIdToTransformation = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();
        Map<String, Object> variableValues = new LinkedHashMap<>(executionContext.getVariables());
        Metadata removedFieldMap = new Metadata();


        NadelContext nadelContext = executionContext.getContext();

        SelectionSet topLevelFieldSelectionSet = transformNode(
                executionContext,
                underlyingSchema,
                topLevelField.getSelectionSet(),
                topLevelFieldTypeOverall,
                fieldIdToTransformation,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext,
                removedFieldMap
        );

        Field transformedTopLevelField = topLevelField.transform(builder -> builder.selectionSet(topLevelFieldSelectionSet));

        transformedTopLevelField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, transformedTopLevelField, topLevelFieldTypeOverall);

        List<VariableDefinition> variableDefinitions = buildReferencedVariableDefinitions(referencedVariables, executionContext.getGraphQLSchema(), typeRenameMappings);
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                underlyingSchema,
                executionContext.getFragmentsByName(),
                fieldIdToTransformation,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext,
                removedFieldMap);

        SelectionSet newOperationSelectionSet = newSelectionSet().selection(transformedTopLevelField).build();
        OperationDefinition operationDefinition = newOperationDefinition()
                .name(operationName)
                .operation(operation.getAstOperation())
                .selectionSet(newOperationSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Document newDocument = newDocument(operationDefinition, transformedFragments);

        MergedField transformedMergedField = MergedField.newMergedField(transformedTopLevelField).build();
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformHydratedTopLevelField time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                Collections.singletonList(transformedMergedField),
                typeRenameMappings,
                referencedVariableNames,
                fieldIdToTransformation,
                transformedFragments,
                variableValues,
                removedFieldMap);

    }

    QueryTransformationResult transformMergedFields(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName, Operation operation,
            List<MergedField> mergedFields,
            ServiceExecutionHooks serviceExecutionHooks,
            Service service,
            Object serviceContext
    ) {
        long startTime = System.currentTimeMillis();
        NadelContext nadelContext = executionContext.getContext();
        Set<String> fragmentsDirectlyReferenced = new LinkedHashSet<>();
        Map<String, FieldTransformation> fieldIdToTransformation = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();
        Map<String, Object> variableValues = new LinkedHashMap<>(executionContext.getVariables());
        Metadata removedFieldMap = new Metadata();

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
                        fieldIdToTransformation,
                        typeRenameMappings,
                        fragmentsDirectlyReferenced,
                        referencedVariables,
                        nadelContext,
                        serviceExecutionHooks,
                        variableValues,
                        service,
                        serviceContext,
                        removedFieldMap
                );
                // Case happens when the high level field is removed
                if (newField == null) {
                    newField = field.transform(builder -> builder.selectionSet(SelectionSet.newSelectionSet().build()));
                }
                // if all child fields of the high level field are removed then the top-level field is nulled

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

        // create a new Document including referenced Fragments
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
                fieldIdToTransformation,
                typeRenameMappings,
                fragmentsDirectlyReferenced,
                referencedVariables,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext,
                removedFieldMap);

        Document newDocument = newDocument(operationDefinition, transformedFragments);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformMergedFields time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                transformedMergedFields,
                typeRenameMappings,
                referencedVariableNames,
                fieldIdToTransformation,
                transformedFragments,
                variableValues,
                removedFieldMap);
    }

    private Document newDocument(OperationDefinition operationDefinition, Map<String, FragmentDefinition> transformedFragments) {
        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (FragmentDefinition transformedFragment : transformedFragments.values()) {
            newDocumentBuilder.definition(transformedFragment);
        }
        return newDocumentBuilder.build();
    }


    private Map<String, FragmentDefinition> transformFragments(ExecutionContext executionContext,
                                                               GraphQLSchema underlyingSchema,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<String, FieldTransformation> transformationByResultField,
                                                               Map<String, String> typeRenameMappings,
                                                               Set<String> referencedFragmentNames,
                                                               Map<String, VariableDefinition> referencedVariables,
                                                               ServiceExecutionHooks serviceExecutionHooks,
                                                               Map<String, Object> variableValues,
                                                               Service service,
                                                               Object serviceContext,
                                                               Metadata removedFieldMap) {

        Set<String> fragmentsToTransform = new LinkedHashSet<>(referencedFragmentNames);
        List<FragmentDefinition> transformedFragments = new ArrayList<>();
        while (!fragmentsToTransform.isEmpty()) {
            String fragmentName = fragmentsToTransform.iterator().next();
            Set<String> newReferencedFragments = new LinkedHashSet<>();
            FragmentDefinition transformedFragment = transformFragmentDefinition(
                    executionContext,
                    underlyingSchema,
                    fragments.get(fragmentName),
                    transformationByResultField,
                    typeRenameMappings,
                    newReferencedFragments,
                    referencedVariables,
                    serviceExecutionHooks,
                    variableValues,
                    service,
                    serviceContext,
                    removedFieldMap
            );
            transformedFragments.add(transformedFragment);
            fragmentsToTransform.addAll(newReferencedFragments);
            fragmentsToTransform.remove(fragmentName);
        }
        return groupingByUniqueKey(transformedFragments, FragmentDefinition::getName);
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           GraphQLSchema underlyingSchema,
                                                           FragmentDefinition fragmentDefinitionWithoutTypeInfo,
                                                           Map<String, FieldTransformation> transformationByResultField,
                                                           Map<String, String> typeRenameMappings,
                                                           Set<String> referencedFragmentNames,
                                                           Map<String, VariableDefinition> referencedVariables,
                                                           ServiceExecutionHooks serviceExecutionHooks,
                                                           Map<String, Object> variableValues,
                                                           Service service,
                                                           Object serviceContext,
                                                           Metadata removedFieldMap
    ) {
        NadelContext nadelContext = executionContext.getContext();

        OverallTypeInformation<FragmentDefinition> overallTypeInformation = recordOverallTypeInformation.recordOverallTypes(
                fragmentDefinitionWithoutTypeInfo,
                executionContext.getGraphQLSchema(),
                null);


        Transformer transformer = new Transformer(
                executionContext,
                underlyingSchema,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                overallTypeInformation,
                variableValues,
                service,
                serviceContext,
                removedFieldMap
        );
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(UnderlyingTypeContext.class, newUnderlyingTypeContext().build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(fragmentDefinitionWithoutTypeInfo, new TraverserVisitorStub<Node>() {
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
                                             T nodeWithoutTypeInfo,
                                             GraphQLCompositeType parentTypeOverall,
                                             Map<String, FieldTransformation> fieldIdToTransformation,
                                             Map<String, String> typeRenameMappings,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables,
                                             NadelContext nadelContext,
                                             ServiceExecutionHooks serviceExecutionHooks,
                                             Map<String, Object> variableValues,
                                             Service service,
                                             Object serviceContext,
                                             Metadata removedFieldMap) {
        OverallTypeInformation<T> overallTypeInformation = recordOverallTypeInformation.recordOverallTypes(
                nodeWithoutTypeInfo,
                executionContext.getGraphQLSchema(),
                parentTypeOverall);


        Transformer transformer = new Transformer(
                executionContext,
                underlyingSchema,
                fieldIdToTransformation,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                overallTypeInformation,
                variableValues,
                service,
                serviceContext,
                removedFieldMap
        );
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        String underlyingParentName = getUnderlyingTypeNameAndRecordMapping(parentTypeOverall, typeRenameMappings);
        GraphQLOutputType underlyingSchemaParent = (GraphQLOutputType) underlyingSchema.getType(underlyingParentName);
        rootVars.put(UnderlyingTypeContext.class, newUnderlyingTypeContext()
                .outputTypeUnderlying(underlyingSchemaParent)
                .build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(nodeWithoutTypeInfo, new TraverserVisitorStub<Node>() {
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

    private String getUnderlyingTypeNameAndRecordMapping(GraphQLCompositeType typeOverall, Map<String, String> typeRenameMappings) {
        TypeMappingDefinition mappingDefinition = getTypeMappingDefinitionFor(typeOverall);
        if (mappingDefinition == null) {
            return typeOverall.getName();
        }
        typeRenameMappings.put(mappingDefinition.getUnderlyingName(), mappingDefinition.getOverallName());
        return mappingDefinition.getUnderlyingName();
    }


}
