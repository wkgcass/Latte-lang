package lt.test;

import org.junit.*;
import org.junit.Assert;

import java.io.File;

/**
 * test the latte-test module
 */
public class TestTest {
        private static TestContent testContent;

        @BeforeClass
        public static void setUpClass() throws Exception {
                String cls = "lt.test.TestContentImpl";
                ClassLoader cl = LatteTest.compileIfNotExist(cls, new File("./src/test/latte/"));
                testContent = (TestContent) cl.loadClass(cls).newInstance();
        }

        @Test
        public void testFail() {
                try {
                        testContent.testFail();
                        Assert.fail();
                } catch (AssertionError e) {
                        Assert.assertEquals("testFail", e.getMessage());
                }
        }

        @Test
        public void testSuccess() {
                testContent.testSuccess();
        }

        @Test
        public void testTimeout() {
                try {
                        testContent.testTimeout();
                        Assert.fail();
                } catch (AssertionError e) {
                        Assert.assertEquals("timeout reached! 1000 ms", e.getMessage());
                }
        }

        @Test
        public void testJunit() {
                testContent.testJunit();
        }
}
