package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LtRuntime;

/**
 * rich Float
 */
@Implicit
public class RichFloat {
        private Float f;

        public RichFloat(Float f) {
                this.f = f;
        }

        public float negate() {
                return -f;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(f);
        }

        /*
         * ============
         *     add
         * ============
         */

        public float add(Integer i) {
                return f + i;
        }

        public float add(Byte b) {
                return f + b;
        }

        public float add(Short s) {
                return f + s;
        }

        public float add(Character c) {
                return f + c;
        }

        public float add(Long l) {
                return f + l;
        }

        public float add(Float f) {
                return this.f + f;
        }

        public double add(Double d) {
                return f + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(f) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(f) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public float divide(Integer i) {
                return f / i;
        }

        public float divide(Byte b) {
                return f / b;
        }

        public float divide(Short s) {
                return f / s;
        }

        public float divide(Character c) {
                return f / c;
        }

        public float divide(Long l) {
                return f / l;
        }

        public float divide(Float f) {
                return this.f / f;
        }

        public double divide(Double d) {
                return f / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return f >= i;
        }

        public boolean ge(Byte b) {
                return f >= b;
        }

        public boolean ge(Short s) {
                return f >= s;
        }

        public boolean ge(Character c) {
                return f >= c;
        }

        public boolean ge(Long l) {
                return f >= l;
        }

        public boolean ge(Float f) {
                return this.f >= f;
        }

        public boolean ge(Double d) {
                return f >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return f > i;
        }

        public boolean gt(Byte b) {
                return f > b;
        }

        public boolean gt(Short s) {
                return f > s;
        }

        public boolean gt(Character c) {
                return f > c;
        }

        public boolean gt(Long l) {
                return f > l;
        }

        public boolean gt(Float f) {
                return this.f > f;
        }

        public boolean gt(Double d) {
                return f > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return f <= i;
        }

        public boolean le(Byte b) {
                return f <= b;
        }

        public boolean le(Short s) {
                return f <= s;
        }

        public boolean le(Character c) {
                return f <= c;
        }

        public boolean le(Long l) {
                return f <= l;
        }

        public boolean le(Float f) {
                return this.f <= f;
        }

        public boolean le(Double d) {
                return f <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return f < i;
        }

        public boolean lt(Byte b) {
                return f < b;
        }

        public boolean lt(Short s) {
                return f < s;
        }

        public boolean lt(Character c) {
                return f < c;
        }

        public boolean lt(Long l) {
                return f < l;
        }

        public boolean lt(Float f) {
                return this.f < f;
        }

        public boolean lt(Double d) {
                return f < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public float multiply(Integer i) {
                return f * i;
        }

        public float multiply(Byte b) {
                return f * b;
        }

        public float multiply(Short s) {
                return f * s;
        }

        public float multiply(Character c) {
                return f * c;
        }

        public float multiply(Long l) {
                return f * l;
        }

        public float multiply(Float f) {
                return this.f * f;
        }

        public double multiply(Double d) {
                return f * d;
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

        public float subtract(Integer i) {
                return f - i;
        }

        public float subtract(Byte b) {
                return f - b;
        }

        public float subtract(Short s) {
                return f - s;
        }

        public float subtract(Character c) {
                return f - c;
        }

        public float subtract(Long l) {
                return f - l;
        }

        public float subtract(Float f) {
                return this.f - f;
        }

        public double subtract(Double d) {
                return f - d;
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
                return LtRuntime.castToBool(f) ^ b;
        }
}
