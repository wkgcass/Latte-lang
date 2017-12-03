package org.lattelang.idea.highlighter;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * latte syntax highlighter factory
 */
public class LatteSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
        @NotNull
        @Override
        public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
                return new LatteSyntaxHighlighter();
        }
}
