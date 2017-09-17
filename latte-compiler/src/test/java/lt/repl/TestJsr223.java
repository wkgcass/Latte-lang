package lt.repl;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.pre.Modifier;
import lt.repl.scripting.CL;
import lt.repl.scripting.LatteEngine;
import lt.repl.scripting.LatteEngineFactory;
import lt.repl.scripting.LatteScope;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * test scripting
 */
public class TestJsr223 {
        private LatteEngineFactory factory;
        private LatteEngine engine;
        private LatteScope engineScope;

        @Before
        public void setUp() throws Exception {
                factory = new LatteEngineFactory();
                engine = (LatteEngine) new ScriptEngineManager().getEngineByName("Latte-lang");
                engineScope = (LatteScope) engine.getBindings(ScriptContext.ENGINE_SCOPE);
        }

        @Test
        public void testFactoryGetPrintStatement() throws Exception {
                String res = factory.getOutputStatement("hello world");
                assertEquals("println(\"hello world\")", res);
                res = factory.getOutputStatement("\"");
                assertEquals("println(\"\\\"\")", res);
        }

        @Test
        public void testFactoryGetMethodInvocation() throws Exception {
                String res = factory.getMethodCallSyntax("a", "m", "arg1", "arg2");
                assertEquals("a.`m`(arg1,arg2)", res);
        }

        @Test
        public void testScopeAfterEvalNonAssignmentExp() throws Exception {
                assertEquals(0, engineScope.size());
                assertEquals(4, engine.eval("1 + 3"));
                assertEquals(5, engineScope.size());
                assertEquals(Collections.emptyList(), engineScope.get("$latte.scripting.imports"));
                assertTrue(engineScope.get("$latte.scripting.CL") instanceof CL);
                assertEquals(Collections.emptyList(), engineScope.get("$latte.scripting.methods"));
                assertEquals(1, engineScope.get("$latte.scripting.eval_count"));
                assertEquals(0, engineScope.get("$latte.scripting.res_count"));
        }

        @Test
        public void testScopeAfterEvalAssignment() throws Exception {
                assertEquals(4, engine.eval("a = 1 + 3"));
                assertNull(engineScope.get("$latte.scripting.res_count"));
                assertEquals(4, engineScope.get("a"));
        }

        @Test
        public void testScopeAfterMethodDef() throws Exception {
                assertNull(engine.eval("def method = 1"));
                @SuppressWarnings("unchecked")
                List<MethodDef> methods = (List<MethodDef>) engineScope.get("$latte.scripting.methods");
                assertEquals(1, methods.size());
                MethodDef methodDef = methods.get(0);
                assertEquals(new MethodDef("method", Collections.singleton(new Modifier(Modifier.Available.DEF, LineCol.SYNTHETIC)),
                        null, Collections.<VariableDef>emptyList(), Collections.<AST.Anno>emptySet(),
                        Collections.<Statement>singletonList(new AST.Return(new NumberLiteral("1", LineCol.SYNTHETIC), LineCol.SYNTHETIC)),
                        LineCol.SYNTHETIC), methodDef);
        }

        @Test
        public void testBindings() throws Exception {
                Object a = engineScope.putNew("a", 1, int.class);
                assertNull(a);
                assertEquals(1, engineScope.get("a"));
                try {
                        engineScope.put("a", 2.1);
                        fail();
                } catch (IllegalArgumentException ignore) {
                        try {
                                engineScope.put("a", null);
                                fail();
                        } catch (NullPointerException ignore2) {
                        }
                }
        }
}
