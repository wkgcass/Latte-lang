package lt.compiler.semantic;

import lt.compiler.LineCol;
import lt.compiler.semantic.builtin.NullTypeDef;

import java.util.ArrayList;
import java.util.List;

/**
 * type definition
 */
public class STypeDef implements SAnnotationPresentable {
        private String pkg;
        private String fullName;
        private List<SAnno> annos = new ArrayList<>();
        private final LineCol lineCol;

        public STypeDef(LineCol lineCol) {
                this.lineCol = lineCol;
        }

        public void setFullName(String fullName) {
                this.fullName = fullName;
        }

        public void setPkg(String pkg) {
                this.pkg = pkg;
        }

        public String fullName() {
                return fullName;
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        public String pkg() {
                return pkg;
        }

        public boolean isAssignableFrom(STypeDef cls) {
                if (cls == null) throw new NullPointerException();
                if (cls instanceof NullTypeDef)
                        return !(this instanceof PrimitiveTypeDef);
                return cls.equals(this);
        }

        public LineCol line_col() {
                return lineCol;
        }
}
