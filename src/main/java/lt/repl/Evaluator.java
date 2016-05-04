package lt.repl;

import lt.compiler.*;
import lt.compiler.Scanner;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.VariableDef;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.*;

/**
 * evaluator for LessTyping
 */
public class Evaluator {
        /**
         * the originally input and transformed statements
         */
        private String recordedStatements = EVALUATE_BASIC_FORMAT;
        private int lines = 3;

        private static final String EVALUATE_BASIC_FORMAT = "" +
                "#>  java::util::_\n" +
                "    java::math::_\n" +
                "class Evaluate\n";

        private int generatedVariableIndex = 0;
        private final String varNameBase;

        public Evaluator() {
                this("$res_");
        }

        public Evaluator(String varNameBase) {
                this.varNameBase = varNameBase;
        }

        public static class Entry {
                public final String name;
                public final Object result;

                public Entry(String name, Object result) {
                        this.name = name;
                        this.result = result;
                }
        }

        public Entry eval(String stmt) throws Exception {
                if (null == stmt || stmt.trim().isEmpty()) throw new IllegalArgumentException("the input string cannot be empty or null");
                BufferedReader bufferedReader = new BufferedReader(new StringReader(stmt));

                StringBuilder sb = new StringBuilder();
                sb.append(recordedStatements);
                int newLines = 0;
                String s;
                while ((s = bufferedReader.readLine()) != null) {
                        sb.append("    ").append(s).append("\n");
                        ++newLines;
                }

                String code = sb.toString(); // the generated code
                Scanner scanner = new Scanner("EVALUATE.lts", new StringReader(code), 4);
                ElementStartNode root = scanner.parse();

                Parser parser = new Parser(root);
                List<Statement> statements = parser.parse();

                ClassDef classDef = (ClassDef) statements.get(1); // it must be class def (class Evaluate)
                List<Statement> classStatements = classDef.statements;
                Statement lastStatement = classStatements.get(classStatements.size() - 1);

                String varName = null;
                LineCol changeLastInputLine = null;
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
                                classStatements.remove(classStatements.size() - 1);
                                classStatements.add(v);

                                changeLastInputLine = lastStatement.line_col();
                        }
                } else if (lastStatement instanceof Expression) {
                        // the last statement is an expression
                        // it can be assigned to a variable
                        // the variable should be
                        // $resX
                        varName = varNameBase + (generatedVariableIndex++);
                        VariableDef v = defineAVariable(varName, (Expression) lastStatement);
                        classStatements.remove(classStatements.size() - 1);
                        classStatements.add(v);

                        changeLastInputLine = lastStatement.line_col();
                }

                SemanticProcessor processor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                        put("EVALUATE.lts", statements);
                }});
                Set<STypeDef> types = processor.parse();

                CodeGenerator codeGen = new CodeGenerator(types);
                Map<String, byte[]> byteCodes = codeGen.generate();

                ClassLoader loader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                byte[] byteCode = byteCodes.get(name);
                                return defineClass(name, byteCode, 0, byteCode.length);
                        }
                };
                Class<?> cls = loader.loadClass("Evaluate");
                Object o = cls.newInstance();

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

                // record last statements
                if (changeLastInputLine == null) {
                        recordedStatements = code;
                        this.lines += newLines;
                } else {
                        int line = changeLastInputLine.line;
                        line -= this.lines; // line of the input

                        int tmpLine = 0;
                        BufferedReader br = new BufferedReader(new StringReader(stmt));
                        sb = new StringBuilder();
                        boolean hasPar = false;
                        while ((s = br.readLine()) != null) {
                                ++tmpLine;
                                sb.append("    ");
                                if (hasPar) sb.append("    ");
                                if (tmpLine == line) {
                                        sb.append(varName).append("=(");
                                        hasPar = true;
                                }
                                sb.append(s).append("\n");
                        }

                        assert newLines == tmpLine;

                        sb.setCharAt(sb.length() - 1, ')');
                        sb.append("\n");
                        this.recordedStatements += sb.toString();
                        this.lines += tmpLine;
                }

                return toReturn;
        }

        private VariableDef defineAVariable(String name, Expression initValue) {
                VariableDef v = new VariableDef(name, Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(initValue);
                return v;
        }
}
