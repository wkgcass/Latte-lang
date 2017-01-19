package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LtRuntime;

/**
 * rich Long
 */
@Implicit
public class RichLong {
        private Long l;

        public RichLong(Long l) {
                this.l = l;
        }

        public long not() {
                return ~l;
        }

        public long negate() {
                return -l;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(l);
        }

        /*
         * ============
         *     add
         * ============
         */

        public long add(Integer i) {
                return l + i;
        }

        public long add(Byte b) {
                return l + b;
        }

        public long add(Short s) {
                return l + s;
        }

        public long add(Character c) {
                return l + c;
        }

        public long add(Long l) {
                return this.l + l;
        }

        public float add(Float f) {
                return l + f;
        }

        public double add(Double d) {
                return l + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public long and(Integer i) {
                return l & i;
        }

        public long and(Byte b) {
                return l & b;
        }

        public long and(Short s) {
                return l & s;
        }

        public long and(Character c) {
                return l & c;
        }

        public long and(Long l) {
                return this.l & l;
        }

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(l) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public long or(Integer i) {
                return l | i;
        }

        public long or(Byte b) {
                return l | b;
        }

        public long or(Short s) {
                return l | s;
        }

        public long or(Character c) {
                return l | c;
        }

        public long or(Long l) {
                return this.l | l;
        }

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(l) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public long divide(Integer i) {
                return l / i;
        }

        public long divide(Byte b) {
                return l / b;
        }

        public long divide(Short s) {
                return l / s;
        }

        public long divide(Character c) {
                return l / c;
        }

        public long divide(Long l) {
                return this.l / l;
        }

        public float divide(Float f) {
                return l / f;
        }

        public double divide(Double d) {
                return l / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return l >= i;
        }

        public boolean ge(Byte b) {
                return l >= b;
        }

        public boolean ge(Short s) {
                return l >= s;
        }

        public boolean ge(Character c) {
                return l >= c;
        }

        public boolean ge(Long l) {
                return this.l >= l;
        }

        public boolean ge(Float f) {
                return l >= f;
        }

        public boolean ge(Double d) {
                return l >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return l > i;
        }

        public boolean gt(Byte b) {
                return l > b;
        }

        public boolean gt(Short s) {
                return l > s;
        }

        public boolean gt(Character c) {
                return l > c;
        }

        public boolean gt(Long l) {
                return this.l > l;
        }

        public boolean gt(Float f) {
                return l > f;
        }

        public boolean gt(Double d) {
                return l > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return l <= i;
        }

        public boolean le(Byte b) {
                return l <= b;
        }

        public boolean le(Short s) {
                return l <= s;
        }

        public boolean le(Character c) {
                return l <= c;
        }

        public boolean le(Long l) {
                return this.l <= l;
        }

        public boolean le(Float f) {
                return l <= f;
        }

        public boolean le(Double d) {
                return l <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return l < i;
        }

        public boolean lt(Byte b) {
                return l < b;
        }

        public boolean lt(Short s) {
                return l < s;
        }

        public boolean lt(Character c) {
                return l < c;
        }

        public boolean lt(Long l) {
                return this.l < l;
        }

        public boolean lt(Float f) {
                return l < f;
        }

        public boolean lt(Double d) {
                return l < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public long multiply(Integer i) {
                return l * i;
        }

        public long multiply(Byte b) {
                return l * b;
        }

        public long multiply(Short s) {
                return l * s;
        }

        public long multiply(Character c) {
                return l * c;
        }

        public long multiply(Long l) {
                return this.l * l;
        }

        public float multiply(Float f) {
                return l * f;
        }

        public double multiply(Double d) {
                return l * d;
        }

        /*
         * ============
         *   remainder
         * ============
         */

        public long remainder(Integer i) {
                return l % i;
        }

        public long remainder(Byte b) {
                return l % b;
        }

        public long remainder(Short s) {
                return l % s;
        }

        public long remainder(Character c) {
                return l % c;
        }

        public long remainder(Long l) {
                return this.l % l;
        }

        /*
         * ============
         *   shiftLeft
         * ============
         */

        public long shiftLeft(Integer i) {
                return l << i;
        }

        public long shiftLeft(Byte b) {
                return l << b;
        }

        public long shiftLeft(Short s) {
                return l << s;
        }

        public long shiftLeft(Character c) {
                return l << c;
        }

        public long shiftLeft(Long l) {
                return this.l << l;
        }

        /*
         * ============
         *  shiftRight
         * ============
         */

        public long shiftRight(Integer i) {
                return l >> i;
        }

        public long shiftRight(Byte b) {
                return l >> b;
        }

        public long shiftRight(Short s) {
                return l >> s;
        }

        public long shiftRight(Character c) {
                return l >> c;
        }

        public long shiftRight(Long l) {
                return this.l >> l;
        }

        /*
         * ============
         *   subtract
         * ============
         */

        public long subtract(Integer i) {
                return l - i;
        }

        public long subtract(Byte b) {
                return l - b;
        }

        public long subtract(Short s) {
                return l - s;
        }

        public long subtract(Character c) {
                return l - c;
        }

        public long subtract(Long l) {
                return this.l - l;
        }

        public float subtract(Float f) {
                return l - f;
        }

        public double subtract(Double d) {
                return l - d;
        }

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        public long unsignedShiftRight(Integer i) {
                return l >>> i;
        }

        public long unsignedShiftRight(Byte b) {
                return l >>> b;
        }

        public long unsignedShiftRight(Short s) {
                return l >>> s;
        }

        public long unsignedShiftRight(Character c) {
                return l >>> c;
        }

        public long unsignedShiftRight(Long l) {
                return this.l >>> l;
        }

        /*
         * ============
         *     xor
         * ============
         */

        public long xor(Integer i) {
                return l ^ i;
        }

        public long xor(Byte b) {
                return l ^ b;
        }

        public long xor(Short s) {
                return l ^ s;
        }

        public long xor(Character c) {
                return l ^ c;
        }

        public long xor(Long l) {
                return this.l ^ l;
        }

        public boolean xor(Boolean b) throws Throwable {
                return LtRuntime.castToBool(l) ^ b;
        }
}
