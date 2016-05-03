package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * method definition
 */
public class SMethodDef extends SInvokable {
        private String name;
        private final List<SMethodDef> overRide = new ArrayList<>();
        private final List<SMethodDef> overridden = new ArrayList<>();

        public SMethodDef(LineCol lineCol) {
                super(lineCol);
        }

        public void setName(String name) {
                this.name = name;
        }

        public String name() {
                return name;
        }

        public List<SMethodDef> overRide() {
                return overRide;
        }

        public List<SMethodDef> overridden() {
                return overridden;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                for (SModifier m : modifiers()) {
                        sb.append(m.toString().toLowerCase()).append(" ");
                }
                sb.append(getReturnType().fullName()).append(" ").append(declaringType().fullName()).append(".").append(name).append("(");
                boolean isFirst = true;
                for (SParameter param : getParameters()) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(param);
                }
                sb.append(")");
                return sb.toString();
        }
}
