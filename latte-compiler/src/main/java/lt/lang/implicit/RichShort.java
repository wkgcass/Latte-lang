package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LtRuntime;

/**
 * rich Integer
 */
@Implicit
public class RichShort {
        private Short s;

        public RichShort(Short s) {
                this.s = s;
        }

        public int not() {
                return ~s;
        }

        public int negate() {
                return -s;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(s);
        }

        /*
         * ============
         *     add
         * ============
         */

        public int add(Integer i) {
                return this.s + i;
        }

        public int add(Byte b) {
                return this.s + b;
        }

        public int add(Short s) {
                return this.s + s;
        }

        public int add(Character c) {
                return s + c;
        }

        public long add(Long l) {
                return s + l;
        }

        public float add(Float f) {
                return s + f;
        }

        public double add(Double d) {
                return s + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public int and(Integer i) {
                return this.s & i;
        }

        public int and(Byte b) {
                return this.s & b;
        }

        public int and(Short s) {
                return this.s & s;
        }

        public int and(Character c) {
                return s & c;
        }

        public long and(Long l) {
                return s & l;
        }

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.s) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public int or(Integer i) {
                return this.s | i;
        }

        public int or(Byte b) {
                return this.s | b;
        }

        public int or(Short s) {
                return this.s | s;
        }

        public int or(Character c) {
                return s | c;
        }

        public long or(Long l) {
                return s | l;
        }

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.s) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public int divide(Integer i) {
                return this.s / i;
        }

        public int divide(Byte b) {
                return this.s / b;
        }

        public int divide(Short s) {
                return this.s / s;
        }

        public int divide(Character c) {
                return s / c;
        }

        public long divide(Long l) {
                return s / l;
        }

        public float divide(Float f) {
                return s / f;
        }

        public double divide(Double d) {
                return s / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return this.s >= i;
        }

        public boolean ge(Byte b) {
                return this.s >= b;
        }

        public boolean ge(Short s) {
                return this.s >= s;
        }

        public boolean ge(Character c) {
                return s >= c;
        }

        public boolean ge(Long l) {
                return s >= l;
        }

        public boolean ge(Float f) {
                return s >= f;
        }

        public boolean ge(Double d) {
                return s >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return this.s > i;
        }

        public boolean gt(Byte b) {
                return this.s > b;
        }

        public boolean gt(Short s) {
                return this.s > s;
        }

        public boolean gt(Character c) {
                return s > c;
        }

        public boolean gt(Long l) {
                return s > l;
        }

        public boolean gt(Float f) {
                return s > f;
        }

        public boolean gt(Double d) {
                return s > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return this.s <= i;
        }

        public boolean le(Byte b) {
                return this.s <= b;
        }

        public boolean le(Short s) {
                return this.s <= s;
        }

        public boolean le(Character c) {
                return s <= c;
        }

        public boolean le(Long l) {
                return s <= l;
        }

        public boolean le(Float f) {
                return s <= f;
        }

        public boolean le(Double d) {
                return s <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return this.s < i;
        }

        public boolean lt(Byte b) {
                return this.s < b;
        }

        public boolean lt(Short s) {
                return this.s < s;
        }

        public boolean lt(Character c) {
                return s < c;
        }

        public boolean lt(Long l) {
                return s < l;
        }

        public boolean lt(Float f) {
                return s < f;
        }

        public boolean lt(Double d) {
                return s < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public int multiply(Integer i) {
                return this.s * i;
        }

        public int multiply(Byte b) {
                return this.s * b;
        }

        public int multiply(Short s) {
                return this.s * s;
        }

        public int multiply(Character c) {
                return s * c;
        }

        public long multiply(Long l) {
                return s * l;
        }

        public float multiply(Float f) {
                return s * f;
        }

        public double multiply(Double d) {
                return s * d;
        }

        /*
         * ============
         *   remainder
         * ============
         */

        public int remainder(Integer i) {
                return this.s % i;
        }

        public int remainder(Byte b) {
                return this.s % b;
        }

        public int remainder(Short s) {
                return this.s % s;
        }

        public int remainder(Character c) {
                return s % c;
        }

        public long remainder(Long l) {
                return s % l;
        }

        /*
         * ============
         *   shiftLeft
         * ============
         */

        public int shiftLeft(Integer i) {
                return this.s << i;
        }

        public int shiftLeft(Byte b) {
                return this.s << b;
        }

        public int shiftLeft(Short s) {
                return this.s << s;
        }

        public int shiftLeft(Character c) {
                return s << c;
        }

        public int shiftLeft(Long l) {
                return s << l;
        }

        /*
         * ============
         *  shiftRight
         * ============
         */

        public int shiftRight(Integer i) {
                return this.s >> i;
        }

        public int shiftRight(Byte b) {
                return this.s >> b;
        }

        public int shiftRight(Short s) {
                return this.s >> s;
        }

        public int shiftRight(Character c) {
                return s >> c;
        }

        public int shiftRight(Long l) {
                return s >> l;
        }

        /*
         * ============
         *   subtract
         * ============
         */

        public int subtract(Integer i) {
                return this.s - i;
        }

        public int subtract(Byte b) {
                return this.s - b;
        }

        public int subtract(Short s) {
                return this.s - s;
        }

        public int subtract(Character c) {
                return s - c;
        }

        public long subtract(Long l) {
                return s - l;
        }

        public float subtract(Float f) {
                return s - f;
        }

        public double subtract(Double d) {
                return s - d;
        }

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        public int unsignedShiftRight(Integer i) {
                return this.s >>> i;
        }

        public int unsignedShiftRight(Byte b) {
                return this.s >>> b;
        }

        public int unsignedShiftRight(Short s) {
                return this.s >>> s;
        }

        public int unsignedShiftRight(Character c) {
                return s >>> c;
        }

        public int unsignedShiftRight(Long l) {
                return s >>> l;
        }

        /*
         * ============
         *     xor
         * ============
         */

        public int xor(Integer i) {
                return this.s ^ i;
        }

        public int xor(Byte b) {
                return this.s ^ b;
        }

        public int xor(Short s) {
                return this.s ^ s;
        }

        public int xor(Character c) {
                return s ^ c;
        }

        public long xor(Long l) {
                return s ^ l;
        }

        public boolean xor(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.s) ^ b;
        }
}
