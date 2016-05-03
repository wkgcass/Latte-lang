package lt.lang.function;

/**
 * function with 6 args
 */
@FunctionalInterface
public interface Function6 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e,
                     Object f) throws Throwable;
}
