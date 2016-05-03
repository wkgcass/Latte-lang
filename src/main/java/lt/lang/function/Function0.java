package lt.lang.function;

/**
 * function with 0 args
 */
@FunctionalInterface
public interface Function0 extends Function {
        Object apply() throws Throwable;
}
