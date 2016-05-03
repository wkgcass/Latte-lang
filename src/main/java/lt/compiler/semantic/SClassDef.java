package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * class definition
 */
public class SClassDef extends STypeDef {
        private final List<SModifier> modifiers = new ArrayList<>();
        private final List<SFieldDef> fields = new ArrayList<>();
        private final List<SConstructorDef> constructors = new ArrayList<>();
        private final List<SMethodDef> methods = new ArrayList<>();
        private SClassDef parent;
        private final List<SInterfaceDef> superInterfaces = new ArrayList<>();
        private final List<Instruction> staticStatements = new ArrayList<>();
        private final List<ExceptionTable> staticExceptionTable = new ArrayList<>();

        public SClassDef(LineCol lineCol) {
                super(lineCol);
        }

        public void setParent(SClassDef parent) {
                this.parent = parent;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }

        public List<SFieldDef> fields() {
                return fields;
        }

        public List<SConstructorDef> constructors() {
                return constructors;
        }

        public List<SMethodDef> methods() {
                return methods;
        }

        public SClassDef parent() {
                return parent;
        }

        public List<SInterfaceDef> superInterfaces() {
                return superInterfaces;
        }

        public List<Instruction> staticStatements() {
                return staticStatements;
        }

        public List<ExceptionTable> staticExceptionTable() {
                return staticExceptionTable;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                for (SModifier m : modifiers()) {
                        sb.append(m.toString().toLowerCase()).append(" ");
                }
                sb.append("class ").append(fullName());
                if (parent() != null) sb.append(" extends ").append(parent().fullName());
                if (!superInterfaces().isEmpty())
                        sb.append(" implements ");
                boolean isFirst = true;
                for (SInterfaceDef i : superInterfaces()) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(i.fullName());
                }
                return sb.toString();
        }

        @Override
        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                if (cls instanceof SClassDef) {
                        while (cls != null) {
                                if (cls.equals(this)) return true;
                                cls = ((SClassDef) cls).parent();
                        }
                        return false;
                }
                return false;
        }
}
