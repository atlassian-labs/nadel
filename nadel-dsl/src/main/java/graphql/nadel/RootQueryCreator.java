//package graphql.nadel;
//
//import graphql.analysis.QueryTraversal;
//import graphql.language.Document;
//import graphql.language.Field;
//import graphql.language.FieldDefinition;
//import graphql.language.OperationDefinition;
//import graphql.language.SelectionSet;
//import graphql.nadel.dsl.FieldTransformation;
//import graphql.nadel.dsl.ServiceDefinition;
//import graphql.nadel.dsl.StitchingDsl;
//import graphql.schema.DataFetchingEnvironment;
//
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//public class RootQueryCreator {
//
//    private ServiceDefinition serviceDefinition;
//    private StitchingDsl stitchingDsl;
//
//
//    public RootQueryCreator(ServiceDefinition serviceDefinition, StitchingDsl stitchingDsl) {
//        this.serviceDefinition = serviceDefinition;
//        this.stitchingDsl = stitchingDsl;
//    }
//
//    public Document createQuery(DataFetchingEnvironment environment) {
//
//        OperationDefinition operationDefinition = new OperationDefinition();
//        operationDefinition.setOperation(OperationDefinition.Operation.QUERY);
//        operationDefinition.setSelectionSet(new SelectionSet(Collections.singletonList(environment.getFields().get(0).deepCopy())));
//        Document document = new Document();
//        document.getDefinitions().add(operationDefinition);
//        QueryTraversal queryTraversal = new QueryTraversal(environment.getGraphQLSchema(), document, null, environment.getArguments());
//        Map<Field, FieldDefinition> fieldsToBeReplaced = new LinkedHashMap<>();
//        queryTraversal.visitPreOrder(queryVisitorEnvironment -> {
//            FieldDefinition definition = queryVisitorEnvironment.getFieldDefinition().getDefinition();
//            FieldTransformation fieldTransformation = this.stitchingDsl.getTransformationsByFieldDefinition().get(definition);
//            if (fieldTransformation != null) {
//                fieldsToBeReplaced.put(queryVisitorEnvironment.getField(), definition);
//            }
//        });
//        for (Map.Entry<Field, FieldDefinition> toReplace : fieldsToBeReplaced.entrySet()) {
//            Field field = toReplace.getKey();
//            FieldDefinition fieldDefinition = toReplace.getValue();
//            field.setName(fieldDefinition.getName());
//            field.setSelectionSet(null);
//        }
//        return document;
//    }
//
//
//}
