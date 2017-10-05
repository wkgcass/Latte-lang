package lt.compiler.cases;

import lt.compiler.cases.anno.TestFieldAnno;
import lt.repl.Compiler;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * test dot package
 */
public class TestDotPackage {
        private ScriptEngine scriptEngine;

        private Class<?> compile(String className, final String... codes) throws Exception {
                Compiler compiler = new Compiler(Thread.currentThread().getContextClassLoader());
                ClassLoader cl = compiler.compile(new HashMap<String, String>() {{
                        int i = 0;
                        for (String code : codes) {
                                put("f" + (i++), code);
                        }
                }});
                return cl.loadClass(className);
        }

        @Before
        public void setUp() throws Exception {
                scriptEngine = new ScriptEngineManager().getEngineByName("Latte-lang");
        }

        @Test
        public void testGetType() throws Exception {
                scriptEngine.eval("c = type java.lang.Object");
                assertEquals(Object.class, scriptEngine.get("c"));
        }

        @Test
        public void testGetTypeLong() throws Exception {
                scriptEngine.eval("c = type lt.compiler.cases.anno.TestFieldAnno");
                assertEquals(TestFieldAnno.class, scriptEngine.get("c"));
        }

        @Test
        public void testAccess() throws Exception {
                Class<?> R = compile("R", "" +
                                "package x::y::z::a::b::c\n" +
                                "class X {\n" +
                                "    static\n" +
                                "        a = 100\n" +
                                "}",
                        "" +
                                "class R {\n" +
                                "    static\n" +
                                "        def m = x.y.z.a.b.c.X.a\n" +
                                "}"
                );
                assertEquals(100, R.getMethod("m").invoke(null));
        }

        @Test
        public void testInvoke() throws Exception {
                Class<?> R = compile("R", "" +
                                "package a::b::c::d::e\n" +
                                "class X {\n" +
                                "    static\n" +
                                "        def a = 200\n" +
                                "}",
                        "" +
                                "class R {\n" +
                                "    static\n" +
                                "        def m = a.b.c.d.e.X.a()\n" +
                                "}"
                );
                assertEquals(200, R.getMethod("m").invoke(null));
        }

        @Test
        public void testNew() throws Exception {
                Class<?> R = compile("R", "" +
                                "package m::n::o::p::q\n" +
                                "class X\n" +
                                "    public f = 123",
                        "" +
                                "class R\n" +
                                "    static\n" +
                                "        def m = m.n.o.p.q.X().f"
                );
                assertEquals(123, R.getMethod("m").invoke(null));
        }

        @Test
        public void testPackageName() throws Exception {
                Class<?> R = compile("x.y.z.m.n.o.p.q.R", "" +
                        "package x.y.z.m.n.o.p.q\n" +
                        "class R"
                );
                assertEquals("x.y.z.m.n.o.p.q.R", R.getName());
        }

        @Test
        public void testImport() throws Exception {
                Class<?> R = compile("R", "" +
                                "package a.b.c.d.e.f\n" +
                                "class X\n" +
                                "    static\n" +
                                "        a = 1",
                        "" +
                                "import a.b.c.d.e.f._\n" +
                                "class R\n" +
                                "    static\n" +
                                "        def m = X.a"
                );
                assertEquals(1, R.getMethod("m").invoke(null));
        }

        @Test
        public void testImportStatic() throws Exception {
                Class<?> R = compile("R", "" +
                                "package a.b.c.d.e.f\n" +
                                "class X\n" +
                                "    static\n" +
                                "        a = 2",
                        "" +
                                "import a.b.c.d.e.f.X._\n" +
                                "class R\n" +
                                "    static\n" +
                                "        def m = a"
                );
                assertEquals(2, R.getMethod("m").invoke(null));
        }

        @Test
        public void testImportShort() throws Exception {
                Class<?> R = compile("R", "" +
                                "package a\n" +
                                "class X\n" +
                                "    static\n" +
                                "        a = 3",
                        "" +
                                "import a._\n" +
                                "class R\n" +
                                "    static\n" +
                                "        def m = X.a"
                );
                assertEquals(3, R.getMethod("m").invoke(null));
        }

        @Test
        public void testImportStaticShort() throws Exception {
                Class<?> R = compile("R", "" +
                                "class X\n" +
                                "    static\n" +
                                "        a = 4",
                        "" +
                                "import X._\n" +
                                "class R\n" +
                                "    static\n" +
                                "        def m = a");
                assertEquals(4, R.getMethod("m").invoke(null));
        }
}
