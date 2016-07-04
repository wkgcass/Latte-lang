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

package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.cases.anno.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * test annotations
 */
public class TestAnnotations {
        private Class<?> retrieveClass(String code, String clsName) throws IOException, SyntaxException, ClassNotFoundException {
                ErrorManager err = new ErrorManager(true);
                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader(code), new Scanner.Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();

                ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name)
                                throws ClassNotFoundException {
                                byte[] bs = list.get(name);
                                return defineClass(name, bs, 0, bs.length);
                        }
                };

                return classLoader.loadClass(clsName);
        }

        @Test
        public void testType() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "@TestTypeAnno\n" +
                        "class TestType"
                        , "TestType");
                assertTrue(cls.isAnnotationPresent(TestTypeAnno.class));
        }

        @Test
        public void testConstructor() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "@TestConstructorAnno\n" +
                        "class TestConstructor"
                        , "TestConstructor");
                Constructor<?> cons = cls.getConstructor();
                assertTrue(cons.isAnnotationPresent(TestConstructorAnno.class));
        }

        @Test
        public void testConsParameter() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestConsParameter(\n" +
                        "    @TestParamAnno\n" +
                        "    a\n" +
                        ")"
                        , "TestConsParameter");
                Annotation[] annos = cls.getConstructor(Object.class).getParameterAnnotations()[0];
                assertEquals(1, annos.length);
                assertTrue(TestParamAnno.class.isAssignableFrom(annos[0].getClass()));
        }

        @Test
        public void testPField() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestPField(\n" +
                        "    @TestFieldAnno\n" +
                        "    a\n" +
                        ")"
                        , "TestPField");
                Field f = cls.getDeclaredField("a");
                assertTrue(f.isAnnotationPresent(TestFieldAnno.class));
        }

        @Test
        public void testField() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestField\n" +
                        "    @TestFieldAnno\n" +
                        "    a\n"
                        , "TestField");
                Field f = cls.getDeclaredField("a");
                assertTrue(f.isAnnotationPresent(TestFieldAnno.class));
        }

        @Test
        public void testMethod() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestMethod\n" +
                        "    @TestMethodAnno\n" +
                        "    method():Unit\n"
                        , "TestMethod");
                Method method = cls.getMethod("method");
                assertTrue(method.isAnnotationPresent(TestMethodAnno.class));
        }

        @Test
        public void testMethodParam() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestMethod\n" +
                        "    method(\n" +
                        "        @TestParamAnno\n" +
                        "        a\n" +
                        "    ):Unit\n"
                        , "TestMethod");
                Method method = cls.getMethod("method", Object.class);
                Annotation[] annotations = method.getParameterAnnotations()[0];
                assertTrue(TestParamAnno.class.isAssignableFrom(annotations[0].getClass()));
        }

        @Test
        public void testClassDefaultValue() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "@TestConstructorAnno\n" +
                        "class TestClassDefaultValue(a=1)"
                        , "TestClassDefaultValue");
                Constructor<?> cons = cls.getConstructor(Object.class);
                assertTrue(cons.isAnnotationPresent(TestConstructorAnno.class));
                cons = cls.getConstructor();
                assertTrue(cons.isAnnotationPresent(TestConstructorAnno.class));
        }

        @Test
        public void testMethodDefaultValue() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "class TestMethod\n" +
                        "    @TestMethodAnno\n" +
                        "    method(a=1):Unit\n"
                        , "TestMethod");
                Method method = cls.getMethod("method");
                assertTrue(method.isAnnotationPresent(TestMethodAnno.class));
                method = cls.getMethod("method", Object.class);
                assertTrue(method.isAnnotationPresent(TestMethodAnno.class));
        }

        @Test
        public void testDataClass() throws Exception {
                Class<?> cls = retrieveClass("" +
                        "import lt::compiler::cases::anno::_\n" +
                        "@TestConstructorAnno\n" +
                        "data class TestDataClass(a,b)"
                        , "TestDataClass");
                Constructor<?> cons = cls.getConstructor();
                assertTrue(cons.isAnnotationPresent(TestConstructorAnno.class));
        }
}
