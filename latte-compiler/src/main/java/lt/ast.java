package lt;

import lt.compiler.*;
import lt.compiler.syntactic.Statement;
import lt.generator.SourceGenerator;

import java.util.List;

/**
 * serialize the expression as output
 */
public class ast implements SourceGenerator {
        private List<Statement> ast;

        @Override
        public void init(List<Statement> ast, SemanticProcessor processor, SemanticScope scope, LineCol lineCol, ErrorManager err) {
                this.ast = ast;
        }

        @Override
        public Object generate() throws SyntaxException {
                return ast;
        }

        @Override
        public int resultType() {
                return SERIALIZE;
        }
}
