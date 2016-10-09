package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * pointer type
 */
public class PointerType extends STypeDef {
        private final STypeDef pointingType;

        public PointerType(STypeDef pointingType) {
                super(LineCol.SYNTHETIC);
                this.pointingType = pointingType;
                setFullName("lt.lang.Pointer");
                setPkg("lt.lang");
        }

        public STypeDef getPointingType() {
                return pointingType;
        }

        @Override
        public String toString() {
                return "*" + pointingType.fullName();
        }
}
