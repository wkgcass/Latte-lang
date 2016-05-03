package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * invokable
 */
abstract public class SInvokable extends SMember {
        private final List<SParameter> parameters = new ArrayList<>();
        private STypeDef returnType;
        private final List<Instruction> statements = new ArrayList<>();
        private final List<ExceptionTable> exceptionTables = new ArrayList<>();

        public SInvokable(LineCol lineCol) {
                super(lineCol);
        }

        public void setReturnType(STypeDef returnType) {
                this.returnType = returnType;
        }

        public List<SParameter> getParameters() {
                return parameters;
        }

        public STypeDef getReturnType() {
                return returnType;
        }

        public List<Instruction> statements() {
                return statements;
        }

        public List<ExceptionTable> exceptionTables() {
                return exceptionTables;
        }
}
