package lt.compiler.util;

import lt.compiler.CodeInfo;
import lt.compiler.LtBug;
import lt.compiler.SemanticScope;
import lt.compiler.semantic.*;
import lt.compiler.semantic.builtin.DoubleTypeDef;
import lt.compiler.semantic.builtin.LongTypeDef;
import lt.lang.Pointer;

import java.util.Collection;
import java.util.Iterator;

/**
 * utils for local variables
 */
public class LocalVariables {
        private LocalVariables() {
        }

        private static int _slots(STypeDef type) {
                if (type.equals(LongTypeDef.get()) || type.equals(DoubleTypeDef.get())) {
                        return 2;
                } else {
                        return 1;
                }
        }

        public static int calculateIndexForLocalVariable(LeftValue theVar, SemanticScope scope, boolean isStatic) {
                InvokableMeta meta = scope.getMeta();
                Collection<LeftValue> localVariables = scope.getRawLocalVariables().values();

                Iterator<LeftValue> localVarIte = localVariables.iterator();
                if (!localVarIte.hasNext())
                        throw new LtBug("empty local variables ???");
                LeftValue current = localVarIte.next();

                int insIndex;
                // static methods doesn't contain `this` in slot0
                // so the iteration goes directly from the first param
                // else, this is in slot0, so iteration goes from this
                // then the first param
                if (isStatic) {
                        insIndex = 0;
                } else {
                        insIndex = 1;
                }
                while (theVar != current /* reference eq */) {
                        // check whether it's captured but not used
                        // which should be removed
                        if (current instanceof SParameter) {
                                SParameter p = (SParameter) current;
                                if (p.isCapture() && !p.isUsed()) {
                                        current = localVarIte.next();
                                        continue;
                                }
                        }

                        // the local var should be added
                        int currentSlot = _slots(current.type());
                        insIndex += currentSlot;

                        // check whether it's pointer and should not be pointer
                        // which should be optimized as normal local variable
                        if (current.type().fullName().equals(Pointer.class.getName()) && !meta.pointerLocalVar.contains(current)) {
                                assert currentSlot == 1; // pointer is a ref type, should take 1 slot
                                assert current.type() instanceof PointerType; // pointerType
                                if (isParameterWrappingPointer(current)) {
                                        // it will be totally ignored, so index should be subtracted as well
                                        insIndex -= currentSlot;
                                } else {
                                        STypeDef t = ((PointerType) current.type()).getPointingType();
                                        insIndex += (_slots(t) - 1); // expand the slot size
                                }
                        }

                        if (!localVarIte.hasNext())
                                throw new LtBug("empty local variable ???");
                        current = localVarIte.next();
                }
                return insIndex;
        }

        public static boolean isParameterWrappingPointer(LeftValue v) {
                return v instanceof LocalVariable && ((LocalVariable) v).getWrappingParam() != null;
        }
}
