package lt.repl.scripting;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

/**
 * script context
 */
public class LatteContext implements ScriptContext {
        private Bindings engineScope;
        private Bindings globalScope;

        public LatteContext(Bindings engineScope) {
                this.engineScope = engineScope;
        }

        @Override
        public void setBindings(Bindings bindings, int scope) {
                switch (scope) {
                        case ENGINE_SCOPE:
                                engineScope = bindings;
                                break;
                        case GLOBAL_SCOPE:
                                globalScope = bindings;
                                break;
                        default:
                                throw new IllegalArgumentException("invalid scope");
                }
        }

        private void validateScope(int scope) {
                if (scope != ENGINE_SCOPE && scope != GLOBAL_SCOPE) {
                        throw new IllegalArgumentException("invalid scope, not current|engine|global");
                }
        }

        private void validateName(String name) {
                if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("invalid name, empty");
                }
        }

        private Bindings getScope(int scope) {
                switch (scope) {
                        case ENGINE_SCOPE:
                                return engineScope;
                        case GLOBAL_SCOPE:
                                return globalScope;
                        default:
                                throw new Error("won't reach here!");
                }
        }

        @Override
        public Bindings getBindings(int scope) {
                validateScope(scope);
                return getScope(scope);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
                validateScope(scope);
                validateName(name);
                getScope(scope).put(name, value);
        }

        @Override
        public Object getAttribute(String name, int scope) {
                validateScope(scope);
                validateName(name);
                return getScope(scope).get(name);
        }

        @Override
        public Object removeAttribute(String name, int scope) {
                validateScope(scope);
                validateName(name);
                return getScope(scope).remove(name);
        }

        @Override
        public Object getAttribute(String name) {
                if (engineScope.containsKey(name)) return engineScope.get(name);
                if (globalScope.containsKey(name)) return globalScope.get(name);
                return null;
        }

        @Override
        public int getAttributesScope(String name) {
                if (engineScope.containsKey(name)) return ENGINE_SCOPE;
                if (globalScope.containsKey(name)) return GLOBAL_SCOPE;
                return -1;
        }

        @Override
        public Writer getWriter() {
                return new PrintWriter(System.out);
        }

        @Override
        public Writer getErrorWriter() {
                return new PrintWriter(System.err);
        }

        @Override
        public void setWriter(Writer writer) {
                throw new UnsupportedOperationException("Latte is compiled to jvm byte code and run directly on JVM, set System.out if you really want to.");
        }

        @Override
        public void setErrorWriter(Writer writer) {
                throw new UnsupportedOperationException("Latte is compiled to jvm byte code and run directly on JVM, set System.err if you really want to.");
        }

        @Override
        public Reader getReader() {
                return new InputStreamReader(System.in);
        }

        @Override
        public void setReader(Reader reader) {
                throw new UnsupportedOperationException("Latte is compiled to jvm byte code and run directly on JVM, set System.in if you really want to.");
        }

        @Override
        public List<Integer> getScopes() {
                return Arrays.asList(ENGINE_SCOPE, GLOBAL_SCOPE);
        }
}
