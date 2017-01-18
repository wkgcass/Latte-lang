package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LtRuntime;

/**
 * rich Integer
 */
@Implicit
public class RichChar {
        private Character c;

        public RichChar(Character c) {
                this.c = c;
        }

        public int not() {
                return ~c;
        }

        public int negate() {
                return -c;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(c);
        }

        /*
         * ============
         *     add
         * ============
         */

        public int add(Integer i) {
                return this.c + i;
        }

        public int add(Byte b) {
                return c + b;
        }

        public int add(Short s) {
                return c + s;
        }

        public int add(Character c) {
                return this.c + c;
        }

        public long add(Long l) {
                return c + l;
        }

        public float add(Float f) {
                return c + f;
        }

        public double add(Double d) {
                return c + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public int and(Integer i) {
                return this.c & i;
        }

        public int and(Byte b) {
                return c & b;
        }

        public int and(Short s) {
                return c & s;
        }

        public int and(Character c) {
                return this.c & c;
        }

        public long and(Long l) {
                return c & l;
        }

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(c) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public int or(Integer i) {
                return this.c | i;
        }

        public int or(Byte b) {
                return c | b;
        }

        public int or(Short s) {
                return c | s;
        }

        public int or(Character c) {
                return this.c | c;
        }

        public long or(Long l) {
                return c | l;
        }

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(c) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public int divide(Integer i) {
                return this.c / i;
        }

        public int divide(Byte b) {
                return c / b;
        }

        public int divide(Short s) {
                return c / s;
        }

        public int divide(Character c) {
                return this.c / c;
        }

        public long divide(Long l) {
                return c / l;
        }

        public float divide(Float f) {
                return c / f;
        }

        public double divide(Double d) {
                return c / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return this.c >= i;
        }

        public boolean ge(Byte b) {
                return c >= b;
        }

        public boolean ge(Short s) {
                return c >= s;
        }

        public boolean ge(Character c) {
                return this.c >= c;
        }

        public boolean ge(Long l) {
                return c >= l;
        }

        public boolean ge(Float f) {
                return c >= f;
        }

        public boolean ge(Double d) {
                return c >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return this.c > i;
        }

        public boolean gt(Byte b) {
                return c > b;
        }

        public boolean gt(Short s) {
                return c > s;
        }

        public boolean gt(Character c) {
                return this.c > c;
        }

        public boolean gt(Long l) {
                return c > l;
        }

        public boolean gt(Float f) {
                return c > f;
        }

        public boolean gt(Double d) {
                return c > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return this.c <= i;
        }

        public boolean le(Byte b) {
                return c <= b;
        }

        public boolean le(Short s) {
                return c <= s;
        }

        public boolean le(Character c) {
                return this.c <= c;
        }

        public boolean le(Long l) {
                return c <= l;
        }

        public boolean le(Float f) {
                return c <= f;
        }

        public boolean le(Double d) {
                return c <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return this.c < i;
        }

        public boolean lt(Byte b) {
                return c < b;
        }

        public boolean lt(Short s) {
                return c < s;
        }

        public boolean lt(Character c) {
                return this.c < c;
        }

        public boolean lt(Long l) {
                return c < l;
        }

        public boolean lt(Float f) {
                return c < f;
        }

        public boolean lt(Double d) {
                return c < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public int multiply(Integer i) {
                return this.c * i;
        }

        public int multiply(Byte b) {
                return c * b;
        }

        public int multiply(Short s) {
                return c * s;
        }

        public int multiply(Character c) {
                return this.c * c;
        }

        public long multiply(Long l) {
                return c * l;
        }

        public float multiply(Float f) {
                return c * f;
        }

        public double multiply(Double d) {
                return c * d;
        }

        /*
         * ============
         *   remainder
         * ============
         */

        public int remainder(Integer i) {
                return this.c % i;
        }

        public int remainder(Byte b) {
                return c % b;
        }

        public int remainder(Short s) {
                return c % s;
        }

        public int remainder(Character c) {
                return this.c % c;
        }

        public long remainder(Long l) {
                return c % l;
        }

        /*
         * ============
         *   shiftLeft
         * ============
         */

        public int shiftLeft(Integer i) {
                return this.c << i;
        }

        public int shiftLeft(Byte b) {
                return c << b;
        }

        public int shiftLeft(Short s) {
                return c << s;
        }

        public int shiftLeft(Character c) {
                return this.c << c;
        }

        public int shiftLeft(Long l) {
                return c << l;
        }

        /*
         * ============
         *  shiftRight
         * ============
         */

        public int shiftRight(Integer i) {
                return this.c >> i;
        }

        public int shiftRight(Byte b) {
                return c >> b;
        }

        public int shiftRight(Short s) {
                return c >> s;
        }

        public int shiftRight(Character c) {
                return this.c >> c;
        }

        public int shiftRight(Long l) {
                return c >> l;
        }

        /*
         * ============
         *   subtract
         * ============
         */

        public int subtract(Integer i) {
                return this.c - i;
        }

        public int subtract(Byte b) {
                return c - b;
        }

        public int subtract(Short s) {
                return c - s;
        }

        public int subtract(Character c) {
                return this.c - c;
        }

        public long subtract(Long l) {
                return c - l;
        }

        public float subtract(Float f) {
                return c - f;
        }

        public double subtract(Double d) {
                return c - d;
        }

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        public int unsignedShiftRight(Integer i) {
                return this.c >>> i;
        }

        public int unsignedShiftRight(Byte b) {
                return c >>> b;
        }

        public int unsignedShiftRight(Short s) {
                return c >>> s;
        }

        public int unsignedShiftRight(Character c) {
                return this.c >>> c;
        }

        public int unsignedShiftRight(Long l) {
                return c >>> l;
        }

        /*
         * ============
         *     xor
         * ============
         */

        public int xor(Integer i) {
                return this.c ^ i;
        }

        public int xor(Byte b) {
                return c ^ b;
        }

        public int xor(Short s) {
                return c ^ s;
        }

        public int xor(Character c) {
                return this.c ^ c;
        }

        public long xor(Long l) {
                return c ^ l;
        }

        public boolean xor(Boolean b) throws Throwable {
                return LtRuntime.castToBool(c) ^ b;
        }
}
