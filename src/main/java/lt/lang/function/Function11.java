package lt.lang.function;

/**
 * function with 11 args
 */
@FunctionalInterface
public interface Function11 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h, Object i, Object j,
                     Object k) throws Throwable;
}
