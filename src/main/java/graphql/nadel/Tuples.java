package graphql.nadel;

public class Tuples {

    public static <T1, T2> NadelTuple2<T1, T2> of(T1 t1, T2 t2) {
        return new NadelTuple2<>(t1, t2);
    }
}
