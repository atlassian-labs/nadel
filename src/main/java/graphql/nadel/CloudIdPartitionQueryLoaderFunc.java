package graphql.nadel;

import com.atlassian.braid.BraidContext;
import com.atlassian.braid.MutableBraidContext;
import com.atlassian.braid.source.QueryPartitionFunction;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionContextBuilder;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.StringValue;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.GraphQLArgument;
import javafx.util.Pair;
import org.dataloader.DataLoaderRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CloudIdPartitionQueryLoaderFunc implements QueryPartitionFunction {

    @Override
    public CompletionStage<List<DataFetcherResult<Object>>> apply(List<DataFetchingEnvironment> environments,
                                                                  Function<List<DataFetchingEnvironment>,
                                                                          CompletionStage<List<DataFetcherResult<Object>>>> queryLoadFn) {
        //get env with cloudId  directives
        List<DataFetchingEnvironment> cloudIdEnvs = environments.stream()
                .filter(e -> e.getFieldDefinition().getArguments().stream()
                        .anyMatch(a -> a.getDirective("cloudId") != null))
                .collect(Collectors.toList());
        Map<String, Set<DataFetchingEnvironment>> cloudIdEnvMap = new LinkedHashMap<>();
        List<DataFetchingEnvironment> nonPartitionedEnvs = new ArrayList<>(environments);
        nonPartitionedEnvs.removeAll(cloudIdEnvs);
        if (nonPartitionedEnvs.size() > 0) cloudIdEnvMap.put("", new HashSet<>(nonPartitionedEnvs));

        //get cloudId target Argument Name
        List<Pair<String, DataFetchingEnvironment>> argmentMap = new ArrayList<>();
        for (DataFetchingEnvironment dfe : cloudIdEnvs) {
            List<GraphQLArgument> arguments = dfe.getFieldDefinition().getArguments().stream()
                    .filter(a -> a.getDirective("cloudId") != null)
                    .collect(Collectors.toList());
            arguments.forEach(arg -> argmentMap.add(new Pair(arg.getName(), dfe)));
        }

        //for each cloudId, need update argument and set cloudId header
        for (Pair pair : argmentMap) {
            DataFetchingEnvironment dfe = (DataFetchingEnvironment) pair.getValue();
            String value = dfe.getArgument(pair.getKey().toString());
            Pair<String, String> cloudIdAndId = splitAri(value, dfe.getParentType().getName());
            Map<String, Object> arguments = dfe.getArguments();
            arguments.put(pair.getKey().toString(), cloudIdAndId.getValue());

            //Update argument from client request to convert ari to id.
            List<Field> fields = dfe.getFields();
            for (Field field : fields) {
                List<Argument> argumentsOld = field.getArguments();
                for (Argument argument : argumentsOld) {
                    if (argument.getName().equals(pair.getKey().toString())) {
                        argument.setValue(StringValue.newStringValue(cloudIdAndId.getValue()).build());
                    }
                }
            }


            class HeaderBraidContext implements BraidContext
            {
                @Override
                public void addMissingFields(String s, List list) {

                }

                @Override
                public List<Field> getMissingFields(String s) {
                    return null;
                }

                @Override
                public DataLoaderRegistry getDataLoaderRegistry() {
                    return null;
                }

                @Override
                public Object getContext() {
                    return headers;
                }

                private Map<String, String> headers;
                public void setContext(Map<String, String> headers) {
                    this.headers = headers;
                }

            }
            MutableBraidContext newContext = (MutableBraidContext)dfe.getExecutionContext().getContext();
            BraidContext processContext = new HeaderBraidContext();
            Map<String, String> test = new HashMap<>();
            test.put("ATL-CLOUD-ID", cloudIdAndId.getKey());
            ((HeaderBraidContext) processContext).setContext(test);
            newContext.setContext(processContext);
            //put cloudId into context.
            //TODO: Check the way to update context is correct. seems like context has other info.
            DataFetchingEnvironment newDfe = DataFetchingEnvironmentBuilder.newDataFetchingEnvironment(dfe)
                    .arguments(arguments)
                    .fields(fields)
                    .fieldDefinition(dfe.getFieldDefinition())
                    .executionContext(ExecutionContextBuilder.newExecutionContextBuilder(dfe.getExecutionContext())
                            .context(newContext).build())
                    .build();
            cloudIdEnvMap.putIfAbsent(cloudIdAndId.getKey(), new HashSet<>());
            cloudIdEnvMap.get(cloudIdAndId.getKey()).add(newDfe);
        }

        //call downstream services partitioned by cloudId and merge results.
        CompletableFuture<List<DataFetcherResult<Object>>> results = null;
        for (String cloudId : cloudIdEnvMap.keySet()) {
            List<DataFetchingEnvironment> environmentList = new ArrayList<>(cloudIdEnvMap.get(cloudId));
            CompletableFuture<List<DataFetcherResult<Object>>> pResult = queryLoadFn.apply(environmentList).toCompletableFuture();
            if (results == null) {
                results = pResult;
            } else {
                results.thenCombine(pResult, (l1, l2) -> l1.addAll(l2));
            }
        }
        return results;
    }

    //get cloudId from value and type
    private Pair<String, String> splitAri(String value, String type) {
        String[] vas = value.split(":");
        return new Pair<>(vas[0], vas[1]);
    }
}