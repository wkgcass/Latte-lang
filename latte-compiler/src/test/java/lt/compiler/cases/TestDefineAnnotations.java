package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * test defined annotations
 */
public class TestDefineAnnotations {
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

        private Annotation getAnno(Class<?> cls, String name) {
                for (Annotation a : cls.getAnnotations())
                        if (a.annotationType().getName().equals(name)) return a;
                fail("annotation not found");
                throw new AssertionError();
        }

        @Test
        public void testPrimitives() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "annotation A\n" +
                                "    i:int=1\n" +
                                "    l:long=2\n" +
                                "    f:float=3\n" +
                                "    d:double=4\n" +
                                "    aByte:byte=5\n" +
                                "    aBool:bool=true\n" +
                                "    aShort:short=6\n" +
                                "    aChar:char='c'\n" +
                                "    str:String='xx'\n" +
                                "    cls:Class=type Object\n" +
                                "@A\n" +
                                "class TestPrimitives"
                        , "TestPrimitives");
                Annotation annoA = getAnno(cls, "A");
                assertNotNull(annoA);
                assertEquals(1, annoA.getClass().getMethod("i").invoke(annoA));
                assertEquals(2L, annoA.getClass().getMethod("l").invoke(annoA));
                assertEquals(3f, annoA.getClass().getMethod("f").invoke(annoA));
                assertEquals(4d, annoA.getClass().getMethod("d").invoke(annoA));
                assertEquals((byte) 5, annoA.getClass().getMethod("aByte").invoke(annoA));
                assertEquals(true, annoA.getClass().getMethod("aBool").invoke(annoA));
                assertEquals((short) 6, annoA.getClass().getMethod("aShort").invoke(annoA));
                assertEquals('c', annoA.getClass().getMethod("aChar").invoke(annoA));
                assertEquals("xx", annoA.getClass().getMethod("str").invoke(annoA));
                assertEquals(Object.class, annoA.getClass().getMethod("cls").invoke(annoA));
        }

        @Test
        public void testAnno() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "import lt::compiler::_\n" +
                                "annotation A\n" +
                                "    a:AnnotationTest=@AnnotationTest\n" +
                                "@A\n" +
                                "class TestAnno"
                        , "TestAnno");
                Annotation annoA = getAnno(cls, "A");
                assertEquals("@lt.compiler.AnnotationTest(" +
                                "f=100.0, " +
                                "d=100.0, " +
                                "e=PUBLIC, " +
                                "cls=class java.lang.String, " +
                                "b=100, " +
                                "c=a, " +
                                "l=100, " +
                                "i=100, " +
                                "bo=true, " +
                                "str=str, " +
                                "s=100, " +
                                "anno=@lt.compiler.MyAnno(" +
                                /**/"i=100, " +
                                /**/"str=a" +
                                "), " +
                                "clsArr=[" +
                                /**/"class java.lang.Class, " +
                                /**/"class java.lang.String], " +
                                /**/"strArr=[a, b]" +
                                ")",
                        annoA.getClass().getMethod("a").invoke(annoA).toString());
        }

        @Test
        public void testArrayPrimitives() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "annotation A\n" +
                                "    i:[]int = [1,2,3]\n" +
                                "    l:[]long = [1,2,3]\n" +
                                "    f:[]float = [1,2,3]\n" +
                                "    d:[]double = [1,2,3]\n" +
                                "    aByte:[]byte = [1,2,3]\n" +
                                "    aShort:[]short = [1,2,3]\n" +
                                "    aChar:[]char = ['a','b','c']\n" +
                                "    aBool:[]bool = [true,false,true]\n" +
                                "    str:[]String = ['xx', 'yy', 'zz']\n" +
                                "    cls:[]Class = [type Object, type Integer]\n" +
                                "@A\n" +
                                "class TestArrayPrimitives"
                        , "TestArrayPrimitives");
                Annotation annoA = getAnno(cls, "A");
                assertEquals("@A(" +
                                "f=[1.0, 2.0, 3.0], " +
                                "aByte=[1, 2, 3], " +
                                "d=[1.0, 2.0, 3.0], " +
                                "aShort=[1, 2, 3], " +
                                "cls=[class java.lang.Object, class java.lang.Integer], " +
                                "str=[xx, yy, zz], " +
                                "aChar=[a, b, c], " +
                                "l=[1, 2, 3], " +
                                "aBool=[true, false, true], " +
                                "i=[1, 2, 3])",
                        annoA.toString());
        }
}
