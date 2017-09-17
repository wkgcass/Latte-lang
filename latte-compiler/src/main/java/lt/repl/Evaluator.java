/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.repl;

import lt.repl.scripting.*;

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * evaluator for Latte
 */
public class Evaluator {
        private final LatteEngine latteEngine;
        private final String varNameBase;
        private int scannerType = Config.SCANNER_TYPE_INDENT;

        public Evaluator(ClassPathLoader classPathLoader) {
                this("res", classPathLoader);
        }

        public Evaluator(String varNameBase, ClassPathLoader classPathLoader) {
                this.varNameBase = varNameBase;
                this.latteEngine = new LatteEngine(classPathLoader);
                this.latteEngine.setContext(new LatteContext(new LatteScope()));
        }

        public void setScannerType(int type) {
                this.scannerType = type;
        }

        public void put(String name, Object var) {
                latteEngine.put(name, var);
        }

        public EvalEntry eval(String stmt) throws Exception {
                try {
                        return (EvalEntry) latteEngine.eval(stmt, latteEngine.getBindings(ScriptContext.ENGINE_SCOPE),
                                new Config()
                                        .setScannerType(scannerType).setVarNamePrefix(varNameBase).setEval(true));
                } catch (ScriptException e) {
                        throw (Exception) e.getCause();
                }
        }
}
