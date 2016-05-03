package lt.compiler.semantic;

import lt.compiler.LineCol;
import lt.compiler.LtBug;
import lt.compiler.SyntaxException;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * annotation definition
 */
public class SAnnoDef extends STypeDef {
        private final List<SAnnoField> annoFields = new ArrayList<>();
        private final List<SModifier> modifiers = new ArrayList<>();

        public SAnnoDef() {
                // annotations cannot be defined in LessTyping
                super(LineCol.SYNTHETIC);
        }

        public List<SAnnoField> annoFields() {
                return annoFields;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }

        public boolean canPresentOn(ElementType type) throws SyntaxException {
                String name = fullName();
                try {
                        Class<?> cls = Class.forName(name);
                        Annotation[] annotations = cls.getAnnotations();
                        for (Annotation a : annotations) {
                                if (a instanceof Target) {
                                        Target target = (Target) a;
                                        ElementType[] types = target.value();
                                        for (ElementType t : types) {
                                                if (t.equals(type)) return true;
                                        }
                                        return false;
                                }
                        }
                        return true;
                } catch (ClassNotFoundException e) {
                        throw new LtBug(e);
                }
        }
}
