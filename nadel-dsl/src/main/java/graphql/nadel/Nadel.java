package graphql.nadel;

import com.atlassian.braid.Braid;
import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.source.GraphQLRemoteRetriever;
import graphql.Assert;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static graphql.Assert.assertNotNull;

//import graphql.nadel.dsl.LinkedField;

@PublicApi
public class Nadel {

    private final String dsl;
    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private SchemaSourceFactory schemaSourceFactory;
    private Braid braid;

    public Nadel(String dsl, GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory) {
        this(dsl, new GraphQLRemoteSchemaSourceFactory(graphQLRemoteRetrieverFactory));
    }

    public Nadel(String dsl, SchemaSourceFactory schemaSourceFactory) {
        this.dsl = dsl;
        this.schemaSourceFactory = schemaSourceFactory;
        this.stitchingDsl = this.parser.parseDSL(dsl);
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.stitchingDsl.getTypesByService().put(serviceDefinition.getName(), typeDefinitionRegistry);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            this.stitchingDsl.getNamespaceByService().put(serviceDefinition.getName(), schemaNamespace);

        }
        Map<SchemaNamespace, List<Link>> linksByNamespace = createLinks();
        List<SchemaSource> schemaSources = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {

            SchemaNamespace namespace = this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName());
            TypeDefinitionRegistry typeDefinitionRegistry = this.stitchingDsl.getTypesByService().get(serviceDefinition.getName());
            List<Link> links = linksByNamespace.getOrDefault(namespace, new ArrayList<>());
            SchemaSource schemaSource = schemaSourceFactory.createSchemaSource(serviceDefinition, namespace,
                    typeDefinitionRegistry, links);
            schemaSources.add(schemaSource);
        }
        // Stitching DSL
        AsyncExecutionStrategy asyncExecutionStrategy = new AsyncExecutionStrategy();
        this.braid = Braid.builder()
                .executionStrategy(asyncExecutionStrategy)
                .schemaSources(schemaSources)
                // .withRuntimeWiring(this::addDataFetchersForLinks)
                .build();
    }

//    private void addDataFetchersForLinks(RuntimeWiring.Builder runtimeWiring) {
//        this.stitchingDsl.getServiceDefinitions().forEach(serviceDefinition -> {
//            serviceDefinition.getLinks().forEach(linkedField -> {
//                String parentType = linkedField.getParentType();
//
//                String topLevelQueryField = linkedField.getTopLevelQueryField();
//                TopLevelFieldInfo topLevelFieldInfo = findServiceByTopLevelField(topLevelQueryField);
//                ServiceDefinition targetServiceDefinition = stitchingDsl.getServiceDefinition(topLevelFieldInfo.serviceName);
//                GraphQLRemoteRetriever remoteRetriever = graphQLRemoteRetrieverFactory.createRemoteRetriever(targetServiceDefinition);
//
//
//                runtimeWiring.type(parentType, builder -> builder.dataFetcher(linkedField.getFieldName(), dataFetcherForLink(linkedField, remoteRetriever)));
//            });
//        });
//    }
//
//    private DataFetcher dataFetcherForLink(LinkedField linkedField, GraphQLRemoteRetriever remoteRetriever) {
//        return new DataFetcher() {
//            @Override
//            public CompletableFuture<DataFetcherResult> get(DataFetchingEnvironment environment) {
//                Field field = new Field(linkedField.getTopLevelQueryField());
//                PropertyDataFetcher propertyDataFetcher = new PropertyDataFetcher(linkedField.getVariableName());
//                Object value = propertyDataFetcher.get(environment);
//                Argument argument = new Argument(linkedField.getArgumentName(), new StringValue(value.toString()));
//                field.setArguments(Arrays.asList(argument));
//                field.setSelectionSet(environment.getField().getSelectionSet());
//                Document document = createDocument(field);
//                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
//                        .query(AstPrinter.printAst(document))
//                        .build();
//                CompletableFuture<Map<String, Object>> completableFuture = remoteRetriever.queryGraphQL(executionInput, null);
//                return completableFuture.thenApply(result -> handleResponse(result, field.getName()));
//            }
//        };
//    }

    public TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
        List<GraphQLError> errors = new ArrayList<>();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (TypeDefinition definition : serviceDefinition.getTypeDefinitions()) {
            typeRegistry.add(definition).ifPresent(errors::add);
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }

    private Map<SchemaNamespace, List<Link>> createLinks() {
        Map<SchemaNamespace, List<Link>> result = new LinkedHashMap<>();
        Map<FieldDefinition, FieldTransformation> transformationsByFieldDefinition = this.stitchingDsl.getTransformationsByFieldDefinition();
        transformationsByFieldDefinition.forEach((fieldDefinition, fieldTransformation) -> {
            ServiceDefinition serviceDefinition = this.stitchingDsl.getServiceByField().get(fieldDefinition);
            SchemaNamespace schemaNamespace = assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName()));
            result.putIfAbsent(schemaNamespace, new ArrayList<>());


            String parentType = fieldTransformation.getParentDefinition().getName();
            String originalFieldName = fieldDefinition.getName();
            TopLevelFieldInfo topLevelFieldInfo = findServiceByTopLevelField(fieldTransformation.getTopLevelField());
            String targetType = TypeInfo.typeInfo(topLevelFieldInfo.fieldDefinition.getType()).getName();
            String newFieldName = fieldTransformation.getTargetName();
            SchemaNamespace targetNamespace = findNameSpace(targetType);
            Link.LinkBuilder link = Link
                    .from(schemaNamespace, parentType, newFieldName, originalFieldName)
                    .to(targetNamespace, targetType, fieldTransformation.getTopLevelField())
                    .argument(fieldTransformation.getArgumentName());
            link.replaceFromField();
            result.get(schemaNamespace).add(link.build());
        });
        this.stitchingDsl.getServiceDefinitions().forEach(serviceDefinition -> {
            if (!this.stitchingDsl.getStandaloneTransformationsByService().containsKey(serviceDefinition.getName())) {
                return;
            }
            this.stitchingDsl.getStandaloneTransformationsByService().get(serviceDefinition.getName()).forEach(fieldTransformation -> {
//                ServiceDefinition serviceDefinition = this.stitchingDsl.getServiceByField().get(fieldDefinition);
                SchemaNamespace schemaNamespace = assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName()));
                result.putIfAbsent(schemaNamespace, new ArrayList<>());

                String parentType = fieldTransformation.getParentDefinition().getName();
                String originalFieldName = fieldTransformation.getFromFieldName();
                TopLevelFieldInfo topLevelFieldInfo = findServiceByTopLevelField(fieldTransformation.getTopLevelField());
                String targetType = TypeInfo.typeInfo(topLevelFieldInfo.fieldDefinition.getType()).getName();
                String newFieldName = fieldTransformation.getTargetName();
                SchemaNamespace targetNamespace = findNameSpace(targetType);
                Link.LinkBuilder link = Link
                        .from(schemaNamespace, parentType, newFieldName, originalFieldName)
                        .to(targetNamespace, targetType, fieldTransformation.getTopLevelField())
                        .argument(fieldTransformation.getArgumentName());
                result.get(schemaNamespace).add(link.build());

            });
        });

        return result;
    }

    static class TopLevelFieldInfo {
        public TopLevelFieldInfo(String serviceName, FieldDefinition fieldDefinition) {
            this.serviceName = serviceName;
            this.fieldDefinition = fieldDefinition;
        }

        String serviceName;
        FieldDefinition fieldDefinition;
    }

    private TopLevelFieldInfo findServiceByTopLevelField(String topLevelField) {
        Set<Map.Entry<String, TypeDefinitionRegistry>> entries = this.stitchingDsl.getTypesByService().entrySet();
        for (Map.Entry<String, TypeDefinitionRegistry> entry : entries) {
            ObjectTypeDefinition query = (ObjectTypeDefinition) entry.getValue().getType("Query").get();
            Optional<FieldDefinition> fieldDef = query.getFieldDefinitions().stream().filter(fieldDefinition -> fieldDefinition.getName().equals(topLevelField)).findFirst();
            if (fieldDef.isPresent()) {
                return new TopLevelFieldInfo(entry.getKey(), fieldDef.get());
            }
        }
        return Assert.assertShouldNeverHappen();
    }

    private SchemaNamespace findNameSpace(String type) {
        Map<String, TypeDefinitionRegistry> typesByService = this.stitchingDsl.getTypesByService();
        AtomicReference<String> serviceName = new AtomicReference<>();
        typesByService.forEach((service, typeDefinitionRegistry) -> {
            if (typeDefinitionRegistry.hasType(new TypeName(type))) {
                serviceName.set(service);
            }
        });
        if (serviceName.get() != null) {
            return assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceName.get()));
        }
        return Assert.assertShouldNeverHappen();
    }


    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
    }

}
