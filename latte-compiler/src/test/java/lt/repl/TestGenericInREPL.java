package lt.repl;

import lt.compiler.util.Consts;
import lt.lang.GenericTemplate;
import lt.repl.scripting.EvalEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestGenericInREPL {
        Evaluator evaluator;

        @Before
        public void setUp() throws Exception {
                evaluator = new Evaluator(new ClassPathLoader(Thread.currentThread().getContextClassLoader()));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testGetGenericASTFromCompiledClasses() throws Exception {
                for (String x : Arrays.asList("class", "interface", "object")) {
                        evaluator = new Evaluator(new ClassPathLoader(Thread.currentThread().getContextClassLoader()));

                        EvalEntry entry = evaluator.eval("" +
                                x + " A<:T:>");
                        List<Class<?>> definedClasses = (List<Class<?>>) entry.result;
                        assertEquals(1, definedClasses.size());
                        Class<?> templateA = definedClasses.get(0);
                        assertEquals("A", templateA.getName());
                        assertTrue(templateA.isAnnotationPresent(GenericTemplate.class));

                        assertTrue(templateA.getField(Consts.AST_FIELD).get(null) instanceof String);

                        entry = evaluator.eval("" +
                                "type A<:int:>");
                        Class<?> typeA = (Class<?>) entry.result;
                        assertEquals("A" + Consts.GENERIC_NAME_SPLIT + "int", typeA.getName());

                        if (x.equals("class")) {
                                assertTrue(typeA.toString().startsWith("class"));
                        } else if (x.equals("interface")) {
                                assertTrue(typeA.isInterface());
                        } else /* if (x.equals("object") */ {
                                assertTrue(typeA.toString().startsWith("class"));
                        }
                }
        }
}
