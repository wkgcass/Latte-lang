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
import lt.compiler.Scanner;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * evaluator for LessTyping
 */
public class Evaluator {
        private List<Entry> recordedEntries = new ArrayList<>();
        private List<String> imports = new ArrayList<>();

        private String recordedStatements = "\n";

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

        private static final String EVALUATE_BASIC_FORMAT = "" +
                "import java::util::_\n" +
                "import java::math::_\n" +
                "import lt::repl::_\n" +
                "import java::io::_\n";
        private static final String EVALUATE_CLASS_DEF = "class Evaluate";
        private static final int EVALUATE_BASIC_LINES = 5;

        private int generatedVariableIndex = 0;
        private final String varNameBase;

        public Evaluator(ClassPathLoader classPathLoader) {
                this("res", classPathLoader);
        }

        public Evaluator(String varNameBase, ClassPathLoader classPathLoader) {
                this.varNameBase = varNameBase;
                cl = new CL(classPathLoader);
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

        public void addImport(String anImport) {
                for (String im : imports) {
                        if (im.equals(anImport)) return;
                }
                imports.add(anImport);
        }

        public Entry eval(String stmt) throws Exception {
                if (null == stmt || stmt.trim().isEmpty()) throw new IllegalArgumentException("the input string cannot be empty or null");

                if (stmt.startsWith("class") || stmt.startsWith("interface")) {

                        ErrorManager errorManager = new ErrorManager(true);
                        errorManager.out = ErrorManager.Out.allNull();

                        Scanner scanner = new Scanner("EVALUATE.lts", new StringReader(stmt), new Scanner.Properties(), errorManager);
                        Parser parser = new Parser(scanner.scan(), errorManager);
                        SemanticProcessor processor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                                put("EVALUATE.lts", parser.parse());
                        }}, cl);
                        CodeGenerator codeGen = new CodeGenerator(processor.parse());
                        Map<String, byte[]> byteCodes = codeGen.generate();

                        List<Class<?>> classes = new ArrayList<>();
                        cl.byteCodes.putAll(byteCodes);
                        for (String s : byteCodes.keySet()) {
                                Class<?> c = cl.loadClass(s);
                                classes.add(c);
                        }

                        return new Entry("definedClasses", classes);
                } else {

                        BufferedReader bufferedReader = new BufferedReader(new StringReader(stmt));

                        StringBuilder sb = new StringBuilder();

                        sb.append(EVALUATE_BASIC_FORMAT);
                        for (String im : imports) {
                                sb.append("import ").append(im).append("\n");
                        }
                        sb.append(EVALUATE_CLASS_DEF).append("(");

                        // build local variables
                        boolean isFirst = true;
                        for (Entry entry : recordedEntries) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(entry.name);
                        }
                        sb.append(")\n");
                        sb.append(recordedStatements);

                        String s;
                        while ((s = bufferedReader.readLine()) != null) {
                                sb.append("    ").append(s).append("\n");
                        }

                        String code = sb.toString(); // the generated code

                        // check lines in recordedStatements
                        String[] strs = recordedStatements.split("\\n|\\r");

                        Scanner.Properties scanner$properties = new Scanner.Properties();
                        scanner$properties._COLUMN_BASE_ = -4;
                        scanner$properties._LINE_BASE_ = -strs.length - EVALUATE_BASIC_LINES - 1;

                        ErrorManager errorManager = new ErrorManager(true);
                        errorManager.out = ErrorManager.Out.allNull();

                        Scanner scanner = new Scanner("EVALUATE.lts", new StringReader(code), scanner$properties, errorManager);
                        ElementStartNode root = scanner.scan();

                        Parser parser = new Parser(root, errorManager);
                        List<Statement> statements = parser.parse();

                        ClassDef classDef = (ClassDef) statements.get(4 + imports.size()); // it must be class def (class Evaluate)
                        List<Statement> classStatements = classDef.statements;
                        int lastIndex = classStatements.size() - 1;
                        Statement lastStatement = classStatements.get(lastIndex);

                        String varName = null;
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
                                        // cannot catch the name
                                        // generate a name
                                        // $resX
                                        varName = varNameBase + (generatedVariableIndex++);
                                        VariableDef v = defineAVariable(varName, (Expression) lastStatement);
                                        classStatements.set(lastIndex, v);
                                }
                        } else if (lastStatement instanceof Expression) {
                                // the last statement is an expression
                                // it can be assigned to a variable
                                // the variable should be
                                // $resX
                                varName = varNameBase + (generatedVariableIndex++);
                                VariableDef v = defineAVariable(varName, (Expression) lastStatement);
                                classStatements.set(lastIndex, v);
                        }
                        if (lastStatement instanceof MethodDef) {
                                // lastStatement is MethodDef
                                // then the input should be recorded
                                BufferedReader tmpReader = new BufferedReader(new StringReader(stmt));
                                StringBuilder recorded = new StringBuilder();
                                while ((s = tmpReader.readLine()) != null) {
                                        if (s.startsWith(" ")) {
                                                // indentation is not 0
                                                // simply record this
                                                recorded.append("    ").append(s).append("\n");
                                        } else {
                                                // indentation is 0
                                                // clear the recorded string builder and record the line
                                                recorded.delete(0, recorded.length());
                                                recorded.append("    ").append(s).append("\n");
                                        }
                                }
                                recordedStatements += recorded;
                        }

                        SemanticProcessor processor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                                put("EVALUATE.lts", statements);
                        }}, cl);
                        Set<STypeDef> types = processor.parse();

                        CodeGenerator codeGen = new CodeGenerator(types);
                        Map<String, byte[]> byteCodes = codeGen.generate();

                        ClassLoader loader = new ClassLoader(cl) {
                                @Override
                                protected Class<?> findClass(String name) throws ClassNotFoundException {
                                        byte[] byteCode = byteCodes.get(name);
                                        if (byteCode == null) throw new ClassNotFoundException(name);
                                        return defineClass(name, byteCode, 0, byteCode.length);
                                }
                        };
                        Class<?> cls = loader.loadClass("Evaluate");
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
                                // simply return the instance
                                toReturn = new Entry(null, o);
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
        }

        private VariableDef defineAVariable(String name, Expression initValue) {
                VariableDef v = new VariableDef(name, Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(initValue);
                return v;
        }
}
