package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.Scanner;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import lt.lang.RangeList;
import lt.lang.Undefined;
import lt.lang.Wrapper;
import lt.lang.function.Function1;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * test code generator
 */
public class TestCodeGen {
        private Class<?> retrieveClass(String code, String clsName) throws IOException, SyntaxException, ClassNotFoundException {
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader(code), new Scanner.Properties(), new ErrorManager(true));
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();

                byte[] bs = list.get(clsName);
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, bs, 0, bs.length);
                        }
                };

                return classLoader.loadClass(clsName);
        }

        @Test
        public void testPkg() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "# my::test\n" +
                                "class TestPkg",
                        "my.test.TestPkg");
                assertEquals("my.test.TestPkg", cls.getName());
        }

        @Test
        public void testClass() throws Exception {
                Class<?> cls = retrieveClass("class TestClass", "TestClass");
                assertEquals("TestClass", cls.getName());
                assertEquals(Modifier.PUBLIC, cls.getModifiers());
                assertEquals(1, cls.getDeclaredConstructors().length);
                assertEquals(0, cls.getDeclaredConstructors()[0].getParameterCount());
        }

        @Test
        public void testConstructorParam() throws Exception {
                Class<?> cls = retrieveClass("class TestConstructorParam(a,b)\n", "TestConstructorParam");
                assertEquals("TestConstructorParam", cls.getName());
                assertEquals(Modifier.PUBLIC, cls.getModifiers());
                assertEquals(1, cls.getDeclaredConstructors().length);
                assertEquals(2, cls.getDeclaredConstructors()[0].getParameterCount());
        }

        @Test
        public void testField() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestField\n" +
                                "    a=1\n" +
                                "    b:int\n" +
                                "    c:Integer",
                        "TestField");
                assertEquals(3, cls.getDeclaredFields().length);
                assertEquals(Object.class, cls.getDeclaredField("a").getType());
                assertEquals(int.class, cls.getDeclaredField("b").getType());
                assertEquals(Integer.class, cls.getDeclaredField("c").getType());
        }

        @Test
        public void testMethod() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestMethod\n" +
                                "    method():Unit",
                        "TestMethod");
                assertEquals(1, cls.getDeclaredMethods().length);
                Method m = cls.getDeclaredMethods()[0];
                assertEquals("method", m.getName());
                assertEquals(void.class, m.getReturnType());
                assertEquals(0, m.getParameterCount());
                assertEquals(Modifier.PUBLIC, m.getModifiers());
        }

        @Test
        public void testModifier() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "pri class TestModifier\n" +
                                "    pub field\n" +
                                "    pro method():Unit",
                        "TestModifier");
                assertEquals(Modifier.PUBLIC, cls.getModifiers());

                assertEquals(1, cls.getDeclaredConstructors().length);
                assertEquals(Modifier.PRIVATE, cls.getDeclaredConstructors()[0].getModifiers());

                assertEquals(1, cls.getDeclaredFields().length);
                assertEquals(Modifier.PUBLIC, cls.getDeclaredFields()[0].getModifiers());

                assertEquals(1, cls.getDeclaredMethods().length);
                assertEquals(Modifier.PROTECTED, cls.getDeclaredMethods()[0].getModifiers());
        }

        @Test
        public void testReturnStr() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestReturnStr\n" +
                                "    static\n" +
                                "        method()='abc'",
                        "TestReturnStr");
                Method method = cls.getMethod("method");
                String str = (String) method.invoke(null);
                assertEquals("abc", str);
        }

        @Test
        public void testInvokeStatic() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeStatic\n" +
                                "    static\n" +
                                "        method()=System.getProperties()",
                        "TestInvokeStatic");
                Method method = cls.getMethod("method");
                Object o = method.invoke(null);
                assertTrue(o instanceof Properties);
        }

        @Test
        public void testInvokeVirtual() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeVirtual\n" +
                                "    static\n" +
                                "        method()=System.getProperties().get('java.version')",
                        "TestInvokeVirtual");

                Method method = cls.getMethod("method");
                Object o = method.invoke(null);
                assertEquals(System.getProperties().get("java.version"), o);
        }

        @Test
        public void testInvokeInterface() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#> java::util::_\n" +
                                "class TestInvokeInterface\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <Collections.singletonList('abc').size()",
                        "TestInvokeInterface");

                Method method = cls.getMethod("method");
                Object o = method.invoke(null);
                assertEquals(1, o);
        }

        @Test
        public void testInvokeSpecial() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeSpecial\n" +
                                "    pri priMethod()\n" +
                                "        <1\n" +
                                "    method()\n" +
                                "        <priMethod()",
                        "TestInvokeSpecial");

                Object o = cls.newInstance();
                Method method = cls.getMethod("method");
                Object res = method.invoke(o);
                assertEquals(1, res);
        }

        @Test
        public void testInvokeDynamic() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeDynamic\n" +
                                "    static\n" +
                                "        method(o)\n" +
                                "            <o.size()",
                        "TestInvokeDynamic");

                Method method = cls.getMethod("method", Object.class);
                List<Integer> l = new ArrayList<>();
                l.add(1);
                l.add(2);
                Object o = method.invoke(null, l);
                assertEquals(2, o);
        }

        @Test
        public void testInvokeDynamic_WithArgs() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeDynamic\n" +
                                "    static\n" +
                                "        method(o)\n" +
                                "            <o.add(3)",
                        "TestInvokeDynamic");

                Method method = cls.getMethod("method", Object.class);
                List<Integer> l = new ArrayList<>();
                l.add(1);
                l.add(2);
                method.invoke(null, l);
                assertEquals(Arrays.asList(1, 2, 3), l);
        }

        @Test
        public void testInvokeDynamic_WithArgs_Method_Primitive() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeDynamic\n" +
                                "    static\n" +
                                "        method(o)\n" +
                                "            <o.add(0,3)",
                        "TestInvokeDynamic");

                Method method = cls.getMethod("method", Object.class);
                List<Integer> l = new ArrayList<>();
                l.add(1);
                l.add(2);
                method.invoke(null, l);
                assertEquals(Arrays.asList(3, 1, 2), l);
        }

        @Test
        public void testTstore() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeDynamic\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            i=1\n" +
                                "            <i",
                        "TestInvokeDynamic");

                Method method = cls.getMethod("method");
                assertEquals(1, method.invoke(null));
        }

        @Test
        public void testNew() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#>java::util::_\n" +
                                "class TestNew\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <ArrayList()",
                        "TestNew");

                Method method = cls.getMethod("method");
                Object o = method.invoke(null);
                assertTrue(o instanceof ArrayList);
        }

        @Test
        public void testTwoVarOp() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#>java::util::_\n" +
                                "class TestTwoVarOp\n" +
                                "    static\n" +
                                "        method(a,b)\n" +
                                "            <a|b",
                        "TestTwoVarOp");

                Method method = cls.getMethod("method", Object.class, Object.class);
                assertEquals(3, method.invoke(null, 1, 2));
                assertEquals(true, method.invoke(null, false, true));
                BigInteger a = new BigInteger("1");
                BigInteger b = new BigInteger("2");
                assertEquals(new BigInteger("3"), method.invoke(null, a, b)); // BigInteger.or(BigInteger)
        }

        @Test
        public void testRange() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestRange\n" +
                                "    static\n" +
                                "        method1()\n" +
                                "            <1..10\n" +
                                "        method2()\n" +
                                "            <1.:10\n" +
                                "        method3()\n" +
                                "            <10..1\n" +
                                "        method4()\n" +
                                "            <10.:1",
                        "TestRange");

                Method method1 = cls.getMethod("method1");
                List range1 = (RangeList) method1.invoke(null);
                assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), range1);

                Method method2 = cls.getMethod("method2");
                List range2 = (RangeList) method2.invoke(null);
                assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), range2);

                Method method3 = cls.getMethod("method3");
                List range3 = (RangeList) method3.invoke(null);
                assertEquals(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), range3);

                Method method4 = cls.getMethod("method4");
                List range4 = (RangeList) method4.invoke(null);
                assertEquals(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2), range4);
        }

        @Test
        public void testPow() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestPow\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <2^^4\n",
                        "TestPow");
                Method method = cls.getMethod("method");
                assertEquals(16.0, method.invoke(null));
        }

        @Test
        public void testPowObj() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestPowObj\n" +
                                "    static\n" +
                                "        method(a,b)\n" +
                                "            <a^^b\n",
                        "TestPowObj");
                Method method = cls.getMethod("method", Object.class, Object.class);
                assertEquals(new BigInteger("16"), method.invoke(null, new BigInteger("2"), 4));
        }

        @Test
        public void testIn() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#>java::util::_\n" +
                                "class TestIn\n" +
                                "    static\n" +
                                "        method(a,ls)\n" +
                                "            <a in ls\n",
                        "TestIn");
                Method method = cls.getMethod("method", Object.class, Object.class);
                assertEquals(true, method.invoke(null, 1, Arrays.asList(1, 2)));
                assertEquals(false, method.invoke(null, 3, Arrays.asList(1, 2)));
        }

        @Test
        public void testInRange() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#>java::util::_\n" +
                                "class TestInRange\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <2 in 1..2\n",
                        "TestInRange");
                Method method = cls.getMethod("method");
                assertEquals(true, method.invoke(null));
        }

        @Test
        public void testIf() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestIf\n" +
                                "    static\n" +
                                "        method(a)\n" +
                                "            if a\n" +
                                "                <1\n" +
                                "            else\n" +
                                "                <2",
                        "TestIf");

                Method method = cls.getMethod("method", Object.class);
                assertEquals(1, method.invoke(null, true));
                assertEquals(2, method.invoke(null, false));

                assertEquals(1, method.invoke(null, 1)); // if 1
                assertEquals(2, method.invoke(null, 0)); // if 0

                assertEquals(2, method.invoke(null, (Object) null)); // if null

                assertEquals(2, method.invoke(null, Undefined.get())); // undefined
        }

        @Test
        public void testIfComplex() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestIfComplex\n" +
                                "    static\n" +
                                "        method(a,b,c)\n" +
                                "            i=0\n" +
                                "            if a\n" +
                                "                i=1\n" +
                                "            elseif b\n" +
                                "                i=2\n" +
                                "            elseif c\n" +
                                "                i=3\n" +
                                "            else\n" +
                                "                i=4\n" +
                                "            <i",
                        "TestIfComplex");

                Method method = cls.getMethod("method", Object.class, Object.class, Object.class);
                assertEquals(1, method.invoke(null, true, false, false));
                assertEquals(1, method.invoke(null, true, true, false));
                assertEquals(1, method.invoke(null, true, false, true));
                assertEquals(1, method.invoke(null, true, true, true));

                assertEquals(2, method.invoke(null, false, true, false));
                assertEquals(2, method.invoke(null, false, true, true));

                assertEquals(3, method.invoke(null, false, false, true));

                assertEquals(4, method.invoke(null, false, false, false));
        }

        @Test
        public void testWhile() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestWhile\n" +
                                "    static\n" +
                                "        method(a:int)\n" +
                                "            s=StringBuilder()\n" +
                                "            while a\n" +
                                "                s.append(a)\n" +
                                "                a=a-1\n" +
                                "            <s.toString()",
                        "TestWhile");

                Method method = cls.getMethod("method", int.class);
                assertEquals("321", method.invoke(null, 3));
        }

        @Test
        public void testDoWhile() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestWhile\n" +
                                "    static\n" +
                                "        method(a:int)\n" +
                                "            s=StringBuilder()\n" +
                                "            do\n" +
                                "                s.append(a)\n" +
                                "                a=a-1\n" +
                                "            while a\n" +
                                "            <s.toString()",
                        "TestWhile");

                Method method = cls.getMethod("method", int.class);
                assertEquals("321", method.invoke(null, 3));
        }

        @Test
        public void testFor() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestFor\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            s=StringBuilder()\n" +
                                "            for i in 1..3\n" +
                                "                s.append(i)\n" +
                                "            <s.toString()",
                        "TestFor");

                Method method = cls.getMethod("method");
                assertEquals("123", method.invoke(null));
        }

        @Test
        public void testThrow() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestThrow\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            throw RuntimeException('ex')",
                        "TestThrow");

                Method method = cls.getMethod("method");
                try {
                        method.invoke(null);
                        fail("method should throw an exception");
                } catch (InvocationTargetException e) {
                        assertTrue(e.getCause() instanceof RuntimeException);
                        assertEquals("ex", e.getCause().getMessage());
                }
        }

        @Test
        public void testTryCatch() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestTryCatch\n" +
                                "    static\n" +
                                "        method(func)\n" +
                                "            try\n" +
                                "                func.apply()\n" +
                                "            catch e\n" +
                                "                if e is type NullPointerException or e is type ClassCastException\n" +
                                "                    <1\n" +
                                "                elseif e is type Error\n" +
                                "                    <e.getMessage()\n" +
                                "                elseif e is type Throwable\n" +
                                "                    <3\n" +
                                "            <4",
                        "TestTryCatch");

                Method method = cls.getMethod("method", Object.class);
                // null pointer exception
                assertEquals(1, method.invoke(null, (I) () -> {
                        throw new NullPointerException();
                }));
                // class cast exception
                assertEquals(1, method.invoke(null, (I) () -> {
                        throw new ClassCastException();
                }));
                // error
                assertEquals("msg", method.invoke(null, (I) () -> {
                        throw new Error("msg");
                }));
                // throwable
                assertEquals(3, method.invoke(null, (I) () -> {
                        throw new Throwable();
                }));
                // none
                assertEquals(4, method.invoke(null, (I) () -> {
                }));
        }

        @Test
        public void testTryCatchTemp() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestTryCatch\n" +
                                "    static\n" +
                                "        method(func)\n" +
                                "            try\n" +
                                "                func.apply()\n" +
                                "            catch e\n" +
                                "                if 1===1\n" +
                                "                    <1\n" +
                                "            finally\n" +
                                "            <4",
                        "TestTryCatch");

                Method method = cls.getMethod("method", Object.class);
                // null pointer exception
                assertEquals(1, method.invoke(null, (I) () -> {
                        throw new NullPointerException();
                }));
        }

        interface I {
                @SuppressWarnings("unused")
                void apply() throws Throwable;
        }

        @Test
        public void testContinualAssign() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestContinualAssign\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            b=a=2\n" +
                                "            <b",
                        "TestContinualAssign");

                Method method = cls.getMethod("method");
                assertEquals(2, method.invoke(null));
        }

        @Test
        public void testUndefined() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestUndefined\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <undefined",
                        "TestUndefined");

                Method method = cls.getMethod("method");
                assertEquals(Undefined.get(), method.invoke(null));
        }

        @Test
        public void testStringAdd() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestStringAdd\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            <'abc'+'d'+1",
                        "TestStringAdd");

                Method method = cls.getMethod("method");
                assertEquals("abcd1", method.invoke(null));
        }

        @Test
        public void testLogicAnd() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicAnd\n" +
                                "    static\n" +
                                "        method(a,b)\n" +
                                "            <a&&b",
                        "TestLogicAnd");

                Method method = cls.getMethod("method", Object.class, Object.class);
                assertEquals(true, method.invoke(null, true, true));
                assertEquals(false, method.invoke(null, false, true));
                assertEquals(false, method.invoke(null, true, false));
                assertEquals(false, method.invoke(null, false, false));
        }

        @Test
        public void testLogicAnd2() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicAnd\n" +
                                "    static\n" +
                                "        method(a,ls)\n" +
                                "            a&&ls.add(1)",
                        "TestLogicAnd");

                Method method = cls.getMethod("method", Object.class, Object.class);

                List<Integer> ls = new ArrayList<>();
                method.invoke(null, true, ls);
                assertEquals(Collections.singletonList(1), ls);

                ls.clear();
                method.invoke(null, false, ls);
                assertTrue(ls.isEmpty());
        }

        @Test
        public void testLogicAnd3() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicAnd\n" +
                                "    static\n" +
                                "        method(a,b,c)\n" +
                                "            <a&&b&&c",
                        "TestLogicAnd");

                Method method = cls.getMethod("method", Object.class, Object.class, Object.class);

                assertEquals(true, method.invoke(null, true, true, true));
                assertEquals(false, method.invoke(null, false, true, true));
                assertEquals(false, method.invoke(null, true, false, true));
                assertEquals(false, method.invoke(null, true, true, false));
                assertEquals(false, method.invoke(null, false, false, true));
                assertEquals(false, method.invoke(null, false, true, false));
                assertEquals(false, method.invoke(null, true, false, false));
                assertEquals(false, method.invoke(null, false, false, false));
        }

        @Test
        public void testLogicOr() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicOr\n" +
                                "    static\n" +
                                "        method(a,b)\n" +
                                "            <a||b",
                        "TestLogicOr");

                Method method = cls.getMethod("method", Object.class, Object.class);
                assertEquals(true, method.invoke(null, true, true));
                assertEquals(true, method.invoke(null, false, true));
                assertEquals(true, method.invoke(null, true, false));
                assertEquals(false, method.invoke(null, false, false));
        }

        @Test
        public void testLogicOr2() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicOr\n" +
                                "    static\n" +
                                "        method(a,ls)\n" +
                                "            a||ls.add(1)",
                        "TestLogicOr");

                Method method = cls.getMethod("method", Object.class, Object.class);

                List<Integer> ls = new ArrayList<>();
                method.invoke(null, false, ls);
                assertEquals(Collections.singletonList(1), ls);

                ls.clear();
                method.invoke(null, true, ls);
                assertTrue(ls.isEmpty());
        }

        @Test
        public void testLogicOr3() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicOr\n" +
                                "    static\n" +
                                "        method(a,b,c)\n" +
                                "            <a||b||c",
                        "TestLogicOr");

                Method method = cls.getMethod("method", Object.class, Object.class, Object.class);

                assertEquals(true, method.invoke(null, true, true, true));
                assertEquals(true, method.invoke(null, false, true, true));
                assertEquals(true, method.invoke(null, true, false, true));
                assertEquals(true, method.invoke(null, true, true, false));
                assertEquals(true, method.invoke(null, false, false, true));
                assertEquals(true, method.invoke(null, false, true, false));
                assertEquals(true, method.invoke(null, true, false, false));
                assertEquals(false, method.invoke(null, false, false, false));
        }

        @Test
        public void testLogicAndOr() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestLogicAndOr\n" +
                                "    static\n" +
                                "        method(a,b,c)\n" +
                                "            <a or b and c",
                        "TestLogicAndOr");

                // && has higher priority than ||
                // a or b and c
                // ==
                // a or (b and c)
                Method method = cls.getMethod("method", Object.class, Object.class, Object.class);

                //                                       a  or ( b  and  c )
                assertEquals(true, method.invoke(null, true, false, false));
                assertEquals(false, method.invoke(null, false, false, true));
                assertEquals(true, method.invoke(null, false, true, true));
        }

        @Test
        public void testUnaryInc() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestUnaryInc\n" +
                                "    static\n" +
                                "        method(a:int)\n" +
                                "            <++a",
                        "TestUnaryInc");
                Method method = cls.getMethod("method", int.class);
                assertEquals(2, method.invoke(null, 1));
        }

        @Test
        public void testSelfInc() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestSelfInc\n" +
                                "    static\n" +
                                "        method(a:int)\n" +
                                "            <a++",
                        "TestSelfInc");
                Method method = cls.getMethod("method", int.class);
                assertEquals(1, method.invoke(null, 1));
        }

        @Test
        public void testInvokeVoidMethodReturnUndefined() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestInvokeVoidMethodReturnUndefined\n" +
                                "    static\n" +
                                "        pri m():Unit\n" +
                                "        method()\n" +
                                "            <m()",
                        "TestInvokeVoidMethodReturnUndefined");
                Method method = cls.getMethod("method");
                assertEquals(Undefined.get(), method.invoke(null));
        }

        @Test
        public void testVoidMethodLogicAnd() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestVoidMethodLogicAnd\n" +
                                "    static\n" +
                                "        i:int\n" +
                                "        pri m():Unit\n" +
                                "            i=100\n" +
                                "        method(a)\n" +
                                "            <a&&m()\n" +
                                "        getI()=i",
                        "TestVoidMethodLogicAnd");
                Method method = cls.getMethod("method", Object.class);
                Method getI = cls.getMethod("getI");

                assertEquals(false, method.invoke(null, false));
                assertEquals(0, getI.invoke(null));

                assertEquals(false, method.invoke(null, true));
                assertEquals(100, getI.invoke(null));
        }

        @Test
        public void testTAStore() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestTAStore\n" +
                                "    static\n" +
                                "        method(a:[]int)\n" +
                                "            <a[1]=100",
                        "TestTAStore");
                Method method = cls.getMethod("method", int[].class);

                int[] arr = new int[]{1, 2};
                method.invoke(null, (Object) arr);
                assertEquals(100, arr[1]);
        }

        @Test
        public void testIndexAccessObject() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestIndexAccessObject\n" +
                                "    static\n" +
                                "        method(a)\n" +
                                "            <a[1]=100",
                        "TestIndexAccessObject");
                Method method = cls.getMethod("method", Object.class);

                List<Integer> arr = new ArrayList<>();
                arr.add(1);
                arr.add(2);
                method.invoke(null, arr);
                assertEquals(Integer.valueOf(100), arr.get(1));

                Map<Integer, Integer> map = new HashMap<>();
                map.put(1, 1);
                map.put(2, 2);
                method.invoke(null, map);
                assertEquals(Integer.valueOf(100), map.get(1));
        }

        @Test
        public void testNull() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNull\n" +
                                "    static\n" +
                                "        method()=null",
                        "TestNull");
                Method method = cls.getMethod("method");
                assertNull(method.invoke(null));
        }

        @Test
        public void testArrayLength() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestArrayLength\n" +
                                "    static\n" +
                                "        method(arr:[]int)=arr.length",
                        "TestArrayLength");
                Method method = cls.getMethod("method", int[].class);
                int[] arr = new int[]{1, 1, 1, 1};
                assertEquals(4, method.invoke(null, (Object) arr));
        }

        @Test
        public void testNot() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNot\n" +
                                "    static\n" +
                                "        method(i:int)=~i",
                        "TestNot");
                Method method = cls.getMethod("method", int.class);

                assertEquals(-3, method.invoke(null, 2));
        }

        @Test
        public void testNot2() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNot\n" +
                                "    static\n" +
                                "        method(arr)=~arr",
                        "TestNot");
                Method method = cls.getMethod("method", Object.class);

                BigInteger bigInteger = new BigInteger("2");
                BigInteger result = (BigInteger) method.invoke(null, bigInteger); // invoke bigInteger.not();
                assertEquals(new BigInteger("-3"), result);
        }

        @Test
        public void testNeg() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNeg\n" +
                                "    static\n" +
                                "        method(i:int)=-i",
                        "TestNeg");
                Method method = cls.getMethod("method", int.class);
                assertEquals(-2, method.invoke(null, 2));
        }

        @Test
        public void testNeg2() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNeg\n" +
                                "    static\n" +
                                "        method(i)=-i",
                        "TestNeg");
                Method method = cls.getMethod("method", Object.class);
                assertEquals(new BigInteger("-2"), method.invoke(null, new BigInteger("2")));
        }

        @Test
        public void testNewArray() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNewArray\n" +
                                "    static\n" +
                                "        method():[]int=[10,20]",
                        "TestNewArray");
                Method method = cls.getMethod("method");
                int[] arr = (int[]) method.invoke(null);
                assertEquals(2, arr.length);
                assertEquals(10, arr[0]);
                assertEquals(20, arr[1]);
        }

        @Test
        public void testANewArray() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestANewArray\n" +
                                "    static\n" +
                                "        method():[]Object=[10,20]",
                        "TestANewArray");
                Method method = cls.getMethod("method");
                Object[] arr = (Object[]) method.invoke(null);
                assertEquals(2, arr.length);
                assertEquals(10, arr[0]);
                assertEquals(20, arr[1]);
        }

        @Test
        public void testNewList() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestANewArray\n" +
                                "    static\n" +
                                "        method()=[10,20]",
                        "TestANewArray");
                Method method = cls.getMethod("method");
                @SuppressWarnings("unchecked")
                List<Integer> list = (List<Integer>) method.invoke(null);

                assertTrue(list.getClass().equals(LinkedList.class));

                assertEquals(10, list.get(0).intValue());
                assertEquals(20, list.get(1).intValue());
        }

        @Test
        public void test2DArray() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class Test2DArray\n" +
                                "    static\n" +
                                "        method():[][]int=[[1,2],[],[3]]",
                        "Test2DArray");
                Method method = cls.getMethod("method");
                int[][] arr = (int[][]) method.invoke(null);
                assertArrayEquals(new int[]{1, 2}, arr[0]);
                assertArrayEquals(new int[]{}, arr[1]);
                assertArrayEquals(new int[]{3}, arr[2]);
        }

        @Test
        public void testNewMap() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestNewMap\n" +
                                "    static\n" +
                                "        method()={\n" +
                                "            \"a\":1\n" +
                                "            \"b\":2" +
                                "        }",
                        "TestNewMap");
                Method method = cls.getMethod("method");
                LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
                expected.put("a", 1);
                expected.put("b", 2);
                Object o = expected;
                assertEquals(o, method.invoke(null));
        }

        @Test
        public void testAnnotation() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#>lt::compiler::_\n" +
                                "" +
                                "class TestAnnotation\n" +
                                "    static\n" +
                                "        @MyAnno(str='abc')\n" +
                                "        method():Unit",
                        "TestAnnotation");
                Method method = cls.getMethod("method");
                MyAnno myAnno = method.getDeclaredAnnotation(MyAnno.class);
                assertNotNull(myAnno);
                assertEquals("abc", myAnno.str());
                assertEquals(100, myAnno.i());
        }

        @Test
        public void testSynchronized() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestAnnotation\n" +
                                "    static\n" +
                                "        method(a,b):Unit\n" +
                                "            sync(a,b)\n" +
                                "                t=System.currentTimeMillis()\n" +
                                "                while(System.currentTimeMillis()+100<t)\n" +
                                "                    ...\n" +
                                "                a.i+=1\n" +
                                "                b.i+=2",
                        "TestAnnotation");
                class Container {
                        int i = 0;
                }
                Method method = cls.getMethod("method", Object.class, Object.class);
                Container a = new Container();
                Container b = new Container();
                new Thread(() -> {
                        try {
                                method.invoke(null, a, b);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }).start();

                Thread.sleep(10); // wait 10 ms to let monitorEnter execute

                class Result {
                        boolean pass1 = false;
                        boolean pass2 = false;
                }
                Result result = new Result();

                Thread t1 = new Thread(() -> {
                        synchronized (a) {
                                if (1 == a.i) result.pass1 = true;
                        }
                });
                t1.start();
                Thread t2 = new Thread(() -> {
                        synchronized (b) {
                                if (2 == b.i) result.pass2 = true;
                        }
                });
                t2.start();
                t1.join();
                t2.join();
                assertTrue(result.pass1);
                assertTrue(result.pass2);
        }

        @Test
        public void testSynchronizedReturn() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestSynchronizedReturn\n" +
                                "    static\n" +
                                "        method(a,b)\n" +
                                "            sync(a,b)\n" +
                                "                t=System.currentTimeMillis()\n" +
                                "                while(System.currentTimeMillis()+100<t)\n" +
                                "                    ...\n" +
                                "                a.i+=1\n" +
                                "                b.i+=2\n" +
                                "                <10",
                        "TestSynchronizedReturn");
                class Container {
                        int i = 0;
                }
                Method method = cls.getMethod("method", Object.class, Object.class);
                Container a = new Container();
                Container b = new Container();
                class Result {
                        boolean result = false;
                        boolean pass1 = false;
                        boolean pass2 = false;
                }
                Result result = new Result();
                new Thread(() -> {
                        try {
                                Object res = method.invoke(null, a, b);
                                if (res.equals(10)) result.result = true;
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }).start();

                Thread.sleep(10); // wait 10 ms to let monitorEnter execute

                Thread t1 = new Thread(() -> {
                        synchronized (a) {
                                if (1 == a.i) result.pass1 = true;
                        }
                });
                t1.start();
                Thread t2 = new Thread(() -> {
                        synchronized (b) {
                                if (2 == b.i) result.pass2 = true;
                        }
                });
                t2.start();
                t1.join();
                t2.join();
                assertTrue(result.result);
                assertTrue(result.pass1);
                assertTrue(result.pass2);
        }

        @Test
        public void testDefineSQL_DSL() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "define 'CREATE TABLE' as 'class'\n" +
                                "define 'VARCHAR' as ':String'\n" +
                                "define 'NUMBER' as ':int'\n" +
                                "CREATE TABLE User(\n" +
                                "    id NUMBER\n" +
                                "    name VARCHAR\n" +
                                ")",
                        "User");
                assertEquals("User", cls.getName());
                Constructor<?> con = cls.getDeclaredConstructors()[0];
                assertEquals(2, con.getParameterCount());
                assertEquals(int.class, con.getParameterTypes()[0]);
                assertEquals(String.class, con.getParameterTypes()[1]);
        }

        @Test
        public void testTryCatchCanLoad() throws Exception {
                retrieveClass(
                        "" +
                                "class TestTryCatchCanLoad\n" +
                                "    method():int\n" +
                                "        i:int=1\n" +
                                "        try\n" +
                                "            i=2\n" +
                                "            if i==3\n" +
                                "                i=10\n" +
                                "                <1\n" +
                                "            else\n" +
                                "                i=11\n" +
                                "                <2\n" +
                                "        catch e\n" +
                                "            if e is type RuntimeException\n" +
                                "                i=4\n" +
                                "                if i==5\n" +
                                "                    i=12\n" +
                                "                    <3\n" +
                                "                else\n" +
                                "                    i=13\n" +
                                "                    <4\n" +
                                "        finally\n" +
                                "            i=6",
                        "TestTryCatchCanLoad"
                );
        }

        @Test
        public void testInnerMethod1() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class TestInnerMethod\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            inner():int=1\n" +
                                "            <inner()",
                        "TestInnerMethod");
                assertEquals(2, cls.getDeclaredMethods().length);
                Method m = cls.getDeclaredMethod("method");
                assertEquals(1, m.invoke(null));
        }

        @Test
        public void testInnerMethod2() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class TestInnerMethod\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            i:int=1\n" +
                                "            j:int=2\n" +
                                "            inner():int=i+j\n" +
                                "            <inner()",
                        "TestInnerMethod");
                assertEquals(2, cls.getDeclaredMethods().length);
                Method m = cls.getDeclaredMethod("method");
                assertEquals(3, m.invoke(null));
        }

        @Test
        public void testInnerMethod3() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class TestInnerMethod\n" +
                                "    method()\n" +
                                "        i:int=1\n" +
                                "        j:int=2\n" +
                                "        inner():int=i+j\n" +
                                "        <inner()",
                        "TestInnerMethod");
                assertEquals(2, cls.getDeclaredMethods().length);
                Method m = cls.getDeclaredMethod("method");
                assertEquals(3, m.invoke(cls.newInstance()));
        }

        @Test
        public void testInnerMethod4() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class TestInnerMethod\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            i:int=1\n" +
                                "            j:int=2\n" +
                                "            inner(k:int):int=i+j+k\n" +
                                "            <inner(3)",
                        "TestInnerMethod");
                assertEquals(2, cls.getDeclaredMethods().length);
                Method m = cls.getDeclaredMethod("method");
                assertEquals(6, m.invoke(null));
        }

        @Test
        public void testLambdaJDK1() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "#>java::util::function::_\n" +
                                "class TestLambdaJDK\n" +
                                "    static\n" +
                                "        method():Function\n" +
                                "            <(o)->o+1",
                        "TestLambdaJDK");
                Method m = cls.getDeclaredMethod("method");
                @SuppressWarnings("unchecked")
                Function<Object, Object> f = (Function<Object, Object>) m.invoke(null);
                assertEquals(2, f.apply(1));
        }

        @Test
        public void testLambdaJDK2() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "#>java::util::function::_\n" +
                                "class TestLambdaJDK\n" +
                                "    static\n" +
                                "        method():Function\n" +
                                "            i=1\n" +
                                "            <(o)->o+1+i",
                        "TestLambdaJDK");
                Method m = cls.getDeclaredMethod("method");
                @SuppressWarnings("unchecked")
                Function<Object, Object> f = (Function<Object, Object>) m.invoke(null);
                assertEquals(3, f.apply(1));
        }

        @Test
        public void testLambdaJDK3() throws Throwable {
                Class<?> cls = retrieveClass("" +
                                "#>java::util::function::_\n" +
                                "class TestLambdaJDK\n" +
                                "    static\n" +
                                "        method()\n" +
                                "            i=1\n" +
                                "            <(o)->o+1+i",
                        "TestLambdaJDK");
                Method m = cls.getDeclaredMethod("method");
                @SuppressWarnings("unchecked")
                Function1 f = (Function1) m.invoke(null);
                assertEquals(3, f.apply(1));
        }

        @Test
        public void testLambdaLT1() throws Exception {
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader("" +
                        "#>lt::compiler::_\n" +
                        "class TestLambdaLT\n" +
                        "    static\n" +
                        "        method():TestLambdaFunc\n" +
                        "            i=1\n" +
                        "            <(o)->o+1+i"), new Scanner.Properties(), new ErrorManager(true));
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();

                byte[] b1 = list.get("TestLambdaLT");
                byte[] b2 = list.get("TestLambdaLT$LessTyping$Lambda$0");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                if (name.equals("TestLambdaLT")) {
                                        return defineClass(name, b1, 0, b1.length);
                                } else {
                                        return defineClass(name, b2, 0, b2.length);
                                }
                        }
                };

                Class<?> TestLambdaLT = classLoader.loadClass("TestLambdaLT");
                Class<?> lambda = classLoader.loadClass("TestLambdaLT$LessTyping$Lambda$0");

                TestLambdaFunc func = (TestLambdaFunc) TestLambdaLT.getDeclaredMethod("method").invoke(null);
                assertEquals(3, func.apply(1));

                assertEquals(2, lambda.getDeclaredFields().length);
        }

        @Test
        public void testLambdaLT2() throws Exception {
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader("" +
                        "#>lt::compiler::_\n" +
                        "class TestLambdaLT\n" +
                        "    method():TestLambdaFunc\n" +
                        "        i=1\n" +
                        "        <(o)->o+1+i"), new Scanner.Properties(), new ErrorManager(true));
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();

                byte[] b1 = list.get("TestLambdaLT");
                byte[] b2 = list.get("TestLambdaLT$LessTyping$Lambda$0");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                if (name.equals("TestLambdaLT")) {
                                        return defineClass(name, b1, 0, b1.length);
                                } else {
                                        return defineClass(name, b2, 0, b2.length);
                                }
                        }
                };

                Class<?> TestLambdaLT = classLoader.loadClass("TestLambdaLT");
                Class<?> lambda = classLoader.loadClass("TestLambdaLT$LessTyping$Lambda$0");

                TestLambdaFunc func = (TestLambdaFunc) TestLambdaLT.getDeclaredMethod("method").invoke(TestLambdaLT.newInstance());
                assertEquals(3, func.apply(1));

                assertEquals(3, lambda.getDeclaredFields().length);
        }

        @Test
        public void testLambdaLT3() throws Throwable {
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader("" +
                        "#>java::util::function::_\n" +
                        "class TestLambdaLT\n" +
                        "    method():Function\n" +
                        "        i=1\n" +
                        "        <(o)->o+1+i"), new Scanner.Properties(), new ErrorManager(true));
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();

                byte[] b1 = list.get("TestLambdaLT");
                byte[] b2 = list.get("TestLambdaLT$LessTyping$Lambda$0");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                if (name.equals("TestLambdaLT")) {
                                        return defineClass(name, b1, 0, b1.length);
                                } else {
                                        return defineClass(name, b2, 0, b2.length);
                                }
                        }
                };

                Class<?> TestLambdaLT = classLoader.loadClass("TestLambdaLT");
                Class<?> lambda = classLoader.loadClass("TestLambdaLT$LessTyping$Lambda$0");

                Function func = (Function) TestLambdaLT.getDeclaredMethod("method").invoke(TestLambdaLT.newInstance());
                assertEquals(3, func.apply(1));

                assertEquals(3, lambda.getDeclaredFields().length);
        }

        @Test
        public void testListRemove() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "#> java::util::_\n" +
                                "class TestInvokeInterface\n" +
                                "    static\n" +
                                "        methodRemoveInt1(ls)\n" +
                                "            ls.remove(1)\n" +
                                "        methodRemoveInteger1(ls)\n" +
                                "            ls.remove(Integer(1))",
                        "TestInvokeInterface");

                List<Integer> list = new ArrayList<>();
                list.add(1);
                list.add(2);
                list.add(3);
                Method methodRemoveInt1 = cls.getMethod("methodRemoveInt1", Object.class);
                methodRemoveInt1.invoke(null, list);
                assertEquals(Arrays.asList(1, 3), list);

                list.clear();
                list.add(1);
                list.add(2);
                list.add(3);
                Method methodRemoveInteger1 = cls.getMethod("methodRemoveInteger1", Object.class);
                methodRemoveInteger1.invoke(null, list);
                assertEquals(Arrays.asList(2, 3), list);
        }

        @Test
        public void testArrayAccess() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestArrayAccess\n" +
                                "    static\n" +
                                "        arr:[]String = ['test1','test2']\n" +
                                "        method(i,o)\n" +
                                "            arr[i]=o",
                        "TestArrayAccess");
                Field f = cls.getDeclaredField("arr");
                f.setAccessible(true);
                Method method = cls.getMethod("method", Object.class, Object.class);
                method.invoke(null, 1, "changed");
                String[] arr = (String[]) f.get(null);
                assertEquals("test1", arr[0]);
                assertEquals("changed", arr[1]);
        }

        @Test
        public void testPrimitiveType() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestPrimitiveType\n" +
                                "    static\n" +
                                "        typeInt=type int\n" +
                                "        typeLong=type long\n" +
                                "        typeShort=type short\n" +
                                "        typeByte=type byte\n" +
                                "        typeChar=type char\n" +
                                "        typeBool=type bool\n" +
                                "        typeFloat=type float\n" +
                                "        typeDouble=type double\n" +
                                "        typeVoid=type void\n" +
                                "        typeUnit=type Unit",
                        "TestPrimitiveType");
                Field typeInt = cls.getDeclaredField("typeInt");
                typeInt.setAccessible(true);
                Field typeLong = cls.getDeclaredField("typeLong");
                typeLong.setAccessible(true);
                Field typeShort = cls.getDeclaredField("typeShort");
                typeShort.setAccessible(true);
                Field typeByte = cls.getDeclaredField("typeByte");
                typeByte.setAccessible(true);
                Field typeChar = cls.getDeclaredField("typeChar");
                typeChar.setAccessible(true);
                Field typeBool = cls.getDeclaredField("typeBool");
                typeBool.setAccessible(true);
                Field typeFloat = cls.getDeclaredField("typeFloat");
                typeFloat.setAccessible(true);
                Field typeDouble = cls.getDeclaredField("typeDouble");
                typeDouble.setAccessible(true);
                Field typeVoid = cls.getDeclaredField("typeVoid");
                typeVoid.setAccessible(true);
                Field typeUnit = cls.getDeclaredField("typeUnit");
                typeUnit.setAccessible(true);

                assertEquals(int.class, typeInt.get(null));
                assertEquals(long.class, typeLong.get(null));
                assertEquals(short.class, typeShort.get(null));
                assertEquals(byte.class, typeByte.get(null));
                assertEquals(char.class, typeChar.get(null));
                assertEquals(boolean.class, typeBool.get(null));
                assertEquals(float.class, typeFloat.get(null));
                assertEquals(double.class, typeDouble.get(null));
                assertEquals(void.class, typeVoid.get(null));
                assertEquals(void.class, typeUnit.get(null));
        }

        @Test
        public void testThrowAnyObject() throws Exception {
                Class<?> cls = retrieveClass(
                        "" +
                                "class TestThrowAnyObject\n" +
                                "    static\n" +
                                "        testThrow()\n" +
                                "            throw 'abc'\n" +
                                "        testCatch(func)\n" +
                                "            try\n" +
                                "                func.apply()\n" +
                                "            catch e\n" +
                                "                <e",
                        "TestThrowAnyObject");
                Method testThrow = cls.getMethod("testThrow");
                try {
                        testThrow.invoke(null);
                        fail();
                } catch (InvocationTargetException in) {
                        Wrapper w = (Wrapper) in.getCause();
                        assertEquals("abc", w.object);
                }

                Method testCatch = cls.getMethod("testCatch", Object.class);
                assertEquals("abc", testCatch.invoke(null, (I) () -> {
                        throw new Wrapper("abc");
                }));
                assertEquals(1, testCatch.invoke(null, (I) () -> {
                        throw new Wrapper(1);
                }));
        }
}
