package graphql.nadel.normalized;

import graphql.Internal;
import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.NodeUtil;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.normalized.FieldCollectorNormalizedQuery.CollectFieldResult;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Internal
public class NormalizedQueryFactory {

    /**
     * Creates a new Query execution tree for the provided query
     */
    public NormalizedQueryFromAst createNormalizedQuery(GraphQLSchema graphQLSchema, Document document, String operationName, Map<String, Object> variables) {

        NodeUtil.GetOperationResult getOperationResult = NodeUtil.getOperation(document, operationName);

        FieldCollectorNormalizedQuery fieldCollector = new FieldCollectorNormalizedQuery();
        FieldCollectorNormalizedQueryParams parameters = FieldCollectorNormalizedQueryParams
                .newParameters()
                .fragments(getOperationResult.fragmentsByName)
                .schema(graphQLSchema)
                .variables(variables)
                .build();

        CollectFieldResult roots = fieldCollector.collectFromOperation(parameters, getOperationResult.operationDefinition, graphQLSchema.getQueryType());

        Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId = new LinkedHashMap<>();
        Map<NormalizedQueryField, MergedField> mergedFieldsByNormalizedField = new LinkedHashMap<>();
        List<NormalizedQueryField> realRoots = new ArrayList<>();

        for (NormalizedQueryField root : roots.getChildren()) {

            MergedField mergedField = roots.getMergedFieldByNormalized().get(root);
            NormalizedQueryField realRoot = buildFieldWithChildren(root, mergedField, fieldCollector, parameters, normalizedFieldsByFieldId, mergedFieldsByNormalizedField, 1);
            fixUpParentReference(realRoot);

            updateByIdMap(realRoot, mergedField, normalizedFieldsByFieldId);
            mergedFieldsByNormalizedField.put(realRoot, mergedField);
            realRoots.add(realRoot);
        }

        return new NormalizedQueryFromAst(realRoots, normalizedFieldsByFieldId, mergedFieldsByNormalizedField);
    }

    private void fixUpParentReference(NormalizedQueryField rootNormalizedField) {
        for (NormalizedQueryField child : rootNormalizedField.getChildren()) {
            child.replaceParent(rootNormalizedField);
        }
    }


    private NormalizedQueryField buildFieldWithChildren(NormalizedQueryField field,
                                                        MergedField mergedField,
                                                        FieldCollectorNormalizedQuery fieldCollector,
                                                        FieldCollectorNormalizedQueryParams fieldCollectorNormalizedQueryParams,
                                                        Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId,
                                                        Map<NormalizedQueryField, MergedField> mergedFieldsByNormalizedField,
                                                        int curLevel) {
        CollectFieldResult fieldsWithoutChildren = fieldCollector.collectFields(fieldCollectorNormalizedQueryParams, field, mergedField, curLevel + 1);
        List<NormalizedQueryField> realChildren = new ArrayList<>();
        for (NormalizedQueryField fieldWithoutChildren : fieldsWithoutChildren.getChildren()) {
            MergedField mergedFieldForChild = fieldsWithoutChildren.getMergedFieldByNormalized().get(fieldWithoutChildren);
            NormalizedQueryField realChild = buildFieldWithChildren(fieldWithoutChildren, mergedFieldForChild, fieldCollector, fieldCollectorNormalizedQueryParams, normalizedFieldsByFieldId, mergedFieldsByNormalizedField, curLevel + 1);
            fixUpParentReference(realChild);
            mergedFieldsByNormalizedField.put(realChild, mergedFieldForChild);
            realChildren.add(realChild);

            updateByIdMap(realChild, mergedFieldForChild, normalizedFieldsByFieldId);
        }
        return field.transform(builder -> builder.children(realChildren));
    }

    private void updateByIdMap(NormalizedQueryField normalizedQueryField, MergedField mergedField, Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId) {
        for (Field astField : mergedField.getFields()) {
            String id = NodeId.getId(astField);
            normalizedFieldsByFieldId.computeIfAbsent(id, ignored -> new ArrayList<>());
            normalizedFieldsByFieldId.get(id).add(normalizedQueryField);
        }
    }
}
