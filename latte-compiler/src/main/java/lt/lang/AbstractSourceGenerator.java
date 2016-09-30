package lt.lang;

import lt.compiler.*;
import lt.compiler.syntactic.Statement;
import lt.generator.SourceGenerator;

import java.util.List;

/**
 * the generator that save all init params to fields.
 */
public abstract class AbstractSourceGenerator implements SourceGenerator {
        protected List<Statement> ast;
        protected SemanticProcessor processor;
        protected SemanticScope scope;
        protected LineCol lineCol;
        protected ErrorManager err;

        @Override
        public void init(List<Statement> ast, SemanticProcessor processor, SemanticScope scope, LineCol lineCol, ErrorManager err) throws SyntaxException {
                this.ast = ast;
                this.processor = processor;
                this.scope = scope;
                this.lineCol = lineCol;
                this.err = err;
        }
}
