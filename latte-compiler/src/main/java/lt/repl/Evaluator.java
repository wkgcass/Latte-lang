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

import lt.compiler.*;
import lt.compiler.Properties;
import lt.compiler.Scanner;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.PackageDeclare;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * evaluator for Latte
 */
public class Evaluator {
        private final static String evalFileName = "eval";
        private final static String EvaluateClassName = "Evaluate";

        private List<Entry> recordedEntries = new ArrayList<>();
        private List<Import> imports = new ArrayList<>();
        private List<MethodDef> recordedMethods = new ArrayList<>();

        private class CL extends ClassLoader {
                private Map<String, byte[]> byteCodes = new HashMap<>();

                CL(ClassLoader cl) {
                        super(cl);
                }

                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                        byte[] byteCode = byteCodes.get(name);
                        if (byteCode == null) throw new ClassNotFoundException(name);
                        return defineClass(name, byteCode, 0, byteCode.length);
                }
        }

        private final CL cl;

        private int generatedVariableIndex = 0;
        private final String varNameBase;

        public static final int SCANNER_TYPE_INDENT = 0;
        public static final int SCANNER_TYPE_BRACE = 1;

        private int scannerType = 0;

        public Evaluator(ClassPathLoader classPathLoader) {
                this("res", classPathLoader);
        }

        public Evaluator(String varNameBase, ClassPathLoader classPathLoader) {
                this.varNameBase = varNameBase;
                cl = new CL(classPathLoader);
        }

        public void setScannerType(int type) {
                this.scannerType = type;
        }

        public static class Entry {
                public final String name;
                public final Object result;

                public Entry(String name, Object result) {
                        this.name = name;
                        this.result = result;
                }
        }

        public void put(String name, Object var) {
                Iterator<Entry> entryIterator = recordedEntries.iterator();
                while (entryIterator.hasNext()) {
                        Entry e = entryIterator.next();
                        if (e.name.equals(name)) entryIterator.remove();
                }
                recordedEntries.add(new Entry(name, var));
        }

        public Entry eval(String stmt) throws Exception {
                if (null == stmt || stmt.trim().isEmpty())
                        throw new IllegalArgumentException("the input string cannot be empty or null");

                ErrorManager err = new ErrorManager(true);
                Scanner scanner;
                switch (scannerType) {
                        case SCANNER_TYPE_BRACE:
                                scanner = new BraceScanner(evalFileName, new StringReader(stmt), new Properties(), err);
                                break;
                        case SCANNER_TYPE_INDENT:
                        default:
                                scanner = new IndentScanner(evalFileName, new StringReader(stmt), new Properties(), err);
                }
                Parser parser = new Parser(scanner.scan(), err);

                List<Statement> statements = parser.parse();
                Statement lastStatement = statements.isEmpty() ? null : statements.get(statements.size() - 1);
                String varName = null;
                if (null != lastStatement) {
                        if (lastStatement instanceof VariableDef) {
                                // variable def
                                // so, the name can be retrieved
                                VariableDef v = (VariableDef) lastStatement;
                                varName = v.getName();
                        } else if (lastStatement instanceof AST.Assignment) {
                                // it's an assignment
                                // the name can be retrieved
                                AST.Assignment ass = (AST.Assignment) lastStatement;
                                AST.Access assignTo = ass.assignTo;
                                if (assignTo.exp == null) {
                                        // assign to the field
                                        varName = assignTo.name;
                                } else {
                                        // cannot capture the name
                                        // generate a name
                                        // $resX
                                        varName = varNameBase + (generatedVariableIndex++);
                                        lastStatement = defineAVariable(varName, (Expression) lastStatement);
                                }
                        } else if (lastStatement instanceof Expression) {
                                // the last statement is an expression
                                // it can be assigned to a variable
                                // the variable should be
                                // $resX
                                varName = varNameBase + (generatedVariableIndex++);
                                lastStatement = defineAVariable(varName, (Expression) lastStatement);
                        }

                        // replace the last statement
                        statements.remove(statements.size() - 1);
                        statements.add(lastStatement);
                }

                List<Statement> innerStatements = new ArrayList<>();
                List<Statement> defList = new ArrayList<>();

                for (Statement s : statements) {
                        if (s instanceof ClassDef || s instanceof InterfaceDef || s instanceof FunDef) {
                                defList.add(s);
                        } else if (s instanceof Import) {
                                imports.add((Import) s);
                        } else if (s instanceof PackageDeclare) {
                                err.SyntaxException("scripts cannot have package declaration", s.line_col());
                        } else if (s instanceof MethodDef) {
                                recordedMethods.add((MethodDef) s);
                        } else {
                                innerStatements.add(s);
                        }
                }
                // validate innerStatements
                // VariableDef might be transformed into assign
                for (int i = 0; i < innerStatements.size(); ++i) {
                        Statement s = innerStatements.get(i);
                        if (s instanceof VariableDef) {
                                String name = ((VariableDef) s).getName();
                                boolean found = false;
                                for (Entry e : recordedEntries) {
                                        if (e.name.equals(name)) {
                                                found = true;
                                                break;
                                        }
                                }

                                if (found) {
                                        if (((VariableDef) s).getAnnos().isEmpty()
                                                && ((VariableDef) s).getModifiers().isEmpty()
                                                && ((VariableDef) s).getInit() != null) {
                                                innerStatements.set(i, new AST.Assignment(
                                                        new AST.Access(null, name, s.line_col()),
                                                        "=",
                                                        ((VariableDef) s).getInit(),
                                                        s.line_col()
                                                ));
                                        }
                                }
                        }
                }
                // fill the methods
                innerStatements.addAll(recordedMethods);

                // parameters of the class
                List<VariableDef> parameters = recordedEntries.
                        stream().
                        map(entry ->
                                new VariableDef(
                                        entry.name,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        LineCol.SYNTHETIC)).
                        collect(Collectors.toList());

                ClassDef evalClass = new ClassDef(
                        EvaluateClassName, Collections.emptySet(), parameters, null, Collections.emptyList(), Collections.emptySet(),
                        innerStatements, new LineCol(evalFileName, 0, 0)
                );

                // fill the eval class into the def list
                defList.add(evalClass);
                defList.add(new Import(new AST.PackageRef("lt::util", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defList.add(new Import(new AST.PackageRef("java::util", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defList.add(new Import(new AST.PackageRef("java::math", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defList.add(new Import(new AST.PackageRef("java::io", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defList.add(new Import(new AST.PackageRef("lt::repl", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defList.addAll(imports);

                SemanticProcessor processor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                        put(evalFileName, defList);
                }}, cl, err);
                CodeGenerator codeGen = new CodeGenerator(processor.parse(), processor.getTypes());
                Map<String, byte[]> byteCodes = codeGen.generate();
                List<Class<?>> classes = new ArrayList<>();
                for (Map.Entry<String, byte[]> entry : byteCodes.entrySet()) {
                        if (!entry.getKey().equals(EvaluateClassName)) {
                                cl.byteCodes.put(entry.getKey(), entry.getValue());
                                Class<?> c = cl.loadClass(entry.getKey());
                                classes.add(c);
                                c.getDeclaredFields(); // check the class format and throw exception
                        }
                }
                byte[] EvaluateBytes = byteCodes.get(EvaluateClassName);
                ClassLoader classLoader = new ClassLoader(cl) {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                if (name.equals(EvaluateClassName)) {
                                        return defineClass(name, EvaluateBytes, 0, EvaluateBytes.length);
                                } else throw new ClassNotFoundException(name);
                        }
                };
                Class<?> cls = classLoader.loadClass(EvaluateClassName);
                Class<?>[] consParams = new Class[recordedEntries.size()];
                Object[] args = new Object[recordedEntries.size()];
                for (int i = 0; i < consParams.length; ++i) {
                        consParams[i] = Object.class;
                        args[i] = recordedEntries.get(i).result;
                }
                Constructor<?> con = cls.getDeclaredConstructor(consParams);
                Object o = con.newInstance(args);

                Entry toReturn;
                if (varName == null) {
                        // there's nothing to print
                        if (classes.isEmpty()) {
                                // simply return the instance
                                toReturn = new Entry(null, o);
                        } else {
                                toReturn = new Entry("definedClasses", classes);
                        }
                } else {
                        Field f = cls.getDeclaredField(varName);
                        f.setAccessible(true);
                        toReturn = new Entry(varName, f.get(o));
                }

                // record the entry if name is not null
                recordedEntries.clear();
                for (Field f : cls.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object value = f.get(o);
                        recordedEntries.add(new Entry(f.getName(), value));
                }

                return toReturn;
        }

        private VariableDef defineAVariable(String name, Expression initValue) {
                VariableDef v = new VariableDef(name, Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(initValue);
                return v;
        }
}
