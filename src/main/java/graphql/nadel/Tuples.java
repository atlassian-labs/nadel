package graphql.nadel;

import graphql.Internal;

@Internal
public class Tuples {

    public static <T1, T2> TuplesTwo<T1, T2> of(T1 t1, T2 t2) {
        return new TuplesTwo<>(t1, t2);
    }

    public static <T1, T2, T3> TuplesThree<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new TuplesThree<>(t1, t2, t3);
    }
}
