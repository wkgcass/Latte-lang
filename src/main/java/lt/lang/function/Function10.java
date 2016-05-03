package lt.lang.function;

/**
 * function with 10 args
 */
@FunctionalInterface
public interface Function10 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h, Object i, Object j) throws Throwable;
}
