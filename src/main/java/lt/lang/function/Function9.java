package lt.lang.function;

/**
 * function with 9 args
 */
@FunctionalInterface
public interface Function9 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h, Object i) throws Throwable;
}
