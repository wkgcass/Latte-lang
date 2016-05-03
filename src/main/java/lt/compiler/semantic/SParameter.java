package lt.compiler.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * parameter definition
 */
public class SParameter implements LeftValue, SAnnotationPresentable {
        private String name;
        private final List<SAnno> annos = new ArrayList<>();
        private STypeDef type;
        private SInvokable target;
        private boolean canChange = true;

        public void setTarget(SInvokable target) {
                this.target = target;
        }

        public void setName(String name) {
                this.name = name;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        public void setCanChange(boolean canChange) {
                this.canChange = canChange;
        }

        public String name() {
                return name;
        }

        @Override
        public boolean canChange() {
                return canChange;
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public SInvokable target() {
                return target;
        }

        @Override
        public String toString() {
                String str = "";
                if (!canChange()) str += "final ";
                str += type().fullName() + " " + name();
                return str;
        }
}
