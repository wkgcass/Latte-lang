package org.lattelang.idea;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * latte fileType
 */
public class LatteFileType extends LanguageFileType {
        public static LatteFileType INSTANCE = new LatteFileType();

        private LatteFileType() {
                super(LatteLanguage.INSTANCE);
        }

        @NotNull
        @Override
        public String getName() {
                return "Latte-lang file";
        }

        @NotNull
        @Override
        public String getDescription() {
                return "Latte-lang source code";
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
                return "lt";
        }

        @Nullable
        @Override
        public Icon getIcon() {
                return Consts.ICON;
        }
}
