package lt.lang.callback;

import lt.lang.Unit;
import lt.lang.function.Callback0;
import lt.lang.function.Callback1;

/**
 * callback func
 */
public class CallbackFunc<T> implements Callback0, Callback1<T> {
        private final CallbackResultHolder<T> holder;

        public CallbackFunc(CallbackResultHolder<T> holder) {
                this.holder = holder;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void apply(Throwable err) throws Exception {
                holder.err = err;
                holder.res = (T) Unit.get();
                holder.finished = true;
        }

        @Override
        public void apply(Throwable err, T res) throws Exception {
                holder.err = err;
                holder.res = res;
                holder.finished = true;
        }
}
