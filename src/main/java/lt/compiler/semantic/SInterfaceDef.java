package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * interface definition
 */
public class SInterfaceDef extends STypeDef {
        private final List<SFieldDef> fields = new ArrayList<>();
        private final List<SMethodDef> methods = new ArrayList<>();
        private final List<SModifier> modifiers = new ArrayList<>();
        private final List<SInterfaceDef> superInterfaces = new ArrayList<>();

        private final List<Instruction> staticStatements = new ArrayList<>();
        private final List<ExceptionTable> staticExceptionTable = new ArrayList<>();

        public SInterfaceDef(LineCol lineCol) {
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
                sb.append("interface ").append(fullName());
                if (!superInterfaces().isEmpty()) {
                        sb.append(" extends ");
                }
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

        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                Queue<SInterfaceDef> q = new ArrayDeque<>();
                if (cls instanceof SClassDef) {
                        SClassDef tmp = (SClassDef) cls;
                        q.addAll(tmp.superInterfaces().stream().collect(Collectors.toList()));
                } else if (cls instanceof SInterfaceDef) {
                        SInterfaceDef tmp = (SInterfaceDef) cls;
                        q.addAll(tmp.superInterfaces().stream().collect(Collectors.toList()));
                }
                while (!q.isEmpty()) {
                        SInterfaceDef i = q.remove();
                        if (isAssignableFrom(i)) return true;
                        q.addAll(i.superInterfaces().stream().collect(Collectors.toList()));
                }
                return false;
        }
}
