package lt.compiler.cases;

import lt.RepeatRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;

import static lt.compiler.cases.TestCodeGen.retrieveClass;
import static org.junit.Assert.assertTrue;

/**
 * synchronized
 */
public class TestSynchronized {
        @Rule
        public RepeatRule repeatRule = new RepeatRule();
        private static Class<?> testSynchronizedCls;
        private static Class<?> testSynchronizedReturnCls;

        @BeforeClass
        public static void classSetUp() throws Exception {
                testSynchronizedCls = retrieveClass(
                        "" +
                                "class TestAnnotation\n" +
                                "    static\n" +
                                "        method(a,b,c):Unit\n" +
                                "            synchronized(a,b)\n" +
                                "                c.i=1\n" +
                                "                Thread.sleep(200)\n" +
                                "                a.i+=1\n" +
                                "                b.i+=2",
                        "TestAnnotation");
                testSynchronizedReturnCls = retrieveClass(
                        "" +
                                "class TestSynchronizedReturn\n" +
                                "    static\n" +
                                "        def method(a,b,c)\n" +
                                "            synchronized(a,b)\n" +
                                "                c.i=1\n" +
                                "                Thread.sleep(200)\n" +
                                "                a.i+=1\n" +
                                "                b.i+=2\n" +
                                "                return 10",
                        "TestSynchronizedReturn");
        }

        @Test
        @RepeatRule.Repeat(times = 20)
        public void testSynchronized() throws Exception {
                Class<?> cls = testSynchronizedCls;
                class Container {
                        public int i = 0;
                }
                final Method method = cls.getMethod("method", Object.class, Object.class, Object.class);
                final Container a = new Container();
                final Container b = new Container();
                final Container c = new Container();
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                                try {
                                        method.invoke(null, a, b, c);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                }
                        }
                }).start();

                while (c.i == 0) {
                        Thread.sleep(1);
                }

                class Result {
                        boolean pass1 = false;
                        boolean pass2 = false;
                }
                final Result result = new Result();

                Thread t1 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                                synchronized (a) {
                                        if (1 == a.i) result.pass1 = true;
                                }
                        }
                });
                t1.start();
                Thread t2;
                t2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                                synchronized (b) {
                                        if (2 == b.i) result.pass2 = true;
                                }
                        }
                });
                t2.start();
                t1.join();
                t2.join();
                assertTrue(result.pass1);
                assertTrue(result.pass2);
        }

        @Test
        @RepeatRule.Repeat(times = 20)
        public void testSynchronizedReturn() throws Exception {
                Class<?> cls = testSynchronizedReturnCls;
                class Container {
                        public int i = 0;
                }
                final Method method = cls.getMethod("method", Object.class, Object.class, Object.class);
                final Container a = new Container();
                final Container b = new Container();
                final Container c = new Container();
                class Result {
                        boolean result = false;
                        boolean pass1 = false;
                        boolean pass2 = false;
                }
                final Result result = new Result();
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                                try {
                                        Object res = method.invoke(null, a, b, c);
                                        if (res.equals(10)) result.result = true;
                                        c.i = 2;
                                } catch (Exception e) {
                                        e.printStackTrace();
                                }
                        }
                }).start();

                while (c.i == 0) {
                        Thread.sleep(1);
                }

                Thread t1 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                                synchronized (a) {
                                        if (1 == a.i) result.pass1 = true;
                                }
                        }
                });
                t1.start();
                Thread t2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                                synchronized (b) {
                                        if (2 == b.i) result.pass2 = true;
                                }
                        }
                });
                t2.start();
                t1.join();
                t2.join();

                while (c.i == 1) {
                        Thread.sleep(1);
                }

                assertTrue(result.result);
                assertTrue(result.pass1);
                assertTrue(result.pass2);
        }
}
