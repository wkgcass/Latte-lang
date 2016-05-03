package lt.lang.function;

/**
 * function with 16 args
 */
@FunctionalInterface
public interface Function16 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h, Object i, Object j,
                     Object k, Object l, Object m, Object n, Object o,
                     Object p) throws Throwable;
}
