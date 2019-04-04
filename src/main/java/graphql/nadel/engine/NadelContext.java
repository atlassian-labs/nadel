package graphql.nadel.engine;

import graphql.Internal;

import java.util.UUID;

/**
 * We sue a wrapper Nadel context object over top of the caller supplied one'
 * so that we can get access to Nadel specific context properties in
 * a type safe manner
 */
@Internal
public class NadelContext {
    final Object callerSuppliedContext;
    final String underscoreTypeNameAlias;

    private NadelContext(Object callerSuppliedContext, String underscoreTypeNameAlias) {
        this.callerSuppliedContext = callerSuppliedContext;
        this.underscoreTypeNameAlias = underscoreTypeNameAlias;
    }

    public Object getCallerSuppliedContext() {
        return callerSuppliedContext;
    }

    public String getUnderscoreTypeNameAlias() {
        return underscoreTypeNameAlias;
    }

    public static NadelContext newContext() {
        return newContext(null);
    }

    public static NadelContext newContext(Object callerSuppliedContext) {
        return new NadelContext(callerSuppliedContext, mkUnderscoreTypeNameAlias());
    }

    private static String mkUnderscoreTypeNameAlias() {
        String uuid = UUID.randomUUID().toString();
        return String.format("typename__%s", uuid.replaceAll("-", "_"));
    }
}
