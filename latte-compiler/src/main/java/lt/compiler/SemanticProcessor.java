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

package lt.compiler;

import lt.compiler.semantic.*;
import lt.compiler.semantic.builtin.*;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.RegexLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.generator.SourceGenerator;
import lt.lang.Dynamic;
import lt.lang.LtRuntime;
import lt.lang.LtIterator;
import lt.lang.Undefined;
import lt.dependencies.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * semantic processor
 */
public class SemanticProcessor {
        private static final int PARSING_CLASS = 0;
        private static final int PARSING_INTERFACE = 1;

        /**
         * Map&lt;FileName, List of statements&gt;
         */
        private final Map<String, List<Statement>> mapOfStatements;
        /**
         * maps full name to type<br>
         * one type should only exist once in a Processor
         */
        private Map<String, STypeDef> types = new HashMap<>();
        /**
         * full name to {@link ClassDef} from {@link Parser}
         */
        private Map<String, ClassDef> originalClasses = new HashMap<>();
        /**
         * full name to {@link InterfaceDef} from {@link Parser}
         */
        private Map<String, InterfaceDef> originalInterfaces = new HashMap<>();
        /**
         * {@link SMethodDef} to it's containing statements
         */
        private Map<SMethodDef, List<Statement>> methodToStatements = new HashMap<>();
        /**
         * file name to Import info
         */
        private Map<String, List<Import>> fileNameToImport = new HashMap<>();
        /**
         * a set of types that should be return value of {@link #parse()} method.<br>
         * these types are to be compiled into byte codes
         */
        private final Set<STypeDef> typeDefSet = new HashSet<>();
        /**
         * invokable =&gt; (the-invokable-to-invoke =&gt; the current default parameter).
         */
        private Map<SInvokable, Map<SInvokable, Expression>> defaultParamInvokable = new HashMap<>();
        /**
         * retrieve existing classes from this class loader
         */
        private final ClassLoader classLoader;
        /**
         * error manager
         */
        private final ErrorManager err;
        /**
         * access which represents a type can be converted into instantiation
         */
        private boolean enableTypeAccess = true;

        /**
         * initialize the Processor
         *
         * @param mapOfStatements a map of fileName to statements
         * @param classLoader     retrieve loaded classes from this class loader
         * @param err             error manager. the fast fail would be set to `true`
         */
        public SemanticProcessor(Map<String, List<Statement>> mapOfStatements, ClassLoader classLoader, ErrorManager err) {
                this.mapOfStatements = mapOfStatements;
                this.classLoader = classLoader;
                this.err = err;
                err.setFastFail(true);
                // initiate types map
                // primitive and void
                types.put("int", IntTypeDef.get());
                types.put("long", LongTypeDef.get());
                types.put("double", DoubleTypeDef.get());
                types.put("float", FloatTypeDef.get());
                types.put("boolean", BoolTypeDef.get());
                types.put("bool", BoolTypeDef.get());
                types.put("char", CharTypeDef.get());
                types.put("short", ShortTypeDef.get());
                types.put("byte", ByteTypeDef.get());
                types.put("void", VoidType.get());
                types.put("Unit", VoidType.get());
        }

        /**
         * get the map of defined types.
         *
         * @return a map of name =&gt; type
         */
        public Map<String, STypeDef> getTypes() {
                return types;
        }

        /**
         * parse the input AST into STypeDef objects.<br>
         * the parsing process are divided into 4 steps.<br>
         * <ol>
         * <li><b>recording</b> : scan all classes and record them</li>
         * <li><b>signatures</b> : parse parents/superInterfaces, members, annotations, but don't parse statements or annotation values</li>
         * <li><b>validate</b> :
         * check circular inheritance and override,
         * check overload with super classes/interfaces and check whether the class overrides all methods from super interfaces/super abstract classes,
         * check @Override @FunctionalInterface @FunctionalAbstractClass,
         * check whether the class is a `data class` and do some modifications</li>
         * <li><b>parse</b> : parse annotation values and statements</li>
         * </ol>
         *
         * @return parsed types, including all inside members and statements
         * @throws SyntaxException compile error
         */
        public Set<STypeDef> parse() throws SyntaxException {
                Map<String, List<ClassDef>> fileNameToClassDef = new HashMap<>();
                Map<String, List<InterfaceDef>> fileNameToInterfaceDef = new HashMap<>();
                Map<String, List<FunDef>> fileNameToFunctions = new HashMap<>();
                Map<String, String> fileNameToPackageName = new HashMap<>();
                for (String fileName : mapOfStatements.keySet()) {
                        List<Statement> statements = mapOfStatements.get(fileName);
                        // package
                        String pkg; // if no package, then it's an empty string, otherwise, it's 'packageName.' with dot at the end
                        Iterator<Statement> statementIterator = statements.iterator();

                        // import
                        List<Import> imports = new ArrayList<>();
                        // class definition
                        List<ClassDef> classDefs = new ArrayList<>();
                        // interface definition
                        List<InterfaceDef> interfaceDefs = new ArrayList<>();
                        // fun definition
                        List<FunDef> funDefs = new ArrayList<>();

                        // put into map
                        fileNameToImport.put(fileName, imports);
                        fileNameToClassDef.put(fileName, classDefs);
                        fileNameToInterfaceDef.put(fileName, interfaceDefs);
                        fileNameToFunctions.put(fileName, funDefs);

                        if (statementIterator.hasNext()) {
                                Statement statement = statementIterator.next();
                                if (statement instanceof PackageDeclare) {
                                        PackageDeclare p = (PackageDeclare) statement;
                                        pkg = p.pkg.pkg.replace("::", ".") + ".";
                                } else {
                                        pkg = "";
                                        select_import_class_interface_fun(statement, imports, classDefs, interfaceDefs, funDefs);
                                }
                                while (statementIterator.hasNext()) {
                                        Statement stmt = statementIterator.next();
                                        select_import_class_interface_fun(stmt, imports, classDefs, interfaceDefs, funDefs);
                                }
                        } else {
                                // no statements,then continue
                                continue;
                        }

                        // add package into import list at index 0
                        imports.add(0, new Import(new AST.PackageRef(
                                pkg.endsWith(".")
                                        ? pkg.substring(0, pkg.length() - 1).replace(".", "::")
                                        : pkg
                                , LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                        // add java.lang into import list
                        // java::lang::_
                        // lt::lang::_
                        // lt::lang::Utils._
                        imports.add(new Import(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                        imports.add(new Import(new AST.PackageRef("java::lang", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Utils", LineCol.SYNTHETIC), true, LineCol.SYNTHETIC));

                        fileNameToPackageName.put(fileName, pkg);

                        Set<String> importSimpleNames = new HashSet<>();
                        for (Import i : imports) {
                                if (i.pkg == null && !i.importAll) {
                                        // class name are the same
                                        if (importSimpleNames.contains(i.access.name)) {
                                                err.SyntaxException("duplicate imports", i.line_col());
                                                return null;
                                        }
                                        importSimpleNames.add(i.access.name);
                                }
                        }
                        importSimpleNames.clear(); // release

                        // ======= step 1 =======
                        // record all classes and interfaces to compile
                        // classes and interfaces should be filled with
                        // package|fullName|modifiers
                        // in this step
                        for (ClassDef c : classDefs) {
                                String className = pkg + c.name;
                                // check occurrence
                                if (typeExists(className)) {
                                        err.SyntaxException("duplicate type names " + className, c.line_col());
                                        return null;
                                }

                                SClassDef sClassDef = new SClassDef(false, c.line_col());
                                sClassDef.setFullName(className);
                                sClassDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                                sClassDef.modifiers().add(SModifier.PUBLIC);

                                for (Modifier m : c.modifiers) {
                                        switch (m.modifier) {
                                                case ABSTRACT:
                                                        sClassDef.modifiers().add(SModifier.ABSTRACT);
                                                        break;
                                                case VAL:
                                                        sClassDef.modifiers().add(SModifier.FINAL);
                                                        break;
                                                case PUBLIC:
                                                case PRIVATE:
                                                case PROTECTED:
                                                case PKG:
                                                        // pub|pri|pro|pkg are for constructors
                                                        break;
                                                case DATA:
                                                        sClassDef.setIsDataClass(true);
                                                        break;
                                                default:
                                                        err.UnexpectedTokenException("valid modifier for class (val|abstract|public|private|protected|pkg)", m.toString(), m.line_col());
                                                        return null;
                                        }
                                }

                                types.put(className, sClassDef); // record the class
                                originalClasses.put(className, c);
                                typeDefSet.add(sClassDef);
                        }
                        for (InterfaceDef i : interfaceDefs) {
                                String interfaceName = pkg + i.name;
                                // check occurrence
                                if (typeExists(interfaceName)) {
                                        err.SyntaxException("duplicate type names " + interfaceName, i.line_col());
                                        return null;
                                }

                                SInterfaceDef sInterfaceDef = new SInterfaceDef(i.line_col());
                                sInterfaceDef.setFullName(interfaceName);
                                sInterfaceDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                                sInterfaceDef.modifiers().add(SModifier.PUBLIC);
                                sInterfaceDef.modifiers().add(SModifier.ABSTRACT);

                                for (Modifier m : i.modifiers) {
                                        switch (m.modifier) {
                                                case PUBLIC:
                                                case ABSTRACT:
                                                        // can only be abstract or public
                                                        break;
                                                default:
                                                        err.UnexpectedTokenException("valid modifier for interface (abs)", m.toString(), m.line_col());
                                                        return null;
                                        }
                                }

                                types.put(interfaceName, sInterfaceDef); // record the interface
                                originalInterfaces.put(interfaceName, i);
                                typeDefSet.add(sInterfaceDef);
                        }
                        for (FunDef f : funDefs) {
                                String className = pkg + f.name;
                                // check occurrence
                                if (typeExists(className)) {
                                        err.SyntaxException("duplicate type names " + className, f.line_col());
                                        return null;
                                }

                                SClassDef sClassDef = new SClassDef(true, f.line_col());
                                sClassDef.setFullName(className);
                                sClassDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                                sClassDef.modifiers().add(SModifier.PUBLIC);
                                sClassDef.modifiers().add(SModifier.FINAL);

                                types.put(className, sClassDef); // record the class
                                typeDefSet.add(sClassDef);
                        }

                        // all classes occurred in the parsing process will be inside `types` map or is already defined
                }

                step2(fileNameToClassDef, fileNameToInterfaceDef, fileNameToPackageName);
                step3(fileNameToPackageName, fileNameToFunctions);
                step4();

                return typeDefSet;
        }

        /**
         * ======= step 2 =======
         * build fields,methods,constructors,parameters,parent-classes,super-interfaces,annotations.
         * but no details (annotation's values|method statements|constructor statements won't be parsed)
         *
         * @param fileNameToClassDef     file name to class defintion
         * @param fileNameToInterfaceDef file name to interface definition
         * @param fileNameToPackageName  file name to package name
         * @throws SyntaxException exception
         */
        private void step2(Map<String, List<ClassDef>> fileNameToClassDef,
                           Map<String, List<InterfaceDef>> fileNameToInterfaceDef,
                           Map<String, String> fileNameToPackageName) throws SyntaxException {
                for (String fileName : mapOfStatements.keySet()) {
                        List<Import> imports = fileNameToImport.get(fileName);
                        List<ClassDef> classDefs = fileNameToClassDef.get(fileName);
                        List<InterfaceDef> interfaceDefs = fileNameToInterfaceDef.get(fileName);
                        String pkg = fileNameToPackageName.get(fileName);

                        for (ClassDef classDef : classDefs) {
                                SClassDef sClassDef = (SClassDef) types.get(pkg + classDef.name);

                                // parse parent
                                Iterator<AST.Access> superWithoutInvocationAccess;
                                if (classDef.superWithInvocation == null) {
                                        if (classDef.superWithoutInvocation.isEmpty()) {
                                                // no interfaces, no parent class
                                                sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", classDef.line_col()));
                                                superWithoutInvocationAccess = null;
                                        } else {
                                                superWithoutInvocationAccess = classDef.superWithoutInvocation.iterator();
                                                AST.Access mightBeClassAccess = superWithoutInvocationAccess.next();
                                                STypeDef tmp = getTypeWithAccess(mightBeClassAccess, imports);
                                                if (tmp instanceof SClassDef) {
                                                        // constructor without constructor invocation
                                                        sClassDef.setParent((SClassDef) tmp);
                                                } else if (tmp instanceof SInterfaceDef) {
                                                        // interface
                                                        sClassDef.superInterfaces().add((SInterfaceDef) tmp);
                                                        // set java.lang.Object as super class
                                                        sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", classDef.line_col()));
                                                } else {
                                                        err.SyntaxException(mightBeClassAccess.toString() + " is not class or interface",
                                                                mightBeClassAccess.line_col());
                                                        return;
                                                }
                                        }
                                } else {
                                        // super class
                                        if (!(classDef.superWithInvocation.exp instanceof AST.Access)) {
                                                throw new LtBug("classDef.superWithInvocation.exp should always be AST.Access");
                                        }

                                        AST.Access access = (AST.Access) classDef.superWithInvocation.exp;
                                        STypeDef tmp = getTypeWithAccess(access, imports);
                                        if (tmp instanceof SClassDef) {
                                                sClassDef.setParent((SClassDef) tmp);
                                        } else {
                                                err.SyntaxException(access.toString() + " is not class or interface",
                                                        access.line_col());
                                                return;
                                        }
                                        superWithoutInvocationAccess = classDef.superWithoutInvocation.iterator();
                                }
                                // interfaces to be parsed
                                while (superWithoutInvocationAccess != null && superWithoutInvocationAccess.hasNext()) {
                                        AST.Access interfaceAccess = superWithoutInvocationAccess.next();
                                        STypeDef tmp = getTypeWithAccess(interfaceAccess, imports);
                                        if (tmp instanceof SInterfaceDef) {
                                                sClassDef.superInterfaces().add((SInterfaceDef) tmp);
                                        } else {
                                                err.SyntaxException(interfaceAccess.toString() + " is not interface",
                                                        interfaceAccess.line_col());
                                                return;
                                        }
                                }

                                // annos
                                parseAnnos(classDef.annos, sClassDef, imports, ElementType.TYPE,
                                        Collections.singletonList(ElementType.CONSTRUCTOR));

                                // parse constructor
                                int generateIndex = -1;
                                for (VariableDef v : classDef.params) {
                                        if (v.getInit() == null) {
                                                ++generateIndex;
                                        } else break;
                                }

                                SConstructorDef lastConstructor = null;
                                for (int i = classDef.params.size(); i > generateIndex; --i) {
                                        // generate constructors
                                        // these constructors will call the constructor that has length+1 parameter length constructor
                                        // e.g.
                                        // class Cls(a,b=1)
                                        // will be parsed into
                                        // public class Cls{
                                        //      public Cls(a){
                                        //              this(a,1);
                                        //      }
                                        //      public Cls(a,b){
                                        //              ...
                                        //      }
                                        // }
                                        // however the statements won't be filled in this step
                                        SConstructorDef constructor = new SConstructorDef(classDef.line_col());

                                        // constructor should be filled with
                                        // parameters|declaringType(class)|modifiers
                                        // in this step

                                        constructor.setDeclaringType(sClassDef);

                                        // annotation
                                        parseAnnos(classDef.annos, constructor, imports, ElementType.CONSTRUCTOR,
                                                Collections.singletonList(ElementType.TYPE));

                                        // modifier
                                        boolean hasAccessModifier = false;
                                        for (Modifier m : classDef.modifiers) {
                                                if (m.modifier.equals(Modifier.Available.PUBLIC)
                                                        || m.modifier.equals(Modifier.Available.PRIVATE)
                                                        || m.modifier.equals(Modifier.Available.PROTECTED)
                                                        || m.modifier.equals(Modifier.Available.PKG)) {
                                                        hasAccessModifier = true;
                                                }
                                        }
                                        if (!hasAccessModifier) {
                                                constructor.modifiers().add(SModifier.PUBLIC);
                                        }
                                        for (Modifier m : classDef.modifiers) {
                                                switch (m.modifier) {
                                                        case PUBLIC:
                                                                constructor.modifiers().add(SModifier.PUBLIC);
                                                                break;
                                                        case PRIVATE:
                                                                constructor.modifiers().add(SModifier.PRIVATE);
                                                                break;
                                                        case PROTECTED:
                                                                constructor.modifiers().add(SModifier.PROTECTED);
                                                                break;
                                                        case VAL:
                                                        case PKG:
                                                        case DATA:
                                                        case ABSTRACT:
                                                                // data, val and abs are presented on class
                                                                break; // pkg don't need to sign modifier
                                                        default:
                                                                err.UnexpectedTokenException("valid constructor modifier (public|private|protected|pkg)", m.toString(), m.line_col());
                                                                return;
                                                }
                                        }

                                        // parameters
                                        parseParameters(classDef.params, i, constructor, imports, true);

                                        constructor.setDeclaringType(sClassDef);
                                        sClassDef.constructors().add(constructor);

                                        if (lastConstructor != null) {
                                                // record the constructor and expressions
                                                Map<SInvokable, Expression> invoke = new HashMap<>();
                                                invoke.put(lastConstructor, classDef.params.get(i).getInit());
                                                defaultParamInvokable.put(constructor, invoke);
                                        }
                                        lastConstructor = constructor;
                                }
                                // constructor finished

                                // parse field from constructor parameters
                                for (VariableDef v : classDef.params) {
                                        parseField(v, sClassDef, imports, PARSING_CLASS, false);
                                }

                                // get static scope and parse non-static fields/methods
                                List<AST.StaticScope> staticScopes = new ArrayList<>();
                                for (Statement stmt : classDef.statements) {
                                        if (stmt instanceof AST.StaticScope) {
                                                staticScopes.add((AST.StaticScope) stmt);
                                        } else if (stmt instanceof VariableDef) {
                                                // define a non-static field
                                                parseField((VariableDef) stmt, sClassDef, imports, PARSING_CLASS, false);
                                        } else if (stmt instanceof MethodDef) {
                                                // define a non-static method
                                                MethodDef methodDef = (MethodDef) stmt;
                                                generateIndex = -1;
                                                for (VariableDef v : methodDef.params) {
                                                        if (v.getInit() == null) {
                                                                ++generateIndex;
                                                        } else break;
                                                }

                                                SMethodDef lastMethod = null;
                                                for (int i = methodDef.params.size(); i > generateIndex; --i) {
                                                        parseMethod((MethodDef) stmt, i, sClassDef, lastMethod, imports, PARSING_CLASS, false);
                                                        lastMethod = sClassDef.methods().get(sClassDef.methods().size() - 1);

                                                        // record the method
                                                        methodToStatements.put(lastMethod, methodDef.body);
                                                }
                                        }
                                }
                                // get static field and methods
                                for (AST.StaticScope scope : staticScopes) {
                                        for (Statement stmt : scope.statements) {
                                                if (stmt instanceof VariableDef) {
                                                        // define a static field
                                                        parseField((VariableDef) stmt, sClassDef, imports, PARSING_CLASS, true);
                                                } else if (stmt instanceof MethodDef) {
                                                        // define a static method
                                                        MethodDef methodDef = (MethodDef) stmt;
                                                        generateIndex = -1;
                                                        for (VariableDef v : methodDef.params) {
                                                                if (v.getInit() == null) {
                                                                        ++generateIndex;
                                                                } else break;
                                                        }

                                                        SMethodDef lastMethod = null;
                                                        for (int i = methodDef.params.size(); i > generateIndex; --i) {
                                                                parseMethod((MethodDef) stmt, i, sClassDef, lastMethod, imports, PARSING_CLASS, true);
                                                                lastMethod = sClassDef.methods().get(sClassDef.methods().size() - 1);

                                                                // record the method
                                                                methodToStatements.put(lastMethod, methodDef.body);
                                                        }
                                                }
                                        }
                                }
                        }
                        for (InterfaceDef interfaceDef : interfaceDefs) {
                                SInterfaceDef sInterfaceDef = (SInterfaceDef) types.get(pkg + interfaceDef.name);

                                // parse super interfaces
                                for (AST.Access access : interfaceDef.superInterfaces) {
                                        SInterfaceDef superInterface = (SInterfaceDef) getTypeWithAccess(access, imports);
                                        sInterfaceDef.superInterfaces().add(superInterface);
                                }

                                // parse annos
                                parseAnnos(interfaceDef.annos, sInterfaceDef, imports, ElementType.TYPE, Collections.emptyList());

                                // parse fields and methods
                                List<AST.StaticScope> staticScopes = new ArrayList<>();
                                for (Statement stmt : interfaceDef.statements) {
                                        if (stmt instanceof VariableDef) {
                                                parseField((VariableDef) stmt, sInterfaceDef, imports, PARSING_INTERFACE, false);
                                        } else if (stmt instanceof MethodDef) {
                                                MethodDef m = (MethodDef) stmt;
                                                int generateIndex = -1;
                                                for (VariableDef param : m.params) {
                                                        if (param.getInit() == null) {
                                                                ++generateIndex;
                                                        } else break;
                                                }

                                                SMethodDef lastMethod = null;
                                                for (int i = m.params.size(); i > generateIndex; --i) {
                                                        parseMethod(m, i, sInterfaceDef, lastMethod, imports, PARSING_INTERFACE, false);
                                                        lastMethod = sInterfaceDef.methods().get(sInterfaceDef.methods().size() - 1);

                                                        // record the method
                                                        methodToStatements.put(lastMethod, m.body);
                                                }

                                        } else if (stmt instanceof AST.StaticScope) {
                                                staticScopes.add((AST.StaticScope) stmt);
                                        } else {
                                                err.SyntaxException("interfaces don't have initiators", stmt.line_col());
                                                return;
                                        }
                                }
                                for (AST.StaticScope staticScope : staticScopes) {
                                        for (Statement stmt : staticScope.statements) {
                                                if (stmt instanceof VariableDef) {
                                                        parseField((VariableDef) stmt, sInterfaceDef, imports, PARSING_INTERFACE, true);
                                                } else if (stmt instanceof MethodDef) {
                                                        MethodDef m = (MethodDef) stmt;
                                                        int generateIndex = -1;
                                                        for (VariableDef param : m.params) {
                                                                if (param.getInit() == null) {
                                                                        ++generateIndex;
                                                                } else break;
                                                        }

                                                        SMethodDef lastMethod = null;
                                                        for (int i = m.params.size(); i > generateIndex; --i) {
                                                                parseMethod(m, i, sInterfaceDef, lastMethod, imports, PARSING_INTERFACE, true);
                                                                lastMethod = sInterfaceDef.methods().get(sInterfaceDef.methods().size() - 1);

                                                                // record the method
                                                                methodToStatements.put(lastMethod, m.body);
                                                        }

                                                } else {
                                                        err.SyntaxException("interfaces don't have initiators", stmt.line_col());
                                                        return;
                                                }
                                        }
                                }
                        }
                }
        }

        /**
         * ========step 3========
         * check circular inheritance
         *
         * @param fileNameToPackageName file name to package name
         * @param fileNameToFunctions   file name to functions
         * @throws SyntaxException exception
         */
        private void step3(Map<String, String> fileNameToPackageName,
                           Map<String, List<FunDef>> fileNameToFunctions) throws SyntaxException {
                for (STypeDef sTypeDef : typeDefSet) {
                        if (sTypeDef instanceof SClassDef) {
                                List<STypeDef> circularRecorder = new ArrayList<>();
                                SClassDef parent = ((SClassDef) sTypeDef).parent();
                                while (parent != null) {
                                        circularRecorder.add(parent);
                                        if (parent.equals(sTypeDef)) {
                                                err.SyntaxException("circular inheritance " + circularRecorder, LineCol.SYNTHETIC);
                                                return;
                                        }
                                        parent = parent.parent();
                                }
                                circularRecorder.clear();
                        } else if (sTypeDef instanceof SInterfaceDef) {
                                SInterfaceDef i = (SInterfaceDef) sTypeDef;
                                checkInterfaceCircularInheritance(i, i.superInterfaces(), new ArrayList<>());
                        } else {
                                throw new LtBug("wrong STypeDefType " + sTypeDef.getClass());
                        }
                }
                // check override and overload with super methods
                for (STypeDef sTypeDef : typeDefSet) {
                        checkOverrideAllMethods(sTypeDef);
                }

                // after the override check are done, try to get signatures of functions.
                for (String fileName : mapOfStatements.keySet()) {
                        List<Import> imports = fileNameToImport.get(fileName);
                        String pkg = fileNameToPackageName.get(fileName);
                        List<FunDef> functionDefs = fileNameToFunctions.get(fileName);
                        for (FunDef fun : functionDefs) {
                                // get super class/interface
                                STypeDef type = getTypeWithAccess(fun.superType, imports);
                                if (!(type instanceof SClassDef || type instanceof SInterfaceDef)) {
                                        err.SyntaxException("function super type should be functional interfaces or functional abstract classes", fun.superType.line_col());
                                        return;
                                }
                                SConstructorDef[] zeroParamConstructor = new SConstructorDef[1];
                                SMethodDef[] methodToOverride = new SMethodDef[1];
                                if (!getMethodForLambda(type, zeroParamConstructor, methodToOverride)) {
                                        err.SyntaxException("function super type should be functional interfaces or functional abstract classes", fun.superType.line_col());
                                        return;
                                }

                                // class and the annos, super class
                                SClassDef sClassDef = (SClassDef) types.get(pkg + fun.name);
                                parseAnnos(fun.annos, sClassDef, imports, ElementType.TYPE, Arrays.asList(ElementType.METHOD, ElementType.CONSTRUCTOR));
                                if (zeroParamConstructor[0] == null) {
                                        sClassDef.setParent(getObject_Class());
                                        assert type instanceof SInterfaceDef;
                                        sClassDef.superInterfaces().add((SInterfaceDef) type);
                                } else {
                                        sClassDef.setParent((SClassDef) zeroParamConstructor[0].declaringType());
                                }

                                // constructors (fill statements directly)
                                SConstructorDef cons = new SConstructorDef(LineCol.SYNTHETIC);
                                parseAnnos(fun.annos, cons, imports, ElementType.CONSTRUCTOR, Arrays.asList(ElementType.TYPE, ElementType.METHOD));
                                cons.setDeclaringType(sClassDef);
                                sClassDef.constructors().add(cons);
                                if (zeroParamConstructor[0] == null) {
                                        zeroParamConstructor[0] = getObject_Class().constructors().get(0);
                                }
                                cons.statements().add(new Ins.InvokeSpecial(new Ins.This(sClassDef), zeroParamConstructor[0], LineCol.SYNTHETIC));
                                cons.modifiers().add(SModifier.PUBLIC);

                                // method name, declaringType, return type, params
                                SMethodDef method = new SMethodDef(LineCol.SYNTHETIC);
                                method.setDeclaringType(sClassDef);
                                method.setReturnType(methodToOverride[0].getReturnType());
                                method.setName(methodToOverride[0].name());
                                sClassDef.methods().add(method);
                                parseAnnos(fun.annos, method, imports, ElementType.METHOD, Arrays.asList(ElementType.TYPE, ElementType.CONSTRUCTOR));
                                method.modifiers().add(SModifier.PUBLIC);

                                // fill parameters
                                parseParameters(fun.params, fun.params.size(), method, imports, false);

                                methodToStatements.put(method, fun.statements);

                                // check signature
                                checkOverrideAllMethods(sClassDef);
                        }
                }

                // check annotation (@FunctionalInterface @FunctionalAbstractClass)
                for (STypeDef typeDef : typeDefSet) {
                        for (SAnno anno : typeDef.annos()) {
                                if (anno.type().fullName().equals("java.lang.FunctionalInterface")) {
                                        final String msg = typeDef + " is not a functional interface";
                                        if (typeDef instanceof SInterfaceDef) {
                                                if (!getMethodForLambda(typeDef, new SConstructorDef[1], new SMethodDef[1])) {
                                                        err.SyntaxException(msg, typeDef.line_col());
                                                        return;
                                                }
                                        } else {
                                                err.SyntaxException(msg, typeDef.line_col());
                                                return;
                                        }
                                } else if (anno.type().fullName().equals("lt.lang.FunctionalAbstractClass")) {
                                        final String msg = typeDef + " is not a functional abstract class";
                                        if (typeDef instanceof SClassDef) {
                                                if (!getMethodForLambda(typeDef, new SConstructorDef[1], new SMethodDef[1])) {
                                                        err.SyntaxException(msg, typeDef.line_col());
                                                        return;
                                                }
                                        } else {
                                                err.SyntaxException(msg, typeDef.line_col());
                                                return;
                                        }
                                }
                        }
                        List<SMethodDef> methods;
                        if (typeDef instanceof SClassDef) methods = ((SClassDef) typeDef).methods();
                        else methods = ((SInterfaceDef) typeDef).methods();

                        for (SMethodDef method : methods) {
                                for (SAnno anno : method.annos()) {
                                        if (anno.type().fullName().equals("java.lang.Override")) {
                                                if (method.overRide().isEmpty()) {
                                                        err.SyntaxException(method + " doesn't override any method", method.line_col());
                                                        return;
                                                }
                                        }
                                }
                        }
                }

                // data class
                for (STypeDef typeDef : typeDefSet) {
                        if (typeDef instanceof SClassDef) {
                                SClassDef cls = (SClassDef) typeDef;
                                if (cls.isDataClass()) {
                                        fillMethodsIntoDataClass(cls);
                                }
                        }
                }
        }

        /**
         * ========step 4========
         * first parse anno types
         * the annotations presented on these anno types will also be parsed
         *
         * @throws SyntaxException exception
         */
        private void step4() throws SyntaxException {
                for (STypeDef typeDef : types.values()) {
                        if (typeDef instanceof SAnnoDef) {
                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                Class<?> cls;
                                try {
                                        cls = loadClass(annoDef.fullName());
                                } catch (ClassNotFoundException e) {
                                        throw new LtBug(e);
                                }
                                // parse field default values
                                for (SAnnoField f : annoDef.annoFields()) {
                                        try {
                                                Method annoM = cls.getDeclaredMethod(f.name());
                                                try {
                                                        Object o = annoM.getDefaultValue();
                                                        if (null != o) {
                                                                Value value = parseValueFromObject(o);
                                                                f.setDefaultValue(value);
                                                        }
                                                } catch (TypeNotPresentException ignore) {
                                                }
                                        } catch (NoSuchMethodException e) {
                                                throw new LtBug(e);
                                        }
                                }
                                // parse annotations presented on this type
                                parseAnnoValues(annoDef.annos());
                        }
                }
                // then
                // foreach typeDefSet, parse their statements
                List<STypeDef> typeDefList = new ArrayList<>(typeDefSet);
                for (STypeDef sTypeDef : typeDefList) {
                        if (sTypeDef instanceof SClassDef) {
                                SClassDef sClassDef = (SClassDef) sTypeDef;
                                ClassDef astClass = originalClasses.get(sClassDef.fullName());

                                parseAnnoValues(sClassDef.annos());

                                // initiate the type scope
                                SemanticScope scope = new SemanticScope(sTypeDef);

                                // parse constructors
                                for (SConstructorDef constructorToFillStatements : sClassDef.constructors()) {
                                        // if is not empty then continue
                                        if (!constructorToFillStatements.statements().isEmpty())
                                                continue;
                                        // initiate constructor scope
                                        SemanticScope constructorScope = new SemanticScope(scope);
                                        constructorScope.setThis(new Ins.This(sTypeDef)); // set `this`
                                        for (SParameter param : constructorToFillStatements.getParameters()) {
                                                constructorScope.putLeftValue(param.name(), param);
                                        }

                                        if (defaultParamInvokable.containsKey(constructorToFillStatements)) {
                                                fillDefaultParamMethod(constructorToFillStatements, constructorScope);
                                        } else {
                                                // parse invoke super constructor statement
                                                SClassDef parent = sClassDef.parent();
                                                Ins.InvokeSpecial invokeConstructor = null;
                                                if (null == astClass.superWithInvocation) {
                                                        // invoke super();
                                                        for (SConstructorDef cons : parent.constructors()) {
                                                                if (cons.getParameters().size() == 0) {
                                                                        invokeConstructor = new Ins.InvokeSpecial(new Ins.This(sClassDef), cons,
                                                                                astClass.line_col());
                                                                        break;
                                                                }
                                                        }
                                                } else {
                                                        // invoke super with args
                                                        for (SConstructorDef cons : parent.constructors()) {
                                                                if (cons.getParameters().size() == astClass.superWithInvocation.args.size()) {
                                                                        invokeConstructor = new Ins.InvokeSpecial(new Ins.This(sClassDef), cons,
                                                                                astClass.superWithInvocation.line_col());

                                                                        List<SParameter> parameters = cons.getParameters();
                                                                        List<Expression> args = astClass.superWithInvocation.args;
                                                                        for (int i = 0; i < parameters.size(); ++i) {
                                                                                Value v = parseValueFromExpression(args.get(i), parameters.get(i).type(), constructorScope);
                                                                                invokeConstructor.arguments().add(v);
                                                                        }
                                                                        break;
                                                                }
                                                        }
                                                }
                                                if (null == invokeConstructor) {
                                                        err.SyntaxException("no suitable super constructor to invoke in " + sClassDef, sClassDef.line_col());
                                                        return;
                                                }
                                                constructorToFillStatements.statements().add(invokeConstructor);

                                                // put field
                                                for (SParameter param : constructorToFillStatements.getParameters()) {
                                                        SFieldDef f = null;
                                                        for (SFieldDef field : sClassDef.fields()) {
                                                                if (field.name().equals(param.name())) {
                                                                        f = field;
                                                                        break;
                                                                }
                                                        }
                                                        if (f == null) throw new LtBug("f should not be null");

                                                        Ins.PutField putField = new Ins.PutField(f, constructorScope.getThis(),
                                                                new Ins.TLoad(param, constructorScope, LineCol.SYNTHETIC), LineCol.SYNTHETIC, err);
                                                        constructorToFillStatements.statements().add(putField);
                                                }

                                                // a new constructor scope
                                                // the parameters are ignored and all variables are fields
                                                constructorScope = new SemanticScope(scope);
                                                constructorScope.setThis(new Ins.This(sTypeDef)); // set `this`
                                                for (SParameter param : constructorToFillStatements.getParameters()) {
                                                        constructorScope.putLeftValue(constructorScope.generateTempName(), param);
                                                }

                                                // parse this constructor
                                                for (Statement stmt : astClass.statements) {
                                                        parseStatement(
                                                                stmt,
                                                                VoidType.get(),
                                                                constructorScope,
                                                                constructorToFillStatements.statements(),
                                                                constructorToFillStatements.exceptionTables(),
                                                                null, null,
                                                                true);
                                                }
                                        }
                                }

                                // parse method
                                // use traditional for loop because the method list might be modified
                                int methodSize = sClassDef.methods().size();
                                List<SMethodDef> methods = sClassDef.methods();
                                for (int i = 0; i < methodSize; i++) {
                                        SMethodDef method = methods.get(i);
                                        parseAnnoValues(method.annos());
                                        parseMethod(method, methodToStatements.get(method), scope);
                                }

                                // astClass == null means it's function instead of normal class
                                if (astClass != null) {
                                        // parse static
                                        SemanticScope staticScope = new SemanticScope(scope);
                                        for (Statement statement : astClass.statements) {
                                                if (statement instanceof AST.StaticScope) {
                                                        AST.StaticScope sta = (AST.StaticScope) statement;
                                                        for (Statement stmt : sta.statements) {
                                                                parseStatement(
                                                                        stmt,
                                                                        VoidType.get(),
                                                                        staticScope,
                                                                        sClassDef.staticStatements(),
                                                                        sClassDef.staticExceptionTable(),
                                                                        null, null,
                                                                        true);
                                                        }
                                                }
                                        }
                                }
                        } else if (sTypeDef instanceof SInterfaceDef) {
                                SInterfaceDef sInterfaceDef = (SInterfaceDef) sTypeDef;
                                InterfaceDef astInterface = originalInterfaces.get(sInterfaceDef.fullName());

                                parseAnnoValues(sInterfaceDef.annos());

                                SemanticScope scope = new SemanticScope(sInterfaceDef);

                                // parse method
                                // use traditional for loop because the method list might be modified
                                int methodSize = sInterfaceDef.methods().size();
                                List<SMethodDef> methods = sInterfaceDef.methods();
                                for (int i = 0; i < methodSize; ++i) {
                                        SMethodDef method = methods.get(i);
                                        parseMethod(method, methodToStatements.get(method), scope);
                                }

                                // parse static
                                SemanticScope staticScope = new SemanticScope(scope);
                                for (Statement statement : astInterface.statements) {
                                        parseStatement(
                                                statement,
                                                VoidType.get(),
                                                staticScope,
                                                sInterfaceDef.staticStatements(),
                                                sInterfaceDef.staticExceptionTable(),
                                                null, null,
                                                true);
                                }
                        } else throw new LtBug("wrong STypeDefType " + sTypeDef.getClass());
                }
        }

        /**
         * check whether overrides all methods from super
         *
         * @param sTypeDef sTypeDef
         * @throws SyntaxException exception
         */
        private void checkOverrideAllMethods(STypeDef sTypeDef) throws SyntaxException {
                checkOverride(sTypeDef);

                // check whether overrides all methods from super
                if (sTypeDef instanceof SClassDef) {
                        SClassDef c = (SClassDef) sTypeDef;
                        if (c.modifiers().contains(SModifier.ABSTRACT)) return;

                        // check all abstract methods
                        // record them
                        List<SMethodDef> abstractMethods = new ArrayList<>();
                        recordAbstractMethodsForOverrideCheck(c, abstractMethods);

                        // do check
                        for (SMethodDef m : abstractMethods) {
                                boolean found = false;
                                for (SMethodDef overridden : m.overridden()) {
                                        if (overridden.declaringType().equals(c)) {
                                                found = true;
                                                break;
                                        }
                                }
                                if (!found) {
                                        err.SyntaxException(m + " is not overridden in " + c, c.line_col());
                                        return;
                                }
                        }
                }
        }

        /**
         * fill statements into default param invokable.
         *
         * @param invokable the invokable to fill
         * @param scope     the invokable scope
         * @throws SyntaxException exception
         */
        private void fillDefaultParamMethod(SInvokable invokable, SemanticScope scope) throws SyntaxException {
                Map<SInvokable, Expression> invokePair = defaultParamInvokable.get(invokable);
                SInvokable methodToInvoke = invokePair.keySet().iterator().next();
                Expression arg = invokePair.get(methodToInvoke);
                if (invokable instanceof SConstructorDef) {
                        // invoke another constructor
                        Ins.InvokeSpecial invoke = new Ins.InvokeSpecial(scope.getThis(), methodToInvoke, LineCol.SYNTHETIC);
                        int count = 1;
                        for (SParameter p : invokable.getParameters()) {
                                invoke.arguments().add(new Ins.TLoad(p, count++, LineCol.SYNTHETIC));
                        }
                        List<SParameter> paramsOfLast = methodToInvoke.getParameters();
                        invoke.arguments().add(parseValueFromExpression(arg, paramsOfLast.get(paramsOfLast.size() - 1).type(),
                                scope));

                        invokable.statements().add(invoke);
                } else {
                        assert invokable instanceof SMethodDef;
                        SMethodDef methodDef = (SMethodDef) invokable;
                        SMethodDef lastMethod = (SMethodDef) methodToInvoke;
                        boolean isStatic = lastMethod.modifiers().contains(SModifier.STATIC);

                        Ins.Invoke invoke;
                        if (lastMethod.modifiers().contains(SModifier.PRIVATE)) {
                                invoke = new Ins.InvokeSpecial(new Ins.This(methodDef.declaringType()), lastMethod, LineCol.SYNTHETIC);
                        } else if (isStatic) {
                                invoke = new Ins.InvokeStatic(lastMethod, LineCol.SYNTHETIC);
                        } else {
                                invoke = new Ins.InvokeVirtual(new Ins.This(methodDef.declaringType()), lastMethod, LineCol.SYNTHETIC);
                        }
                        for (int ii = 0; ii < methodDef.getParameters().size(); ++ii) {
                                // load arguments
                                invoke.arguments().add(new Ins.TLoad(methodDef.getParameters().get(ii), ii + (isStatic ? 0 : 1), LineCol.SYNTHETIC));
                        }
                        List<SParameter> lastParams = lastMethod.getParameters();
                        invoke.arguments().add(parseValueFromExpression(arg, lastParams.get(lastParams.size() - 1).type(), null));
                        if (methodDef.getReturnType().equals(VoidType.get())) {
                                methodDef.statements().add(invoke);
                        } else {
                                methodDef.statements().add(new Ins.TReturn(invoke, LineCol.SYNTHETIC));
                        }
                }
        }

        /**
         * override {@link Object#toString()} {@link Object#hashCode()} {@link Object#equals(Object)}
         * and generate g/setters for the data class
         *
         * @param cls the class should be data class
         * @throws SyntaxException compiling error
         */
        private void fillMethodsIntoDataClass(SClassDef cls) throws SyntaxException {
                // check parameter modifiers
                // cannot be `val`
                for (SParameter p : cls.constructors().get(0).getParameters()) {
                        if (!p.canChange()) {
                                err.SyntaxException("data class cannot have `val` parameters", cls.line_col());
                                return;
                        }
                }

                Map<SFieldDef, SMethodDef> setters = new HashMap<>();
                Map<SFieldDef, SMethodDef> getters = new HashMap<>();
                SMethodDef toStringOverride = null;
                SMethodDef equalsOverride = null;
                SMethodDef hashCodeOverride = null;
                SConstructorDef zeroParamCons = null;

                // get existing setters
                for (SFieldDef f : cls.fields()) {
                        if (f.modifiers().contains(SModifier.STATIC)) continue;
                        String name = f.name();
                        String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals(setterName)
                                        &&
                                        m.getParameters().size() == 1
                                        &&
                                        m.getParameters().get(0).type().equals(f.type())) {
                                        setters.put(f, m);
                                }
                                if (m.name().equals(getterName)
                                        &&
                                        m.getParameters().size() == 0) {
                                        getters.put(f, m);
                                }
                        }
                }

                // get existing equals(o)/toString()/hashCode()
                for (SMethodDef m : cls.methods()) {
                        if (m.name().equals("toString") && m.getParameters().size() == 0) toStringOverride = m;
                        if (m.name().equals("equals")
                                && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().fullName().equals("java.lang.Object"))
                                equalsOverride = m;
                        if (m.name().equals("hashCode") && m.getParameters().size() == 0) hashCodeOverride = m;
                }

                // get existing zero param constructor
                for (SConstructorDef con : cls.constructors()) {
                        if (con.getParameters().isEmpty()) {
                                zeroParamCons = con;
                                break;
                        }
                }

                SemanticScope scope = new SemanticScope(cls);
                scope.setThis(new Ins.This(cls));

                String className = cls.fullName();
                String simpleName = className.contains(".") ? className.substring(className.lastIndexOf(".") + 1) : className;

                LineCol lineCol = new LineCol(cls.line_col().fileName, 0, 0);

                if (toStringOverride == null) {
                        // toString():String
                        //     return "SimpleName("+
                        //     "field="+field+
                        //     ", field2="+field2+
                        //     ...+
                        //     ")"
                        toStringOverride = new SMethodDef(lineCol);
                        toStringOverride.setName("toString");
                        toStringOverride.setDeclaringType(cls);
                        cls.methods().add(toStringOverride);
                        toStringOverride.setReturnType(getTypeWithName("java.lang.String", lineCol));
                        toStringOverride.modifiers().add(SModifier.PUBLIC);

                        // SimpleName(
                        Expression lastExp = new StringLiteral("\"" + simpleName + "(\"", lineCol);
                        // fields
                        boolean isFirst = true;
                        for (SFieldDef f : cls.fields()) {
                                if (f.modifiers().contains(SModifier.STATIC)) continue;
                                String literal = "";
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        literal = ", ";
                                }
                                literal += (f.name() + "=");

                                StringLiteral s = new StringLiteral("\"" + literal + "\"", lineCol);
                                lastExp = new TwoVariableOperation(
                                        "+", lastExp, s, lineCol
                                );
                                lastExp = new TwoVariableOperation(
                                        "+", lastExp, new AST.Access(
                                        new AST.Access(null, "this", lineCol),
                                        f.name(), lineCol),
                                        lineCol
                                );
                        }
                        // )
                        StringLiteral s2 = new StringLiteral("\")\"", lineCol);
                        lastExp = new TwoVariableOperation(
                                "+", lastExp, s2, lineCol
                        );


                        Statement stmt = new AST.Return(lastExp, lineCol);

                        parseStatement(stmt, toStringOverride.getReturnType(),
                                new SemanticScope(scope),
                                toStringOverride.statements(), toStringOverride.exceptionTables(),
                                null, null, false);
                }

                if (hashCodeOverride == null) {
                        // hashCode():int
                        //     return field1 as int +
                        //     field2 as int +
                        //     ...
                        hashCodeOverride = new SMethodDef(lineCol);
                        hashCodeOverride.setName("hashCode");
                        hashCodeOverride.setDeclaringType(cls);
                        cls.methods().add(hashCodeOverride);
                        hashCodeOverride.setReturnType(IntTypeDef.get());
                        hashCodeOverride.modifiers().add(SModifier.PUBLIC);

                        Expression lastExp;
                        if (cls.fields().isEmpty()) {
                                lastExp = new NumberLiteral("0", lineCol);
                        } else {
                                Iterator<SFieldDef> it = cls.fields().iterator();
                                // LtRuntime.getHashCode(this.field)
                                lastExp = null;
                                while (it.hasNext()) {
                                        SFieldDef f = it.next();
                                        if (f.modifiers().contains(SModifier.STATIC)) continue;

                                        Expression exp = new AST.Invocation(
                                                new AST.Access(
                                                        new AST.Access(
                                                                new AST.PackageRef("lt::lang", lineCol),
                                                                "LtRuntime",
                                                                lineCol
                                                        ),
                                                        "getHashCode",
                                                        lineCol
                                                ),
                                                Collections.singletonList(
                                                        new AST.Access(
                                                                new AST.Access(
                                                                        null, "this", lineCol
                                                                ),
                                                                f.name(),
                                                                lineCol
                                                        )
                                                ),
                                                false,
                                                lineCol
                                        );

                                        if (lastExp == null) {
                                                lastExp = exp;
                                        } else {
                                                lastExp =
                                                        new TwoVariableOperation(
                                                                "+",
                                                                lastExp,
                                                                exp, lineCol
                                                        );
                                        }
                                }
                        }

                        if (lastExp == null) {
                                lastExp = new NumberLiteral("0", lineCol);
                        }

                        Statement stmt = new AST.Return(lastExp, lineCol);

                        parseStatement(stmt, hashCodeOverride.getReturnType(),
                                new SemanticScope(scope),
                                hashCodeOverride.statements(), hashCodeOverride.exceptionTables(),
                                null, null, false);
                }

                if (equalsOverride == null) {
                        // equals(o):bool
                        //     return o is type CurrentType and
                        //     o.field1 is this.field1 and
                        //     o.field2 is this.field2
                        //     ...
                        equalsOverride = new SMethodDef(lineCol);
                        equalsOverride.setName("equals");
                        equalsOverride.setDeclaringType(cls);
                        cls.methods().add(equalsOverride);
                        equalsOverride.setReturnType(BoolTypeDef.get());
                        SParameter o = new SParameter();
                        o.setTarget(equalsOverride);
                        equalsOverride.getParameters().add(o);
                        o.setName("o");
                        o.setType(getTypeWithName("java.lang.Object", lineCol));
                        equalsOverride.modifiers().add(SModifier.PUBLIC);

                        // o is type CurrentType
                        Expression lastExp = new TwoVariableOperation(
                                "is",
                                new AST.Access(null, "o", lineCol),
                                new AST.TypeOf(
                                        new AST.Access(null, simpleName, lineCol),
                                        lineCol
                                ),
                                lineCol
                        );
                        for (SFieldDef f : cls.fields()) {
                                if (f.modifiers().contains(SModifier.STATIC)) continue;
                                lastExp = new TwoVariableOperation(
                                        "and",
                                        lastExp,
                                        // o.field is this.field
                                        new TwoVariableOperation(
                                                "is",
                                                new AST.Access(
                                                        new AST.Access(
                                                                null, "o", lineCol
                                                        ),
                                                        f.name(),
                                                        lineCol
                                                ),
                                                new AST.Access(
                                                        new AST.Access(
                                                                null, "this", lineCol
                                                        ),
                                                        f.name(),
                                                        lineCol
                                                ),
                                                lineCol
                                        ),
                                        lineCol
                                );
                        }

                        Statement stmt = new AST.Return(lastExp, lineCol);
                        SemanticScope equalsScope = new SemanticScope(scope);
                        equalsScope.putLeftValue("o", o);
                        parseStatement(stmt, equalsOverride.getReturnType(),
                                equalsScope,
                                equalsOverride.statements(), equalsOverride.exceptionTables(),
                                null, null, false);
                }
                if (zeroParamCons == null) {
                        zeroParamCons = new SConstructorDef(lineCol);
                        zeroParamCons.setDeclaringType(cls);
                        cls.constructors().add(zeroParamCons);
                        zeroParamCons.modifiers().add(SModifier.PUBLIC);

                        SConstructorDef con = cls.constructors().get(cls.constructors().size() - 2);
                        List<Value> initValues = new ArrayList<>();
                        for (SParameter p : con.getParameters()) {
                                if (p.type().equals(IntTypeDef.get())) {
                                        initValues.add(new IntValue(0));
                                } else if (p.type().equals(ShortTypeDef.get())) {
                                        initValues.add(new ShortValue((short) 0));
                                } else if (p.type().equals(ByteTypeDef.get())) {
                                        initValues.add(new ByteValue((byte) 0));
                                } else if (p.type().equals(BoolTypeDef.get())) {
                                        initValues.add(new BoolValue(false));
                                } else if (p.type().equals(CharTypeDef.get())) {
                                        initValues.add(new CharValue((char) 0));
                                } else if (p.type().equals(LongTypeDef.get())) {
                                        initValues.add(new LongValue(0));
                                } else if (p.type().equals(FloatTypeDef.get())) {
                                        initValues.add(new FloatValue(0));
                                } else if (p.type().equals(DoubleTypeDef.get())) {
                                        initValues.add(new DoubleValue(0));
                                } else {
                                        initValues.add(NullValue.get());
                                }
                        }

                        Ins.InvokeSpecial is = new Ins.InvokeSpecial(
                                new Ins.This(cls),
                                con,
                                LineCol.SYNTHETIC
                        );
                        is.arguments().addAll(initValues);
                        zeroParamCons.statements().add(is);

                        // annotations
                        for (SAnno anno : con.annos()) {
                                SAnno newAnno = new SAnno();
                                newAnno.setAnnoDef(anno.type());
                                newAnno.setPresent(zeroParamCons);
                                newAnno.values().putAll(anno.values());
                                zeroParamCons.annos().add(newAnno);
                        }
                }
                for (SFieldDef f : cls.fields()) {
                        if (f.modifiers().contains(SModifier.STATIC)) continue;
                        SMethodDef getter = getters.get(f);
                        SMethodDef setter = setters.get(f);

                        String name = f.name();
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);

                        if (getter == null) {
                                // getField():Type
                                //     return this.field
                                getter = new SMethodDef(lineCol);
                                getter.setName("get" + name);
                                getter.setDeclaringType(cls);
                                cls.methods().add(getter);
                                getter.setReturnType(f.type());
                                getter.modifiers().add(SModifier.PUBLIC);

                                Statement stmt = new AST.Return(
                                        new AST.Access(
                                                new AST.Access(null, "this", lineCol),
                                                f.name(),
                                                lineCol
                                        ),
                                        lineCol
                                );
                                parseStatement(stmt, getter.getReturnType(), new SemanticScope(scope), getter.statements(),
                                        getter.exceptionTables(), null, null, false);
                        }
                        if (setter == null) {
                                // setField(field:Type)
                                //     this.field=field
                                setter = new SMethodDef(lineCol);
                                setter.setName("set" + name);
                                setter.setDeclaringType(cls);
                                cls.methods().add(setter);
                                setter.setReturnType(VoidType.get());
                                SParameter p = new SParameter();
                                p.setName(f.name());
                                setter.getParameters().add(p);
                                p.setTarget(setter);
                                p.setType(f.type());
                                setter.modifiers().add(SModifier.PUBLIC);

                                SemanticScope setterScope = new SemanticScope(scope);
                                setterScope.putLeftValue(f.name(), p);

                                Statement stmt = new AST.Assignment(
                                        new AST.Access(
                                                new AST.Access(null, "this", lineCol),
                                                f.name(),
                                                lineCol
                                        ),
                                        "=",
                                        new AST.Access(null, f.name(), lineCol),
                                        lineCol
                                );
                                parseStatement(stmt, setter.getReturnType(), setterScope, setter.statements(),
                                        setter.exceptionTables(), null, null, false);
                        }
                }
        }

        /**
         * in step 4<br>
         * fills the method with instructions
         *
         * @param methodDef  the method to be filled with instructions
         * @param statements statements to be parsed into instructions
         * @param superScope the class scope
         * @throws SyntaxException compile error
         */
        private void parseMethod(SMethodDef methodDef, List<Statement> statements, SemanticScope superScope) throws SyntaxException {
                if (!methodDef.statements().isEmpty()) return;
                if (methodDef.modifiers().contains(SModifier.ABSTRACT)) {
                        if (!statements.isEmpty()) {
                                err.SyntaxException("abstract method cannot contain statements", statements.get(0).line_col());
                                return;
                        }
                }
                SemanticScope scope = new SemanticScope(superScope);
                if (!methodDef.modifiers().contains(SModifier.STATIC)) {
                        scope.setThis(new Ins.This(scope.type()));
                }

                if (defaultParamInvokable.containsKey(methodDef)) {
                        fillDefaultParamMethod(methodDef, scope);
                } else {
                        for (SParameter p : methodDef.getParameters()) {
                                scope.putLeftValue(p.name(), p);
                        }
                        // fill statements
                        for (Statement stmt : statements) {
                                parseStatement(
                                        stmt,
                                        methodDef.getReturnType(),
                                        scope,
                                        methodDef.statements(),
                                        methodDef.exceptionTables(),
                                        null, null,
                                        false);
                        }
                }
        }

        /**
         * in step 4<br>
         * fill the given annotation with parsed values
         *
         * @param annos annotations to fill values
         * @throws SyntaxException exception
         */
        private void parseAnnoValues(Collection<SAnno> annos) throws SyntaxException {
                // check annotation
                for (SAnno sAnno : annos) {
                        AST.Anno anno = annotationRecorder.get(sAnno); // get original anno object
                        Map<SAnnoField, Value> map = new HashMap<>();
                        out:
                        for (SAnnoField f : sAnno.type().annoFields()) {
                                if (anno == null) {
                                        for (Map.Entry<String, Object> entry : sAnno.alreadyCompiledAnnotationValueMap().entrySet()) {
                                                if (entry.getKey().equals(f.name())) {
                                                        // find annotation field
                                                        Value v = parseValueFromObject(entry.getValue());
                                                        map.put(f, v);
                                                        continue out;
                                                }
                                        }
                                } else {
                                        for (AST.Assignment a : anno.args) {
                                                if (a.assignTo.name.equals(f.name())) {
                                                        // find annotation field
                                                        Value v = parseValueFromExpression(a.assignFrom, f.type(), null);
                                                        v = checkAndCastAnnotationValues(v, a.assignTo.line_col());
                                                        map.put(f, v);
                                                        continue out;
                                                }
                                        }
                                }
                                // not found, check defaultValue
                                if (f.defaultValue() == null) {
                                        err.SyntaxException(f.name() + " is missing",
                                                anno == null ? LineCol.SYNTHETIC : anno.line_col());
                                        return;
                                }
                        }
                        sAnno.values().putAll(map);
                }
        }

        /**
         * check and cast annotation values
         *
         * @param value   value
         * @param lineCol line column
         * @return the cast value
         * @throws SyntaxException compiling error
         */
        private Value checkAndCastAnnotationValues(Value value, LineCol lineCol) throws SyntaxException {
                if (value instanceof IntValue
                        || value instanceof ShortValue
                        || value instanceof ByteValue
                        || value instanceof CharValue
                        || value instanceof BoolValue
                        || value instanceof LongValue
                        || value instanceof DoubleValue
                        || value instanceof FloatValue
                        || value instanceof SArrayValue
                        || value instanceof StringConstantValue
                        || value instanceof SAnno
                        || value instanceof Ins.GetClass
                        || value instanceof EnumValue) {
                        return value;
                } else if (value instanceof Ins.GetStatic) {
                        Ins.GetStatic gs = (Ins.GetStatic) value;
                        EnumValue enumValue = new EnumValue();
                        enumValue.setType(gs.field().declaringType());
                        enumValue.setEnumStr(gs.field().name());
                        return enumValue;
                } else if (value instanceof Ins.NewArray || value instanceof Ins.ANewArray) {
                        List<Value> theValues =
                                value instanceof Ins.NewArray
                                        ? ((Ins.NewArray) value).initValues()
                                        : ((Ins.ANewArray) value).initValues();

                        SArrayValue arr = new SArrayValue();
                        arr.setDimension(1);
                        arr.setType((SArrayTypeDef) value.type());

                        List<Value> values = new ArrayList<>();
                        for (Value v : theValues) {
                                values.add(checkAndCastAnnotationValues(v, lineCol));
                        }

                        arr.setValues(values.toArray(new Value[values.size()]));
                        return arr;
                } else {
                        err.SyntaxException("invalid annotation field " + value, lineCol);
                        return null;
                }
        }

        /**
         * check whether the literal is int type<br>
         * also, the requiredType should be int or superclass or Integer or null
         *
         * @param requiredType required type(should be int or superclass of Integer)
         *                     or null
         * @param literal      int literal
         * @param lineCol      file_line_col
         * @return true if it's int(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isInt(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof IntTypeDef ||
                        (requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Integer", lineCol))
                        )) &&
                        // &&
                        !literal.literal().contains(".");
        }

        /**
         * check whether the literal is long type
         *
         * @param requiredType required type(should be long or superclass of Long)
         *                     or null
         * @param literal      long literal
         * @param lineCol      file_line_col
         * @return true if it's long(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isLong(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof LongTypeDef ||
                        (requiredType instanceof SClassDef) && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Long", lineCol)
                        )) &&
                        // &&
                        !literal.literal().contains(".");
        }

        /**
         * check whether the literal is short type
         *
         * @param requiredType required type(should be short or superclass of Short)
         *                     or null
         * @param literal      short literal
         * @param lineCol      file_line_col
         * @return true if it's short(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isShort(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof ShortTypeDef ||
                        requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Short", lineCol)
                        )) &&
                        // &&
                        !literal.literal().contains(".");
        }

        /**
         * check whether the literal is byte type
         *
         * @param requiredType required type(should be byte or superclass of Byte)
         *                     or null
         * @param literal      byte literal
         * @param lineCol      file_line_col
         * @return true if it's byte(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isByte(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof ByteTypeDef ||
                        requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Byte", lineCol)
                        )) &&
                        // &&
                        !literal.literal().contains(".");
        }

        /**
         * check whether the required type is float type<br>
         * number literal always will match float value
         *
         * @param requiredType required type(should be float or superclass of Float)
         *                     or null
         * @param lineCol      file_line_col
         * @return true if it's float(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isFloat(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof FloatTypeDef ||
                        requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Float", lineCol)
                        ));
        }

        /**
         * check whether the required type is double type<br>
         * number literal always will match double value
         *
         * @param requiredType required type(should be double or superclass of Double)
         *                     or null
         * @param lineCol      file_line_col
         * @return true if it's double(boxed or primitive)
         * @throws SyntaxException exception
         */
        private boolean isDouble(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof DoubleTypeDef ||
                        requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Double", lineCol)
                        ));
        }

        /**
         * check whetehr the required type is bool type
         *
         * @param requiredType required type(should be boolean or superclass of Boolean)
         *                     or null
         * @param lineCol      file_line_col
         * @return true if it's boolean(boxed or primitve)
         * @throws SyntaxException exception
         */
        private boolean isBool(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
                return (requiredType == null ||
                        requiredType instanceof BoolTypeDef ||
                        requiredType instanceof SClassDef && requiredType.isAssignableFrom(
                                getTypeWithName("java.lang.Boolean", lineCol)
                        ));
        }

        /**
         * check whether the literal is char type<br>
         * the literal should be string literal with only one character and required type should match char or Character<br>
         * if the requiredType is Character or char, the literal can be surrounded with either \" or \'.<br>
         * if the requiredType is not Character but assignable from Character and not assignable from String, the literal can be surrounded with either \" or \'<br>
         * if the requiredType is assignable from both Character and String, the literal should be surrounded with \' and length should == 1
         *
         * @param requiredType required type
         * @param literal      literal
         * @param lineCol      file_line_col
         * @return true if it's char
         * @throws SyntaxException exception
         */
        private boolean isChar(STypeDef requiredType, StringLiteral literal, LineCol lineCol) throws SyntaxException {
                if (requiredType == null)
                        return isChar(literal, lineCol, true);
                if (requiredType instanceof CharTypeDef)
                        return isChar(literal, lineCol, false);
                if (requiredType instanceof SClassDef) {
                        SClassDef characterClass = (SClassDef) getTypeWithName("java.lang.Character", lineCol);
                        if (requiredType.equals(characterClass)) return true; // Character
                        if (requiredType.isAssignableFrom(characterClass)) {
                                SClassDef stringClass = (SClassDef) getTypeWithName("java.lang.String", lineCol);
                                if (requiredType.isAssignableFrom(stringClass)) {
                                        // assignable from Character and String
                                        // check start with \' and length == 1
                                        return isChar(literal, lineCol, true);
                                } else // assignable from only Character
                                        return isChar(literal, lineCol, false);
                        }
                }
                return false;
        }

        /**
         * check whether the literal is char<br>
         * the method only checks literal start symbol and it's length
         *
         * @param literal    literal
         * @param lineCol    file_line_col
         * @param testSymbol true if the symbol is taken into consideration
         * @return true if the literal is char
         * @throws SyntaxException exception
         */
        private boolean isChar(StringLiteral literal, LineCol lineCol, boolean testSymbol) throws SyntaxException {
                String str = literal.literal();
                str = str.substring(1);
                str = str.substring(0, str.length() - 1);
                // testSymbol==true and not start with \' then return false
                if (testSymbol && !literal.literal().startsWith("\'")) return false;
                // check whether the string length is 1
                String s = unescape(str, lineCol);
                assert s != null;
                return s.length() == 1;
        }

        private SClassDef Throwable_Class;

        private SClassDef getThrowable_Class() throws SyntaxException {
                if (Throwable_Class == null) {
                        Throwable_Class = (SClassDef) getTypeWithName("java.lang.Throwable", LineCol.SYNTHETIC);
                }
                return Throwable_Class;
        }

        /**
         * in step 4<br>
         * parse instructions<br>
         * <ul>
         * <li>{@link Statement} =&gt; {@link Instruction}</li>
         * <li>{@link Expression} =&gt; {@link Value}</li>
         * <li>{@link lt.compiler.syntactic.AST.Return} =&gt; {@link lt.compiler.semantic.Ins.TReturn}</li>
         * <li>{@link lt.compiler.syntactic.AST.If} =&gt; a list of Instructions containing {@link lt.compiler.semantic.Ins.IfEq} {@link lt.compiler.semantic.Ins.IfNe} {@link lt.compiler.semantic.Ins.Goto}</li>
         * <li>{@link lt.compiler.syntactic.AST.While} =&gt; a list of Instructions (same as above)</li>
         * <li>{@link lt.compiler.syntactic.AST.For} =&gt; a list of Instructions (same as above)</li>
         * <li>{@link lt.compiler.syntactic.AST.Throw} =&gt; {@link lt.compiler.semantic.Ins.AThrow}</li>
         * <li>{@link lt.compiler.syntactic.AST.Try} =&gt; a list of Instructions and some ExceptionTable</li>
         * <li>{@link lt.compiler.syntactic.AST.Synchronized} =&gt; {@link lt.compiler.semantic.Ins.MonitorEnter} and {@link lt.compiler.semantic.Ins.MonitorExit}</li>
         * <li>{@link MethodDef} =&gt; {@link #parseInnerMethod(MethodDef, SemanticScope)}</li>
         * <li>{@link lt.compiler.syntactic.AST.Break =&gt; goto instruction}</li>
         * <li>{@link lt.compiler.syntactic.AST.Continue =&gt; goto instruction}</li>
         * </ul>
         *
         * @param statement           instructions
         * @param scope               scope that contains local variables and local methods
         * @param instructions        currently parsing {@link SInvokable} object instructions
         * @param exceptionTable      the exception table (start,end,handle,type)
         * @param breakIns            jump to this position when meets a break    (or null if it's not inside any loop)
         * @param continueIns         jump to this position when meets a continue (or null if it's not inside any loop)
         * @param doNotParseMethodDef the methodDef should not be parsed( in this case, they should be outer methods instead of inner methods)
         * @throws SyntaxException compile error
         */
        private void parseStatement(Statement statement,
                                    STypeDef methodReturnType,
                                    SemanticScope scope,
                                    List<Instruction> instructions,
                                    List<ExceptionTable> exceptionTable,
                                    Ins.Nop breakIns,
                                    Ins.Nop continueIns,
                                    boolean doNotParseMethodDef) throws SyntaxException {
                if (statement instanceof Expression) {
                        // expression
                        // required type is null , which means no required type
                        Value v = parseValueFromExpression((Expression) statement, null, scope);
                        if (v instanceof Instruction) {
                                instructions.add((Instruction) v);
                        } // else ignore the result
                } else if (statement instanceof AST.Return) {
                        parseInstructionFromReturn((AST.Return) statement, methodReturnType, scope, instructions);

                } else if (statement instanceof AST.If) {
                        parseInstructionFromIf((AST.If) statement, methodReturnType, scope,
                                instructions, exceptionTable, breakIns, continueIns);
                } else if (statement instanceof AST.While) {
                        // while or do while
                        parseInstructionFromWhile((AST.While) statement, methodReturnType, scope, instructions, exceptionTable);
                } else if (statement instanceof AST.For) {
                        // for i in xxx
                        parseInstructionFromFor((AST.For) statement, methodReturnType, scope, instructions, exceptionTable);
                } else if (statement instanceof AST.Throw) {
                        // throw xxx
                        Value throwable = parseValueFromExpression(
                                ((AST.Throw) statement).exp,
                                null,
                                scope);
                        assert throwable != null;
                        if (!getThrowable_Class().isAssignableFrom(throwable.type())) {
                                // cast to throwable
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(getLang_castToThrowable(), LineCol.SYNTHETIC);
                                invokeStatic.arguments().add(throwable);
                                throwable = invokeStatic;
                        }
                        Ins.AThrow aThrow = new Ins.AThrow(throwable, statement.line_col());
                        instructions.add(aThrow);
                } else if (statement instanceof AST.Try) {
                        parseInstructionFromTry((AST.Try) statement, methodReturnType, scope, instructions, exceptionTable, breakIns, continueIns);
                } else if (statement instanceof AST.Synchronized) {
                        parseInstructionFromSynchronized((AST.Synchronized) statement,
                                methodReturnType,
                                scope,
                                instructions,
                                exceptionTable,
                                breakIns, continueIns);
                } else if (statement instanceof MethodDef) {
                        if (!doNotParseMethodDef)
                                parseInnerMethod((MethodDef) statement, scope);
                } else if (statement instanceof AST.Break) {
                        if (breakIns == null) {
                                err.SyntaxException("break should be inside a loop", statement.line_col());
                                return;
                        }
                        instructions.add(new Ins.Goto(breakIns));
                } else if (statement instanceof AST.Continue) {
                        if (continueIns == null) {
                                err.SyntaxException("continue should be inside a loop", statement.line_col());
                                return;
                        }
                        instructions.add(new Ins.Goto(continueIns));
                } else if (!(statement instanceof AST.StaticScope || statement instanceof AST.Pass)) {
                        throw new LtBug("unknown statement " + statement);
                }
        }

        /**
         * {@link LtRuntime#castToThrowable(Object)}
         */
        private SMethodDef Lang_castToThrowable;

        /**
         * @return {@link LtRuntime#castToThrowable(Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_castToThrowable() throws SyntaxException {
                if (Lang_castToThrowable == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToThrowable")) {
                                        Lang_castToThrowable = m;
                                        break;
                                }
                        }
                }
                return Lang_castToThrowable;
        }

        /**
         * parse inner method<br>
         * the inner method name is automatically generated<br>
         * method parameters would capture all existing local variables. they are positioned ahead of params that the inner method requires<br>
         * e.g.<br>
         * <code>
         * outer()<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;i=1<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;j=2<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;inner(x)=1+i+j+x
         * </code><br>
         * the inner method would be parsed into <code>inner$SomeGeneratedName$(i,j,x)</code><br>
         * when invoking, the captured local variables would be passed in as parameters<br>
         * both final and not-final variables can be captured, <br>
         * but the variable inside the inner method would NOT have effect on the outer one
         *
         * @param methodDef method def object, defines the inner method
         * @param scope     current scope
         * @return the new method (the inner method)
         * @throws SyntaxException compile error
         */
        private SMethodDef parseInnerMethod(MethodDef methodDef, SemanticScope scope) throws SyntaxException {
                if (scope.parent == null) throw new LtBug("scope.parent should not be null");

                SemanticScope theTopScope = scope.parent;
                while (theTopScope.parent != null) theTopScope = theTopScope.parent;

                // check method name
                if (scope.containsInnerMethod(methodDef.name)) {
                        err.SyntaxException("duplicate inner method name", methodDef.line_col());
                        return null;
                }

                // inner method cannot have modifiers or annotations
                if (!methodDef.modifiers.isEmpty()) {
                        err.SyntaxException("inner method cannot have modifiers", methodDef.line_col());
                        return null;
                }
                if (!methodDef.annos.isEmpty()) {
                        err.SyntaxException("inner method cannot have annotations", methodDef.line_col());
                        return null;
                }

                // check param names, see if it's already used
                // also, init values are not allowed
                for (VariableDef v : methodDef.params) {
                        if (null != scope.getLeftValue(v.getName())) {
                                err.SyntaxException(v.getName() + " is already used", v.line_col());
                                return null;
                        }
                        if (v.getInit() != null) {
                                err.SyntaxException("parameters of inner methods cannot have default value", v.line_col());
                                return null;
                        }
                }

                // get current type methods
                List<SMethodDef> methods;
                if (scope.type() instanceof SClassDef) {
                        methods = ((SClassDef) scope.type()).methods();
                } else {
                        methods = ((SInterfaceDef) scope.type()).methods();
                }

                // generate method name
                String generatedMethodName = methodDef.name + "$Latte$InnerMethod$";
                int i = 0;
                out:
                while (true) {
                        String tmpName = generatedMethodName + i;
                        for (SMethodDef m : methods) {
                                if (m.name().equals(tmpName)) {
                                        ++i;
                                        continue out;
                                }
                        }
                        break;
                }
                generatedMethodName += i;

                String name = methodDef.name;
                int paramCount = methodDef.params.size();

                // fill in local variable as parameters
                // the params are added to front positions
                LinkedHashMap<String, STypeDef> localVariables = scope.getLocalVariables();
                List<VariableDef> param4Locals = new ArrayList<>();
                localVariables.forEach((k, v) -> {
                        // construct a synthetic VariableDef as param
                        VariableDef variable = new VariableDef(k, Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                        if (v instanceof SArrayTypeDef) {
                                STypeDef x = ((SArrayTypeDef) v).type();
                                AST.Access theType = new AST.Access(
                                        x.pkg() == null
                                                ? null
                                                : new AST.PackageRef(x.pkg(), LineCol.SYNTHETIC),
                                        x.fullName().contains(".")
                                                ? x.fullName().substring(x.fullName().lastIndexOf('.') + 1)
                                                : x.fullName(),
                                        LineCol.SYNTHETIC
                                );
                                for (int ii = 0; ii < ((SArrayTypeDef) v).dimension(); ++ii) {
                                        theType = new AST.Access(theType, "[]", LineCol.SYNTHETIC);
                                }
                                variable.setType(theType);
                        } else {
                                variable.setType(
                                        new AST.Access(
                                                v.pkg() == null
                                                        ? null
                                                        : new AST.PackageRef(v.pkg(), LineCol.SYNTHETIC),
                                                v.fullName().contains(".")
                                                        ? v.fullName().substring(v.fullName().lastIndexOf('.') + 1)
                                                        : v.fullName(),
                                                LineCol.SYNTHETIC
                                        )
                                );
                        }
                        param4Locals.add(variable);
                });
                MethodDef newMethodDef = new MethodDef(
                        generatedMethodName,
                        Collections.emptySet(),
                        methodDef.returnType,
                        new ArrayList<>(methodDef.params),
                        Collections.emptySet(),
                        methodDef.body,
                        methodDef.line_col()
                );
                newMethodDef.params.addAll(0, param4Locals);

                // parse the method
                parseMethod(newMethodDef, newMethodDef.params.size(), scope.type(), null, fileNameToImport.get(newMethodDef.line_col().fileName),
                        (scope.type() instanceof SClassDef) ? PARSING_CLASS : PARSING_INTERFACE, scope.getThis() == null);
                SMethodDef m = methods.get(methods.size() - 1);
                // change the modifier
                m.modifiers().remove(SModifier.PUBLIC);
                m.modifiers().remove(SModifier.PROTECTED);
                m.modifiers().add(0, SModifier.PRIVATE);

                // add into scope
                SemanticScope.MethodRecorder rec = new SemanticScope.MethodRecorder(m, paramCount);
                scope.addMethodDef(name, rec);

                // generate a scope for the inner method
                // the scope contains the inner method itself
                SemanticScope innerMethodScope = new SemanticScope(theTopScope);
                innerMethodScope.addMethodDef(name, rec);

                parseMethod(m, newMethodDef.body, innerMethodScope);

                return m;
        }

        /**
         * parse synchronized<br>
         * every monitor must have an exit<br>
         * <code>
         * sync(a,b,c)<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * ==><br>
         * a<br>
         * astoreX<br>
         * monitor enter<br>
         * b<br>
         * astoreY<br>
         * monitor enter<br>
         * c<br>
         * astoreZ<br>
         * monitor enter<br>
         * ...<br>
         * astore<br>
         * Z<br>
         * monitor exit<br>
         * Y<br>
         * monitor exit<br>
         * X<br>
         * monitor exit<br>
         * goto `nop`<br>
         * [end normally]<br>
         * --------------------<br>
         * [end with exception]<br>
         * Z<br>
         * monitor exit<br>
         * Y<br>
         * monitor exit<br>
         * X<br>
         * monitor exit<br>
         * athrow<br>
         * nop
         * </code><br>
         * and every position of return should insert after monitor exit instructions
         *
         * @param aSynchronized    synchronized object
         * @param methodReturnType method return type
         * @param scope            current scope
         * @param instructions     instructions
         * @param exceptionTable   exception table
         * @param breakIns         jump to this position when meets a break
         * @param continueIns      jump to this position when meets a continue
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromSynchronized(AST.Synchronized aSynchronized,
                                                      STypeDef methodReturnType,
                                                      SemanticScope scope,
                                                      List<Instruction> instructions,
                                                      List<ExceptionTable> exceptionTable,
                                                      Ins.Nop breakIns,
                                                      Ins.Nop continueIns) throws SyntaxException {

                SemanticScope subScope = new SemanticScope(scope);
                Stack<Ins.MonitorEnter> stack = new Stack<>();
                for (Expression exp : aSynchronized.toSync) {
                        Value v = parseValueFromExpression(exp, null, subScope);
                        Ins.MonitorEnter enter = new Ins.MonitorEnter(v, subScope, exp.line_col());
                        stack.push(enter);

                        instructions.add(enter); // monitor enter
                }

                // parse statements
                List<Instruction> instructionList = new ArrayList<>();
                for (Statement stmt : aSynchronized.statements) {
                        parseStatement(stmt, methodReturnType, subScope, instructionList, exceptionTable, breakIns, continueIns, false);
                }
                if (instructionList.size() == 0) instructionList.add(new Ins.Nop());

                // build exit for return, continue and break

                int returnCount = 0;
                int continueCount = 0;
                int breakCount = 0;
                for (Instruction ins : instructionList) {
                        if (ins instanceof Ins.TReturn) ++returnCount;
                        else if (breakIns != null) {
                                assert continueIns != null;
                                if (ins instanceof Ins.Goto) {
                                        if (((Ins.Goto) ins).gotoIns() == breakIns) ++breakCount;
                                        else if (((Ins.Goto) ins).gotoIns() == continueIns) ++continueCount;
                                }
                        }
                }

                List<Ins.MonitorExit> exitNormal = new ArrayList<>(stack.size());
                List<Ins.MonitorExit> exitForExceptions = new ArrayList<>(stack.size());
                List<List<Ins.MonitorExit>> exitForReturn = new ArrayList<>();
                List<List<Ins.MonitorExit>> exitForBreak = new ArrayList<>();
                List<List<Ins.MonitorExit>> exitForContinue = new ArrayList<>();
                for (int i = 0; i < returnCount; ++i) exitForReturn.add(new ArrayList<>());
                for (int i = 0; i < continueCount; ++i) exitForContinue.add(new ArrayList<>());
                for (int i = 0; i < breakCount; ++i) exitForBreak.add(new ArrayList<>());

                while (!stack.empty()) {
                        Ins.MonitorEnter monitorEnter = stack.pop();
                        exitNormal.add(new Ins.MonitorExit(monitorEnter));
                        exitForExceptions.add(new Ins.MonitorExit(monitorEnter));
                        for (List<Ins.MonitorExit> list : exitForReturn) list.add(new Ins.MonitorExit(monitorEnter));
                        for (List<Ins.MonitorExit> list : exitForContinue) list.add(new Ins.MonitorExit(monitorEnter));
                        for (List<Ins.MonitorExit> list : exitForBreak) list.add(new Ins.MonitorExit(monitorEnter));
                }

                // insert exit before return
                returnCount = 0;
                continueCount = 0;
                breakCount = 0;
                for (int i = 0; i < instructionList.size(); ++i) {
                        Instruction ins = instructionList.get(i);

                        if (ins instanceof Ins.TReturn) {
                                i += insertInstructionsBeforeReturn(instructionList, i, exitForReturn.get(returnCount++), subScope);
                        } else if (breakIns != null) {
                                if (ins instanceof Ins.Goto) {
                                        List<Ins.MonitorExit> exitList = null;
                                        if (((Ins.Goto) ins).gotoIns() == breakIns) {
                                                exitList = exitForBreak.get(breakCount++);
                                        } else if (((Ins.Goto) ins).gotoIns() == continueIns) {
                                                exitList = exitForContinue.get(continueCount++);
                                        }

                                        if (exitList != null) {
                                                instructionList.addAll(i, exitList);
                                                i += exitList.size();
                                        }
                                }
                        }
                }

                instructions.addAll(instructionList);
                instructions.addAll(exitNormal);

                // might occur an exception
                LocalVariable localVariable = new LocalVariable(
                        getTypeWithName("java.lang.Throwable", aSynchronized.line_col()),
                        false);
                subScope.putLeftValue(subScope.generateTempName(), localVariable);

                Ins.AThrow aThrow = new Ins.AThrow(
                        new Ins.TLoad(localVariable, subScope, aSynchronized.line_col()),
                        aSynchronized.line_col());

                Ins.ExStore exStore = new Ins.ExStore(localVariable, subScope);
                Ins.Nop nop = new Ins.Nop();

                Ins.Goto aGoto = new Ins.Goto(nop);
                instructions.add(aGoto); // goto athrow
                instructions.add(exStore); // astore
                instructions.addAll(exitForExceptions);
                instructions.add(aThrow); // athrow
                instructions.add(nop); // nop

                ExceptionTable table = new ExceptionTable(instructionList.get(0), exitNormal.get(0), exStore, null);
                exceptionTable.add(table);
        }

        /**
         * separate TReturn with it's return value, and insert instructions<br>
         * <code>TheValueToReturn, TStore, instructionToInsert, TLoad, TReturn</code>
         *
         * @param instructions the original instruction list
         * @param returnIndex  TReturn position
         * @param toInsert     the instructions to insert
         * @param scope        scope
         * @return the cursor should += this return value
         */
        private int insertInstructionsBeforeReturn(List<Instruction> instructions,
                                                   int returnIndex,
                                                   List<? extends Instruction> toInsert,
                                                   SemanticScope scope) throws SyntaxException {
                Ins.TReturn tReturn = (Ins.TReturn) instructions.remove(returnIndex); // get return
                Value returnValue = tReturn.value();
                if (returnValue != null) {
                        LocalVariable tmp = new LocalVariable(returnValue.type(), false);
                        scope.putLeftValue(scope.generateTempName(), tmp);

                        Ins.TStore TStore = new Ins.TStore(tmp, returnValue, scope, LineCol.SYNTHETIC, err); // store the value
                        Ins.TLoad tLoad = new Ins.TLoad(tmp, scope, LineCol.SYNTHETIC); // retrieve the value
                        tReturn.setReturnValue(tLoad); // return the value

                        instructions.add(returnIndex++, TStore);
                }

                instructions.addAll(returnIndex, toInsert);
                returnIndex += toInsert.size();
                instructions.add(returnIndex, tReturn);

                return toInsert.size() + 2;
        }

        /**
         * {@link LtRuntime#throwableWrapperObject(Throwable)}
         */
        private SMethodDef Lang_throwableWrapperObject;

        /**
         * @return {@link LtRuntime#throwableWrapperObject(Throwable)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_throwableWrapperObject() throws SyntaxException {
                if (Lang_throwableWrapperObject == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("throwableWrapperObject")) {
                                        Lang_throwableWrapperObject = m;
                                        break;
                                }
                        }
                }
                return Lang_throwableWrapperObject;
        }

        /**
         * parse try<br>
         * <pre>
         * try
         *     A
         * catch Ex
         *     B
         * finally
         *     D
         * E
         *
         * ==>
         *
         * A------------------┐
         * (                  │
         *     DoSomething    │
         *     D              │
         *     return        goto D2
         * or                 │
         *     DoSomething    │
         * )                  │
         * goto D1------------┘
         * B-----------------goto D2
         * goto D1------------┘
         * D2 (exception)
         * D1 (normal)
         * E
         * </pre>
         * <br>
         * when meets a return value, separate <code>value to return</code> and the return instruction itself using {@link #insertInstructionsBeforeReturn(List, int, List, SemanticScope)} <br>
         * the <tt>finally</tt> part would be inserted and they shouldn't be caught<br>
         * <pre>
         * something
         * TReturn(Value)
         *
         * ==>
         *
         * something-------------┐
         * Value               caught
         * TStore----------------┘
         * [the finally part]----shoudn't be caught
         * TLoad
         * TReturn
         * </pre>
         *
         * @param aTry             try
         * @param methodReturnType method return type
         * @param scope            scope
         * @param instructions     instruction list
         * @param exceptionTable   exception table
         * @param breakIns         jump to this position when meets a break
         * @param continueIns      jump to this position when meets a continue
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromTry(AST.Try aTry,
                                             STypeDef methodReturnType,
                                             SemanticScope scope,
                                             List<Instruction> instructions,
                                             List<ExceptionTable> exceptionTable,
                                             Ins.Nop breakIns,
                                             Ins.Nop continueIns) throws SyntaxException {
                // try ...
                SemanticScope scopeA = new SemanticScope(scope);
                List<Instruction> insA = new ArrayList<>(); // instructions in scope A
                for (Statement stmt : aTry.statements) {
                        parseStatement(stmt, methodReturnType, scopeA, insA, exceptionTable, breakIns, continueIns, false);
                }

                // record the start and end for exception table
                // end is inclusive in this map
                // and should be converted to an exclusive one when added into exception table
                LinkedHashMap<Instruction, Instruction> startToEnd = new LinkedHashMap<>();
                Instruction start = null;
                for (int i1 = 0; i1 < insA.size(); i1++) {
                        Instruction i = insA.get(i1);
                        if (start == null) { // start is not set
                                // if no instructions before return
                                // then the block would not be added into exception table
                                // else
                                if (!(i instanceof Ins.TReturn)) {
                                        start = i;
                                }
                        } else {
                                // start is already set
                                // if it's return
                                // put into map and set `start` to null
                                if (i instanceof Ins.TReturn
                                        ||
                                        (breakIns != null && i instanceof Ins.Goto && (((Ins.Goto) i).gotoIns() == breakIns
                                                || ((Ins.Goto) i).gotoIns() == continueIns))
                                        ) {
                                        startToEnd.put(start, insA.get(i1 - 1));
                                        start = null;
                                }
                        }
                }
                // put last pair
                if (start != null) startToEnd.put(start, insA.get(insA.size() - 1));
                if (startToEnd.isEmpty() && insA.size() == 1) {
                        // insA[0] is return / break / continue
                        insA.add(0, new Ins.Nop());
                        startToEnd.put(insA.get(0), insA.get(0));
                }

                // the map preparation is done

                // build normal finally (D1)
                List<Instruction> normalFinally = new ArrayList<>();
                for (Statement stmt : aTry.fin) {
                        parseStatement(stmt, methodReturnType, new SemanticScope(scope), normalFinally, exceptionTable, breakIns, continueIns, false);
                }
                if (normalFinally.isEmpty()) {
                        normalFinally.add(new Ins.Nop());
                }
                Instruction D1start = normalFinally.get(0);

                // build exception finally (D2)
                SemanticScope exceptionFinallyScope = new SemanticScope(scope);
                // add D2start to exception finally at 0
                // add throw to exception finally at end
                LocalVariable tmpForExStore = new LocalVariable(
                        getTypeWithName("java.lang.Throwable", aTry.line_col()), false);
                exceptionFinallyScope.putLeftValue(exceptionFinallyScope.generateTempName(), tmpForExStore);
                // store
                Ins.ExStore D2start = new Ins.ExStore(tmpForExStore, exceptionFinallyScope);
                // throw
                Ins.AThrow aThrow = new Ins.AThrow(
                        new Ins.TLoad(tmpForExStore, exceptionFinallyScope,
                                aTry.line_col()),
                        aTry.line_col());
                List<Instruction> exceptionFinally = new ArrayList<>();
                // fill the list
                exceptionFinally.add(D2start);
                for (Statement stmt : aTry.fin) {
                        parseStatement(stmt, methodReturnType, exceptionFinallyScope,
                                exceptionFinally, exceptionTable, breakIns, continueIns, false);
                }
                exceptionFinally.add(aThrow);

                // add D into every position before
                // return, break, continue in insA
                for (int i = 0; i < insA.size(); ++i) {
                        Instruction ins = insA.get(i);
                        if (ins instanceof Ins.TReturn) {
                                List<Instruction> list = new ArrayList<>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType,
                                                new SemanticScope(scopeA),
                                                list, exceptionTable, breakIns, continueIns, false);
                                }
                                i += insertInstructionsBeforeReturn(insA, i, list, scopeA);
                        } else if (breakIns != null) {
                                if (ins instanceof Ins.Goto) {
                                        if (((Ins.Goto) ins).gotoIns() == breakIns
                                                || ((Ins.Goto) ins).gotoIns() == continueIns) {

                                                List<Instruction> list = new ArrayList<>();
                                                for (Statement stmt : aTry.fin) {
                                                        parseStatement(stmt, methodReturnType,
                                                                new SemanticScope(scopeA),
                                                                list, exceptionTable, breakIns, continueIns, false);
                                                }
                                                insA.addAll(i, list);
                                                i += list.size();
                                        }
                                }
                        }
                }

                insA.add(new Ins.Goto(D1start)); // goto D1
                // add A into instruction list
                instructions.addAll(insA); // A

                // create a map that end is exclusive
                LinkedHashMap<Instruction, Instruction> startToEndEx = new LinkedHashMap<>();
                int cursor = 0;
                for (Map.Entry<Instruction, Instruction> entry : startToEnd.entrySet()) {
                        Instruction key = entry.getKey();
                        Instruction inclusive = entry.getValue();
                        Instruction exclusive = null;
                        for (; cursor < insA.size(); ++cursor) {
                                Instruction i = insA.get(cursor);
                                if (inclusive.equals(i)) {
                                        Instruction tmp = insA.get(++cursor);
                                        if (tmp instanceof Ins.TStore) {
                                                exclusive = insA.get(++cursor);
                                        } else {
                                                exclusive = tmp;
                                        }
                                        break;
                                }
                        }
                        if (exclusive == null) throw new LtBug("exclusive should not be null");
                        startToEndEx.put(key, exclusive);
                }

                SemanticScope catchScope = new SemanticScope(scope); // catch scope

                STypeDef THROWABLE = getTypeWithName("java.lang.Throwable", LineCol.SYNTHETIC);

                LocalVariable ex = new LocalVariable(THROWABLE, true);
                catchScope.putLeftValue(catchScope.generateTempName(), ex); // the exception value

                LocalVariable unwrapped = new LocalVariable(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC), false);
                catchScope.putLeftValue(aTry.varName, unwrapped);

                // build instructions
                List<Instruction> insCatch = new ArrayList<>();
                insCatch.add(new Ins.ExStore(ex, catchScope)); // store the ex

                Ins.InvokeStatic invokeUnwrap = new Ins.InvokeStatic(
                        getLang_throwableWrapperObject(), LineCol.SYNTHETIC
                );
                invokeUnwrap.arguments().add(new Ins.TLoad(
                        ex,
                        catchScope,
                        LineCol.SYNTHETIC
                ));
                Ins.TStore storeUnwrapped = new Ins.TStore(
                        unwrapped,
                        invokeUnwrap,
                        catchScope,
                        LineCol.SYNTHETIC, err
                );
                insCatch.add(storeUnwrapped);

                for (Statement stmt : aTry.catchStatements) {
                        parseStatement(stmt, methodReturnType, catchScope, insCatch,
                                exceptionTable, breakIns, continueIns, false);
                }

                // record start to end for exception table
                // end is inclusive
                // and should be parsed into an exclusive one when added into exception table
                LinkedHashMap<Instruction, Instruction> catch_startToEnd = new LinkedHashMap<>();
                Instruction catch_start = null;
                for (int i1 = 0; i1 < insCatch.size(); ++i1) {
                        Instruction i = insCatch.get(i1);
                        if (catch_start == null) { // the start hasn't been recorded
                                // if i is return
                                // then ignore it and continue
                                // else record the instruction as start
                                if (!(i instanceof Ins.TReturn)) {
                                        catch_start = i;
                                }
                        } else { // start is already recorded
                                if (i instanceof Ins.TReturn
                                        ||
                                        (breakIns != null && i instanceof Ins.Goto && (((Ins.Goto) i).gotoIns() == breakIns
                                                || ((Ins.Goto) i).gotoIns() == continueIns))
                                        ) {
                                        catch_startToEnd.put(catch_start, insCatch.get(i1 - 1));
                                        catch_start = null;
                                }
                        }
                }
                // record last pair
                if (catch_start != null) catch_startToEnd.put(catch_start, insCatch.get(insCatch.size() - 1));

                // add finally before every return, break and continue
                for (int i = 0; i < insCatch.size(); ++i) {
                        Instruction ins = insCatch.get(i);
                        if (ins instanceof Ins.TReturn) {
                                List<Instruction> list = new ArrayList<>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType, new SemanticScope(catchScope),
                                                list, exceptionTable, breakIns, continueIns, false);
                                }
                                i += insertInstructionsBeforeReturn(insCatch, i, list, catchScope);
                        } else if (breakIns != null) {
                                if (ins instanceof Ins.Goto) {
                                        if (((Ins.Goto) ins).gotoIns() == breakIns
                                                || ((Ins.Goto) ins).gotoIns() == continueIns) {

                                                List<Instruction> list = new ArrayList<>();
                                                for (Statement stmt : aTry.fin) {
                                                        parseStatement(stmt, methodReturnType,
                                                                new SemanticScope(catchScope),
                                                                list, exceptionTable, breakIns, continueIns, false);
                                                }
                                                insCatch.addAll(i, list);
                                                i += list.size();
                                        }
                                }
                        }
                }
                insCatch.add(new Ins.Goto(D1start)); // goto D1
                instructions.addAll(insCatch); // B

                // transform the startToEnd map into exclusive end map
                LinkedHashMap<Instruction, Instruction> catch_startToEndEx = new LinkedHashMap<>();
                cursor = 0;
                for (Map.Entry<Instruction, Instruction> entry : catch_startToEnd.entrySet()) {
                        Instruction key = entry.getKey();
                        Instruction inclusive = entry.getValue();
                        Instruction exclusive = null;
                        for (; cursor < insCatch.size(); ++cursor) {
                                Instruction i = insCatch.get(cursor);
                                if (i.equals(inclusive)) {
                                        Instruction tmp = insCatch.get(++cursor);
                                        if (tmp instanceof Ins.TStore) {
                                                exclusive = insCatch.get(++cursor);
                                        } else {
                                                exclusive = tmp;
                                        }
                                        break;
                                }
                        }
                        if (exclusive == null) throw new LtBug("exclusive should not be null");
                        catch_startToEndEx.put(key, exclusive);
                }

                // build exception table for A
                for (Map.Entry<Instruction, Instruction> entry : startToEndEx.entrySet()) {
                        ExceptionTable tbl = new ExceptionTable(entry.getKey(), entry.getValue(), insCatch.get(0), THROWABLE);
                        exceptionTable.add(tbl);
                }

                // build exception table for B
                for (Map.Entry<Instruction, Instruction> entry : catch_startToEndEx.entrySet()) {
                        ExceptionTable tbl = new ExceptionTable(entry.getKey(), entry.getValue(), D2start, null);
                        exceptionTable.add(tbl);
                }

                instructions.addAll(exceptionFinally); // D2
                instructions.addAll(normalFinally); // D1
        }

        /**
         * {@link lt.lang.LtIterator#getIterator(Object)}
         */
        private SMethodDef LtIterator_getIterator;

        /**
         * @return {@link lt.lang.LtIterator#getIterator(Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLtIterator_Get() throws SyntaxException {
                if (LtIterator_getIterator == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtIterator", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("getIterator")) {
                                        LtIterator_getIterator = m;
                                        break;
                                }
                        }
                }
                return LtIterator_getIterator;
        }

        /**
         * {@link LtIterator#hasNext()}
         */
        private SMethodDef LtIterator_hasNext;

        /**
         * @return {@link LtIterator#hasNext()}
         * @throws SyntaxException exception
         */
        private SMethodDef getLtIterator_hasNext() throws SyntaxException {
                if (LtIterator_hasNext == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtIterator", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("hasNext")) {
                                        LtIterator_hasNext = m;
                                        break;
                                }
                        }
                }
                return LtIterator_hasNext;
        }

        /**
         * {@link LtIterator#next()}
         */
        private SMethodDef LtIterator_next;

        /**
         * @return {@link LtIterator#next()}
         * @throws SyntaxException exception
         */
        private SMethodDef getLtIterator_next() throws SyntaxException {
                if (LtIterator_next == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtIterator", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("next")) {
                                        LtIterator_next = m;
                                        break;
                                }
                        }
                }
                return LtIterator_next;
        }

        /**
         * parse for<br>
         * <br>
         * <pre>
         * for i in I
         *     A
         * B
         *
         * ==>
         *
         * LtIterator.get(aFor.exp)
         * aStore
         * here::
         * aLoad
         * hasNext
         * if eq (==false) goto B
         * aLoad
         * next
         * A
         * noo --------- also known as continue position
         * goto here
         * B ----------- also known as break position
         * </pre>
         *
         * @param aFor             for
         * @param methodReturnType method return type
         * @param scope            scope
         * @param instructions     instruction list
         * @param exceptionTable   exception table
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromFor(AST.For aFor,
                                             STypeDef methodReturnType,
                                             SemanticScope scope,
                                             List<Instruction> instructions,
                                             List<ExceptionTable> exceptionTable) throws SyntaxException {
                // LtIterator.get(aFor.exp)
                Ins.InvokeStatic getIterator = new Ins.InvokeStatic(getLtIterator_Get(), LineCol.SYNTHETIC);
                Value looper = parseValueFromExpression(aFor.exp, null, scope);
                assert looper != null;
                if (looper.type() instanceof PrimitiveTypeDef)
                        looper = boxPrimitive(looper, LineCol.SYNTHETIC);
                getIterator.arguments().add(looper);

                // aStore
                LocalVariable localVariable = new LocalVariable(getTypeWithName("lt.lang.LtIterator", LineCol.SYNTHETIC), false);
                scope.putLeftValue(scope.generateTempName(), localVariable);
                Ins.TStore tStore = new Ins.TStore(localVariable, getIterator, scope, LineCol.SYNTHETIC, err);
                instructions.add(tStore);
                // aLoad
                Ins.TLoad tLoad = new Ins.TLoad(localVariable, scope, LineCol.SYNTHETIC);
                // hasNext
                Ins.InvokeVirtual hasNext = new Ins.InvokeVirtual(tLoad, getLtIterator_hasNext(), LineCol.SYNTHETIC);
                // if eq goto B
                Ins.Nop nop = new Ins.Nop(); // B
                // if eq goto
                Ins.IfEq ifEq = new Ins.IfEq(hasNext, nop, aFor.line_col());
                instructions.add(ifEq);
                // aLoad
                Ins.TLoad tLoad1 = new Ins.TLoad(localVariable, scope, LineCol.SYNTHETIC);
                // next
                Ins.InvokeVirtual next = new Ins.InvokeVirtual(tLoad1, getLtIterator_next(), LineCol.SYNTHETIC);
                SemanticScope subScope = new SemanticScope(scope);
                LocalVariable newLocal = new LocalVariable(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC), true);
                subScope.putLeftValue(aFor.name, newLocal);
                Ins.TStore tStore1 = new Ins.TStore(newLocal, next, subScope, LineCol.SYNTHETIC, err);
                instructions.add(tStore1); // name = it.next()

                Ins.Nop nopForContinue = new Ins.Nop();

                for (Statement stmt : aFor.body) {
                        parseStatement(
                                stmt,
                                methodReturnType,
                                subScope,
                                instructions,
                                exceptionTable, nop, nopForContinue, false);
                }
                instructions.add(nopForContinue);
                instructions.add(new Ins.Goto(ifEq));
                instructions.add(nop);
        }

        /**
         * parse while<br><br>
         * while:
         * <pre>
         * while B
         *     A
         * C
         *
         * ==>
         *
         * ifEq B (B==false) goto C
         * A
         * goto while
         * C
         * </pre>
         * do-while:
         * <pre>
         * do
         *     A
         * while B
         * C
         *
         * ==>
         *
         * A
         * ifNe B (B==true) goto A
         * C
         * </pre>
         *
         * @param aWhile           while
         * @param methodReturnType method return type
         * @param scope            scope
         * @param instructions     instruction list
         * @param exceptionTable   exception table
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromWhile(AST.While aWhile, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions, List<ExceptionTable> exceptionTable) throws SyntaxException {
                Ins.Nop nopBreak = new Ins.Nop();
                Ins.Nop nopContinue = new Ins.Nop();

                SemanticScope whileScope = new SemanticScope(scope);

                List<Instruction> ins = new ArrayList<>();
                for (Statement stmt : aWhile.statements) {
                        parseStatement(
                                stmt,
                                methodReturnType,
                                whileScope,
                                ins,
                                exceptionTable, nopBreak, nopContinue, false);
                }

                Value condition = parseValueFromExpression(aWhile.condition, BoolTypeDef.get(), whileScope);

                if (aWhile.doWhile) {
                        /*
                         * do
                         *     A
                         * while B
                         * C
                         *
                         * ==>
                         *
                         * A
                         * nop --------- continue nop
                         * if B goto A
                         * C ----------- break nop
                         */
                        instructions.addAll(ins); // A
                        instructions.add(nopContinue);
                        // loop, if B goto A
                        if (ins.isEmpty()) {
                                // A is empty then goto self
                                Ins.IfNe ifNe = new Ins.IfNe(condition, null, aWhile.line_col());
                                ifNe.setGotoIns(ifNe);
                                instructions.add(ifNe);
                        } else {
                                // A not empty then goto A
                                Ins.IfNe ifNe = new Ins.IfNe(condition, ins.get(0), aWhile.line_col());
                                instructions.add(ifNe);
                        }
                        instructions.add(nopBreak);
                } else {
                        /*
                         * while B
                         *     A
                         * C
                         *
                         * ==>
                         *
                         * if not B goto C
                         * A
                         * nop ------------- continue nop
                         * goto while
                         * C --------------- break nop
                         */
                        Ins.IfEq ifEq; // if B == 0 (false) goto C
                        ifEq = new Ins.IfEq(condition, nopBreak, aWhile.line_col());
                        instructions.add(ifEq); // if not B goto C

                        instructions.addAll(ins); // A
                        instructions.add(nopContinue);
                        instructions.add(new Ins.Goto(ifEq)); // goto while
                        instructions.add(nopBreak); // C
                }
        }

        /**
         * parse if<br><br>
         * <pre>
         * if a
         *     A
         * elseif b
         *     B
         * elseif c
         *     C
         * else
         *     D
         *
         * ==>
         *
         * a ifEq (a!=true) goto nop1
         * A
         * goto nop
         * nop1
         * b ifEq (b!=true) goto nop2
         * B
         * goto nop
         * nop2
         * c ifEq (c!=true) goto nop3
         * C
         * goto nop
         * nop3
         * D
         * nop
         * </pre>
         *
         * @param anIf             if
         * @param methodReturnType method return type
         * @param scope            current scope
         * @param instructions     instruction list
         * @param exceptionTable   exception table
         * @param breakIns         jump to this position when meets break
         * @param continueIns      to this position when meets continue
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromIf(AST.If anIf,
                                            STypeDef methodReturnType,
                                            SemanticScope scope,
                                            List<Instruction> instructions,
                                            List<ExceptionTable> exceptionTable,
                                            Ins.Nop breakIns,
                                            Ins.Nop continueIns) throws SyntaxException {
                Ins.Nop nop = new Ins.Nop();

                for (AST.If.IfPair ifPair : anIf.ifs) {
                        SemanticScope ifScope = new SemanticScope(scope);
                        List<Instruction> instructionList = new ArrayList<>();

                        List<Instruction> ins = new ArrayList<>();
                        for (Statement stmt : ifPair.body) {
                                parseStatement(
                                        stmt,
                                        methodReturnType,
                                        ifScope,
                                        ins,
                                        exceptionTable, breakIns, continueIns, false);
                        }

                        if (ifPair.condition == null) {
                                // it's else
                                instructionList.addAll(ins);
                        } else {
                                // if/elseif

                                Ins.Goto gotoNop = new Ins.Goto(nop); // goto nop
                                Ins.Nop thisNop = new Ins.Nop(); // nop1/nop2/nop3/...

                                Value condition = parseValueFromExpression(ifPair.condition, BoolTypeDef.get(), ifScope);
                                Ins.IfEq ifEq = new Ins.IfEq(condition, thisNop, ifPair.condition.line_col());
                                instructionList.add(ifEq); // a ifEq (a!=true) goto nop
                                instructionList.addAll(ins); // A
                                instructionList.add(gotoNop); // goto nop
                                instructionList.add(thisNop); // nop1
                        }

                        instructions.addAll(instructionList);
                }

                instructions.add(nop); // nop
        }

        /**
         * parse return
         *
         * @param ret              return object
         * @param methodReturnType method return type
         * @param scope            current scope
         * @param instructions     instruction list
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromReturn(AST.Return ret, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions) throws SyntaxException {
                Ins.TReturn tReturn;
                if (ret.exp == null) {
                        if (!methodReturnType.equals(VoidType.get())) {
                                err.SyntaxException("the method is not void but returns nothing", ret.line_col());
                                return;
                        }
                        tReturn = new Ins.TReturn(null, ret.line_col());
                } else {
                        if (methodReturnType.equals(VoidType.get())) {
                                err.SyntaxException("the method is void but returns a value", ret.line_col());
                                return;
                        }
                        Value v = parseValueFromExpression(ret.exp, methodReturnType, scope);

                        tReturn = new Ins.TReturn(v, ret.line_col());
                }
                instructions.add(tReturn);
        }

        /**
         * {@link LtRuntime#putField(Object, String, Object, Class)}
         */
        private SMethodDef Lang_putField;

        /**
         * @return {@link LtRuntime#putField(Object, String, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_putField() throws SyntaxException {
                if (null == Lang_putField) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("putField")) {
                                        Lang_putField = m;
                                        break;
                                }
                        }
                }
                return Lang_putField;
        }

        /**
         * parse assignment<br>
         * the rules are very simple, +=,-=,*=,/=,%= are converted to a=a+b/a-b/a*b/a/b/a%b<br>
         * the parsed result is filled into the given instruction list<br>
         * it's NOT directly invoked in {@link #parseStatement(Statement, STypeDef, SemanticScope, List, List, lt.compiler.semantic.Ins.Nop, lt.compiler.semantic.Ins.Nop, boolean)}<br>
         * but in {@link #parseValueFromAssignment(AST.Assignment, SemanticScope)}<br>
         * the instructions should be {@link ValuePack} instruction list
         *
         * @param assignTo     assignTo -- the value before "="
         * @param assignFrom   the value after "="
         * @param scope        current scope
         * @param instructions instruction list
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromAssignment(Value assignTo,
                                                    Value assignFrom,
                                                    SemanticScope scope,
                                                    List<Instruction> instructions,
                                                    LineCol lineCol) throws SyntaxException {
                // []= means Tastore or <set(?,?) or put(?,?)> ==> (reflectively invoke)
                // []+= means TALoad then Tastore, or get(?) then <set(?,?) or put(?,?)> ==> then set/put step would be invoked reflectively

                // else
                // simply assign `assignFrom` to `assignTo`
                // the following actions would be assign work
                if (assignTo instanceof Ins.GetField) {
                        // field
                        instructions.add(new Ins.PutField(
                                ((Ins.GetField) assignTo).field(),
                                ((Ins.GetField) assignTo).object(),
                                cast(((Ins.GetField) assignTo).field().type(), assignFrom, ((Ins.GetField) assignTo).line_col()),
                                lineCol, err));
                } else if (assignTo instanceof Ins.GetStatic) {
                        // static
                        instructions.add(new Ins.PutStatic(((Ins.GetStatic) assignTo).field(), assignFrom, lineCol, err));
                } else if (assignTo instanceof Ins.TALoad) {
                        // arr[?]
                        Ins.TALoad TALoad = (Ins.TALoad) assignTo;
                        SArrayTypeDef arrayType = (SArrayTypeDef) TALoad.arr().type();
                        if (arrayType.dimension() > 1) {
                                // dimension>1 so it's AASTORE
                                // object[] AASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.AASTORE,
                                        TALoad.index(),
                                        cast(arrayType.type(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(IntTypeDef.get())) {
                                // int[] IASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.IASTORE,
                                        TALoad.index(),
                                        cast(IntTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(LongTypeDef.get())) {
                                // long[] LASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.LASTORE,
                                        TALoad.index(),
                                        cast(LongTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(ShortTypeDef.get())) {
                                // short[] SASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.SASTORE,
                                        TALoad.index(),
                                        cast(ShortTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(ByteTypeDef.get()) || arrayType.type().equals(BoolTypeDef.get())) {
                                // byte[]/boolean[] BASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.BASTORE,
                                        TALoad.index(),
                                        cast(ByteTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(FloatTypeDef.get())) {
                                // float[] FASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.FASTORE,
                                        TALoad.index(),
                                        cast(FloatTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(DoubleTypeDef.get())) {
                                // double[] DASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.DASTORE,
                                        TALoad.index(),
                                        cast(DoubleTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(CharTypeDef.get())) {
                                // char[] CASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.CASTORE,
                                        TALoad.index(),
                                        cast(CharTypeDef.get(), assignFrom, lineCol),
                                        lineCol));
                        } else {
                                // object[] AASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.AASTORE,
                                        TALoad.index(),
                                        cast(arrayType.type(), assignFrom, lineCol),
                                        lineCol));
                        }
                } else if (assignTo instanceof Ins.TLoad) {
                        // local variable
                        instructions.add(new Ins.TStore(
                                ((Ins.TLoad) assignTo).value(),
                                cast(
                                        assignTo.type(),
                                        assignFrom,
                                        lineCol),
                                scope, lineCol, err));
                } else if (assignTo instanceof Ins.InvokeStatic) {
                        // assignTo should be lt.lang.LtRuntime.getField(o,name)
                        // which means
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) assignTo;
                        if (isGetFieldAtRuntime(invokeStatic)) {
                                // dynamically get field
                                // invoke lt.lang.LtRuntime.putField(o,name,value)
                                SMethodDef putField = getLang_putField();
                                Ins.InvokeStatic invoke = new Ins.InvokeStatic(putField, lineCol);
                                invoke.arguments().add(invokeStatic.arguments().get(0));
                                invoke.arguments().add(invokeStatic.arguments().get(1));

                                if (assignFrom.type() instanceof PrimitiveTypeDef) {
                                        assignFrom = boxPrimitive(assignFrom, LineCol.SYNTHETIC);
                                }

                                invoke.arguments().add(assignFrom);
                                invoke.arguments().add(
                                        new Ins.GetClass(scope.type(),
                                                (SClassDef) getTypeWithName("java.lang.Class", lineCol)));

                                // add into instructin list
                                instructions.add(invoke);

                                return;
                        }
                        err.SyntaxException("cannot assign", lineCol);
                        // code won't reach here
                } else if (assignTo instanceof Ins.InvokeDynamic) {
                        // invoke dynamic get(?)
                        // list[?1]=?2
                        // or
                        // map[?1]=?2
                        Ins.InvokeDynamic invokeDynamic = (Ins.InvokeDynamic) assignTo;
                        if (invokeDynamic.methodName().equals("get") && invokeDynamic.arguments().size() > 0) {
                                // the method to invoke should be set
                                List<Value> list = new ArrayList<>();
                                Iterator<Value> it = invokeDynamic.arguments().iterator();
                                Ins.GetClass cls = (Ins.GetClass) it.next(); // class
                                Value target = it.next();
                                it.next(); // it is functional object, and it's NullValue.get()
                                while (it.hasNext()) list.add(it.next());
                                list.add(assignFrom);

                                instructions.add((Instruction) invokeMethodWithArgs(
                                        lineCol,
                                        cls.targetType(),
                                        target,
                                        "set",
                                        list,
                                        scope));
                                return;
                        }
                        err.SyntaxException("cannot assign", lineCol);
                        // code won't reach here
                } else if (assignTo instanceof Ins.InvokeWithTarget) {
                        // the method name should be 'get(?1)'
                        Ins.InvokeWithTarget invoke = (Ins.InvokeWithTarget) assignTo;
                        if (invoke.invokable() instanceof SMethodDef) {
                                SMethodDef method = (SMethodDef) invoke.invokable();
                                if (method.name().equals("get")) {
                                        // the method to invoke should be `set(?1,?2)`
                                        List<Value> list = new ArrayList<>();
                                        list.addAll(invoke.arguments());
                                        list.add(assignFrom);
                                        instructions.add((Instruction) invokeMethodWithArgs(
                                                lineCol,
                                                invoke.target().type(),
                                                invoke.target(),
                                                "set",
                                                list,
                                                scope));
                                        return;
                                }
                        }
                        err.SyntaxException("cannot assign", lineCol);
                        // code won't reach here
                } else {
                        err.SyntaxException("cannot assign", lineCol);
                        // code won't reach here
                }
        }

        /**
         * parse variable def
         *
         * @param variableDef variable definition
         * @param scope       current scope
         * @return retrieved Value or null if no initValue is assigned
         * @throws SyntaxException compile error
         */
        private Value parseInsFromVariableDef(VariableDef variableDef, SemanticScope scope) throws SyntaxException {
                List<Import> imports = fileNameToImport.get(variableDef.line_col().fileName);

                STypeDef type = variableDef.getType() == null

                        ? getTypeWithName(
                        "java.lang.Object",
                        variableDef.line_col())

                        : getTypeWithAccess(
                        variableDef.getType(),
                        imports);

                // try to find field and putField/putStatic
                SFieldDef field = findFieldFromTypeDef(variableDef.getName(), scope.type(), scope.type(),
                        scope.getThis() == null ? FIND_MODE_STATIC : FIND_MODE_ANY,
                        false);

                boolean isLocalVar = false;
                if (field == null) {
                        // field is null, define local variable
                        if (scope.getLeftValue(variableDef.getName()) == null) {
                                isLocalVar = true;
                        } else {
                                err.SyntaxException(variableDef.getName() + " is already defined", variableDef.line_col());
                                return null;
                        }
                }

                if (variableDef.getInit() != null) {
                        // variable def with a init value

                        Value v = parseValueFromExpression(
                                variableDef.getInit(),
                                type,
                                scope);

                        if (isLocalVar) {
                                boolean canChange = true;
                                for (Modifier m : variableDef.getModifiers()) {
                                        if (m.modifier.equals(Modifier.Available.VAL)) {
                                                canChange = false;
                                        }
                                }
                                LocalVariable localVariable = new LocalVariable(type, canChange);
                                scope.putLeftValue(variableDef.getName(), localVariable);
                        }

                        ValuePack pack = new ValuePack(true);
                        if (null != field) {
                                if (field.modifiers().contains(SModifier.STATIC)) {
                                        // putStatic
                                        pack.instructions().add(new Ins.PutStatic(field, v, variableDef.line_col(), err));
                                        Ins.GetStatic getStatic = new Ins.GetStatic(field, variableDef.line_col());
                                        pack.instructions().add(getStatic);
                                } else {
                                        // putField
                                        pack.instructions().add(new Ins.PutField(field, scope.getThis(), v, variableDef.line_col(), err));
                                        Ins.GetField getField = new Ins.GetField(field, scope.getThis(), variableDef.line_col());
                                        pack.instructions().add(getField);
                                }
                        } else {
                                // else field not found
                                // local variable
                                LocalVariable localVariable = (LocalVariable) scope.getLeftValue(variableDef.getName());
                                pack.instructions().add(new Ins.TStore(localVariable, v, scope, variableDef.line_col(), err));
                                Ins.TLoad tLoad = new Ins.TLoad(localVariable, scope, variableDef.line_col());
                                pack.instructions().add(tLoad);
                        }
                        return pack;
                }

                // else ignore the var def
                return null;
        }

        /**
         * parse value from expression<br>
         * <ul>
         * <li>{@link NumberLiteral} =&gt; different kinds of Numbers</li>
         * <li>{@link BoolLiteral} =&gt; {@link BoolValue}</li>
         * <li>{@link StringLiteral} =&gt; {@link StringConstantValue} or {@link CharValue}</li>
         * <li>{@link VariableDef}</li>
         * <li>{@link lt.compiler.syntactic.AST.Invocation} =&gt; {@link lt.compiler.semantic.Ins.New} or {@link lt.compiler.semantic.Ins.Invoke}</li>
         * <li>{@link lt.compiler.syntactic.AST.AsType}</li>
         * <li>{@link lt.compiler.syntactic.AST.Access}</li>
         * <li>{@link lt.compiler.syntactic.AST.Index}</li>
         * <li>{@link OneVariableOperation} {@link UnaryOneVariableOperation}</li>
         * <li>{@link TwoVariableOperation}</li>
         * <li>{@link lt.compiler.syntactic.AST.Assignment}</li>
         * <li>{@link lt.compiler.syntactic.AST.UndefinedExp}</li>
         * <li>{@link lt.compiler.syntactic.AST.Null}</li>
         * <li>{@link lt.compiler.syntactic.AST.ArrayExp} =&gt; array/lt.util.List</li>
         * <li>{@link lt.compiler.syntactic.AST.MapExp} =&gt; lt.util.Map</li>
         * <li>{@link lt.compiler.syntactic.AST.Procedure}</li>
         * <li>{@link lt.compiler.syntactic.AST.Lambda}</li>
         * <li>{@link lt.compiler.syntactic.AST.TypeOf} =&gt; {@link lt.compiler.semantic.Ins.GetClass}</li>
         * <li>{@link lt.compiler.syntactic.AST.AnnoExpression}</li>
         * <li>{@link lt.compiler.syntactic.AST.Require}</li>
         * </ul>
         *
         * @param exp          expression
         * @param requiredType required type (null means no type limit)
         * @param scope        a scope that contains local variables and local methods
         * @return parsed result
         * @throws SyntaxException exception
         */
        private Value parseValueFromExpression(Expression exp, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                List<Import> imports = fileNameToImport.get(exp.line_col().fileName);

                Value v; // retrieved value without type check
                // literals:
                if (exp instanceof NumberLiteral) {
                        try {
                                if (isInt(requiredType, (NumberLiteral) exp, exp.line_col())) {
                                        IntValue intValue = new IntValue(Integer.parseInt(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return intValue;
                                        } else {
                                                return boxPrimitive(intValue, exp.line_col());
                                        }
                                } else if (isLong(requiredType, (NumberLiteral) exp, exp.line_col())) {
                                        LongValue longValue = new LongValue(Long.parseLong(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return longValue;
                                        } else {
                                                return boxPrimitive(longValue, exp.line_col());
                                        }
                                } else if (isShort(requiredType, (NumberLiteral) exp, exp.line_col())) {
                                        ShortValue shortValue = new ShortValue(Short.parseShort(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return shortValue;
                                        } else {
                                                return boxPrimitive(shortValue, exp.line_col());
                                        }
                                } else if (isByte(requiredType, (NumberLiteral) exp, exp.line_col())) {
                                        ByteValue byteValue = new ByteValue(Byte.parseByte(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return byteValue;
                                        } else {
                                                return boxPrimitive(byteValue, exp.line_col());
                                        }
                                } else if (isDouble(requiredType, exp.line_col())) {
                                        DoubleValue doubleValue = new DoubleValue(Double.parseDouble(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return doubleValue;
                                        } else {
                                                return boxPrimitive(doubleValue, exp.line_col());
                                        }
                                } else if (isFloat(requiredType, exp.line_col())) {
                                        FloatValue floatValue = new FloatValue(Float.parseFloat(((NumberLiteral) exp).literal()));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return floatValue;
                                        } else {
                                                return boxPrimitive(floatValue, exp.line_col());
                                        }
                                } else {
                                        err.SyntaxException(exp + " cannot be converted into " + requiredType.fullName(), exp.line_col());
                                        return null;
                                }
                        } catch (NumberFormatException e) {
                                err.SyntaxException(exp + " is not valid " + requiredType, exp.line_col());
                                return null;
                        }
                } else if (exp instanceof BoolLiteral) {
                        if (isBool(requiredType, exp.line_col())) {
                                String literal = ((BoolLiteral) exp).literal();
                                boolean b;
                                switch (literal) {
                                        case "true":
                                        case "yes":
                                                b = true;
                                                break;
                                        case "false":
                                        case "no":
                                                b = false;
                                                break;
                                        default:
                                                throw new LtBug(literal + " for bool literal");
                                }
                                BoolValue boolValue = new BoolValue(b);
                                if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                        return boolValue;
                                } else {
                                        return boxPrimitive(boolValue, exp.line_col());
                                }
                        } else {
                                err.SyntaxException(exp + " cannot be converted into " + requiredType, exp.line_col());
                                return null;
                        }
                } else if (exp instanceof StringLiteral) {
                        String str = ((StringLiteral) exp).literal();
                        // remove "" or ''
                        str = str.substring(1);
                        str = str.substring(0, str.length() - 1);
                        str = unescape(str, exp.line_col());
                        if (isChar(requiredType, (StringLiteral) exp, exp.line_col())) {
                                assert str != null;
                                if (str.length() == 1) {
                                        CharValue charValue = new CharValue(str.charAt(0));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return charValue;
                                        } else {
                                                return boxPrimitive(charValue, exp.line_col());
                                        }
                                } else {
                                        err.SyntaxException(exp + " cannot be converted into char, char must hold one character", exp.line_col());
                                        return null;
                                }
                        } else if (requiredType == null || requiredType.isAssignableFrom(getTypeWithName("java.lang.String", exp.line_col()))) {
                                StringConstantValue s = new StringConstantValue(str);
                                s.setType((SClassDef) getTypeWithName("java.lang.String", exp.line_col()));
                                return s;
                        } else {
                                err.SyntaxException(exp + " cannot be converted into " + requiredType, exp.line_col());
                                return null;
                        }
                } else if (exp instanceof VariableDef) {
                        // variable def
                        // putField/putStatic/TStore
                        return parseInsFromVariableDef((VariableDef) exp, scope);
                } else if (exp instanceof AST.Invocation) {
                        // parse invocation
                        // the result can be invokeXXX or new
                        if (((AST.Invocation) exp).invokeWithNames) {
                                v = parseValueFromInvocationWithNames((AST.Invocation) exp, scope);
                        } else if (((AST.Invocation) exp).exp instanceof AST.Access) {
                                v = parseValueFromInvocation((AST.Invocation) exp, scope);
                        } else {
                                v = parseValueFromInvocationFunctionalObject((AST.Invocation) exp, scope);
                        }
                } else if (exp instanceof AST.AsType) {
                        AST.AsType asType = (AST.AsType) exp;
                        v = parseValueFromExpression(asType.exp, getTypeWithAccess(asType.type, imports), scope);
                } else if (exp instanceof AST.Access) {
                        // parse access
                        if (((AST.Access) exp).name == null) {
                                // name is null, then only parse the expression
                                v = parseValueFromExpression(((AST.Access) exp).exp, requiredType, scope);
                        } else {
                                // the result can be getField/getStatic/xload/invokeStatic(dynamically get field)
                                v = parseValueFromAccess((AST.Access) exp, scope);
                        }
                } else if (exp instanceof AST.Index) {
                        // parse index
                        // the result can be xxload/invokeStatic(dynatically get index)
                        v = parseValueFromIndex((AST.Index) exp, scope);
                } else if (exp instanceof OneVariableOperation | exp instanceof UnaryOneVariableOperation) {
                        // parse one variable operation
                        v = parseValueFromOneVarOp((Operation) exp, scope);
                } else if (exp instanceof TwoVariableOperation) {
                        // parse two variable operation
                        v = parseValueFromTwoVarOp((TwoVariableOperation) exp, scope);
                } else if (exp instanceof AST.Assignment) {
                        v = parseValueFromAssignment((AST.Assignment) exp, scope);
                } else if (exp instanceof AST.UndefinedExp) {
                        v = invoke_Undefined_get(exp.line_col());
                } else if (exp instanceof AST.Null) {
                        v = NullValue.get();
                } else if (exp instanceof AST.ArrayExp) {
                        v = parseValueFromArrayExp((AST.ArrayExp) exp, requiredType, scope);
                } else if (exp instanceof AST.MapExp) {
                        v = parseValueFromMapExp((AST.MapExp) exp, scope);
                } else if (exp instanceof AST.Procedure) {
                        v = parseValueFromProcedure((AST.Procedure) exp, requiredType, scope);
                } else if (exp instanceof AST.Lambda) {
                        v = parseValueFromLambda((AST.Lambda) exp, requiredType, scope);
                } else if (exp instanceof AST.TypeOf) {
                        v = new Ins.GetClass(
                                getTypeWithAccess(((AST.TypeOf) exp).type, imports),
                                (SClassDef) getTypeWithName(
                                        "java.lang.Class",
                                        LineCol.SYNTHETIC
                                ));
                } else if (exp instanceof AST.AnnoExpression) {
                        SAnno anno = new SAnno();
                        AST.Anno astAnno = ((AST.AnnoExpression) exp).anno;
                        STypeDef type = getTypeWithAccess(astAnno.anno, imports);
                        if (!(type instanceof SAnnoDef)) {
                                err.SyntaxException(type + " is not annotation type", astAnno.anno.line_col());
                                return null;
                        }
                        anno.setAnnoDef((SAnnoDef) type);
                        annotationRecorder.put(anno, astAnno);
                        parseAnnoValues(Collections.singleton(anno));
                        v = anno;
                } else if (exp instanceof AST.Require) {
                        // invoke LtRuntime.require(?,?)
                        SMethodDef method = getLang_require();
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, exp.line_col());
                        // caller class
                        invokeStatic.arguments().add(new Ins.GetClass(
                                scope.type(),
                                (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)
                        ));
                        // require file
                        invokeStatic.arguments().add(parseValueFromExpression(
                                ((AST.Require) exp).required,
                                getTypeWithName("java.lang.String", ((AST.Require) exp).required.line_col()),
                                scope));
                        v = invokeStatic;
                } else if (exp instanceof RegexLiteral) {
                        // regex
                        v = parseValueFromRegex((RegexLiteral) exp);
                } else if (exp instanceof AST.New) {
                        v = parseValueFromNew((AST.New) exp, scope);
                } else if (exp instanceof AST.GeneratorSpec) {
                        v = parseValueFromGeneratorSpec((AST.GeneratorSpec) exp);
                } else {
                        throw new LtBug("unknown expression " + exp);
                }
                return cast(requiredType, v, exp.line_col());
        }

        private Value parseValueFromGeneratorSpec(AST.GeneratorSpec gs) throws SyntaxException {
                STypeDef type = getTypeWithAccess(gs.type, fileNameToImport.get(gs.line_col().fileName));
                if (type instanceof SClassDef && !((SClassDef) type).modifiers().contains(SModifier.ABSTRACT)) {
                        Class<?> c;
                        try {
                                c = loadClass(type.fullName());
                        } catch (ClassNotFoundException e) {
                                err.SyntaxException(type.fullName() + " has not been compiled", gs.type.line_col());
                                return null;
                        }
                        if (!SourceGenerator.class.isAssignableFrom(c)) {
                                err.SyntaxException(type.fullName() + " is not implementation of lt::generator::SourceGenerator", gs.type.line_col());
                                return null;
                        }
                        Constructor<?> con;
                        try {
                                con = c.getConstructor();
                        } catch (NoSuchMethodException e) {
                                err.SyntaxException("cannot find constructor : " + type.fullName() + "()", gs.type.line_col());
                                return null;
                        }
                        Method init;
                        Method generate;
                        try {
                                init = SourceGenerator.class.getMethod("init", List.class, ErrorManager.class);
                                generate = SourceGenerator.class.getMethod("generate");
                        } catch (NoSuchMethodException e) {
                                throw new LtBug(e);
                        }
                        String src;
                        try {
                                Object o = con.newInstance();
                                init.invoke(o, gs.ast, err);
                                src = (String) generate.invoke(o);
                        } catch (InvocationTargetException t) {
                                if (t.getTargetException() instanceof SyntaxException) {
                                        throw (SyntaxException) t.getTargetException();
                                } else {
                                        throw new LtBug("caught exception in generator", t);
                                }
                        } catch (Throwable e) {
                                throw new LtBug("caught exception in generator", e);
                        }
                        StringConstantValue v = new StringConstantValue(src);
                        v.setType((SClassDef) getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                        return v;
                } else {
                        err.SyntaxException("Generator should be a class", gs.line_col());
                        return null;
                }
        }

        /**
         * parse value from new
         *
         * @param aNew new
         * @return constructing a new object
         */
        private Value parseValueFromNew(AST.New aNew, SemanticScope scope) throws SyntaxException {
                SClassDef type;
                try {
                        type = (SClassDef) getTypeWithAccess((AST.Access) aNew.invocation.exp,
                                fileNameToImport.get(aNew.line_col().fileName));
                } catch (Throwable t) {
                        err.SyntaxException(aNew.invocation.exp + " is not a class", aNew.line_col());
                        return null;
                }
                assert type != null;
                if (type.modifiers().contains(SModifier.ABSTRACT)) {
                        err.SyntaxException("abstract class cannot be instantiated", aNew.line_col());
                        return null;
                }
                List<Value> argList = new ArrayList<>();
                for (Expression e : aNew.invocation.args) {
                        argList.add(parseValueFromExpression(e, null, scope));
                }
                return constructingNewInst(type, argList, aNew.line_col());
        }

        private SMethodDef Pattern_compile;

        private SMethodDef getPattern_compile() throws SyntaxException {
                if (Pattern_compile == null) {
                        SClassDef Pattern = (SClassDef) getTypeWithName("java.util.regex.Pattern", LineCol.SYNTHETIC);
                        assert Pattern != null;
                        for (SMethodDef m : Pattern.methods()) {
                                if (m.name().equals("compile")
                                        && m.getParameters().size() == 1
                                        && m.getParameters().get(0).type().fullName().equals("java.lang.String")) {
                                        Pattern_compile = m;
                                        break;
                                }
                        }
                }
                if (Pattern_compile == null) throw new LtBug("java.util.regex.Pattern.compile(String) should exist");
                return Pattern_compile;
        }

        /**
         * parse value from regex
         *
         * @param exp regex literal
         * @return invokes Pattern.compile('str')
         */
        private Value parseValueFromRegex(RegexLiteral exp) throws SyntaxException {
                SMethodDef compile = getPattern_compile();
                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(
                        compile, exp.line_col()
                );

                String regexStr = CompileUtil.getRegexStr(exp.literal());
                Pattern p;
                try {
                        p = Pattern.compile(regexStr);
                } catch (PatternSyntaxException e) {
                        err.SyntaxException("Invalid regular expression " + regexStr + " : " + e.getMessage(), exp.line_col());
                        return null;
                }

                StringConstantValue theRegex = new StringConstantValue(p.pattern());
                theRegex.setType((SClassDef) getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                invokeStatic.arguments().add(theRegex);

                return invokeStatic;
        }

        /**
         * {@link LtRuntime#getField(Object, String, Class)}
         */
        private SMethodDef Lang_require = null;

        /**
         * @return {@link LtRuntime#getField(Object, String, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_require() throws SyntaxException {
                if (Lang_require == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;

                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("require")) {
                                        Lang_require = m;
                                        break;
                                }
                        }
                }
                if (Lang_require == null)
                        throw new LtBug("lt.lang.LtRuntime.require(Class,String) should exist");
                return Lang_require;
        }

        /**
         * parse expressions like this:<br>
         * <pre>
         * User(id=1, name='n')
         * </pre>
         *
         * @param invocation the invocation
         * @param scope      current scope
         * @return ValuePack, the pack result is the constructed variable
         * @throws SyntaxException compiling error
         */
        private ValuePack parseValueFromInvocationWithNames(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                STypeDef theType;
                try {
                        theType = getTypeWithAccess((AST.Access) invocation.exp,
                                fileNameToImport.get(invocation.line_col().fileName));
                } catch (Throwable t) {
                        err.SyntaxException(invocation.exp + " is not a type", invocation.exp.line_col());
                        return null;
                }
                if (theType instanceof SInterfaceDef) {
                        err.SyntaxException("cannot instantiate interfaces", invocation.line_col());
                        return null;
                }
                SClassDef classType = (SClassDef) theType;
                assert classType != null;

                SConstructorDef con = null;
                for (SConstructorDef c : classType.constructors()) {
                        if (c.getParameters().isEmpty()) {
                                con = c;
                                break;
                        }
                }

                if (con == null) {
                        err.SyntaxException("cannot find constructor with 0 parameters", invocation.line_col());
                        return null;
                }

                ValuePack valuePack = new ValuePack(true);
                valuePack.setType(theType);

                LocalVariable local = new LocalVariable(theType, false);
                String name = scope.generateTempName();
                scope.putLeftValue(name, local);

                Ins.New aNew = new Ins.New(con, invocation.line_col());
                Ins.TStore store = new Ins.TStore(local, aNew, scope, invocation.line_col(), err);
                valuePack.instructions().add(store);

                for (Expression exp : invocation.args) {
                        VariableDef v = (VariableDef) exp;
                        Expression initValue = v.getInit();

                        String n = v.getName();
                        // ?.x = initValue
                        AST.Assignment ass = new AST.Assignment(
                                new AST.Access(
                                        new AST.Access(null, name, invocation.line_col()),
                                        n,
                                        LineCol.SYNTHETIC
                                ),
                                "=",
                                initValue,
                                LineCol.SYNTHETIC
                        );

                        parseStatement(
                                ass, null, scope, valuePack.instructions(), null, null, null, false
                        );
                }

                valuePack.instructions().add(new Ins.TLoad(local, scope, invocation.line_col()));
                return valuePack;
        }

        /**
         * get interfaces defined in package {@link lt.lang.function}
         *
         * @param argCount argument count
         * @param lineCol  line-col
         * @return Function0 to Function 26
         * @throws SyntaxException exception
         */
        private SInterfaceDef getDefaultLambdaFunction(int argCount, LineCol lineCol) throws SyntaxException {
                if (argCount > 26) {
                        err.SyntaxException("too may arguments for a lambda expression, maximum arg count is 26", lineCol);
                        return null;
                }
                String className = "lt.lang.function.Function" + argCount;
                return (SInterfaceDef) getTypeWithName(className, lineCol);
        }

        /**
         * retrieve abstract method and possible constructor for the lambda
         *
         * @param requiredType                         required type
         * @param constructorWithZeroParamAndCanAccess the constructor, use array[0] to store the constructor, maybe null
         * @param methodToOverride                     the method to override, use array[0] to store the method, not null
         * @return true if lambda can be used on the required type. false otherwise
         */
        private boolean getMethodForLambda(STypeDef requiredType,
                                           SConstructorDef[] constructorWithZeroParamAndCanAccess,
                                           SMethodDef[] methodToOverride) {
                if (requiredType instanceof SClassDef) {
                        if (((SClassDef) requiredType).modifiers().contains(SModifier.ABSTRACT)) {
                                SClassDef c = (SClassDef) requiredType;
                                // check constructors
                                for (SConstructorDef con : c.constructors()) {
                                        if (con.getParameters().size() == 0) {
                                                // 0 param
                                                if (con.modifiers().contains(SModifier.PUBLIC)) {
                                                        // constructor can access
                                                        constructorWithZeroParamAndCanAccess[0] = con;
                                                        break;
                                                }
                                        }
                                }
                                if (constructorWithZeroParamAndCanAccess[0] != null) {

                                        // check unimplemented methods
                                        int count = 0;
                                        // record all abstract classes
                                        List<SClassDef> classes = new ArrayList<>();
                                        while (c.modifiers().contains(SModifier.ABSTRACT)) {
                                                classes.add(c);
                                                c = c.parent();
                                        }
                                        // check class abstract methods
                                        out:
                                        for (SClassDef cls : classes) {
                                                for (SMethodDef m : cls.methods()) {
                                                        if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                                                boolean isOverridden = false;
                                                                for (SMethodDef o : m.overridden()) {
                                                                        //noinspection SuspiciousMethodCalls
                                                                        if (classes.contains(o.declaringType())) {
                                                                                isOverridden = true;
                                                                                break;
                                                                        }
                                                                }
                                                                if (!isOverridden) {
                                                                        ++count;
                                                                        if (count > 1) break out;
                                                                        methodToOverride[0] = m;
                                                                }
                                                        }
                                                }
                                        }
                                        if (count <= 1) {
                                                // record all interfaces
                                                Set<SInterfaceDef> interfaces = new HashSet<>();
                                                Queue<SInterfaceDef> q = new ArrayDeque<>();
                                                for (SInterfaceDef i : ((SClassDef) requiredType).superInterfaces()) {
                                                        q.add(i);
                                                        interfaces.add(i);
                                                }
                                                while (!q.isEmpty()) {
                                                        SInterfaceDef i = q.remove();
                                                        for (SInterfaceDef ii : i.superInterfaces()) {
                                                                interfaces.add(ii);
                                                                q.add(ii);
                                                        }
                                                }
                                                // check interfaces
                                                out:
                                                for (SInterfaceDef i : interfaces) {
                                                        for (SMethodDef m : i.methods()) {
                                                                if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                                                        boolean isOverridden = false;
                                                                        for (SMethodDef o : m.overridden()) {
                                                                                //noinspection SuspiciousMethodCalls
                                                                                if (interfaces.contains(o.declaringType())
                                                                                        ||
                                                                                        classes.contains(o.declaringType())) {
                                                                                        isOverridden = true;
                                                                                        break;
                                                                                }
                                                                        }
                                                                        if (!isOverridden) {
                                                                                ++count;
                                                                                if (count > 1) break out;
                                                                                methodToOverride[0] = m;
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                        if (count == 1) return true;
                                }
                        }
                } else if (requiredType instanceof SInterfaceDef) {
                        int count = 0;
                        // record all super interfaces
                        Set<SInterfaceDef> interfaces = new HashSet<>();
                        Queue<SInterfaceDef> q = new ArrayDeque<>();
                        q.add((SInterfaceDef) requiredType);
                        interfaces.add((SInterfaceDef) requiredType);
                        while (!q.isEmpty()) {
                                SInterfaceDef i = q.remove();
                                for (SInterfaceDef ii : i.superInterfaces()) {
                                        interfaces.add(ii);
                                        q.add(ii);
                                }
                        }
                        // check interfaces
                        out:
                        for (SInterfaceDef i : interfaces) {
                                for (SMethodDef m : i.methods()) {
                                        if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                                // check whether it's overridden
                                                boolean isOverridden = false;
                                                for (SMethodDef o : m.overridden()) {
                                                        //noinspection SuspiciousMethodCalls
                                                        if (interfaces.contains(o.declaringType())) {
                                                                // overridden
                                                                isOverridden = true;
                                                        }
                                                }
                                                if (!isOverridden) {
                                                        ++count;
                                                        if (count > 1) break out;
                                                        methodToOverride[0] = m;
                                                }
                                        }
                                }
                        }
                        if (count == 1) return true;
                }

                return false;
        }

        /**
         * parse lambda<br>
         * <ol>
         * <li>the function is Functional Interface, and current method is static: use JDK {@link java.lang.invoke.LambdaMetafactory}</li>
         * <li>the function is Functional Abstract Class, or current method is non-static : create a new class for the lambda</li>
         * </ol>
         * <tt>Functional Abstract Class</tt> means an abstract class with accessible constructor whose parameter count is 0 and only one unimplemented method.<br>
         * if not using {@link java.lang.invoke.LambdaMetafactory}, the generated class would be in the same package of the caller class<br>
         * the class and constructor would have no access modifiers<br>
         * the constructor takes 2 or 3 params: 1. MethodHandle (required), 2. Instance (omitted if the caller method is static), 3. captured local variables (required)<br>
         * when called, it creates a new List based on the 3rd param from constructor<br>
         * and add(0, Instance) (omitted if the caller method is static), and foreach argument passed in, add(the argument)<br>
         * then it invokes {@link java.lang.invoke.MethodHandle#invokeWithArguments(List)}<br>
         * finally, it return the result or simple return if the abstract method returns void<br>
         * generally the class look like this in java <br>
         * (assume that the 2nd param exists)<br>
         * <pre>
         * class LambdaClassName extends SomeFunctionalAbstractClass { // may be implements SomeFunctionalInterface
         *         MethodHandle methodHandle;
         *         Object o;
         *         List local;
         *         LambdaClassName(MethodHandle methodHandle, Object o, List local){
         *                 this.methodHandle=methodHandle;
         *                 this.o=o;
         *                 this.local=local;
         *         }
         *         public Object theAbstractMethodToImpl(Object x,Object y){
         *                 List newList=new LinkedList(local);
         *                 newList.add(0, o);
         *                 newList.add(x);
         *                 newList.add(y);
         *
         *                 return methodHandle.invokeWithArguments(newList);
         *         }
         *
         * }
         * </pre>
         *
         * @param lambda       lambda
         * @param requiredType required type
         * @param scope        current scope
         * @return return the new instance
         * @throws SyntaxException compile error
         * @see #buildAClassForLambda(STypeDef, boolean, SMethodDef, SConstructorDef, SInterfaceDef, boolean)
         */
        private Value parseValueFromLambda(AST.Lambda lambda, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                SMethodDef methodToOverride;
                SConstructorDef constructorWithZeroParamAndCanAccess = null;
                if (requiredType == null || requiredType.fullName().equals("java.lang.Object")) {
                        SInterfaceDef interfaceDef = getDefaultLambdaFunction(lambda.params.size(), lambda.line_col());
                        assert interfaceDef != null;

                        requiredType = interfaceDef;
                        methodToOverride = interfaceDef.methods().get(0);
                } else {
                        // examine whether it's a functional interface
                        // or it's an abstract class with only one unimplemented method and accessible constructor with no params
                        SConstructorDef[] cons_arr = new SConstructorDef[1];
                        SMethodDef[] method_arr = new SMethodDef[1];
                        boolean valid = getMethodForLambda(requiredType, cons_arr, method_arr);
                        methodToOverride = method_arr[0];
                        constructorWithZeroParamAndCanAccess = cons_arr[0];

                        if (!valid) {
                                err.SyntaxException("lambda should be subtype of functional interface or abstract class with only one unimplemented method and a constructor with no params, but got " + requiredType, lambda.line_col());
                                return null;
                        }
                }

                if (methodToOverride == null) throw new LtBug("methodToOverride should not be null");
                if (methodToOverride.getParameters().size() != lambda.params.size()) {
                        err.SyntaxException("lambda parameter count differs from " + methodToOverride, lambda.line_col());
                        return null;
                }

                String methodName = methodToOverride.name() + "$lambda$";
                int i = 0;
                while (scope.getInnerMethod(methodName + i) != null) ++i;
                methodName += i;
                STypeDef returnType = methodToOverride.getReturnType();
                MethodDef methodDef = new MethodDef(
                        methodName,
                        Collections.emptySet(),
                        new AST.Access(
                                returnType.pkg() == null
                                        ? null
                                        : new AST.PackageRef(returnType.pkg(), LineCol.SYNTHETIC),
                                returnType.fullName().contains(".")
                                        ? returnType.fullName().substring(returnType.fullName().lastIndexOf('.') + 1)
                                        : returnType.fullName(),
                                LineCol.SYNTHETIC
                        ),
                        lambda.params,
                        Collections.emptySet(),
                        lambda.statements,
                        LineCol.SYNTHETIC
                );
                SMethodDef method = parseInnerMethod(methodDef, scope);
                methodToStatements.put(method, lambda.statements);

                List<Value> args = new ArrayList<>();
                assert method != null;
                args.addAll(
                        scope.getLeftValues(method.getParameters().size() - lambda.params.size())
                                .stream()
                                .map(l -> new Ins.TLoad(l, scope, LineCol.SYNTHETIC)).collect(Collectors.toList())
                );

                // interface and current method is static
                if (requiredType instanceof SInterfaceDef && scope.getThis() == null) {
                        // use Lambda Bootstrap method
                        Ins.InvokeDynamic invokeDynamic = new Ins.InvokeDynamic(
                                getLambdaMetafactory_metafactory(),
                                methodToOverride.name(),
                                args,
                                methodToOverride.declaringType(),
                                Dynamic.INVOKE_STATIC,
                                lambda.line_col());
                        // samMethodType
                        invokeDynamic.indyArgs().add(
                                new MethodTypeValue(methodToOverride.getParameters().stream().map(SParameter::type).collect(Collectors.toList()),
                                        methodToOverride.getReturnType(),
                                        getMethodType_Class()));
                        // implMethod
                        invokeDynamic.indyArgs().add(
                                new MethodHandleValue(
                                        method,
                                        method.modifiers().contains(SModifier.STATIC)
                                                ? Dynamic.INVOKE_STATIC
                                                : Dynamic.INVOKE_SPECIAL,
                                        getMethodHandle_Class()
                                ));
                        // instantiatedMethodType
                        invokeDynamic.indyArgs().add(
                                new MethodTypeValue(methodToOverride.getParameters().stream().map(SParameter::type).collect(Collectors.toList()),
                                        methodToOverride.getReturnType(),
                                        getMethodType_Class()));
                        return invokeDynamic;
                } else {
                        // inherit from abstract class
                        if (constructorWithZeroParamAndCanAccess == null && !(requiredType instanceof SInterfaceDef))
                                throw new LtBug("constructorWithZeroParamAndCanAccess should not be null");
                        // construct a class
                        // class XXX(methodHandle:MethodHandle, o:Object, local:List) : C
                        //     methodToOverride(xxx)
                        //         local.add(?)
                        //         ...
                        //         methodHandle.invokeExact(o,local)

                        // constructor arguments
                        List<Value> consArgs = new ArrayList<>();
                        // methodHandle
                        consArgs.add(new MethodHandleValue(
                                method,
                                method.modifiers().contains(SModifier.STATIC)
                                        ? Dynamic.INVOKE_STATIC
                                        : Dynamic.INVOKE_SPECIAL,
                                getMethodHandle_Class()
                        ));
                        // o
                        if (scope.getThis() != null) consArgs.add(scope.getThis());
                        // local
                        Ins.NewList newList = new Ins.NewList(getTypeWithName("java.util.LinkedList", LineCol.SYNTHETIC));
                        for (Value arg : args) {
                                if (arg.type() instanceof PrimitiveTypeDef) {
                                        arg = boxPrimitive(arg, LineCol.SYNTHETIC);
                                }
                                newList.initValues().add(arg);
                        }
                        consArgs.add(newList);

                        boolean isInterface = requiredType instanceof SInterfaceDef;
                        SClassDef builtClass = buildAClassForLambda(
                                scope.type(), scope.getThis() == null, methodToOverride,
                                constructorWithZeroParamAndCanAccess,
                                isInterface ? (SInterfaceDef) requiredType : null,
                                isInterface);

                        SConstructorDef cons = builtClass.constructors().get(0);
                        Ins.New aNew = new Ins.New(cons, LineCol.SYNTHETIC);
                        aNew.args().addAll(consArgs);

                        return aNew;
                }
        }

        private SClassDef Object_Class;

        private SClassDef getObject_Class() throws SyntaxException {
                if (Object_Class == null) {
                        Object_Class = (SClassDef) getTypeWithName("java.lang.Object", LineCol.SYNTHETIC);
                }
                return Object_Class;
        }

        /**
         * build a class for lambda<br>
         * see {@link #parseValueFromLambda(AST.Lambda, STypeDef, SemanticScope)} to see the class structure
         *
         * @param lambdaClassType  the caller method's declaring type
         * @param isStatic         the method is static (then omit the 2nd param)
         * @param methodToOverride the chosen method to override
         * @param superConstructor the constructor to invoke (or null if it's interface)
         * @param interfaceType    the interface type (or null if it's class)
         * @param isInterface      the type to extend/implement is interface
         * @return generated class (SClassDef form)
         * @throws SyntaxException compile error
         */
        private SClassDef buildAClassForLambda(STypeDef lambdaClassType, boolean isStatic, SMethodDef methodToOverride,
                                               SConstructorDef superConstructor,
                                               SInterfaceDef interfaceType,
                                               boolean isInterface) throws SyntaxException {
                // class
                SClassDef sClassDef = new SClassDef(false, LineCol.SYNTHETIC);
                typeDefSet.add(sClassDef);
                SemanticScope scope = new SemanticScope(sClassDef);

                if (isInterface) {
                        sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        sClassDef.superInterfaces().add(interfaceType);
                } else {
                        sClassDef.setParent((SClassDef) methodToOverride.declaringType());
                }
                sClassDef.setPkg(lambdaClassType.pkg());
                String className = lambdaClassType.fullName() + "$Latte$Lambda$";
                int i = 0;
                while (types.containsKey(className + i)) ++i;
                className += i;
                sClassDef.setFullName(className);
                types.put(className, sClassDef);

                sClassDef.modifiers().add(SModifier.PUBLIC);

                // fields
                // methodHandle
                SFieldDef f1 = new SFieldDef(LineCol.SYNTHETIC);
                f1.setName("methodHandle");
                f1.setType(getMethodHandle_Class());
                sClassDef.fields().add(f1);
                f1.setDeclaringType(sClassDef);
                // o
                SFieldDef f2 = null;
                if (!isStatic) {
                        f2 = new SFieldDef(LineCol.SYNTHETIC);
                        f2.setName("o");
                        f2.setType(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        sClassDef.fields().add(f2);
                        f2.setDeclaringType(sClassDef);
                }
                // local
                SFieldDef f3 = new SFieldDef(LineCol.SYNTHETIC);
                f3.setName("local");
                f3.setType(getTypeWithName("java.util.List", LineCol.SYNTHETIC));
                sClassDef.fields().add(f3);
                f3.setDeclaringType(sClassDef);

                // constructor
                SConstructorDef con = new SConstructorDef(LineCol.SYNTHETIC);
                sClassDef.constructors().add(con);
                con.setDeclaringType(sClassDef);
                SemanticScope conScope = new SemanticScope(scope);
                conScope.setThis(new Ins.This(sClassDef));
                if (isInterface) {
                        con.statements().add(new Ins.InvokeSpecial(
                                conScope.getThis(),
                                getObject_Class().constructors().get(0),
                                LineCol.SYNTHETIC
                        ));
                } else {
                        con.statements().add(new Ins.InvokeSpecial(
                                conScope.getThis(),
                                superConstructor,
                                LineCol.SYNTHETIC
                        ));
                }
                con.modifiers().add(SModifier.PUBLIC);
                // p1
                SParameter p1 = new SParameter();
                p1.setType(getMethodHandle_Class());
                con.getParameters().add(p1);
                conScope.putLeftValue("p1", p1);
                con.statements().add(new Ins.PutField(
                        f1,
                        conScope.getThis(),
                        new Ins.TLoad(p1, conScope, LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC, err
                ));
                // p2
                if (!isStatic) {
                        SParameter p2 = new SParameter();
                        p2.setType(getObject_Class());
                        con.getParameters().add(p2);
                        conScope.putLeftValue("p2", p2);
                        con.statements().add(new Ins.PutField(
                                f2,
                                conScope.getThis(),
                                new Ins.TLoad(p2, conScope, LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC, err
                        ));
                }
                // p3
                SParameter p3 = new SParameter();
                p3.setType(getTypeWithName("java.util.List", LineCol.SYNTHETIC));
                con.getParameters().add(p3);
                conScope.putLeftValue("p3", p3);
                con.statements().add(new Ins.PutField(
                        f3,
                        conScope.getThis(),
                        new Ins.TLoad(p3, conScope, LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC, err
                ));

                // method
                SMethodDef theMethod = new SMethodDef(LineCol.SYNTHETIC);
                SemanticScope meScope = new SemanticScope(scope);
                meScope.setThis(new Ins.This(sClassDef));
                sClassDef.methods().add(theMethod);
                theMethod.setDeclaringType(sClassDef);
                theMethod.setName(methodToOverride.name());
                theMethod.setReturnType(methodToOverride.getReturnType());
                theMethod.modifiers().add(SModifier.PUBLIC);
                for (SParameter param : methodToOverride.getParameters()) {
                        SParameter mp = new SParameter();
                        mp.setType(param.type());
                        theMethod.getParameters().add(mp);

                        String name = meScope.generateTempName();
                        meScope.putLeftValue(name, mp);
                        mp.setName(name);
                }
                // new ArrayList
                SClassDef LinkedList_Type = (SClassDef) getTypeWithName("java.util.LinkedList", LineCol.SYNTHETIC);
                SConstructorDef LinkedList_Con = null;
                assert LinkedList_Type != null;
                for (SConstructorDef co : LinkedList_Type.constructors()) {
                        if (co.getParameters().size() == 1 && co.getParameters().get(0).type().fullName().equals("java.util.Collection")) {
                                LinkedList_Con = co;
                                break;
                        }
                }
                if (LinkedList_Con == null)
                        throw new LtBug("java.util.LinkedList should have constructor LinkedList(Collection)");
                Ins.New aNew = new Ins.New(LinkedList_Con, LineCol.SYNTHETIC);
                aNew.args().add(new Ins.GetField(f3, meScope.getThis(), LineCol.SYNTHETIC));
                LocalVariable localVariable = new LocalVariable(LinkedList_Type, false);
                meScope.putLeftValue("list", localVariable);
                theMethod.statements().add(
                        new Ins.TStore(localVariable, aNew, meScope, LineCol.SYNTHETIC, err)
                );
                // add all params
                SMethodDef LinkedList_add = null;
                for (SMethodDef m : LinkedList_Type.methods()) {
                        if (m.name().equals("add") && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().fullName().equals("java.lang.Object")) {
                                LinkedList_add = m;
                                break;
                        }
                }
                if (LinkedList_add == null) throw new LtBug("java.util.LinkedList should have method add(Object)");
                for (SParameter param : theMethod.getParameters()) {
                        LeftValue mp = meScope.getLeftValue(param.name());
                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(
                                new Ins.TLoad(localVariable, meScope, LineCol.SYNTHETIC),
                                LinkedList_add,
                                LineCol.SYNTHETIC
                        );
                        invokeVirtual.arguments().add(
                                new Ins.TLoad(mp, meScope, LineCol.SYNTHETIC)
                        );
                        theMethod.statements().add(invokeVirtual);
                }
                // add o if not static
                if (!isStatic) {
                        SMethodDef LinkedList_add2 = null;
                        for (SMethodDef m : LinkedList_Type.methods()) {
                                if (m.name().equals("add") && m.getParameters().size() == 2
                                        && m.getParameters().get(0).type().equals(IntTypeDef.get())
                                        && m.getParameters().get(1).type().fullName().equals("java.lang.Object")) {
                                        LinkedList_add2 = m;
                                        break;
                                }
                        }
                        if (LinkedList_add2 == null)
                                throw new LtBug("java.util.LinkedList should have method add(int,Object)");
                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(
                                new Ins.TLoad(localVariable, meScope, LineCol.SYNTHETIC),
                                LinkedList_add2,
                                LineCol.SYNTHETIC
                        );
                        invokeVirtual.arguments().add(new IntValue(0));
                        invokeVirtual.arguments().add(new Ins.GetField(f2, meScope.getThis(), LineCol.SYNTHETIC));
                        theMethod.statements().add(invokeVirtual);
                }

                /* invoke println(list)
                SClassDef cls = (SClassDef) getTypeWithName("lt.lang.Utils", LineCol.SYNTHETIC);
                SMethodDef println = null;
                for (SMethodDef m : cls.methods()) {
                        if (m.name().equals("println") && m.getParameters().size() == 1 && m.getParameters().get(0).type().fullName().equals("java.lang.Object")) {
                                println = m;
                                break;
                        }
                }
                Ins.InvokeStatic is = new Ins.InvokeStatic(println, LineCol.SYNTHETIC);
                is.arguments().add(new Ins.TLoad(localVariable, meScope, LineCol.SYNTHETIC));
                theMethod.statements().add(is);
                */

                // invoke the method handle
                SMethodDef MethodHandle_invokeWithArguments = null;
                for (SMethodDef m : getMethodHandle_Class().methods()) {
                        if (m.name().equals("invokeWithArguments")
                                && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().fullName().equals("java.util.List")) {
                                MethodHandle_invokeWithArguments = m;
                                break;
                        }
                }
                if (MethodHandle_invokeWithArguments == null)
                        throw new LtBug("java.lang.invoke.MethodHandle should have method invokeWithArguments(Object[])");
                Ins.InvokeVirtual invokeMethodHandle = new Ins.InvokeVirtual(
                        new Ins.GetField(f1, meScope.getThis(), LineCol.SYNTHETIC),
                        MethodHandle_invokeWithArguments,
                        LineCol.SYNTHETIC
                );
                invokeMethodHandle.arguments().add(
                        new Ins.TLoad(localVariable, meScope, LineCol.SYNTHETIC)
                );

                // return or not return
                if (theMethod.getReturnType().equals(VoidType.get())) {
                        theMethod.statements().add(invokeMethodHandle);
                } else {
                        theMethod.statements().add(
                                new Ins.TReturn(
                                        cast(theMethod.getReturnType(),
                                                invokeMethodHandle,
                                                LineCol.SYNTHETIC),
                                        LineCol.SYNTHETIC
                                )
                        );
                }

                return sClassDef;
        }

        /**
         * {@link java.lang.invoke.MethodHandle}
         */
        private SClassDef MethodHandle_Class;

        /**
         * @return {@link java.lang.invoke.MethodHandle}
         * @throws SyntaxException exception
         */
        private SClassDef getMethodHandle_Class() throws SyntaxException {
                if (MethodHandle_Class == null) {
                        MethodHandle_Class = (SClassDef) getTypeWithName("java.lang.invoke.MethodHandle", LineCol.SYNTHETIC);
                }
                return MethodHandle_Class;
        }

        /**
         * {@link java.lang.invoke.MethodType}
         */
        private SClassDef MethodType_Class;

        /**
         * @return {@link java.lang.invoke.MethodType}
         * @throws SyntaxException exception
         */
        private SClassDef getMethodType_Class() throws SyntaxException {
                if (MethodType_Class == null) {
                        MethodType_Class = (SClassDef) getTypeWithName("java.lang.invoke.MethodType", LineCol.SYNTHETIC);
                }
                return MethodType_Class;
        }

        /**
         * {@link java.lang.invoke.LambdaMetafactory}
         */
        private SMethodDef LambdaMetafactory_metafactory;

        /**
         * @return {@link java.lang.invoke.LambdaMetafactory}
         * @throws SyntaxException exception
         */
        private SMethodDef getLambdaMetafactory_metafactory() throws SyntaxException {
                if (LambdaMetafactory_metafactory == null) {
                        SClassDef LambdaMetafactory = (SClassDef) getTypeWithName("java.lang.invoke.LambdaMetafactory", LineCol.SYNTHETIC);
                        assert LambdaMetafactory != null;
                        for (SMethodDef m : LambdaMetafactory.methods()) {
                                if (m.name().equals("metafactory") && m.getParameters().size() == 6) {
                                        LambdaMetafactory_metafactory = m;
                                        break;
                                }
                        }
                }
                return LambdaMetafactory_metafactory;
        }

        /**
         * parse procedure<br>
         * first create an inner method, then invoke it
         *
         * @param procedure    the procedure
         * @param requiredType required type
         * @param scope        current scope
         * @return the invocation of the inner method
         * @throws SyntaxException compile error
         */
        private Value parseValueFromProcedure(AST.Procedure procedure, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                String methodName = "procedure$0";
                int i = 1;
                while (scope.getInnerMethod(methodName) != null) {
                        methodName = "procedure$" + i;
                        ++i;
                }

                if (requiredType == null) {
                        requiredType = getTypeWithName("java.lang.Object", LineCol.SYNTHETIC);
                }

                assert requiredType != null;
                MethodDef methodDef = new MethodDef(
                        methodName, Collections.emptySet(),
                        new AST.Access(
                                requiredType.pkg() == null ? null : new AST.PackageRef(requiredType.pkg(), LineCol.SYNTHETIC),
                                requiredType.fullName().contains(".")
                                        ? requiredType.fullName().substring(requiredType.fullName().lastIndexOf('.') + 1)
                                        : requiredType.fullName(),
                                LineCol.SYNTHETIC
                        )
                        , Collections.emptyList(), Collections.emptySet(),
                        procedure.statements, procedure.line_col()
                );
                parseInnerMethod(methodDef, scope);
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(null, methodName, procedure.line_col()), Collections.emptyList(), false, procedure.line_col());
                return parseValueFromInvocation(invocation, scope);
        }

        /**
         * parse MapExp<br>
         * details are handled by {@link CodeGenerator#buildNewMap(MethodVisitor, CodeInfo, Ins.NewMap)}
         *
         * @param mapExp map expression (json format)
         * @param scope  current scope
         * @return {@link lt.compiler.semantic.Ins.NewMap}
         * @throws SyntaxException compile error
         */
        private Value parseValueFromMapExp(AST.MapExp mapExp, SemanticScope scope) throws SyntaxException {
                Ins.NewMap newMap = new Ins.NewMap(getTypeWithName("lt.util.Map", mapExp.line_col()));

                SClassDef Object_type = (SClassDef) getTypeWithName("java.lang.Object", mapExp.line_col());
                for (Map.Entry<Expression, Expression> expEntry : mapExp.map.entrySet()) {
                        Value key = parseValueFromExpression(expEntry.getKey(), Object_type, scope);
                        Value value = parseValueFromExpression(expEntry.getValue(), Object_type, scope);
                        assert key != null;
                        assert value != null;

                        if (key.type() instanceof PrimitiveTypeDef) {
                                key = boxPrimitive(key, LineCol.SYNTHETIC);
                        }
                        if (value.type() instanceof PrimitiveTypeDef) {
                                value = boxPrimitive(value, LineCol.SYNTHETIC);
                        }
                        newMap.initValues().put(key, value);
                }
                return newMap;
        }

        /**
         * parse ArrayExp<br>
         * the array could be <tt>int[]</tt> or <tt>int[][]</tt> or <tt>Object[]</tt>,<br>
         * and can also be {@link lt.compiler.semantic.Ins.NewList} (LinkedList)<br>
         * it's based on the <tt>requiredType</tt>, LinkedList is the default
         *
         * @param arrayExp     array expression
         * @param requiredType required type
         * @param scope        current scope
         * @return new array or new list
         * @throws SyntaxException compile error
         */
        private Value parseValueFromArrayExp(AST.ArrayExp arrayExp, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                // first build an array
                if (requiredType instanceof SArrayTypeDef
                        && ((SArrayTypeDef) requiredType).dimension() == 1
                        && ((SArrayTypeDef) requiredType).type() instanceof PrimitiveTypeDef) {
                        STypeDef type = ((SArrayTypeDef) requiredType).type();
                        IntValue count = new IntValue(arrayExp.list.size());

                        int mode;
                        int storeMode;
                        if (type.equals(IntTypeDef.get())) {
                                mode = Ins.NewArray.NewIntArray;
                                storeMode = Ins.TAStore.IASTORE;
                        } else if (type.equals(LongTypeDef.get())) {
                                mode = Ins.NewArray.NewLongArray;
                                storeMode = Ins.TAStore.LASTORE;
                        } else if (type.equals(ShortTypeDef.get())) {
                                mode = Ins.NewArray.NewShortArray;
                                storeMode = Ins.TAStore.SASTORE;
                        } else if (type.equals(ByteTypeDef.get())) {
                                mode = Ins.NewArray.NewByteArray;
                                storeMode = Ins.TAStore.BASTORE;
                        } else if (type.equals(BoolTypeDef.get())) {
                                mode = Ins.NewArray.NewBoolArray;
                                storeMode = Ins.TAStore.BASTORE;
                        } else if (type.equals(CharTypeDef.get())) {
                                mode = Ins.NewArray.NewCharArray;
                                storeMode = Ins.TAStore.CASTORE;
                        } else if (type.equals(FloatTypeDef.get())) {
                                mode = Ins.NewArray.NewFloatArray;
                                storeMode = Ins.TAStore.FASTORE;
                        } else if (type.equals(DoubleTypeDef.get())) {
                                mode = Ins.NewArray.NewDoubleArray;
                                storeMode = Ins.TAStore.DASTORE;
                        } else throw new LtBug("unknown primitive type " + type);

                        Ins.NewArray newArray = new Ins.NewArray(count, mode, storeMode, requiredType);
                        for (Expression exp : arrayExp.list) {
                                newArray.initValues().add(parseValueFromExpression(exp, type, scope));
                        }
                        return newArray;
                } else if (requiredType instanceof SArrayTypeDef) {
                        // ANewArray
                        SArrayTypeDef arrayTypeDef = (SArrayTypeDef) requiredType;
                        STypeDef componentType;
                        if (arrayTypeDef.dimension() == 1) {
                                componentType = arrayTypeDef.type();
                        } else {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < arrayTypeDef.dimension() - 1; ++i) sb.append("[");
                                if (arrayTypeDef.type().equals(IntTypeDef.get())) {
                                        sb.append("I");
                                } else if (arrayTypeDef.type().equals(LongTypeDef.get())) {
                                        sb.append("J");
                                } else if (arrayTypeDef.type().equals(ShortTypeDef.get())) {
                                        sb.append("S");
                                } else if (arrayTypeDef.type().equals(BoolTypeDef.get())) {
                                        sb.append("Z");
                                } else if (arrayTypeDef.type().equals(ByteTypeDef.get())) {
                                        sb.append("B");
                                } else if (arrayTypeDef.type().equals(CharTypeDef.get())) {
                                        sb.append("C");
                                } else if (arrayTypeDef.type().equals(FloatTypeDef.get())) {
                                        sb.append("F");
                                } else if (arrayTypeDef.type().equals(DoubleTypeDef.get())) {
                                        sb.append("D");
                                } else {
                                        sb.append("L").append(arrayTypeDef.type().fullName()).append(";");
                                }
                                componentType = getTypeWithName(sb.toString(), arrayExp.line_col());
                        }
                        Ins.ANewArray aNewArray = new Ins.ANewArray(arrayTypeDef, componentType, new IntValue(arrayExp.list.size()));
                        for (Expression exp : arrayExp.list) {
                                aNewArray.initValues().add(
                                        parseValueFromExpression(exp, componentType, scope)
                                );
                        }
                        return aNewArray;
                } else {
                        // construct an ArrayList
                        Ins.NewList newList = new Ins.NewList(
                                getTypeWithName("lt.util.List", arrayExp.line_col())
                        );
                        SClassDef Object_type = (SClassDef) getTypeWithName("java.lang.Object", arrayExp.line_col());
                        // init values
                        for (Expression exp : arrayExp.list) {
                                Value v = parseValueFromExpression(exp, Object_type, scope);
                                assert v != null;
                                if (v.type() instanceof PrimitiveTypeDef) {
                                        v = boxPrimitive(v, LineCol.SYNTHETIC);
                                }
                                newList.initValues().add(
                                        v
                                );
                        }
                        return newList;
                }
        }

        /**
         * parse assignment<br>
         * generate a ValuePack, then invoke {@link #parseInstructionFromAssignment(Value, Value, SemanticScope, List, LineCol)}<br>
         * then add a retrieve expression to the pack
         *
         * @param exp   assignment expression
         * @param scope current scope
         * @return ValuePack
         * @throws SyntaxException compile error
         */
        private ValuePack parseValueFromAssignment(AST.Assignment exp, SemanticScope scope) throws SyntaxException {
                ValuePack pack = new ValuePack(true); // value pack
                // assignment
                // =/+=/-=/*=//=/%=
                Value assignFrom = parseValueFromExpression(exp.assignFrom, null, scope);
                Value assignTo = parseValueFromExpression(exp.assignTo, null, scope);
                if (!exp.op.equals("=")) {
                        assignFrom = parseValueFromTwoVarOp(assignTo,
                                exp.op.substring(0, exp.op.length() - 1),
                                assignFrom, scope, LineCol.SYNTHETIC);
                }

                assert assignFrom != null;
                LocalVariable local = new LocalVariable(assignFrom.type(), false);
                scope.putLeftValue(scope.generateTempName(), local);

                Ins.TStore tStore = new Ins.TStore(local, assignFrom, scope, LineCol.SYNTHETIC, err);
                pack.instructions().add(tStore);

                Ins.TLoad tLoad = new Ins.TLoad(local, scope, LineCol.SYNTHETIC);

                if (tLoad.mode() == Ins.TLoad.Aload) {
                        parseInstructionFromAssignment(assignTo,
                                new Ins.CheckCast(tLoad, assignFrom.type(), LineCol.SYNTHETIC),
                                scope, pack.instructions(), exp.line_col());
                } else {
                        parseInstructionFromAssignment(assignTo, tLoad,
                                scope, pack.instructions(), exp.line_col());
                }


                pack.instructions().add(new Ins.TLoad(local, scope, LineCol.SYNTHETIC));

                return pack;
        }

        /**
         * {@link Undefined#get()}
         */
        private SMethodDef Undefined_get;

        /**
         * invoke {@link Undefined#get()}
         *
         * @param lineCol caller's line and column
         * @return InvokeStatic
         * @throws SyntaxException exception
         */
        private Ins.InvokeStatic invoke_Undefined_get(LineCol lineCol) throws SyntaxException {
                if (Undefined_get == null) {
                        SClassDef UndefiendClass = (SClassDef) getTypeWithName("lt.lang.Undefined", lineCol);
                        assert UndefiendClass != null;
                        for (SMethodDef m : UndefiendClass.methods()) {
                                if (m.name().equals("get")) {
                                        Undefined_get = m;
                                        break;
                                }
                        }
                }
                return new Ins.InvokeStatic(Undefined_get, lineCol);
        }

        /**
         * parse two var op<br>
         * if two values are primitive, then use jvm instruction to finish this<br>
         * otherwise, invoke bond methods
         *
         * @param tvo   two variable operation
         * @param scope current scope
         * @return the result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromTwoVarOp(TwoVariableOperation tvo, SemanticScope scope) throws SyntaxException {
                String op = tvo.operator();
                Value left = parseValueFromExpression(tvo.expressions().get(0), null, scope);
                Value right = parseValueFromExpression(tvo.expressions().get(1), null, scope);
                return parseValueFromTwoVarOp(left, op, right, scope, tvo.line_col());
        }

        /**
         * {@link Math#pow(double, double)}
         */
        private SMethodDef Math_pow;

        /**
         * @return {@link Math#pow(double, double)}
         * @throws SyntaxException exception
         */
        private SMethodDef getMath_pow() throws SyntaxException {
                if (Math_pow == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("java.lang.Math", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("pow") && m.getParameters().size() == 2
                                        && m.getParameters().get(0).type().equals(DoubleTypeDef.get())
                                        && m.getParameters().get(1).type().equals(DoubleTypeDef.get())
                                        && m.modifiers().contains(SModifier.STATIC)) {
                                        // static Math.pow(double,double)
                                        Math_pow = m;
                                        break;
                                }
                        }
                }
                return Math_pow;
        }

        /**
         * {@link lt.lang.RangeList}
         */
        private SConstructorDef RangeListCons;

        /**
         * @return {@link lt.lang.RangeList}
         * @throws SyntaxException exception
         */
        private SConstructorDef getRangeListCons() throws SyntaxException {
                if (RangeListCons == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.RangeList", LineCol.SYNTHETIC);
                        assert cls != null;
                        RangeListCons = cls.constructors().get(0);
                }
                return RangeListCons;
        }

        /**
         * invoke method with given arguments<br>
         * <code>a.method(args)</code><br>
         * first find method candidates, if not found, then invoke dynamic<br>
         * if found, then select the best match to invoke
         *
         * @param lineCol    line column info
         * @param targetType the type to invoke methods on (if the method is static, target type is this parameter. else the target type is invokeOn.getClass())
         * @param invokeOn   invokeOn (a)
         * @param methodName name of the method
         * @param args       arguments
         * @param scope      current scope
         * @return a subclass of {@link lt.compiler.semantic.Ins.Invoke}
         * @throws SyntaxException compile error
         */
        private Value invokeMethodWithArgs(LineCol lineCol, STypeDef targetType, Value invokeOn, String methodName, List<Value> args, SemanticScope scope) throws SyntaxException {
                List<SMethodDef> methods = new ArrayList<>();
                int FIND_MODE;
                if (invokeOn.equals(NullValue.get())) {
                        FIND_MODE = FIND_MODE_STATIC;
                } else {
                        FIND_MODE = FIND_MODE_NON_STATIC;
                }
                if (targetType.equals(NullTypeDef.get())) {
                        targetType = getTypeWithName("java.lang.Object", LineCol.SYNTHETIC);
                }
                findMethodFromTypeWithArguments(lineCol, methodName, args, scope.type(), targetType, FIND_MODE, methods, true);
                if (methods.isEmpty()) {
                        // invoke dynamic
                        args.add(0, new Ins.GetClass(targetType, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                        args.add(1, invokeOn);
                        args.add(2, NullValue.get()); // xx.method(...) doesn't support invoking method on functional objects
                        return new Ins.InvokeDynamic(
                                getInvokeDynamicBootstrapMethod(),
                                methodName,
                                args,
                                getTypeWithName("java.lang.Object", lineCol)
                                , Dynamic.INVOKE_STATIC, lineCol);
                } else {
                        SMethodDef method = findBestMatch(args, methods, lineCol);
                        args = castArgsForMethodInvoke(args, method.getParameters(), lineCol);
                        if (method.modifiers().contains(SModifier.STATIC)) {
                                // invoke static
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                                invokeStatic.arguments().addAll(args);

                                if (invokeStatic.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeStatic, invokeStatic.line_col()
                                        );
                                }

                                return invokeStatic;
                        } else if (method.modifiers().contains(SModifier.PRIVATE)) {
                                // invoke special
                                Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(invokeOn, method, lineCol);
                                invokeSpecial.arguments().addAll(args);

                                if (invokeSpecial.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeSpecial, invokeSpecial.line_col()
                                        );
                                }

                                return invokeSpecial;
                        } else if (method.declaringType() instanceof SInterfaceDef) {
                                // invoke interface
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(invokeOn, method, lineCol);
                                invokeInterface.arguments().addAll(args);

                                if (invokeInterface.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeInterface, invokeInterface.line_col()
                                        );
                                }

                                return invokeInterface;
                        } else {
                                // invoke virtual
                                Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(invokeOn, method, lineCol);
                                invokeVirtual.arguments().addAll(args);

                                if (invokeVirtual.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeVirtual, invokeVirtual.line_col()
                                        );
                                }

                                return invokeVirtual;
                        }
                }
        }

        /**
         * parse two variable operation for int/long/float/double
         *
         * @param left       the value on the left of the operator
         * @param baseOp     base operation, long op = baseOp+1, float op = baseOp+2, double op = baseOp + 3
         * @param methodName if requires method invocation, use this method to invoke
         * @param right      the value on the right of the operator
         * @param scope      current scope
         * @param lineCol    line column info
         * @return the result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromTwoVarOpILFD(Value left, int baseOp, String methodName, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (left.type() instanceof PrimitiveTypeDef) {
                        if (right.type() instanceof PrimitiveTypeDef) {
                                if (left.type().equals(DoubleTypeDef.get()) || right.type().equals(DoubleTypeDef.get())) {
                                        // cast to double
                                        Value a = cast(DoubleTypeDef.get(), left, lineCol);
                                        Value b = cast(DoubleTypeDef.get(), right, lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 3, DoubleTypeDef.get(), lineCol);
                                } else if (left.type().equals(FloatTypeDef.get()) || right.type().equals(FloatTypeDef.get())) {
                                        // cast to float
                                        Value a = cast(FloatTypeDef.get(), left, lineCol);
                                        Value b = cast(FloatTypeDef.get(), right, lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 2, FloatTypeDef.get(), lineCol);
                                } else if (left.type().equals(LongTypeDef.get()) || right.type().equals(LongTypeDef.get())) {
                                        // cast to long
                                        Value a = cast(LongTypeDef.get(), left, lineCol);
                                        Value b = cast(LongTypeDef.get(), right, lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 1, LongTypeDef.get(), lineCol);
                                } else {
                                        if ((baseOp == Ins.TwoVarOp.Iand || baseOp == Ins.TwoVarOp.Ior || baseOp == Ins.TwoVarOp.Ixor)
                                                && left.type().equals(BoolTypeDef.get()) && right.type().equals(BoolTypeDef.get())) {
                                                return new Ins.TwoVarOp(left, right, baseOp, BoolTypeDef.get(), lineCol);
                                        } else {
                                                // cast to int
                                                Value a = cast(IntTypeDef.get(), left, lineCol);
                                                Value b = cast(IntTypeDef.get(), right, lineCol);
                                                return new Ins.TwoVarOp(a, b, baseOp, IntTypeDef.get(), lineCol);
                                        }
                                }
                        } else {
                                // box 'left' and give result
                                return parseValueFromTwoVarOpILFD(
                                        boxPrimitive(left, lineCol),
                                        baseOp, methodName, right, scope, lineCol);
                        }
                } else {
                        List<Value> args = new ArrayList<>();
                        args.add(right);
                        return invokeMethodWithArgs(lineCol, left.type(), left, methodName, args, scope);
                }
        }

        /**
         * {@link LtRuntime#compare(int, int)}
         */
        private SMethodDef Lang_compare;

        /**
         * @return {@link LtRuntime#compare(int, int)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_compare() throws SyntaxException {
                if (Lang_compare == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("compare")
                                        && m.getParameters().size() == 2
                                        && m.getParameters().get(0).type().equals(IntTypeDef.get())
                                        && m.getParameters().get(1).type().equals(IntTypeDef.get())
                                        && m.modifiers().contains(SModifier.STATIC)) {
                                        Lang_compare = m;
                                        break;
                                }
                        }
                }
                return Lang_compare;
        }

        /**
         * {@link Comparable#compareTo(Object)}
         */
        private SMethodDef Comparable_compareTo;

        /**
         * @return {@link Comparable#compareTo(Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getComparable_compareTo() throws SyntaxException {
                if (null == Comparable_compareTo) {
                        SInterfaceDef comparable = (SInterfaceDef) getTypeWithName("java.lang.Comparable", LineCol.SYNTHETIC);
                        assert comparable != null;
                        for (SMethodDef m : comparable.methods()) {
                                if (m.name().equals("compareTo")
                                        && m.getParameters().size() == 1) {
                                        Comparable_compareTo = m;
                                        break;
                                }
                        }
                }
                return Comparable_compareTo;
        }

        /**
         * parse value from two var op compare
         *
         * @param left         the value of the left of the operator
         * @param compare_mode compare_mode {@link LtRuntime#COMPARE_MODE_EQ} {@link LtRuntime#COMPARE_MODE_GT} {@link LtRuntime#COMPARE_MODE_LT}
         * @param methodName   if requires method invocation, use this method to invoke
         * @param right        the value of the right of the operator
         * @param scope        current scope
         * @param lineCol      line column info
         * @return the result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromTwoVarOpCompare(Value left, int compare_mode, String methodName, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (left.type() instanceof PrimitiveTypeDef) {
                        if (right.type() instanceof PrimitiveTypeDef) {
                                Ins.TwoVarOp twoVarOp;
                                if (left.type().equals(DoubleTypeDef.get()) || right.type().equals(DoubleTypeDef.get())) {
                                        // cast to double
                                        Value a = cast(DoubleTypeDef.get(), left, lineCol);
                                        Value b = cast(DoubleTypeDef.get(), right, lineCol);
                                        twoVarOp = new Ins.TwoVarOp(a, b, Ins.TwoVarOp.Dcmpg, IntTypeDef.get(), lineCol);
                                } else if (left.type().equals(FloatTypeDef.get()) || right.type().equals(FloatTypeDef.get())) {
                                        // cast to float
                                        Value a = cast(FloatTypeDef.get(), left, lineCol);
                                        Value b = cast(FloatTypeDef.get(), right, lineCol);
                                        twoVarOp = new Ins.TwoVarOp(a, b, Ins.TwoVarOp.Fcmpg, IntTypeDef.get(), lineCol);
                                } else {
                                        // cast to long
                                        Value a = cast(LongTypeDef.get(), left, lineCol);
                                        Value b = cast(LongTypeDef.get(), right, lineCol);
                                        twoVarOp = new Ins.TwoVarOp(a, b, Ins.TwoVarOp.Lcmp, IntTypeDef.get(), lineCol);
                                }
                                SMethodDef compare = getLang_compare();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(compare, lineCol);
                                invokeStatic.arguments().add(twoVarOp);
                                invokeStatic.arguments().add(new IntValue(compare_mode));
                                return invokeStatic;
                        } else {
                                return parseValueFromTwoVarOpCompare(
                                        boxPrimitive(left, lineCol),
                                        compare_mode, methodName, right, scope, lineCol);
                        }
                } else {
                        STypeDef comparable = getTypeWithName("java.lang.Comparable", lineCol);
                        assert comparable != null;
                        if (comparable.isAssignableFrom(left.type())) { // Comparable
                                // left.compareTo(right)
                                SMethodDef m = getComparable_compareTo();
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(left, m, lineCol);
                                invokeInterface.arguments().add(right);

                                // LtRuntime.compare(left.compareTo(right), compare_mode)
                                SMethodDef compare = getLang_compare();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(compare, lineCol);
                                invokeStatic.arguments().add(invokeInterface);
                                invokeStatic.arguments().add(new IntValue(compare_mode));
                                return invokeStatic;
                        } else {
                                List<Value> args = new ArrayList<>();
                                args.add(right);
                                return invokeMethodWithArgs(lineCol, left.type(), left, methodName, args, scope);
                        }
                }
        }

        /**
         * {@link LtRuntime#compareRef(Object, Object)}
         */
        private SMethodDef Lang_compareRef;

        /**
         * @return {@link LtRuntime#compareRef(Object, Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_compareRef() throws SyntaxException {
                if (Lang_compareRef == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("compareRef")
                                        && m.getParameters().size() == 2
                                        && m.modifiers().contains(SModifier.STATIC)) {
                                        Lang_compareRef = m;
                                        break;
                                }
                        }
                }
                return Lang_compareRef;
        }

        /**
         * parse value from two variable operation<br>
         * invoke {@link #parseValueFromTwoVarOpILFD(Value, int, String, Value, SemanticScope, LineCol)}
         * and {@link #parseValueFromTwoVarOpCompare(Value, int, String, Value, SemanticScope, LineCol)}<br>
         * invoked by {@link #parseValueFromTwoVarOp(TwoVariableOperation, SemanticScope)}
         *
         * @param left    the value of the left of the operator
         * @param op      operator
         * @param right   the value of the right of the operator
         * @param scope   current scope
         * @param lineCol line column info
         * @return result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromTwoVarOp(Value left, String op, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                switch (op) {
                        case "..":
                                Value intLeft = cast(IntTypeDef.get(), left, lineCol);
                                Value intRight = cast(IntTypeDef.get(), right, lineCol);

                                Ins.New n = new Ins.New(getRangeListCons(), lineCol);
                                n.args().add(intLeft);
                                n.args().add(intRight);
                                n.args().add(new BoolValue(true)); // end_inclusive
                                return n;
                        case ".:":
                                intLeft = cast(IntTypeDef.get(), left, lineCol);
                                intRight = cast(IntTypeDef.get(), right, lineCol);

                                n = new Ins.New(getRangeListCons(), lineCol);
                                n.args().add(intLeft);
                                n.args().add(intRight);
                                n.args().add(new BoolValue(false)); // end_exclusive
                                return n;
                        case ":::":
                                List<Value> arg = new ArrayList<>();
                                arg.add(right);
                                return invokeMethodWithArgs(lineCol, left.type(), left, "concat", arg, scope);
                        case "^^":
                                STypeDef Number_Class = getTypeWithName("java.lang.Number", LineCol.SYNTHETIC);
                                assert Number_Class != null;
                                if (left.type() instanceof PrimitiveTypeDef ||
                                        Number_Class.isAssignableFrom(left.type())) {
                                        // primitive or Number
                                        Value doubleLeft = cast(DoubleTypeDef.get(), left, lineCol);
                                        Value doubleRight = cast(DoubleTypeDef.get(), right, lineCol);

                                        SMethodDef math_pow = getMath_pow();
                                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(math_pow, lineCol);
                                        invokeStatic.arguments().add(doubleLeft);
                                        invokeStatic.arguments().add(doubleRight);
                                        return invokeStatic;
                                } else {
                                        List<Value> args = new ArrayList<>();
                                        args.add(right);
                                        return invokeMethodWithArgs(lineCol, left.type(), left, "pow", args, scope);
                                }
                        case "*":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Imul, LtRuntime.multiply, right, scope, lineCol);
                        case "/":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Idiv, LtRuntime.divide, right, scope, lineCol);
                        case "%":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Irem, LtRuntime.remainder, right, scope, lineCol);
                        case "+":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iadd, LtRuntime.add, right, scope, lineCol);
                        case "-":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Isub, LtRuntime.subtract, right, scope, lineCol);
                        case "<<":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishl, LtRuntime.shiftLeft, right, scope, lineCol);
                        case ">>":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishr, LtRuntime.shiftRight, right, scope, lineCol);
                        case ">>>":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iushr, LtRuntime.unsignedShiftRight, right, scope, lineCol);
                        case ">":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_GT, LtRuntime.gt, right, scope, lineCol);
                        case "<":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_LT, LtRuntime.lt, right, scope, lineCol);
                        case ">=":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_GT | LtRuntime.COMPARE_MODE_EQ, LtRuntime.ge, right, scope, lineCol);
                        case "<=":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_LT | LtRuntime.COMPARE_MODE_EQ, LtRuntime.le, right, scope, lineCol);
                        case "==":
                                // null check
                                if (left.equals(NullValue.get()) || right.equals(NullValue.get())) {
                                        if (right.equals(NullValue.get())) {
                                                Value tmp = left;
                                                left = right;
                                                right = tmp;
                                        }
                                        return parseValueFromTwoVarOp(left, "is", right, scope, lineCol);
                                }
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_EQ, "equals", right, scope, lineCol);
                        case "!=":
                                // null check
                                if (left.equals(NullValue.get()) || right.equals(NullValue.get())) {
                                        if (right.equals(NullValue.get())) {
                                                Value tmp = left;
                                                left = right;
                                                right = tmp;
                                        }
                                        return parseValueFromTwoVarOp(left, "not", right, scope, lineCol);
                                }
                                Value eq = parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_EQ, "equals", right, scope, lineCol);
                                return parseValueFromTwoVarOpILFD(eq, Ins.TwoVarOp.Ixor, null, new BoolValue(true), scope, lineCol);
                        case "===":
                                if (left.type() instanceof PrimitiveTypeDef && right.type() instanceof PrimitiveTypeDef) {
                                        return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_EQ, null, right, scope, lineCol);
                                } else {
                                        if (left.type() instanceof PrimitiveTypeDef || right.type() instanceof PrimitiveTypeDef) {
                                                err.SyntaxException("reference type cannot compare to primitive type", lineCol);
                                                return null;
                                        } else {
                                                SMethodDef m = getLang_compareRef();
                                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                                                invokeStatic.arguments().add(left);
                                                invokeStatic.arguments().add(right);
                                                return invokeStatic;
                                        }
                                }
                        case "!==":
                                if (left.type() instanceof PrimitiveTypeDef && right.type() instanceof PrimitiveTypeDef) {
                                        return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_LT | LtRuntime.COMPARE_MODE_GT, null, right, scope, lineCol);
                                } else {
                                        if (left.type() instanceof PrimitiveTypeDef || right.type() instanceof PrimitiveTypeDef) {
                                                err.SyntaxException("reference type cannot compare to primitive type", lineCol);
                                                return null;
                                        } else {
                                                // LtRuntime.compareRef(left,right)
                                                SMethodDef m = getLang_compareRef();
                                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                                                invokeStatic.arguments().add(left);
                                                invokeStatic.arguments().add(right);

                                                // ! LtRuntime.compareRef(left,right)
                                                return parseValueFromTwoVarOpILFD(invokeStatic, Ins.TwoVarOp.Ixor, null, new BoolValue(true), scope, lineCol);
                                        }
                                }
                        case "=:=":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_EQ, LtRuntime.equal, right, scope, lineCol);
                        case "!:=":
                                return parseValueFromTwoVarOpCompare(left, LtRuntime.COMPARE_MODE_EQ, LtRuntime.notEqual, right, scope, lineCol);
                        case "is": {
                                // invoke static LtRuntime.is
                                SMethodDef m = getLang_is();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                                if (left.type() instanceof PrimitiveTypeDef) {
                                        left = boxPrimitive(left, lineCol);
                                }
                                if (right.type() instanceof PrimitiveTypeDef) {
                                        right = boxPrimitive(right, lineCol);
                                }
                                invokeStatic.arguments().add(left);
                                invokeStatic.arguments().add(right);
                                invokeStatic.arguments().add(
                                        new Ins.GetClass(scope.type(),
                                                (SClassDef) getTypeWithName(
                                                        "java.lang.Class",
                                                        invokeStatic.line_col()))
                                );
                                return invokeStatic;
                        }
                        case "not": {
                                // invoke static LtRuntime.not
                                SMethodDef m = getLang_not();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                                if (left.type() instanceof PrimitiveTypeDef) {
                                        left = boxPrimitive(left, lineCol);
                                }
                                if (right.type() instanceof PrimitiveTypeDef) {
                                        right = boxPrimitive(right, lineCol);
                                }
                                invokeStatic.arguments().add(left);
                                invokeStatic.arguments().add(right);
                                invokeStatic.arguments().add(
                                        new Ins.GetClass(scope.type(),
                                                (SClassDef) getTypeWithName(
                                                        "java.lang.Class",
                                                        invokeStatic.line_col()))
                                );
                                return invokeStatic;
                        }
                        case "in":
                                List<Value> args = new ArrayList<>();
                                args.add(left);
                                return invokeMethodWithArgs(lineCol, right.type(), right, "contains", args, scope);
                        case "&":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iand, LtRuntime.and, right, scope, lineCol);
                        case "^":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ixor, LtRuntime.xor, right, scope, lineCol);
                        case "|":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ior, LtRuntime.or, right, scope, lineCol);
                        case "&&":
                        case "and":
                                // logic and with short cut
                                return new Ins.LogicAnd(
                                        cast(BoolTypeDef.get(), left, lineCol),
                                        cast(BoolTypeDef.get(), right, lineCol),
                                        lineCol);
                        case "||":
                        case "or":
                                // logic or with short cut
                                return new Ins.LogicOr(
                                        cast(BoolTypeDef.get(), left, lineCol),
                                        cast(BoolTypeDef.get(), right, lineCol),
                                        lineCol
                                );
                        case ":=":
                                // assign
                                args = new ArrayList<>();
                                args.add(right);
                                return invokeMethodWithArgs(lineCol, left.type(), left, "assign", args, scope);
                        default:
                                err.SyntaxException("unknown two variable operator " + op, lineCol);
                                return null;
                }
        }

        /**
         * {@link LtRuntime#is(Object, Object, Class)}
         */
        private SMethodDef Lang_is;

        /**
         * @return {@link LtRuntime#is(Object, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_is() throws SyntaxException {
                if (Lang_is == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("is")
                                        && m.getParameters().size() == 3
                                        && m.modifiers().contains(SModifier.STATIC)) {
                                        Lang_is = m;
                                        break;
                                }
                        }
                }
                return Lang_is;
        }

        /**
         * {@link LtRuntime#not(Object, Object, Class)}
         */
        private SMethodDef Lang_not;

        /**
         * @return {@link LtRuntime#not(Object, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_not() throws SyntaxException {
                if (Lang_not == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert cls != null;
                        for (SMethodDef m : cls.methods()) {
                                if (m.name().equals("not")
                                        && m.getParameters().size() == 3
                                        && m.modifiers().contains(SModifier.STATIC)) {
                                        Lang_not = m;
                                        break;
                                }
                        }
                }
                return Lang_not;
        }

        /**
         * parse one variable operation<br>
         * can only be <code>
         * ++, unary++, --
         * </code><br>
         * simply change the expression to assignment and invoke {@link #parseValueFromAssignment(AST.Assignment, SemanticScope)}
         *
         * @param exp   one variable operation
         * @param scope current scope
         * @return result
         * @throws SyntaxException compile error
         */
        private Value parseSelfOneVarOp(Operation exp, SemanticScope scope) throws SyntaxException {
                Expression e = exp.expressions().get(0);
                if (!(e instanceof AST.Access)) {
                        err.SyntaxException(exp.operator() + " cannot operate on " + e, exp.line_col());
                        return null;
                }

                if (exp.operator().equals("++")) {
                        AST.Assignment assignment = new AST.Assignment(
                                (AST.Access) e,
                                "+=",
                                new NumberLiteral("1", exp.line_col()),
                                exp.line_col());
                        ValuePack valuePack = parseValueFromAssignment(assignment, scope);
                        if (!exp.isUnary()) {
                                /*
                                 * get value
                                 * invoke +=
                                 * pop result
                                 * // the stack top value would be original value
                                 */
                                ValuePack thePackToReturn = new ValuePack(false);

                                Value v = parseValueFromAccess((AST.Access) e, scope);
                                assert v instanceof Instruction;
                                thePackToReturn.instructions().add((Instruction) v);
                                thePackToReturn.instructions().add(valuePack);
                                thePackToReturn.instructions().add(new Ins.Pop());

                                STypeDef type = valuePack.type();
                                thePackToReturn.setType(type);

                                return thePackToReturn;
                        } else {
                                return valuePack;
                        }
                } else if (exp.operator().equals("--")) {
                        AST.Assignment assignment = new AST.Assignment(
                                (AST.Access) e,
                                "-=",
                                new NumberLiteral("1", exp.line_col()),
                                exp.line_col());
                        ValuePack valuePack = parseValueFromAssignment(assignment, scope);
                        if (!exp.isUnary()) {
                                /*
                                 * get value
                                 * invoke -=
                                 * pop result
                                 * // the stack top value would be original value
                                 */
                                ValuePack thePackToReturn = new ValuePack(false);

                                Value v = parseValueFromAccess((AST.Access) e, scope);
                                assert v instanceof Instruction;
                                thePackToReturn.instructions().add((Instruction) v);
                                thePackToReturn.instructions().add(valuePack);
                                thePackToReturn.instructions().add(new Ins.Pop());

                                STypeDef type = valuePack.type();
                                thePackToReturn.setType(type);

                                return thePackToReturn;
                        } else {
                                return valuePack;
                        }
                } else throw new LtBug("this method only supports ++ and --, but got unknown op " + exp.operator());
        }

        /**
         * parse one variable operation
         *
         * @param exp   one variable operation
         * @param scope current scope
         * @return result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromOneVarOp(Operation exp, SemanticScope scope) throws SyntaxException {
                String op = exp.operator();
                boolean unary = exp.isUnary();

                if (op.equals("++")) {

                        return parseSelfOneVarOp(exp, scope);

                } else if (op.equals("--")) {

                        return parseSelfOneVarOp(exp, scope);

                } else if (op.equals("!") && unary) {
                        // !b (bool:not ; other: invoke logicNot())
                        Value v = parseValueFromExpression(exp.expressions().get(0), null, scope);
                        assert v != null;
                        if (v.type().equals(BoolTypeDef.get()) || v.type().fullName().equals("java.lang.Boolean")) {
                                v = cast(BoolTypeDef.get(), v, exp.line_col());
                                // v is bool
                                // (v & 1) ^ 1
                                return new Ins.TwoVarOp(
                                        new Ins.TwoVarOp(
                                                v,
                                                new IntValue(1),
                                                Ins.TwoVarOp.Iand, BoolTypeDef.get(),
                                                exp.line_col()),
                                        new IntValue(1),
                                        Ins.TwoVarOp.Ixor,
                                        BoolTypeDef.get(),
                                        exp.line_col());
                        } else {
                                if (v.type() instanceof PrimitiveTypeDef) {
                                        v = boxPrimitive(v, exp.line_col());
                                }
                                // v is object
                                // invoke `logicNot`
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "logicNot", new ArrayList<>(), scope);
                        }

                } else if (op.equals("~") && unary) {
                        // ~i (int:xor(-1);long:xor(-1L);other:invoke not())
                        Value v = parseValueFromExpression(exp.expressions().get(0), null, scope);
                        assert v != null;
                        if (v.type() instanceof IntTypeDef || v.type().fullName().equals("java.lang.Integer")
                                ||
                                v.type() instanceof ByteTypeDef || v.type().fullName().equals("java.lang.Byte")
                                ||
                                v.type() instanceof ShortTypeDef || v.type().fullName().equals("java.lang.Short")
                                ) {
                                v = cast(IntTypeDef.get(), v, exp.line_col());
                                return new Ins.TwoVarOp(
                                        v,
                                        new IntValue(-1),
                                        Ins.TwoVarOp.Ixor,
                                        IntTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof LongTypeDef || v.type().fullName().equals("java.lang.Long")) {
                                v = cast(LongTypeDef.get(), v, exp.line_col());
                                return new Ins.TwoVarOp(
                                        v,
                                        new LongValue(-1),
                                        Ins.TwoVarOp.Lxor,
                                        IntTypeDef.get(),
                                        exp.line_col());
                        } else {
                                if (v.type() instanceof PrimitiveTypeDef) {
                                        v = boxPrimitive(v, exp.line_col());
                                }
                                // v is object
                                // invoke `bitwiseNot`
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "not", new ArrayList<>(), scope);
                        }
                } else if (op.equals("+") && unary) {
                        // +i (do nothing)
                        return parseValueFromExpression(exp.expressions().get(0), null, scope);
                } else if (op.equals("-") && unary) {
                        // -i (number:negative; other:invoke negate())
                        Value v = parseValueFromExpression(exp.expressions().get(0), null, scope);
                        assert v != null;
                        if (v.type() instanceof IntTypeDef || v.type().fullName().equals("java.lang.Integer")
                                ||
                                v.type() instanceof ByteTypeDef || v.type().fullName().equals("java.lang.Byte")
                                ||
                                v.type() instanceof ShortTypeDef || v.type().fullName().equals("java.lang.Short")
                                ) {
                                v = cast(IntTypeDef.get(), v, exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Ineg,
                                        IntTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof LongTypeDef || v.type().fullName().equals("java.lang.Long")) {
                                v = cast(LongTypeDef.get(), v, exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Lneg,
                                        LongTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof FloatTypeDef || v.type().fullName().equals("java.lang.Float")) {
                                v = cast(FloatTypeDef.get(), v, exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Fneg,
                                        FloatTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof DoubleTypeDef || v.type().fullName().equals("java.lang.Double")) {
                                v = cast(DoubleTypeDef.get(), v, exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Dneg,
                                        DoubleTypeDef.get(),
                                        exp.line_col());
                        } else {
                                if (v.type() instanceof PrimitiveTypeDef) {
                                        v = boxPrimitive(v, exp.line_col());
                                }
                                // v is object
                                // invoke `negate`
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "negate", new ArrayList<>(), scope);
                        }
                } else {
                        err.SyntaxException("unknown one variable operator " + (unary ? (op + "v") : ("v" + op)), exp.line_col());
                        return null;
                }
        }

        /**
         * get arguments
         *
         * @param args  args(expression)
         * @param scope scope
         * @return list
         * @throws SyntaxException exception
         */
        private List<Value> parseArguments(List<Expression> args, SemanticScope scope) throws SyntaxException {
                List<Value> list = new ArrayList<>();
                for (Expression exp : args) {
                        Value a = parseValueFromExpression(exp, null, scope);
                        if (a == null) {
                                err.SyntaxException(exp + " is not method argument", exp.line_col());
                                return null;
                        }
                        list.add(a);
                }
                return list;
        }

        /**
         * parse value from Index<br>
         * <code>arr[i]</code>
         *
         * @param index AST.Index
         * @param scope current scope
         * @return result
         * @throws SyntaxException compile error
         */
        private Value parseValueFromIndex(AST.Index index, SemanticScope scope) throws SyntaxException {
                Value v = parseValueFromExpression(index.exp, null, scope);
                assert v != null;

                List<Value> list = parseArguments(index.args, scope);

                assert list != null;
                if (v.type() instanceof SArrayTypeDef && list.size() == 1) {
                        try {
                                Value i = cast(IntTypeDef.get(), list.get(0), index.args.get(0).line_col());
                                return new Ins.TALoad(v, i, index.line_col());
                        } catch (Throwable ignore) {
                                // cast failed
                        }
                }
                // not array
                // try to find `get`
                List<SMethodDef> methods = new ArrayList<>();
                findMethodFromTypeWithArguments(index.line_col(), "get", list, scope.type(), v.type(), FIND_MODE_NON_STATIC, methods, true);
                if (methods.isEmpty()) {
                        // invoke dynamic
                        list.add(0, new Ins.GetClass(v.type(), (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                        list.add(1, v); // add invoke target into list
                        list.add(2, NullValue.get()); // xx[...] doesn't support invoking method on functional objects

                        return new Ins.InvokeDynamic(
                                getInvokeDynamicBootstrapMethod(),
                                "get",
                                list,
                                getTypeWithName("java.lang.Object", index.line_col()),
                                Dynamic.INVOKE_STATIC,
                                index.line_col());
                } else {
                        SMethodDef methodDef = findBestMatch(list, methods, index.line_col());
                        list = castArgsForMethodInvoke(list, methodDef.getParameters(), LineCol.SYNTHETIC);
                        if (methodDef.modifiers().contains(SModifier.PRIVATE)) {
                                // invoke special
                                Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(v, methodDef, index.line_col());
                                invokeSpecial.arguments().addAll(list);

                                if (invokeSpecial.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeSpecial, invokeSpecial.line_col()
                                        );
                                }

                                return invokeSpecial;
                        } else if (methodDef.declaringType() instanceof SInterfaceDef) {
                                // invoke interface
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(v, methodDef, index.line_col());
                                invokeInterface.arguments().addAll(list);

                                if (invokeInterface.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeInterface, invokeInterface.line_col()
                                        );
                                }

                                return invokeInterface;
                        } else {
                                // invoke virtual
                                Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(v, methodDef, index.line_col());
                                invokeVirtual.arguments().addAll(list);

                                if (invokeVirtual.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invokeVirtual, invokeVirtual.line_col()
                                        );
                                }

                                return invokeVirtual;
                        }
                }
        }

        /**
         * parse value from access (the access represents a type).
         *
         * @param access      the type
         * @param imports     imports
         * @param currentType caller type
         * @return New instruction
         * @throws SyntaxException exception
         */
        private Ins.New parseValueFromAccessType(AST.Access access, List<Import> imports, STypeDef currentType) throws SyntaxException {
                SClassDef type = (SClassDef) getTypeWithAccess(access, imports);
                assert type != null;
                SConstructorDef zeroParamCons = null;
                for (SConstructorDef c : type.constructors()) {
                        if (c.getParameters().isEmpty()) {
                                if (c.modifiers().contains(SModifier.PRIVATE)) {
                                        if (!type.equals(currentType)) continue;
                                } else if (c.modifiers().contains(SModifier.PROTECTED)) {
                                        if (!type.pkg().equals(currentType.pkg())
                                                && !type.isAssignableFrom(currentType))
                                                continue;
                                } else if (!c.modifiers().contains(SModifier.PUBLIC)) {
                                        if (!type.pkg().equals(currentType.pkg()))
                                                continue;
                                }
                                zeroParamCons = c;
                                break;
                        }
                }
                if (zeroParamCons == null) {
                        err.SyntaxException(type + " do not have zero parameter constructor", access.line_col());
                        return null;
                } else {
                        return new Ins.New(zeroParamCons, access.line_col());
                }
        }

        /**
         * parse value from access object<br>
         * the access object can be : (null,fieldName),(null,localVariableName),(this,fieldName),(Type,fieldName),((Type,this),fieldName),(exp,fieldName)
         *
         * @param access access object
         * @param scope  scope that contains localvariables
         * @return retrieved value can be getField/getStatic/TLoad/arraylength
         */
        private Value parseValueFromAccess(AST.Access access, SemanticScope scope) throws SyntaxException {
                List<Import> imports = fileNameToImport.get(access.line_col().fileName);
                if (access.exp == null) {
                        // Access(null,name)
                        if (access.name.equals("this")) {
                                if (scope.getThis() == null) {
                                        err.SyntaxException("static scope do not have `this` to access", access.line_col());
                                        return null;
                                }
                                return scope.getThis();
                        }
                        LeftValue v = scope.getLeftValue(access.name);
                        if (null == v) {
                                // cannot find value from local variables
                                // try to get from this or ThisClass or import static
                                // try this and ThisClass
                                SFieldDef f = findFieldFromTypeDef(access.name, scope.type(), scope.type(), FIND_MODE_ANY, true);
                                if (null != f) {
                                        if (f.modifiers().contains(SModifier.STATIC)) {
                                                return new Ins.GetStatic(f, access.line_col());
                                        } else {
                                                return new Ins.GetField(f, scope.getThis(), access.line_col());
                                        }
                                }

                                // get from import static
                                for (Import im : imports) {
                                        if (im.importAll) {
                                                if (im.pkg == null) {
                                                        // import static
                                                        f = findFieldFromTypeDef(
                                                                access.name,
                                                                getTypeWithAccess(im.access, imports),
                                                                scope.type(),
                                                                FIND_MODE_STATIC,
                                                                true);
                                                        if (null != f) {
                                                                return new Ins.GetStatic(f, access.line_col());
                                                        }
                                                }
                                        }
                                }
                        } else {
                                // value is local variable
                                return new Ins.TLoad(v, scope, access.line_col());
                        }

                        if (enableTypeAccess) {
                                // check whether it's a type and construct a new object
                                try {
                                        return parseValueFromAccessType(access, imports, scope.type());
                                } catch (Throwable ignore) {
                                }
                        }

                        // still not found
                        // check whether it's a non-static field
                        if (scope.getThis() == null) {
                                err.SyntaxException("cannot find static field " + scope.type().fullName() + "." + access.name, access.line_col());
                                return null;
                        } else {
                                // get field at runtime
                                return invokeGetField(scope.getThis(), access.name, scope.type(), access.line_col());
                        }

                } else {
                        // Access(...,fieldName)
                        if (access.exp instanceof AST.Access) {
                                AST.Access access1 = (AST.Access) access.exp;
                                if (access1.exp == null && access1.name.equals("this")) {
                                        // this.fieldName
                                        if (scope.getThis() == null) {
                                                err.SyntaxException("static methods don't have `this` variable", access1.line_col());
                                                return null;
                                        }
                                        SFieldDef f = findFieldFromTypeDef(access.name, scope.type(), scope.type(), FIND_MODE_NON_STATIC, true);
                                        if (null != f) {
                                                return new Ins.GetField(f, scope.getThis(), access.line_col());
                                        }

                                        // not found
                                        // get field at runtime
                                        return invokeGetField(scope.getThis(), access.name, scope.type(), access.line_col());
                                } else if (access1.exp instanceof AST.Access && access1.name.equals("this")) {
                                        // SuperClass.this.fieldName
                                        STypeDef type = getTypeWithAccess((AST.Access) access1.exp, imports);
                                        assert type != null;

                                        if (!type.isAssignableFrom(scope.type())) {
                                                err.SyntaxException("`SuperClass` in SuperClass.this should be super class of this class", access1.line_col());
                                                return null;
                                        }
                                        SFieldDef f = findFieldFromTypeDef(access.name, type, scope.type(), FIND_MODE_NON_STATIC, false);
                                        if (null != f) {
                                                return new Ins.GetField(f, scope.getThis(), access.line_col());
                                        } else {
                                                err.SyntaxException("cannot find static field `" + access.name + "` in " + type, access.line_col());
                                                return null;
                                        }
                                }
                        } else if (access.exp instanceof AST.PackageRef && enableTypeAccess) {
                                try {
                                        return parseValueFromAccessType(access, imports, scope.type());
                                } catch (Throwable ignore) {
                                }
                        }
                        // other conditions
                        // the access.exp should be a Type or value
                        // SomeClass.fieldName or value.fieldName

                        // try to find type
                        STypeDef type = null;
                        // not value, then try to find type
                        if (access.exp instanceof AST.Access) {
                                try {
                                        type = getTypeWithAccess((AST.Access) access.exp, imports);
                                } catch (Throwable ignore) {
                                        // type not found or wrong Access format
                                }
                        }

                        Value v = null;
                        // try to get value
                        SyntaxException ex = null; // the exception would be recorded
                        // and would be thrown if `type` can not be found
                        try {
                                if (type != null) {
                                        // the access.exp can be type, so in this step firstly try not constructing the object
                                        // if it cannot be type, the inner part of access.exp might need to construct.
                                        enableTypeAccess = false;
                                }
                                v = parseValueFromExpression(access.exp, null, scope);
                        } catch (Throwable e) {
                                if (e instanceof SyntaxException)
                                        ex = (SyntaxException) e;
                        } finally {
                                if (type != null) {
                                        enableTypeAccess = true;
                                }
                        }

                        // value found, then ignore the `type`
                        if (v != null && !isGetFieldAtRuntime(v))
                                type = null;

                        // handle
                        if (type != null) {
                                // SomeClass.fieldName -- getStatic
                                SFieldDef f = findFieldFromTypeDef(access.name, type, scope == null ? null : scope.type(), FIND_MODE_STATIC, true);
                                if (null != f) {
                                        return new Ins.GetStatic(f, access.line_col());
                                } else {
                                        try {
                                                assert scope != null;
                                                v = parseValueFromAccessType((AST.Access) access.exp, imports, scope.type());
                                        } catch (Throwable ignore) {
                                                err.SyntaxException("cannot find accessible static field `" + access.name + "` in " + type, access.line_col());
                                                return null;
                                        }
                                }
                        }
                        if (v != null) {
                                // value.fieldName -- getField
                                // (exp,fieldName)
                                if (v.type() instanceof SArrayTypeDef) {
                                        // array field can be `length`
                                        if (access.name.equals("length"))
                                                return new Ins.ArrayLength(v, access.line_col());
                                        else
                                                return invokeGetField(v, access.name, scope.type(), access.line_col());
                                } else {
                                        // check primitive
                                        if (v.type() instanceof PrimitiveTypeDef) {
                                                v = boxPrimitive(v, access.line_col());
                                        }
                                        SFieldDef f = findFieldFromTypeDef(access.name, v.type(), scope.type(), FIND_MODE_NON_STATIC, true);
                                        if (null == f) {
                                                return invokeGetField(v, access.name, scope.type(), access.line_col());
                                        } else {
                                                return new Ins.GetField(f, v, access.line_col());
                                        }
                                }
                        }
                        // type and value are not found
                        if (ex == null) {
                                err.SyntaxException("cannot parse " + access, access.line_col());
                                return null;
                        } else
                                throw ex;
                }
        }

        /**
         * invoke {@link LtRuntime#getField(Object, String, Class)}
         *
         * @param target      1st arg
         * @param name        2nd arg
         * @param callerClass 3rd arg
         * @param lineCol     line and column info
         * @return InvokeStatic
         * @throws SyntaxException exception
         */
        private Ins.InvokeStatic invokeGetField(Value target, String name, STypeDef callerClass, LineCol lineCol) throws SyntaxException {
                SMethodDef m = getLang_getField();
                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                invokeStatic.arguments().add(target);
                StringConstantValue s = new StringConstantValue(name);
                s.setType((SClassDef) getTypeWithName("java.lang.String", lineCol));
                invokeStatic.arguments().add(s);
                invokeStatic.arguments().add(new Ins.GetClass(callerClass, (SClassDef) getTypeWithName("java.lang.Class", lineCol)));
                return invokeStatic;
        }

        /**
         * {@link LtRuntime#getField(Object, String, Class)}
         */
        private SMethodDef Lang_getField = null;

        /**
         * @return {@link LtRuntime#getField(Object, String, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_getField() throws SyntaxException {
                if (Lang_getField == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;

                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("getField")) {
                                        Lang_getField = m;
                                        break;
                                }
                        }
                }
                if (Lang_getField == null)
                        throw new LtBug("lt.lang.LtRuntime.getField(Object,String,Class) should exist");
                return Lang_getField;
        }

        /**
         * get SFieldDef from the given type
         *
         * @param fieldName  field name
         * @param targetType type to search
         * @param callerType caller type(used to check accessiblility)
         * @param mode       {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param checkSuper check the super class/interfaces if it's set to true
         * @return retrieved SFieldDef or null if not found
         */
        private SFieldDef findFieldFromTypeDef(String fieldName, STypeDef targetType, STypeDef callerType, int mode, boolean checkSuper) {
                if (targetType instanceof SClassDef) {
                        return findFieldFromClassDef(fieldName, (SClassDef) targetType, callerType, mode, checkSuper);
                } else if (targetType instanceof SInterfaceDef) {
                        return findFieldFromInterfaceDef(fieldName, (SInterfaceDef) targetType, checkSuper);
                } else throw new LtBug("the type to get field from cannot be " + targetType);
        }

        /**
         * get SField from the class
         *
         * @param fieldName  field name
         * @param theClass   class to search
         * @param type       caller type
         * @param mode       {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param checkSuper check the super class/interfaces if it's set to true
         * @return retrieved SFieldDef or null if not found
         */
        private SFieldDef findFieldFromClassDef(String fieldName, SClassDef theClass, STypeDef type, int mode, boolean checkSuper) {
                for (SFieldDef f : theClass.fields()) {
                        if (mode == FIND_MODE_STATIC) {
                                if (!f.modifiers().contains(SModifier.STATIC)) continue;
                                // ignore all non static
                        } else if (mode == FIND_MODE_NON_STATIC) {
                                if (f.modifiers().contains(SModifier.STATIC)) continue;
                                // ignore all static
                        }

                        if (f.name().equals(fieldName)) {
                                if (f.modifiers().contains(SModifier.PUBLIC)) return f;
                                else {
                                        if (type != null) {
                                                if (f.modifiers().contains(SModifier.PROTECTED)) {
                                                        if (theClass.isAssignableFrom(type) // type is subclass of theClass
                                                                ||
                                                                theClass.pkg().equals(type.pkg()) // same package
                                                                ) return f;
                                                } else if (f.modifiers().contains(SModifier.PRIVATE)) {
                                                        if (theClass.equals(type)) return f;
                                                } else {
                                                        // package access
                                                        if (theClass.pkg().equals(type.pkg())) return f;
                                                }
                                        }
                                }
                        }
                }

                if (checkSuper) {
                        SFieldDef f = null;
                        if (theClass.parent() != null) {
                                f = findFieldFromClassDef(fieldName, theClass.parent(), type, mode, true);
                        }
                        if (null == f) {
                                if (mode != FIND_MODE_NON_STATIC) {
                                        for (SInterfaceDef i : theClass.superInterfaces()) {
                                                if (f != null) return f;
                                                f = findFieldFromInterfaceDef(fieldName, i, true);
                                        }
                                }
                        }
                }

                return null;
        }

        /**
         * get SField from the interface
         *
         * @param fieldName    field name
         * @param theInterface interface
         * @param checkSuper   check the super interfaces if it's set to true
         * @return retrieved SFieldDef or null if not found
         */
        private SFieldDef findFieldFromInterfaceDef(String fieldName, SInterfaceDef theInterface, boolean checkSuper) {
                for (SFieldDef f : theInterface.fields()) {
                        if (f.name().equals(fieldName)) return f;
                }

                if (checkSuper) {
                        SFieldDef f = null;
                        for (SInterfaceDef i : theInterface.superInterfaces()) {
                                if (f != null) return f;
                                f = findFieldFromInterfaceDef(fieldName, i, true);
                        }
                }

                return null;
        }

        /**
         * cast retrieved value to required type
         *
         * @param requiredType required type. null means no type limit
         * @param v            value
         * @param lineCol      file_line_col
         * @return casted value
         * @throws SyntaxException exception
         */
        private Value cast(STypeDef requiredType, Value v, LineCol lineCol) throws SyntaxException {
                // if v is instance of requiredType, return
                if (requiredType == null
                        ||
                        (!(requiredType instanceof PrimitiveTypeDef) // not primitive
                                &&                                      // and
                                requiredType.isAssignableFrom(v.type()) // v instanceof requiredType
                        )
                        ||
                        requiredType.equals(v.type()) // requiredType is v.type
                        ) return v;
                Value resultVal;
                if (requiredType instanceof PrimitiveTypeDef) {
                        // requiredType is primitive
                        if (v.type() instanceof PrimitiveTypeDef) {
                                if (v.type().equals(IntTypeDef.get())
                                        || v.type().equals(ShortTypeDef.get())
                                        || v.type().equals(ByteTypeDef.get())
                                        || v.type().equals(CharTypeDef.get())) {

                                        if (requiredType.equals(IntTypeDef.get())) {
                                                // int
                                                return new ValueAnotherType(requiredType, v, lineCol);
                                        } else if (requiredType.equals(ShortTypeDef.get())) {
                                                // to short
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_SHORT, lineCol);
                                        } else if (requiredType.equals(ByteTypeDef.get())) {
                                                // to byte
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_BYTE, lineCol);
                                        } else if (requiredType.equals(CharTypeDef.get())) {
                                                // to char
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_CHAR, lineCol);
                                        } else if (requiredType instanceof LongTypeDef) {
                                                // int to long
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_LONG, lineCol);
                                        } else if (requiredType instanceof FloatTypeDef) {
                                                // int to float
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_FLOAT, lineCol);
                                        } else if (requiredType instanceof DoubleTypeDef) {
                                                // int to double
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_DOUBLE, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else
                                                throw new LtBug("unknown primitive requiredType " + requiredType);
                                } else if (v.type().equals(LongTypeDef.get())) {
                                        if (requiredType instanceof IntTypeDef) {
                                                // long to int
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_LONG_TO_INT, lineCol);
                                        } else if (requiredType instanceof ShortTypeDef) {
                                                // long to int and int to short
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_LONG_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_SHORT, lineCol);
                                        } else if (requiredType instanceof ByteTypeDef) {
                                                // long to int and int to byte
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_LONG_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_BYTE, lineCol);
                                        } else if (requiredType instanceof CharTypeDef) {
                                                // long to int and int to char
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_LONG_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_CHAR, lineCol);
                                        } else if (requiredType instanceof FloatTypeDef) {
                                                // long to float
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_LONG_TO_FLOAT, lineCol);
                                        } else if (requiredType instanceof DoubleTypeDef) {
                                                // long to double
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_LONG_TO_DOUBLE, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else
                                                throw new LtBug("unknown primitive requiredType " + requiredType);
                                } else if (v.type().equals(FloatTypeDef.get())) {
                                        if (requiredType instanceof IntTypeDef) {
                                                // float to int
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_FLOAT_TO_INT, lineCol);
                                        } else if (requiredType instanceof ShortTypeDef) {
                                                // float to int and int to short
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_FLOAT_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_SHORT, lineCol);
                                        } else if (requiredType instanceof ByteTypeDef) {
                                                // float to int and int to byte
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_FLOAT_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_BYTE, lineCol);
                                        } else if (requiredType instanceof CharTypeDef) {
                                                // float to int and int to char
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_FLOAT_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_CHAR, lineCol);
                                        } else if (requiredType instanceof LongTypeDef) {
                                                // float to long
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_FLOAT_TO_LONG, lineCol);
                                        } else if (requiredType instanceof DoubleTypeDef) {
                                                // float to double
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_FLOAT_TO_DOUBLE, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else
                                                throw new LtBug("unknown primitive requiredType " + requiredType);
                                } else if (v.type().equals(DoubleTypeDef.get())) {
                                        if (requiredType instanceof IntTypeDef) {
                                                // double to int
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_DOUBLE_TO_INT, lineCol);
                                        } else if (requiredType instanceof ShortTypeDef) {
                                                // double to int and int to short
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_DOUBLE_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_SHORT, lineCol);
                                        } else if (requiredType instanceof ByteTypeDef) {
                                                // double to int and int to byte
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_DOUBLE_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_BYTE, lineCol);
                                        } else if (requiredType instanceof CharTypeDef) {
                                                // double to int and int to char
                                                Ins.Cast c1 = new Ins.Cast(IntTypeDef.get(), v, Ins.Cast.CAST_DOUBLE_TO_INT, lineCol);
                                                return new Ins.Cast(requiredType, c1, Ins.Cast.CAST_INT_TO_CHAR, lineCol);
                                        } else if (requiredType instanceof FloatTypeDef) {
                                                // double to float
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_DOUBLE_TO_FLOAT, lineCol);
                                        } else if (requiredType instanceof LongTypeDef) {
                                                // double to long
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_DOUBLE_TO_LONG, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else
                                                throw new LtBug("unknown primitive requiredType " + requiredType);
                                } else if (v.type().equals(BoolTypeDef.get())) {
                                        err.SyntaxException("cannot cast from boolean to other primitives", lineCol);
                                        return null;
                                } else throw new LtBug("unknown primitive value " + v);
                        } else {
                                // cast obj to primitive
                                return castObjToPrimitive((PrimitiveTypeDef) requiredType, v, lineCol);
                        }
                } else {
                        // requiredType is not primitive
                        if (v.type() instanceof PrimitiveTypeDef) {
                                // v is primitive
                                v = boxPrimitive(v, lineCol);
                                // box then check cast
                                if (requiredType.isAssignableFrom(v.type())) return v;
                                // invoke cast(Object)
                                resultVal = castObjToObj(requiredType, v, lineCol);
                        } else {
                                // cast object to object
                                resultVal = castObjToObj(requiredType, v, lineCol);
                        }
                }
                return new Ins.CheckCast(resultVal, requiredType, lineCol);
        }

        /**
         * invoke {@link LtRuntime#cast(Object, Class)}<br>
         * note that the result object is always `java.lang.Object` when compiling,<br>
         * use {@link lt.compiler.semantic.Ins.CheckCast} to cast to required type to avoid some error when runtime validates the class file
         *
         * @param type    2nd arg
         * @param v       1st arg
         * @param lineCol line column info
         * @return casted value
         * @throws SyntaxException exception
         */
        private Value castObjToObj(STypeDef type, Value v, LineCol lineCol) throws SyntaxException {
                SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", lineCol);
                assert Lang != null;

                SMethodDef method = null;
                for (SMethodDef m : Lang.methods()) {
                        if (m.name().equals("cast")) {
                                method = m;
                                break;
                        }
                }
                if (method == null) throw new LtBug("lt.lang.LtRuntime.castToInt(Object,Class) should exist");
                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                invokeStatic.arguments().add(v);
                invokeStatic.arguments().add(
                        new Ins.GetClass(
                                type,
                                (SClassDef) getTypeWithName("java.lang.Class", lineCol))
                );
                return invokeStatic;
        }

        /**
         * invoke castToX methods defined in lt.lang.LtRuntime
         *
         * @param type    the primitive type
         * @param v       value to cast
         * @param lineCol line and column info
         * @return casted value
         * @throws SyntaxException exception
         */
        private Value castObjToPrimitive(PrimitiveTypeDef type, Value v, LineCol lineCol) throws SyntaxException {
                SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.LtRuntime", LineCol.SYNTHETIC);
                assert Lang != null;

                SMethodDef method = null;
                if (type instanceof IntTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToInt")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToInt(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof LongTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToLong")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToLong(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof ShortTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToShort")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToShort(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof ByteTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToByte")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToByte(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof FloatTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToFloat")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToFloat(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof DoubleTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToDouble")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToDouble(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof BoolTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToBool")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToBool(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof CharTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToChar")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.LtRuntime.castToChar(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else throw new LtBug("unknown primitive type " + type);
        }

        /**
         * check whether the method is overridden by list methods
         *
         * @param method     method
         * @param methodList method list
         * @return true/false
         */
        private boolean whetherTheMethodIsOverriddenByMethodsInTheList(SMethodDef method, List<SMethodDef> methodList) throws SyntaxException {
                // methodList is null
                // return false
                if (methodList == null) return false;
                if (method.modifiers().contains(SModifier.STATIC)) return false;
                /*
                // foreach m in methodList
                for (SMethodDef m : methodList) {
                        // equals means overridden
                        if (method.equals(m)) return true;
                        // check m.overRide methods
                        if (whetherTheMethodIsOverriddenByMethodsInTheList(method, m.overRide())) return true;
                }
                */
                // check signature
                if (null != findMethodWithSameSignature(method, methodList, true)) return true;
                // still not found, return false
                return false;
        }

        /**
         * search for static and non-static
         */
        private static final int FIND_MODE_ANY = 0;
        /**
         * only search for static
         */
        private static final int FIND_MODE_STATIC = 1;
        /**
         * only search for non-static
         */
        private static final int FIND_MODE_NON_STATIC = 2;

        /**
         * find method from interface and it's super interfaces
         *
         * @param name           method name
         * @param argList        argument list
         * @param sInterfaceDef  interface definition(where to find method from)
         * @param mode           find_method_mode {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param matchedMethods matched methods
         * @param checkSuper     whether to check super types
         */
        private void findMethodFromInterfaceWithArguments(String name, List<Value> argList, SInterfaceDef sInterfaceDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
                out:
                for (SMethodDef m : sInterfaceDef.methods()) {
                        // check name
                        if (!m.name().equals(name)) continue;

                        if (mode == FIND_MODE_STATIC) {
                                if (!m.modifiers().contains(SModifier.STATIC)) continue;
                        } else if (mode == FIND_MODE_NON_STATIC) {
                                if (m.modifiers().contains(SModifier.STATIC)) continue;
                        }

                        List<SParameter> parameters = m.getParameters();
                        if (parameters.size() == argList.size()) {
                                // parameter size match
                                for (int i = 0; i < parameters.size(); ++i) {
                                        SParameter param = parameters.get(i);
                                        Value v = argList.get(i);

                                        if (!param.type().isAssignableFrom(v.type())) {
                                                if (!(param.type() instanceof PrimitiveTypeDef)
                                                        && v.type() instanceof PrimitiveTypeDef) {
                                                        v = boxPrimitive(v, LineCol.SYNTHETIC);
                                                        if (!param.type().isAssignableFrom(v.type()))
                                                                continue out;
                                                } else continue out;
                                        }
                                }

                                if (!whetherTheMethodIsOverriddenByMethodsInTheList(m, matchedMethods))
                                        matchedMethods.add(m);
                        }
                }

                if (checkSuper) {
                        // recursively get super interfaces' method
                        for (SInterfaceDef i : sInterfaceDef.superInterfaces()) {
                                findMethodFromInterfaceWithArguments(name, argList, i, mode, matchedMethods, true);
                        }
                }
        }

        /**
         * find method from class and it's super class and super interfaces
         *
         * @param name           method name
         * @param argList        argument list or null
         * @param invokeOn       the invoker's class
         * @param sClassDef      class definition(where to find method from)
         * @param mode           find_method_mode {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param matchedMethods matched methods, the final result would be chosen from the list
         * @param checkSuper     whether to check super types
         */
        private void findMethodFromClassWithArguments(String name, List<Value> argList, STypeDef invokeOn, SClassDef sClassDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
                // prevent invalid class access
                if (!sClassDef.modifiers().contains(SModifier.PUBLIC)) {
                        if (sClassDef.modifiers().contains(SModifier.PROTECTED)) {
                                if (!sClassDef.isAssignableFrom(invokeOn) && !sClassDef.pkg().equals(invokeOn.pkg()))
                                        return;
                        } else if (sClassDef.modifiers().contains(SModifier.PRIVATE)) return;
                        else {
                                // package
                                if (!sClassDef.pkg().equals(invokeOn.pkg())) return;
                        }
                }
                out:
                for (SMethodDef m : sClassDef.methods()) {
                        // check name
                        if (!m.name().equals(name)) continue;

                        if (mode == FIND_MODE_STATIC) {
                                if (!m.modifiers().contains(SModifier.STATIC)) continue;
                        } else if (mode == FIND_MODE_NON_STATIC) {
                                if (m.modifiers().contains(SModifier.STATIC)) continue;
                        }

                        List<SParameter> parameters = m.getParameters();
                        if (parameters.size() == argList.size()) {
                                // parameter size match
                                for (int i = 0; i < parameters.size(); ++i) {
                                        SParameter param = parameters.get(i);
                                        Value v = argList.get(i);

                                        if (!param.type().isAssignableFrom(v.type())) {
                                                if (!(param.type() instanceof PrimitiveTypeDef)
                                                        && v.type() instanceof PrimitiveTypeDef) {
                                                        v = boxPrimitive(v, LineCol.SYNTHETIC);
                                                        if (!param.type().isAssignableFrom(v.type()))
                                                                continue out;
                                                } else continue out;
                                        }
                                }

                                // check accessible
                                if (m.modifiers().contains(SModifier.PRIVATE)) {
                                        // private then ignore
                                        if (!invokeOn.equals(sClassDef))
                                                continue;
                                } else if (m.modifiers().contains(SModifier.PROTECTED)) {
                                        // protected
                                        // subclass or same package
                                        if (!sClassDef.isAssignableFrom(invokeOn) && !sClassDef.pkg().equals(invokeOn.pkg()))
                                                continue;
                                } else if (!m.modifiers().contains(SModifier.PUBLIC)) {
                                        // package access, check package
                                        if (!sClassDef.pkg().equals(invokeOn.pkg())) continue;
                                }

                                // check overridden
                                if (!whetherTheMethodIsOverriddenByMethodsInTheList(m, matchedMethods)) {
                                        // check return type accessible
                                        STypeDef type = m.getReturnType();
                                        boolean canAccess = false;

                                        if (type instanceof SClassDef) {
                                                if (((SClassDef) type).modifiers().contains(SModifier.PUBLIC)) {
                                                        canAccess = true;
                                                } else if (((SClassDef) type).modifiers().contains(SModifier.PROTECTED)) {
                                                        if (type.isAssignableFrom(invokeOn) || type.pkg().equals(invokeOn.pkg())) {
                                                                canAccess = true;
                                                        }
                                                } else if (!((SClassDef) type).modifiers().contains(SModifier.PUBLIC)) {
                                                        if (type.pkg().equals(invokeOn.pkg())) {
                                                                canAccess = true;
                                                        }
                                                }
                                        } else {
                                                canAccess = true;
                                        }

                                        if (canAccess) {
                                                matchedMethods.add(m);
                                        }
                                }
                        }
                }

                if (checkSuper) {
                        if (sClassDef.parent() != null) {
                                findMethodFromClassWithArguments(name, argList, invokeOn, sClassDef.parent(), mode, matchedMethods, true);
                        }
                        for (SInterfaceDef i : sClassDef.superInterfaces()) {
                                findMethodFromInterfaceWithArguments(name, argList, i, mode, matchedMethods, true);
                        }
                }
        }

        /**
         * find method from type with arguments<br>
         * it redirects to {@link #findMethodFromClassWithArguments(String, List, STypeDef, SClassDef, int, List, boolean)}<br>
         * or {@link #findMethodFromInterfaceWithArguments(String, List, SInterfaceDef, int, List, boolean)}<br>
         * or get methods from annotation
         *
         * @param lineCol        file_line_col
         * @param name           method name
         * @param argList        argument list
         * @param invokeOn       invoker's type
         * @param sTypeDef       type definition
         * @param mode           find_method_mode {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param matchedMethods matched methods
         * @param checkSuper     whether to check super types
         * @throws SyntaxException exception
         */
        private void findMethodFromTypeWithArguments(LineCol lineCol, String name, List<Value> argList, STypeDef invokeOn, STypeDef sTypeDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
                if (sTypeDef instanceof SClassDef) {
                        findMethodFromClassWithArguments(name, argList, invokeOn, (SClassDef) sTypeDef, mode, matchedMethods, checkSuper);
                } else if (sTypeDef instanceof SInterfaceDef) {
                        findMethodFromInterfaceWithArguments(name, argList, (SInterfaceDef) sTypeDef, mode, matchedMethods, checkSuper);
                } else if (sTypeDef instanceof SAnnoDef) {
                        if (argList.size() != 0) {
                                err.SyntaxException("invoking methods in annotation should contain no arguments", lineCol);
                                return;
                        }
                        matchedMethods.addAll(
                                ((SAnnoDef) sTypeDef).annoFields().stream().
                                        filter(f -> f.name().equals(name)).
                                        collect(Collectors.toList())
                        );
                } else throw new LtBug("sTypeDef can only be SClassDef or SInterfaceDef or SAnnoDef");
        }

        /**
         * constructing new instances
         *
         * @param classDef class definition
         * @param argList  argument list
         * @param lineCol  line col
         * @return {@link lt.compiler.semantic.Ins.New} or {@link lt.compiler.semantic.Ins.InvokeDynamic}
         * @throws SyntaxException exception
         */
        private Value constructingNewInst(SClassDef classDef, List<Value> argList, LineCol lineCol) throws SyntaxException {
                out:
                for (SConstructorDef con : classDef.constructors()) {
                        // foreach constructor, check its parameters
                        List<SParameter> params = con.getParameters();
                        if (argList.size() == params.size()) {
                                // length match
                                for (int i = 0; i < argList.size(); ++i) {
                                        Value v = argList.get(i);
                                        SParameter param = params.get(i);

                                        if (!param.type().isAssignableFrom(v.type())) {
                                                // not assignable
                                                // see whether the v.type is primitive
                                                if (!(param.type() instanceof PrimitiveTypeDef)
                                                        &&
                                                        v.type() instanceof PrimitiveTypeDef) {
                                                        v = boxPrimitive(v, LineCol.SYNTHETIC);
                                                        if (!param.type().isAssignableFrom(v.type())) {
                                                                continue out;
                                                        }
                                                } else continue out;
                                        }
                                }
                                // all matches
                                Ins.New aNew = new Ins.New(con, lineCol);
                                argList = castArgsForMethodInvoke(argList, con.getParameters(), lineCol);
                                aNew.args().addAll(argList);
                                return aNew;
                        }
                }
                // not found

                // add current class into arg list
                return new Ins.InvokeDynamic(getConstructBootstrapMethod(),
                        "_init_", argList, classDef, Dynamic.INVOKE_STATIC, lineCol);
        }

        /**
         * call the functional object just like calling a method.
         *
         * @param invocation invocation object. the invocation.exp is any expression but AST.Access.
         * @param scope      scope that contains local variables and local methods.
         * @return The invocation result. Usually the result is InvokeDynamic.
         * @throws SyntaxException compiling error.
         * @see #parseValueFromInvocation(AST.Invocation, SemanticScope)
         * @see #parseValueFromInvocationWithNames(AST.Invocation, SemanticScope)
         */
        private Value parseValueFromInvocationFunctionalObject(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                Expression exp = invocation.exp;
                Value possibleFunctionalObject = parseValueFromExpression(exp, null, scope);

                List<Value> arguments = new ArrayList<>();
                arguments.add(possibleFunctionalObject);
                for (Expression e : invocation.args) {
                        arguments.add(parseValueFromExpression(e, null, scope));
                }

                return new Ins.InvokeDynamic(
                        getCallFunctionalObjectBootstrapMethod(),
                        "_call_functional_object_", arguments,
                        getObject_Class(), Dynamic.INVOKE_STATIC, invocation.line_col()
                );
        }

        /**
         * get value from Invocation<br>
         * the Invocation may<br>
         * <ol>
         * <li>invoke methods on `this` -- Access(`this`,methodName) / Access(null,methodName)</li>
         * <li>invoke methods from ThisClass (static) -- Access(ThisClass,methodName) / Access(null,methodName)</li>
         * <li>invoke methods from import static -- Access(null,methodName)</li>
         * <li>invoke methods on values -- Access(value,methodName)</li>
         * <li>invoke methods from SomeClass -- Access(SomeClass,methodName)</li>
         * <li>invoke methods on `this` but invoke methods in super class/interface (invoke special) -- Access(Access(SuperClass,`this`),methodName)</li>
         * <li>construct an object -- {@link #getTypeWithAccess(AST.Access, List)}</li>
         * </ol>
         *
         * @param invocation invocation object. invocation.exp should be AST.Access when calling this method.
         *                   and invocation.invokeWithNames should be false.
         * @param scope      scope that contains local variables and local methods
         * @return Invoke object or New object(represents invokeXXX or new instruction)
         * @throws SyntaxException exceptions
         * @see #parseValueFromInvocationWithNames(AST.Invocation, SemanticScope)
         * @see #parseValueFromInvocationFunctionalObject(AST.Invocation, SemanticScope)
         */
        private Value parseValueFromInvocation(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                // parse args
                List<Value> argList = new ArrayList<>();
                boolean tmpEnableTypeAccess = enableTypeAccess;
                for (Expression arg : invocation.args) {
                        enableTypeAccess = true;
                        Value v;
                        try {
                                v = parseValueFromExpression(arg, null, scope);
                        } finally {
                                enableTypeAccess = tmpEnableTypeAccess;
                        }
                        if (v == null) {
                                err.SyntaxException(arg + " is not method argument", arg.line_col());
                                return null;
                        }
                        argList.add(v);
                }

                List<SMethodDef> methodsToInvoke = new ArrayList<>();
                SemanticScope.MethodRecorder innerMethod = null; // inner method ?
                Value target = null;
                // get method and target
                // get import
                List<Import> imports = fileNameToImport.get(invocation.line_col().fileName);
                AST.Access access = (AST.Access) invocation.exp;
                if (access.exp == null) {
                        // access structure should be
                        // Access(null, methodName)
                        // the method could be this.methodName(...)
                        // or ThisClass.methodName(...) (invokeStatic)
                        // or invoke from `import static`
                        // or inner method

                        // access.name should be method name
                        // try inner method
                        SemanticScope.MethodRecorder r = scope.getInnerMethod(access.name);
                        if (null != r) {
                                SMethodDef m = r.method;
                                if (r.paramCount == argList.size()) {
                                        int inc = m.getParameters().size() - r.paramCount;
                                        boolean canUse = true;
                                        for (int i = 0; i < r.paramCount; ++i) {
                                                STypeDef pType = m.getParameters().get(i + inc).type();
                                                STypeDef aType = argList.get(i).type();
                                                if (!pType.isAssignableFrom(aType)) {
                                                        if (aType instanceof PrimitiveTypeDef) {
                                                                if (pType.isAssignableFrom(boxPrimitive(
                                                                        argList.get(i),
                                                                        LineCol.SYNTHETIC
                                                                ).type())) {
                                                                        continue;
                                                                }
                                                        }
                                                        canUse = false;
                                                        break;
                                                }
                                        }
                                        if (canUse) {
                                                innerMethod = r;
                                        }
                                }
                        }

                        // this.methodName or ThisClass.methodName
                        findMethodFromTypeWithArguments(
                                access.line_col(),
                                access.name,
                                argList,
                                scope.getThis() == null
                                        ? scope.type()
                                        : scope.getThis().type(),
                                scope.type(),
                                FIND_MODE_ANY,
                                methodsToInvoke,
                                true);

                        if (methodsToInvoke.isEmpty()) {
                                // try to invoke from `import static`
                                for (Import im : imports) {
                                        if (!methodsToInvoke.isEmpty()) break;
                                        if (im.importAll && im.pkg == null) {
                                                // this import type is import static

                                                STypeDef type = getTypeWithAccess(im.access, imports);
                                                findMethodFromTypeWithArguments(
                                                        access.line_col(),
                                                        access.name,
                                                        argList,
                                                        null,
                                                        type,
                                                        FIND_MODE_STATIC,
                                                        methodsToInvoke,
                                                        true);
                                        }
                                }
                        }
                }

                boolean doInvokeSpecial = false;

                if (methodsToInvoke.isEmpty()) {
                        // not found or access.exp is not null
                        if (access.exp != null) {
                                // the access structure should be
                                // Access(Access(type,`this`),methodName)
                                // or
                                // Access(type,methodName)
                                // or
                                // Access(value,methodName)
                                if (access.exp instanceof AST.Access && ((AST.Access) access.exp).name.equals("this")) {
                                        // Access(Access(type or null,`this`),methodName)

                                        // access1 represents access.exp
                                        // access1.exp == access.exp.exp
                                        AST.Access access1 = (AST.Access) access.exp;
                                        if (access1.exp == null) {
                                                // represents `this`
                                                findMethodFromTypeWithArguments(
                                                        access.line_col(),
                                                        access.name, argList,
                                                        scope.getThis() == null
                                                                ? scope.type()
                                                                : scope.getThis().type(),
                                                        scope.type(),
                                                        FIND_MODE_NON_STATIC,
                                                        methodsToInvoke,
                                                        true);
                                        } else if (access1.exp instanceof AST.Access) {
                                                // represents invoke methods in super classes/interfaces

                                                doInvokeSpecial = true;

                                                STypeDef type = getTypeWithAccess((AST.Access) access1.exp, imports);
                                                assert type != null;
                                                // type should be assignable from scope.type()
                                                if (!type.isAssignableFrom(scope.type())) {
                                                        err.SyntaxException("invokespecial type should be assignable from current class", access1.line_col());
                                                        return null;
                                                }
                                                findMethodFromTypeWithArguments(
                                                        access.line_col(),
                                                        access.name,
                                                        argList,
                                                        scope.getThis() == null
                                                                ? scope.type()
                                                                : scope.getThis().type(),
                                                        type,
                                                        FIND_MODE_NON_STATIC,
                                                        methodsToInvoke,
                                                        false); // only find method in given interface
                                        } else {
                                                err.SyntaxException("`Type` in Type.this.methodName should be Class/Interface name", access1.exp.line_col());
                                                return null;
                                        }
                                } else if (!(access.exp instanceof AST.PackageRef)) {
                                        // check whether access.exp is type
                                        STypeDef type = null;
                                        if (access.exp instanceof AST.Access) {
                                                try {
                                                        type = getTypeWithAccess((AST.Access) access.exp, imports);
                                                } catch (Throwable ignore) {
                                                }
                                                if (type != null) {
                                                        findMethodFromTypeWithArguments(
                                                                access.line_col(),
                                                                access.name,
                                                                argList,
                                                                scope.type(),
                                                                type,
                                                                FIND_MODE_STATIC,
                                                                methodsToInvoke,
                                                                true);
                                                }
                                        }
                                        // then:
                                        // assume access.exp is value
                                        // Access(value, methodName)
                                        boolean isValue = true;
                                        Throwable throwableWhenTryValue = null;
                                        try {
                                                if (type != null) {
                                                        enableTypeAccess = false;
                                                }
                                                target = parseValueFromExpression(access.exp, null, scope);
                                        } catch (Throwable e) {
                                                // parse from value failed
                                                isValue = false;
                                                throwableWhenTryValue = e;
                                        } finally {
                                                if (type != null) {
                                                        enableTypeAccess = true;
                                                }
                                        }

                                        if (target == null) {
                                                if (type != null && methodsToInvoke.isEmpty()) {
                                                        try {
                                                                // try to instantiate the class
                                                                target = parseValueFromAccessType(
                                                                        (AST.Access) access.exp, imports, scope.type());
                                                        } catch (Throwable t) {
                                                                // failed then it's not a value
                                                                isValue = false;
                                                        }
                                                } else {
                                                        isValue = false;
                                                }
                                        } else if (isGetFieldAtRuntime(target)) {
                                                if (type != null) isValue = false;
                                        }

                                        if (isValue) {
                                                assert target != null;

                                                methodsToInvoke.clear();
                                                if (target.type() instanceof SClassDef || target.type() instanceof SInterfaceDef) {
                                                        findMethodFromTypeWithArguments(
                                                                access.line_col(),
                                                                access.name,
                                                                argList,
                                                                scope.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true);
                                                } else if (target.type() instanceof SAnnoDef) {
                                                        if (argList.size() != 0) {
                                                                err.SyntaxException("Annotation don't have methods with non zero parameters", access.exp.line_col());
                                                                return null;
                                                        }
                                                        findMethodFromTypeWithArguments(
                                                                access.exp.line_col(),
                                                                access.name,
                                                                argList,
                                                                scope.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true
                                                        ); // this method will find method from annotation
                                                        if (methodsToInvoke.isEmpty()) {
                                                                err.SyntaxException("cannot find " + access.name + " in " + target.type(), access.exp.line_col());
                                                                return null;
                                                        }
                                                } else if (target.type() instanceof PrimitiveTypeDef) {
                                                        // box primitive then invoke
                                                        target = boxPrimitive(target, access.exp.line_col());
                                                        findMethodFromTypeWithArguments(
                                                                access.line_col(),
                                                                access.name,
                                                                argList,
                                                                scope.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true);
                                                } else {
                                                        throw new LtBug("type should not be " + target.type());
                                                }
                                        } else if (access.exp instanceof AST.Access && type != null) {
                                                // access.exp is type
                                                if (methodsToInvoke.isEmpty()) {
                                                        List<Value> args = new ArrayList<>();
                                                        args.add(new Ins.GetClass(type, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                                                        args.add(NullValue.get());

                                                        // try to get static field
                                                        SFieldDef field = findFieldFromTypeDef(access.name, type, scope.type(), FIND_MODE_STATIC, true);
                                                        if (field == null) {
                                                                args.add(NullValue.get()); // XXX.method(...) doesn't support invoking method on functional objects
                                                        } else {
                                                                args.add(new Ins.GetStatic(field, invocation.line_col()));
                                                        }
                                                        args.addAll(argList);

                                                        // invoke dynamic
                                                        return new Ins.InvokeDynamic(
                                                                getInvokeDynamicBootstrapMethod(),
                                                                access.name,
                                                                args,
                                                                getTypeWithName("java.lang.Object", LineCol.SYNTHETIC),
                                                                Dynamic.INVOKE_STATIC,
                                                                invocation.line_col()
                                                        );
                                                }
                                        } else {
                                                if (throwableWhenTryValue == null) {
                                                        err.SyntaxException(
                                                                "method access structure should only be " +
                                                                        "(type,methodName)" +
                                                                        "/((type or null,\"this\"),methodName)" +
                                                                        "/(null,methodName)/(value,methodName) " +
                                                                        "but got " + invocation.exp, access.exp.line_col());
                                                        return null;
                                                } else {
                                                        if (throwableWhenTryValue instanceof SyntaxException) {
                                                                err.SyntaxException(((SyntaxException) throwableWhenTryValue).msg, ((SyntaxException) throwableWhenTryValue).lineCol);
                                                        } else {
                                                                throw new LtBug(throwableWhenTryValue);
                                                        }
                                                        return null;
                                                }
                                        }
                                }
                        }
                }

                // no method found
                if (methodsToInvoke.isEmpty() && innerMethod == null) {
                        // try to find constructor
                        STypeDef type = null;
                        try {
                                type = getTypeWithAccess(access, imports);
                        } catch (Throwable ignore) {
                                // not found or not type format
                        }
                        if (type instanceof SClassDef) {
                                // only SClassDef have constructors
                                return constructingNewInst((SClassDef) type, argList, invocation.line_col());
                        }
                }

                if (access.exp instanceof AST.PackageRef) {
                        // should be constructor
                        // but not found
                        err.SyntaxException("cannot find constructor " + invocation, invocation.line_col());
                        return null;
                }

                if (target == null) target = scope.getThis();

                if (methodsToInvoke.isEmpty() && innerMethod == null) {
                        // invoke dynamic
                        argList.add(0, new Ins.GetClass(
                                target == null ? scope.type() : target.type(),
                                (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                        if (target == null) {
                                if (scope.getThis() == null)
                                        argList.add(1, NullValue.get());
                                else
                                        argList.add(1, scope.getThis());
                        } else {
                                argList.add(1, target);
                        }

                        if (access.exp == null) {
                                // invoking method on functional objects
                                argList.add(2, findObjectInCurrentScopeWithName(access.name, scope));
                        } else {
                                if (target == null) {
                                        throw new LtBug("code should not reach here");
                                } else {
                                        // xx.method(...)
                                        SFieldDef field = findFieldFromTypeDef(access.name, target.type(), scope.type(), FIND_MODE_ANY, true);
                                        if (field == null) {
                                                argList.add(2, NullValue.get());
                                        } else {
                                                if (field.modifiers().contains(SModifier.STATIC)) {
                                                        argList.add(2, new Ins.GetStatic(field, invocation.line_col()));
                                                } else {
                                                        argList.add(2, new Ins.GetField(field, target, invocation.line_col()));
                                                }
                                        }
                                }
                        }

                        return new Ins.InvokeDynamic(
                                getInvokeDynamicBootstrapMethod(),
                                access.name,
                                argList,
                                getTypeWithName("java.lang.Object", invocation.line_col()),
                                Dynamic.INVOKE_STATIC,
                                invocation.line_col());
                } else {
                        SMethodDef methodToInvoke;
                        boolean invokeInnerMethod = false;
                        if (methodsToInvoke.isEmpty()) {
                                assert innerMethod != null;
                                methodToInvoke = innerMethod.method;
                                invokeInnerMethod = true;
                        } else if (innerMethod == null) {
                                methodToInvoke = findBestMatch(argList, methodsToInvoke, invocation.line_col());
                        } else {
                                methodToInvoke = findBestMatch(argList, methodsToInvoke, invocation.line_col());
                                // choose from inner method and normal method
                                int inc = innerMethod.method.getParameters().size() - innerMethod.paramCount;
                                Boolean normalIsBetter = null;
                                for (int i = 0; i < methodToInvoke.getParameters().size(); ++i) {
                                        STypeDef innerT = innerMethod.method.getParameters().get(i + inc).type();
                                        STypeDef normalT = methodToInvoke.getParameters().get(i).type();
                                        if (normalT.isAssignableFrom(innerT)) {
                                                if (normalIsBetter == null) {
                                                        normalIsBetter = false;
                                                } else if (normalIsBetter) {
                                                        err.SyntaxException("cannot choose between " + methodToInvoke + " and " + innerMethod.method + " with args " + argList, invocation.line_col());
                                                        return null;
                                                }
                                        } else if (innerT.isAssignableFrom(normalT)) {
                                                if (normalIsBetter == null) {
                                                        normalIsBetter = true;
                                                } else if (!normalIsBetter) {
                                                        err.SyntaxException("cannot choose between " + methodToInvoke + " and " + innerMethod.method + " with args " + argList, invocation.line_col());
                                                        return null;
                                                }
                                        }
                                }
                                if (normalIsBetter == null || !normalIsBetter) {
                                        invokeInnerMethod = true;
                                        methodToInvoke = innerMethod.method;
                                }
                        }

                        if (invokeInnerMethod) {
                                List<Value> values = new ArrayList<>();
                                int inc = innerMethod.method.getParameters().size() - innerMethod.paramCount;
                                for (int i = 0; i < argList.size(); ++i) {
                                        STypeDef requiredType = innerMethod.method.getParameters().get(i + inc).type();
                                        values.add(cast(
                                                requiredType,
                                                argList.get(i),
                                                invocation.line_col()
                                        ));
                                }

                                Ins.Invoke invoke;
                                if (innerMethod.method.modifiers().contains(SModifier.STATIC)) {
                                        invoke = new Ins.InvokeStatic(innerMethod.method, invocation.line_col());
                                } else {
                                        invoke = new Ins.InvokeSpecial(scope.getThis(), innerMethod.method, invocation.line_col());
                                }
                                int requiredLocalVariableCount = innerMethod.method.getParameters().size() - innerMethod.paramCount;
                                List<LeftValue> leftValues = scope.getLeftValues(requiredLocalVariableCount);
                                if (leftValues.size() != requiredLocalVariableCount)
                                        throw new LtBug("require " + requiredLocalVariableCount + " local variable(s), got " + leftValues.size());

                                invoke.arguments().addAll(leftValues.stream().map(v -> new Ins.TLoad(v, scope, LineCol.SYNTHETIC)).collect(Collectors.toList()));
                                invoke.arguments().addAll(values);

                                if (invoke.type().equals(VoidType.get()))
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                invoke, invoke.line_col()
                                        );

                                return invoke;
                        } else {
                                argList = castArgsForMethodInvoke(argList, methodToInvoke.getParameters(), invocation.line_col());

                                if (methodToInvoke.modifiers().contains(SModifier.STATIC)) {
                                        // invoke static
                                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(methodToInvoke, invocation.line_col());
                                        invokeStatic.arguments().addAll(argList);

                                        if (invokeStatic.type().equals(VoidType.get()))
                                                return new ValueAnotherType(
                                                        getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                        invokeStatic, invokeStatic.line_col()
                                                );

                                        return invokeStatic;
                                } else if (methodToInvoke.declaringType() instanceof SInterfaceDef || methodToInvoke.declaringType() instanceof SAnnoDef) {
                                        // invoke interface
                                        if (target == null) {
                                                err.SyntaxException("invoke interface should have an invoke target", invocation.line_col());
                                                return null;
                                        }
                                        Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(target, methodToInvoke, invocation.line_col());
                                        invokeInterface.arguments().addAll(argList);

                                        if (invokeInterface.type().equals(VoidType.get()))
                                                return new ValueAnotherType(
                                                        getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                        invokeInterface, invokeInterface.line_col()
                                                );

                                        return invokeInterface;
                                } else if (doInvokeSpecial || methodToInvoke.modifiers().contains(SModifier.PRIVATE)) {
                                        // invoke special
                                        if (target == null) {
                                                err.SyntaxException("invoke special should have an invoke target", invocation.line_col());
                                                return null;
                                        }
                                        Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(target, methodToInvoke, invocation.line_col());
                                        invokeSpecial.arguments().addAll(argList);

                                        if (invokeSpecial.type().equals(VoidType.get()))
                                                return new ValueAnotherType(
                                                        getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                        invokeSpecial, invokeSpecial.line_col()
                                                );

                                        return invokeSpecial;
                                } else {
                                        // invoke virtual
                                        if (target == null) {
                                                err.SyntaxException("invoke virtual should have an invoke target", invocation.line_col());
                                                return null;
                                        }
                                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(target, methodToInvoke, invocation.line_col());
                                        invokeVirtual.arguments().addAll(argList);

                                        if (invokeVirtual.type().equals(VoidType.get()))
                                                return new ValueAnotherType(
                                                        getTypeWithName("lt.lang.Undefined", LineCol.SYNTHETIC),
                                                        invokeVirtual, invokeVirtual.line_col()
                                                );

                                        return invokeVirtual;
                                }
                        }
                }
        }

        /**
         * check whether the `target` is <code>invokeStatic lt.lang.LtRuntime.getField</code>
         *
         * @param target target
         * @return true or false
         */
        private boolean isGetFieldAtRuntime(Value target) {
                if (target instanceof Ins.InvokeStatic) {
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) target;
                        if (invokeStatic.invokable() instanceof SMethodDef) {
                                SMethodDef m = (SMethodDef) invokeStatic.invokable();
                                if (
                                        (
                                                m.name().equals("getField")
                                        )
                                                && m.declaringType().fullName().equals("lt.lang.LtRuntime")) {
                                        return true;
                                }
                        }
                }
                return false;
        }


        // the following 3 SWAPs are used when choosing the best method to invoke
        /**
         * no swap info
         */
        private static final int SWAP_NONE = 0;
        /**
         * do swap
         */
        private static final int SWAP_SWAP = 1;
        /**
         * do not swap
         */
        private static final int SWAP_NO_SWAP = 2;

        /**
         * find best match
         *
         * @param argList argList
         * @param methods methods to choose from
         * @param lineCol file_line_col
         * @return selected method
         * @throws SyntaxException exception
         */
        private SMethodDef findBestMatch(List<Value> argList, List<SMethodDef> methods, LineCol lineCol) throws SyntaxException {
                if (null == methods || methods.isEmpty()) return null;
                Iterator<SMethodDef> it = methods.iterator();
                SMethodDef method = it.next();
                while (it.hasNext()) {
                        int swap = SWAP_NONE;
                        SMethodDef methodCurrent = it.next();

                        for (int i = 0; i < argList.size(); ++i) {
                                SParameter paramLast = method.getParameters().get(i);
                                SParameter paramCurrent = methodCurrent.getParameters().get(i);

                                if (paramLast.type().isAssignableFrom(paramCurrent.type()) && !paramLast.type().equals(paramCurrent.type())) {
                                        // assignable
                                        if (swap == SWAP_NONE) {
                                                swap = SWAP_SWAP;
                                        } else if (swap == SWAP_NO_SWAP) {
                                                err.SyntaxException("cannot choose between " + method + " and " + methodCurrent + " with args " + argList, lineCol);
                                                return null;
                                        }
                                } else {
                                        // not assignable
                                        if (swap == SWAP_NONE) {
                                                swap = SWAP_NO_SWAP;
                                        } else if (swap == SWAP_SWAP) {
                                                err.SyntaxException("cannot choose between " + method + " and " + methodCurrent + " with args " + argList, lineCol);
                                                return null;
                                        }
                                }
                        }

                        if (swap == SWAP_SWAP) {
                                method = methodCurrent;
                        }
                }
                return method;
        }

        /**
         * cast every argument to the method parameter type
         *
         * @param args       arguments
         * @param parameters parameters
         * @param lineCol    line column info
         * @return a new list containing modified arguments
         * @throws SyntaxException compile error
         */
        private List<Value> castArgsForMethodInvoke(List<Value> args, List<SParameter> parameters, LineCol lineCol) throws SyntaxException {
                List<Value> result = new ArrayList<>();

                for (int i = 0; i < parameters.size(); ++i) {
                        Value v = args.get(i);
                        SParameter param = parameters.get(i);

                        result.add(cast(param.type(), v, lineCol));
                }

                return result;
        }

        /**
         * get object in current scope.
         *
         * @param name  the local variable or field's name
         * @param scope current scope
         * @return TLoad or GetField or GetStatic or NullValue
         */
        private Value findObjectInCurrentScopeWithName(String name, SemanticScope scope) {
                LeftValue v = scope.getLeftValue(name);
                if (v != null) return new Ins.TLoad(v, scope, LineCol.SYNTHETIC);

                // try to get field
                Ins.This aThis = scope.getThis();
                if (aThis != null) {
                        // search for static and non-static
                        SFieldDef field = findFieldFromTypeDef(name, aThis.type(), aThis.type(), FIND_MODE_ANY, true);
                        if (field == null) return NullValue.get();
                        return new Ins.GetField(field, aThis, LineCol.SYNTHETIC);
                } else {
                        // search for static
                        SFieldDef field = findFieldFromTypeDef(name, scope.type(), scope.type(), FIND_MODE_STATIC, true);
                        if (field == null) return NullValue.get();
                        return new Ins.GetStatic(field, LineCol.SYNTHETIC);
                }
        }

        /**
         * {@link Dynamic#bootstrap(MethodHandles.Lookup, String, MethodType)}
         */
        private SMethodDef invokeDynamicBootstrapMethod;

        /**
         * @return {@link Dynamic#bootstrap(MethodHandles.Lookup, String, MethodType)}
         * @throws SyntaxException exception
         */
        private SMethodDef getInvokeDynamicBootstrapMethod() throws SyntaxException {
                if (invokeDynamicBootstrapMethod == null) {
                        SClassDef indyType = (SClassDef) getTypeWithName("lt.lang.Dynamic", LineCol.SYNTHETIC);
                        assert indyType != null;

                        SMethodDef indyMethod = null;
                        for (SMethodDef m : indyType.methods()) {
                                if (m.name().equals("bootstrap") && m.getParameters().size() == 3) {
                                        List<SParameter> parameters = m.getParameters();
                                        if (parameters.get(0).type().fullName().equals("java.lang.invoke.MethodHandles$Lookup")
                                                &&
                                                parameters.get(1).type().fullName().equals("java.lang.String")
                                                &&
                                                parameters.get(2).type().fullName().equals("java.lang.invoke.MethodType")) {
                                                // bootstrap method found
                                                indyMethod = m;
                                                break;
                                        }
                                }
                        }
                        if (indyMethod == null)
                                throw new LtBug("bootstrap method should exist. lt.lang.Dynamic.bootstrap(java.lang.invoke.MethodHandles.Lookup, java.lang.String, java.lang.invoke.MethodType)");
                        invokeDynamicBootstrapMethod = indyMethod;
                }
                return invokeDynamicBootstrapMethod;
        }

        private SMethodDef constructBootstrapMethod;

        private SMethodDef getConstructBootstrapMethod() throws SyntaxException {
                if (constructBootstrapMethod == null) {
                        SClassDef indyType = (SClassDef) getTypeWithName("lt.lang.Dynamic", LineCol.SYNTHETIC);
                        assert indyType != null;

                        SMethodDef indyMethod = null;
                        for (SMethodDef m : indyType.methods()) {
                                if (m.name().equals("bootstrapConstructor") && m.getParameters().size() == 3) {
                                        List<SParameter> parameters = m.getParameters();
                                        if (parameters.get(0).type().fullName().equals("java.lang.invoke.MethodHandles$Lookup")
                                                &&
                                                parameters.get(1).type().fullName().equals("java.lang.String")
                                                &&
                                                parameters.get(2).type().fullName().equals("java.lang.invoke.MethodType")) {
                                                // bootstrap method found
                                                indyMethod = m;
                                                break;
                                        }
                                }
                        }
                        if (indyMethod == null)
                                throw new LtBug("bootstrap method should exist. lt.lang.Dynamic.bootstrapConstructor(java.lang.invoke.MethodHandles.Lookup, java.lang.String, java.lang.invoke.MethodType)");
                        constructBootstrapMethod = indyMethod;
                }
                return constructBootstrapMethod;
        }

        private SMethodDef callFunctionalObjectBootstrapMethod;

        private SMethodDef getCallFunctionalObjectBootstrapMethod() throws SyntaxException {
                if (callFunctionalObjectBootstrapMethod == null) {
                        SClassDef indyType = (SClassDef) getTypeWithName("lt.lang.Dynamic", LineCol.SYNTHETIC);
                        assert indyType != null;

                        SMethodDef indyMethod = null;
                        for (SMethodDef m : indyType.methods()) {
                                if (m.name().equals("bootstrapCallFunctionalObject") && m.getParameters().size() == 3) {
                                        List<SParameter> parameters = m.getParameters();
                                        if (parameters.get(0).type().fullName().equals("java.lang.invoke.MethodHandles$Lookup")
                                                &&
                                                parameters.get(1).type().fullName().equals("java.lang.String")
                                                &&
                                                parameters.get(2).type().fullName().equals("java.lang.invoke.MethodType")) {
                                                // bootstrap method found
                                                indyMethod = m;
                                                break;
                                        }
                                }
                        }
                        if (indyMethod == null)
                                throw new LtBug("bootstrap method should exist. lt.lang.Dynamic.bootstrapCallFunctionalObject(java.lang.invoke.MethodHandles.Lookup, java.lang.String, java.lang.invoke.MethodType)");
                        callFunctionalObjectBootstrapMethod = indyMethod;
                }
                return callFunctionalObjectBootstrapMethod;
        }

        /**
         * change primitive into its box type
         *
         * @param primitive primitive value
         * @param lineCol   file_line_col
         * @return InvokeStatic (all operations must invoke box type's static valueOf(..) method)
         * @throws SyntaxException exception
         */
        private Ins.InvokeStatic boxPrimitive(Value primitive, LineCol lineCol) throws SyntaxException {
                assert primitive.type() instanceof PrimitiveTypeDef;
                if (primitive.type() instanceof ByteTypeDef) {
                        SClassDef aByte = (SClassDef) getTypeWithName("java.lang.Byte", lineCol);
                        assert aByte != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aByte.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(ByteTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Byte.valueOf(byte) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof BoolTypeDef) {
                        SClassDef aBoolean = (SClassDef) getTypeWithName("java.lang.Boolean", lineCol);
                        assert aBoolean != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aBoolean.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(BoolTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Boolean.valueOf(boolean) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof CharTypeDef) {
                        SClassDef integer = (SClassDef) getTypeWithName("java.lang.Character", lineCol);
                        assert integer != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : integer.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(CharTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Character.valueOf(char) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof DoubleTypeDef) {
                        SClassDef aDouble = (SClassDef) getTypeWithName("java.lang.Double", lineCol);
                        assert aDouble != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aDouble.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(DoubleTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Double.valueOf(double) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof FloatTypeDef) {
                        SClassDef aFloat = (SClassDef) getTypeWithName("java.lang.Float", lineCol);
                        assert aFloat != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aFloat.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(FloatTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Float.valueOf(float) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof IntTypeDef) {
                        SClassDef integer = (SClassDef) getTypeWithName("java.lang.Integer", lineCol);
                        assert integer != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : integer.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(IntTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Integer.valueOf(int) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof LongTypeDef) {
                        SClassDef aLong = (SClassDef) getTypeWithName("java.lang.Long", lineCol);
                        assert aLong != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aLong.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(LongTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Long.valueOf(long) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else if (primitive.type() instanceof ShortTypeDef) {
                        SClassDef aShort = (SClassDef) getTypeWithName("java.lang.Short", lineCol);
                        assert aShort != null;

                        SMethodDef valueOf = null;
                        for (SMethodDef m : aShort.methods()) {
                                if (m.name().equals("valueOf") && m.getParameters().size() == 1 && m.getParameters().get(0).type().equals(ShortTypeDef.get())) {
                                        valueOf = m;
                                }
                        }
                        if (valueOf == null) throw new LtBug("java.lang.Short.valueOf(short) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(valueOf, lineCol);
                        invokeStatic.arguments().add(primitive);
                        return invokeStatic;
                } else throw new LtBug("primitive can only be byte/boolean/char/double/float/int/long/short");
        }

        /**
         * unescape the string<br>
         * unescape \n,\t,\r,\',\",\\
         *
         * @param s       the string to unescape
         * @param lineCol file_line_col
         * @return unescaped string
         * @throws SyntaxException exception
         */
        private String unescape(String s, LineCol lineCol) throws SyntaxException {
                char[] chars = s.toCharArray();
                char[] preResult = new char[chars.length];
                int j = 0;
                for (int i = 0; i < chars.length; ++i, ++j) {
                        char c = chars[i];
                        if (c == '\\') {
                                char anotherChar = chars[++i];
                                if (anotherChar == 'n') preResult[j] = '\n';
                                else if (anotherChar == 't') preResult[j] = '\t';
                                else if (anotherChar == 'r') preResult[j] = '\r';
                                else if (anotherChar == '\'') preResult[j] = '\'';
                                else if (anotherChar == '\\') preResult[j] = '\\';
                                else if (anotherChar == '\"') preResult[j] = '\"';
                                else {
                                        err.SyntaxException("cannot unescape \\" + anotherChar, lineCol);
                                        return null;
                                }
                        } else {
                                preResult[j] = c;
                        }
                }
                char[] result = new char[j];
                System.arraycopy(preResult, 0, result, 0, j);
                return new String(result);
        }

        /**
         * parse value from object<br>
         * the object can be<br>
         * <ul>
         * <li>Integer</li>
         * <li>Long</li>
         * <li>Character</li>
         * <li>Short</li>
         * <li>Byte</li>
         * <li>Boolean</li>
         * <li>Float</li>
         * <li>Double</li>
         * <li>String</li>
         * <li>enum</li>
         * <li>Class</li>
         * <li>Annotation</li>
         * <li>Array of all above types</li>
         * </ul>
         *
         * @param o the input object
         * @return Value object.<br>
         * <ul>
         * <li>IntValue</li>
         * <li>LongValue</li>
         * <li>CharValue</li>
         * <li>ShortValue</li>
         * <li>ByteValue</li>
         * <li>BoolValue</li>
         * <li>FloatValue</li>
         * <li>DoubleValue</li>
         * <li>StringConstantValue</li>
         * <li>EnumValue</li>
         * <li>Ins.GetClass</li>
         * <li>SAnno</li>
         * <li>SArrayValue</li>
         * </ul>
         * @throws SyntaxException exception
         */
        private Value parseValueFromObject(Object o) throws SyntaxException {
                // primitives
                if (o instanceof Integer) {
                        return new IntValue((Integer) o);
                } else if (o instanceof Long) {
                        return new LongValue((Long) o);
                } else if (o instanceof Character) {
                        return new CharValue((Character) o);
                } else if (o instanceof Short) {
                        return new ShortValue((Short) o);
                } else if (o instanceof Byte) {
                        return new ByteValue((Byte) o);
                } else if (o instanceof Boolean) {
                        return new BoolValue((Boolean) o);
                } else if (o instanceof Float) {
                        return new FloatValue((Float) o);
                } else if (o instanceof Double) {
                        return new DoubleValue((Double) o);
                } else if (o instanceof String) {
                        // string
                        StringConstantValue v = new StringConstantValue((String) o);
                        v.setType((SClassDef) getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                        return v;
                } else if (o.getClass().isEnum()) {
                        // enum
                        EnumValue e = new EnumValue();
                        e.setType(getTypeWithName(o.getClass().getName(), LineCol.SYNTHETIC));
                        e.setEnumStr(o.toString());
                        return e;
                } else if (o instanceof Class) {
                        // class
                        return new Ins.GetClass(
                                getTypeWithName(((Class) o).getName(), LineCol.SYNTHETIC),
                                (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC));
                } else if (o instanceof Annotation) {
                        // annotation
                        SAnno a = new SAnno();
                        Class<?> annoCls = o.getClass().getInterfaces()[0];
                        a.setAnnoDef((SAnnoDef) getTypeWithName(annoCls.getName(), LineCol.SYNTHETIC));
                        Map<SAnnoField, Value> map = new HashMap<>();
                        for (SAnnoField f : a.type().annoFields()) {
                                try {
                                        Object obj = annoCls.getMethod(f.name()).invoke(o);
                                        Value v = parseValueFromObject(obj);
                                        v = checkAndCastAnnotationValues(v, LineCol.SYNTHETIC);
                                        map.put(f, v);
                                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                        throw new LtBug(e);
                                }
                        }
                        a.values().putAll(map);

                        return a;
                } else if (o.getClass().isArray()) {
                        // array
                        assert !o.getClass().getComponentType().isArray();

                        SArrayValue arr = new SArrayValue();
                        int length = Array.getLength(o);
                        Value[] values = new Value[length];
                        for (int i = 0; i < length; ++i) {
                                Object elem = Array.get(o, i);
                                Value v = parseValueFromObject(elem);
                                values[i] = v;
                        }
                        arr.setType((SArrayTypeDef) getTypeWithName(o.getClass().getName(), LineCol.SYNTHETIC));
                        arr.setDimension(1);
                        arr.setValues(values);

                        return arr;
                } else throw new LtBug("cannot parse " + o + " into Value");
        }

        /**
         * record abstract methods for override check. methods are retrieved from interfaces.
         *
         * @param i               the interface to retrieve method from
         * @param abstractMethods record abstract methods
         * @param visitedMethods  already visited methods
         * @param visitedType     already visited types
         * @throws SyntaxException exception
         */
        private void recordAbstractMethodsForOverrideCheck_interface(SInterfaceDef i,
                                                                     List<SMethodDef> abstractMethods,
                                                                     List<SMethodDef> visitedMethods,
                                                                     Set<SInterfaceDef> visitedType) throws SyntaxException {
                // check only when it's not visited
                if (visitedType.add(i)) {
                        for (SMethodDef m : i.methods()) {
                                if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                        if (null == findMethodWithSameSignature(m, visitedMethods, true)) {
                                                abstractMethods.add(m);
                                        }
                                }
                                visitedMethods.add(m);
                        }
                        // check super interfaces
                        for (SInterfaceDef ii : i.superInterfaces()) {
                                recordAbstractMethodsForOverrideCheck_interface(
                                        ii, abstractMethods, visitedMethods, visitedType
                                );
                        }
                }
        }

        /**
         * record abstract methods for override check. methods are retrieved from classes.
         *
         * @param c               the class to retrieve method from
         * @param abstractMethods recorded abstract methods
         * @param visitedMethods  already visited methods
         * @param visitedType     already visited types
         * @throws SyntaxException exception
         */
        private void recordAbstractMethodsForOverrideCheck_class(SClassDef c,
                                                                 List<SMethodDef> abstractMethods,
                                                                 List<SMethodDef> visitedMethods,
                                                                 Set<SInterfaceDef> visitedType) throws SyntaxException {
                if (c == null || !c.modifiers().contains(SModifier.ABSTRACT)) return;
                // check the class
                for (SMethodDef m : c.methods()) {
                        if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                if (null == findMethodWithSameSignature(m, visitedMethods, true)) {
                                        abstractMethods.add(m);
                                }
                        }
                        visitedMethods.add(m);
                }
                // check parent class
                recordAbstractMethodsForOverrideCheck_class(c.parent(), abstractMethods, visitedMethods, visitedType);
                // check interfaces
                for (SInterfaceDef i : c.superInterfaces()) {
                        recordAbstractMethodsForOverrideCheck_interface(
                                i, abstractMethods, visitedMethods, visitedType
                        );
                }
        }

        /**
         * record abstract methods for override check.
         *
         * @param c               the base class
         * @param abstractMethods record abstract methods
         * @throws SyntaxException exception
         */
        private void recordAbstractMethodsForOverrideCheck(SClassDef c, List<SMethodDef> abstractMethods) throws SyntaxException {
                List<SMethodDef> visitedMethods = new ArrayList<>();
                Set<SInterfaceDef> visitedTypes = new HashSet<>();
                recordAbstractMethodsForOverrideCheck_class(
                        c.parent(),
                        abstractMethods,
                        visitedMethods,
                        visitedTypes);
                for (SInterfaceDef i : c.superInterfaces()) {
                        recordAbstractMethodsForOverrideCheck_interface(
                                i, abstractMethods, visitedMethods, visitedTypes
                        );
                }
        }

        /**
         * check whether the overridden method is modified with final.
         * if not, the overridden/override list would be added with corresponding value
         *
         * @param method           method in current type
         * @param overriddenMethod method in super class/interface
         * @throws SyntaxException compiling error
         */
        private void checkFinalAndOverride(SMethodDef method, SMethodDef overriddenMethod) throws SyntaxException {
                if (overriddenMethod.modifiers().contains(SModifier.FINAL)) {
                        err.SyntaxException(overriddenMethod + " cannot be overridden", method.line_col());
                        return;
                }

                overriddenMethod.overridden().add(method);
                method.overRide().add(overriddenMethod);
        }

        /**
         * check whether the method overrides method in the class (and its parent classes and interfaces)
         *
         * @param method       method
         * @param sClassDef    class
         * @param visitedTypes types already visited
         * @throws SyntaxException exception
         */
        private void checkOverride_class(SMethodDef method,
                                         SClassDef sClassDef,
                                         Set<STypeDef> visitedTypes) throws SyntaxException {
                if (visitedTypes.contains(sClassDef)) return;
                visitedTypes.add(sClassDef);

                SMethodDef methodInSuper = findMethodWithSameSignature(method, sClassDef.methods(), false);
                if (methodInSuper == null) {
                        // check super class
                        if (sClassDef.parent() != null) {
                                checkOverride_class(method, sClassDef.parent(), visitedTypes);
                        }
                        // check super interfaces
                        for (SInterfaceDef i : sClassDef.superInterfaces()) {
                                checkOverride_interface(method, i, visitedTypes);
                        }
                } else {
                        checkFinalAndOverride(method, methodInSuper);
                }
        }

        /**
         * check whether the method overrides method in the interface (and its super interfaces)
         *
         * @param method        method
         * @param sInterfaceDef interface
         * @param visitedTypes  types already visited
         * @throws SyntaxException exception
         */
        private void checkOverride_interface(SMethodDef method,
                                             SInterfaceDef sInterfaceDef,
                                             Set<STypeDef> visitedTypes) throws SyntaxException {
                if (visitedTypes.contains(sInterfaceDef)) return;
                visitedTypes.add(sInterfaceDef);

                SMethodDef methodInSuper = findMethodWithSameSignature(method, sInterfaceDef.methods(), false);
                if (methodInSuper == null) {
                        // check super interfaces
                        for (SInterfaceDef i : sInterfaceDef.superInterfaces()) {
                                checkOverride_interface(method, i, visitedTypes);
                        }
                } else {
                        checkFinalAndOverride(method, methodInSuper);
                }
        }

        /**
         * check whether the class already overrides all abstract methods in super class/interfaces
         *
         * @param sTypeDef type to be checked
         * @throws SyntaxException exception
         */
        private void checkOverride(STypeDef sTypeDef) throws SyntaxException {
                if (sTypeDef instanceof SClassDef) {
                        for (SMethodDef m : ((SClassDef) sTypeDef).methods()) {
                                checkOverride_class(m, ((SClassDef) sTypeDef).parent(), new HashSet<>());
                                for (SInterfaceDef i : ((SClassDef) sTypeDef).superInterfaces()) {
                                        checkOverride_interface(m, i, new HashSet<>());
                                }
                        }
                } else if (sTypeDef instanceof SInterfaceDef) {
                        for (SMethodDef m : ((SInterfaceDef) sTypeDef).methods()) {
                                for (SInterfaceDef i : ((SInterfaceDef) sTypeDef).superInterfaces()) {
                                        checkOverride_interface(m, i, new HashSet<>());
                                }
                        }
                } else {
                        throw new LtBug("wrong STypeDefType " + sTypeDef.getClass());
                }
        }

        /**
         * check whether the interface extends itself
         *
         * @param toCheck  the interface to be checked
         * @param current  list of super interfaces
         * @param recorder a queue of interfaces to be checked
         * @throws SyntaxException exception
         */
        private void checkInterfaceCircularInheritance(final SInterfaceDef toCheck, List<SInterfaceDef> current, List<SInterfaceDef> recorder) throws SyntaxException {
                for (SInterfaceDef i : current) {
                        recorder.add(i);
                        if (i.equals(toCheck)) {
                                err.SyntaxException("circular inheritance " + recorder, LineCol.SYNTHETIC);
                                return;
                        }
                        checkInterfaceCircularInheritance(toCheck, i.superInterfaces(), recorder);
                        recorder.remove(recorder.size() - 1);
                }
        }

        /**
         * find method with same signature<br>
         * method name and arguments should be the same<br>
         * if the above conditions are matched, the found return type should be assignable from return type of the method to be checked
         *
         * @param method             the method to be checked
         * @param methodList         super methods
         * @param onlyCheckSignature only check signature, doesn't check modifier and return type
         * @return found method or null
         * @throws SyntaxException exception
         */
        private SMethodDef findMethodWithSameSignature(SMethodDef method,
                                                       List<SMethodDef> methodList,
                                                       boolean onlyCheckSignature) throws SyntaxException {
                outer:
                for (SMethodDef m : methodList) {
                        // same name
                        if (m.name().equals(method.name())) {
                                // same param length
                                if (m.getParameters().size() == method.getParameters().size()) {
                                        // same type
                                        for (int i = 0; i < m.getParameters().size(); ++i) {
                                                SParameter p = m.getParameters().get(i);
                                                SParameter param = method.getParameters().get(i);
                                                if (!p.type().equals(param.type())) {
                                                        continue outer;
                                                }
                                        }

                                        if (!onlyCheckSignature) {

                                                // method
                                                // check access modifier
                                                // super is private, continue
                                                if (m.modifiers().contains(SModifier.PRIVATE)) continue;
                                                if (
                                                        // super is public, this is not public
                                                        (m.modifiers().contains(SModifier.PUBLIC)
                                                                && !method.modifiers().contains(SModifier.PUBLIC)
                                                        )
                                                                ||
                                                                // super is protected, this is not public/protected
                                                                (m.modifiers().contains(SModifier.PROTECTED)
                                                                        && !m.modifiers().contains(SModifier.PUBLIC)
                                                                        && !m.modifiers().contains(SModifier.PROTECTED)
                                                                )
                                                                ||
                                                                // super is package access, (this is private or they are not in same pkg)
                                                                (!m.modifiers().contains(SModifier.PUBLIC)
                                                                        && !m.modifiers().contains(SModifier.PROTECTED)
                                                                        && !m.modifiers().contains(SModifier.PRIVATE)
                                                                        &&
                                                                        (
                                                                                method.modifiers().contains(SModifier.PRIVATE)
                                                                                        ||
                                                                                        !m.declaringType().pkg().equals(method.declaringType().pkg())
                                                                        )
                                                                )
                                                        ) {
                                                        err.SyntaxException(method + " cannot override " + m, method.line_col());
                                                        return null;
                                                }

                                                if (!m.getReturnType().isAssignableFrom(method.getReturnType())
                                                        &&
                                                        !method.getReturnType().isAssignableFrom(m.getReturnType())) {
                                                        err.SyntaxException(m + " return type should be assignable from " + method + " 's", method.line_col());
                                                        return null;
                                                }

                                        }

                                        return m;
                                }
                        }
                }
                return null;
        }

        private Map<SAnno, AST.Anno> annotationRecorder = new HashMap<>();

        /**
         * parse the annotations<br>
         * but the <tt>annotationFields</tt> won't be set
         *
         * @param annos                 a set of annotations
         * @param annotationPresentable the annotation is presented on the object
         * @param imports               imports
         * @param type                  the annotation accepts element type
         * @throws SyntaxException exception
         */
        private void parseAnnos(Set<AST.Anno> annos,
                                SAnnotationPresentable annotationPresentable,
                                List<Import> imports,
                                ElementType type,
                                List<ElementType> checkTheseWhenFail) throws SyntaxException {
                for (AST.Anno anno : annos) {
                        SAnnoDef annoType = (SAnnoDef) getTypeWithAccess(anno.anno, imports);
                        assert annoType != null;

                        if (annoType.canPresentOn(type)) {
                                SAnno s = new SAnno();
                                s.setAnnoDef(annoType);
                                s.setPresent(annotationPresentable);
                                annotationPresentable.annos().add(s);

                                annotationRecorder.put(s, anno);
                        } else {
                                boolean fail = true;
                                for (ElementType t : checkTheseWhenFail) {
                                        if (annoType.canPresentOn(t)) {
                                                fail = false;
                                                break;
                                        }
                                }
                                if (fail) {
                                        err.SyntaxException("annotation " + annoType + " cannot present on " + type, anno.line_col());
                                        return;
                                }
                        }
                }
        }

        /**
         * parse parameters
         *
         * @param variableDefList a list of parameters (in the form of VariableDef)
         * @param i               parameter length (invokes the method with i+1 parameters)
         * @param invokable       the parameters belong to this object
         * @param imports         imports
         * @throws SyntaxException exceptions
         */
        private void parseParameters(List<VariableDef> variableDefList, int i, SInvokable invokable, List<Import> imports,
                                     boolean allowAccessModifier) throws SyntaxException {
                for (int j = 0; j < i; ++j) {
                        // foreach variable
                        // set their name|target(constructor)|type|modifier(val)|anno_general
                        // and add into parameter list

                        VariableDef v = variableDefList.get(j);
                        SParameter param = new SParameter();
                        param.setName(v.getName());
                        param.setTarget(invokable);
                        STypeDef type;
                        if (v.getType() == null) {
                                type = getTypeWithName("java.lang.Object",
                                        v.line_col());
                        } else {
                                type = getTypeWithAccess(v.getType(), imports);
                        }
                        param.setType(type);

                        for (Modifier m : v.getModifiers()) {
                                switch (m.modifier) {
                                        case VAL:
                                                param.setCanChange(false);
                                                break;
                                        case PUBLIC:
                                        case PRIVATE:
                                        case PROTECTED:
                                        case PKG:
                                                if (!allowAccessModifier) {
                                                        err.SyntaxException("access modifiers for parameters are only allowed on class constructing parameters",
                                                                m.line_col());
                                                        return;
                                                }
                                                break;
                                        default:
                                                err.UnexpectedTokenException("valid modifier for parameters (val)", m.toString(), m.line_col());
                                                return;
                                }
                        }

                        parseAnnos(v.getAnnos(), param, imports, ElementType.PARAMETER,
                                // the modifier may be field
                                allowAccessModifier ? Collections.singletonList(ElementType.FIELD) : Collections.emptyList()
                        );

                        invokable.getParameters().add(param);
                }
        }

        /**
         * parse field<br>
         * the field would be added into the type
         *
         * @param v        field (in the form of VariableDef)
         * @param type     the field is defined in this type
         * @param imports  imports
         * @param mode     {@link #PARSING_CLASS} {@link #PARSING_INTERFACE}
         * @param isStatic whether the field is static
         * @throws SyntaxException exception
         */
        private void parseField(VariableDef v, STypeDef type, List<Import> imports, int mode, boolean isStatic) throws SyntaxException {
                // field, set its name|type|modifier|annos|declaringClass
                SFieldDef fieldDef = new SFieldDef(v.line_col());
                fieldDef.setName(v.getName()); // name

                // type
                fieldDef.setType(
                        v.getType() == null
                                ? getTypeWithName("java.lang.Object", v.line_col())
                                : getTypeWithAccess(v.getType(), imports)
                );
                fieldDef.setDeclaringType(type); // declaringClass

                // try to get access flags
                boolean hasAccessModifier = false;
                for (Modifier m : v.getModifiers()) {
                        if (m.modifier.equals(Modifier.Available.PUBLIC)
                                || m.modifier.equals(Modifier.Available.PRIVATE)
                                || m.modifier.equals(Modifier.Available.PROTECTED)
                                || m.modifier.equals(Modifier.Available.PKG)) {
                                hasAccessModifier = true;
                        }
                }
                if (!hasAccessModifier) {
                        if (mode == PARSING_CLASS) {
                                if (isStatic) {
                                        fieldDef.modifiers().add(SModifier.PUBLIC); // default modifier for static field is public
                                } else {
                                        fieldDef.modifiers().add(SModifier.PRIVATE); // default modifier for instance field is private
                                }
                        } else if (mode == PARSING_INTERFACE) {
                                fieldDef.modifiers().add(SModifier.PUBLIC);
                                fieldDef.modifiers().add(SModifier.STATIC);
                        } // no else
                }
                // modifiers
                for (Modifier m : v.getModifiers()) {
                        switch (m.modifier) {
                                case PUBLIC:
                                        fieldDef.modifiers().add(SModifier.PUBLIC);
                                        break;
                                case PRIVATE:
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        fieldDef.modifiers().add(SModifier.PRIVATE);
                                        break;
                                case PROTECTED:
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        fieldDef.modifiers().add(SModifier.PROTECTED);
                                        break;
                                case PKG: // no need to assign modifier
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        break;
                                case VAL:
                                        fieldDef.modifiers().add(SModifier.FINAL);
                                        break;
                                default:
                                        err.UnexpectedTokenException("valid modifier for fields (class:(public|private|protected|pkg|val)|interface:(pub|val))", m.toString(), m.line_col());
                                        return;
                        }
                }
                if (mode == PARSING_INTERFACE && !fieldDef.modifiers().contains(SModifier.FINAL)) {
                        fieldDef.modifiers().add(SModifier.FINAL);
                }
                if (isStatic && mode == PARSING_CLASS) {
                        fieldDef.modifiers().add(SModifier.STATIC);
                }
                // annos
                parseAnnos(v.getAnnos(), fieldDef, imports, ElementType.FIELD, Collections.singletonList(ElementType.PARAMETER));

                // check whether the field has already been defined
                List<SFieldDef> fields;
                if (mode == PARSING_CLASS) {
                        fields = ((SClassDef) type).fields();
                } else if (mode == PARSING_INTERFACE) {
                        fields = ((SInterfaceDef) type).fields();
                } else throw new LtBug(Integer.toString(mode)); // no else
                for (SFieldDef f : fields) {
                        if (fieldDef.name().equals(f.name())) {
                                if (
                                        (fieldDef.modifiers().contains(SModifier.STATIC) && f.modifiers().contains(SModifier.STATIC))
                                                ||
                                                (!fieldDef.modifiers().contains(SModifier.STATIC) && !f.modifiers().contains(SModifier.STATIC))) {
                                        err.DuplicateVariableNameException(v.getName(), v.line_col());
                                        return;
                                }
                        }
                }

                // add into class/interface
                if (mode == PARSING_CLASS) {
                        ((SClassDef) type).fields().add(fieldDef);
                } else {
                        // mode == PARSING_INTERFACE;
                        ((SInterfaceDef) type).fields().add(fieldDef);
                }
        }

        /**
         * parse method<br>
         * the method would be added into the type
         *
         * @param m          method (in the form of MethodDef)
         * @param i          parameter length to parse
         * @param type       the method is defined in this type
         * @param lastMethod the last parsed method (with i+1 parameter length)
         * @param imports    imports
         * @param mode       {@link #PARSING_CLASS} {@link #PARSING_INTERFACE}
         * @param isStatic   whether the method is static
         * @throws SyntaxException exception
         */
        private void parseMethod(MethodDef m, int i, STypeDef type, SMethodDef lastMethod, List<Import> imports, int mode, boolean isStatic) throws SyntaxException {
                // method name|declaringType|returnType|parameters|modifier|anno
                SMethodDef methodDef = new SMethodDef(m.line_col());
                methodDef.setName(m.name);
                methodDef.setDeclaringType(type);
                methodDef.setReturnType(
                        m.returnType == null
                                ? getTypeWithName("java.lang.Object", m.line_col())
                                : getTypeWithAccess(m.returnType, imports)
                );
                parseParameters(m.params, i, methodDef, imports, false);

                // modifier
                // try to get access flags
                boolean hasAccessModifier = false;
                for (Modifier mod : m.modifiers) {
                        if (mod.modifier.equals(Modifier.Available.PUBLIC)
                                || mod.modifier.equals(Modifier.Available.PRIVATE)
                                || mod.modifier.equals(Modifier.Available.PROTECTED)
                                || mod.modifier.equals(Modifier.Available.PKG)) {
                                hasAccessModifier = true;
                        }
                }
                if (!hasAccessModifier) {
                        methodDef.modifiers().add(SModifier.PUBLIC); // default modifier is public
                }
                for (Modifier mod : m.modifiers) {
                        switch (mod.modifier) {
                                case PUBLIC:
                                        methodDef.modifiers().add(SModifier.PUBLIC);
                                        break;
                                case PRIVATE:
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        methodDef.modifiers().add(SModifier.PRIVATE);
                                        break;
                                case PROTECTED:
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        methodDef.modifiers().add(SModifier.PROTECTED);
                                        break;
                                case PKG: // no need to assign modifier
                                        if (mode == PARSING_INTERFACE) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString(), m.line_col());
                                                return;
                                        }
                                        break;
                                case VAL:
                                        methodDef.modifiers().add(SModifier.FINAL);
                                        break;
                                case ABSTRACT:
                                        methodDef.modifiers().add(SModifier.ABSTRACT);
                                        // check method body. it should no have body
                                        if (!m.body.isEmpty()) {
                                                err.SyntaxException("abstract methods cannot have body", m.line_col());
                                                return;
                                        }
                                        break;
                                default:
                                        err.UnexpectedTokenException("valid modifier for fields (class:(public|private|protected|pkg|val)|interface:(pub|val))", m.toString(), m.line_col());
                                        return;
                        }
                }
                if (isStatic) {
                        methodDef.modifiers().add(SModifier.STATIC);
                }
                if (mode == PARSING_INTERFACE && !methodDef.modifiers().contains(SModifier.ABSTRACT) && m.body.isEmpty()) {
                        methodDef.modifiers().add(SModifier.ABSTRACT);
                }

                // annos
                parseAnnos(m.annos, methodDef, imports, ElementType.METHOD, Collections.emptyList());

                // check can be added into class
                List<SMethodDef> methods;
                if (mode == PARSING_CLASS) {
                        methods = ((SClassDef) type).methods();
                } else if (mode == PARSING_INTERFACE) {
                        methods = ((SInterfaceDef) type).methods();
                } else throw new LtBug("invalid mode " + mode);
                for (SMethodDef builtMethod : methods) {
                        if (builtMethod.name().equals(methodDef.name())) {
                                // names are the same
                                // check param length
                                if (builtMethod.getParameters().size() == methodDef.getParameters().size()) {
                                        // sizes are the same
                                        // check types
                                        int size = methodDef.getParameters().size();
                                        List<SParameter> builtParam = builtMethod.getParameters();
                                        List<SParameter> current = methodDef.getParameters();
                                        boolean passCheck = false;
                                        for (int in = 0; in < size; ++in) {
                                                if (builtParam.get(in).type() != current.get(in).type()) {
                                                        passCheck = true;
                                                        break;
                                                }
                                        }
                                        if (!passCheck) {
                                                err.SyntaxException("method signature check failed on " + methodDef, m.line_col());
                                                return;
                                        }
                                }
                        }
                }

                if (null != lastMethod) {
                        Map<SInvokable, Expression> invoke = new HashMap<>();
                        invoke.put(lastMethod, m.params.get(i).getInit());
                        defaultParamInvokable.put(methodDef, invoke);
                }

                // add into class/interface
                if (mode == PARSING_CLASS) {
                        ((SClassDef) type).methods().add(methodDef);
                } else {
                        // mode == PARSING_INTERFACE;
                        ((SInterfaceDef) type).methods().add(methodDef);
                }
        }

        /**
         * get Import/ClassDef/InterfaceDef from Statement object
         *
         * @param stmt          the statement
         * @param imports       import list
         * @param classDefs     classDef list
         * @param interfaceDefs interfaceDef list
         * @throws UnexpectedTokenException the statement is not import/class/interface
         */
        private void select_import_class_interface_fun(Statement stmt,
                                                       List<Import> imports,
                                                       List<ClassDef> classDefs,
                                                       List<InterfaceDef> interfaceDefs,
                                                       List<FunDef> funDefs) throws UnexpectedTokenException {
                if (stmt instanceof Import) {
                        imports.add((Import) stmt);
                } else if (stmt instanceof ClassDef) {
                        classDefs.add((ClassDef) stmt);
                } else if (stmt instanceof InterfaceDef) {
                        interfaceDefs.add((InterfaceDef) stmt);
                } else if (stmt instanceof FunDef) {
                        funDefs.add((FunDef) stmt);
                } else {
                        err.UnexpectedTokenException("class/interface definition or import", stmt.toString(), stmt.line_col());
                        // code won't reach here
                }
        }

        /**
         * get type by class name
         *
         * @param clsName class name
         * @param lineCol file_line_col
         * @return STypeDef (not null)
         * @throws SyntaxException exception
         */
        private STypeDef getTypeWithName(String clsName, LineCol lineCol) throws SyntaxException {
                if (types.containsKey(clsName)) {
                        return types.get(clsName);
                } else {
                        // check already compiled class
                        try {
                                Class<?> cls = loadClass(clsName);
                                if (cls.isArray()) {
                                        String name = cls.getName();
                                        int dimension = 0;
                                        while (cls.isArray()) {
                                                ++dimension;
                                                cls = cls.getComponentType();
                                        }
                                        SArrayTypeDef arrType = new SArrayTypeDef();
                                        arrType.setFullName(name);
                                        putNameAndTypeDef(arrType, lineCol);
                                        arrType.setDimension(dimension);
                                        arrType.setType(getTypeWithName(cls.getName(), lineCol));

                                        return arrType;
                                } else {

                                        List<SModifier> modifiers; // modifiers
                                        STypeDef typeDef;
                                        if (cls.isAnnotation()) {
                                                SAnnoDef a = new SAnnoDef();
                                                a.setFullName(clsName);

                                                typeDef = a;
                                                modifiers = a.modifiers();
                                        } else if (cls.isInterface()) {
                                                SInterfaceDef i = new SInterfaceDef(LineCol.SYNTHETIC);
                                                i.setFullName(clsName);

                                                typeDef = i;
                                                modifiers = i.modifiers();
                                        } else { // class
                                                SClassDef c = new SClassDef(false, LineCol.SYNTHETIC);
                                                c.setFullName(clsName);

                                                typeDef = c;
                                                modifiers = c.modifiers();
                                        }
                                        if (cls.getPackage() != null) {
                                                typeDef.setPkg(cls.getPackage().getName());
                                        }
                                        // put into map
                                        putNameAndTypeDef(typeDef, lineCol);
                                        // annos
                                        getAnnotationFromAnnotatedElement(cls, typeDef);
                                        // modifiers
                                        getModifierFromClass(cls, modifiers);

                                        if (typeDef instanceof SInterfaceDef) {
                                                SInterfaceDef i = (SInterfaceDef) typeDef;
                                                // super interfaces
                                                getSuperInterfaceFromClass(cls, i.superInterfaces());
                                                // fields methods
                                                getFieldsAndMethodsFromClass(cls, i, i.fields(), i.methods());
                                        } else if (typeDef instanceof SClassDef) {
                                                SClassDef c = (SClassDef) typeDef;
                                                // super interfaces
                                                getSuperInterfaceFromClass(cls, ((SClassDef) typeDef).superInterfaces());
                                                if (cls != Object.class) {
                                                        // super class
                                                        ((SClassDef) typeDef).setParent((SClassDef) getTypeWithName(cls.getSuperclass().getName(), lineCol));
                                                }
                                                // fields methods
                                                getFieldsAndMethodsFromClass(cls, c, c.fields(), c.methods());
                                                // constructors
                                                for (Constructor<?> con : cls.getDeclaredConstructors()) {
                                                        SConstructorDef constructorDef = new SConstructorDef(LineCol.SYNTHETIC);
                                                        constructorDef.setDeclaringType(c);

                                                        getAnnotationFromAnnotatedElement(con, constructorDef);
                                                        getParameterFromClassArray(con.getParameterTypes(), constructorDef);
                                                        getModifierFromMember(con, constructorDef);

                                                        c.constructors().add(constructorDef);
                                                }
                                        } else {
                                                // typeDef instanceof SAnnoDef;

                                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                                // parse anno fields
                                                for (Method annoM : cls.getDeclaredMethods()) {
                                                        assert annoM.getParameters().length == 0;
                                                        SAnnoField annoField = new SAnnoField();
                                                        annoField.setName(annoM.getName());
                                                        annoField.setType(getTypeWithName(annoM.getReturnType().getName(), lineCol));

                                                        annoDef.annoFields().add(annoField);
                                                }
                                        }
                                        return typeDef;
                                }
                        } catch (ClassNotFoundException e) {
                                err.SyntaxException("undefined class " + clsName, lineCol);
                                return null;
                        }
                }
        }

        /**
         * parse the annotations
         *
         * @param elem        java AnnotatedElement
         * @param presentable compiler presentable object
         * @throws SyntaxException exception
         */
        private void getAnnotationFromAnnotatedElement(AnnotatedElement elem, SAnnotationPresentable presentable) throws SyntaxException {
                for (Annotation a : elem.getAnnotations()) {
                        Class<?> aClass = a.getClass();
                        while (aClass != null && !aClass.isAnnotation() && aClass.getInterfaces().length != 0) {
                                aClass = aClass.getInterfaces()[0];
                        }
                        if (aClass != null && aClass.isAnnotation()) {
                                SAnno sAnno = new SAnno();
                                sAnno.setPresent(presentable);
                                sAnno.setAnnoDef((SAnnoDef) getTypeWithName(aClass.getName(), LineCol.SYNTHETIC));

                                presentable.annos().add(sAnno);

                                // retrieve annotation keys and values
                                for (Method m : aClass.getDeclaredMethods()) {
                                        assert m.getParameterCount() == 0;
                                        try {
                                                sAnno.alreadyCompiledAnnotationValueMap().put(m.getName(), m.invoke(a));
                                        } catch (Throwable e) {
                                                // the exception should never occur
                                                throw new LtBug(e);
                                        }
                                }
                        }
                }
        }

        /**
         * get modifier from class object<br>
         * and add them into the list
         *
         * @param cls       class object
         * @param modifiers modifiers
         */
        private void getModifierFromClass(Class<?> cls, List<SModifier> modifiers) {
                int ms = cls.getModifiers();
                if (java.lang.reflect.Modifier.isAbstract(ms)) {
                        modifiers.add(SModifier.ABSTRACT);
                }
                if (java.lang.reflect.Modifier.isFinal(ms)) {
                        modifiers.add(SModifier.FINAL);
                }
                if (java.lang.reflect.Modifier.isNative(ms)) {
                        modifiers.add(SModifier.NATIVE);
                }
                if (java.lang.reflect.Modifier.isPrivate(ms)) {
                        modifiers.add(SModifier.PRIVATE);
                }
                if (java.lang.reflect.Modifier.isProtected(ms)) {
                        modifiers.add(SModifier.PROTECTED);
                }
                if (java.lang.reflect.Modifier.isPublic(ms)) {
                        modifiers.add(SModifier.PUBLIC);
                }
                if (java.lang.reflect.Modifier.isStatic(ms)) {
                        modifiers.add(SModifier.STATIC);
                }
                if (java.lang.reflect.Modifier.isStrict(ms)) {
                        modifiers.add(SModifier.STRICT);
                }
                if (java.lang.reflect.Modifier.isSynchronized(ms)) {
                        modifiers.add(SModifier.SYNCHRONIZED);
                }
                if (java.lang.reflect.Modifier.isTransient(ms)) {
                        modifiers.add(SModifier.TRANSIENT);
                }
                if (java.lang.reflect.Modifier.isVolatile(ms)) {
                        modifiers.add(SModifier.VOLATILE);
                }
        }

        /**
         * get modifiers from Member<br>
         * and append them to the SMember object
         *
         * @param member  member
         * @param sMember sMember
         */
        private void getModifierFromMember(Member member, SMember sMember) {
                int ms = member.getModifiers();
                List<SModifier> modifiers = sMember.modifiers();
                if (java.lang.reflect.Modifier.isAbstract(ms)) {
                        modifiers.add(SModifier.ABSTRACT);
                }
                if (java.lang.reflect.Modifier.isFinal(ms)) {
                        modifiers.add(SModifier.FINAL);
                }
                if (java.lang.reflect.Modifier.isNative(ms)) {
                        modifiers.add(SModifier.NATIVE);
                }
                if (java.lang.reflect.Modifier.isPrivate(ms)) {
                        modifiers.add(SModifier.PRIVATE);
                }
                if (java.lang.reflect.Modifier.isProtected(ms)) {
                        modifiers.add(SModifier.PROTECTED);
                }
                if (java.lang.reflect.Modifier.isPublic(ms)) {
                        modifiers.add(SModifier.PUBLIC);
                }
                if (java.lang.reflect.Modifier.isStatic(ms)) {
                        modifiers.add(SModifier.STATIC);
                }
                if (java.lang.reflect.Modifier.isStrict(ms)) {
                        modifiers.add(SModifier.STRICT);
                }
                if (java.lang.reflect.Modifier.isSynchronized(ms)) {
                        modifiers.add(SModifier.SYNCHRONIZED);
                }
                if (java.lang.reflect.Modifier.isTransient(ms)) {
                        modifiers.add(SModifier.TRANSIENT);
                }
                if (java.lang.reflect.Modifier.isVolatile(ms)) {
                        modifiers.add(SModifier.VOLATILE);
                }
        }

        /**
         * get suepr interface from class
         *
         * @param cls        class
         * @param interfaces interfaces
         * @throws SyntaxException exception
         */
        private void getSuperInterfaceFromClass(Class<?> cls, List<SInterfaceDef> interfaces) throws SyntaxException {
                for (Class<?> i : cls.getInterfaces()) {
                        if (!i.isAnnotation()) {
                                interfaces.add((SInterfaceDef) getTypeWithName(i.getName(), LineCol.SYNTHETIC));
                        }
                }
        }

        /**
         * get field and methods from class object
         *
         * @param cls           class object
         * @param declaringType field/method is defined in this type
         * @param fields        field list
         * @param methods       method list
         * @throws SyntaxException exceptions
         */
        private void getFieldsAndMethodsFromClass(Class<?> cls, STypeDef declaringType, List<SFieldDef> fields, List<SMethodDef> methods) throws SyntaxException {
                for (Field f : cls.getDeclaredFields()) {
                        SFieldDef fieldDef = new SFieldDef(LineCol.SYNTHETIC);
                        fieldDef.setName(f.getName());
                        fieldDef.setType(getTypeWithName(f.getType().getName(), LineCol.SYNTHETIC));
                        getModifierFromMember(f, fieldDef);

                        getAnnotationFromAnnotatedElement(f, fieldDef);

                        fieldDef.setDeclaringType(declaringType);
                        fields.add(fieldDef);
                }

                for (Method m : cls.getDeclaredMethods()) {
                        SMethodDef methodDef = new SMethodDef(LineCol.SYNTHETIC);
                        methodDef.setName(m.getName());
                        methodDef.setDeclaringType(declaringType);
                        if (m.getReturnType().equals(Void.TYPE)) {
                                methodDef.setReturnType(VoidType.get());
                        } else {
                                methodDef.setReturnType(getTypeWithName(m.getReturnType().getName(), LineCol.SYNTHETIC));
                        }

                        getAnnotationFromAnnotatedElement(m, methodDef);

                        getModifierFromMember(m, methodDef);

                        // parameters
                        getParameterFromClassArray(m.getParameterTypes(), methodDef);

                        methods.add(methodDef);
                }
        }

        /**
         * get parameter from class array<br>
         * the array is from getParameterTypes();
         *
         * @param paramTypes parameter types
         * @param invokable  the parameters belong to this invokable
         * @throws SyntaxException exception
         */
        private void getParameterFromClassArray(Class<?>[] paramTypes, SInvokable invokable) throws SyntaxException {
                for (Class<?> paramType : paramTypes) {
                        SParameter param = new SParameter();
                        param.setName("?");
                        param.setTarget(invokable);
                        param.setType(getTypeWithName(paramType.getName(), LineCol.SYNTHETIC));

                        getAnnotationFromAnnotatedElement(paramType, param);

                        invokable.getParameters().add(param);
                }
        }

        /**
         * check whether the type exists<br>
         * first check types to compile, then check existing types
         *
         * @param type class name
         * @return true/false
         */
        private boolean typeExists(String type) {
                if (!types.containsKey(type)) {
                        try {
                                loadClass(type);
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                                return false;
                        }
                }
                return true;
        }

        /**
         * get class name from Access object.<br>
         * the access object may contain package or class only
         *
         * @param access access
         * @return retrieved class name
         */
        private String getClassNameFromAccess(AST.Access access) {
                String pre;
                if (access.exp instanceof AST.Access) {
                        pre = getClassNameFromAccess((AST.Access) access.exp) + ".";
                } else if (access.exp instanceof AST.PackageRef) {
                        pre = ((AST.PackageRef) access.exp).pkg.replace("::", ".") + ".";
                } else {
                        pre = "";
                }
                return pre + access.name;
        }

        /**
         * find class name with import statements
         *
         * @param name    the simple name or full name to be found
         * @param imports import statements
         * @return found class name or <tt>null</tt> if not found
         */
        private String findClassNameWithImport(String name, List<Import> imports) {
                if (typeExists(name)) return name;
                // first try to find those classes with the same simple name
                // e.g. import java.util.List
                for (Import i : imports) {
                        if (!i.importAll) {
                                if (i.access.name.equals(name)) {
                                        // use this name
                                        return getClassNameFromAccess(i.access);
                                }
                        }
                }
                // try to find `import all`
                for (Import i : imports) {
                        if (i.importAll && i.pkg != null) {
                                String possibleClassName = i.pkg.pkg.replace("::", ".") + "." + name;
                                if (typeExists(possibleClassName)) return possibleClassName;
                        }
                }

                return null;
        }

        /**
         * get type with access
         *
         * @param access  access object
         * @param imports import list
         * @return retrieved STypeDef (not null)
         * @throws SyntaxException exception
         */
        private STypeDef getTypeWithAccess(AST.Access access, List<Import> imports) throws SyntaxException {
                assert access.exp == null || access.exp instanceof AST.PackageRef || "[]".equals(access.name);

                if ("[]".equals(access.name)) {
                        assert access.exp instanceof AST.Access;
                        STypeDef type = getTypeWithAccess((AST.Access) access.exp, imports);
                        int dimension;
                        if (type instanceof SArrayTypeDef) {
                                SArrayTypeDef a = (SArrayTypeDef) type;
                                type = a.type();
                                dimension = a.dimension() + 1;
                        } else {
                                dimension = 1;
                        }
                        SArrayTypeDef a = new SArrayTypeDef();
                        a.setType(type);
                        a.setDimension(dimension);
                        if (types.containsKey(a.fullName())) {
                                return types.get(a.fullName());
                        } else {
                                putNameAndTypeDef(a, access.line_col());
                                return a;
                        }

                }

                assert access.exp == null || access.exp instanceof AST.PackageRef;

                AST.PackageRef pkg = (AST.PackageRef) access.exp;
                String name = access.name;

                String className;
                if (pkg == null) {
                        className = findClassNameWithImport(name, imports);
                } else {
                        className = pkg.pkg.replace("::", ".") + "." + name;
                }

                if (null == className || !typeExists(className)) {
                        err.SyntaxException("type " + name + " not defined", access.line_col());
                        return null;
                }
                return getTypeWithName(className, access.line_col());
        }

        /**
         * put {fileName:TypeDef} into the map
         *
         * @param type    STypeDef object
         * @param lineCol file_line_col
         * @throws SyntaxException exception
         */
        private void putNameAndTypeDef(STypeDef type, LineCol lineCol) throws SyntaxException {
                if (types.containsKey(type.fullName())) {
                        err.SyntaxException("duplicate type names " + type.fullName(), lineCol);
                        // code won't reach here
                } else {
                        types.put(type.fullName(), type);
                }
        }

        /**
         * load a class
         *
         * @param name class name
         * @return load a class
         * @throws ClassNotFoundException exception
         */
        private Class<?> loadClass(String name) throws ClassNotFoundException {
                int dimensions = 0; // 0 means not an array
                if (name.startsWith("[") && name.endsWith(";")) {
                        int i = 0;
                        char[] chars = name.toCharArray();
                        for (; i < chars.length; ++i) {
                                if (chars[i] == '[') {
                                        ++dimensions;
                                } else {
                                        break;
                                }
                        }
                        name = name.substring(i + 1); // chars[i] should be `L`
                        name = name.substring(0, name.length() - 1); // remove `;`
                }

                Class<?> cls;
                try {
                        cls = Class.forName(name);
                } catch (ClassNotFoundException e) {
                        cls = classLoader.loadClass(name);
                }

                if (dimensions == 0) {
                        return cls;
                } else {
                        int[] d = new int[dimensions];
                        return Array.newInstance(cls, d).getClass();
                }
        }
}
