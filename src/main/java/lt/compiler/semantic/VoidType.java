package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.Collections;
import java.util.List;

/**
 * void type
 */
public class VoidType extends STypeDef {
        private VoidType() {
                super(LineCol.SYNTHETIC);
        }

        private static VoidType v = new VoidType();

        public static VoidType get() {
                return v;
        }

        @Override
        public String fullName() {
                return "void";
        }

        @Override
        public List<SAnno> annos() {
                return Collections.emptyList();
        }
}
