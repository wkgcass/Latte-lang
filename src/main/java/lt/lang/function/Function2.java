package lt.lang.function;

/**
 * function with 2 args
 */
@FunctionalInterface
public interface Function2 extends Function {
        Object apply(Object a, Object b) throws Throwable;
}
