/*
 * Copyright 2000-2007 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lattelang.idea.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.lattelang.idea.psi.LatteLexer;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static org.lattelang.idea.psi.LatteTokenTypes.*;

public class LatteSyntaxHighlighter extends SyntaxHighlighterBase {
        public static final TextAttributesKey _ERR =
                createTextAttributesKey("ERR", HighlighterColors.BAD_CHARACTER);
        public static final TextAttributesKey _NUMBER =
                createTextAttributesKey("NUMBER", DefaultLanguageHighlighterColors.NUMBER);
        public static final TextAttributesKey _END_MARK =
                createTextAttributesKey("END_MARK", DefaultLanguageHighlighterColors.SEMICOLON);
        public static final TextAttributesKey _KEYWORD =
                createTextAttributesKey("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
        public static final TextAttributesKey _BOOL =
                createTextAttributesKey("BOOL", DefaultLanguageHighlighterColors.KEYWORD);
        public static final TextAttributesKey _COMMENT =
                createTextAttributesKey("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
        public static final TextAttributesKey _STRING =
                createTextAttributesKey("STRING", DefaultLanguageHighlighterColors.STRING);
        public static final TextAttributesKey _MODIFIER =
                createTextAttributesKey("MODIFIER", DefaultLanguageHighlighterColors.KEYWORD);

        private static final TextAttributesKey[] ERR_KEYS = new TextAttributesKey[]{_ERR};
        private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{_NUMBER};
        private static final TextAttributesKey[] END_KEYS = new TextAttributesKey[]{_END_MARK};
        private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{_KEYWORD};
        private static final TextAttributesKey[] BOOL_KEYS = new TextAttributesKey[]{_BOOL};
        private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{_COMMENT};
        private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{_STRING};
        private static final TextAttributesKey[] MODIFIER_KEYS = new TextAttributesKey[]{_MODIFIER};
        private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

        @NotNull
        @Override
        public Lexer getHighlightingLexer() {
                return new LatteLexer();
        }

        @NotNull
        @Override
        public TextAttributesKey[] getTokenHighlights(IElementType type) {
                if (type == NUMBER) {
                        return NUMBER_KEYS;
                } else if (type == END_MARK) {
                        return END_KEYS;
                } else if (type == KEY) {
                        return KEYWORD_KEYS;
                } else if (type == BOOL) {
                        return BOOL_KEYS;
                } else if (type == STRING) {
                        return STRING_KEYS;
                } else if (type == MODIFIER) {
                        return MODIFIER_KEYS;
                } else if (type == COMMENT) {
                        return COMMENT_KEYS;
                } else if (type == ERR) {
                        return ERR_KEYS;
                } else {
                        return EMPTY_KEYS;
                }
        }
}
