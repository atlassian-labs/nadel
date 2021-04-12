package graphql.nadel.normalized;

import graphql.Assert;
import graphql.Internal;
import graphql.execution.ConditionalNodes;
import graphql.execution.MergedField;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.GraphQLUnmodifiedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Creates a the direct NormalizedQueryFields children, this means it goes only one level deep!
 * This also means the NormalizedQueryFields returned dont have any children.
 */
@Internal
public class FieldCollectorNormalizedQuery {

    private final ConditionalNodes conditionalNodes = new ConditionalNodes();

    public static class CollectFieldResult {
        private final List<NormalizedQueryField> children;
        private final Map<NormalizedQueryField, MergedField> mergedFieldByNormalized;

        public CollectFieldResult(List<NormalizedQueryField> children, Map<NormalizedQueryField, MergedField> mergedFieldByNormalized) {
            this.children = children;
            this.mergedFieldByNormalized = mergedFieldByNormalized;
        }

        public List<NormalizedQueryField> getChildren() {
            return children;
        }

        public Map<NormalizedQueryField, MergedField> getMergedFieldByNormalized() {
            return mergedFieldByNormalized;
        }
    }


    public CollectFieldResult collectFields(FieldCollectorNormalizedQueryParams parameters, NormalizedQueryField normalizedQueryField, MergedField mergedField, int level) {
        GraphQLUnmodifiedType fieldType = GraphQLTypeUtil.unwrapAll(normalizedQueryField.getFieldDefinition().getType());
        // if not composite we don't have any selectionSet because it is a Scalar or enum
        if (!(fieldType instanceof GraphQLCompositeType)) {
            return new CollectFieldResult(Collections.emptyList(), Collections.emptyMap());
        }

        // result key -> ObjectType -> NormalizedQueryField
        Map<String, Map<GraphQLObjectType, NormalizedQueryField>> subFields = new LinkedHashMap<>();
        Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField = new LinkedHashMap<>();
        List<String> visitedFragments = new ArrayList<>();
        Set<GraphQLObjectType> possibleObjects
                = new LinkedHashSet<>(resolvePossibleObjects((GraphQLCompositeType) fieldType, parameters.getGraphQLSchema()));
        for (Field field : mergedField.getFields()) {
            if (field.getSelectionSet() == null) {
                continue;
            }
            this.collectFields(parameters,
                    field.getSelectionSet(),
                    visitedFragments,
                    subFields,
                    mergedFieldByNormalizedField,
                    possibleObjects,
                    level,
                    normalizedQueryField);
        }
        List<NormalizedQueryField> children = subFieldsToList(subFields);
        return new CollectFieldResult(children, mergedFieldByNormalizedField);
    }

    public CollectFieldResult collectFromOperation(FieldCollectorNormalizedQueryParams parameters,
                                                   OperationDefinition operationDefinition,
                                                   GraphQLObjectType rootType) {
        Map<String, Map<GraphQLObjectType, NormalizedQueryField>> subFields = new LinkedHashMap<>();
        Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField = new LinkedHashMap<>();
        List<String> visitedFragments = new ArrayList<>();
        Set<GraphQLObjectType> possibleObjects = new LinkedHashSet<>();
        possibleObjects.add(rootType);
        this.collectFields(parameters, operationDefinition.getSelectionSet(), visitedFragments, subFields, mergedFieldByNormalizedField, possibleObjects, 1, null);
        List<NormalizedQueryField> children = subFieldsToList(subFields);
        return new CollectFieldResult(children, mergedFieldByNormalizedField);
    }

    private List<NormalizedQueryField> subFieldsToList(Map<String, Map<GraphQLObjectType, NormalizedQueryField>> subFields) {
        List<NormalizedQueryField> children = new ArrayList<>();
        subFields.values().forEach(setMergedFieldWTCMap -> {
            children.addAll(setMergedFieldWTCMap.values());
        });
        return children;
    }


    private void collectFields(FieldCollectorNormalizedQueryParams parameters,
                               SelectionSet selectionSet,
                               List<String> visitedFragments,
                               Map<String, Map<GraphQLObjectType, NormalizedQueryField>> result,
                               Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField,
                               Set<GraphQLObjectType> possibleObjects,
                               int level,
                               NormalizedQueryField parent) {

        for (Selection selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                collectField(parameters, result, mergedFieldByNormalizedField, (Field) selection, possibleObjects, level, parent);
            } else if (selection instanceof InlineFragment) {
                collectInlineFragment(parameters, visitedFragments, result, mergedFieldByNormalizedField, (InlineFragment) selection, possibleObjects, level, parent);
            } else if (selection instanceof FragmentSpread) {
                collectFragmentSpread(parameters, visitedFragments, result, mergedFieldByNormalizedField, (FragmentSpread) selection, possibleObjects, level, parent);
            }
        }
    }

    private void collectFragmentSpread(FieldCollectorNormalizedQueryParams parameters,
                                       List<String> visitedFragments,
                                       Map<String, Map<GraphQLObjectType, NormalizedQueryField>> result,
                                       Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField,
                                       FragmentSpread fragmentSpread,
                                       Set<GraphQLObjectType> possibleObjects,
                                       int level,
                                       NormalizedQueryField parent) {
        if (visitedFragments.contains(fragmentSpread.getName())) {
            return;
        }
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentSpread.getDirectives())) {
            return;
        }
        visitedFragments.add(fragmentSpread.getName());
        FragmentDefinition fragmentDefinition = Assert.assertNotNull(parameters.getFragmentsByName().get(fragmentSpread.getName()));

        if (!conditionalNodes.shouldInclude(parameters.getVariables(), fragmentDefinition.getDirectives())) {
            return;
        }
        GraphQLCompositeType newCondition = (GraphQLCompositeType) parameters.getGraphQLSchema().getType(fragmentDefinition.getTypeCondition().getName());
        Set<GraphQLObjectType> newConditions = narrowDownPossibleObjects(possibleObjects, newCondition, parameters.getGraphQLSchema());
        collectFields(parameters, fragmentDefinition.getSelectionSet(), visitedFragments, result, mergedFieldByNormalizedField, newConditions, level, parent);
    }

    private void collectInlineFragment(FieldCollectorNormalizedQueryParams parameters,
                                       List<String> visitedFragments,
                                       Map<String, Map<GraphQLObjectType, NormalizedQueryField>> result,
                                       Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField,
                                       InlineFragment inlineFragment,
                                       Set<GraphQLObjectType> possibleObjects,
                                       int level, NormalizedQueryField parent) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), inlineFragment.getDirectives())) {
            return;
        }
        Set<GraphQLObjectType> newPossibleObjects = possibleObjects;

        if (inlineFragment.getTypeCondition() != null) {
            GraphQLCompositeType newCondition = (GraphQLCompositeType) parameters.getGraphQLSchema().getType(inlineFragment.getTypeCondition().getName());
            newPossibleObjects = narrowDownPossibleObjects(possibleObjects, newCondition, parameters.getGraphQLSchema());

        }
        collectFields(parameters, inlineFragment.getSelectionSet(), visitedFragments, result, mergedFieldByNormalizedField, newPossibleObjects, level, parent);
    }

    private void collectField(FieldCollectorNormalizedQueryParams parameters,
                              Map<String, Map<GraphQLObjectType, NormalizedQueryField>> result,
                              Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedField,
                              Field field,
                              Set<GraphQLObjectType> objectTypes,
                              int level,
                              NormalizedQueryField parent) {
        if (!conditionalNodes.shouldInclude(parameters.getVariables(), field.getDirectives())) {
            return;
        }
//        if (field.getName().equalsIgnoreCase(TypeNameMetaFieldDef.getName())) {
//
//        }
        String name = getFieldEntryKey(field);
        result.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
        Map<GraphQLObjectType, NormalizedQueryField> existingFieldWTC = result.get(name);

        for (GraphQLObjectType objectType : objectTypes) {

            if (existingFieldWTC.containsKey(objectType)) {
                NormalizedQueryField normalizedQueryField = existingFieldWTC.get(objectType);

                MergedField mergedField1 = mergedFieldByNormalizedField.get(normalizedQueryField);
                MergedField updatedMergedField = mergedField1.transform(builder -> builder.addField(field));
                mergedFieldByNormalizedField.put(normalizedQueryField, updatedMergedField);

            } else {
                GraphQLFieldDefinition fieldDefinition;
                if (field.getName().equals(Introspection.TypeNameMetaFieldDef.getName())) {
                    fieldDefinition = Introspection.TypeNameMetaFieldDef;
                } else if (field.getName().equals(Introspection.SchemaMetaFieldDef.getName())) {
                    fieldDefinition = Introspection.SchemaMetaFieldDef;
                } else if (field.getName().equals(Introspection.TypeMetaFieldDef.getName())) {
                    fieldDefinition = Introspection.TypeMetaFieldDef;
                } else {
                    fieldDefinition = Assert.assertNotNull(objectType.getFieldDefinition(field.getName()), () -> String.format("no field with name %s found in object %s", field.getName(), objectType.getName()));
                }
                NormalizedQueryField newFieldWTC = NormalizedQueryField.newQueryExecutionField()
                        .alias(field.getAlias())
                        .arguments(field.getArguments())
                        .objectType(objectType)
                        .fieldDefinition(fieldDefinition)
                        .level(level)
                        .parent(parent)
                        .build();
                existingFieldWTC.put(objectType, newFieldWTC);
                mergedFieldByNormalizedField.put(newFieldWTC, MergedField.newMergedField(field).build());
            }
        }
    }


    private String getFieldEntryKey(Field field) {
        if (field.getAlias() != null) {
            return field.getAlias();
        } else {
            return field.getName();
        }
    }

    private Set<GraphQLObjectType> narrowDownPossibleObjects(Set<GraphQLObjectType> currentOnes,
                                                             GraphQLCompositeType typeCondition,
                                                             GraphQLSchema graphQLSchema) {

        List<GraphQLObjectType> resolvedTypeCondition = resolvePossibleObjects(typeCondition, graphQLSchema);
        if (currentOnes.size() == 0) {
            return new LinkedHashSet<>(resolvedTypeCondition);
        }

        Set<GraphQLObjectType> result = new LinkedHashSet<>(currentOnes);
        result.retainAll(resolvedTypeCondition);
        return result;
    }

    private List<GraphQLObjectType> resolvePossibleObjects(GraphQLCompositeType type, GraphQLSchema graphQLSchema) {
        if (type instanceof GraphQLObjectType) {
            return Collections.singletonList((GraphQLObjectType) type);
        } else if (type instanceof GraphQLInterfaceType) {
            return graphQLSchema.getImplementations((GraphQLInterfaceType) type);
        } else if (type instanceof GraphQLUnionType) {
            List types = ((GraphQLUnionType) type).getTypes();
            return new ArrayList<GraphQLObjectType>(types);
        } else {
            return Assert.assertShouldNeverHappen();
        }

    }

}
