package lt.lang.function;

/**
 * function with 12 args
 */
@FunctionalInterface
public interface Function12 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h, Object i, Object j,
                     Object k, Object l) throws Throwable;
}
