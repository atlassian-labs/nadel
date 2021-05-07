package graphql.nadel.hooks;

import graphql.PublicApi;
import graphql.language.Argument;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLArgument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@PublicApi
public class HydrationArguments {

    private final List<GraphQLArgument> hydrationRootGqlArguments;
    private final List<Argument> hydrationRootAstArguments;

    public HydrationArguments(List<GraphQLArgument> hydrationRootGqlArguments, List<Argument> hydrationRootAstArguments) {
        if (hydrationRootAstArguments == null ||
                hydrationRootGqlArguments == null ||
                hydrationRootAstArguments.isEmpty() ||
                hydrationRootGqlArguments.isEmpty()
        ) {
            this.hydrationRootGqlArguments = Collections.emptyList();
            this.hydrationRootAstArguments = Collections.emptyList();
        } else {
            this.hydrationRootGqlArguments = hydrationRootGqlArguments;
            this.hydrationRootAstArguments = hydrationRootAstArguments;
        }
    }

    public static HydrationArguments empty() {
        return new HydrationArguments(Collections.emptyList(), Collections.emptyList());
    }

    public List<GraphQLArgument> getHydrationRootGqlArguments() {
        return hydrationRootGqlArguments;
    }

    public List<Argument> getHydrationRootAstArguments() {
        return hydrationRootAstArguments;
    }

    public Optional<ArgumentsTuple> get(String argumentName) {
        Optional<Argument> maybeArgument = FpKit.findOne(this.hydrationRootAstArguments, arg -> arg.getName().equals(argumentName));
        Optional<GraphQLArgument> maybeGqlArgument = FpKit.findOne(this.hydrationRootGqlArguments, arg -> arg.getName().equals(argumentName));

        return maybeArgument.flatMap(argument ->
                maybeGqlArgument.map(graphQLArgument ->
                        ArgumentsTuple.of(graphQLArgument, argument)));
    }

    public static class ArgumentsTuple {

        public final GraphQLArgument graphQLArgument;
        public final Argument astArgument;

        private ArgumentsTuple(GraphQLArgument graphQLArgument, Argument astArgument) {
            this.graphQLArgument = graphQLArgument;
            this.astArgument = astArgument;
        }

        public static ArgumentsTuple of(GraphQLArgument graphQLArgument, Argument astArgument) {
            return new ArgumentsTuple(graphQLArgument, astArgument);
        }
    }
}
