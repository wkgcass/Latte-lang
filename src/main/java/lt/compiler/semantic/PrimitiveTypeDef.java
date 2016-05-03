package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.Collections;
import java.util.List;

/**
 * primitive
 */
public abstract class PrimitiveTypeDef extends STypeDef {

        public PrimitiveTypeDef() {
                super(LineCol.SYNTHETIC);
        }

        @Override
        public List<SAnno> annos() {
                return Collections.emptyList();
        }
}
