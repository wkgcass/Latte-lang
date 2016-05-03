package lt.lang;

/**
 * undefined object
 */
public class Undefined {
        private static final Undefined undefined = new Undefined();

        private Undefined() {
        }

        public static Undefined get() {
                return undefined;
        }

        @Override
        public String toString() {
                return "undefined";
        }
}
