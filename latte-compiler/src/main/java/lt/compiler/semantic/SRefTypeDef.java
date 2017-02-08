package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * reference types
 */
public abstract class SRefTypeDef extends STypeDef {
        private final List<SFieldDef> fields = new ArrayList<SFieldDef>();
        private final List<SMethodDef> methods = new ArrayList<SMethodDef>();
        private final List<SModifier> modifiers = new ArrayList<SModifier>();

        public SRefTypeDef(LineCol lineCol) {
                super(lineCol);
        }

        public List<SFieldDef> fields() {
                return fields;
        }

        public List<SMethodDef> methods() {
                return methods;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }
}
