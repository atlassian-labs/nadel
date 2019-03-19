package graphql.nadel.engine;

import graphql.Assert;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.FpKit;

import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

public class ExecutionStepInfoMapper {


    public ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo executionStepInfo,
                                                  GraphQLSchema overallSchema,
                                                  Map<Field, FieldTransformation> transformationMap) {
        //TODO: handle __typename
        MergedField mergedField = executionStepInfo.getField();
        if (!executionStepInfo.isListType() && transformationMap.containsKey(mergedField.getSingleField())) {
            mergedField = unapplyTransformation(transformationMap.get(mergedField.getSingleField()), mergedField);
        }
        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();
        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        GraphQLOutputType mappedFieldType = mapOutputType(fieldType, overallSchema);
        GraphQLFieldDefinition fieldDefinition = executionStepInfo.getFieldDefinition();
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(fieldDefinition.getName());

        // TODO: map path

        MergedField finalMergedField = mergedField;
        return executionStepInfo.transform(builder -> builder
                .field(finalMergedField)
                .type(mappedFieldType)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition));

    }

    private GraphQLOutputType mapOutputType(GraphQLOutputType graphQLOutputType, GraphQLSchema overallSchema) {
        if (GraphQLTypeUtil.isNotWrapped(graphQLOutputType)) {
            return assertNotNull((GraphQLOutputType) overallSchema.getType(graphQLOutputType.getName()), "type " + graphQLOutputType.getName() + " not found in overall schema");
        }
        if (GraphQLTypeUtil.isList(graphQLOutputType)) {
            return GraphQLList.list(mapOutputType((GraphQLOutputType) ((GraphQLList) graphQLOutputType).getWrappedType(), overallSchema));
        }
        if (GraphQLTypeUtil.isNonNull(graphQLOutputType)) {
            return GraphQLNonNull.nonNull(mapOutputType((GraphQLOutputType) ((GraphQLNonNull) graphQLOutputType).getWrappedType(), overallSchema));
        }
        return Assert.assertShouldNeverHappen();
    }

    private MergedField unapplyTransformation(FieldTransformation fieldTransformation, MergedField mergedField) {
        if (fieldTransformation instanceof FieldRenameTransformation) {
            String originalName = ((FieldRenameTransformation) fieldTransformation).getOriginalName();
            List<Field> fields = FpKit.map(mergedField.getFields(), field -> field.transform(builder -> builder.name(originalName)));
            return MergedField.newMergedField(fields).build();
        }
        return Assert.assertShouldNeverHappen("unexpected transformation");
    }
}
