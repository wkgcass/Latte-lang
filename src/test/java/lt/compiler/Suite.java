package lt.compiler;

import junit.framework.TestSuite;
import lt.compiler.cases.*;
import org.junit.runner.RunWith;

/**
 * test suite
 */
@RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
        TestScanner.class,
        TestParser.class,
        TestParserMix.class,
        TestSemantic.class,
        TestCodeGen.class,
        TestLang.class,
        TestDemo.class
})
public class Suite extends TestSuite {
}
