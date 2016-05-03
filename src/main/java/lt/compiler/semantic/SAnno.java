package lt.compiler.semantic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * annotation
 */
public class SAnno implements Value {
        private SAnnoDef annoDef;
        private final Map<SAnnoField, Value> valueMap = new LinkedHashMap<>();
        private final Map<String, Object> alreadyCompiledAnnotationValueMap = new LinkedHashMap<>();
        private SAnnotationPresentable present;

        public void setAnnoDef(SAnnoDef annoDef) {
                this.annoDef = annoDef;
        }

        public void setPresent(SAnnotationPresentable present) {
                this.present = present;
        }

        @Override
        public SAnnoDef type() {
                return annoDef;
        }

        public Map<SAnnoField, Value> values() {
                return valueMap;
        }

        public SAnnotationPresentable present() {
                return present;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(type().fullName()).append("(");
                boolean isFirst = true;
                for (SAnnoField f : valueMap.keySet()) {
                        Value v = valueMap.get(f);
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(f.name()).append("=").append(v);
                }
                sb.append(")");
                return sb.toString();
        }

        public Map<String, Object> alreadyCompiledAnnotationValueMap() {
                return alreadyCompiledAnnotationValueMap;
        }
}
