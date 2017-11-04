package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.Properties;
import lt.compiler.Scanner;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function1;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * primitive operators
 */
public class TestPrimitiveOperators {
        @SuppressWarnings("unchecked")
        public List<Function1<Object, Number>> numberFuncs = Arrays.asList(
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.intValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.longValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.floatValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.doubleValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.byteValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.shortValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number n) throws Exception {
                                return (char) n.intValue();
                        }
                }
        );
        @SuppressWarnings("unchecked")
        public List<Function1<Object, Number>> integerFuncs = Arrays.asList(
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.intValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.longValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.byteValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number number) throws Exception {
                                return number.shortValue();
                        }
                },
                new Function1<Object, Number>() {
                        @Override
                        public Object apply(Number n) throws Exception {
                                return (char) n.intValue();
                        }
                }
        );

        public static Class<?> retrieveClass(String code, String clsName) throws IOException, SyntaxException, ClassNotFoundException {
                ErrorManager err = new ErrorManager(true);
                Scanner lexicalProcessor = new ScannerSwitcher("test.lt", new StringReader(code), new Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<String, List<Statement>>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types, semanticProcessor.getTypes());
                final Map<String, byte[]> list = codeGenerator.generate();

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

                for (Function1<Object, Number> f1 : numberFuncs) {
                        Object aa = f1.apply(a);
                        for (Function1<Object, Number> f2 : numberFuncs) {
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
                for (Function1<Object, Number> f1 : integerFuncs) {
                        Object aa = f1.apply(a);
                        for (Function1<Object, Number> f2 : integerFuncs) {
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

                for (Function1<Object, Number> f1 : integerFuncs) {
                        Object aa = f1.apply(a);
                        for (Function1<Object, Number> f2 : integerFuncs) {
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
                for (Function1<Object, Number> f : numberFuncs) {
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
                for (Function1<Object, Number> f : numberFuncs) {
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
                for (Function1<Object, Number> f : integerFuncs) {
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
                for (Function1<Object, Number> f1 : numberFuncs) {
                        Object aa = f1.apply(a);
                        for (Function1<Object, Number> f2 : numberFuncs) {
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
                for (Function1<Object, Number> f1 : numberFuncs) {
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
                for (Function1<Object, Number> f1 : numberFuncs) {
                        Object a = f1.apply(3);
                        for (Function1<Object, Number> f2 : numberFuncs) {
                                Object b = f2.apply(4);
                                assertEquals(result, method.invoke(null, a, b));
                        }
                }
        }

        @Test
        public void testImplicitCastWhenInvokingMethods() throws Exception {
                Class<?> c = retrieveClass("" +
                                "class TestImplicitCastWhenInvokingMethods\n" +
                                "    static\n" +
                                "        private i(i:int)=i\n" +
                                "        private l(l:long)=l\n" +
                                "        private f(f:float)=f\n" +
                                "        private d(d:double)=d\n" +
                                "        b2i(b:byte)=i(b)\n" +
                                "        b2l(b:byte)=l(b)\n" +
                                "        b2f(b:byte)=f(b)\n" +
                                "        b2d(b:byte)=d(b)\n" +
                                "        s2i(b:short)=i(b)\n" +
                                "        s2l(b:short)=l(b)\n" +
                                "        s2f(b:short)=f(b)\n" +
                                "        s2d(b:short)=d(b)\n" +
                                "        c2i(b:char)=i(b)\n" +
                                "        c2l(b:char)=l(b)\n" +
                                "        c2f(b:char)=f(b)\n" +
                                "        c2d(b:char)=d(b)\n" +
                                "        i2l(b:int)=l(b)\n" +
                                "        i2f(b:int)=f(b)\n" +
                                "        i2d(b:int)=d(b)\n" +
                                "        l2f(b:long)=f(b)\n" +
                                "        l2d(b:long)=d(b)\n" +
                                "        f2d(b:float)=d(b)\n"
                        , "TestImplicitCastWhenInvokingMethods");
                for (Class<?> from : Arrays.<Class<?>>asList(
                        byte.class, short.class, char.class,
                        int.class, long.class, float.class
                )) {
                        for (Class<?> to : Arrays.<Class<?>>asList(int.class, long.class, float.class, double.class)) {
                                if (from == to) {
                                        continue;
                                }
                                if (from == long.class && to == int.class) {
                                        continue;
                                } else if (from == float.class && (to == int.class || to == long.class)) {
                                        continue;
                                }

                                Method x = c.getMethod(from.getName().charAt(0) + "2" + to.getName().charAt(0), from);
                                Object input = produce(from);
                                Object output = produce(to);

                                Object res = x.invoke(null, input);
                                assertEquals(output, res);
                        }
                }
        }

        private Object produce(Class<?> t) {
                if (t == byte.class) {
                        return (byte) 1;
                } else if (t == short.class) {
                        return (short) 1;
                } else if (t == char.class) {
                        return (char) 1;
                } else if (t == int.class) {
                        return 1;
                } else if (t == long.class) {
                        return 1L;
                } else if (t == float.class) {
                        return 1f;
                } else if (t == double.class) {
                        return 1d;
                } else throw new RuntimeException();
        }

        @Test
        public void testOverwritten() throws Exception {
                Class<?> c = retrieveClass("" +
                                "class TestOverwritten1\n" +
                                "  static\n" +
                                "    private x(i:int)=i\n" +
                                "    private x(f:float)=f\n" +
                                "    private x(l:long)=l\n" +
                                "    private x(d:double)=d\n" +
                                "    def method_byte(o:byte)=x(o)\n" +
                                "    def method_short(o:short)=x(o)\n" +
                                "    def method_char(o:char)=x(o)\n" +
                                "    def method_int(o:int)=x(o)"
                        , "TestOverwritten1");
                for (Class<?> t : Arrays.<Class<?>>asList(byte.class, short.class, char.class, int.class)) {
                        Method method = c.getMethod("method_" + t.getName(), t);
                        Object input = produce(t);
                        Object output = produce(int.class);

                        Object result = method.invoke(null, input);
                        assertEquals("input " + t + ", output: int, result " + result.getClass(), output, result);
                }

                c = retrieveClass("" +
                                "class TestOverwritten2\n" +
                                "  static\n" +
                                "    private x(f:float)=f\n" +
                                "    private x(l:long)=l\n" +
                                "    private x(d:double)=d\n" +
                                "    def method_byte(o:byte)=x(o)\n" +
                                "    def method_short(o:short)=x(o)\n" +
                                "    def method_char(o:char)=x(o)\n" +
                                "    def method_int(o:int)=x(o)"
                        , "TestOverwritten2");
                for (Class<?> t : Arrays.<Class<?>>asList(byte.class, short.class, char.class, int.class)) {
                        Method method = c.getMethod("method_" + t.getName(), t);
                        Object input = produce(t);
                        Object output = produce(long.class);

                        Object result = method.invoke(null, input);
                        assertEquals("input " + t + ", output: long, result " + result.getClass(), output, result);
                }

                c = retrieveClass("" +
                                "class TestOverwritten3\n" +
                                "  static\n" +
                                "    private x(f:float)=f\n" +
                                "    private x(d:double)=d\n" +
                                "    def method_byte(o:byte)=x(o)\n" +
                                "    def method_short(o:short)=x(o)\n" +
                                "    def method_char(o:char)=x(o)\n" +
                                "    def method_int(o:int)=x(o)"
                        , "TestOverwritten3");
                for (Class<?> t : Arrays.<Class<?>>asList(byte.class, short.class, char.class, int.class)) {
                        Method method = c.getMethod("method_" + t.getName(), t);
                        Object input = produce(t);
                        Object output = produce(float.class);

                        Object result = method.invoke(null, input);
                        assertEquals("input " + t + ", output: float, result " + result.getClass(), output, result);
                }
        }
}
