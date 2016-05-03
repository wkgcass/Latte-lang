package lt.lang.function;

/**
 * function with 3 args
 */
@FunctionalInterface
public interface Function3 extends Function {
        Object apply(Object a, Object b, Object c) throws Throwable;
}
