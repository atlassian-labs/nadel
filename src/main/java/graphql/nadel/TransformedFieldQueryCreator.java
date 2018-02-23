package graphql.nadel;

import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.nadel.dsl.FieldTransformation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.PropertyDataFetcher;
import graphql.validation.ValidationUtil;

import java.util.Arrays;

import static graphql.Assert.assertNotNull;

public class TransformedFieldQueryCreator {

    private FieldDefinition fieldDefinition;
    private FieldTransformation fieldTransformation;
    private ValidationUtil validationUtil = new ValidationUtil();


    public TransformedFieldQueryCreator(FieldDefinition fieldDefinition, FieldTransformation fieldTransformation) {
        this.fieldDefinition = fieldDefinition;
        this.fieldTransformation = fieldTransformation;
    }

    public Document createQuery(DataFetchingEnvironment dataFetchingEnvironment) {
        TypeName targetTypeName = validationUtil.getUnmodifiedType(fieldTransformation.getTargetType());
        GraphQLObjectType targetType = (GraphQLObjectType) dataFetchingEnvironment.getGraphQLSchema().getType(targetTypeName.getName());

        FieldDefinition foreignKeyField = getForeignKeyField(targetType);
        String foreignKeyName = foreignKeyField.getName();
        Object foreignKeyValue = getForeignKeyValue(dataFetchingEnvironment, foreignKeyName);

        Field rootField = createRootFieldForQuery(targetType, foreignKeyName, foreignKeyValue);
        rootField.setSelectionSet(dataFetchingEnvironment.getField().getSelectionSet());
        addAdditionalFieldsToQuery(rootField.getSelectionSet(), foreignKeyName);

        Document document = createDocumentWithQueryOperation(rootField);
        return document;
    }

    private void addAdditionalFieldsToQuery(SelectionSet selectionSet, String foreignKeyName) {
        // make sure we always query the foreign key value so that we can identify the object
        boolean containsForeignKeyField = selectionSet.getSelections().stream()
                .filter(selection -> selection instanceof Field)
                .map(selection -> (Field) selection)
                .anyMatch(field -> field.getName().equals(foreignKeyName));
        if (!containsForeignKeyField) {
            selectionSet.getSelections().add(0, new Field(foreignKeyName));
        }
    }

    private Document createDocumentWithQueryOperation(Field rootField) {
        OperationDefinition query = new OperationDefinition();
        query.setOperation(OperationDefinition.Operation.QUERY);
        query.setSelectionSet(new SelectionSet(Arrays.asList(rootField)));
        Document document = new Document();
        document.getDefinitions().add(query);
        return document;
    }

    private Field createRootFieldForQuery(GraphQLObjectType targetObjectType, String foreignKeyName, Object foreignKeyValue) {
        // currently we assume it is the name of the Type to lowerCase
        // and with argument same as foreignKey
        Field field = new Field(targetObjectType.getName().toLowerCase());
        field.getArguments().add(new Argument(foreignKeyName, new StringValue(foreignKeyValue.toString())));
        return field;
    }

    private Object getForeignKeyValue(DataFetchingEnvironment environment, String foreignKeyName) {
        PropertyDataFetcher propertyDataFetcher = new PropertyDataFetcher(foreignKeyName);
        Object foreignKeyValue = propertyDataFetcher.get(environment);
        return assertNotNull(foreignKeyValue, "foreign key value %s is null", foreignKeyName);
    }

    private FieldDefinition getForeignKeyField(GraphQLObjectType objectType) {
        //currently we assume it is 'id'
        GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition("id");
        assertNotNull(fieldDefinition, "id field for target type %s required", objectType.getName());
        return fieldDefinition.getDefinition();
    }

}
