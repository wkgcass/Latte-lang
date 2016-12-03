package lt.lang.function;

/**
 * the callback function
 */
public interface Callback1<T> extends Callback {
        void apply(Throwable err, T res) throws Exception;
}
