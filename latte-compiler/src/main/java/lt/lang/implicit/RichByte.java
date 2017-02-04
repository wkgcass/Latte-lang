package lt.lang.implicit;

import lt.lang.LtRuntime;

/**
 * rich Byte
 */
public class RichByte {
        private Byte b;

        public RichByte(Byte b) {
                this.b = b;
        }

        public int not() {
                return ~b;
        }

        public int negate() {
                return -b;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(b);
        }

        /*
         * ============
         *     add
         * ============
         */

        public int add(Integer i) {
                return this.b + i;
        }

        public int add(Byte b) {
                return this.b + b;
        }

        public int add(Short s) {
                return b + s;
        }

        public int add(Character c) {
                return b + c;
        }

        public long add(Long l) {
                return b + l;
        }

        public float add(Float f) {
                return b + f;
        }

        public double add(Double d) {
                return b + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public int and(Integer i) {
                return this.b & i;
        }

        public int and(Byte b) {
                return this.b & b;
        }

        public int and(Short s) {
                return b & s;
        }

        public int and(Character c) {
                return b & c;
        }

        public long and(Long l) {
                return b & l;
        }

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.b) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public int or(Integer i) {
                return this.b | i;
        }

        public int or(Byte b) {
                return this.b | b;
        }

        public int or(Short s) {
                return b | s;
        }

        public int or(Character c) {
                return b | c;
        }

        public long or(Long l) {
                return b | l;
        }

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.b) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public int divide(Integer i) {
                return this.b / i;
        }

        public int divide(Byte b) {
                return this.b / b;
        }

        public int divide(Short s) {
                return b / s;
        }

        public int divide(Character c) {
                return b / c;
        }

        public long divide(Long l) {
                return b / l;
        }

        public float divide(Float f) {
                return b / f;
        }

        public double divide(Double d) {
                return b / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return this.b >= i;
        }

        public boolean ge(Byte b) {
                return this.b >= b;
        }

        public boolean ge(Short s) {
                return b >= s;
        }

        public boolean ge(Character c) {
                return b >= c;
        }

        public boolean ge(Long l) {
                return b >= l;
        }

        public boolean ge(Float f) {
                return b >= f;
        }

        public boolean ge(Double d) {
                return b >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return this.b > i;
        }

        public boolean gt(Byte b) {
                return this.b > b;
        }

        public boolean gt(Short s) {
                return b > s;
        }

        public boolean gt(Character c) {
                return b > c;
        }

        public boolean gt(Long l) {
                return b > l;
        }

        public boolean gt(Float f) {
                return b > f;
        }

        public boolean gt(Double d) {
                return b > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return this.b <= i;
        }

        public boolean le(Byte b) {
                return this.b <= b;
        }

        public boolean le(Short s) {
                return b <= s;
        }

        public boolean le(Character c) {
                return b <= c;
        }

        public boolean le(Long l) {
                return b <= l;
        }

        public boolean le(Float f) {
                return b <= f;
        }

        public boolean le(Double d) {
                return b <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return this.b < i;
        }

        public boolean lt(Byte b) {
                return this.b < b;
        }

        public boolean lt(Short s) {
                return b < s;
        }

        public boolean lt(Character c) {
                return b < c;
        }

        public boolean lt(Long l) {
                return b < l;
        }

        public boolean lt(Float f) {
                return b < f;
        }

        public boolean lt(Double d) {
                return b < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public int multiply(Integer i) {
                return this.b * i;
        }

        public int multiply(Byte b) {
                return this.b * b;
        }

        public int multiply(Short s) {
                return b * s;
        }

        public int multiply(Character c) {
                return b * c;
        }

        public long multiply(Long l) {
                return b * l;
        }

        public float multiply(Float f) {
                return b * f;
        }

        public double multiply(Double d) {
                return b * d;
        }

        /*
         * ============
         *   remainder
         * ============
         */

        public int remainder(Integer i) {
                return this.b % i;
        }

        public int remainder(Byte b) {
                return this.b % b;
        }

        public int remainder(Short s) {
                return b % s;
        }

        public int remainder(Character c) {
                return b % c;
        }

        public long remainder(Long l) {
                return b % l;
        }

        /*
         * ============
         *   shiftLeft
         * ============
         */

        public int shiftLeft(Integer i) {
                return this.b << i;
        }

        public int shiftLeft(Byte b) {
                return this.b << b;
        }

        public int shiftLeft(Short s) {
                return b << s;
        }

        public int shiftLeft(Character c) {
                return b << c;
        }

        public int shiftLeft(Long l) {
                return b << l;
        }

        /*
         * ============
         *  shiftRight
         * ============
         */

        public int shiftRight(Integer i) {
                return this.b >> i;
        }

        public int shiftRight(Byte b) {
                return this.b >> b;
        }

        public int shiftRight(Short s) {
                return b >> s;
        }

        public int shiftRight(Character c) {
                return b >> c;
        }

        public int shiftRight(Long l) {
                return b >> l;
        }

        /*
         * ============
         *   subtract
         * ============
         */

        public int subtract(Integer i) {
                return this.b - i;
        }

        public int subtract(Byte b) {
                return this.b - b;
        }

        public int subtract(Short s) {
                return b - s;
        }

        public int subtract(Character c) {
                return b - c;
        }

        public long subtract(Long l) {
                return b - l;
        }

        public float subtract(Float f) {
                return b - f;
        }

        public double subtract(Double d) {
                return b - d;
        }

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        public int unsignedShiftRight(Integer i) {
                return this.b >>> i;
        }

        public int unsignedShiftRight(Byte b) {
                return this.b >>> b;
        }

        public int unsignedShiftRight(Short s) {
                return b >>> s;
        }

        public int unsignedShiftRight(Character c) {
                return b >>> c;
        }

        public int unsignedShiftRight(Long l) {
                return b >>> l;
        }

        /*
         * ============
         *     xor
         * ============
         */

        public int xor(Integer i) {
                return this.b ^ i;
        }

        public int xor(Byte b) {
                return this.b ^ b;
        }

        public int xor(Short s) {
                return b ^ s;
        }

        public int xor(Character c) {
                return b ^ c;
        }

        public long xor(Long l) {
                return b ^ l;
        }

        public boolean xor(Boolean b) throws Throwable {
                return LtRuntime.castToBool(this.b) ^ b;
        }

        /*
         * ============
         *     pow
         * ============
         */

        public double pow(Integer i) {
                return Math.pow(b, i);
        }

        public double pow(Long l) {
                return Math.pow(b, l);
        }

        public double pow(Float f) {
                return Math.pow(b, f);
        }

        public double pow(Double d) {
                return Math.pow(b, d);
        }

        public double pow(Byte b) {
                return Math.pow(this.b, b);
        }

        public double pow(Short s) {
                return Math.pow(b, s);
        }

        public double pow(Character c) {
                return Math.pow(b, c);
        }
}
