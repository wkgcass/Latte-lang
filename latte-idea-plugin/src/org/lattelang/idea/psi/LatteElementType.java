package org.lattelang.idea.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.lattelang.idea.LatteLanguage;

public class LatteElementType extends IElementType {
        public LatteElementType(@NotNull String debugName) {
                super(debugName, LatteLanguage.INSTANCE);
        }
}
