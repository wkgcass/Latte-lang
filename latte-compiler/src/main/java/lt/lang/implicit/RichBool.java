package lt.lang.implicit;

import lt.runtime.LtRuntime;

/**
 * rich Bool
 */
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

        // none

        /*
         * ============
         *     and
         * ============
         */

        public boolean and(Boolean b) {
                return this.b & b;
        }

        public boolean and(Object o) throws Throwable {
                return this.b & LtRuntime.castToBool(o);
        }

        /*
         * ============
         *     or
         * ============
         */

        public boolean or(Boolean b) {
                return this.b | b;
        }

        public boolean or(Object o) throws Throwable {
                return this.b | LtRuntime.castToBool(o);
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
