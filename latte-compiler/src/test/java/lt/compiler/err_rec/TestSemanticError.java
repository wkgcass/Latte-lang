package lt.compiler.err_rec;

import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TestSemanticError {
        private Set<STypeDef> get(String code) throws IOException, SyntaxException {
                ErrorManager err = new ErrorManager(true);
                IndentScanner scanner = new IndentScanner("test.lt", new StringReader(code), new Properties(), err);
                Parser parser = new Parser(scanner.scan(), err);
                SemanticProcessor semanticProcessor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                        put("test.lt", parser.parse());
                }}, Thread.currentThread().getContextClassLoader(), err);
                return semanticProcessor.parse();
        }

        @Test
        public void testFailWhenNoMethod() throws Exception {
                try {
                        get("" +
                                "package test\n" +
                                "class A\n" +
                                "    method()\n" +
                                "        inner():Unit");
                } catch (Throwable t) {
                        t.printStackTrace();
                        fail();
                }
        }

        @Test
        public void testImportFail() throws Exception {
                try {
                        get("import java::xyz::_");
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testImplicitFail() throws Exception {
                try {
                        get("" +
                                "@Implicit\n" +
                                "class X"
                        );
                        fail();
                } catch (SyntaxException ignore) {
                }

                try {
                        get("" +
                                "@Implicit\n" +
                                "object X\n" +
                                "    @Implicit\n" +
                                "    def m"
                        );
                        fail();
                } catch (SyntaxException ignore) {
                }

                try {
                        get("" +
                                "@Implicit\n" +
                                "object X\n" +
                                "    @Implicit\n" +
                                "    def m(o):Unit"
                        );
                        fail();
                } catch (SyntaxException ignore) {
                }

                // implicit

                try {
                        get("" +
                                "implicit object X\n" +
                                "    implicit def m"
                        );
                        fail();
                } catch (SyntaxException ignore) {
                }

                try {
                        get("" +
                                "implicit object X\n" +
                                "    implicit def m(o):Unit"
                        );
                        fail();
                } catch (SyntaxException ignore) {
                }
        }
}
