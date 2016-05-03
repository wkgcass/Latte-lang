package lt.compiler.semantic;

/**
 * exceptionTable
 */
public class ExceptionTable {
        private final Instruction from;
        private final Instruction to;
        private final Instruction target;
        private final STypeDef type;

        public ExceptionTable(Instruction from, Instruction to, Instruction target, STypeDef type) {
                this.from = from;
                this.to = to;
                this.target = target;
                this.type = type;
        }

        public Instruction getTo() {
                return to;
        }

        public Instruction getFrom() {
                return from;
        }

        public Instruction getTarget() {
                return target;
        }

        public STypeDef getType() {
                return type;
        }
}
