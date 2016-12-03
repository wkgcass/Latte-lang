package lt.lang.callback;

import lt.lang.function.Function1;

/**
 * get one single async result
 */
public class AsyncResultFunc<R, T> implements Function1<R, T> {
        private final CallbackFunc<T> callbackFunc;

        public AsyncResultFunc(CallbackFunc<T> callbackFunc) {
                this.callbackFunc = callbackFunc;
        }

        @Override
        public R apply(T t) throws Exception {
                callbackFunc.apply(null, t);
                return null;
        }
}
