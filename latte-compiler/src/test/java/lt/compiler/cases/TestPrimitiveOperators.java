package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.Properties;
import lt.compiler.Scanner;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * primitive operators
 */
public class TestPrimitiveOperators {
        public List<Function<Number, ?>> numberFuncs = Arrays.asList(
                Number::intValue,
                Number::longValue,
                Number::floatValue,
                Number::doubleValue,
                Number::byteValue,
                Number::shortValue,
                (Number n) -> (char) n.intValue()
        );
        public List<Function<Number, ?>> integerFuncs = Arrays.asList(
                Number::intValue,
                Number::longValue,
                Number::byteValue,
                Number::shortValue,
                (Number n) -> (char) n.intValue()
        );

        public static Class<?> retrieveClass(String code, String clsName) throws IOException, SyntaxException, ClassNotFoundException {
                ErrorManager err = new ErrorManager(true);
                Scanner lexicalProcessor = new ScannerSwitcher("test.lt", new StringReader(code), new Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types, semanticProcessor.getTypes());
                Map<String, byte[]> list = codeGenerator.generate();

                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                byte[] bs = list.get(name);
                                if (bs == null) throw new ClassNotFoundException(name);
                                return defineClass(name, bs, 0, bs.length);
                        }
                };

                return classLoader.loadClass(clsName);
        }

        private void testAddSubtractMultiplyDivide(Number a, Number b, Number r, Method method) throws Exception {
                int countDouble = 0;
                int countFloat = 0;
                int countLong = 0;
                int countInt = 0;

                for (Function<Number, ?> f1 : numberFuncs) {
                        Object aa = f1.apply(a);
                        for (Function<Number, ?> f2 : numberFuncs) {
                                Object bb = f2.apply(b);
                                Object rr = method.invoke(null, aa, bb);

                                if (aa instanceof Double || bb instanceof Double) {
                                        ++countDouble;
                                        assertEquals(r.doubleValue(), rr);
                                } else if (aa instanceof Float || bb instanceof Float) {
                                        ++countFloat;
                                        assertEquals(r.floatValue(), rr);
                                } else if (aa instanceof Long || bb instanceof Long) {
                                        ++countLong;
                                        assertEquals(r.longValue(), rr);
                                } else {
                                        ++countInt;
                                        assertEquals(r.intValue(), rr);
                                }
                        }
                }

                assertEquals(11, countFloat);
                assertEquals(9, countLong);
                assertEquals(16, countInt);
                assertEquals(13, countDouble);
        }

        @Test
        public void testAdd() throws Exception {
                Class<?> add = retrieveClass("" +
                                "class TestAdd\n" +
                                "    static\n" +
                                "        method(a, b)= a + b"
                        , "TestAdd");
                Method method = add.getMethod("method", Object.class, Object.class);

                testAddSubtractMultiplyDivide(1, 8, 9, method);
        }

        @Test
        public void testSubtract() throws Exception {
                Class<?> subtract = retrieveClass("" +
                                "class TestSubtract\n" +
                                "    static\n" +
                                "        method(a, b)= a - b"
                        , "TestSubtract");
                Method method = subtract.getMethod("method", Object.class, Object.class);

                testAddSubtractMultiplyDivide(1, 8, -7, method);
        }

        @Test
        public void testMultiply() throws Exception {
                Class<?> multiply = retrieveClass("" +
                                "class TestMultiply\n" +
                                "    static\n" +
                                "        method(a, b)= a * b"
                        , "TestMultiply");
                Method method = multiply.getMethod("method", Object.class, Object.class);

                testAddSubtractMultiplyDivide(4, 3, 12, method);
        }

        @Test
        public void testDivide() throws Exception {
                Class<?> divide = retrieveClass("" +
                                "class TestDivide\n" +
                                "    static\n" +
                                "        method(a, b)= a / b"
                        , "TestDivide");
                Method method = divide.getMethod("method", Object.class, Object.class);

                testAddSubtractMultiplyDivide(4, 3, 4d / 3d, method);
        }

        private void testShift(Number a, Number b, int by, int s, int c, int i, long l, Method method) throws Exception {
                for (Function<Number, ?> f1 : integerFuncs) {
                        Object aa = f1.apply(a);
                        for (Function<Number, ?> f2 : integerFuncs) {
                                Object bb = f2.apply(b);
                                Object rr = method.invoke(null, aa, bb);

                                if (aa instanceof Long) {
                                        assertEquals(l, rr);
                                } else if (aa instanceof Integer) {
                                        assertEquals(i, rr);
                                } else if (aa instanceof Character) {
                                        assertEquals(c, rr);
                                } else if (aa instanceof Short) {
                                        assertEquals(s, rr);
                                } else if (aa instanceof Byte) {
                                        assertEquals(by, rr);
                                } else {
                                        fail();
                                }
                        }
                }
        }

        @Test
        public void testShiftLeft() throws Exception {
                Class<?> shiftLeft = retrieveClass("" +
                                "class TestShiftLeft\n" +
                                "    static\n" +
                                "        method(a, b)= a << b"
                        , "TestShiftLeft");
                Method method = shiftLeft.getMethod("method", Object.class, Object.class);

                testShift(3, 5, ((byte) 3) << 5, ((short) 3) << 5, ((char) 3) << 5, (3 << 5), 3L << 5, method);
        }

        @Test
        public void testShiftRight() throws Exception {
                Class<?> shiftRight = retrieveClass("" +
                                "class TestShiftRight\n" +
                                "    static\n" +
                                "        method(a, b)= a >> b"
                        , "TestShiftRight");
                Method method = shiftRight.getMethod("method", Object.class, Object.class);

                testShift(-123, 2, ((byte) -123) >> 2, ((short) -123) >> 2, ((char) -123) >> 2, -123 >> 2, -123L >> 2, method);
        }

        @Test
        public void testUnsignedShiftRight() throws Exception {
                Class<?> unsignedShiftRight = retrieveClass("" +
                                "class TestUnsignedShiftRight\n" +
                                "    static\n" +
                                "        method(a, b)= a >>> b"
                        , "TestUnsignedShiftRight");
                Method method = unsignedShiftRight.getMethod("method", Object.class, Object.class);

                testShift(-123, 2, ((byte) -123) >>> 2, ((short) -123) >>> 2, ((char) -123) >>> 2, -123 >>> 2, -123L >>> 2, method);
        }

        private void testLogic(Number a, Number b, Number r, int rci, long rcl, Method method) throws Exception {
                int countLong = 0;
                int countInt = 0;

                for (Function<Number, ?> f1 : integerFuncs) {
                        Object aa = f1.apply(a);
                        for (Function<Number, ?> f2 : integerFuncs) {
                                Object bb = f2.apply(b);
                                Object rr = method.invoke(null, aa, bb);

                                if (aa instanceof Long || bb instanceof Long) {
                                        ++countLong;
                                        if (aa instanceof Character) {
                                                assertEquals(rcl, rr);
                                        } else {
                                                assertEquals(r.longValue(), rr);
                                        }
                                } else {
                                        ++countInt;
                                        if (aa instanceof Character) {
                                                assertEquals(rci, rr);
                                        } else {
                                                assertEquals(r.intValue(), rr);
                                        }
                                }
                        }
                }

                assertEquals(9, countLong);
                assertEquals(16, countInt);
        }

        private void testBoolOps(Number a, boolean b, boolean res, Method method) throws Exception {
                for (Function<Number, ?> f : numberFuncs) {
                        Object aa = f.apply(a);
                        Object rr1 = method.invoke(null, aa, b);
                        Object rr2 = method.invoke(null, b, aa);
                        assertEquals(res, rr1);
                        assertEquals(res, rr2);
                }
        }

        @Test
        public void testAnd() throws Exception {
                Class<?> and = retrieveClass("" +
                                "class TestAnd\n" +
                                "    static\n" +
                                "        method(a, b)= a & b"
                        , "TestAnd");
                Method method = and.getMethod("method", Object.class, Object.class);

                testLogic(-123, 4, -123 & 4, ((char) -123) & 4, ((char) -123) & 4L, method);
                testBoolOps(3, true, true, method);
                testBoolOps(3, false, false, method);
                testBoolOps(0, true, false, method);
                testBoolOps(0, false, false, method);
        }

        @Test
        public void testOr() throws Exception {
                Class<?> or = retrieveClass("" +
                                "class TestOr\n" +
                                "    static\n" +
                                "        method(a, b)= a | b"
                        , "TestOr");
                Method method = or.getMethod("method", Object.class, Object.class);

                testLogic(-123, 2, -123 | 2, ((char) -123) | 2, ((char) -123) | 2L, method);
                testBoolOps(3, true, true, method);
                testBoolOps(3, false, true, method);
                testBoolOps(0, true, true, method);
                testBoolOps(0, false, false, method);
        }

        @Test
        public void testXor() throws Exception {
                Class<?> xor = retrieveClass("" +
                                "class TestXor\n" +
                                "    static\n" +
                                "        method(a, b)= a ^ b"
                        , "TestXor");
                Method method = xor.getMethod("method", Object.class, Object.class);

                testLogic(-123, 2, -123 ^ 2, ((char) -123) ^ 2, ((char) -123) ^ 2L, method);
        }

        @Test
        public void testRemainder() throws Exception {
                Class<?> remainder = retrieveClass("" +
                                "class TestRemainder\n" +
                                "    static\n" +
                                "        method(a, b)= a % b"
                        , "TestRemainder");
                Method method = remainder.getMethod("method", Object.class, Object.class);

                testLogic(4, 3, 1, 1, 1L, method);
        }

        @Test
        public void testNegate() throws Exception {
                Class<?> negate = retrieveClass("" +
                                "class TestNegate\n" +
                                "    static\n" +
                                "        method(a)= -a"
                        , "TestNegate");
                Method method = negate.getMethod("method", Object.class);

                Number a = 3;
                Number r = -3;
                for (Function<Number, ?> f : numberFuncs) {
                        Object aa = f.apply(a);
                        Object rr = method.invoke(null, aa);
                        if (aa instanceof Double) {
                                assertEquals(r.doubleValue(), rr);
                        } else if (aa instanceof Float) {
                                assertEquals(r.floatValue(), rr);
                        } else if (aa instanceof Long) {
                                assertEquals(r.longValue(), rr);
                        } else {
                                assertEquals(r.intValue(), rr);
                        }
                }
        }

        @Test
        public void testNot() throws Exception {
                Class<?> not = retrieveClass("" +
                                "class TestNot\n" +
                                "    static\n" +
                                "        method(a)= ~a"
                        , "TestNot");
                Method method = not.getMethod("method", Object.class);

                Number a = 3;
                for (Function<Number, ?> f : integerFuncs) {
                        Object aa = f.apply(a);
                        Object rr = method.invoke(null, aa);
                        if (aa instanceof Long) {
                                assertEquals(~3L, rr);
                        } else {
                                assertEquals(~3, rr);
                        }
                }
        }

        private void testBoolRes(Number a, Number b, boolean res, Method method) throws Exception {
                for (Function<Number, ?> f1 : numberFuncs) {
                        Object aa = f1.apply(a);
                        for (Function<Number, ?> f2 : numberFuncs) {
                                Object bb = f2.apply(b);
                                Object rr = method.invoke(null, aa, bb);
                                assertEquals(res, rr);
                        }
                }
        }

        @Test
        public void testGt() throws Exception {
                Class<?> gt = retrieveClass("" +
                                "class TestGt\n" +
                                "    static\n" +
                                "        method(a, b)= a > b"
                        , "TestGt");
                Method method = gt.getMethod("method", Object.class, Object.class);

                testBoolRes(3, 5, false, method);
                testBoolRes(3, 2, true, method);
                testBoolRes(3, 3, false, method);
        }

        @Test
        public void testGe() throws Exception {
                Class<?> ge = retrieveClass("" +
                                "class TestGe\n" +
                                "    static\n" +
                                "        method(a, b)= a >= b"
                        , "TestGe");
                Method method = ge.getMethod("method", Object.class, Object.class);

                testBoolRes(3, 5, false, method);
                testBoolRes(3, 2, true, method);
                testBoolRes(3, 3, true, method);
        }

        @Test
        public void testLt() throws Exception {
                Class<?> lt = retrieveClass("" +
                                "class TestLt\n" +
                                "    static\n" +
                                "        method(a, b)= a < b"
                        , "TestLt");
                Method method = lt.getMethod("method", Object.class, Object.class);

                testBoolRes(3, 5, true, method);
                testBoolRes(3, 2, false, method);
                testBoolRes(3, 3, false, method);
        }

        @Test
        public void testLe() throws Exception {
                Class<?> le = retrieveClass("" +
                                "class TestLe\n" +
                                "    static\n" +
                                "        method(a, b)= a <= b"
                        , "TestLe");
                Method method = le.getMethod("method", Object.class, Object.class);

                testBoolRes(3, 5, true, method);
                testBoolRes(3, 2, false, method);
                testBoolRes(3, 3, true, method);
        }

        private void testBoolRes(Number a, boolean res, Method method) throws Exception {
                for (Function<Number, ?> f1 : numberFuncs) {
                        Object aa = f1.apply(a);
                        Object rr = method.invoke(null, aa);
                        assertEquals(res, rr);
                }
        }

        @Test
        public void testLogicNot() throws Exception {
                Class<?> logicNot = retrieveClass("" +
                                "class TestLe\n" +
                                "    static\n" +
                                "        method(a)= !a"
                        , "TestLe");
                Method method = logicNot.getMethod("method", Object.class);

                testBoolRes(2, false, method);
                testBoolRes(0, true, method);
                assertEquals(false, method.invoke(null, true));
                assertEquals(true, method.invoke(null, false));
        }

        @Test
        public void testStringObject() throws Exception {
                Class<?> str = retrieveClass("" +
                                "class TestStringObject\n" +
                                "    static\n" +
                                "        method(a, b)= a + b"
                        , "TestStringObject");
                Method method = str.getMethod("method", Object.class, Object.class);

                assertEquals("abc", method.invoke(null, "a", "bc"));
                assertEquals("abc", method.invoke(null, "ab", "c"));
                assertEquals("atrue", method.invoke(null, "a", true));
                assertEquals("truea", method.invoke(null, true, "a"));
                String x = method.invoke(null, new Object(), "a").toString();
                assertTrue(x.startsWith("java.lang.Object@") && x.endsWith("a"));
        }

        @Test
        public void testPow() throws Exception {
                Class<?> pow = retrieveClass("" +
                                "class TestPow\n" +
                                "    static\n" +
                                "        method(a,b) = a ^^ b"
                        , "TestPow");
                double result = Math.pow(3, 4);
                Method method = pow.getMethod("method", Object.class, Object.class);
                for (Function<Number, ?> f1 : numberFuncs) {
                        Object a = f1.apply(3);
                        for (Function<Number, ?> f2 : numberFuncs) {
                                Object b = f2.apply(4);
                                assertEquals(result, method.invoke(null, a, b));
                        }
                }
        }
}
