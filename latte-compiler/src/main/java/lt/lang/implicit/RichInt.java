package lt.lang.implicit;

import lt.runtime.LtRuntime;
import lt.util.RangeList;

/**
 * rich Integer
 */
public class RichInt {
        private Integer i;

        public RichInt(Integer i) {
                this.i = i;
        }

        public int not() {
                return ~i;
        }

        public int negate() {
                return -i;
        }

        public boolean logicNot() throws Throwable {
                return !LtRuntime.castToBool(i);
        }

        /*
         * ============
         *     add
         * ============
         */

        public int add(Integer i) {
                return this.i + i;
        }

        public int add(Byte b) {
                return i + b;
        }

        public int add(Short s) {
                return i + s;
        }

        public int add(Character c) {
                return i + c;
        }

        public long add(Long l) {
                return i + l;
        }

        public float add(Float f) {
                return i + f;
        }

        public double add(Double d) {
                return i + d;
        }

        /*
         * ============
         *     and
         * ============
         */

        public int and(Integer i) {
                return this.i & i;
        }

        public int and(Byte b) {
                return i & b;
        }

        public int and(Short s) {
                return i & s;
        }

        public int and(Character c) {
                return i & c;
        }

        public long and(Long l) {
                return i & l;
        }

        public boolean and(Boolean b) throws Throwable {
                return LtRuntime.castToBool(i) & b;
        }

        /*
         * ============
         *     or
         * ============
         */

        public int or(Integer i) {
                return this.i | i;
        }

        public int or(Byte b) {
                return i | b;
        }

        public int or(Short s) {
                return i | s;
        }

        public int or(Character c) {
                return i | c;
        }

        public long or(Long l) {
                return i | l;
        }

        public boolean or(Boolean b) throws Throwable {
                return LtRuntime.castToBool(i) | b;
        }

        /*
         * ============
         *    divide
         * ============
         */

        public int divide(Integer i) {
                return this.i / i;
        }

        public int divide(Byte b) {
                return i / b;
        }

        public int divide(Short s) {
                return i / s;
        }

        public int divide(Character c) {
                return i / c;
        }

        public long divide(Long l) {
                return i / l;
        }

        public float divide(Float f) {
                return i / f;
        }

        public double divide(Double d) {
                return i / d;
        }

        /*
         * ============
         *      ge
         * ============
         */

        public boolean ge(Integer i) {
                return this.i >= i;
        }

        public boolean ge(Byte b) {
                return i >= b;
        }

        public boolean ge(Short s) {
                return i >= s;
        }

        public boolean ge(Character c) {
                return i >= c;
        }

        public boolean ge(Long l) {
                return i >= l;
        }

        public boolean ge(Float f) {
                return i >= f;
        }

        public boolean ge(Double d) {
                return i >= d;
        }

        /*
         * ============
         *      gt
         * ============
         */

        public boolean gt(Integer i) {
                return this.i > i;
        }

        public boolean gt(Byte b) {
                return i > b;
        }

        public boolean gt(Short s) {
                return i > s;
        }

        public boolean gt(Character c) {
                return i > c;
        }

        public boolean gt(Long l) {
                return i > l;
        }

        public boolean gt(Float f) {
                return i > f;
        }

        public boolean gt(Double d) {
                return i > d;
        }

        /*
         * ============
         *      le
         * ============
         */

        public boolean le(Integer i) {
                return this.i <= i;
        }

        public boolean le(Byte b) {
                return i <= b;
        }

        public boolean le(Short s) {
                return i <= s;
        }

        public boolean le(Character c) {
                return i <= c;
        }

        public boolean le(Long l) {
                return i <= l;
        }

        public boolean le(Float f) {
                return i <= f;
        }

        public boolean le(Double d) {
                return i <= d;
        }

        /*
         * ============
         *      lt
         * ============
         */

        public boolean lt(Integer i) {
                return this.i < i;
        }

        public boolean lt(Byte b) {
                return i < b;
        }

        public boolean lt(Short s) {
                return i < s;
        }

        public boolean lt(Character c) {
                return i < c;
        }

        public boolean lt(Long l) {
                return i < l;
        }

        public boolean lt(Float f) {
                return i < f;
        }

        public boolean lt(Double d) {
                return i < d;
        }

        /*
         * ============
         *   multiply
         * ============
         */

        public int multiply(Integer i) {
                return this.i * i;
        }

        public int multiply(Byte b) {
                return i * b;
        }

        public int multiply(Short s) {
                return i * s;
        }

        public int multiply(Character c) {
                return i * c;
        }

        public long multiply(Long l) {
                return i * l;
        }

        public float multiply(Float f) {
                return i * f;
        }

        public double multiply(Double d) {
                return i * d;
        }

        /*
         * ============
         *   remainder
         * ============
         */

        public int remainder(Integer i) {
                return this.i % i;
        }

        public int remainder(Byte b) {
                return i % b;
        }

        public int remainder(Short s) {
                return i % s;
        }

        public int remainder(Character c) {
                return i % c;
        }

        public long remainder(Long l) {
                return i % l;
        }

        /*
         * ============
         *   shiftLeft
         * ============
         */

        public int shiftLeft(Integer i) {
                return this.i << i;
        }

        public int shiftLeft(Byte b) {
                return i << b;
        }

        public int shiftLeft(Short s) {
                return i << s;
        }

        public int shiftLeft(Character c) {
                return i << c;
        }

        public int shiftLeft(Long l) {
                return i << l;
        }

        /*
         * ============
         *  shiftRight
         * ============
         */

        public int shiftRight(Integer i) {
                return this.i >> i;
        }

        public int shiftRight(Byte b) {
                return i >> b;
        }

        public int shiftRight(Short s) {
                return i >> s;
        }

        public int shiftRight(Character c) {
                return i >> c;
        }

        public int shiftRight(Long l) {
                return i >> l;
        }

        /*
         * ============
         *   subtract
         * ============
         */

        public int subtract(Integer i) {
                return this.i - i;
        }

        public int subtract(Byte b) {
                return i - b;
        }

        public int subtract(Short s) {
                return i - s;
        }

        public int subtract(Character c) {
                return i - c;
        }

        public long subtract(Long l) {
                return i - l;
        }

        public float subtract(Float f) {
                return i - f;
        }

        public double subtract(Double d) {
                return i - d;
        }

        /*
         * ====================
         *  unsignedShiftRight
         * ====================
         */

        public int unsignedShiftRight(Integer i) {
                return this.i >>> i;
        }

        public int unsignedShiftRight(Byte b) {
                return i >>> b;
        }

        public int unsignedShiftRight(Short s) {
                return i >>> s;
        }

        public int unsignedShiftRight(Character c) {
                return i >>> c;
        }

        public int unsignedShiftRight(Long l) {
                return i >>> l;
        }

        /*
         * ============
         *     xor
         * ============
         */

        public int xor(Integer i) {
                return this.i ^ i;
        }

        public int xor(Byte b) {
                return i ^ b;
        }

        public int xor(Short s) {
                return i ^ s;
        }

        public int xor(Character c) {
                return i ^ c;
        }

        public long xor(Long l) {
                return i ^ l;
        }

        public boolean xor(Boolean b) throws Throwable {
                return LtRuntime.castToBool(i) ^ b;
        }

        /*
         * =========
         *   range
         * =========
         */

        public RangeList to(Integer i) {
                return new RangeList(this.i, i, true);
        }

        public RangeList until(Integer i) {
                return new RangeList(this.i, i, false);
        }

        /*
         * ============
         *     pow
         * ============
         */

        public double pow(Integer i) {
                return Math.pow(this.i, i);
        }

        public double pow(Long l) {
                return Math.pow(i, l);
        }

        public double pow(Float f) {
                return Math.pow(i, f);
        }

        public double pow(Double d) {
                return Math.pow(i, d);
        }

        public double pow(Byte b) {
                return Math.pow(i, b);
        }

        public double pow(Short s) {
                return Math.pow(i, s);
        }

        public double pow(Character c) {
                return Math.pow(i, c);
        }
}
