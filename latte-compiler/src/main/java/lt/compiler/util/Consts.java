package lt.compiler.util;

/**
 * the flags
 */
public class Consts {
        public static final int IS_POINTER_SET = 1; // 0001
        public static final int IS_POINTER_GET = 2; // 0010
        public static final int IS_POINTER_NEW = 4; // 0100

        public static final String AST_FIELD = "_LATTE$AST";
        public static final String GENERIC_NAME_SPLIT = "_$G$_";
        public static final String GENERIC_TEMPLATE_FIELD = "_LATTE$GENERIC$AST";

        private Consts() {
        }

        public static boolean match(int field, int flag) {
                return (field & flag) == flag;
        }
}
