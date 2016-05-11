package lt.compiler.err_rec;

import lt.compiler.*;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * parser error recovery
 */
public class TestParserErrorRecovery {
        private List<Statement> parse(String code, ErrorManager err) throws IOException, SyntaxException {
                Scanner scanner = new Scanner("test.lt", new StringReader(code), new Scanner.Properties(), err);
                Parser parser = new Parser(scanner.scan(), err);
                return parser.parse();
        }

        @Test
        public void testNextNodeNotNull() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                        "1+,2"
                /*         ^Unexpected End */
                        , err);
                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(3, err.errorList.get(0).lineCol.column);

                assertEquals(1, statements.size());
                assertEquals(
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC), LineCol.SYNTHETIC),
                        statements.get(0)
                );
        }
}
