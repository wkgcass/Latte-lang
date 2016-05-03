package lt.lang.function;

/**
 * function with 8 args
 */
@FunctionalInterface
public interface Function8 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g, Object h) throws Throwable;
}
