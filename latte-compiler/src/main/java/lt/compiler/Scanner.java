package lt.compiler;

import lt.compiler.lexical.ElementStartNode;

import java.io.IOException;

/**
 * the scanner interface.
 */
public interface Scanner {
        ElementStartNode scan() throws IOException, SyntaxException;
}
