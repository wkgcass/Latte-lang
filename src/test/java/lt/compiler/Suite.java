package lt.compiler;

import junit.framework.TestSuite;
import lt.compiler.cases.*;
import lt.repl.TestEvaluator;
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
        TestDemo.class,
        TestEvaluator.class
})
public class Suite extends TestSuite {
}
