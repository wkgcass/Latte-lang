package lt.repl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * tests for pointers
 */
public class TestPointer {
        Evaluator evaluator;

        @Before
        public void setUp() throws Exception {
                evaluator = new Evaluator(new ClassPathLoader(ClassLoader.getSystemClassLoader()));

        }

        @Test
        public void testInvoke() throws Exception {
                Object result = evaluator.eval("" +
                        "i:*lt::lang::function::Function0=()->1\n" +
                        "i()"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testAccessField() throws Exception {
                Object result = evaluator.eval("" +
                        "class X(public num)\n" +
                        "i:*X = X(1)\n" +
                        "i.num"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testAccessMethod() throws Exception {
                Object result = evaluator.eval("" +
                        "data class X(num)\n" +
                        "i:*X = X(1)\n" +
                        "i.num"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testLambdaChangeValue() throws Exception {
                Object result = evaluator.eval("" +
                        "i:*int = 1\n" +
                        "f=()->\n" +
                        "    i=2\n" +
                        "f()\n" +
                        "i").result;
                assertEquals(2, result);
        }

        @Test
        public void testArrayPointer() throws Exception {
                Object result = evaluator.eval("" +
                        "i:*[]int = [1,2]\n" +
                        "i[0]").result;
                assertEquals(1, result);
        }

        @Test
        public void test2dArrayPointer() throws Exception {
                Object result = evaluator.eval("" +
                        "i:*[][]int = [[1,2],[3,4]]\n" +
                        "i[1, 0]").result;
                assertEquals(3, result);
        }
}
