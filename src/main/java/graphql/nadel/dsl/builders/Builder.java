package graphql.nadel.dsl.builders;

import graphql.language.AbstractNode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Builder<S extends ParserRuleContext, T extends AbstractNode<T>> {
    public abstract T build(S input);

    public Iterable<T> buildMany(List<S> input) {
        return input
                .stream()
                .map(this::build)
                .collect(Collectors.toList());
    }

    protected Object invokeMethod(Object o, String name) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method m = o.getClass().getMethod(name, new Class[] {});
        return m.invoke(o, new Object[] {});
    }

    protected String getName(Object o) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return (String) invokeMethod(invokeMethod(o, "name"), "getText");
    }
}
