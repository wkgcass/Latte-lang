package lt.compiler.cases;

import lt.compiler.ErrorManager;
import lt.compiler.Properties;
import lt.compiler.ScannerSwitcher;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.StringReader;

/**
 * test scanner switcher
 */
public class TestScannerSwitcher {
        @Test
        public void test1() throws Exception {
                ScannerSwitcher s1 = new ScannerSwitcher("test", new StringReader(
                        "" +
                                "/// :scanner-brace\n" +
                                "class A(a, public b):B(a) {\n" +
                                "protected x=1\n" +
                                "}"
                ), new Properties(), new ErrorManager(true));
                ScannerSwitcher s2 = new ScannerSwitcher("test", new StringReader(
                        "" +
                                "class A(a, public b):B(a)\n" +
                                "    protected x=1"
                ), new Properties(), new ErrorManager(true));
                assertTrue(s1.scan().equalsIgnoreIndent(s2.scan()));
        }

        @Test
        public void testMap() throws Exception {
                ScannerSwitcher s1 = new ScannerSwitcher("test", new StringReader(
                        "" +
                                "/// :scanner-brace\n" +
                                "[\"a\":1\n" +
                                "\"b\":2,\"c\":3\n" +
                                "\"d\":4]"
                ), new Properties(), new ErrorManager(true));
                ScannerSwitcher s2 = new ScannerSwitcher("test", new StringReader(
                        "" +
                                "[\n" +
                                "    \"a\":1\n" +
                                "    \"b\":2,\n" +
                                "    \"c\":3\n" +
                                "    \"d\":4\n" +
                                "]"
                ), new Properties(), new ErrorManager(true));

                assertTrue(s1.scan().equalsIgnoreIndent(s2.scan()));
        }
}
