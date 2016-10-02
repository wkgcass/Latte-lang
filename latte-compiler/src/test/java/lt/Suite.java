/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt;

import junit.framework.TestSuite;
import lt.compiler.cases.*;
import lt.compiler.err_rec.TestParserErrorRecovery;
import lt.compiler.err_rec.TestScannerErrorRecovery;
import lt.generator.TestJsSupport;
import lt.repl.TestBugsInEval;
import lt.repl.TestEvaluator;
import lt.repl.TestPointer;
import lt.repl.TestScript;
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
        TestEvaluator.class,
        TestBugsInEval.class,
        TestScannerErrorRecovery.class,
        TestParserErrorRecovery.class,
        TestScript.class,
        TestAnnotations.class,
        TestJsSupport.class,
        TestBraceScanner.class,
        TestScannerSwitcher.class,
        TestPointer.class
})
public class Suite extends TestSuite {
}
