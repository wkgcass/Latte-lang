package lt.lang.function;

/**
 * function with 4 args
 */
@FunctionalInterface
public interface Function4 extends Function {
        Object apply(Object a, Object b, Object c, Object d) throws Throwable;
}
