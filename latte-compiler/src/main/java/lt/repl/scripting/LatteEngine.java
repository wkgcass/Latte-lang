package lt.repl.scripting;

import lt.compiler.*;
import lt.compiler.Properties;
import lt.compiler.Scanner;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.lang.Unit;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * the script engine
 */
public class LatteEngine implements ScriptEngine {
        private static final String SCRIPT_CLASS_NAME = "LATTE_SCRIPTING";
        private final LatteEngineFactory factory;
        private ScriptContext context;
        private ClassLoader classLoader;

        LatteEngine(LatteEngineFactory factory) {
                this.factory = factory;
                Bindings engineScope = new LatteScope();
                context = new LatteContext(engineScope);
        }

        public LatteEngine(ClassLoader loader) {
                factory = null;
                classLoader = loader;
        }

        @Override
        public Object eval(String script, ScriptContext context) throws ScriptException {
                return eval(script, context.getBindings(ScriptContext.ENGINE_SCOPE));
        }

        @Override
        public Object eval(Reader reader, ScriptContext context) throws ScriptException {
                return eval(readFully(reader), context);
        }

        @Override
        public Object eval(String script) throws ScriptException {
                return eval(script, context);
        }

        @Override
        public Object eval(Reader reader) throws ScriptException {
                return eval(readFully(reader));
        }

        @Override
        public Object eval(String script, Bindings n) throws ScriptException {
                return eval(script, n, new Config()
                        .setScannerType(Config.SCANNER_TYPE_INDENT)
                        .setVarNamePrefix("res")
                        .setEval(false));
        }

        public Object eval(String script, Bindings n, Config config) throws ScriptException {
                List<Import> imports = initImports(n);
                CL cl = initCL(n);
                List<MethodDef> recordedMethods = initMethodList(n);
                int evalCount = incAndGetEvalCount(n);
                final String scriptName = "latte-scripting.lts";
                boolean isLatteScope = n instanceof LatteScope;
                boolean recordVarIfPresent = true;

                try {
                        ErrorManager err = new ErrorManager(true);
                        Scanner scanner;
                        switch (config.getScannerType()) {
                                case Config.SCANNER_TYPE_BRACE:
                                        scanner = new BraceScanner(scriptName, new StringReader(script), new Properties(), err);
                                        break;
                                case Config.SCANNER_TYPE_INDENT:
                                default:
                                        scanner = new IndentScanner(scriptName, new StringReader(script), new Properties(), err);
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
                                                varName = config.getVarNamePrefix() + incAndGetResCount(n);
                                                lastStatement = defineAVariable(varName, (Expression) lastStatement);
                                        }
                                } else if (lastStatement instanceof Expression) {
                                        // the last statement is an expression
                                        // it can be assigned to a variable
                                        // the variable should be generated
                                        varName = config.getVarNamePrefix() + incAndGetResCount(n);
                                        lastStatement = defineAVariable(varName, (Expression) lastStatement);
                                        if (!config.isEval()) {
                                                recordVarIfPresent = false;
                                        }
                                }

                                // replace the last statement
                                statements.remove(statements.size() - 1);
                                statements.add(lastStatement);
                        }

                        List<Statement> scriptStatements = new ArrayList<Statement>();
                        final List<Statement> defList = new ArrayList<Statement>();
                        // buffer these imports and add them if pass compile
                        List<Import> readyToAddIntoImport = new ArrayList<Import>();

                        for (Statement s : statements) {
                                if (s instanceof ClassDef || s instanceof InterfaceDef || s instanceof FunDef || s instanceof ObjectDef) {
                                        defList.add(s);
                                } else if (s instanceof Import) {
                                        readyToAddIntoImport.add((Import) s);
                                } else if (s instanceof PackageDeclare) {
                                        err.SyntaxException("scripts cannot have package declaration", s.line_col());
                                } else if (s instanceof MethodDef) {
                                        recordedMethods.add((MethodDef) s);
                                } else {
                                        scriptStatements.add(s);
                                }
                        }
                        // validate innerStatements
                        // VariableDef might be transformed into assign
                        for (int i = 0; i < scriptStatements.size(); ++i) {
                                Statement s = scriptStatements.get(i);
                                if (s instanceof VariableDef) {
                                        String name = ((VariableDef) s).getName();
                                        boolean found = false;
                                        for (String key : n.keySet()) {
                                                if (key.equals(name)) {
                                                        found = true;
                                                        break;
                                                }
                                        }

                                        if (found) {
                                                if (((VariableDef) s).getAnnos().isEmpty()
                                                        && ((VariableDef) s).getModifiers().isEmpty()
                                                        && ((VariableDef) s).getInit() != null) {
                                                        scriptStatements.set(i, new AST.Assignment(
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
                        scriptStatements.addAll(recordedMethods);

                        // parameters and args of the class
                        List<VariableDef> parameters = new ArrayList<VariableDef>();
                        List<Class<?>> paramClasses = new ArrayList<Class<?>>();
                        List<Object> paramInstances = new ArrayList<Object>();
                        for (Map.Entry<String, Object> entry : n.entrySet()) {
                                String name = entry.getKey();
                                Object value = entry.getValue();
                                if (!isValidName(name)) continue;
                                // parameter
                                VariableDef v = new VariableDef(
                                        name,
                                        Collections.<Modifier>emptySet(),
                                        Collections.<AST.Anno>emptySet(),
                                        LineCol.SYNTHETIC);
                                parameters.add(v);
                                // class
                                if (isLatteScope) {
                                        Class<?> cls = ((LatteScope) n).getType(name);
                                        paramClasses.add(cls);

                                        String clsName = cls.getName();
                                        if (clsName.contains(".")) {
                                                v.setType(new AST.Access(
                                                        new AST.PackageRef(
                                                                clsName.substring(0, clsName.lastIndexOf(".")),
                                                                LineCol.SYNTHETIC),
                                                        clsName.substring(clsName.lastIndexOf(".") + 1),
                                                        LineCol.SYNTHETIC)
                                                );
                                        } else {
                                                v.setType(new AST.Access(
                                                        null, clsName, LineCol.SYNTHETIC
                                                ));
                                        }
                                } else {
                                        paramClasses.add(Object.class);
                                }
                                // instance
                                paramInstances.add(value);
                        }

                        final String className = SCRIPT_CLASS_NAME + '_' + evalCount;
                        ClassDef evalClass = new ClassDef(
                                className,
                                Collections.<Modifier>emptySet(),
                                parameters,
                                null,
                                Collections.<AST.Access>emptyList(),
                                Collections.<AST.Anno>emptySet(),
                                scriptStatements,
                                new LineCol(scriptName, 0, 0)
                        );

                        // fill the eval class into the def list
                        defList.add(evalClass);
                        defList.add(new Import(new AST.PackageRef("java::util", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        defList.add(new Import(new AST.PackageRef("java::math", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        defList.add(new Import(new AST.PackageRef("java::io", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        defList.add(new Import(new AST.PackageRef("lt::repl", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        defList.addAll(imports);
                        defList.addAll(readyToAddIntoImport);

                        SemanticProcessor processor = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                                put(scriptName, defList);
                        }}, cl, err);
                        CodeGenerator codeGen = new CodeGenerator(processor.parse(), processor.getTypes());
                        // the imports are valid now, add into import list
                        imports.addAll(readyToAddIntoImport);
                        Map<String, byte[]> byteCodes = codeGen.generate();
                        List<Class<?>> classes = new ArrayList<Class<?>>();
                        for (Map.Entry<String, byte[]> entry : byteCodes.entrySet()) {
                                cl.addByteCodes(entry.getKey(), entry.getValue());
                        }
                        for (Map.Entry<String, byte[]> entry : byteCodes.entrySet()) {
                                Class<?> c = cl.loadClass(entry.getKey());
                                if (!entry.getKey().equals(className))
                                        classes.add(c);
                                c.getDeclaredFields(); // check the class format and throw exception
                        }

                        // get real class
                        Class<?> cls = cl.loadClass(className);
                        Class<?>[] consParams = paramClasses.toArray(new Class[paramClasses.size()]);
                        Object[] args = paramInstances.toArray(new Object[paramInstances.size()]);
                        Constructor<?> con = cls.getDeclaredConstructor(consParams);
                        Object o;
                        try {
                                o = con.newInstance(args);
                        } catch (InvocationTargetException ite) {
                                throw ite.getTargetException();
                        }

                        // record the entry if name is not null
                        for (Field f : cls.getDeclaredFields()) {
                                f.setAccessible(true);
                                Object value = f.get(o);
                                if (!recordVarIfPresent && !config.isEval() && f.getName().equals(varName)) {
                                        continue;
                                }
                                if (!isLatteScope || n.containsKey(f.getName())) {
                                        n.put(f.getName(), value);
                                } else {
                                        ((LatteScope) n).putNew(f.getName(), value, f.getType());
                                }
                        }

                        // the result
                        Object result;
                        if (varName == null) {
                                // there's nothing to print
                                if (classes.isEmpty()) {
                                        // simply return the instance
                                        if (config.isEval()) {
                                                result = new EvalEntry(null, o, o.getClass());
                                        } else {
                                                result = null;
                                        }
                                } else {
                                        if (config.isEval()) {
                                                result = new EvalEntry("definedClasses", classes, List.class);
                                        } else {
                                                result = Unit.get();
                                        }
                                }
                        } else {
                                Field f = cls.getDeclaredField(varName);
                                f.setAccessible(true);
                                Object v = f.get(o);
                                if (config.isEval()) {
                                        result = new EvalEntry(varName, v, f.getType());
                                } else {
                                        result = v;
                                }
                        }

                        return result;
                } catch (Throwable t) {
                        if (t instanceof Exception) {
                                throw new ScriptException((Exception) t);
                        } else {
                                throw new ScriptException(new Exception(t));
                        }
                }
        }

        private VariableDef defineAVariable(String name, Expression initValue) {
                VariableDef v = new VariableDef(name, Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC);
                v.setInit(initValue);
                return v;
        }

        @Override
        public Object eval(Reader reader, Bindings n) throws ScriptException {
                return eval(readFully(reader), n);
        }

        @Override
        public void put(String key, Object value) {
                getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
        }

        @Override
        public Object get(String key) {
                return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
        }

        @Override
        public Bindings getBindings(int scope) {
                return context.getBindings(scope);
        }

        @Override
        public void setBindings(Bindings bindings, int scope) {
                context.setBindings(bindings, scope);
        }

        @Override
        public Bindings createBindings() {
                return new LatteScope();
        }

        @Override
        public ScriptContext getContext() {
                return context;
        }

        @Override
        public void setContext(ScriptContext context) {
                this.context = context;
        }

        @Override
        public ScriptEngineFactory getFactory() {
                return factory;
        }

        private static String readFully(Reader reader) throws ScriptException {
                char[] arr = new char[8 * 1024]; // 8K at a time
                StringBuilder buf = new StringBuilder();
                int numChars;
                try {
                        while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                                buf.append(arr, 0, numChars);
                        }
                } catch (IOException exp) {
                        throw new ScriptException(exp);
                }
                return buf.toString();
        }

        private static List<Import> initImports(Bindings n) {
                final String importsName = "$latte.scripting.imports";
                @SuppressWarnings("unchecked")
                List<Import> imports = (List<Import>) n.get(importsName);
                if (imports == null) {
                        imports = new ArrayList<Import>();
                        n.put(importsName, imports);
                }
                return imports;
        }

        private CL initCL(Bindings n) {
                final String clName = "$latte.scripting.CL";
                CL cl = (CL) n.get(clName);
                if (cl == null) {
                        if (this.classLoader == null) {
                                cl = new CL(Thread.currentThread().getContextClassLoader());
                        } else {
                                cl = new CL(this.classLoader);
                        }
                        n.put(clName, cl);
                }
                return cl;
        }

        private static List<MethodDef> initMethodList(Bindings n) {
                final String methodsName = "$latte.scripting.methods";
                @SuppressWarnings("unchecked")
                List<MethodDef> methodDefs = (List<MethodDef>) n.get(methodsName);
                if (methodDefs == null) {
                        methodDefs = new ArrayList<MethodDef>();
                        n.put(methodsName, methodDefs);
                }
                return methodDefs;
        }

        private static int incAndGetEvalCount(Bindings n) {
                final String evalCountName = "$latte.scripting.eval_count";
                Integer evalCount = (Integer) n.get(evalCountName);
                if (evalCount == null) {
                        evalCount = 1;
                } else {
                        ++evalCount;
                }
                n.put(evalCountName, evalCount);
                return evalCount;
        }

        private static int incAndGetResCount(Bindings n) {
                final String resCountName = "$latte.scripting.res_count";
                Integer resCount = (Integer) n.get(resCountName);
                if (resCount == null) {
                        resCount = 0;
                } else {
                        ++resCount;
                }
                n.put(resCountName, resCount);
                return resCount;
        }

        private static boolean isValidName(String name) {
                for (char c : name.toCharArray()) {
                        if (c != '_' && c != '$' && (c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9')) {
                                return false;
                        }
                }
                return true;
        }
}
