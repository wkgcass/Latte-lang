package lt.lang.function;

/**
 * function with 7 args
 */
@FunctionalInterface
public interface Function7 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f, Object g) throws Throwable;
}
