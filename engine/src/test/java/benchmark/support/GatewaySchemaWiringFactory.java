package benchmark.support;

import graphql.Scalars;
import graphql.nadel.schema.NeverWiringFactory;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.ScalarWiringEnvironment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class GatewaySchemaWiringFactory extends NeverWiringFactory {

    private Map<String, GraphQLScalarType> passThruScalars = new ConcurrentHashMap<>();

    private GraphQLScalarType urlScalar = GraphQLScalarType.newScalar()
            .name("URL")
            .description("A URL Scalar type")
            .coercing(Scalars.GraphQLString.getCoercing())
            .build();

    private GraphQLScalarType dateTimeScalar = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("DateTime type")
            .coercing(Scalars.GraphQLString.getCoercing())
            .build();

    private Map<String, GraphQLScalarType> defaultScalars = Map.of(
            urlScalar.getName(), urlScalar,
            ExtendedScalars.Json.getName(), ExtendedScalars.Json,
            ExtendedScalars.GraphQLLong.getName(), ExtendedScalars.GraphQLLong,
            dateTimeScalar.getName(), dateTimeScalar
    );

    @Override
    public boolean providesScalar(ScalarWiringEnvironment env) {
        String scalarName = env.getScalarTypeDefinition().getName();
        if (defaultScalars.containsKey(scalarName)) {
            return true;
        } else {
            return !ScalarInfo.isGraphqlSpecifiedScalar(scalarName);
        }
    }

    @Override
    public GraphQLScalarType getScalar(ScalarWiringEnvironment env) {
        String scalarName = env.getScalarTypeDefinition().getName();
        GraphQLScalarType scalarType = defaultScalars.get(scalarName);
        if (scalarType == null) {
            scalarType = passThruScalars.computeIfAbsent(scalarName, passThruScalar(env));
        }
        return scalarType;
    }

    private Function<String, GraphQLScalarType> passThruScalar(ScalarWiringEnvironment env) {
        return key -> {
            var scalarTypeDefinition = env.getScalarTypeDefinition();
            var scalarName = scalarTypeDefinition.getName();
            return GraphQLScalarType.newScalar().name(scalarName)
                    .definition(scalarTypeDefinition)
                    .description(scalarName)
                    .coercing(Scalars.GraphQLString.getCoercing())
                    .build();
        };
    }
}
