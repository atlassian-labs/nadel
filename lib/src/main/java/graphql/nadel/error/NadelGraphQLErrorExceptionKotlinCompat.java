package graphql.nadel.error;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Yes this is in Java, because of a compiler bug with conflicting `getMessage` declarations.
 */
public abstract class NadelGraphQLErrorExceptionKotlinCompat extends RuntimeException implements GraphQLError {
    public NadelGraphQLErrorExceptionKotlinCompat(@NotNull String message) {
        super(message);
    }

    public NadelGraphQLErrorExceptionKotlinCompat(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    @Override
    @Nullable
    public List<Object> getPath() {
        return null;
    }

    @Override
    @Nullable
    public List<SourceLocation> getLocations() {
        return null;
    }

    @NotNull
    @Override
    public abstract ErrorClassification getErrorType();
}
