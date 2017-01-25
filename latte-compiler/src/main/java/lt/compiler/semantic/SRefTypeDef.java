package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * reference types
 */
public abstract class SRefTypeDef extends STypeDef {
        private final List<SFieldDef> fields = new ArrayList<>();
        private final List<SMethodDef> methods = new ArrayList<>();
        private final List<SModifier> modifiers = new ArrayList<>();

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
