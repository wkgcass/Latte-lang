package lt.compiler.semantic.builtin;

import lt.compiler.LineCol;
import lt.compiler.semantic.SAnno;
import lt.compiler.semantic.STypeDef;

import java.util.Collections;
import java.util.List;

/**
 * null type
 */
public class NullTypeDef extends STypeDef {
        private NullTypeDef() {
                super(LineCol.SYNTHETIC);
        }

        private static NullTypeDef t = new NullTypeDef();

        public static NullTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "null";
        }

        @Override
        public List<SAnno> annos() {
                return Collections.emptyList();
        }
}
