package lt.repl.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * the script factory
 */
public class LatteEngineFactory implements ScriptEngineFactory {
        @Override
        public String getEngineName() {
                return "Latte-lang Scripting Engine";
        }

        @Override
        public String getEngineVersion() {
                return "ALPHA";
        }

        @Override
        public List<String> getExtensions() {
                return Collections.singletonList("lts");
        }

        @Override
        public List<String> getMimeTypes() {
                return Arrays.asList("application/x-latte", "text/latte", "application/latte");
        }

        @Override
        public List<String> getNames() {
                return Arrays.asList("latte", "Latte", "latte-lang", "Latte-lang", "Latte-Lang");
        }

        @Override
        public String getLanguageName() {
                return "Latte-lang";
        }

        @Override
        public String getLanguageVersion() {
                return "ALPHA";
        }

        @Override
        public Object getParameter(String key) {
                if (key.equals(ScriptEngine.ENGINE)) {
                        return getEngineName();
                } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
                        return getEngineVersion();
                } else if (key.equals(ScriptEngine.NAME)) {
                        return getNames().get(0);
                } else if (key.equals(ScriptEngine.LANGUAGE)) {
                        return getLanguageName();
                } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
                        return getLanguageVersion();
                } else if (key.equals("THREADING")) {
                        return null;
                } else {
                        return null;
                }
        }

        @Override
        public String getMethodCallSyntax(String obj, String m, String... args) {
                StringBuilder sb = new StringBuilder();
                if (obj == null) {
                        sb.append("`").append(m).append("`");
                } else {
                        sb.append(obj).append(".`").append(m).append("`(");
                }
                boolean isFirst = true;
                for (String a : args) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(a);
                }
                sb.append(")");
                return sb.toString();
        }

        @Override
        public String getOutputStatement(String toDisplay) {
                StringBuilder sb = new StringBuilder();
                sb.append("println(\"");
                int len = toDisplay.length();
                for (int i = 0; i < len; i++) {
                        char ch = toDisplay.charAt(i);
                        switch (ch) {
                                case '"':
                                        sb.append("\\\"");
                                        break;
                                case '\\':
                                        sb.append("\\\\");
                                        break;
                                default:
                                        sb.append(ch);
                                        break;
                        }
                }
                sb.append("\")");
                return sb.toString();
        }

        @Override
        public String getProgram(String... statements) {
                StringBuilder sb = new StringBuilder();
                for (String s : statements) {
                        sb.append(s).append("\n");
                }
                return sb.toString();
        }

        @Override
        public ScriptEngine getScriptEngine() {
                return new LatteEngine(this);
        }
}
