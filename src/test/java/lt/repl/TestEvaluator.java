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

import lt.compiler.JarLoader;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * test evaluator
 */
public class TestEvaluator {
        private static final JarLoader jarLoader = new JarLoader();

        @Test
        public void testSimpleCode() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
                assertEquals(1, evaluator.eval("1").result);
        }

        @Test
        public void testSimpleExpression() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
                assertEquals(2, evaluator.eval("1+1").result);
        }

        @Test
        public void testMultipleLineExpression() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
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
                Evaluator evaluator = new Evaluator(jarLoader);
                assertEquals(1, evaluator.eval("1").result);
                assertEquals(2, evaluator.eval("1+1").result);
        }

        @Test
        public void testEvaluateTwiceWithMultipleLineExp() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
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
                Evaluator evaluator = new Evaluator(jarLoader);
                assertEquals(1, evaluator.eval("1").result);
                assertEquals(2, evaluator.eval("1+1").result);
                assertEquals(100, evaluator.eval("10*10").result);
        }

        @Test
        public void testEvaluateThreeTimesWithMultipleLineExp() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
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
                Evaluator evaluator = new Evaluator(jarLoader);
                Evaluator.Entry entry = evaluator.eval("i=10*10");
                assertEquals("i", entry.name);
                assertEquals(100, entry.result);
        }

        @Test
        public void testEvaluateFieldSet() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
                Evaluator.Entry entry = evaluator.eval("i=10*10");
                assertEquals("i", entry.name);
                assertEquals(100, entry.result);
                assertEquals(2, evaluator.eval("i=2").result);
                assertEquals(2, evaluator.eval("i").result);
        }

        @Test
        public void testEvaluateStmt() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
                Evaluator.Entry entry = evaluator.eval("method()=1");
                assertNull(entry.name);
                Object o = entry.result;
                Method m = o.getClass().getDeclaredMethod("method");
                assertEquals(1, m.invoke(o));
        }

        @Test
        public void testEvaluateMethod() throws Exception {
                Evaluator evaluator = new Evaluator(jarLoader);
                evaluator.eval("method()=1");
                assertEquals(1, evaluator.eval("method()").result);
        }
}
