package lt.compiler.semantic;

/**
 * constantValue,which can be put into constant pool
 */
public interface ConstantValue extends Value {
        byte[] getByte();
}
