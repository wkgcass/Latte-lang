package lt.lang.implicit;

import lt.runtime.Implicit;
import lt.runtime.LatteObject;

/**
 * implicit casts for primitives
 */
@Implicit
@LatteObject
public class PrimitivesImplicit {
        public static final PrimitivesImplicit singletonInstance = new PrimitivesImplicit();

        private PrimitivesImplicit() {
        }

        @Implicit
        public final RichBool cast(Boolean b) {
                return new RichBool(b);
        }

        @Implicit
        public final RichByte cast(Byte b) {
                return new RichByte(b);
        }

        @Implicit
        public final RichChar cast(Character c) {
                return new RichChar(c);
        }

        @Implicit
        public final RichDouble cast(Double d) {
                return new RichDouble(d);
        }

        @Implicit
        public final RichFloat cast(Float f) {
                return new RichFloat(f);
        }

        @Implicit
        public final RichInt cast(Integer i) {
                return new RichInt(i);
        }

        @Implicit
        public final RichLong cast(Long l) {
                return new RichLong(l);
        }

        @Implicit
        public final RichShort cast(Short s) {
                return new RichShort(s);
        }
}
