package lt.lang.function;

/**
 * function with 1 arg
 */
@FunctionalInterface
public interface Function1 extends Function {
        Object apply(Object a) throws Throwable;
}
