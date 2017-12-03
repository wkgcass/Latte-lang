package org.lattelang.idea;

import com.intellij.lang.Language;

/**
 * latte language
 */
public class LatteLanguage extends Language {
        public static final LatteLanguage INSTANCE = new LatteLanguage();

        private LatteLanguage() {
                super("Latte-lang");
        }
}
