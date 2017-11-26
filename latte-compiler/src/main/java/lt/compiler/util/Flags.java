package lt.compiler.util;

/**
 * the flags
 */
public class Flags {
        public static final int IS_POINTER_SET = 1; // 0001
        public static final int IS_POINTER_GET = 2; // 0010
        public static final int IS_POINTER_NEW = 4; // 0100

        private Flags() {
        }

        public static boolean match(int field, int flag) {
                return (field & flag) == flag;
        }
}
