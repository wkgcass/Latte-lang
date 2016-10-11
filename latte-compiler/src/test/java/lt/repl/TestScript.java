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

package lt.repl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * script
 */
public class TestScript {
        ScriptCompiler scriptCompiler;

        @Before
        public void setUp() throws Exception {
                scriptCompiler = new ScriptCompiler(ClassLoader.getSystemClassLoader());
        }

        @Test
        public void testSimpleScript() throws Throwable {
                ScriptCompiler.Script script = scriptCompiler.compile("script", "return 1");
                assertEquals(1, script.run().getResult());
        }

        @Test
        public void testScriptArgs() throws Throwable {
                ScriptCompiler.Script script = scriptCompiler.compile("script", "return args");
                assertArrayEquals(new String[0], (Object[]) script.run().getResult());
                String[] args = new String[]{"a", "b", "c"};
                assertArrayEquals(args, (Object[]) script.run(args).getResult());
        }

        @Test
        public void testStaticMethod() throws Throwable {
                assertEquals(3,
                        scriptCompiler.compile("yy", "" +
                                "import lt::repl::TestStaticMethod\n" +
                                "a = 2\n" +
                                "return TestStaticMethod.x(a)"
                        ).run().getResult()
                );
        }

        @Test
        public void testInternalLambdaBug() throws Throwable {
                String code = "" +
                        "[1, 2, 3, 4].stream.\n" +
                        "filter |-it > 2-|.\n" +
                        "map |-it + 1-|\n" +
                        "\n" +
                        "a = 1\n" +
                        "b = 2\n" +
                        "val result = (if a>b |-1-| else |-2-|)\n" +
                        "return result";
                assertEquals(2, scriptCompiler.compile("testInternalLambdaBug", code).run().getResult());
        }
}
