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

package lt.compiler.library;

import lt.repl.Compiler;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * test libraries
 */
public class TestLibraries {
        public static ClassLoader load(Map<String, Object> map) throws Exception {
                Compiler compiler = new Compiler();
                return compiler.compile(map);
        }

        @Test
        public void testHtml() throws Exception {
                ClassLoader loader = load(new HashMap<String, Object>() {{
                        try {
                                ClassLoader.getSystemClassLoader().loadClass("lt.html.HTMLElement");
                        } catch (ClassNotFoundException ignore) {
                                put("html.lt", TestLibraries.class.getResourceAsStream("/lt/html.lt"));
                        }
                        put("test_html.lt", TestLibraries.class.getResourceAsStream("/test_libraries/test_html.lt"));
                }});
                Class<?> TestHtml = loader.loadClass("lt.html.test.TestHtml");
                Class<?> Html = loader.loadClass("lt.html.Html");
                Object html = Html.newInstance(); // html=Html()

                Method testConstructHtml = TestHtml.getMethod("testConstructHtml");

                assertEquals(html, testConstructHtml.invoke(null));

                Method testHtmlFormat = TestHtml.getMethod("testHtmlFormat");
                assertEquals("" +
                        "<html>" +
                        "<head>" +
                        "</head>" +
                        "<body>" +
                        "<form>" +
                        "<input type='text' value='hello world'>" +
                        "</form>" +
                        "</body>" +
                        "</html>", testHtmlFormat.invoke(null).toString());
        }

        @Test
        public void testSQL() throws Exception {
                ClassLoader loader = load(new HashMap<String, Object>() {{
                        try {
                                ClassLoader.getSystemClassLoader().loadClass("lt.sql.DB");
                        } catch (ClassNotFoundException ignore) {
                                put("sql.lt", TestLibraries.class.getResourceAsStream("/lt/sql.lt"));
                        }
                        put("test_sql.lt", TestLibraries.class.getResourceAsStream("/test_libraries/test_sql.lt"));
                }});
                Class<?> TestSQL = loader.loadClass("lt.sql.test.TestSQL");
                Method testSelectString = TestSQL.getMethod("testSelectString");
                assertEquals("select `User`.`id`, `User`.`name` from `User` where `User`.`id` > ?1 order by `User`.`id` desc limit ?2,?3",
                        testSelectString.invoke(null));
                Method testSelectWithSubQuery = TestSQL.getMethod("testSelectWithSubQuery");
                assertEquals("select `User`.`id`, `User`.`name` from `User` where `User`.`id` <= " +
                                "(select `User`.`id` from `User` limit ?1) order by `User`.`id` desc",
                        testSelectWithSubQuery.invoke(null));
                Method testInsertString = TestSQL.getMethod("testInsertString");
                assertEquals("insert into `User` (`User`.`id`, `User`.`name`) values (?1, ?2)",
                        testInsertString.invoke(null));
                Method testUpdateString = TestSQL.getMethod("testUpdateString");
                assertEquals("update `User` set `User`.`id` = ?1 where `User`.`name` == ?2",
                        testUpdateString.invoke(null));
                Method testDeleteString = TestSQL.getMethod("testDeleteString");
                assertEquals("delete from `User` where `User`.`id` == ?1",
                        testDeleteString.invoke(null));
                Method testDBUpdate = TestSQL.getMethod("testDBUpdate");
                // System.out.println(testDBUpdate.invoke(null));
                // Method testManipDbQuery = TestSQL.getMethod("testManipDbQuery");
        }
}
