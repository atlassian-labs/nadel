//package graphql.nadel.engine;
//
//import graphql.Internal;
//import graphql.Scalars;
//import graphql.SerializationError;
//import graphql.TypeMismatchError;
//import graphql.execution.ExecutionContext;
//import graphql.execution.ExecutionPath;
//import graphql.execution.FetchedValue;
//import graphql.nadel.normalized.NormalizedQueryField;
//import graphql.schema.CoercingSerializeException;
//import graphql.schema.GraphQLEnumType;
//import graphql.schema.GraphQLList;
//import graphql.schema.GraphQLObjectType;
//import graphql.schema.GraphQLOutputType;
//import graphql.schema.GraphQLScalarType;
//import graphql.schema.GraphQLType;
//import graphql.schema.GraphQLTypeUtil;
//import graphql.util.FpKit;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
//import static graphql.Assert.assertNotNull;
//import static graphql.Assert.assertTrue;
//import static graphql.nadel.engine.FetchedValueAnalysis.FetchedValueType.ENUM;
//import static graphql.nadel.engine.FetchedValueAnalysis.FetchedValueType.LIST;
//import static graphql.nadel.engine.FetchedValueAnalysis.FetchedValueType.OBJECT;
//import static graphql.nadel.engine.FetchedValueAnalysis.FetchedValueType.SCALAR;
//import static graphql.nadel.engine.FetchedValueAnalysis.newFetchedValueAnalysis;
//import static graphql.schema.GraphQLTypeUtil.isList;
//
//@Internal
//public class FetchedValueAnalyzer {
//
//    /*
//     * scalar: the value, null and/or error
//     * enum: same as scalar
//     * list: list of X: X can be list again, list of scalars or enum or objects
//     */
//    public FetchedValueAnalysis analyzeFetchedValue(ExecutionContext executionContext,
//                                                    FetchedValue fetchedValue,
//                                                    NormalizedQueryField normalizedQueryField,
//                                                    ExecutionPath executionPath,
//                                                    List<String> fieldIds) {
//        return analyzeFetchedValueImpl(executionContext, fetchedValue,
//                fetchedValue.getFetchedValue(),
//                normalizedQueryField,
//                normalizedQueryField.getFieldDefinition().getType(),
//                executionPath,
//                fieldIds);
//    }
//
//    private FetchedValueAnalysis analyzeFetchedValueImpl(ExecutionContext executionContext,
//                                                         FetchedValue fetchedValue,
//                                                         Object toAnalyze,
//                                                         NormalizedQueryField normalizedQueryField,
//                                                         GraphQLOutputType curType,
//                                                         ExecutionPath executionPath,
//                                                         List<String> fieldIds) {
//        curType = (GraphQLOutputType) GraphQLTypeUtil.unwrapNonNull(curType);
//
//        if (isList(curType)) {
//            return analyzeList(executionContext, fetchedValue, toAnalyze, (GraphQLList) curType, normalizedQueryField, executionPath, fieldIds);
//        } else if (curType instanceof GraphQLScalarType) {
//            return analyzeScalarValue(fetchedValue, toAnalyze, (GraphQLScalarType) curType, normalizedQueryField, executionPath, fieldIds);
//        } else if (curType instanceof GraphQLEnumType) {
//            return analyzeEnumValue(fetchedValue, toAnalyze, (GraphQLEnumType) curType, normalizedQueryField, executionPath, fieldIds);
//        }
//
//        // when we are here, we have a complex type: Interface, Union or Object
//        // and we must go deeper
//        //
//        if (toAnalyze == null) {
//            return newFetchedValueAnalysis(OBJECT)
//                    .actualType(curType)
//                    .fetchedValue(fetchedValue)
//                    .executionPath(executionPath)
//                    .normalizedQueryField(normalizedQueryField)
//                    .fieldIds(fieldIds)
//                    .nullValue()
//                    .build();
//        }
//        GraphQLObjectType resolvedObjectType = resolveType(executionContext, toAnalyze, curType);
//        return newFetchedValueAnalysis(OBJECT)
//                .actualType(curType)
//                .fetchedValue(fetchedValue)
//                .executionPath(executionPath)
//                .normalizedQueryField(normalizedQueryField)
//                .fieldIds(fieldIds)
//                .completedValue(toAnalyze)
//                .resolvedType(resolvedObjectType)
//                .build();
//
//    }
//
//    private GraphQLObjectType resolveType(ExecutionContext executionContext, Object source, GraphQLType curType) {
//        if (curType instanceof GraphQLObjectType) {
//            return (GraphQLObjectType) curType;
//        }
//        NadelContext nadelContext = executionContext.getContext();
//        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
//
//        assertTrue(source instanceof Map, "The Nadel result object MUST be a Map");
//
//        Map<String, Object> sourceMap = (Map<String, Object>) source;
//        assertTrue(sourceMap.containsKey(underscoreTypeNameAlias), "The Nadel result object for interfaces and unions MUST have an aliased __typename in them");
//
//        Object typeName = sourceMap.get(underscoreTypeNameAlias);
//        assertNotNull(typeName, "The Nadel result object for interfaces and unions MUST have an aliased__typename with a non null value in them");
//
//        GraphQLObjectType objectType = executionContext.getGraphQLSchema().getObjectType(typeName.toString());
//        assertNotNull(objectType, "There must be an underlying graphql object type called '%s'", typeName);
//        return objectType;
//
//
//    }
//
//
//    private FetchedValueAnalysis analyzeList(ExecutionContext executionContext,
//                                             FetchedValue fetchedValue,
//                                             Object toAnalyze,
//                                             GraphQLList curType,
//                                             NormalizedQueryField normalizedQueryField,
//                                             ExecutionPath executionPath,
//                                             List<String> fieldIds) {
//        if (toAnalyze == null) {
//            return newFetchedValueAnalysis(LIST)
//                    .actualType(curType)
//                    .fetchedValue(fetchedValue)
//                    .normalizedQueryField(normalizedQueryField)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .nullValue()
//                    .build();
//        }
//
//        if (toAnalyze.getClass().isArray() || toAnalyze instanceof Iterable) {
//            Collection<Object> collection = FpKit.toCollection(toAnalyze);
//            return analyzeIterable(executionContext, fetchedValue, collection, curType, normalizedQueryField, executionPath, fieldIds);
//        } else {
//            TypeMismatchError error = new TypeMismatchError(executionPath, curType);
//            return newFetchedValueAnalysis(LIST)
//                    .fetchedValue(fetchedValue)
//                    .actualType(curType)
//                    .executionPath(executionPath)
//                    .fieldIds(fieldIds)
//                    .normalizedQueryField(normalizedQueryField)
//                    .nullValue()
//                    .error(error)
//                    .build();
//        }
//
//    }
//
//    private FetchedValueAnalysis analyzeIterable(ExecutionContext executionContext,
//                                                 FetchedValue fetchedValue,
//                                                 Iterable<Object> iterableValues,
//                                                 GraphQLList currentType,
//                                                 NormalizedQueryField normalizedQueryField,
//                                                 ExecutionPath executionPath,
//                                                 List<String> fieldIds) {
//        Collection<Object> values = FpKit.toCollection(iterableValues);
//        List<FetchedValueAnalysis> children = new ArrayList<>();
//        int index = 0;
//        for (Object item : values) {
//            ExecutionPath indexedPath = executionPath.segment(index);
//            children.add(analyzeFetchedValueImpl(executionContext, fetchedValue, item, normalizedQueryField, (GraphQLOutputType) GraphQLTypeUtil.unwrapOne(currentType), indexedPath, fieldIds));
//            index++;
//        }
//        return newFetchedValueAnalysis(LIST)
//                .actualType(currentType)
//                .fetchedValue(fetchedValue)
//                .executionPath(executionPath)
//                .normalizedQueryField(normalizedQueryField)
//                .fieldIds(fieldIds)
//                .children(children)
//                .build();
//
//    }
//
//    protected Object serializeScalarValue(Object toAnalyze, GraphQLScalarType scalarType) throws CoercingSerializeException {
//        if (scalarType == Scalars.GraphQLString) {
//            if (toAnalyze instanceof String) {
//                return toAnalyze;
//            } else {
//                throw new CoercingSerializeException("Unexpected value '" + toAnalyze + "'. String expected");
//            }
//        }
//        return scalarType.getCoercing().serialize(toAnalyze);
//    }
//
//
//    private FetchedValueAnalysis analyzeScalarValue(FetchedValue fetchedValue,
//                                                    Object toAnalyze,
//                                                    GraphQLScalarType scalarType,
//                                                    NormalizedQueryField normalizedQueryField,
//                                                    ExecutionPath executionPath, List<String> fieldIds) {
//        if (toAnalyze == null) {
//            return newFetchedValueAnalysis(SCALAR)
//                    .fetchedValue(fetchedValue)
//                    .actualType(scalarType)
//                    .normalizedQueryField(normalizedQueryField)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .nullValue()
//                    .build();
//        }
//        Object serialized;
//        try {
//            serialized = serializeScalarValue(toAnalyze, scalarType);
//        } catch (CoercingSerializeException e) {
//            SerializationError error = new SerializationError(executionPath, e);
//            return newFetchedValueAnalysis(SCALAR)
//                    .fetchedValue(fetchedValue)
//                    .normalizedQueryField(normalizedQueryField)
//                    .actualType(scalarType)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .error(error)
//                    .nullValue()
//                    .build();
//        }
//
//        // TODO: fix that: this should not be handled here
//        //6.6.1 http://facebook.github.io/graphql/#sec-Field-entries
//        if (serialized instanceof Double && ((Double) serialized).isNaN()) {
//            return newFetchedValueAnalysis(SCALAR)
//                    .fetchedValue(fetchedValue)
//                    .normalizedQueryField(normalizedQueryField)
//                    .actualType(scalarType)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .nullValue()
//                    .build();
//        }
//        // handle non null
//
//        return newFetchedValueAnalysis(SCALAR)
//                .fetchedValue(fetchedValue)
//                .normalizedQueryField(normalizedQueryField)
//                .actualType(scalarType)
//                .fieldIds(fieldIds)
//                .executionPath(executionPath)
//                .completedValue(serialized)
//                .build();
//    }
//
//    private FetchedValueAnalysis analyzeEnumValue(FetchedValue fetchedValue,
//                                                  Object toAnalyze,
//                                                  GraphQLEnumType enumType,
//                                                  NormalizedQueryField normalizedQueryField,
//                                                  ExecutionPath executionPath,
//                                                  List<String> fieldIds) {
//        if (toAnalyze == null) {
//            return newFetchedValueAnalysis(SCALAR)
//                    .fetchedValue(fetchedValue)
//                    .actualType(enumType)
//                    .normalizedQueryField(normalizedQueryField)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .nullValue()
//                    .build();
//
//        }
//        Object serialized;
//        try {
//            serialized = enumType.serialize(toAnalyze);
//        } catch (CoercingSerializeException e) {
//            SerializationError error = new SerializationError(executionPath, e);
//            return newFetchedValueAnalysis(SCALAR)
//                    .fetchedValue(fetchedValue)
//                    .normalizedQueryField(normalizedQueryField)
//                    .actualType(enumType)
//                    .fieldIds(fieldIds)
//                    .executionPath(executionPath)
//                    .nullValue()
//                    .error(error)
//                    .build();
//        }
//        // handle non null values
//        return newFetchedValueAnalysis(ENUM)
//                .normalizedQueryField(normalizedQueryField)
//                .actualType(enumType)
//                .fieldIds(fieldIds)
//                .executionPath(executionPath)
//                .fetchedValue(fetchedValue)
//                .completedValue(serialized)
//                .build();
//    }
//
//}
