package lt.compiler.semantic;

import java.util.HashSet;
import java.util.Set;

/**
 * meta data about invokable
 */
public class InvokableMeta {
        public final Set<LeftValue> pointerLocalVar = new HashSet<LeftValue>();
}
