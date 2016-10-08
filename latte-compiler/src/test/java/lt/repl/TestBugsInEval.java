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

import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * bugs caught by eval
 */
public class TestBugsInEval {
        Evaluator evaluator;

        @Before
        public void setUp() throws Exception {
                evaluator = new Evaluator(new ClassPathLoader(Thread.currentThread().getContextClassLoader()));
        }

        private Class<?> retrieveClass(String code, String clsName) throws IOException, SyntaxException, ClassNotFoundException {
                ErrorManager err = new ErrorManager(true);

                IndentScanner lexicalProcessor = new IndentScanner("test.lt", new StringReader(code), new Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(
                        map,
                        Thread.currentThread().getContextClassLoader(),
                        err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types, semanticProcessor.getTypes());
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

        /**
         * arr:[]String = [null]<br>
         * arr[0]='abc'<br>
         * <pre>
         * java.lang.VerifyError: Bad type on operand stack in putfield
         * Exception Details:
         * Location:
         * Evaluate.<init>(Ljava/lang/Object;)V @32: putfield
         * Reason:
         * Type 'java/lang/Object' (current frame, stack[1]) is not assignable to 'Evaluate' (constant pool 41)
         * Current Frame:
         * bci: @32
         * flags: { }
         * locals: { 'Evaluate', 'java/lang/Object' }
         * stack: { 'Evaluate', 'java/lang/Object', 'java/lang/Object' }
         * Bytecode:
         * 0x0000000: 2ab7 000e b800 1457 2a2b b500 162a 2b12
         * 0x0000010: 1712 19ba 0024 0000 2b12 17ba 0027 0000
         * 0x0000020: b500 292a b400 2957 572a 2bb5 0016 2ab4
         * 0x0000030: 0016 57b1
         * </pre><br>
         * date: 2016-05-09 10:37
         *
         * @throws Exception ex
         */
        @Test
        public void test1() throws Exception {
                evaluator.eval("arr:[]String=[null]");
                Evaluator.Entry entry = evaluator.eval("arr[0]='abc'");
                assertEquals("res0", entry.name);
                assertEquals("abc", entry.result);
        }

        @Test
        public void test1_same() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class Test1(arr)\n" +
                                "    res0=(arr[0]='abc')\n" +
                                "    this.arr=arr"
                        , "Test1");
                Constructor<?> cons = cls.getConstructor(Object.class);
                String[] arr = new String[]{null};
                Object o = cons.newInstance((Object) arr);
                Field f = cls.getDeclaredField("res0");
                f.setAccessible(true);
                assertEquals("abc", f.get(o));
        }

        /**
         * class Test<br>
         * m(a):Unit<br>
         * t = Test()<br>
         * t.m(null) ----- error<br>
         * n=null<br>
         * t.m(n) ------ ok<br>
         * Test().m(null) ------- ok<br>
         * <pre>
         * java.lang.BootstrapMethodError: java.lang.NoClassDefFoundError: null
         * at Evaluate.<init>(EVALUATE.lts:1)
         * at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
         * at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
         * at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
         * at java.lang.reflect.Constructor.newInstance(Constructor.java:422)
         * at lt.repl.Evaluator.eval(Evaluator.java:225)
         * at lt.repl.REPL.main(REPL.java:82)
         * Caused by: java.lang.NoClassDefFoundError: null
         * ... 7 more
         * Caused by: java.lang.ClassNotFoundException: null
         * at lt.repl.Evaluator$3.findClass(Evaluator.java:213)
         * at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
         * at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
         * ... 7 more
         * </pre><br>
         * date : 2016-05-09 20:09
         *
         * @throws Exception ex
         */
        @Test
        public void test2() throws Exception {
                evaluator.eval("" +
                        "class Test\n" +
                        "    m(a):Unit");
                evaluator.eval("t=Test()");
                evaluator.eval("t.m(null)");
        }

        @Test
        public void testFunctionAdd() throws Exception {
                Object res = evaluator.eval("" +
                        "x = (a,b,c)->a+b+c\n" +
                        "x(1,2,3)" +
                        "").result;
                assertEquals(6, res);
        }

        @Test
        public void testEmptyLambda() throws Exception {
                evaluator.eval("()->...");
        }

        @Test
        public void testOpAssign() throws Exception {
                evaluator.eval("i = 1");
                Object res = evaluator.eval("i <<= 2").result;
                assertEquals(4, res);
        }

        @Test
        public void testInternalLambdaAndOtherStatements() throws Exception {
                evaluator.setScannerType(Evaluator.SCANNER_TYPE_BRACE);
                assertEquals(2, evaluator.eval("" +
                        "[1, 2, 3, 4].stream.filter{it > 2}\n" +
                        "1 + 1").result);
        }
}
