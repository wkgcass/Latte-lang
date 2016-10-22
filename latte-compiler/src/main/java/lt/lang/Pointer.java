package lt.lang;

/**
 * the pointer object.
 * use p[] to access/modify the contained value.
 */
public class Pointer {
        private Object item;
        private final boolean nonnull;
        private final boolean nonempty;

        public Pointer(boolean nonnull, boolean nonempty) {
                this.nonnull = nonnull;
                this.nonempty = nonempty;
        }

        /**
         * retrieve the contained object (or null)
         *
         * @return the contained object
         */
        public Object get() {
                return item;
        }

        /**
         * cast type and set the contained object and return the pointer itself.
         *
         * @param item input object (or null)
         * @return input object
         * @throws Throwable exception when trying to cast the object
         */
        public Pointer set(Object item) throws Throwable {
                if (nonempty) {
                        if (!LtRuntime.castToBool(item)) throw new IllegalArgumentException();
                }
                if (nonnull) {
                        if (item == null) throw new NullPointerException();
                        if (item instanceof Unit) throw new IllegalArgumentException();
                }
                this.item = item;
                return this;
        }
}
