package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * constructor definition
 */
public class SConstructorDef extends SInvokable {
        public SConstructorDef(LineCol lineCol) {
                super(lineCol);
                setReturnType(VoidType.get());
        }
}
