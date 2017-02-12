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

package lt.library;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * test libraries
 */
public class TestLibraries {
        @Test
        public void testHtml() throws Exception {
                Class<?> TestHtml = Class.forName("lt.dsl.html.test.TestHtml");
                Class<?> Html = Class.forName("lt.dsl.html.html");
                Object html = Html.newInstance(); // html=html()

                Method testConstructHtml = TestHtml.getMethod("testConstructHtml");

                assertEquals(html, testConstructHtml.invoke(null));

                Method testHtmlFormat = TestHtml.getMethod("testHtmlFormat");
                assertEquals("" +
                        "<html>" +
                        "<head>" +
                        "</head>" +
                        "<body>" +
                        "<form>" +
                        "<input value=\"hello world\" type=\"text\">" +
                        "</form>" +
                        "</body>" +
                        "</html>", testHtmlFormat.invoke(null).toString());

                Method testHtmlEscape = TestHtml.getMethod("testHtmlEscape");
                assertEquals("" +
                                "<html onload=\"window.location.href=&quot;page&quot;\">" +
                                "&nbsp;&gt;&lt;&amp;&quot;" +
                                "</html>"
                        , testHtmlEscape.invoke(null).toString());

                Method testHtmlPretty = TestHtml.getMethod("testHtmlPretty");
                assertEquals("" +
                        "<html>\n" +
                        "  <head>\n" +
                        "    <meta http-equiv=\"Pragma\" content=\"no-cache\">\n" +
                        "    <link rel=\"stylesheet\" src=\"style.css\">\n" +
                        "    <script src=\"x.js\">\n" +
                        "    </script>\n" +
                        "  </head>\n" +
                        "  <body>\n" +
                        "    <form method=\"post\" action=\"x.do\">\n" +
                        "      <input name=\"x\" type=\"text\">\n" +
                        "      1\n" +
                        "      <button type=\"submit\">\n" +
                        "        Submit\n" +
                        "      </button>\n" +
                        "    </form>\n" +
                        "  </body>\n" +
                        "</html>", testHtmlPretty.invoke(null));

                Method testCss = TestHtml.getMethod("testCss");
                assertEquals("" +
                        "body {\n" +
                        "  background-color : #123456;\n" +
                        "}", testCss.invoke(null));

                Method testHtmlAndCss = TestHtml.getMethod("testHtmlAndCss");
                assertEquals("" +
                        "<html>\n" +
                        "  <style>\n" +
                        "    body {\n" +
                        "      background-color : red;\n" +
                        "    }\n" +
                        "  </style>\n" +
                        "</html>", testHtmlAndCss.invoke(null));

                Method testHtmlClass = TestHtml.getMethod("testHtmlClass");
                assertEquals(
                        "<button class=\"btn btn-default\" type=\"submit\"></button>", testHtmlClass.invoke(null)
                );
        }

        @Test
        public void testAsync_waterfall() throws Exception {
                Class<?> TestAsync = Class.forName("lt.async.test.TestAsync");

                Method testWaterfall = TestAsync.getMethod("testWaterfall");
                assertEquals(Arrays.asList(1, 2, 3), testWaterfall.invoke(null));
        }

        @Test
        public void testAsync_parallel() throws Exception {
                Class<?> TestAsync = Class.forName("lt.async.test.TestAsync");

                Method testParallel = TestAsync.getMethod("testParallel");
                assertEquals(Arrays.asList(1, 2, 3), testParallel.invoke(null));
        }

        @Test
        public void testAsync_series() throws Exception {
                Class<?> TestAsync = Class.forName("lt.async.test.TestAsync");

                Method testSeries = TestAsync.getMethod("testSeries");
                assertEquals(Arrays.asList(1, 2, 3), testSeries.invoke(null));
        }

        @Test
        public void testAsync_each() throws Exception {
                Class<?> TestAsync = Class.forName("lt.async.test.TestAsync");

                Method testEach = TestAsync.getMethod("testEach");
                assertEquals(Arrays.asList(1, 2, 3), testEach.invoke(null));
        }

        @Test
        public void testAsync_eachSeries() throws Exception {
                Class<?> TestAsync = Class.forName("lt.async.test.TestAsync");

                Method testEachSeries = TestAsync.getMethod("testEachSeries");
                assertEquals(Arrays.asList(1, 2, 3), testEachSeries.invoke(null));
        }
}
