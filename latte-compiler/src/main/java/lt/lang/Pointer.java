package lt.lang;

/**
 * the pointer object.
 * use p[] to access/modify the contained value.
 */
public class Pointer {
        private Class<?> containedType;
        private Object item;
        private final boolean isVal;
        private boolean assigned = false;
        private final Object lock = new Object();

        public Pointer(Class<?> containedType, boolean isVal) throws Throwable {
                this.containedType = containedType;
                this.isVal = isVal;
        }

        /**
         * check whether the pointer is changeable
         *
         * @return true if the pointer can change.
         */
        public boolean canChange() {
                return !isVal;
        }

        private Object checkType(Object value) throws Throwable {
                if (value == null) return null;
                if (!containedType.isInstance(value)) {
                        return LtRuntime.cast(value, containedType);
                } else {
                        return value;
                }
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
         * cast type and set the contained object and return the input object.
         *
         * @param item input object (or null)
         * @return input object
         * @throws Throwable exception when trying to cast the object
         */
        public Object set(Object item) throws Throwable {
                if (assigned && isVal) throw new IllegalStateException("try to assign an unchangeable object");
                if (assigned) {
                        this.item = checkType(item);
                } else {
                        synchronized (lock) {
                                this.item = checkType(item);
                                assigned = true;
                        }
                }
                return item;
        }
}
