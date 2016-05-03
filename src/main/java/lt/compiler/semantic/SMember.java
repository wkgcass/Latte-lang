package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * define a member
 */
abstract public class SMember implements SAnnotationPresentable {
        private final List<SModifier> modifiers = new ArrayList<>();
        private STypeDef declaringType;
        private final List<SAnno> annos = new ArrayList<>();
        private LineCol lineCol;

        public SMember(LineCol lineCol) {
                this.lineCol = lineCol;
        }

        public void setDeclaringType(STypeDef declaringType) {
                this.declaringType = declaringType;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }

        public STypeDef declaringType() {
                return declaringType;
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        public LineCol line_col() {
                return lineCol;
        }
}
