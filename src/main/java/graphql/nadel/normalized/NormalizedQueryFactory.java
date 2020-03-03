package graphql.nadel.normalized;

import graphql.Internal;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.NodeUtil;
import graphql.nadel.dsl.NodeId;
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
    public NormalizedQuery createNormalizedQuery(GraphQLSchema graphQLSchema, Document document, String operationName, Map<String, Object> variables) {

        NodeUtil.GetOperationResult getOperationResult = NodeUtil.getOperation(document, operationName);

        FieldCollectorNormalizedQuery fieldCollector = new FieldCollectorNormalizedQuery();
        FieldCollectorNormalizedQueryParams parameters = FieldCollectorNormalizedQueryParams
                .newParameters()
                .fragments(getOperationResult.fragmentsByName)
                .schema(graphQLSchema)
                .variables(variables)
                .build();

        List<NormalizedQueryField> roots = fieldCollector.collectFromOperation(parameters, getOperationResult.operationDefinition, graphQLSchema.getQueryType());

        Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId = new LinkedHashMap<>();
        List<NormalizedQueryField> realRoots = new ArrayList<>();
        for (NormalizedQueryField root : roots) {
            updateByIdMap(root, normalizedFieldsByFieldId);
            realRoots.add(buildFieldWithChildren(root, fieldCollector, parameters, normalizedFieldsByFieldId, 1));
        }

        return new NormalizedQuery(realRoots, normalizedFieldsByFieldId);
    }


    private NormalizedQueryField buildFieldWithChildren(NormalizedQueryField field,
                                                        FieldCollectorNormalizedQuery fieldCollector,
                                                        FieldCollectorNormalizedQueryParams fieldCollectorNormalizedQueryParams,
                                                        Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId,
                                                        int curLevel) {
        List<NormalizedQueryField> fieldsWithoutChildren = fieldCollector.collectFields(fieldCollectorNormalizedQueryParams, field, curLevel + 1);
        List<NormalizedQueryField> realChildren = new ArrayList<>();
        for (NormalizedQueryField fieldWithoutChildren : fieldsWithoutChildren) {
            NormalizedQueryField realChild = buildFieldWithChildren(fieldWithoutChildren, fieldCollector, fieldCollectorNormalizedQueryParams, normalizedFieldsByFieldId, curLevel + 1);
            realChildren.add(realChild);

            updateByIdMap(realChild, normalizedFieldsByFieldId);
        }
        return field.transform(builder -> builder.children(realChildren));
    }

    private void updateByIdMap(NormalizedQueryField normalizedQueryField, Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId) {
        for (Field astField : normalizedQueryField.getMergedField().getFields()) {
            String id = NodeId.getId(astField);
            normalizedFieldsByFieldId.computeIfAbsent(id, ignored -> new ArrayList<>());
            normalizedFieldsByFieldId.get(id).add(normalizedQueryField);
        }
    }
}
