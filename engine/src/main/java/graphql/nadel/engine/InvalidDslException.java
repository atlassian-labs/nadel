package graphql.nadel.engine;

import graphql.Internal;
import graphql.language.SourceLocation;

@Internal
public class InvalidDslException extends RuntimeException {
    private final SourceLocation location;

    public InvalidDslException(String message, SourceLocation location) {
        super(String.format("[%d:%d] %s", location.getLine(), location.getColumn(), message));
        this.location = location;
    }

    public SourceLocation location() {
        return location;
    }
}
