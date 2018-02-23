package graphql.nadel;

import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RootQueryCreator {

    private ServiceDefinition serviceDefinition;
    private StitchingDsl stitchingDsl;


    public RootQueryCreator(ServiceDefinition serviceDefinition, StitchingDsl stitchingDsl) {
       this.serviceDefinition = serviceDefinition;
        this.stitchingDsl = stitchingDsl;
    }

    public Document createQuery(DataFetchingEnvironment environment) {
        Document result = new Document();
        OperationDefinition query = new OperationDefinition();
        query.setOperation(OperationDefinition.Operation.QUERY);
        query.setSelectionSet(new SelectionSet(new ArrayList<>(environment.getFields())));
        result.setDefinitions(Arrays.asList(query));
//        query.setOperation(OperationDefinition.Operation.QUERY);
//        List<Field> copiedFields = environment.getFields().stream().map(Field::deepCopy).collect(Collectors.toList());
//        query.setSelectionSet(new SelectionSet(copiedFields));
//        Document document = new Document();
//        document.getDefinitions().add(query);
//
//        QueryTraversal queryTraversal = new QueryTraversal(environment.getGraphQLSchema(), document, null, environment.getArguments());
//
//        Map<Field, FieldDefinition> fieldsToBeReplaced = new LinkedHashMap<>();
//
//        queryTraversal.visitPreOrder(queryVisitorEnvironment -> {
//            FieldDefinition definition = queryVisitorEnvironment.getFieldDefinition().getDefinition();
//            FieldTransformation fieldTransformation = definition.getFieldTransformation();
//            if (fieldTransformation != null) {
//                fieldsToBeReplaced.put(queryVisitorEnvironment.getField(), definition);
//            }
//        });
//
//        for (Map.Entry<Field, FieldDefinition> toReplace : fieldsToBeReplaced.entrySet()) {
//            Field field = toReplace.getKey();
//            FieldDefinition fieldDefinition = toReplace.getValue();
//            field.setName(fieldDefinition.getName());
//            field.setSelectionSet(null);
//        }
//
        return result;
    }


}
