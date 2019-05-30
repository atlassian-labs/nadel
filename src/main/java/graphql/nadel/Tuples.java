package graphql.nadel;

public class Tuples {

    public static <T1, T2> TuplesTwo<T1, T2> of(T1 t1, T2 t2) {
        return new TuplesTwo<>(t1, t2);
    }
}
