package lt.lang.callback;

/**
 * callback result holder
 */
public class CallbackResultHolder<T> {
        Throwable err;
        T res;
        boolean finished = false;

        public T waitResult() throws Throwable {
                while (!finished) {
                        Thread.sleep(1);
                }
                if (err != null) {
                        throw err;
                }
                return res;
        }
}
