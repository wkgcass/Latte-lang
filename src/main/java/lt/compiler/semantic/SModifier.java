package lt.compiler.semantic;

/**
 * modifier
 */
public enum SModifier {
        PUBLIC(0x0001),
        PRIVATE(0x0002),
        PROTECTED(0x0004),
        STATIC(0x0008),
        FINAL(0x0010),
        VOLATILE(0x0040),
        TRANSIENT(0x0080),
        ABSTRACT(0x0400),
        SYNTHETIC(0X1000),
        ENUM(0x4000),
        NATIVE(0x0100),
        STRICT(0x0800),
        SYNCHRONIZED(0x0020);

        public final int flag;

        SModifier(int flag) {
                this.flag = flag;
        }
}
