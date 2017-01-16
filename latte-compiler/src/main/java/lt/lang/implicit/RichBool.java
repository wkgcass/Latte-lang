package lt.lang.implicit;

import lt.lang.Implicit;

/**
 * rich Integer
 */
@Implicit
public class RichBool {
        private Boolean b;

        public RichBool(Boolean b) {
                this.b = b;
        }

        public boolean logicNot() throws Throwable {
                return !b;
        }

        /*
         * ============
         *     add
         * ============
         */

        public String add(String s) {
                return b + s;
        }

        /*
         * ============
         *     and
         * ============
         */

        public boolean and(Boolean b) throws Throwable {
                return this.b & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public boolean or(Boolean b) throws Throwable {
                return this.b | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        // none

        /*
         * ============
         *      ge
         * ============
         */

        // none

        /*
         * ============
         *      gt
         * ============
         */

        // none

        /*
         * ============
         *      le
         * ============
         */

        // none

        /*
         * ============
         *      lt
         * ============
         */

        // none

        /*
         * ============
         *   multiply
         * ============
         */

        // none

        /*
         * ============
         *   remainder
         * ============
         */

        // none

        /*
         * ============
         *   shiftLeft
         * ============
         */

        // none

        /*
         * ============
         *  shiftRight
         * ============
         */

        // none

        /*
         * ============
         *   subtract
         * ============
         */

        // none

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        // none

        /*
         * ============
         *     xor
         * ============
         */

        public boolean xor(Boolean b) throws Throwable {
                return this.b ^ b;
        }
}
