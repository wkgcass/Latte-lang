package lt.lang.function;

/**
 * function with 5 args
 */
@FunctionalInterface
public interface Function5 extends Function {
        Object apply(Object a, Object b, Object c, Object d, Object e) throws Throwable;
}
