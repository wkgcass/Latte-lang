package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function1;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * test demo
 */
public class TestDemo {
        private Map<String, byte[]> generate(BufferedReader br, String fileName) throws IOException, SyntaxException, ClassNotFoundException {
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner(fileName, br, new lt.compiler.Scanner.Properties(), new ErrorManager(true));
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan());
                Map<String, List<Statement>> map = new HashMap<>();
                map.put(fileName, syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                return codeGenerator.generate();
        }

        @Test
        public void testLiteral() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/literals.lts");
                StringBuilder sb = new StringBuilder();
                sb.append("class literals\n");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                        sb.append("    ").append(line).append("\n");
                }

                Map<String, byte[]> map = generate(new BufferedReader(new StringReader(sb.toString())), "literals.lts");
                byte[] bs = map.get("literals");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, bs, 0, bs.length);
                        }
                };

                Class<?> cls = classLoader.loadClass("literals");
                Object o = cls.newInstance();
                int count = 0;

                // i_am_string
                Field i_am_string = cls.getDeclaredField("i_am_string");
                i_am_string.setAccessible(true);
                assertEquals("a string", i_am_string.get(o));
                ++count;

                // or_in_this_form
                Field or_in_this_form = cls.getDeclaredField("or_in_this_form");
                or_in_this_form.setAccessible(true);
                assertEquals("also a string", or_in_this_form.get(o));
                ++count;

                // i_am_a_number
                Field i_am_a_number = cls.getDeclaredField("i_am_a_number");
                i_am_a_number.setAccessible(true);
                assertEquals(1, i_am_a_number.get(o));
                ++count;

                // or_float_number
                Field or_float_number = cls.getDeclaredField("or_float_number");
                or_float_number.setAccessible(true);
                assertEquals(1.2, or_float_number.get(o));
                ++count;

                // json
                Field json = cls.getDeclaredField("json");
                json.setAccessible(true);
                assertEquals(new LinkedHashMap<Object, Object>() {{
                        put('a', 'b');
                }}, json.get(o));
                ++count;

                // list
                Field list = cls.getDeclaredField("list");
                list.setAccessible(true);
                assertEquals(Arrays.asList(1, 2, 3), list.get(o));
                ++count;

                // i_am_bool
                Field i_am_bool = cls.getDeclaredField("i_am_bool");
                i_am_bool.setAccessible(true);
                assertEquals(true, i_am_bool.getBoolean(o));
                ++count;

                // i_am_bool_too
                Field i_am_bool_too = cls.getDeclaredField("i_am_bool_too");
                i_am_bool_too.setAccessible(true);
                assertEquals(true, i_am_bool_too.getBoolean(o));
                ++count;

                // i_am_bool_false
                Field i_am_bool_false = cls.getDeclaredField("i_am_bool_false");
                i_am_bool_false.setAccessible(true);
                assertEquals(false, i_am_bool_false.getBoolean(o));
                ++count;

                // i_am_also_bool_false
                Field i_am_also_bool_false = cls.getDeclaredField("i_am_also_bool_false");
                i_am_also_bool_false.setAccessible(true);
                assertEquals(false, i_am_also_bool_false.getBoolean(o));
                ++count;

                // i_am_char
                Field i_am_char = cls.getDeclaredField("i_am_char");
                i_am_char.setAccessible(true);
                assertEquals('c', i_am_char.getChar(o));
                ++count;

                // i_am_String
                Field i_am_String = cls.getDeclaredField("i_am_String");
                i_am_String.setAccessible(true);
                assertEquals("i am a java.lang.String", i_am_String.get(o));
                ++count;

                // i_am_int
                Field i_am_int = cls.getDeclaredField("i_am_int");
                i_am_int.setAccessible(true);
                assertEquals(1, i_am_int.getInt(o));
                ++count;

                // i_am_double
                Field i_am_double = cls.getDeclaredField("i_am_double");
                i_am_double.setAccessible(true);
                assertEquals(1.2, i_am_double.getDouble(o), 0);
                ++count;

                // i_am_float
                Field i_am_float = cls.getDeclaredField("i_am_float");
                i_am_float.setAccessible(true);
                assertEquals(1.0f, i_am_float.getFloat(o), 0);
                ++count;

                // i_am_long
                Field i_am_long = cls.getDeclaredField("i_am_long");
                i_am_long.setAccessible(true);
                assertEquals(1L, i_am_long.getLong(o));
                ++count;

                // i_am_short
                Field i_am_short = cls.getDeclaredField("i_am_short");
                i_am_short.setAccessible(true);
                assertEquals((short) 1, i_am_short.getShort(o));
                ++count;

                // i_am_byte
                Field i_am_byte = cls.getDeclaredField("i_am_byte");
                i_am_byte.setAccessible(true);
                assertEquals((byte) 1, i_am_byte.getByte(o));
                ++count;

                // this_var_is_int
                Field this_var_is_int = cls.getDeclaredField("this_var_is_int");
                this_var_is_int.setAccessible(true);
                assertEquals(1, this_var_is_int.getInt(o));
                ++count;

                // this_var_is_short
                Field this_var_is_short = cls.getDeclaredField("this_var_is_short");
                this_var_is_short.setAccessible(true);
                assertEquals((short) 1, this_var_is_short.getShort(o));
                ++count;

                // this_var_is_Integer
                Field this_var_is_Integer = cls.getDeclaredField("this_var_is_Integer");
                this_var_is_Integer.setAccessible(true);
                assertEquals(1, this_var_is_Integer.get(o));
                ++count;

                // default_value_of_number
                Field default_value_of_number = cls.getDeclaredField("default_value_of_number");
                default_value_of_number.setAccessible(true);
                assertEquals(1, default_value_of_number.get(o));
                ++count;

                // default_value_of_float_number
                Field default_value_of_float_number = cls.getDeclaredField("default_value_of_float_number");
                default_value_of_float_number.setAccessible(true);
                assertEquals(1.2D, default_value_of_float_number.get(o));
                ++count;

                // default_value_of_char_string
                Field default_value_of_char_string = cls.getDeclaredField("default_value_of_char_string");
                default_value_of_char_string.setAccessible(true);
                assertEquals('c', default_value_of_char_string.get(o));
                ++count;

                // default_value_of_string_1
                Field default_value_of_string_1 = cls.getDeclaredField("default_value_of_string_1");
                default_value_of_string_1.setAccessible(true);
                assertEquals("s", default_value_of_string_1.get(o));
                ++count;

                // default_value_of_string_2
                Field default_value_of_string_2 = cls.getDeclaredField("default_value_of_string_2");
                default_value_of_string_2.setAccessible(true);
                assertEquals("length is greater than 1", default_value_of_string_2.get(o));
                ++count;

                // default_value_of_string_3
                Field default_value_of_string_3 = cls.getDeclaredField("default_value_of_string_3");
                default_value_of_string_3.setAccessible(true);
                assertEquals("", default_value_of_string_3.get(o));
                ++count;

                assertEquals(count, cls.getDeclaredFields().length);
        }

        @Test
        public void testLtFileStructure() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/ltFileStructure.lt");

                Map<String, byte[]> map = generate(new BufferedReader(new InputStreamReader(is)), "ltFileStructure.lt");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, map.get(name), 0, map.get(name).length);
                        }
                };

                // lt.demo.I_Am_A_Class
                Class<?> I_Am_A_Class = classLoader.loadClass("lt.demo.I_Am_A_Class");
                assertEquals(2, I_Am_A_Class.getDeclaredFields().length); // one field
                assertEquals(1, I_Am_A_Class.getDeclaredConstructors().length); // one constructor
                assertEquals(1, I_Am_A_Class.getDeclaredMethods().length); // one method
                assertEquals(Object.class, I_Am_A_Class.getSuperclass()); // extends Object
                assertEquals(0, I_Am_A_Class.getInterfaces().length); // 0 interfaces
                Object I_Am_A_Class_o = I_Am_A_Class.newInstance();

                Field i_am_field = I_Am_A_Class.getDeclaredField("i_am_field");
                assertEquals(int.class, i_am_field.getType()); // int i_am_field
                i_am_field.setAccessible(true);
                assertEquals(0, i_am_field.getInt(I_Am_A_Class_o)); // it's 0

                Field list = I_Am_A_Class.getDeclaredField("list");
                assertEquals(List.class, list.getType());
                list.setAccessible(true);
                assertNull(list.get(I_Am_A_Class_o));

                Method i_am_a_method = I_Am_A_Class.getDeclaredMethod("i_am_a_method");
                assertEquals(List.class, i_am_a_method.getReturnType()); // java.util.List i_am_a_method()
                assertEquals(Collections.EMPTY_LIST, i_am_a_method.invoke(I_Am_A_Class_o));
                assertEquals(Collections.EMPTY_LIST, list.get(I_Am_A_Class_o));
                // the field is emptyList, the list in method is not a local variable

                // lt.demo.I_Am_An_Interface
                Class<?> I_Am_An_Interface = classLoader.loadClass("lt.demo.I_Am_An_Interface");
                assertTrue(I_Am_An_Interface.isInterface());

                // lt.demo.User
                Class<?> User = classLoader.loadClass("lt.demo.User");
                assertEquals(1, User.getDeclaredConstructors().length);
                Constructor<?> con = User.getDeclaredConstructor(int.class, String.class); // User(id,name)

                assertEquals(2, User.getDeclaredFields().length);
                Field id = User.getDeclaredField("id");
                assertEquals(int.class, id.getType());
                Field name = User.getDeclaredField("name");
                assertEquals(String.class, name.getType());

                Object User_o = con.newInstance(1, "cass");
                id.setAccessible(true);
                assertEquals(1, id.getInt(User_o));

                name.setAccessible(true);
                assertEquals("cass", name.get(User_o));
        }

        @Test
        public void testStatements() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/statements.lts");
                StringBuilder sb = new StringBuilder();
                sb.append("class statements\n");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                        sb.append("    ").append(line).append("\n");
                }

                Map<String, byte[]> map = generate(new BufferedReader(new StringReader(sb.toString())), "statements.lts");
                byte[] bs = map.get("statements");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, bs, 0, bs.length);
                        }
                };

                Class<?> cls = classLoader.loadClass("statements");
                Object o = cls.newInstance();

                // arr
                Field arr = cls.getDeclaredField("arr");
                arr.setAccessible(true);
                assertEquals(Arrays.asList(1, 2, 3), arr.get(o));

                // res0
                Field res0 = cls.getDeclaredField("res0");
                res0.setAccessible(true);
                assertEquals(6, res0.get(o));

                // res1
                Field res1 = cls.getDeclaredField("res1");
                res1.setAccessible(true);
                assertEquals(6, res1.get(o));

                // index
                Field index = cls.getDeclaredField("index");
                index.setAccessible(true);
                assertEquals(3, index.get(o));

                // method_if_elseif_else
                Method method_if_elseif_else = cls.getDeclaredMethod("method_if_elseif_else", Object.class, Object.class);
                /*
                 * if a
                 *     <1
                 * elseif b
                 *     <2
                 * else
                 *     <3
                 */
                // a=true
                assertEquals(1, method_if_elseif_else.invoke(o, true, false));
                assertEquals(1, method_if_elseif_else.invoke(o, true, true));
                // a=false and b=true
                assertEquals(2, method_if_elseif_else.invoke(o, false, true));
                // a=false and b=false
                assertEquals(3, method_if_elseif_else.invoke(o, false, false));

                // method_try_catch_finally
                Method method_try_catch_finally = cls.getDeclaredMethod("method_try_catch_finally", Object.class);
                // no exception, then a=1 and finally ++a , a==2
                // RuntimeException, then a=2 and finally ++a , a==3
                // Error, Exception, then a=3 and finally ++a , a==4
                // Throwable, then a=4 and finally ++a , a==5
                assertEquals(2, method_try_catch_finally.invoke(o, (I) () -> {
                }));
                assertEquals(3, method_try_catch_finally.invoke(o, (I) () -> {
                        throw new RuntimeException();
                }));
                assertEquals(4, method_try_catch_finally.invoke(o, (I) () -> {
                        throw new Error();
                }));
                assertEquals(4, method_try_catch_finally.invoke(o, (I) () -> {
                        throw new Exception();
                }));
                assertEquals(5, method_try_catch_finally.invoke(o, (I) () -> {
                        throw new Throwable();
                }));

                // res2
                Field res2 = cls.getDeclaredField("res2");
                res2.setAccessible(true);
                assertEquals(1, res2.get(o));

                // variable1
                Field variable1 = cls.getDeclaredField("variable1");
                variable1.setAccessible(true);
                assertEquals(1, variable1.get(o));

                // variable2
                Field variable2 = cls.getDeclaredField("variable2");
                variable2.setAccessible(true);
                assertEquals(1, variable2.getInt(o));

                // variable3
                Field variable3 = cls.getDeclaredField("variable3");
                variable3.setAccessible(true);
                assertEquals(0, variable3.getInt(o));

                // res3
                Field res3 = cls.getDeclaredField("res3");
                res3.setAccessible(true);
                assertEquals(1, res3.get(o));

                // res4
                Field res4 = cls.getDeclaredField("res4");
                res4.setAccessible(true);
                assertEquals(Object.class, res4.get(o).getClass());

                // list1
                Field list1 = cls.getDeclaredField("list1");
                list1.setAccessible(true);
                assertEquals(Collections.singletonList(1), list1.get(o));

                // list2
                Field list2 = cls.getDeclaredField("list2");
                list2.setAccessible(true);
                assertEquals(Collections.EMPTY_LIST, list2.get(o));

                // res5
                Field res5 = cls.getDeclaredField("res5");
                res5.setAccessible(true);
                assertTrue(res5.get(o) == list1.get(o));

                // list3
                Field list3 = cls.getDeclaredField("list3");
                list3.setAccessible(true);
                assertEquals(Arrays.asList(2, 2, 3), list3.get(o));

                // res6
                Field res6 = cls.getDeclaredField("res6");
                res6.setAccessible(true);
                assertEquals(1, res6.get(o));

                // map
                Field Fmap = cls.getDeclaredField("map");
                Fmap.setAccessible(true);
                assertEquals(new LinkedHashMap<Object, Object>() {{
                        put("a", "b");
                }}, Fmap.get(o));
        }

        interface I {
                @SuppressWarnings("unused")
                void apply() throws Throwable;
        }

        @Test
        public void testAdvanced() throws Throwable {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/advanced.lt");

                Map<String, byte[]> map = generate(new BufferedReader(new InputStreamReader(is)), "advanced.lt");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, map.get(name), 0, map.get(name).length);
                        }
                };

                Class<?> TestInnerMethod = classLoader.loadClass("TestInnerMethod");
                Method outer = TestInnerMethod.getDeclaredMethod("outer");
                assertEquals(1, outer.invoke(TestInnerMethod.newInstance()));

                Class<?> TestProcedure = classLoader.loadClass("TestProcedure");
                Method method = TestProcedure.getDeclaredMethod("method");
                assertEquals(1, method.invoke(TestProcedure.newInstance()));

                Class<?> Func = classLoader.loadClass("Func");
                assertEquals(1, Func.getDeclaredMethods().length);
                Method apply = Func.getDeclaredMethod("apply", Object.class);
                assertTrue(Modifier.isAbstract(apply.getModifiers()));

                Class<?> TestLambda = classLoader.loadClass("TestLambda");
                Object o = TestLambda.newInstance();
                Field func1F = TestLambda.getDeclaredField("func1");
                Field func2F = TestLambda.getDeclaredField("func2");
                Field func3F = TestLambda.getDeclaredField("func3");

                Object func1 = func1F.get(o);
                assertTrue(Func.isInstance(func1));
                Function func2 = (Function) func2F.get(o);
                Function1 func3 = (Function1) func3F.get(o);

                assertEquals(2, apply.invoke(func1, 1));
                assertEquals(2, func2.apply(1));
                assertEquals(2, func3.apply(1));

                Field listF = TestLambda.getDeclaredField("list");
                List list = (List) listF.get(o);
                assertEquals(Arrays.asList("1", "2", "3"), list);
        }

        @Test
        public void testOperator() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/operator.lt");

                Map<String, byte[]> map = generate(new BufferedReader(new InputStreamReader(is)), "operator.lt");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, map.get(name), 0, map.get(name).length);
                        }
                };

                Class<?> Num = classLoader.loadClass("Num");
                Constructor<?> con = Num.getConstructor(Object.class);

                assertEquals(con.newInstance(1), con.newInstance(1));
                assertEquals(con.newInstance(2), con.newInstance(2));
                assertNotEquals(con.newInstance(3), con.newInstance(1));

                Class<?> TestNum = classLoader.loadClass("TestNum");
                Method testAdd = TestNum.getMethod("testAdd");
                assertEquals(con.newInstance(2), testAdd.invoke(null));

                Method testSubtract = TestNum.getMethod("testSubtract");
                assertEquals(con.newInstance(-1), testSubtract.invoke(null));

                Method testMultiply = TestNum.getMethod("testMultiply");
                assertEquals(con.newInstance(12), testMultiply.invoke(null));

                Method testDivide = TestNum.getMethod("testDivide");
                assertEquals(con.newInstance(5), testDivide.invoke(null));

                Method testRemainder = TestNum.getMethod("testRemainder");
                assertEquals(con.newInstance(2), testRemainder.invoke(null));

                Method testShiftLeft = TestNum.getMethod("testShiftLeft");
                assertEquals(con.newInstance(4), testShiftLeft.invoke(null));

                Method testShiftRight = TestNum.getMethod("testShiftRight");
                assertEquals(con.newInstance(1), testShiftRight.invoke(null));

                Method testUnsignedShiftRight = TestNum.getMethod("testUnsignedShiftRight");
                assertEquals(con.newInstance(2), testUnsignedShiftRight.invoke(null));

                Method testEqual = TestNum.getMethod("testEqual");
                assertEquals(true, testEqual.invoke(null));

                Method testNotEqual = TestNum.getMethod("testNotEqual");
                assertEquals(true, testNotEqual.invoke(null));

                Method testEquals = TestNum.getMethod("testEquals");
                assertEquals(true, testEquals.invoke(null));

                Method testContains = TestNum.getMethod("testContains");
                try {
                        testContains.invoke(null);
                        fail();
                } catch (InvocationTargetException e) {
                        assertTrue(e.getCause() instanceof UnsupportedOperationException);
                }

                Method testGt = TestNum.getMethod("testGt");
                assertEquals(true, testGt.invoke(null));

                Method testGe = TestNum.getMethod("testGe");
                assertEquals(true, testGe.invoke(null));

                Method testLt = TestNum.getMethod("testLt");
                assertEquals(true, testLt.invoke(null));

                Method testLe = TestNum.getMethod("testLe");
                assertEquals(true, testLe.invoke(null));

                Method testAnd = TestNum.getMethod("testAnd");
                assertEquals(con.newInstance(0), testAnd.invoke(null));

                Method testXor = TestNum.getMethod("testXor");
                assertEquals(con.newInstance(3), testXor.invoke(null));

                Method testOr = TestNum.getMethod("testOr");
                assertEquals(con.newInstance(3), testOr.invoke(null));

                try {
                        Method testLogicNot = TestNum.getMethod("testLogicNot");
                        testLogicNot.invoke(null);
                        fail();
                } catch (InvocationTargetException e) {
                        assertTrue(e.getCause() instanceof UnsupportedOperationException);
                }

                Method testNot = TestNum.getMethod("testNot");
                assertEquals(con.newInstance(-2), testNot.invoke(null));

                Method testNegate = TestNum.getMethod("testNegate");
                assertEquals(con.newInstance(-1), testNegate.invoke(null));
        }

        @Test
        public void testTypeDef() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/typeDef.lt");

                Map<String, byte[]> map = generate(new BufferedReader(new InputStreamReader(is)), "typeDef.lt");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, map.get(name), 0, map.get(name).length);
                        }
                };

                // I_Have_Params
                Class<?> I_Am_A_Class = classLoader.loadClass("I_Am_A_Class");
                assertEquals(0, I_Am_A_Class.getDeclaredFields().length);
                assertEquals(0, I_Am_A_Class.getDeclaredMethods().length);
                assertEquals(1, I_Am_A_Class.getDeclaredConstructors().length);

                // I_Have_Params
                Class<?> I_Have_Params = classLoader.loadClass("I_Have_Params");
                assertEquals(2, I_Have_Params.getDeclaredFields().length);
                assertEquals(0, I_Have_Params.getDeclaredMethods().length);
                assertEquals(1, I_Have_Params.getDeclaredConstructors().length);

                Constructor<?> I_Have_Params_new = I_Have_Params.getConstructor(int.class, Object.class);

                Field I_Have_Params_param1 = I_Have_Params.getDeclaredField("param1");
                I_Have_Params_param1.setAccessible(true);
                assertEquals(int.class, I_Have_Params_param1.getType());
                Field I_Have_Params_param2 = I_Have_Params.getDeclaredField("param2");
                I_Have_Params_param2.setAccessible(true);
                assertEquals(Object.class, I_Have_Params_param2.getType());

                Object I_Have_Params_o = I_Have_Params_new.newInstance(1, "a");
                assertEquals(1, I_Have_Params_param1.get(I_Have_Params_o));
                assertEquals("a", I_Have_Params_param2.get(I_Have_Params_o));

                // I_Have_Params_too
                Class<?> I_Have_Params_too = classLoader.loadClass("I_Have_Params_too");
                assertEquals(2, I_Have_Params_too.getDeclaredFields().length);
                assertEquals(0, I_Have_Params_too.getDeclaredMethods().length);
                assertEquals(1, I_Have_Params_too.getDeclaredConstructors().length);

                Constructor<?> I_Have_Params_too_new = I_Have_Params_too.getConstructor(int.class, Object.class);

                Field I_Have_Params_too_param1 = I_Have_Params_too.getDeclaredField("param1");
                I_Have_Params_too_param1.setAccessible(true);
                assertEquals(int.class, I_Have_Params_too_param1.getType());
                Field I_Have_Params_too_param2 = I_Have_Params_too.getDeclaredField("param2");
                I_Have_Params_too_param2.setAccessible(true);
                assertEquals(Object.class, I_Have_Params_too_param2.getType());

                Object I_Have_Params_too_o = I_Have_Params_too_new.newInstance(1, "a");
                assertEquals(1, I_Have_Params_too_param1.get(I_Have_Params_too_o));
                assertEquals("a", I_Have_Params_too_param2.get(I_Have_Params_too_o));

                // I_Have_Parent
                Class<?> I_Have_Parent = classLoader.loadClass("I_Have_Parent");
                assertEquals(0, I_Have_Parent.getDeclaredFields().length);
                assertEquals(0, I_Have_Parent.getDeclaredMethods().length);
                assertEquals(1, I_Have_Parent.getDeclaredConstructors().length);
                assertEquals(I_Am_A_Class, I_Have_Parent.getSuperclass());

                // I_Have_Parent_And_Parent_Have_Args
                Class<?> I_Have_Parent_And_Parent_Have_Args = classLoader.loadClass("I_Have_Parent_And_Parent_Have_Args");
                assertEquals(0, I_Have_Parent_And_Parent_Have_Args.getDeclaredFields().length);
                assertEquals(0, I_Have_Parent_And_Parent_Have_Args.getDeclaredMethods().length);
                assertEquals(1, I_Have_Parent_And_Parent_Have_Args.getConstructors().length);
                Object I_Have_Parent_And_Parent_Have_Args_o = I_Have_Parent_And_Parent_Have_Args.newInstance();
                assertEquals(1, I_Have_Params_param1.get(I_Have_Parent_And_Parent_Have_Args_o));
                assertEquals("2", I_Have_Params_param2.get(I_Have_Parent_And_Parent_Have_Args_o));

                // I_Have_Interfaces
                Class<?> I_Have_Interfaces = classLoader.loadClass("I_Have_Interfaces");
                assertEquals(List.class, I_Have_Interfaces.getInterfaces()[0]);
                assertEquals(Serializable.class, I_Have_Interfaces.getInterfaces()[1]);
                assertEquals(2, I_Have_Interfaces.getInterfaces().length);

                // I_Have_Both_Class_And_Interface
                Class<?> I_Have_Both_Class_And_Interface = classLoader.loadClass("I_Have_Both_Class_And_Interface");
                assertEquals(List.class, I_Have_Both_Class_And_Interface.getInterfaces()[0]);
                assertEquals(1, I_Have_Both_Class_And_Interface.getInterfaces().length);
                assertEquals(I_Am_A_Class, I_Have_Both_Class_And_Interface.getSuperclass());

                // My_Constructor_Is_Private_But_Type_Is_Still_Public
                Class<?> My_Constructor_Is_Private_But_Type_Is_Still_Public = classLoader.loadClass("My_Constructor_Is_Private_But_Type_Is_Still_Public");
                assertTrue(Modifier.isPublic(My_Constructor_Is_Private_But_Type_Is_Still_Public.getModifiers()));
                assertTrue(Modifier.isPrivate(My_Constructor_Is_Private_But_Type_Is_Still_Public.getDeclaredConstructors()[0].getModifiers()));

                // I_Have_Fields
                Class<?> I_Have_Fields = classLoader.loadClass("I_Have_Fields");
                Field I_Have_Fields_i_am_field = I_Have_Fields.getDeclaredField("i_am_field");
                I_Have_Fields_i_am_field.setAccessible(true);
                Field I_Have_Fields_i_am_a_field_with_init_value = I_Have_Fields.getDeclaredField("i_am_a_field_with_init_value");
                I_Have_Fields_i_am_a_field_with_init_value.setAccessible(true);
                assertEquals(int.class, I_Have_Fields_i_am_field.getType());
                assertEquals(Object.class, I_Have_Fields_i_am_a_field_with_init_value.getType());

                Object I_Have_Fields_o = I_Have_Fields.newInstance();
                assertEquals(0, I_Have_Fields_i_am_field.get(I_Have_Fields_o));
                assertEquals("init value", I_Have_Fields_i_am_a_field_with_init_value.get(I_Have_Fields_o));

                // I_Have_Methods
                Class<?> I_Have_Methods = classLoader.loadClass("I_Have_Methods");
                Object I_Have_Methods_o = I_Have_Methods.newInstance();

                Method i_am_a_simple_method = I_Have_Methods.getDeclaredMethod("i_am_a_simple_method");
                assertEquals(Object.class, i_am_a_simple_method.getReturnType());

                Method i_am_a_method_with_return_type_spec = I_Have_Methods.getDeclaredMethod("i_am_a_method_with_return_type_spec");
                assertEquals(void.class, i_am_a_method_with_return_type_spec.getReturnType());

                Method i_am_a_method_with_only_one_statement = I_Have_Methods.getDeclaredMethod("i_am_a_method_with_only_one_statement");
                assertEquals(Object.class, i_am_a_method_with_only_one_statement.getReturnType());
                assertEquals("this object will be returned", i_am_a_method_with_only_one_statement.invoke(I_Have_Methods_o));

                Method i_am_a_method_with_only_one_statement_with_return_type = I_Have_Methods.getDeclaredMethod("i_am_a_method_with_only_one_statement_with_return_type");
                assertEquals(String.class, i_am_a_method_with_only_one_statement_with_return_type.getReturnType());
                assertEquals("this string will be returned", i_am_a_method_with_only_one_statement_with_return_type.invoke(I_Have_Methods_o));

                Method i_am_a_method_with_no_statements = I_Have_Methods.getDeclaredMethod("i_am_a_method_with_no_statements");
                assertEquals(Object.class, i_am_a_method_with_no_statements.getReturnType());

                Method i_have_parameters = I_Have_Methods.getDeclaredMethod("i_have_parameters", Object.class);
                assertEquals(Object.class, i_have_parameters.getReturnType());

                // I_Have_Statements
                Class<?> I_Have_Statements = classLoader.loadClass("I_Have_Statements");
                Field I_Have_Statements_list = I_Have_Statements.getDeclaredField("list");
                I_Have_Statements_list.setAccessible(true);
                Object I_Have_Statements_o = I_Have_Statements.newInstance();
                assertEquals(Collections.singletonList(1), I_Have_Statements_list.get(I_Have_Statements_o));

                // I_Have_Static
                Class<?> I_Have_Static = classLoader.loadClass("I_Have_Static");
                Field I_Have_Static_field = I_Have_Static.getDeclaredField("field");
                assertTrue(Modifier.isStatic(I_Have_Static_field.getModifiers()));
                Method I_Have_Static_method = I_Have_Static.getDeclaredMethod("method");
                assertTrue(Modifier.isStatic(I_Have_Static_method.getModifiers()));
                Field I_Have_Static_list = I_Have_Static.getDeclaredField("list");
                I_Have_Static_list.setAccessible(true);
                assertTrue(Modifier.isStatic(I_Have_Static_list.getModifiers()));
                assertEquals(Arrays.asList(1, 2), I_Have_Static_list.get(null));
                Field I_Have_Static_field2 = I_Have_Static.getDeclaredField("field2");
                assertTrue(Modifier.isStatic(I_Have_Static_field2.getModifiers()));
                Method I_Have_Static_method2 = I_Have_Static.getDeclaredMethod("method2");
                assertTrue(Modifier.isStatic(I_Have_Static_method2.getModifiers()));

                // I_Am_An_Interface
                Class<?> I_Am_An_Interface = classLoader.loadClass("I_Am_An_Interface");
                assertTrue(I_Am_An_Interface.isInterface());

                // Interface_Have_Fields
                Class<?> Interface_Have_Fields = classLoader.loadClass("Interface_Have_Fields");
                assertTrue(Interface_Have_Fields.isInterface());
                Field Interface_Have_Fields_field = Interface_Have_Fields.getField("field");
                assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, Interface_Have_Fields_field.getModifiers());
                assertEquals(1, Interface_Have_Fields_field.get(null));

                // Interface_Have_Methods
                Class<?> Interface_Have_Methods = classLoader.loadClass("Interface_Have_Methods");
                assertTrue(Interface_Have_Methods.isInterface());
                Method i_am_an_abstract_method = Interface_Have_Methods.getMethod("i_am_an_abstract_method");
                assertTrue(Modifier.isAbstract(i_am_an_abstract_method.getModifiers()));
                Method i_am_a_default_method = Interface_Have_Methods.getMethod("i_am_a_default_method");
                assertFalse(Modifier.isAbstract(i_am_a_default_method.getModifiers()));
                assertTrue(i_am_a_default_method.isDefault());

                // TesterForInterface_Have_Methods
                Class<?> TesterForInterface_Have_Methods = classLoader.loadClass("TesterForInterface_Have_Methods");
                assertEquals(Interface_Have_Methods, TesterForInterface_Have_Methods.getInterfaces()[0]);
                Object TesterForInterface_Have_Methods_o = TesterForInterface_Have_Methods.newInstance();
                assertEquals("abs method impl", i_am_an_abstract_method.invoke(TesterForInterface_Have_Methods_o));
                assertEquals("default method", i_am_a_default_method.invoke(TesterForInterface_Have_Methods_o));
        }

        @Test
        public void testFunCompile() throws Exception {
                InputStream is = TestDemo.class.getResourceAsStream("/lang-demo/fun.lt");

                Map<String, byte[]> map = generate(new BufferedReader(new InputStreamReader(is)), "fun.lt");
                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                return defineClass(name, map.get(name), 0, map.get(name).length);
                        }
                };

                Class<?> TestStdCout = classLoader.loadClass("TestStdCout");
                TestStdCout.newInstance();
        }
}
