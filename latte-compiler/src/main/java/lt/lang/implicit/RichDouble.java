package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LtRuntime;

/**
 * rich Double
 */
@Implicit
public class RichDouble {
        private Double d;

        public RichDouble(Double d) {
                this.d = d;
        }

        public double negate() {
                return -d;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(d);
        }

        /*
         * ============
         *     add
         * ============
         */

        public double add(Integer i) {
                return d + i;
        }

        public double add(Byte b) {
                return d + b;
        }

        public double add(Short s) {
                return d + s;
        }

        public double add(Character c) {
                return d + c;
        }

        public double add(Long l) {
                return d + l;
        }

        public double add(Float f) {
                return d + f;
        }

        public double add(Double d) {
                return this.d + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(d) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(d) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public double divide(Integer i) {
                return d / i;
        }

        public double divide(Byte b) {
                return d / b;
        }

        public double divide(Short s) {
                return d / s;
        }

        public double divide(Character c) {
                return d / c;
        }

        public double divide(Long l) {
                return d / l;
        }

        public double divide(Float f) {
                return d / f;
        }

        public double divide(Double d) {
                return this.d / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return d >= i;
        }

        public boolean ge(Byte b) {
                return d >= b;
        }

        public boolean ge(Short s) {
                return d >= s;
        }

        public boolean ge(Character c) {
                return d >= c;
        }

        public boolean ge(Long l) {
                return d >= l;
        }

        public boolean ge(Float f) {
                return d >= f;
        }

        public boolean ge(Double d) {
                return this.d >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return d > i;
        }

        public boolean gt(Byte b) {
                return d > b;
        }

        public boolean gt(Short s) {
                return d > s;
        }

        public boolean gt(Character c) {
                return d > c;
        }

        public boolean gt(Long l) {
                return d > l;
        }

        public boolean gt(Float f) {
                return d > f;
        }

        public boolean gt(Double d) {
                return this.d > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return d <= i;
        }

        public boolean le(Byte b) {
                return d <= b;
        }

        public boolean le(Short s) {
                return d <= s;
        }

        public boolean le(Character c) {
                return d <= c;
        }

        public boolean le(Long l) {
                return d <= l;
        }

        public boolean le(Float f) {
                return d <= f;
        }

        public boolean le(Double d) {
                return this.d <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return d < i;
        }

        public boolean lt(Byte b) {
                return d < b;
        }

        public boolean lt(Short s) {
                return d < s;
        }

        public boolean lt(Character c) {
                return d < c;
        }

        public boolean lt(Long l) {
                return d < l;
        }

        public boolean lt(Float f) {
                return d < f;
        }

        public boolean lt(Double d) {
                return this.d < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public double multiply(Integer i) {
                return d * i;
        }

        public double multiply(Byte b) {
                return d * b;
        }

        public double multiply(Short s) {
                return d * s;
        }

        public double multiply(Character c) {
                return d * c;
        }

        public double multiply(Long l) {
                return d * l;
        }

        public double multiply(Float f) {
                return d * f;
        }

        public double multiply(Double d) {
                return this.d * d;
        }

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

        public double subtract(Integer i) {
                return d - i;
        }

        public double subtract(Byte b) {
                return d - b;
        }

        public double subtract(Short s) {
                return d - s;
        }

        public double subtract(Character c) {
                return d - c;
        }

        public double subtract(Long l) {
                return d - l;
        }

        public double subtract(Float f) {
                return d - f;
        }

        public double subtract(Double d) {
                return this.d - d;
        }

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
                return LtRuntime.castToBool(d) ^ b;
        }
}
