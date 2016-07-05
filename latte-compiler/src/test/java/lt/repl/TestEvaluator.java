/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.repl;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * test evaluator
 */
public class TestEvaluator {
        private static final ClassPathLoader CLASS_PATH_LOADER = new ClassPathLoader(Thread.currentThread().getContextClassLoader());

        @Test
        public void testSimpleCode() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(1, evaluator.eval("1").result);
        }

        @Test
        public void testSimpleExpression() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(2, evaluator.eval("1+1").result);
        }

        @Test
        public void testMultipleLineExpression() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(
                        new LinkedHashMap<Object, Object>() {{
                                put("id", 1);
                                put("name", "cass");
                        }},
                        evaluator.eval("" +
                                        "{\n" +
                                        "    'id':1\n" +
                                        "    'name':'cass'\n" +
                                        "}"
                        ).result);
        }

        @Test
        public void testEvaluateTwice() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(1, evaluator.eval("1").result);
                assertEquals(2, evaluator.eval("1+1").result);
        }

        @Test
        public void testEvaluateTwiceWithMultipleLineExp() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(
                        new LinkedHashMap<Object, Object>() {{
                                put("id", 1);
                                put("name", "cass");
                        }},
                        evaluator.eval("" +
                                        "{\n" +
                                        "    'id':1\n" +
                                        "    'name':'cass'\n" +
                                        "}"
                        ).result);
                assertEquals(2, evaluator.eval("1+1").result);
        }

        @Test
        public void testEvaluateThreeTimes() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(1, evaluator.eval("1").result);
                assertEquals(2, evaluator.eval("1+1").result);
                assertEquals(100, evaluator.eval("10*10").result);
        }

        @Test
        public void testEvaluateThreeTimesWithMultipleLineExp() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                assertEquals(
                        new LinkedHashMap<Object, Object>() {{
                                put("id", 1);
                                put("name", "cass");
                        }},
                        evaluator.eval("" +
                                        "{\n" +
                                        "    'id':1\n" +
                                        "    'name':'cass'\n" +
                                        "}"
                        ).result);
                assertEquals(2, evaluator.eval("1+1").result);
                assertEquals(100, evaluator.eval("10*10").result);
        }

        @Test
        public void testEvaluateVariableDef() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                Evaluator.Entry entry = evaluator.eval("i=10*10");
                assertEquals("i", entry.name);
                assertEquals(100, entry.result);
        }

        @Test
        public void testEvaluateFieldSet() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                Evaluator.Entry entry = evaluator.eval("i=10*10");
                assertEquals("i", entry.name);
                assertEquals(100, entry.result);
                assertEquals(2, evaluator.eval("i=2").result);
                assertEquals(2, evaluator.eval("i").result);
        }

        @Test
        public void testEvaluateStmt() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                Evaluator.Entry entry = evaluator.eval("method()=1");
                assertNull(entry.name);
                Object o = entry.result;
                Method m = o.getClass().getDeclaredMethod("method");
                assertEquals(1, m.invoke(o));
        }

        @Test
        public void testEvaluateMethod() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                evaluator.eval("method()=1");
                assertEquals(1, evaluator.eval("method()").result);
        }

        @Test
        public void testEvaluateClass() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                @SuppressWarnings("unchecked")
                java.util.List<Class<?>> list = (java.util.List<Class<?>>) evaluator.eval("class User").result;
                assertEquals(1, list.size());
                assertEquals("User", list.get(0).getName());
        }

        @Test
        public void testEvaluateClassAndStmt() throws Exception {
                Evaluator evaluator = new Evaluator(CLASS_PATH_LOADER);
                Object o = evaluator.eval("" +
                        "class User\n" +
                        "User()").result;
                assertEquals("User", o.getClass().getName());
        }

        @Test
        public void testPrimitiveCast() throws Exception {
                Evaluator e = new Evaluator(CLASS_PATH_LOADER);
                // int to short
                assertEquals((short) 1, e.eval("int_short:int=1\nint_short as short").result);
                // int to byte
                assertEquals((byte) 1, e.eval("int_byte:int=1\nint_byte as byte").result);
                // int to char
                assertEquals('a', e.eval("int_char:int=97\nint_char as char").result);
                // int to long
                assertEquals((long) 1, e.eval("int_long:int=1\nint_long as long").result);
                // int to float
                assertEquals((float) 1, e.eval("int_float:int=1\nint_float as float").result);
                // int to double
                assertEquals((double) 1, e.eval("int_double:int=1\nint_double as double").result);
                // int to bool
                assertEquals(true, e.eval("int_bool1:int=1\nint_bool1 as bool").result);
                assertEquals(false, e.eval("int_bool2:int=0\nint_bool2 as bool").result);

                // short to int
                assertEquals(1, e.eval("short_int:short=1\nshort_int as int").result);
                // short to byte
                assertEquals((byte) 1, e.eval("short_byte:short=1\nshort_byte as byte").result);
                // short to char
                assertEquals('a', e.eval("short_char:short=97\nshort_char as char").result);
                // short to long
                assertEquals((long) 1, e.eval("short_long:short=1\nshort_long as long").result);
                // short to float
                assertEquals((float) 1, e.eval("short_float:short=1\nshort_float as float").result);
                // short to double
                assertEquals((double) 1, e.eval("short_double:short=1\nshort_double as double").result);
                // short to bool
                assertEquals(true, e.eval("short_bool1:short=1\nshort_bool1 as bool").result);
                assertEquals(false, e.eval("short_bool2:short=0\nshort_bool2 as bool").result);

                // byte to int
                assertEquals(1, e.eval("byte_int:byte=1\nbyte_int as int").result);
                // byte to short
                assertEquals((short) 1, e.eval("byte_short:byte=1\nbyte_short as short").result);
                // byte to char
                assertEquals('a', e.eval("byte_char:byte=97\nbyte_char as char").result);
                // byte to long
                assertEquals((long) 1, e.eval("byte_long:byte=1\nbyte_long as long").result);
                // byte to float
                assertEquals((float) 1, e.eval("byte_float:byte=1\nbyte_float as float").result);
                // byte to double
                assertEquals((double) 1, e.eval("byte_double:byte=1\nbyte_double as double").result);
                // byte to bool
                assertEquals(true, e.eval("byte_bool1:byte=1\nbyte_bool1 as bool").result);
                assertEquals(false, e.eval("byte_bool2:byte=0\nbyte_bool2 as bool").result);

                // char to int
                assertEquals(97, e.eval("char_int:char='a'\nchar_int as int").result);
                // char to short
                assertEquals((short) 97, e.eval("char_short:char='a'\nchar_short as short").result);
                // char to byte
                assertEquals((byte) 97, e.eval("char_byte:char='a'\nchar_byte as byte").result);
                // char to long
                assertEquals((long) 97, e.eval("char_long:char='a'\nchar_long as long").result);
                // char to float
                assertEquals((float) 97, e.eval("char_float:char='a'\nchar_float as float").result);
                // char to double
                assertEquals((double) 97, e.eval("char_double:char='a'\nchar_double as double").result);
                // char to bool
                assertEquals(true, e.eval("char_bool:char='a'\nchar_bool as bool").result);

                // long to int
                assertEquals(1, e.eval("long_int:long=1\nlong_int as int").result);
                // long to short
                assertEquals((short) 1, e.eval("long_short:long=1\nlong_short as short").result);
                // long to byte
                assertEquals((byte) 1, e.eval("long_byte:long=1\nlong_byte as byte").result);
                // long to char
                assertEquals('a', e.eval("long_char:long=97\nlong_char as char").result);
                // long to float
                assertEquals((float) 1, e.eval("long_float:long=1\nlong_float as float").result);
                // long to double
                assertEquals((double) 1, e.eval("long_double:long=1\nlong_double as double").result);
                // long to bool
                assertEquals(true, e.eval("long_bool1:long=1\nlong_bool1 as bool").result);
                assertEquals(false, e.eval("long_bool2:long=0\nlong_bool2 as bool").result);

                // float to int
                assertEquals(1, e.eval("float_int:float=1\nfloat_int as int").result);
                // float to short
                assertEquals((short) 1, e.eval("float_short:float=1\nfloat_short as short").result);
                // float to byte
                assertEquals((byte) 1, e.eval("float_byte:float=1\nfloat_byte as byte").result);
                // float to char
                assertEquals('a', e.eval("float_char:float=97\nfloat_char as char").result);
                // float to long
                assertEquals((long) 1, e.eval("float_long:float=1\nfloat_long as long").result);
                // float to double
                assertEquals((double) 1, e.eval("float_double:float=1\nfloat_double as double").result);
                // float to bool
                assertEquals(true, e.eval("float_bool1:float=1\nfloat_bool1 as bool").result);
                assertEquals(false, e.eval("float_bool2:float=0\nfloat_bool2 as bool").result);

                // double to int
                assertEquals(1, e.eval("double_int:double=1\ndouble_int as int").result);
                // double to short
                assertEquals((short) 1, e.eval("double_short:double=1\ndouble_short as short").result);
                // double to byte
                assertEquals((byte) 1, e.eval("double_byte:double=1\ndouble_byte as byte").result);
                // double to char
                assertEquals('a', e.eval("double_char:double=97\ndouble_char as char").result);
                // double to long
                assertEquals((long) 1, e.eval("double_long:double=1\ndouble_long as long").result);
                // double to float
                assertEquals((float) 1, e.eval("double_float:double=1\ndouble_float as float").result);
                // double to bool
                assertEquals(true, e.eval("double_bool1:double=1\ndouble_bool1 as bool").result);
                assertEquals(false, e.eval("double_bool2:double=0\ndouble_bool2 as bool").result);

                // bool cannot be number or char
        }
}
