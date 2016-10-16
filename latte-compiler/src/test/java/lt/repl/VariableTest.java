package lt.repl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * tests for pointers
 */
public class VariableTest {
        Evaluator evaluator;

        @Before
        public void setUp() throws Exception {
                evaluator = new Evaluator(new ClassPathLoader(ClassLoader.getSystemClassLoader()));

        }

        @Test
        public void testInvoke() throws Exception {
                Object result = evaluator.eval("" +
                        "def method()\n" +
                        "    i:lt::lang::function::Function0=()->1\n" +
                        "    return i()\n" +
                        "method()"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testAccessField() throws Exception {
                Object result = evaluator.eval("" +
                        "class X(public num)\n" +
                        "def method()\n" +
                        "    i:X = X(1)\n" +
                        "    return i.num\n" +
                        "method()"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testAccessMethod() throws Exception {
                Object result = evaluator.eval("" +
                        "data class X(num)\n" +
                        "def method()\n" +
                        "    i:X = X(1)\n" +
                        "    return i.num\n" +
                        "method()"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testLambdaChangeValue() throws Exception {
                Object result = evaluator.eval("" +
                        "def method()\n" +
                        "    i:int = 1\n" +
                        "    f=()->\n" +
                        "        i=2\n" +
                        "    f()\n" +
                        "    return i\n" +
                        "method()").result;
                assertEquals(2, result);
        }

        @Test
        public void testLambdaGetValue() throws Exception {
                Object result = evaluator.eval("" +
                        "def method()\n" +
                        "    i:int = 1\n" +
                        "    return (()->i)()\n" +
                        "method()"
                ).result;
                assertEquals(1, result);
        }

        @Test
        public void testArrayPointer() throws Exception {
                Object result = evaluator.eval("" +
                        "def method()\n" +
                        "    i:[]int = [1,2]\n" +
                        "    return i[0]\n" +
                        "method()").result;
                assertEquals(1, result);
        }

        @Test
        public void test2dArrayPointer() throws Exception {
                Object result = evaluator.eval("" +
                        "def method()\n" +
                        "    i:[][]int = [[1,2],[3,4]]\n" +
                        "    return i[1, 0]\n" +
                        "method()").result;
                assertEquals(3, result);
        }
}
