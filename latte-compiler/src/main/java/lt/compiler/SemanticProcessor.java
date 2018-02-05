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
import lt.compiler.semantic.helper.ASTGHolder;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.compiler.util.BindList;
import lt.compiler.util.Consts;
import lt.generator.SourceGenerator;
import lt.dependencies.asm.MethodVisitor;
import lt.lang.GenericTemplate;
import lt.lang.Unit;
import lt.lang.function.Function1;
import lt.runtime.*;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * semantic processor
 */
public class SemanticProcessor {
        public static final int PARSING_CLASS = 0;
        public static final int PARSING_INTERFACE = 1;

        public static final String DYNAMIC_CLASS_NAME = Dynamic.class.getName();

        public static final int INDEX_invoke_targetClass = 0;
        public static final int INDEX_invoke_o = 1;
        public static final int INDEX_invoke_isStatic = 2;
        public static final int INDEX_invoke_functionalObject = 3;
        public static final int INDEX_invoke_invoker = 4;
        public static final int INDEX_invoke_method = 5;
        public static final int INDEX_invoke_primitives = 6;
        public static final int INDEX_invoke_args = 7;
        public static final int INDEX_invoke_canInvokeImport = 8;

        /**
         * Map&lt;FileName, List of statements&gt;
         */
        public final Map<String, List<Statement>> mapOfStatements;
        /**
         * maps full name to type<br>
         * one type should only exist once in a Processor
         */
        public Map<String, STypeDef> types = new HashMap<String, STypeDef>();
        /**
         * full name to {@link ClassDef} from {@link Parser}
         */
        public Map<String, ASTGHolder<ClassDef>> originalClasses = new HashMap<String, ASTGHolder<ClassDef>>();
        /**
         * full name to {@link InterfaceDef} from {@link Parser}
         */
        public Map<String, ASTGHolder<InterfaceDef>> originalInterfaces = new HashMap<String, ASTGHolder<InterfaceDef>>();
        /**
         * full name to {@link FunDef} from {@link Parser}
         */
        public Map<String, ASTGHolder<FunDef>> originalFunctions = new HashMap<String, ASTGHolder<FunDef>>();
        /**
         * full name to {@link ObjectDef} from {@link Parser}
         */
        public Map<String, ASTGHolder<ObjectDef>> originalObjects = new HashMap<String, ASTGHolder<ObjectDef>>();
        /**
         * full name to {@link AnnotationDef} from {@link Parser}
         */
        public Map<String, ASTGHolder<AnnotationDef>> originalAnnotations = new HashMap<String, ASTGHolder<AnnotationDef>>();
        /**
         * {@link SMethodDef} to it's containing statements
         */
        public Map<SMethodDef, List<Statement>> methodToStatements = new HashMap<SMethodDef, List<Statement>>();
        /**
         * file name to Import info
         */
        public Map<String, List<Import>> fileNameToImport = new HashMap<String, List<Import>>();
        /**
         * a set of types that should be return value of {@link #parse()} method.<br>
         * these types are to be compiled into byte codes
         */
        public final Set<STypeDef> typeDefSet = new HashSet<STypeDef>();
        /**
         * invokable =&gt; (the-invokable-to-invoke =&gt; the current default parameter).
         */
        public Map<SInvokable, Map<SInvokable, Expression>> defaultParamInvokable = new HashMap<SInvokable, Map<SInvokable, Expression>>();
        /**
         * retrieve existing classes from this class loader
         */
        public final ClassLoader classLoader;
        /**
         * error manager
         */
        public final ErrorManager err;
        /**
         * access which represents a type can be converted into instantiation
         */
        public boolean enableTypeAccess = true;
        /**
         * source files
         */
        private static List<ZipFile> sourceClasses = new LinkedList<ZipFile>();
        private boolean alreadyWarnJar = false;

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
        }

        public static String byte2hex(byte[] b) {
                StringBuilder sb = new StringBuilder();
                String tmp;
                for (byte aB : b) {
                        tmp = Integer.toHexString(aB & 0XFF);
                        if (tmp.length() == 1) {
                                sb.append("0").append(tmp);
                        } else {
                                sb.append(tmp);
                        }

                }
                return sb.toString();
        }

        @SuppressWarnings("unused")
        public static byte[] hex2byte(String str) {
                str = str.trim();
                int len = str.length();

                if (len == 0 || len % 2 == 1) {
                        return null;
                }

                byte[] b = new byte[len / 2];
                for (int i = 0; i < str.length(); i += 2) {
                        b[i / 2] = (byte) Integer.decode("0X" + str.substring(i, i + 2)).intValue();
                }
                return b;
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
                Map<String, List<ClassDef>> fileNameToClassDef = new HashMap<String, List<ClassDef>>();
                Map<String, List<InterfaceDef>> fileNameToInterfaceDef = new HashMap<String, List<InterfaceDef>>();
                Map<String, List<FunDef>> fileNameToFunctions = new HashMap<String, List<FunDef>>();
                Map<String, List<ObjectDef>> fileNameToObjectDef = new HashMap<String, List<ObjectDef>>();
                Map<String, List<AnnotationDef>> fileNameToAnnotationDef = new HashMap<String, List<AnnotationDef>>();
                final Map<String, String> fileNameToPackageName = new HashMap<String, String>();
                // record types and packages
                for (String fileName : mapOfStatements.keySet()) {
                        List<Statement> statements = mapOfStatements.get(fileName);
                        Iterator<Statement> statementIterator = statements.iterator();

                        // import
                        List<Import> imports = new ArrayList<Import>();
                        // class definition
                        List<ClassDef> classDefs = new ArrayList<ClassDef>();
                        // interface definition
                        List<InterfaceDef> interfaceDefs = new ArrayList<InterfaceDef>();
                        // fun definition
                        List<FunDef> funDefs = new ArrayList<FunDef>();
                        // object definition
                        List<ObjectDef> objectDefs = new ArrayList<ObjectDef>();
                        // annotation definition
                        List<AnnotationDef> annotationDefs = new ArrayList<AnnotationDef>();

                        // put into map
                        fileNameToImport.put(fileName, imports);
                        fileNameToClassDef.put(fileName, classDefs);
                        fileNameToInterfaceDef.put(fileName, interfaceDefs);
                        fileNameToFunctions.put(fileName, funDefs);
                        fileNameToObjectDef.put(fileName, objectDefs);
                        fileNameToAnnotationDef.put(fileName, annotationDefs);

                        // package
                        String pkg; // if no package, then it's an empty string, otherwise, it's 'packageName.' with dot at the end
                        if (statementIterator.hasNext()) {
                                Statement statement = statementIterator.next();
                                if (statement instanceof PackageDeclare) {
                                        PackageDeclare p = (PackageDeclare) statement;
                                        pkg = p.pkg.pkg.replace("::", ".") + ".";
                                } else {
                                        pkg = "";
                                        select_import_class_interface_fun_object(
                                                statement, imports, classDefs, interfaceDefs, funDefs, objectDefs, annotationDefs);
                                }
                                while (statementIterator.hasNext()) {
                                        Statement stmt = statementIterator.next();
                                        select_import_class_interface_fun_object(
                                                stmt, imports, classDefs, interfaceDefs, funDefs, objectDefs, annotationDefs);
                                }
                        } else continue;

                        // add package into import list at index 0
                        imports.add(0, new Import(new AST.PackageRef(
                                pkg.endsWith(".")
                                        ? pkg.substring(0, pkg.length() - 1).replace(".", "::")
                                        : pkg
                                , LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        // add java.lang into import list
                        // java::lang::_
                        // lt::lang::_
                        // lt::util::Utils._
                        imports.add(new Import(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        imports.add(new Import(new AST.PackageRef("java::lang", LineCol.SYNTHETIC), null, true, false, LineCol.SYNTHETIC));
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::util", LineCol.SYNTHETIC), "Utils", LineCol.SYNTHETIC), true, false, LineCol.SYNTHETIC));
                        // import implicit built in casts
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::lang::implicit", LineCol.SYNTHETIC), "PrimitivesImplicit", LineCol.SYNTHETIC), false, true, LineCol.SYNTHETIC));
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::lang::implicit", LineCol.SYNTHETIC), "StringImplicit", LineCol.SYNTHETIC), false, true, LineCol.SYNTHETIC));
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::lang::implicit", LineCol.SYNTHETIC), "CollectionImplicit", LineCol.SYNTHETIC), false, true, LineCol.SYNTHETIC));
                        imports.add(new Import(null, new AST.Access(new AST.PackageRef("lt::lang::implicit", LineCol.SYNTHETIC), "ObjectImplicit", LineCol.SYNTHETIC), false, true, LineCol.SYNTHETIC));

                        fileNameToPackageName.put(fileName, pkg);
                }
                for (String fileName : mapOfStatements.keySet()) {
                        // import
                        List<Import> imports = fileNameToImport.get(fileName);
                        // class definition
                        List<ClassDef> classDefs = fileNameToClassDef.get(fileName);
                        // interface definition
                        List<InterfaceDef> interfaceDefs = fileNameToInterfaceDef.get(fileName);
                        // fun definition
                        List<FunDef> funDefs = fileNameToFunctions.get(fileName);
                        // package name
                        String pkg = fileNameToPackageName.get(fileName);
                        // object definition
                        List<ObjectDef> objectDefs = fileNameToObjectDef.get(fileName);
                        // annotation definition
                        List<AnnotationDef> annotationDefs = fileNameToAnnotationDef.get(fileName);

                        // check importing same simple name
                        Set<String> importSimpleNames = new HashSet<String>();
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
                                recordClass(c, pkg, Collections.<STypeDef>emptyList());
                        }
                        for (InterfaceDef i : interfaceDefs) {
                                recordInterface(i, pkg, Collections.<STypeDef>emptyList());
                        }
                        for (FunDef f : funDefs) {
                                recordFun(f, pkg, Collections.<STypeDef>emptyList());
                        }
                        for (ObjectDef o : objectDefs) {
                                recordObject(o, pkg, Collections.<STypeDef>emptyList());
                        }
                        for (AnnotationDef a : annotationDefs) {
                                recordAnnotation(a, pkg, Collections.<STypeDef>emptyList());
                        }
                }

                for (String file : mapOfStatements.keySet()) {
                        List<Statement> stmts = mapOfStatements.get(file);
                        final List<Import> imports = fileNameToImport.get(file);
                        for (Statement stmt : stmts) {
                                try {
                                        stmt.foreachInnerStatements(new Function1<Boolean, Statement>() {
                                                @Override
                                                public Boolean apply(Statement statement) throws Exception {
                                                        if (statement instanceof AST.Access) {
                                                                AST.Access access = (AST.Access) statement;
                                                                handleGenericAST(access, imports, fileNameToPackageName);
                                                                return false;
                                                        }
                                                        return true;
                                                }
                                        });
                                } catch (SyntaxException e) {
                                        throw e;
                                } catch (Exception e) {
                                        throw new LtBug("should not have any exception other than SyntaxException", e);
                                }
                        }
                }
                // all classes occurred in the parsing process will be inside `types` map or is already defined

                // check package and type exists
                for (String fileName : mapOfStatements.keySet()) {
                        // import
                        List<Import> imports = fileNameToImport.get(fileName);
                        ListIterator<Import> ite = imports.listIterator();
                        while (ite.hasNext()) {
                                Import i = ite.next();
                                if (i.pkg == null) {
                                        // try class
                                        STypeDef type = getTypeWithAccess(i.access, Collections.<String, STypeDef>emptyMap(), Collections.<Import>emptyList(), true);
                                        if (null == type) {
                                                boolean pass = false;
                                                if (i.importAll) {
                                                        // test whether it's a package
                                                        AST.Access access = i.access;
                                                        AST.PackageRef pkgRef = checkAndGetPackage(fileNameToPackageName, access);
                                                        if (pkgRef != null) {
                                                                pass = true;
                                                                ite.set(new Import(pkgRef, null, true, i.implicit, i.line_col()));
                                                        }
                                                        // else {
                                                        // do nothing
                                                        // should error }
                                                } else {
                                                        AST.Access newAccessWithPkg = transformAccess(i.access);
                                                        if (i.access != newAccessWithPkg) {
                                                                pass = true;
                                                                // replace the original object with new one
                                                                ite.set(new Import(null, newAccessWithPkg, false, i.implicit, i.line_col()));
                                                        }
                                                }
                                                if (!pass) {
                                                        err.SyntaxException("Type " + i.access + " not found", i.access.line_col());
                                                }
                                        } // else: pass, for that a type is retrieved
                                } else {
                                        // check package exists
                                        String pkg = i.pkg.pkg.replace("::", ".");
                                        if (!isPackage(fileNameToPackageName, pkg)) {
                                                err.SyntaxException("Package " + i.pkg.pkg + " not found", i.pkg.line_col());
                                        }
                                }
                        }
                }

                step2(fileNameToPackageName);
                step3();
                // ensures that @ImplicitImports and @StaticImports are loaded
                getTypeWithName("lt.runtime.ImplicitImports", LineCol.SYNTHETIC);
                getTypeWithName("lt.runtime.StaticImports", LineCol.SYNTHETIC);
                getTypeWithName("java.lang.annotation.Retention", LineCol.SYNTHETIC);
                step4();
                addImportImplicit();
                addImportStatic();
                addRetention();

                return typeDefSet;
        }

        private void recordClass(ClassDef c, String pkg, List<STypeDef> generics) throws SyntaxException {
                String className = buildTemplateAppliedName(pkg + c.name, generics);
                // check occurrence
                if (typeExists(className)) {
                        err.SyntaxException("duplicate type names " + className, c.line_col());
                        return;
                }

                SClassDef sClassDef = new SClassDef(SClassDef.NORMAL, c.line_col());
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
                                        err.UnexpectedTokenException("valid modifier for class (val|abstract|public|private|protected|internal)", m.toString(), m.line_col());
                                        return;
                        }
                }

                types.put(className, sClassDef); // record the class
                originalClasses.put(className, new ASTGHolder<ClassDef>(c, generics));
                typeDefSet.add(sClassDef);
        }

        private void recordInterface(InterfaceDef i, String pkg, List<STypeDef> generics) throws SyntaxException {
                String interfaceName = buildTemplateAppliedName(pkg + i.name, generics);
                // check occurrence
                if (typeExists(interfaceName)) {
                        err.SyntaxException("duplicate type names " + interfaceName, i.line_col());
                        return;
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
                                        return;
                        }
                }

                types.put(interfaceName, sInterfaceDef); // record the interface
                originalInterfaces.put(interfaceName, new ASTGHolder<InterfaceDef>(i, generics));
                typeDefSet.add(sInterfaceDef);
        }

        private void recordFun(FunDef f, String pkg, List<STypeDef> generics) throws SyntaxException {
                String className = buildTemplateAppliedName(pkg + f.name, generics);
                // check occurrence
                if (typeExists(className)) {
                        err.SyntaxException("duplicate type names " + className, f.line_col());
                        return;
                }

                SClassDef sClassDef = new SClassDef(SClassDef.FUN, f.line_col());
                sClassDef.setFullName(className);
                sClassDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                sClassDef.modifiers().add(SModifier.PUBLIC);
                sClassDef.modifiers().add(SModifier.FINAL);

                types.put(className, sClassDef); // record the class
                originalFunctions.put(className, new ASTGHolder<FunDef>(f, generics));
                typeDefSet.add(sClassDef);
        }

        private void recordObject(ObjectDef o, String pkg, List<STypeDef> generics) throws SyntaxException {
                String className = buildTemplateAppliedName(pkg + o.name, generics);
                // check occurrence
                if (typeExists(className)) {
                        err.SyntaxException("duplicate type names " + className, o.line_col());
                        return;
                }

                SClassDef sClassDef = new SClassDef(SClassDef.OBJECT, o.line_col());
                sClassDef.setFullName(className);
                sClassDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                sClassDef.modifiers().add(SModifier.PUBLIC);
                sClassDef.modifiers().add(SModifier.FINAL);

                types.put(className, sClassDef);
                originalObjects.put(className, new ASTGHolder<ObjectDef>(o, generics));
                typeDefSet.add(sClassDef);
        }

        private void recordAnnotation(AnnotationDef a, String pkg, List<STypeDef> generics) throws SyntaxException {
                String annoName = buildTemplateAppliedName(pkg + a.name, generics);
                // check occurrence
                if (typeExists(annoName)) {
                        err.SyntaxException("duplicate type names " + annoName, a.line_col());
                        return;
                }

                SAnnoDef sAnnoDef = new SAnnoDef(a.line_col());
                sAnnoDef.setPkg(pkg);
                sAnnoDef.setFullName(annoName);

                types.put(annoName, sAnnoDef);
                originalAnnotations.put(annoName, new ASTGHolder<AnnotationDef>(a, generics));
                typeDefSet.add(sAnnoDef);
        }

        private void handleGenericAST(AST.Access access,
                                      List<Import> imports,
                                      Map<String, String> fileNameToPackageName) throws SyntaxException {
                if (access.generics.isEmpty()) {
                        return;
                }
                for (AST.Access g : access.generics) {
                        handleGenericAST(g, imports, fileNameToPackageName);
                }
                AST.Access accessWithoutGeneric = new AST.Access(access.exp, access.name, access.line_col());
                String templateName = accessToClassName(accessWithoutGeneric, Collections.<String, STypeDef>emptyMap(), imports, false);
                String clsName = accessToClassName(access, Collections.<String, STypeDef>emptyMap(), imports, false);
                if (types.containsKey(clsName)) {
                        // already defined
                        // ignore and return
                        return;
                }
                List<STypeDef> genericTypes = new ArrayList<STypeDef>();
                for (AST.Access a : access.generics) {
                        STypeDef t = getTypeWithAccess(a, Collections.<String, STypeDef>emptyMap(), imports);
                        genericTypes.add(t);
                }
                int genericParamSize;
                if (originalClasses.containsKey(templateName)) {
                        ClassDef ast = originalClasses.get(templateName).s;
                        String file = ast.line_col().fileName;
                        recordClass(ast, fileNameToPackageName.get(file), genericTypes);

                        genericParamSize = ast.generics.size();

                } else if (originalInterfaces.containsKey(templateName)) {
                        InterfaceDef ast = originalInterfaces.get(templateName).s;
                        String file = ast.line_col().fileName;
                        recordInterface(ast, fileNameToPackageName.get(file), genericTypes);

                        genericParamSize = ast.generics.size();

                } else if (originalObjects.containsKey(templateName)) {
                        ObjectDef ast = originalObjects.get(templateName).s;
                        String file = ast.line_col().fileName;
                        recordObject(ast, fileNameToPackageName.get(file), genericTypes);

                        genericParamSize = ast.generics.size();

                } else if (originalFunctions.containsKey(templateName)) {
                        err.SyntaxException("function definitions are never generic types", access.line_col());
                        return;

                } else if (originalAnnotations.containsKey(templateName)) {
                        err.SyntaxException("annotations are never generic types", access.line_col());
                        return;

                } else {
                        err.SyntaxException("type " + templateName + " not found", access.line_col());
                        return;
                }

                if (genericParamSize != access.generics.size()) {
                        err.SyntaxException("generic params size is " + genericParamSize
                                        + ", but generic args size is " + access.generics.size(),
                                access.line_col());
                }
        }

        private AST.PackageRef checkAndGetPackage(Map<String, String> fileNameToPackageName, AST.Access access) {
                if (access.exp == null) {
                        if (isPackage(fileNameToPackageName, access.name)) {
                                return new AST.PackageRef(access.name, access.line_col());
                        } else {
                                return null;
                        }
                }
                if (!(access.exp instanceof AST.Access)) {
                        return null;
                }
                List<String> pkgSplitList = new ArrayList<String>();
                pkgSplitList.add(access.name);
                AST.Access tmp = (AST.Access) access.exp;
                while (true) {
                        pkgSplitList.add(tmp.name);
                        Expression exp = tmp.exp;
                        if (null == exp) break;
                        if (!(exp instanceof AST.Access)) return null;
                        tmp = (AST.Access) exp;
                }
                Collections.reverse(pkgSplitList);
                StringBuilder pkgBuilder = new StringBuilder();
                boolean isFirst = true;
                for (String pkgSplitName : pkgSplitList) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                pkgBuilder.append(".");
                        }
                        pkgBuilder.append(pkgSplitName);
                }
                String pkg = pkgBuilder.toString();
                if (isPackage(fileNameToPackageName, pkg)) {
                        return new AST.PackageRef(pkg.replace(".", "::"), access.line_col());
                } else {
                        return null;
                }
        }

        private boolean isPackage(Map<String, String> fileNameToPackageName, String javaPkg) {
                return fileNameToPackageName.containsValue(javaPkg + ".")
                        || packageExistsInClassPath(javaPkg, classLoader)
                        || packageExistInJRE(javaPkg);
        }

        private void addImportImplicit() throws SyntaxException {
                // validate imports
                for (List<Import> imports : fileNameToImport.values()) {
                        for (Import im : imports) {
                                if (im.implicit) {
                                        STypeDef type = getTypeWithAccess(im.access, Collections.<String, STypeDef>emptyMap(), Collections.<Import>emptyList());
                                        if (!(type instanceof SClassDef)) {
                                                err.SyntaxException("import implicit should be an implicit class", im.line_col());
                                                return;
                                        }
                                        SClassDef cType = (SClassDef) type;
                                        int count = 0;
                                        for (SAnno a : cType.annos()) {
                                                if (a.type().fullName().equals("lt.runtime.Implicit")) {
                                                        ++count;
                                                }
                                        }
                                        if (count != 1) {
                                                err.SyntaxException("import implicit should be an implicit class", im.line_col());
                                                return;
                                        }
                                }
                        }
                }

                SAnnoDef ImplicitImports = (SAnnoDef) getTypeWithName("lt.runtime.ImplicitImports", LineCol.SYNTHETIC);
                SArrayTypeDef classArrayTypeDef = (SArrayTypeDef) getTypeWithName("[Ljava.lang.Class;", LineCol.SYNTHETIC);
                SClassDef classTypeDef = (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC);
                SAnnoField annoField = null;
                for (SAnnoField f : ImplicitImports.annoFields()) {
                        if (f.name().equals("implicitImports")) {
                                annoField = f;
                                break;
                        }
                }
                if (annoField == null) throw new LtBug("lt.runtime.ImplicitImports#implicitImports should exist");
                for (STypeDef sTypeDef : typeDefSet) {
                        if (sTypeDef instanceof SAnnoDef) continue;
                        // filter
                        if (sTypeDef.line_col().fileName == null || !fileNameToImport.containsKey(sTypeDef.line_col().fileName)) {
                                continue;
                        }
                        int count = 0;
                        for (SAnno anno : sTypeDef.annos()) {
                                if (anno.type().fullName().equals("lt.runtime.ImplicitImports")) {
                                        ++count;
                                }
                        }
                        boolean alreadyExists = count > 0;
                        if (alreadyExists) continue;
                        List<Import> imports = fileNameToImport.get(sTypeDef.line_col().fileName);
                        List<Ins.GetClass> valueList = new ArrayList<Ins.GetClass>();
                        Set<Ins.GetClass> tmpSet = new HashSet<Ins.GetClass>(); // distinct
                        for (Import i : imports) {
                                if (i.implicit) {
                                        Ins.GetClass getC = new Ins.GetClass(getTypeWithAccess(i.access, Collections.<String, STypeDef>emptyMap(), Collections.<Import>emptyList()), classTypeDef);
                                        if (!getC.targetType().equals(sTypeDef)) {
                                                if (tmpSet.add(getC)) {
                                                        valueList.add(getC);
                                                }
                                        }
                                }
                        }
                        if (valueList.isEmpty()) continue;

                        // build the annotation instance
                        SAnno importImplicit = new SAnno();
                        importImplicit.setAnnoDef(ImplicitImports);
                        importImplicit.setPresent(sTypeDef);
                        sTypeDef.annos().add(importImplicit);

                        // build the array instance
                        SArrayValue arrayValue = new SArrayValue();
                        arrayValue.setDimension(1);
                        arrayValue.setType(classArrayTypeDef);

                        Value[] valueArray = new Value[valueList.size()];
                        arrayValue.setValues(valueList.toArray(valueArray));

                        // add value into anno
                        importImplicit.values().put(annoField, arrayValue);
                }
        }

        private void addImportStatic() throws SyntaxException {
                SAnnoDef StaticImports = (SAnnoDef) getTypeWithName("lt.runtime.StaticImports", LineCol.SYNTHETIC);
                SArrayTypeDef classArrayTypeDef = (SArrayTypeDef) getTypeWithName("[Ljava.lang.Class;", LineCol.SYNTHETIC);
                SClassDef classTypeDef = (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC);
                SAnnoField annoField = null;
                for (SAnnoField f : StaticImports.annoFields()) {
                        if (f.name().equals("staticImports")) {
                                annoField = f;
                                break;
                        }
                }
                if (annoField == null) throw new LtBug("lt.runtime.StaticImports#staticImports should exist");
                for (STypeDef sTypeDef : typeDefSet) {
                        if (sTypeDef instanceof SAnnoDef) continue;
                        // filter
                        if (sTypeDef.line_col().fileName == null || !fileNameToImport.containsKey(sTypeDef.line_col().fileName)) {
                                continue;
                        }
                        int count = 0;
                        for (SAnno anno : sTypeDef.annos()) {
                                if (anno.type().fullName().equals("lt.runtime.StaticImports")) {
                                        ++count;
                                }
                        }
                        boolean alreadyExists = count > 0;
                        if (alreadyExists) continue;
                        List<Import> imports = fileNameToImport.get(sTypeDef.line_col().fileName);
                        List<Ins.GetClass> valueList = new ArrayList<Ins.GetClass>();
                        Set<Ins.GetClass> tmpSet = new HashSet<Ins.GetClass>(); // distinct
                        for (Import i : imports) {
                                if (i.importAll && i.pkg == null) {
                                        Ins.GetClass getC = new Ins.GetClass(getTypeWithAccess(i.access, Collections.<String, STypeDef>emptyMap(), Collections.<Import>emptyList()), classTypeDef);
                                        if (!getC.targetType().equals(sTypeDef)) {
                                                if (tmpSet.add(getC)) {
                                                        valueList.add(getC);
                                                }
                                        }
                                }
                        }
                        if (valueList.isEmpty()) continue;

                        // build the annotation instance
                        SAnno importStatic = new SAnno();
                        importStatic.setAnnoDef(StaticImports);
                        importStatic.setPresent(sTypeDef);
                        sTypeDef.annos().add(importStatic);

                        // build the array instance
                        SArrayValue arrayValue = new SArrayValue();
                        arrayValue.setDimension(1);
                        arrayValue.setType(classArrayTypeDef);

                        Value[] valueArray = new Value[valueList.size()];
                        arrayValue.setValues(valueList.toArray(valueArray));

                        // add value into anno
                        importStatic.values().put(annoField, arrayValue);
                }
        }

        private void addRetention() throws SyntaxException {
                SAnnoDef RetentionType = (SAnnoDef) getTypeWithName("java.lang.annotation.Retention", LineCol.SYNTHETIC);
                SAnnoField value = null;
                EnumValue enumValue = new EnumValue();
                enumValue.setType(getTypeWithName("java.lang.annotation.RetentionPolicy", LineCol.SYNTHETIC));
                enumValue.setEnumStr("RUNTIME");

                for (SAnnoField af : RetentionType.annoFields()) {
                        if (af.name().equals("value")) {
                                value = af;
                                break;
                        }
                }
                assert value != null;
                out:
                for (STypeDef sTypeDef : typeDefSet) {
                        if (!(sTypeDef instanceof SAnnoDef)) continue;
                        SAnnoDef sAnnoDef = (SAnnoDef) sTypeDef;
                        for (SAnno anno : sAnnoDef.annos()) {
                                if (anno.type().equals(RetentionType)) {
                                        continue out;
                                }
                        }
                        SAnno anno = new SAnno();
                        anno.setAnnoDef(RetentionType);
                        anno.setPresent(sAnnoDef);
                        anno.values().put(value, enumValue);
                        sAnnoDef.annos().add(anno);
                }
        }

        public static boolean packageExistsInClassPath(String pkg, ClassLoader classLoader) {
                if (classLoader == null) return false;
                String path = pkg.replace(".", "/");
                URL url = classLoader.getResource(path);
                return url != null || packageExistsInClassPath(pkg, classLoader.getParent());
        }

        public boolean packageExistInJRE(String pkg) {
                if (alreadyWarnJar) return true;
                if (sourceClasses.isEmpty()) {
                        String homePath = System.getProperty("java.home");
                        if (homePath == null) {
                                err.warning("Cannot find java home via System.getProperty('java.home')");
                                alreadyWarnJar = true;
                                return true; // assume it's a valid import
                        }
                        File homePathFile = new File(homePath);
                        File[] rtFileA = null;

                        boolean found = false;

                        // the file may be in $JAVA_HOME/../Contents/Classes/classes.jar
                        // instead of $JAVA_HOME/lib/rt.jar

                        // check for classes.jar
                        File[] classesFileA = homePathFile.getParentFile().listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File f) {
                                        return f.getName().equals("Classes");
                                }
                        });
                        if (classesFileA != null && classesFileA.length != 0) {
                                File classesFile = classesFileA[0];
                                rtFileA = classesFile.listFiles(
                                        new FileFilter() {
                                                @Override
                                                public boolean accept(File f) {
                                                        return f.getName().equals("classes.jar");
                                                }
                                        });
                                found = true;
                        }

                        // not found
                        // check rt.jar
                        if (!found) {
                                File[] libFileA = homePathFile.listFiles(
                                        new FileFilter() {
                                                @Override
                                                public boolean accept(File f) {
                                                        return f.getName().equals("lib");
                                                }
                                        });
                                if (libFileA == null || libFileA.length == 0) {
                                        err.warning(homePath + "/lib not exist");
                                        alreadyWarnJar = true;
                                        return true;
                                }
                                File libFile = libFileA[0];
                                rtFileA = libFile.listFiles(
                                        new FileFilter() {
                                                @Override
                                                public boolean accept(File f) {
                                                        return f.getName().equals("rt.jar");
                                                }
                                        });
                                if (rtFileA != null && rtFileA.length > 0 && rtFileA[0].exists()) {
                                        found = true;
                                }
                        }

                        if (found) {
                                assert rtFileA != null;
                                File rtFile = rtFileA[0];
                                if (!rtFile.exists()) {
                                        err.warning(homePath + "/lib/rt.jar not exist");
                                        alreadyWarnJar = true;
                                        return true;
                                }
                                try {
                                        sourceClasses.add(new JarFile(rtFile));
                                } catch (IOException e) {
                                        err.warning("Occurred exception " + e + " when opening rt.jar");
                                        alreadyWarnJar = true;
                                        return true;
                                }
                        } else {
                                // check java 9 mods
                                if (!findJava9(homePathFile)) {
                                        err.warning(homePath + "/lib/rt.jar not exist");
                                        alreadyWarnJar = true;
                                        return true;
                                }
                        }
                }
                return findPackage(pkg, sourceClasses);
        }

        private boolean findJava9(File home) {
                return findJava9JMods(home);
        }

        private boolean findJava9JMods(File home) {
                File[] jmodsDirA = home.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                                return file.getName().equals("jmods") && file.isDirectory();
                        }
                });
                if (jmodsDirA == null || jmodsDirA.length == 0) {
                        return false;
                }
                File jmodsDir = jmodsDirA[0];
                File[] mods = jmodsDir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                                return file.getName().endsWith(".jmod") && file.isFile();
                        }
                });
                if (mods == null || mods.length == 0) {
                        return false;
                }
                for (File f : mods) {
                        try {
                                sourceClasses.add(new ZipFile(f));
                        } catch (IOException e) {
                                return false;
                        }
                }
                return true;
        }

        private static boolean findPackage(String pkg, List<ZipFile> files) {
                for (ZipFile f : files) {
                        if (findPackage(pkg, f.entries())) {
                                return true;
                        }
                }
                return false;
        }

        private static boolean findPackage(String pkg, Enumeration<? extends ZipEntry> entries) {
                while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(pkg.replace(".", "/"))) {
                                return true;
                        }
                        if (entry.getName().startsWith("classes/" + pkg.replace(".", "/"))) {
                                return true;
                        }
                }
                return false;
        }

        /**
         * ======= step 2 =======
         * build fields,methods,constructors,parameters,parent-classes,super-interfaces,annotations.
         * but no details (annotation's values|method statements|constructor statements won't be parsed)
         *
         * @param fileNameToPackageName file name to package name
         * @throws SyntaxException exception
         */
        public void step2(Map<String, String> fileNameToPackageName) throws SyntaxException {
                for (STypeDef sTypeDef : typeDefSet) {
                        if (isGenericTemplateType(sTypeDef)) {
                                continue; // ignore the template types
                        }
                        String fileName = sTypeDef.line_col().fileName;
                        List<Import> imports = fileNameToImport.get(fileName);
                        String pkg = fileNameToPackageName.get(fileName);

                        if (sTypeDef instanceof SClassDef) {
                                SClassDef sClassDef = (SClassDef) sTypeDef;
                                if (sClassDef.classType() == SClassDef.FUN) {
                                        SAnno latteFun = new SAnno();
                                        latteFun.setAnnoDef((SAnnoDef) getTypeWithName("lt.runtime.LatteFun", LineCol.SYNTHETIC));
                                        latteFun.setPresent(sClassDef);
                                        sClassDef.annos().add(latteFun);
                                } else if (sClassDef.classType() == SClassDef.OBJECT) {
                                        ASTGHolder<ObjectDef> objectHolder = originalObjects.get(sClassDef.fullName());
                                        ObjectDef objectDef = objectHolder.s;

                                        // present annotation
                                        SAnno objectAnno = new SAnno();
                                        objectAnno.setAnnoDef((SAnnoDef) getTypeWithName("lt.runtime.LatteObject", LineCol.SYNTHETIC));
                                        objectAnno.setPresent(sClassDef);
                                        sClassDef.annos().add(objectAnno);

                                        parseClassDefInfo(sClassDef, objectDef.superWithInvocation,
                                                objectDef.superWithoutInvocation, imports, objectDef.line_col());

                                        // modifiers
                                        boolean isImplicit = false;
                                        for (Modifier m : objectDef.modifiers) {
                                                switch (m.modifier) {
                                                        case IMPLICIT:
                                                                isImplicit = true;
                                                                break;
                                                        default:
                                                                err.UnexpectedTokenException("valid modifier (implicit)", m.toString(), m.line_col());
                                                                return;
                                                }
                                        }

                                        // annos
                                        parseAnnos(objectDef.annos, sClassDef, imports, ElementType.TYPE,
                                                Collections.singletonList(ElementType.CONSTRUCTOR));

                                        if (isImplicit) {
                                                checkAndAddImplicitAnno(sClassDef);
                                        }

                                        // build constructor
                                        SConstructorDef constructor = new SConstructorDef(LineCol.SYNTHETIC);
                                        constructor.setDeclaringType(sClassDef);

                                        // annotation
                                        parseAnnos(objectDef.annos, constructor, imports, ElementType.CONSTRUCTOR,
                                                Collections.singletonList(ElementType.TYPE));

                                        // modifier
                                        constructor.modifiers().add(SModifier.PRIVATE);

                                        // declaring
                                        constructor.setDeclaringType(sClassDef);
                                        sClassDef.constructors().add(constructor);

                                        // only add once, for that all types under one template share the same ast structure
                                        if (objectDef.statements.isEmpty() || !(objectDef.statements.get(0) instanceof AST.StaticScope)) {
                                                // static public val singletonInstance = XXX
                                                VariableDef v = new VariableDef(CompileUtil.SingletonFieldName,
                                                        new HashSet<Modifier>(Arrays.asList(
                                                                new Modifier(Modifier.Available.VAL, LineCol.SYNTHETIC),
                                                                new Modifier(Modifier.Available.PUBLIC, LineCol.SYNTHETIC)
                                                        )), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC);
                                                AST.Access vType = new AST.Access(pkg.isEmpty() ? null : new AST.PackageRef(pkg, LineCol.SYNTHETIC),
                                                        objectDef.name, LineCol.SYNTHETIC);
                                                vType.generics.addAll(objectDef.generics);
                                                v.setType(vType);
                                                objectDef.statements.add(0, new AST.StaticScope(Collections.<Statement>singletonList(v),
                                                        LineCol.SYNTHETIC));

                                        }
                                        // fields methods
                                        parseFieldsAndMethodsForClass(sClassDef, objectDef.statements, imports);
                                } else if (sClassDef.classType() == SClassDef.NORMAL) {
                                        ASTGHolder<ClassDef> classHolder = originalClasses.get(sClassDef.fullName());
                                        ClassDef classDef = classHolder.s;

                                        parseClassDefInfo(sClassDef, classDef.superWithInvocation,
                                                classDef.superWithoutInvocation, imports, classDef.line_col());

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

                                                constructor.setDeclaringType(sClassDef);
                                                sClassDef.constructors().add(constructor);

                                                // constructor should be filled with
                                                // parameters|declaringType(class)|modifiers
                                                // in this step

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
                                                                        err.UnexpectedTokenException("valid constructor modifier (public|private|protected|internal)", m.toString(), m.line_col());
                                                                        return;
                                                        }
                                                }

                                                // parameters
                                                parseParameters(classDef.params, i, constructor, imports, true);

                                                if (lastConstructor != null) {
                                                        // record the constructor and expressions
                                                        Map<SInvokable, Expression> invoke = new HashMap<SInvokable, Expression>();
                                                        invoke.put(lastConstructor, classDef.params.get(i).getInit());
                                                        defaultParamInvokable.put(constructor, invoke);
                                                }
                                                lastConstructor = constructor;
                                        }
                                        // constructor finished

                                        // fields and methods
                                        // parse field from constructor parameters
                                        for (VariableDef v : classDef.params) {
                                                parseField(v, sClassDef, imports, PARSING_CLASS, false, true);
                                        }
                                        parseFieldsAndMethodsForClass(sClassDef, classDef.statements, imports);
                                } else {
                                        throw new LtBug("unknown sClassDef.classType(): " + sClassDef.classType());
                                }
                        } else if (sTypeDef instanceof SInterfaceDef) {
                                SInterfaceDef sInterfaceDef = (SInterfaceDef) sTypeDef;
                                ASTGHolder<InterfaceDef> interfaceHolder = originalInterfaces.get(sInterfaceDef.fullName());
                                InterfaceDef interfaceDef = interfaceHolder.s;

                                // parse super interfaces
                                for (AST.Access access : interfaceDef.superInterfaces) {
                                        SInterfaceDef superInterface = (SInterfaceDef) getTypeWithAccess(access, getGenericMap(sInterfaceDef), imports);
                                        sInterfaceDef.superInterfaces().add(superInterface);
                                }

                                // parse annos
                                parseAnnos(interfaceDef.annos, sInterfaceDef, imports, ElementType.TYPE, Collections.<ElementType>emptyList());

                                // parse fields and methods
                                List<AST.StaticScope> staticScopes = new ArrayList<AST.StaticScope>();
                                for (Statement stmt : interfaceDef.statements) {
                                        if (stmt instanceof VariableDef) {
                                                parseField((VariableDef) stmt, sInterfaceDef, imports, PARSING_INTERFACE, false, false);
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
                                        } else if (stmt instanceof AST.Destruct) {
                                                parseFieldsFromDestruct((AST.Destruct) stmt, sInterfaceDef, true);
                                        } else {
                                                err.SyntaxException("interfaces don't have initiators", stmt.line_col());
                                                return;
                                        }
                                }
                                for (AST.StaticScope staticScope : staticScopes) {
                                        for (Statement stmt : staticScope.statements) {
                                                if (stmt instanceof VariableDef) {
                                                        parseField((VariableDef) stmt, sInterfaceDef, imports, PARSING_INTERFACE, true, false);
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
                                                } else if (stmt instanceof AST.Destruct) {
                                                        parseFieldsFromDestruct((AST.Destruct) stmt, sInterfaceDef, true);
                                                } else {
                                                        err.SyntaxException("interfaces don't have initiators", stmt.line_col());
                                                        return;
                                                }
                                        }
                                }
                        } else if (sTypeDef instanceof SAnnoDef) {
                                SAnnoDef sAnnoDef = (SAnnoDef) sTypeDef;
                                ASTGHolder<AnnotationDef> annoHolder = originalAnnotations.get(sAnnoDef.fullName());
                                AnnotationDef annotationDef = annoHolder.s;

                                // annos
                                parseAnnos(annotationDef.annos, sAnnoDef, imports, ElementType.ANNOTATION_TYPE,
                                        Collections.<ElementType>emptyList());

                                // annotation fields
                                parseAnnotationFields(sAnnoDef, annotationDef.stmts, imports);
                        } else {
                                throw new LtBug("unknown STypeDef " + sTypeDef);
                        }
                }
        }

        private void assertToBeAnnotationField(STypeDef type) throws SyntaxException {
                if (type instanceof PrimitiveTypeDef) return;
                if (type.fullName().equals("java.lang.String")) return;
                if (type.fullName().equals("java.lang.Class")) return;
                if (getTypeWithName(Enum.class.getName(), LineCol.SYNTHETIC).isAssignableFrom(type)) return;
                if (type instanceof SArrayTypeDef && ((SArrayTypeDef) type).dimension() == 1) {
                        assertToBeAnnotationField(((SArrayTypeDef) type).type());
                        return;
                }
                if (type instanceof SAnnoDef) return;
                err.SyntaxException(type + " cannot be type of annotation fields", type.line_col());
        }

        /**
         * fill annotation fields into the annotation, but values are not parsed
         *
         * @param sAnnoDef the annotation def
         * @param stmts    statements in annotation
         * @param imports  imports
         * @throws SyntaxException compiling error
         */
        public void parseAnnotationFields(SAnnoDef sAnnoDef, List<Statement> stmts, List<Import> imports) throws SyntaxException {
                for (Statement stmt : stmts) {
                        if (stmt instanceof VariableDef) {
                                VariableDef v = (VariableDef) stmt;
                                if (!v.getModifiers().isEmpty()) {
                                        err.SyntaxException("modifiers cannot present on annotation fields", v.line_col());
                                }
                                if (!v.getAnnos().isEmpty()) {
                                        err.SyntaxException("annotations cannot present on annotation fields", v.line_col());
                                }
                                if (v.getType() == null) {
                                        err.SyntaxException("annotation fields should have a type", v.line_col());
                                }
                                STypeDef type = getTypeWithAccess(v.getType(), getGenericMap(sAnnoDef), imports);
                                assertToBeAnnotationField(type);
                                SAnnoField f = new SAnnoField();
                                f.setName(v.getName());
                                f.setType(type);
                                f.setDeclaringType(sAnnoDef);
                                sAnnoDef.annoFields().add(f);
                        } else {
                                err.SyntaxException("only variable definition can exist in annotation", stmt.line_col());
                        }
                }
        }

        private void checkAndAddImplicitAnno(SAnnotationPresentable annoPresentable) throws SyntaxException {
                // check implicit annotation
                boolean hasImplicitAnno = false;
                for (SAnno sAnno : annoPresentable.annos()) {
                        if (sAnno.type().fullName().equals("lt.runtime.Implicit")) {
                                hasImplicitAnno = true;
                                break;
                        }
                }
                if (!hasImplicitAnno) {
                        SAnno ImplicitAnno = new SAnno();
                        ImplicitAnno.setAnnoDef((SAnnoDef) getTypeWithName("lt.runtime.Implicit", LineCol.SYNTHETIC));
                        ImplicitAnno.setPresent(annoPresentable);
                        annoPresentable.annos().add(ImplicitAnno);
                }
        }

        /**
         * parse class info
         *
         * @param sClassDef              SClassDef object
         * @param superWithInvocation    :???(...)
         * @param superWithoutInvocation :???
         * @param imports                imports
         * @param lineCol                lineCol
         * @throws SyntaxException compiling error
         */
        private void parseClassDefInfo(SClassDef sClassDef,
                                       AST.Invocation superWithInvocation,
                                       List<AST.Access> superWithoutInvocation,
                                       List<Import> imports,
                                       LineCol lineCol) throws SyntaxException {
                // generic type map
                Map<String, STypeDef> genericTypeMap = getGenericMap(sClassDef);
                // parse parent
                Iterator<AST.Access> superWithoutInvocationAccess;
                if (superWithInvocation == null) {
                        if (superWithoutInvocation.isEmpty()) {
                                // no interfaces, no parent class
                                sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", lineCol));
                                superWithoutInvocationAccess = null;
                        } else {
                                superWithoutInvocationAccess = superWithoutInvocation.iterator();
                                AST.Access mightBeClassAccess = superWithoutInvocationAccess.next();
                                STypeDef tmp = getTypeWithAccess(mightBeClassAccess, genericTypeMap, imports);
                                if (tmp instanceof SClassDef) {
                                        // constructor without constructor invocation
                                        sClassDef.setParent((SClassDef) tmp);
                                } else if (tmp instanceof SInterfaceDef) {
                                        // interface
                                        sClassDef.superInterfaces().add((SInterfaceDef) tmp);
                                        // set java.lang.Object as super class
                                        sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", lineCol));
                                } else {
                                        err.SyntaxException(mightBeClassAccess.toString() + " is not class or interface",
                                                mightBeClassAccess.line_col());
                                        return;
                                }
                        }
                } else {
                        // super class
                        if (!(superWithInvocation.exp instanceof AST.Access)) {
                                throw new LtBug("classDef.superWithInvocation.exp should always be AST.Access");
                        }

                        AST.Access access = (AST.Access) superWithInvocation.exp;
                        STypeDef tmp = getTypeWithAccess(access, genericTypeMap, imports);
                        if (tmp instanceof SClassDef) {
                                sClassDef.setParent((SClassDef) tmp);
                        } else {
                                err.SyntaxException(access.toString() + " is not class or interface",
                                        access.line_col());
                                return;
                        }
                        superWithoutInvocationAccess = superWithoutInvocation.iterator();
                }
                // interfaces to be parsed
                while (superWithoutInvocationAccess != null && superWithoutInvocationAccess.hasNext()) {
                        AST.Access interfaceAccess = superWithoutInvocationAccess.next();
                        STypeDef tmp = getTypeWithAccess(interfaceAccess, getGenericMap(sClassDef), imports);
                        if (tmp instanceof SInterfaceDef) {
                                sClassDef.superInterfaces().add((SInterfaceDef) tmp);
                        } else {
                                err.SyntaxException(interfaceAccess.toString() + " is not interface",
                                        interfaceAccess.line_col());
                                return;
                        }
                }
        }

        /**
         * parse fields and methods for class
         *
         * @param sClassDef  SClassDef object
         * @param statements statements
         * @param imports    imports
         * @throws SyntaxException compiling error
         */
        private void parseFieldsAndMethodsForClass(SClassDef sClassDef,
                                                   List<Statement> statements,
                                                   List<Import> imports) throws SyntaxException {
                // get static scope and parse non-static fields/methods
                List<AST.StaticScope> staticScopes = new ArrayList<AST.StaticScope>();
                for (Statement stmt : statements) {
                        if (stmt instanceof AST.StaticScope) {
                                staticScopes.add((AST.StaticScope) stmt);
                        } else if (stmt instanceof VariableDef) {
                                // define a non-static field
                                parseField((VariableDef) stmt, sClassDef, imports, PARSING_CLASS, false, false);
                        } else if (stmt instanceof MethodDef) {
                                // define a non-static method
                                MethodDef methodDef = (MethodDef) stmt;
                                int generateIndex = -1;
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
                        } else if (stmt instanceof AST.Destruct) {
                                parseFieldsFromDestruct((AST.Destruct) stmt, sClassDef, false);
                        }
                }
                // get static field and methods
                for (AST.StaticScope scope : staticScopes) {
                        for (Statement stmt : scope.statements) {
                                if (stmt instanceof VariableDef) {
                                        // define a static field
                                        parseField((VariableDef) stmt, sClassDef, imports, PARSING_CLASS, true, false);
                                } else if (stmt instanceof MethodDef) {
                                        // define a static method
                                        MethodDef methodDef = (MethodDef) stmt;
                                        int generateIndex = -1;
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
                                } else if (stmt instanceof AST.Destruct) {
                                        parseFieldsFromDestruct((AST.Destruct) stmt, sClassDef, true);
                                }
                        }
                }
        }

        /**
         * ========step 3========
         * validation
         * <p>
         * check circular inheritance
         * check method override
         * check method signature
         * check annotations
         *
         * @throws SyntaxException exception
         */
        public void step3() throws SyntaxException {
                for (STypeDef sTypeDef : typeDefSet) {
                        if (isGenericTemplateType(sTypeDef)) continue;
                        if (sTypeDef instanceof SClassDef) {
                                List<STypeDef> circularRecorder = new ArrayList<STypeDef>();
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
                                checkInterfaceCircularInheritance(i, i.superInterfaces(), new ArrayList<SInterfaceDef>());
                        } else if (!(sTypeDef instanceof SAnnoDef)) {
                                throw new LtBug("wrong STypeDefType " + sTypeDef.getClass());
                        }
                }
                // check override and overload with super methods
                for (STypeDef sTypeDef : typeDefSet) {
                        if (isGenericTemplateType(sTypeDef)) continue;
                        if (sTypeDef instanceof SAnnoDef) continue;
                        checkOverrideAllMethods(sTypeDef);
                }

                // after the override check are done, try to get signatures of functions.
                for (STypeDef sTypeDef : typeDefSet) {
                        if (isGenericTemplateType(sTypeDef)) continue;
                        if (sTypeDef instanceof SClassDef) {
                                SClassDef sClassDef = (SClassDef) sTypeDef;
                                if (sClassDef.classType() != SClassDef.FUN) {
                                        continue;
                                }

                                ASTGHolder<FunDef> funHolder = originalFunctions.get(sClassDef.fullName());
                                FunDef fun = funHolder.s;
                                List<Import> imports = fileNameToImport.get(fun.line_col().fileName);

                                // get super class/interface
                                STypeDef type = getTypeWithAccess(fun.superType, getGenericMap(sTypeDef), imports);
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
                                method.setReturnType(
                                        getRealReturnType(
                                                methodToOverride[0].getReturnType(), true));
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

                // check annotation (@FunctionalInterface @FunctionalAbstractClass @Override @Implicit)
                for (STypeDef typeDef : typeDefSet) {
                        // annotation would never be generic
                        // no need to check
                        // if (isGenericTemplateType(typeDef)) continue;
                        if (typeDef instanceof SAnnoDef) continue;
                        for (SAnno anno : typeDef.annos()) {
                                if (anno.type().fullName().equals("java.lang.FunctionalInterface")
                                        || anno.type().fullName().equals("lt.lang.FunctionalInterface")) {
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
                                } else if (anno.type().fullName().equals("lt.runtime.Implicit")) {
                                        if (typeDef instanceof SClassDef) {
                                                boolean isObject = false;
                                                for (SAnno a : typeDef.annos()) {
                                                        if (a.type().fullName().equals("lt.runtime.LatteObject")) {
                                                                isObject = true;
                                                                break;
                                                        }
                                                }
                                                if (isObject) {
                                                        // check methods
                                                        for (SMethodDef m : ((SClassDef) typeDef).methods()) {
                                                                for (SAnno a : m.annos()) {
                                                                        if (a.type().fullName().equals("lt.runtime.Implicit")) {
                                                                                if (m.getParameters().size() != 1) {
                                                                                        err.SyntaxException("implicit methods should contain only one param", m.line_col());
                                                                                }
                                                                                if (m.getReturnType().equals(VoidType.get())) {
                                                                                        err.SyntaxException("implicit method's return type should not be Unit", m.line_col());
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                } else {
                                                        err.SyntaxException("implicit should only exist on object classes", typeDef.line_col());
                                                }
                                        } else {
                                                err.SyntaxException("implicit should only exist on object classes", typeDef.line_col());
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
                        if (isGenericTemplateType(typeDef)) continue;
                        if (typeDef instanceof SClassDef) {
                                SClassDef cls = (SClassDef) typeDef;
                                if (cls.isDataClass()) {
                                        fillMethodsIntoDataClass(cls);
                                }
                        }
                }
        }

        private void checkAndFillAnnotations() throws SyntaxException {
                // not compiled annotations
                for (STypeDef typeDef : typeDefSet) {
                        if (typeDef instanceof SAnnoDef) {
                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                ASTGHolder<AnnotationDef> holder = originalAnnotations.get(annoDef.fullName());
                                AnnotationDef astAnnoDef = holder.s;
                                for (SAnnoField f : annoDef.annoFields()) {
                                        for (Statement stmt : astAnnoDef.stmts) {
                                                if (stmt instanceof VariableDef
                                                        &&
                                                        ((VariableDef) stmt).getName().equals(f.name())) {

                                                        Expression exp = ((VariableDef) stmt).getInit();
                                                        if (exp != null) {
                                                                // do fill
                                                                f.doesHaveDefaultValue();
                                                        }
                                                }
                                        }
                                }
                        }
                }
                // compiled annotations
                // fill the values directly
                for (STypeDef typeDef : types.values()) {
                        if (typeDef instanceof SAnnoDef) {
                                boolean isCompiledAnnotation = true;
                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                Class<?> cls = null;
                                try {
                                        cls = loadClass(annoDef.fullName());
                                } catch (ClassNotFoundException e) {
                                        isCompiledAnnotation = false;
                                }
                                // parse field default values
                                if (isCompiledAnnotation) {
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
                                }
                        }
                }
                // fill true value to not compiled annotations
                for (STypeDef typeDef : typeDefSet) {
                        if (typeDef instanceof SAnnoDef) {
                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                ASTGHolder<AnnotationDef> holder = originalAnnotations.get(annoDef.fullName());
                                AnnotationDef astAnnoDef = holder.s;
                                for (SAnnoField f : annoDef.annoFields()) {
                                        for (Statement stmt : astAnnoDef.stmts) {
                                                if (stmt instanceof VariableDef
                                                        &&
                                                        ((VariableDef) stmt).getName().equals(f.name())) {

                                                        Expression exp = ((VariableDef) stmt).getInit();
                                                        if (exp != null) {
                                                                Value value = transformIntoAnnoValidValue(
                                                                        parseValueFromExpression(exp, f.type(), null),
                                                                        exp.line_col());
                                                                f.setDefaultValue(value);
                                                        }
                                                }
                                        }
                                }
                        }
                }
                // parse annotations presented on this type
                for (STypeDef typeDef : types.values()) {
                        if (typeDef instanceof SAnnoDef) {
                                SAnnoDef annoDef = (SAnnoDef) typeDef;
                                parseAnnoValues(annoDef.annos());
                        }
                }
        }

        private Value transformIntoAnnoValidValue(Value value, LineCol lineCol) throws SyntaxException {
                if (value.type() instanceof PrimitiveTypeDef) return value;
                if (value instanceof StringConstantValue) return value;
                if (value instanceof Ins.GetClass) return value;
                if (value instanceof SAnno) return value;
                if (value instanceof Ins.GetStatic) {
                        // enum
                        EnumValue enumValue = new EnumValue();
                        enumValue.setType(((Ins.GetStatic) value).field().declaringType());
                        enumValue.setEnumStr(((Ins.GetStatic) value).field().name());
                        return enumValue;
                }
                if (value instanceof Ins.NewArray) {
                        // array
                        SArrayValue sArrayValue = new SArrayValue();
                        sArrayValue.setDimension(1);
                        List<Value> values = ((Ins.NewArray) value).initValues();
                        sArrayValue.setValues(values.toArray(new Value[values.size()]));
                        sArrayValue.setType((SArrayTypeDef) value.type());
                        return sArrayValue;
                }
                if (value instanceof Ins.ANewArray) {
                        // ref array
                        SArrayValue sArrayValue = new SArrayValue();
                        sArrayValue.setDimension(1);
                        List<Value> values = ((Ins.ANewArray) value).initValues();
                        sArrayValue.setValues(values.toArray(new Value[values.size()]));
                        sArrayValue.setType((SArrayTypeDef) value.type());
                        return sArrayValue;
                }
                err.SyntaxException("cannot resolve valid value for annotation field", lineCol);
                return null;
        }

        /**
         * ========step 4========
         * first parse anno types
         * the annotations presented on these anno types will also be parsed
         *
         * @throws SyntaxException exception
         */
        public void step4() throws SyntaxException {
                checkAndFillAnnotations();
                // then
                // foreach typeDefSet, parse their statements
                List<STypeDef> typeDefList = new ArrayList<STypeDef>(typeDefSet);
                for (STypeDef sTypeDef : typeDefList) {
                        if (isGenericTemplateType(sTypeDef)) {
                                typeDefSet.remove(sTypeDef);
                                generateGenericTemplateClass(sTypeDef);
                                continue;
                        }
                        if (sTypeDef instanceof SClassDef) {
                                SClassDef sClassDef = (SClassDef) sTypeDef;
                                ASTGHolder<ClassDef> classHolder = originalClasses.get(sClassDef.fullName());
                                ASTGHolder<ObjectDef> objectHolder = originalObjects.get(sClassDef.fullName());
                                ClassDef astClass = (null == classHolder) ? null : classHolder.s;
                                ObjectDef astObject = (null == objectHolder) ? null : objectHolder.s;

                                parseAnnoValues(sClassDef.annos());

                                // initiate the type scope
                                SemanticScope scope = new SemanticScope(sTypeDef, null);

                                // parse constructors
                                for (SConstructorDef constructorToFillStatements : sClassDef.constructors()) {
                                        // if is not empty then continue
                                        if (!constructorToFillStatements.statements().isEmpty())
                                                continue;
                                        // initiate constructor scope
                                        SemanticScope constructorScope = new SemanticScope(scope, constructorToFillStatements.meta());
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

                                                assert (astClass == null && astObject != null) || (astClass != null && astObject == null);

                                                AST.Invocation superWithInvocation = (
                                                        astClass == null) ? astObject.superWithInvocation
                                                        : astClass.superWithInvocation;

                                                if (null == superWithInvocation) {
                                                        // invoke super();
                                                        for (SConstructorDef cons : parent.constructors()) {
                                                                if (cons.getParameters().size() == 0) {
                                                                        invokeConstructor = new Ins.InvokeSpecial(new Ins.This(sClassDef), cons,
                                                                                sClassDef.line_col());
                                                                        break;
                                                                }
                                                        }
                                                } else {
                                                        // invoke super with args
                                                        for (SConstructorDef cons : parent.constructors()) {
                                                                if (cons.getParameters().size() == superWithInvocation.args.size()) {
                                                                        invokeConstructor = new Ins.InvokeSpecial(new Ins.This(sClassDef), cons,
                                                                                superWithInvocation.line_col());

                                                                        List<SParameter> parameters = cons.getParameters();
                                                                        List<Expression> args = superWithInvocation.args;
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
                                                constructorScope = new SemanticScope(scope, constructorToFillStatements.meta());
                                                constructorScope.setThis(new Ins.This(sTypeDef)); // set `this`
                                                for (SParameter param : constructorToFillStatements.getParameters()) {
                                                        constructorScope.putLeftValue(constructorScope.generateTempName(), param);
                                                }

                                                paramValueAvaliable(constructorToFillStatements.getParameters(),
                                                        constructorToFillStatements.statements(), constructorScope,
                                                        constructorToFillStatements.line_col());

                                                // parse this constructor
                                                List<Statement> statements = (
                                                        astClass == null) ? astObject.statements
                                                        : astClass.statements;
                                                for (Statement stmt : statements) {
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

                                // if not function
                                if (sClassDef.classType() != SClassDef.FUN) {
                                        assert (astClass == null && astObject != null) || (astClass != null && astObject == null);

                                        List<Statement> statements = (
                                                astClass == null) ? astObject.statements
                                                : astClass.statements;
                                        // parse static
                                        SemanticScope staticScope = new SemanticScope(scope, sClassDef.staticMeta());

                                        if (sClassDef.classType() == SClassDef.OBJECT) {
                                                SFieldDef singletonInstanceField = null;
                                                for (SFieldDef f : sClassDef.fields()) {
                                                        if (f.name().equals(CompileUtil.SingletonFieldName)) {
                                                                singletonInstanceField = f;
                                                                break;
                                                        }
                                                }
                                                if (singletonInstanceField == null)
                                                        throw new LtBug("object class should have field " + CompileUtil.SingletonFieldName);
                                                Ins.New aNew = new Ins.New(
                                                        sClassDef.constructors().get(0), LineCol.SYNTHETIC
                                                );
                                                Ins.PutStatic ps = new Ins.PutStatic(singletonInstanceField,
                                                        aNew, LineCol.SYNTHETIC, err);
                                                sClassDef.staticStatements().add(ps);
                                        }

                                        for (Statement statement : statements) {
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
                                ASTGHolder<InterfaceDef> holder = originalInterfaces.get(sInterfaceDef.fullName());
                                InterfaceDef astInterface = holder.s;

                                parseAnnoValues(sInterfaceDef.annos());

                                SemanticScope scope = new SemanticScope(sInterfaceDef, null);

                                // parse method
                                // use traditional for loop because the method list might be modified
                                int methodSize = sInterfaceDef.methods().size();
                                List<SMethodDef> methods = sInterfaceDef.methods();
                                for (int i = 0; i < methodSize; ++i) {
                                        SMethodDef method = methods.get(i);
                                        parseMethod(method, methodToStatements.get(method), scope);
                                }

                                // parse static
                                SemanticScope staticScope = new SemanticScope(scope, sInterfaceDef.staticMeta());
                                for (Statement statement : astInterface.statements) {
                                        if (statement instanceof AST.StaticScope) {
                                                for (Statement statementInStatic : ((AST.StaticScope) statement).statements) {
                                                        parseStatement(
                                                                statementInStatic,
                                                                VoidType.get(),
                                                                staticScope,
                                                                sInterfaceDef.staticStatements(),
                                                                sInterfaceDef.staticExceptionTable(),
                                                                null, null,
                                                                true);
                                                }
                                        } else {
                                                parseStatement(
                                                        statement,
                                                        VoidType.get(),
                                                        staticScope,
                                                        sInterfaceDef.staticStatements(),
                                                        sInterfaceDef.staticExceptionTable(),
                                                        null, null,
                                                        true);
                                        }
                                }
                        } else if (!(sTypeDef instanceof SAnnoDef)) {
                                throw new LtBug("wrong STypeDefType " + sTypeDef.getClass());
                        }
                }
        }

        private void generateGenericTemplateClass(STypeDef sTypeDef) throws SyntaxException {
                Definition defi;
                if (sTypeDef instanceof SClassDef) {
                        switch (((SClassDef) sTypeDef).classType()) {
                                case SClassDef.NORMAL:
                                        defi = originalClasses.get(sTypeDef.fullName()).s;
                                        break;
                                case SClassDef.OBJECT:
                                        defi = originalObjects.get(sTypeDef.fullName()).s;
                                        break;
                                case SClassDef.FUN:
                                        defi = originalFunctions.get(sTypeDef.fullName()).s;
                                        break;
                                default:
                                        throw new LtBug("unknown class type: " + ((SClassDef) sTypeDef).classType());
                        }
                } else if (sTypeDef instanceof SInterfaceDef) {
                        defi = originalInterfaces.get(sTypeDef.fullName()).s;
                } else if (sTypeDef instanceof SAnnoDef) {
                        defi = originalInterfaces.get(sTypeDef.fullName()).s;
                } else {
                        throw new LtBug("unknown sTypeDef " + sTypeDef);
                }
                String str = serializeObjectToString(defi);

                // class
                SClassDef c = new SClassDef(SClassDef.NORMAL, LineCol.SYNTHETIC);
                c.setFullName(sTypeDef.fullName());
                c.modifiers().add(SModifier.PUBLIC);
                // field
                SFieldDef f = new SFieldDef(LineCol.SYNTHETIC);
                f.setName(Consts.AST_FIELD);
                f.setType(getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                f.modifiers().add(SModifier.PUBLIC);
                f.modifiers().add(SModifier.STATIC);
                c.fields().add(f);
                f.setDeclaringType(c);
                c.staticStatements().add(new Ins.PutStatic(f, new StringConstantValue(str), LineCol.SYNTHETIC, err));
                f.alreadyAssigned();
                // anno
                SAnnoDef aDef = (SAnnoDef) getTypeWithName("lt.lang.GenericTemplate", LineCol.SYNTHETIC);
                SAnno a = new SAnno();
                a.setAnnoDef(aDef);
                c.annos().add(a);
                a.setPresent(c);

                typeDefSet.add(c);
        }

        /**
         * check whether overrides all methods from super
         *
         * @param sTypeDef sTypeDef
         * @throws SyntaxException exception
         */
        public void checkOverrideAllMethods(STypeDef sTypeDef) throws SyntaxException {
                checkOverride(sTypeDef);

                // check whether overrides all methods from super
                if (sTypeDef instanceof SClassDef) {
                        SClassDef c = (SClassDef) sTypeDef;
                        if (c.modifiers().contains(SModifier.ABSTRACT)) return;

                        // check all abstract methods
                        // record them
                        List<SMethodDef> abstractMethods = new ArrayList<SMethodDef>();
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
        public void fillDefaultParamMethod(SInvokable invokable, SemanticScope scope) throws SyntaxException {
                Map<SInvokable, Expression> invokePair = defaultParamInvokable.get(invokable);
                SInvokable methodToInvoke = invokePair.keySet().iterator().next();
                Expression arg = invokePair.get(methodToInvoke);
                if (invokable instanceof SConstructorDef) {
                        // invoke another constructor
                        Ins.InvokeSpecial invoke = new Ins.InvokeSpecial(scope.getThis(), methodToInvoke, LineCol.SYNTHETIC);
                        for (SParameter p : invokable.getParameters()) {
                                invoke.arguments().add(new Ins.TLoad(p, scope, LineCol.SYNTHETIC));
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
                                invoke.arguments().add(new Ins.TLoad(methodDef.getParameters().get(ii), scope, LineCol.SYNTHETIC));
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
         * and generate unapply
         *
         * @param cls the class should be data class
         * @throws SyntaxException compiling error
         */
        public void fillMethodsIntoDataClass(SClassDef cls) throws SyntaxException {
                // check parameter modifiers
                // cannot be `val`
                for (SParameter p : cls.constructors().get(0).getParameters()) {
                        if (!p.canChange()) {
                                err.SyntaxException("data class cannot have `val` parameters", cls.line_col());
                                return;
                        }
                }

                // implement Cloneable and Serializable
                cls.superInterfaces().add((SInterfaceDef) getTypeWithName(Cloneable.class.getName(), LineCol.SYNTHETIC));
                cls.superInterfaces().add((SInterfaceDef) getTypeWithName(Serializable.class.getName(), LineCol.SYNTHETIC));
                // add clone()=super.clone()
                SMethodDef methodClone = new SMethodDef(LineCol.SYNTHETIC);
                methodClone.setName("clone");
                methodClone.setDeclaringType(cls);
                cls.methods().add(methodClone);
                methodClone.setReturnType(getObject_Class());
                methodClone.modifiers().add(SModifier.PUBLIC);
                SMethodDef Object_clone = null;
                for (SMethodDef m : getObject_Class().methods()) {
                        if (m.name().equals("clone")) {
                                Object_clone = m;
                                break;
                        }
                }
                assert Object_clone != null;
                Ins.InvokeSpecial invokeObjectClone = new Ins.InvokeSpecial(new Ins.This(cls), Object_clone, LineCol.SYNTHETIC);
                methodClone.statements().add(new Ins.TReturn(invokeObjectClone, LineCol.SYNTHETIC));

                Map<SFieldDef, SMethodDef> setters = new HashMap<SFieldDef, SMethodDef>();
                Map<SFieldDef, SMethodDef> getters = new HashMap<SFieldDef, SMethodDef>();
                SMethodDef toStringOverride = null;
                SMethodDef equalsOverride = null;
                SMethodDef hashCodeOverride = null;
                SMethodDef unapply = null;
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

                // get existing equals(o)/toString()/hashCode()/unapply(?)
                for (SMethodDef m : cls.methods()) {
                        if (m.name().equals("toString") && m.getParameters().size() == 0) toStringOverride = m;
                        if (m.name().equals("equals")
                                && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().fullName().equals("java.lang.Object"))
                                equalsOverride = m;
                        if (m.name().equals("hashCode") && m.getParameters().size() == 0) hashCodeOverride = m;
                        if (m.name().equals("unapply") && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().isAssignableFrom(cls)) unapply = m;
                }

                // get existing zero param constructor
                for (SConstructorDef con : cls.constructors()) {
                        if (con.getParameters().isEmpty()) {
                                zeroParamCons = con;
                                break;
                        }
                }

                SemanticScope scope = new SemanticScope(cls, null);
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
                                new SemanticScope(scope, toStringOverride.meta()),
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
                                                                new AST.PackageRef("lt::runtime", lineCol),
                                                                "LtRuntime",
                                                                lineCol
                                                        ),
                                                        "getHashCode",
                                                        lineCol
                                                ),
                                                Collections.<Expression>singletonList(
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
                                new SemanticScope(scope, hashCodeOverride.meta()),
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
                        SemanticScope equalsScope = new SemanticScope(scope, equalsOverride.meta());
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
                        List<Value> initValues = new ArrayList<Value>();
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
                                parseStatement(stmt, getter.getReturnType(), new SemanticScope(scope, getter.meta()), getter.statements(),
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

                                SemanticScope setterScope = new SemanticScope(scope, setter.meta());
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
                if (unapply == null) {
                        unapply = new SMethodDef(LineCol.SYNTHETIC);
                        unapply.setName("unapply");
                        unapply.setDeclaringType(cls);
                        unapply.setReturnType(getTypeWithName("java.util.List", LineCol.SYNTHETIC));
                        unapply.modifiers().add(SModifier.PUBLIC);
                        unapply.modifiers().add(SModifier.STATIC);
                        cls.methods().add(unapply);

                        SParameter p = new SParameter();
                        p.setTarget(unapply);
                        p.setName("o");
                        p.setType(cls);
                        unapply.getParameters().add(p);

                        // static { unapply }
                        SemanticScope unapplyScope = new SemanticScope(new SemanticScope(cls, null), unapply.meta());
                        unapplyScope.putLeftValue("o", p);

                        List<Expression> tmpList = new ArrayList<Expression>();
                        for (SFieldDef f : cls.fields()) {
                                tmpList.add(new AST.Access(
                                        // o.f
                                        new AST.Access(null, "o", LineCol.SYNTHETIC),
                                        f.name(), LineCol.SYNTHETIC
                                ));
                        }
                        AST.Return ret = new AST.Return(
                                new AST.ArrayExp(tmpList, LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC
                        );
                        parseStatement(ret, unapply.getReturnType(), unapplyScope, unapply.statements(),
                                unapply.exceptionTables(), null, null, false);
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
        public void parseMethod(SMethodDef methodDef, List<Statement> statements, SemanticScope superScope) throws SyntaxException {
                if (!methodDef.statements().isEmpty()) return;
                if (methodDef.modifiers().contains(SModifier.ABSTRACT)) {
                        if (!statements.isEmpty()) {
                                err.SyntaxException("abstract method cannot contain statements", statements.get(0).line_col());
                                return;
                        }
                        return;
                }
                if (methodDef.declaringType() instanceof SInterfaceDef) {
                        if (!statements.isEmpty()) {
                                err.SyntaxException("default methods are not supported in interfaces", statements.get(0).line_col());
                                return;
                        }
                }
                if (methodDef.modifiers().contains(SModifier.STATIC) && methodDef.declaringType() instanceof SInterfaceDef) {
                        err.SyntaxException("static methods are not allowed in interfaces", methodDef.line_col());
                        return;
                }

                // fill in return
                if (!methodDef.getReturnType().equals(VoidType.get())) {
                        transformLastExpToReturn(statements);
                }

                SemanticScope scope = new SemanticScope(superScope, methodDef.meta());
                if (!methodDef.modifiers().contains(SModifier.STATIC)) {
                        scope.setThis(new Ins.This(scope.type()));
                }
                for (SParameter p : methodDef.getParameters()) {
                        if (p.canChange() && !isPointerType(p.type()) && CompileUtil.isValidName(p.name())) {
                                scope.putLeftValue(scope.generateTempName(), p);
                        } else {
                                scope.putLeftValue(p.name(), p);
                        }
                }

                if (defaultParamInvokable.containsKey(methodDef)) {
                        fillDefaultParamMethod(methodDef, scope);
                } else {
                        paramValueAvaliable(methodDef.getParameters(), methodDef.statements(), scope, methodDef.line_col());
                        for (SParameter p : methodDef.getParameters()) {
                                if (p.canChange() && !isPointerType(p.type()) && CompileUtil.isValidName(p.name())) {
                                        // get the value and put into container
                                        PointerType t = new PointerType(p.type());
                                        if (types.containsKey(t.toString())) {
                                                t = (PointerType) types.get(t.toString());
                                        } else {
                                                types.put(t.toString(), t);
                                        }

                                        LocalVariable local = new LocalVariable(t, p.canChange());
                                        scope.putLeftValue(p.name(), local);
                                        local.setWrappingParam(p);

                                        // local = new Pointer(p)
                                        Ins.TStore tStore = new Ins.TStore(
                                                local, invokePointerSet(
                                                constructPointer(p.isNotNull(), p.isNotEmpty()),
                                                new Ins.TLoad(p, scope, LineCol.SYNTHETIC),
                                                LineCol.SYNTHETIC),
                                                scope, LineCol.SYNTHETIC, err);
                                        tStore.flag |= Consts.IS_POINTER_NEW;
                                        methodDef.statements().add(tStore);
                                }
                        }
                        // fill statements
                        if (statements.isEmpty()) {
                                methodDef.statements().add(new Ins.Nop());
                        } else {
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
        }

        private SConstructorDef java_lang_NullPointerException_cons;

        private SConstructorDef getJava_lang_NullPointerException_cons() throws SyntaxException {
                if (java_lang_NullPointerException_cons == null) {
                        SClassDef NPE = (SClassDef) getTypeWithName("java.lang.NullPointerException", LineCol.SYNTHETIC);
                        for (SConstructorDef cons : NPE.constructors()) {
                                if (cons.getParameters().isEmpty()) {
                                        java_lang_NullPointerException_cons = cons;
                                        break;
                                }
                        }
                }
                return java_lang_NullPointerException_cons;
        }

        private SConstructorDef java_lang_IllegalArgumentException_cons;

        private SConstructorDef getJava_lang_IllegalArgumentException_cons() throws SyntaxException {
                if (java_lang_IllegalArgumentException_cons == null) {
                        SClassDef IAE = (SClassDef) getTypeWithName("java.lang.IllegalArgumentException", LineCol.SYNTHETIC);
                        for (SConstructorDef cons : IAE.constructors()) {
                                if (cons.getParameters().isEmpty()) {
                                        java_lang_IllegalArgumentException_cons = cons;
                                        break;
                                }
                        }
                }
                return java_lang_IllegalArgumentException_cons;
        }

        /**
         * handle nonnull/nonempty modifiers on parameters
         *
         * @param params       parameters
         * @param instructions instructions
         * @param scope        current scope
         * @param lineCol      line and column
         * @throws SyntaxException compiling error
         */
        public void paramValueAvaliable(List<SParameter> params, List<Instruction> instructions, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                for (SParameter param : params) {
                        if (param.isNotEmpty()) {
                                Ins.Nop nop = new Ins.Nop();
                                Ins.IfNe ifNe = new Ins.IfNe(
                                        cast(BoolTypeDef.get(), new Ins.TLoad(param, scope, lineCol), scope.type(), lineCol),
                                        nop, lineCol);
                                instructions.add(ifNe);
                                instructions.add(new Ins.AThrow(
                                        new Ins.New(
                                                getJava_lang_IllegalArgumentException_cons(), lineCol
                                        ), lineCol));
                                instructions.add(nop);
                        } else if (param.isNotNull()) {
                                if (param.type() instanceof PrimitiveTypeDef) {
                                        continue;
                                }
                                // null
                                Ins.Nop nop = new Ins.Nop();
                                Ins.IfNonNull ifNonNull = new Ins.IfNonNull(
                                        new Ins.TLoad(param, scope, lineCol),
                                        nop, lineCol);
                                instructions.add(ifNonNull);
                                instructions.add(new Ins.AThrow(
                                        new Ins.New(
                                                getJava_lang_NullPointerException_cons(), lineCol
                                        ), lineCol));
                                instructions.add(nop);
                                // unit
                                Ins.Nop nop2 = new Ins.Nop();
                                Ins.IfACmpNe ifACmpNe = new Ins.IfACmpNe(
                                        new Ins.TLoad(param, scope, lineCol),
                                        invoke_Unit_get(lineCol),
                                        nop2, lineCol
                                );
                                instructions.add(ifACmpNe);
                                instructions.add(new Ins.AThrow(
                                        new Ins.New(
                                                getJava_lang_IllegalArgumentException_cons(), lineCol
                                        ), lineCol));
                                instructions.add(nop2);
                        }
                }
        }

        private void transformLastExpToReturn(List<Statement> statements) {
                if (statements.isEmpty()) return;
                int lastIndex = statements.size() - 1;
                Statement lastStmt = statements.get(lastIndex);
                if (lastStmt instanceof Expression) {
                        AST.Return ret = new AST.Return((Expression) lastStmt, lastStmt.line_col());
                        statements.set(lastIndex, ret);
                } else if (lastStmt instanceof AST.If) {
                        for (AST.If.IfPair pair : ((AST.If) lastStmt).ifs) {
                                transformLastExpToReturn(pair.body);
                        }
                } else if (lastStmt instanceof AST.Synchronized) {
                        transformLastExpToReturn(((AST.Synchronized) lastStmt).statements);
                } else if (lastStmt instanceof AST.Try) {
                        AST.Try aTry = (AST.Try) lastStmt;
                        transformLastExpToReturn(aTry.statements);
                        transformLastExpToReturn(aTry.catchStatements);
                }
        }

        /**
         * in step 4<br>
         * fill the given annotation with parsed values
         *
         * @param annos annotations to fill values
         * @throws SyntaxException exception
         */
        public void parseAnnoValues(Collection<SAnno> annos) throws SyntaxException {
                // check annotation
                for (SAnno sAnno : annos) {
                        AST.Anno anno = annotationRecorder.get(sAnno); // get original anno object
                        Map<SAnnoField, Value> map = new HashMap<SAnnoField, Value>();
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
                                if (!f.hasDefaultValue()) {
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
        public Value checkAndCastAnnotationValues(Value value, LineCol lineCol) throws SyntaxException {
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

                        List<Value> values = new ArrayList<Value>();
                        for (Value v : theValues) {
                                values.add(checkAndCastAnnotationValues(v, lineCol));
                        }

                        arr.setValues(values.toArray(new Value[values.size()]));
                        return arr;
                } else if (value instanceof DummyValue) {
                        return value;
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
        public boolean isInt(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
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
        public boolean isLong(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
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
        public boolean isShort(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
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
        public boolean isByte(STypeDef requiredType, NumberLiteral literal, LineCol lineCol) throws SyntaxException {
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
        public boolean isFloat(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
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
        public boolean isDouble(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
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
        public boolean isBool(STypeDef requiredType, LineCol lineCol) throws SyntaxException {
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
        public boolean isChar(STypeDef requiredType, StringLiteral literal, LineCol lineCol) throws SyntaxException {
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
        public boolean isChar(StringLiteral literal, LineCol lineCol, boolean testSymbol) throws SyntaxException {
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

        public SClassDef getThrowable_Class() throws SyntaxException {
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
         * <li>{@link MethodDef} =&gt; {@link #parseInnerMethod(MethodDef, SemanticScope, boolean)}</li>
         * <li>{@link lt.compiler.syntactic.AST.Break =&gt; goto instruction}</li>
         * <li>{@link lt.compiler.syntactic.AST.Continue =&gt; goto instruction}</li>
         * </ul>
         *
         * @param statement           instructions
         * @param methodReturnType    the method's return type
         * @param scope               scope that contains local variables and local methods
         * @param instructions        currently parsing {@link SInvokable} object instructions
         * @param exceptionTable      the exception table (start,end,handle,type)
         * @param breakIns            jump to this position when meets a break    (or null if it's not inside any loop)
         * @param continueIns         jump to this position when meets a continue (or null if it's not inside any loop)
         * @param doNotParseMethodDef the methodDef should not be parsed( in this case, they should be outer methods instead of inner methods)
         * @throws SyntaxException compile error
         */
        public void parseStatement(Statement statement,
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
                                parseInnerMethod((MethodDef) statement, scope, false);
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
         * {@link LtRuntime#castToBool(Object)}
         */
        private SMethodDef Lang_castToBool;

        /**
         * @return {@link LtRuntime#castToThrowable(Object)}
         * @throws SyntaxException exception
         */
        public SMethodDef getLang_castToBool() throws SyntaxException {
                if (Lang_castToBool == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToBool")) {
                                        Lang_castToBool = m;
                                        break;
                                }
                        }
                }
                return Lang_castToBool;
        }

        /**
         * {@link LtRuntime#castToThrowable(Object)}
         */
        private SMethodDef Lang_castToThrowable;

        /**
         * @return {@link LtRuntime#castToThrowable(Object)}
         * @throws SyntaxException exception
         */
        public SMethodDef getLang_castToThrowable() throws SyntaxException {
                if (Lang_castToThrowable == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
         * @param methodDef   method def object, defines the inner method
         * @param scope       current scope
         * @param lambdaParam add one param for lambda
         * @return the new method (the inner method)
         * @throws SyntaxException compile error
         */
        public SMethodDef parseInnerMethod(MethodDef methodDef, SemanticScope scope, boolean lambdaParam) throws SyntaxException {
                if (scope.parent == null) throw new LtBug("scope.parent should not be null");

                SemanticScope theTopScope = scope.parent;
                while (theTopScope.parent != null) theTopScope = theTopScope.parent;

                // check method name
                if (scope.containsInnerMethod(methodDef.name)) {
                        err.SyntaxException("duplicate inner method name", methodDef.line_col());
                        return null;
                }

                // inner method cannot have modifiers or annotations
                if (!methodDef.modifiers.isEmpty() &&
                        (methodDef.modifiers.size() != 1 || !methodDef.modifiers.iterator().next().modifier.equals(Modifier.Available.DEF))
                        ) {
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
                List<VariableDef> param4Locals = new ArrayList<VariableDef>();
                List<PointerType> realPointerTypes = new ArrayList<PointerType>();
                for (Map.Entry<String, STypeDef> entry : localVariables.entrySet()) {
                        String k = entry.getKey();
                        STypeDef v = entry.getValue();
                        if (!CompileUtil.isValidName(k)) continue;
                        if (k.equals("$")) continue;

                        // construct a synthetic VariableDef as param
                        VariableDef variable = new VariableDef(k, Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC);
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
                                if (v instanceof PointerType) {
                                        realPointerTypes.add((PointerType) v);
                                }
                        }
                        param4Locals.add(variable);
                }
                MethodDef newMethodDef = new MethodDef(
                        generatedMethodName,
                        Collections.<Modifier>emptySet(),
                        methodDef.returnType,
                        new ArrayList<VariableDef>(methodDef.params),
                        Collections.<AST.Anno>emptySet(),
                        methodDef.body,
                        methodDef.line_col()
                );
                newMethodDef.params.addAll(0, param4Locals);
                if (lambdaParam) {
                        newMethodDef.params.add(
                                new VariableDef("$",
                                        Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC));
                }

                // parse the method
                parseMethod(newMethodDef, newMethodDef.params.size(), scope.type(), null, fileNameToImport.get(newMethodDef.line_col().fileName),
                        (scope.type() instanceof SClassDef) ? PARSING_CLASS : PARSING_INTERFACE, scope.getThis() == null);
                SMethodDef m = methods.get(methods.size() - 1);
                // set captured values
                for (int x = 0; x < param4Locals.size(); ++x) {
                        m.meta().pointerLocalVar.add(m.getParameters().get(x));
                }

                // change the modifier
                m.modifiers().remove(SModifier.PUBLIC);
                m.modifiers().remove(SModifier.PROTECTED);
                m.modifiers().add(0, SModifier.PRIVATE);

                // change to real pointer type
                // and mark capture parameters
                int cursor = 0;
                int currentIndex = 0;
                int capturedParamSize = param4Locals.size();
                for (SParameter p : m.getParameters()) {
                        if (isPointerType(p.type())) {
                                p.setType(realPointerTypes.get(cursor++));
                        }
                        if (currentIndex < capturedParamSize) {
                                p.setCapture(true);
                        }
                        ++currentIndex;
                }
                assert cursor == realPointerTypes.size();

                // add into scope
                SemanticScope.MethodRecorder rec = new SemanticScope.MethodRecorder(m, paramCount);
                scope.addMethodDef(name, rec);

                // generate a scope for the inner method
                // the scope contains the inner method itself
                SemanticScope innerMethodScope = new SemanticScope(theTopScope, m.meta());
                for (Map.Entry<String, SemanticScope.MethodRecorder> srec : scope.getInnerMethods().entrySet()) {
                        innerMethodScope.addMethodDef(srec.getKey(), srec.getValue());
                }

                parseMethod(m, newMethodDef.body, innerMethodScope);

                return m;
        }

        /**
         * parse synchronized<br>
         * every monitor must have an exit<br>
         * <code>
         * sync(a,b,c)<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * ==&gt;<br>
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
        public void parseInstructionFromSynchronized(AST.Synchronized aSynchronized,
                                                     STypeDef methodReturnType,
                                                     SemanticScope scope,
                                                     List<Instruction> instructions,
                                                     List<ExceptionTable> exceptionTable,
                                                     Ins.Nop breakIns,
                                                     Ins.Nop continueIns) throws SyntaxException {

                SemanticScope subScope = new SemanticScope(scope, scope.getMeta());
                Stack<Ins.MonitorEnter> stack = new Stack<Ins.MonitorEnter>();
                for (Expression exp : aSynchronized.toSync) {
                        Value v = parseValueFromExpression(exp, null, subScope);
                        Ins.MonitorEnter enter = new Ins.MonitorEnter(v, subScope, exp.line_col());
                        stack.push(enter);

                        instructions.add(enter); // monitor enter
                }

                // parse statements
                List<Instruction> instructionList = new ArrayList<Instruction>();
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

                List<Ins.MonitorExit> exitNormal = new ArrayList<Ins.MonitorExit>(stack.size());
                List<Ins.MonitorExit> exitForExceptions = new ArrayList<Ins.MonitorExit>(stack.size());
                List<List<Ins.MonitorExit>> exitForReturn = new ArrayList<List<Ins.MonitorExit>>();
                List<List<Ins.MonitorExit>> exitForBreak = new ArrayList<List<Ins.MonitorExit>>();
                List<List<Ins.MonitorExit>> exitForContinue = new ArrayList<List<Ins.MonitorExit>>();
                for (int i = 0; i < returnCount; ++i) exitForReturn.add(new ArrayList<Ins.MonitorExit>());
                for (int i = 0; i < continueCount; ++i) exitForContinue.add(new ArrayList<Ins.MonitorExit>());
                for (int i = 0; i < breakCount; ++i) exitForBreak.add(new ArrayList<Ins.MonitorExit>());

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
         * @throws SyntaxException compiling error
         */
        public int insertInstructionsBeforeReturn(List<Instruction> instructions,
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
        public SMethodDef getLang_throwableWrapperObject() throws SyntaxException {
                if (Lang_throwableWrapperObject == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
         * ==&gt;
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
         * ==&gt;
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
        public void parseInstructionFromTry(AST.Try aTry,
                                            STypeDef methodReturnType,
                                            SemanticScope scope,
                                            List<Instruction> instructions,
                                            List<ExceptionTable> exceptionTable,
                                            Ins.Nop breakIns,
                                            Ins.Nop continueIns) throws SyntaxException {
                // try ...
                SemanticScope scopeA = new SemanticScope(scope, scope.getMeta());
                List<Instruction> insA = new ArrayList<Instruction>(); // instructions in scope A
                for (Statement stmt : aTry.statements) {
                        parseStatement(stmt, methodReturnType, scopeA, insA, exceptionTable, breakIns, continueIns, false);
                }

                // record the start and end for exception table
                // end is inclusive in this map
                // and should be converted to an exclusive one when added into exception table
                LinkedHashMap<Instruction, Instruction> startToEnd = new LinkedHashMap<Instruction, Instruction>();
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
                List<Instruction> normalFinally = new ArrayList<Instruction>();
                for (Statement stmt : aTry.fin) {
                        parseStatement(stmt, methodReturnType, new SemanticScope(scope, scope.getMeta()), normalFinally, exceptionTable, breakIns, continueIns, false);
                }
                if (normalFinally.isEmpty()) {
                        normalFinally.add(new Ins.Nop());
                }
                Instruction D1start = normalFinally.get(0);

                // build exception finally (D2)
                SemanticScope exceptionFinallyScope = new SemanticScope(scope, scope.getMeta());
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
                List<Instruction> exceptionFinally = new ArrayList<Instruction>();
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
                                List<Instruction> list = new ArrayList<Instruction>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType,
                                                new SemanticScope(scopeA, scope.getMeta()),
                                                list, exceptionTable, breakIns, continueIns, false);
                                }
                                i += insertInstructionsBeforeReturn(insA, i, list, scopeA);
                        } else if (breakIns != null) {
                                if (ins instanceof Ins.Goto) {
                                        if (((Ins.Goto) ins).gotoIns() == breakIns
                                                || ((Ins.Goto) ins).gotoIns() == continueIns) {

                                                List<Instruction> list = new ArrayList<Instruction>();
                                                for (Statement stmt : aTry.fin) {
                                                        parseStatement(stmt, methodReturnType,
                                                                new SemanticScope(scopeA, scope.getMeta()),
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
                LinkedHashMap<Instruction, Instruction> startToEndEx = new LinkedHashMap<Instruction, Instruction>();
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

                SemanticScope catchScope = new SemanticScope(scope, scope.getMeta()); // catch scope

                STypeDef THROWABLE = getTypeWithName("java.lang.Throwable", LineCol.SYNTHETIC);

                LocalVariable ex = new LocalVariable(THROWABLE, true);
                catchScope.putLeftValue(catchScope.generateTempName(), ex); // the exception value

                LocalVariable unwrapped = new LocalVariable(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC), false);
                catchScope.putLeftValue(aTry.varName, unwrapped);

                // build instructions
                List<Instruction> insCatch = new ArrayList<Instruction>();
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
                LinkedHashMap<Instruction, Instruction> catch_startToEnd = new LinkedHashMap<Instruction, Instruction>();
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
                                List<Instruction> list = new ArrayList<Instruction>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType, new SemanticScope(catchScope, catchScope.getMeta()),
                                                list, exceptionTable, breakIns, continueIns, false);
                                }
                                i += insertInstructionsBeforeReturn(insCatch, i, list, catchScope);
                        } else if (breakIns != null) {
                                if (ins instanceof Ins.Goto) {
                                        if (((Ins.Goto) ins).gotoIns() == breakIns
                                                || ((Ins.Goto) ins).gotoIns() == continueIns) {

                                                List<Instruction> list = new ArrayList<Instruction>();
                                                for (Statement stmt : aTry.fin) {
                                                        parseStatement(stmt, methodReturnType,
                                                                new SemanticScope(catchScope, catchScope.getMeta()),
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
                LinkedHashMap<Instruction, Instruction> catch_startToEndEx = new LinkedHashMap<Instruction, Instruction>();
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
         * {@link LtIterator#getIterator(Object)}
         */
        private SMethodDef LtIterator_getIterator;

        /**
         * @return {@link LtIterator#getIterator(Object)}
         * @throws SyntaxException exception
         */
        public SMethodDef getLtIterator_Get() throws SyntaxException {
                if (LtIterator_getIterator == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtIterator", LineCol.SYNTHETIC);
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
        public SMethodDef getLtIterator_hasNext() throws SyntaxException {
                if (LtIterator_hasNext == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtIterator", LineCol.SYNTHETIC);
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
        public SMethodDef getLtIterator_next() throws SyntaxException {
                if (LtIterator_next == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtIterator", LineCol.SYNTHETIC);
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
         * ==&gt;
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
        public void parseInstructionFromFor(AST.For aFor,
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
                LocalVariable localVariable = new LocalVariable(getTypeWithName("lt.runtime.LtIterator", LineCol.SYNTHETIC), false);
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
                SemanticScope subScope = new SemanticScope(scope, scope.getMeta());
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
         * ==&gt;
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
         * ==&gt;
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
        public void parseInstructionFromWhile(AST.While aWhile, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions, List<ExceptionTable> exceptionTable) throws SyntaxException {
                Ins.Nop nopBreak = new Ins.Nop();
                Ins.Nop nopContinue = new Ins.Nop();

                SemanticScope whileScope = new SemanticScope(scope, scope.getMeta());

                List<Instruction> ins = new ArrayList<Instruction>();
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

        private void parseStatementPartOfIf(AST.If.IfPair ifPair, STypeDef methodReturnType,
                                            SemanticScope ifScope, List<ExceptionTable> exceptionTable,
                                            Ins.Nop breakIns, Ins.Nop continueIns,
                                            List<Instruction> ins) throws SyntaxException {
                for (Statement stmt : ifPair.body) {
                        parseStatement(
                                stmt,
                                methodReturnType,
                                ifScope,
                                ins,
                                exceptionTable, breakIns, continueIns, false);
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
         * ==&gt;
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
        public void parseInstructionFromIf(AST.If anIf,
                                           STypeDef methodReturnType,
                                           SemanticScope scope,
                                           List<Instruction> instructions,
                                           List<ExceptionTable> exceptionTable,
                                           Ins.Nop breakIns,
                                           Ins.Nop continueIns) throws SyntaxException {
                Ins.Nop nop = new Ins.Nop();

                for (AST.If.IfPair ifPair : anIf.ifs) {
                        SemanticScope ifScope = new SemanticScope(scope, scope.getMeta());
                        List<Instruction> instructionList = new ArrayList<Instruction>();

                        if (ifPair.condition == null) {
                                // it's else
                                parseStatementPartOfIf(ifPair,
                                        methodReturnType,
                                        ifScope,
                                        exceptionTable,
                                        breakIns, continueIns,
                                        instructionList
                                );
                        } else {
                                // if/elseif

                                Ins.Goto gotoNop = new Ins.Goto(nop); // goto nop
                                Ins.Nop thisNop = new Ins.Nop(); // nop1/nop2/nop3/...

                                Value condition = parseValueFromExpression(ifPair.condition, BoolTypeDef.get(), ifScope);
                                Ins.IfEq ifEq = new Ins.IfEq(condition, thisNop, ifPair.condition.line_col());
                                instructionList.add(ifEq); // a ifEq (a!=true) goto nop
                                parseStatementPartOfIf(ifPair,
                                        methodReturnType,
                                        ifScope,
                                        exceptionTable,
                                        breakIns, continueIns,
                                        instructionList
                                ); // A
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
        public void parseInstructionFromReturn(AST.Return ret, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions) throws SyntaxException {
                Ins.TReturn tReturn;
                if (ret.exp == null) {
                        if (methodReturnType.equals(VoidType.get())) {
                                tReturn = new Ins.TReturn(null, ret.line_col());
                        } else {
                                if (methodReturnType.fullName().equals("lt.lang.Unit")
                                        || methodReturnType.fullName().equals("java.lang.Object")) {
                                        tReturn = new Ins.TReturn(invoke_Unit_get(LineCol.SYNTHETIC), ret.line_col());
                                } else {
                                        err.SyntaxException("the method is not void but returns nothing", ret.line_col());
                                        return;
                                }
                        }
                } else {
                        Value v = parseValueFromExpression(ret.exp,
                                methodReturnType.equals(VoidType.get()) ? null : methodReturnType, scope);
                        if (methodReturnType.equals(VoidType.get())) {
                                if (v instanceof Instruction) {
                                        instructions.add((Instruction) v);
                                }
                                tReturn = new Ins.TReturn(null, ret.line_col());
                        } else {
                                tReturn = new Ins.TReturn(v, ret.line_col());
                        }
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
        public SMethodDef getLang_putField() throws SyntaxException {
                if (null == Lang_putField) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
         * @param lineCol      lineCol
         * @throws SyntaxException compile error
         */
        public void parseInstructionFromAssignment(Value assignTo,
                                                   Value assignFrom,
                                                   SemanticScope scope,
                                                   List<Instruction> instructions,
                                                   LineCol lineCol) throws SyntaxException {
                // []= means Tastore or <set(?,?) or put(?,?)> ==> (reflectively invoke)
                // []+= means TALoad then Tastore, or get(?) then <set(?,?) or put(?,?)> ==> then set/put step would be invoked reflectively

                // else
                // simply assign `assignFrom` to `assignTo`
                // the following actions would be assign work
                if (isPointerType(assignTo.type())) {
                        if (assignFrom.type() instanceof PrimitiveTypeDef) {
                                assignFrom = boxPrimitive(assignFrom, lineCol);
                        }
                        instructions.add(invokePointerSet(assignTo, assignFrom, lineCol));
                } else if (assignTo instanceof Ins.GetField) {
                        // field
                        instructions.add(new Ins.PutField(
                                ((Ins.GetField) assignTo).field(),
                                ((Ins.GetField) assignTo).object(),
                                cast(((Ins.GetField) assignTo).field().type(), assignFrom, scope.type(), ((Ins.GetField) assignTo).line_col()),
                                lineCol, err));
                } else if (assignTo instanceof Ins.GetStatic) {
                        // static
                        instructions.add(new Ins.PutStatic(
                                ((Ins.GetStatic) assignTo).field(),
                                cast(((Ins.GetStatic) assignTo).field().type(), assignFrom, scope.type(), ((Ins.GetStatic) assignTo).line_col()),
                                lineCol, err));
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
                                        cast(arrayType.type(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(IntTypeDef.get())) {
                                // int[] IASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.IASTORE,
                                        TALoad.index(),
                                        cast(IntTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(LongTypeDef.get())) {
                                // long[] LASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.LASTORE,
                                        TALoad.index(),
                                        cast(LongTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(ShortTypeDef.get())) {
                                // short[] SASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.SASTORE,
                                        TALoad.index(),
                                        cast(ShortTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(ByteTypeDef.get()) || arrayType.type().equals(BoolTypeDef.get())) {
                                // byte[]/boolean[] BASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.BASTORE,
                                        TALoad.index(),
                                        cast(ByteTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(FloatTypeDef.get())) {
                                // float[] FASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.FASTORE,
                                        TALoad.index(),
                                        cast(FloatTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(DoubleTypeDef.get())) {
                                // double[] DASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.DASTORE,
                                        TALoad.index(),
                                        cast(DoubleTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else if (arrayType.type().equals(CharTypeDef.get())) {
                                // char[] CASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.CASTORE,
                                        TALoad.index(),
                                        cast(CharTypeDef.get(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        } else {
                                // object[] AASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.AASTORE,
                                        TALoad.index(),
                                        cast(arrayType.type(), assignFrom, scope.type(), lineCol),
                                        lineCol));
                        }
                } else if (assignTo instanceof Ins.TLoad) {
                        // local variable
                        instructions.add(new Ins.TStore(
                                ((Ins.TLoad) assignTo).value(),
                                cast(
                                        assignTo.type(),
                                        assignFrom,
                                        scope.type(),
                                        lineCol),
                                scope, lineCol, err));
                } else if (assignTo instanceof Ins.InvokeStatic) {
                        // assignTo should be lt.runtime.LtRuntime.getField(o,name)
                        // which means
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) assignTo;
                        if (isGetFieldAtRuntime(invokeStatic)) {
                                // dynamically get field
                                // invoke lt.runtime.LtRuntime.putField(o,name,value)
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
                        } else if (isInvokeAtRuntime(invokeStatic)) {
                                // invoke get(?)
                                // list[?1]=?2
                                // or
                                // map[?1]=?2
                                Ins.InvokeStatic invoke = (Ins.InvokeStatic) assignTo;
                                if (((StringConstantValue) invoke.arguments().get(INDEX_invoke_method)).getStr().equals("get")
                                        && ((Ins.ANewArray) invoke.arguments().get(INDEX_invoke_args)).initValues().size() > 0) {
                                        // the method to invoke should be set
                                        Ins.GetClass cls = (Ins.GetClass) invoke.arguments().get(0); // class
                                        Value target = invoke.arguments().get(1);

                                        // args
                                        List<Value> list = new ArrayList<Value>();
                                        list.addAll(((Ins.ANewArray) invoke.arguments().get(INDEX_invoke_args)).initValues());
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
                                        List<Value> list = new ArrayList<Value>();
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
        public Value parseValueFromVariableDef(VariableDef variableDef, SemanticScope scope) throws SyntaxException {
                // generic type map
                Map<String, STypeDef> genericTypeMap = getGenericMap(scope.type());

                // handle
                List<Import> imports = fileNameToImport.get(variableDef.line_col().fileName);

                STypeDef type = variableDef.getType() == null

                        ? getTypeWithName(
                        "java.lang.Object",
                        variableDef.line_col())

                        : getTypeWithAccess(
                        variableDef.getType(),
                        genericTypeMap,
                        imports);
                STypeDef rawType = type;

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

                if (variableDef.getInit() != null || isLocalVar) {
                        boolean nonnull = false;
                        boolean nonempty = false;
                        // variable def with a init value

                        Value v;

                        ValuePack pack = new ValuePack(true);
                        if (isLocalVar) {
                                boolean canChange = true;
                                for (Modifier m : variableDef.getModifiers()) {
                                        if (m.modifier.equals(Modifier.Available.VAL)) {
                                                canChange = false;
                                        } else if (m.modifier.equals(Modifier.Available.NONNULL)) {
                                                nonnull = true;
                                        } else if (m.modifier.equals(Modifier.Available.NONEMPTY)) {
                                                nonempty = true;
                                        } else if (!m.modifier.equals(Modifier.Available.VAR)) {
                                                err.SyntaxException("invalid modifier for local variable "
                                                        + m.modifier.name().toLowerCase(), m.line_col());
                                        }
                                }

                                AST.Access typeAccess = variableDef.getType();
                                type = getTypeWithAccess(new AST.Access(typeAccess, "*", typeAccess == null ? LineCol.SYNTHETIC : typeAccess.line_col()), genericTypeMap, imports);

                                LocalVariable localVariable = new LocalVariable(type, canChange);
                                scope.putLeftValue(variableDef.getName(), localVariable);

                                Ins.TStore storePtr = new Ins.TStore(localVariable,
                                        constructPointer(nonnull, nonempty), scope, LineCol.SYNTHETIC, err);
                                storePtr.flag |= Consts.IS_POINTER_NEW;
                                pack.instructions().add(storePtr);
                        }

                        if (variableDef.getInit() == null) {
                                if (rawType instanceof PrimitiveTypeDef) {
                                        if (rawType instanceof IntTypeDef) {
                                                v = new IntValue(0);
                                        } else if (rawType instanceof ShortTypeDef || rawType instanceof ByteTypeDef) {
                                                v = new ValueAnotherType(type, new IntValue(0), LineCol.SYNTHETIC);
                                        } else if (rawType instanceof LongTypeDef) {
                                                v = new LongValue(0);
                                        } else if (rawType instanceof FloatTypeDef) {
                                                v = new FloatValue(0);
                                        } else if (rawType instanceof DoubleTypeDef) {
                                                v = new DoubleValue(0);
                                        } else if (rawType instanceof BoolTypeDef) {
                                                v = new BoolValue(false);
                                        } else if (rawType instanceof CharTypeDef) {
                                                v = new CharValue((char) 0);
                                        } else throw new LtBug("unknown primitive type " + type);
                                } else {
                                        v = NullValue.get();
                                }
                        } else {
                                v = parseValueFromExpression(
                                        variableDef.getInit(),
                                        rawType,
                                        scope);
                        }

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
                                assert isPointerType(type);
                                // else field not found
                                /*
                                 * tload
                                 * get and set
                                 */
                                LocalVariable localVariable = (LocalVariable) scope.getLeftValue(variableDef.getName());

                                Ins.InvokeVirtual invokeSet = invokePointerSet(
                                        new Ins.TLoad(localVariable, scope, LineCol.SYNTHETIC),
                                        v, variableDef.line_col());
                                pack.instructions().add(invokeSet);

                                Value get = invokePointerGet(new Ins.TLoad(localVariable, scope, variableDef.line_col()),
                                        variableDef.line_col());
                                pack.instructions().add((Instruction) get);
                                localVariable.alreadyAssigned();

                                if (!localVariable.canChange()) {
                                        // set type for val values
                                        PointerType tmp = new PointerType(v.type());
                                        if (types.containsKey(tmp.toString())) {
                                                tmp = (PointerType) types.get(tmp.toString());
                                        } else {
                                                types.put(tmp.toString(), tmp);
                                        }
                                        localVariable.setType(tmp);
                                }

                                // nonnull and nonempty check
                                if (nonempty || nonnull) {
                                        scope.getMeta().pointerLocalVar.add(localVariable);
                                }
                        }
                        return pack;
                }

                // else ignore the var def
                return null;
        }

        private SConstructorDef Pointer_con;

        public SConstructorDef getPointer_con() throws SyntaxException {
                if (Pointer_con == null) {
                        SClassDef Pointer = (SClassDef) getTypeWithName("lt.lang.Pointer", LineCol.SYNTHETIC);
                        for (SConstructorDef con : Pointer.constructors()) {
                                if (con.getParameters().size() == 2
                                        && con.getParameters().get(0).type().equals(BoolTypeDef.get())
                                        && con.getParameters().get(1).type().equals(BoolTypeDef.get())) {
                                        Pointer_con = con;
                                        break;
                                }
                        }
                }
                return Pointer_con;
        }

        public Ins.New constructPointer(boolean nonnull, boolean nonempty) throws SyntaxException {
                Ins.New aNew = new Ins.New(getPointer_con(), LineCol.SYNTHETIC);
                aNew.args().add(new BoolValue(nonnull));
                aNew.args().add(new BoolValue(nonempty));
                return aNew;
        }

        private SMethodDef Pointer_set;

        public SMethodDef getPointer_set() throws SyntaxException {
                if (Pointer_set == null) {
                        SClassDef Pointer = (SClassDef) getTypeWithName("lt.lang.Pointer", LineCol.SYNTHETIC);
                        for (SMethodDef m : Pointer.methods()) {
                                if (m.name().equals("set") && m.getParameters().size() == 1) {
                                        Pointer_set = m;
                                        break;
                                }
                        }
                }
                return Pointer_set;
        }

        public Ins.InvokeVirtual invokePointerSet(Value target, Value valueToSet, LineCol lineCol) throws SyntaxException {
                assert isPointerType(target.type());
                Ins.InvokeVirtual set = new Ins.InvokeVirtual(
                        target,
                        getPointer_set(),
                        lineCol);
                if (valueToSet.type() instanceof PrimitiveTypeDef) {
                        valueToSet = boxPrimitive(valueToSet, LineCol.SYNTHETIC);
                }
                set.arguments().add(valueToSet);
                set.flag |= Consts.IS_POINTER_SET;
                return set;
        }

        private SMethodDef Pointer_get;

        public SMethodDef getPointer_get() throws SyntaxException {
                if (Pointer_get == null) {
                        SClassDef Pointer = (SClassDef) getTypeWithName("lt.lang.Pointer", LineCol.SYNTHETIC);
                        for (SMethodDef m : Pointer.methods()) {
                                if (m.name().equals("get") && m.getParameters().isEmpty()) {
                                        Pointer_get = m;
                                        break;
                                }
                        }
                }
                return Pointer_get;
        }

        public Value invokePointerGet(Value target, LineCol lineCol) throws SyntaxException {
                assert isPointerType(target.type());
                STypeDef pointingType = getPointingType(target.type());

                Ins.InvokeVirtual get = new Ins.InvokeVirtual(
                        target, getPointer_get(), lineCol);
                get.flag |= Consts.IS_POINTER_GET;

                if (pointingType instanceof PrimitiveTypeDef) {
                        Value after = cast(pointingType, get, null, lineCol);
                        return new Ins.PointerGetCastHelper(after, get);
                } else {
                        return new Ins.CheckCast(get, pointingType, lineCol);
                }
        }

        public STypeDef getPointingType(STypeDef pointerT) throws SyntaxException {
                STypeDef pointingType;
                if (pointerT instanceof PointerType) {
                        pointingType = ((PointerType) pointerT).getPointingType();
                } else {
                        pointingType = getTypeWithName("java.lang.Object", LineCol.SYNTHETIC);
                }
                return pointingType;
        }

        private SMethodDef LtRuntime_destruct;

        public SMethodDef getLtRuntime_destruct() throws SyntaxException {
                if (LtRuntime_destruct == null) {
                        SClassDef c = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
                        assert c != null;
                        for (SMethodDef m : c.methods()) {
                                if (m.name().equals("destruct")) {
                                        LtRuntime_destruct = m;
                                        break;
                                }
                        }
                }
                assert LtRuntime_destruct != null;
                return LtRuntime_destruct;
        }

        private SMethodDef List_get;

        public SMethodDef getList_get() throws SyntaxException {
                if (List_get == null) {
                        SInterfaceDef ListDef = (SInterfaceDef) getTypeWithName("java.util.List", LineCol.SYNTHETIC);
                        for (SMethodDef m : ListDef.methods()) {
                                if (m.name().equals("get") && m.getParameters().size() == 1
                                        && m.getParameters().get(0).type().equals(IntTypeDef.get())) {
                                        List_get = m;
                                        break;
                                }
                        }
                }
                assert List_get != null;
                return List_get;
        }

        private SMethodDef Map_get;

        private SMethodDef getMap_get() throws SyntaxException {
                if (Map_get == null) {
                        SInterfaceDef MapDef = (SInterfaceDef) getTypeWithName("java.util.Map", LineCol.SYNTHETIC);
                        for (SMethodDef m : MapDef.methods()) {
                                if (m.name().equals("get") && m.getParameters().size() == 1
                                        && m.getParameters().get(0).type().equals(getObject_Class())) {
                                        Map_get = m;
                                        break;
                                }
                        }
                }
                assert Map_get != null;
                return Map_get;
        }

        private boolean destructCanChangeInLocalVariable(AST.Destruct d) throws SyntaxException {
                if (d.modifiers.size() > 1) {
                        err.SyntaxException("modifier for destruct should only be `var` or `val`", d.line_col());
                }
                boolean isVar = false;
                boolean isVal = false;
                for (Modifier m : d.modifiers) {
                        if (m.modifier == Modifier.Available.VAR) {
                                isVar = true;
                        } else if (m.modifier == Modifier.Available.VAL) {
                                isVal = true;
                        }
                }
                if (!isVal && !isVar && d.modifiers.size() > 0) {
                        err.SyntaxException("modifier for destruct should only be `var` or `val`", d.line_col());
                }
                return isVar;
        }

        /**
         * parse destruct
         *
         * @param destruct AST destruct
         * @param scope    scope
         * @return bool result
         * @throws SyntaxException compiling error
         */
        public Value parseValueFromDestruct(AST.Destruct destruct, SemanticScope scope) throws SyntaxException {
                // generic type map
                Map<String, STypeDef> genericTypeMap = getGenericMap(scope.type());

                // imports
                List<Import> imports = fileNameToImport.get(destruct.line_col().fileName);

                // init value pack
                ValuePack pack = new ValuePack(true);

                Map<String, SFieldDef> nameToField = new HashMap<String, SFieldDef>(); // not used when pattern variables are defined as local variables
                // define variables
                for (AST.Pattern p : destruct.pattern.subPatterns) {
                        assert (p instanceof AST.Pattern_Default || p instanceof AST.Pattern_Define);

                        if (p instanceof AST.Pattern_Default) continue;
                        AST.Pattern_Define pd = (AST.Pattern_Define) p;

                        SFieldDef f = findFieldFromTypeDef(pd.name, scope.type(), scope.type(),
                                scope.getThis() == null ? FIND_MODE_STATIC : FIND_MODE_NON_STATIC, true);
                        if (f == null) {
                                STypeDef type = getTypeWithAccess(new AST.Access(pd.type, "*", destruct.line_col()), genericTypeMap, imports);

                                LocalVariable localVariable = new LocalVariable(type, destructCanChangeInLocalVariable(destruct));
                                scope.putLeftValue(pd.name, localVariable);

                                Ins.TStore storePtr = new Ins.TStore(localVariable,
                                        constructPointer(false, false), scope, LineCol.SYNTHETIC, err);
                                storePtr.flag |= Consts.IS_POINTER_NEW;
                                pack.instructions().add(storePtr);
                        } else {
                                nameToField.put(pd.name, f);
                        }
                }

                // unapply result list local variable
                LocalVariable listLocalVar = new LocalVariable(getTypeWithName("java.util.List", LineCol.SYNTHETIC), false);
                scope.putLeftValue(scope.generateTempName(), listLocalVar);

                // get result list
                Ins.InvokeStatic getResList = new Ins.InvokeStatic(getLtRuntime_destruct(), destruct.line_col());
                getResList.arguments().add(new IntValue(destruct.pattern.subPatterns.size())); // count
                // destructClass
                if (destruct.pattern.type == null) {
                        getResList.arguments().add(NullValue.get());
                } else {
                        getResList.arguments().add(new Ins.GetClass(getTypeWithAccess(destruct.pattern.type, genericTypeMap, imports),
                                (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                }
                getResList.arguments().add(parseValueFromExpression(destruct.exp, null, scope)); // o
                getResList.arguments().add(new Ins.GetClass(scope.type(),
                        (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC))); // invoker

                // store result list
                Ins.TStore storeList = new Ins.TStore(listLocalVar,
                        getResList, scope, LineCol.SYNTHETIC, err);
                pack.instructions().add(storeList);

                // init bool result value
                LocalVariable boolResult = new LocalVariable(BoolTypeDef.get(), true);
                scope.putLeftValue(scope.generateTempName(), boolResult);
                Ins.TStore storeBoolResult = new Ins.TStore(boolResult, new BoolValue(false), scope, LineCol.SYNTHETIC, err);
                pack.instructions().add(storeBoolResult);

                // check list is null ?
                Ins.Nop flagWhenEnd = new Ins.Nop();
                // is null jump to flagWhenListIsNull
                Ins.IfNull ifNull = new Ins.IfNull(new Ins.TLoad(listLocalVar, scope, LineCol.SYNTHETIC),
                        flagWhenEnd, LineCol.SYNTHETIC);
                pack.instructions().add(ifNull);

                int index = -1;
                for (AST.Pattern p : destruct.pattern.subPatterns) {
                        ++index;
                        if (p instanceof AST.Pattern_Default) continue;
                        assert p instanceof AST.Pattern_Define;

                        // assign value
                        AST.Pattern_Define define = (AST.Pattern_Define) p;

                        // List#get(int)
                        SMethodDef get = getList_get();
                        Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(
                                new Ins.TLoad(listLocalVar, scope, destruct.line_col()),
                                get, destruct.line_col());
                        invokeInterface.arguments().add(new IntValue(index));

                        // set value
                        if (nameToField.containsKey(((AST.Pattern_Define) p).name)) {
                                SFieldDef f = nameToField.get(((AST.Pattern_Define) p).name);
                                if (scope.getThis() == null) {
                                        pack.instructions().add(
                                                new Ins.PutStatic(
                                                        f,
                                                        invokeInterface,
                                                        LineCol.SYNTHETIC, err)
                                        );
                                } else {
                                        pack.instructions().add(
                                                new Ins.PutField(
                                                        f,
                                                        scope.getThis(),
                                                        invokeInterface,
                                                        LineCol.SYNTHETIC, err)
                                        );
                                }
                        } else {
                                LeftValue leftValue = scope.getLeftValue(define.name);
                                leftValue.assign();
                                Ins.TLoad tLoad = new Ins.TLoad(leftValue, scope, LineCol.SYNTHETIC);
                                pack.instructions().add(invokePointerSet(
                                        tLoad,
                                        invokeInterface, destruct.line_col()
                                ));
                                // this is considered as `captured` for that
                                // it's defined before they actually get assigned
                                // and they might be used inside an if expression
                                scope.getMeta().pointerLocalVar.add(tLoad.value());
                        }
                }

                // store result as true
                pack.instructions().add(new Ins.TStore(boolResult, new BoolValue(true), scope, LineCol.SYNTHETIC, err));

                // flag when end
                pack.instructions().add(flagWhenEnd);

                // get result
                pack.instructions().add(new Ins.TLoad(boolResult, scope, LineCol.SYNTHETIC));
                return pack;
        }

        private boolean isAssignable(STypeDef sTypeDef, String typeName) throws SyntaxException {
                return sTypeDef.isAssignableFrom(getTypeWithName(typeName, LineCol.SYNTHETIC));
        }

        private Value primitiveOrBoxed(STypeDef required, Value primitive) throws SyntaxException {
                if (required instanceof PrimitiveTypeDef) {
                        return primitive;
                } else {
                        return boxPrimitive(primitive, LineCol.SYNTHETIC);
                }
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
         * <li>{@link lt.compiler.syntactic.AST.Null}</li>
         * <li>{@link lt.compiler.syntactic.AST.ArrayExp} =&gt; array/java.util.LinkedList</li>
         * <li>{@link lt.compiler.syntactic.AST.MapExp} =&gt; java.util.LinkedHashMap</li>
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
        public Value parseValueFromExpression(Expression exp, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                // generic type map
                Map<String, STypeDef> genericTypeMap = scope == null ? Collections.<String, STypeDef>emptyMap() : getGenericMap(scope.type());
                // handle
                if (requiredType != null && isPointerType(requiredType)) {
                        requiredType = getPointingType(requiredType);
                }

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
                                } else if (requiredType != null && (
                                        requiredType.equals(BoolTypeDef.get()) ||
                                                isAssignable(requiredType, "java.lang.Boolean")
                                )) {
                                        double dbl = Double.parseDouble(((NumberLiteral) exp).literal());
                                        return primitiveOrBoxed(requiredType, new BoolValue(dbl != 0d));
                                } else if (requiredType != null && (
                                        requiredType.equals(CharTypeDef.get()) ||
                                                isAssignable(requiredType, "java.lang.Character")
                                )) {
                                        int anInt = Integer.parseInt(((NumberLiteral) exp).literal());
                                        return primitiveOrBoxed(requiredType, new CharValue((char) anInt));
                                } else {
                                        assert requiredType != null;
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
                                if (literal.equals("true") || literal.equals("yes")) {
                                        b = true;
                                } else if (literal.equals("false") || literal.equals("no")) {
                                        b = false;
                                } else {
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
                                return generateStringConcat(str, scope, exp.line_col());
                        } else if (requiredType.equals(BoolTypeDef.get()) || isAssignable(requiredType, "java.lang.Boolean")) {
                                return primitiveOrBoxed(requiredType, new BoolValue(!str.isEmpty()));
                        } else {
                                if (((StringLiteral) exp).literal().startsWith("'") && str.length() == 1) {
                                        // is char literal
                                        int num = (int) str.charAt(0);
                                        if (requiredType.equals(IntTypeDef.get()) || isAssignable(requiredType, "java.lang.Integer")) {
                                                return primitiveOrBoxed(requiredType, new IntValue(num));
                                        } else if (requiredType.equals(LongTypeDef.get()) || isAssignable(requiredType, "java.lang.Long")) {
                                                return primitiveOrBoxed(requiredType, new LongValue(num));
                                        } else if (requiredType.equals(FloatTypeDef.get()) || isAssignable(requiredType, "java.lang.Float")) {
                                                return primitiveOrBoxed(requiredType, new FloatValue(num));
                                        } else if (requiredType.equals(DoubleTypeDef.get()) || isAssignable(requiredType, "java.lang.Double")) {
                                                return primitiveOrBoxed(requiredType, new DoubleValue(num));
                                        } else if (requiredType.equals(ByteTypeDef.get()) || isAssignable(requiredType, "java.lang.Byte")) {
                                                return primitiveOrBoxed(requiredType, new ByteValue((byte) num));
                                        } else if (requiredType.equals(ShortTypeDef.get()) || isAssignable(requiredType, "java.lang.Short")) {
                                                return primitiveOrBoxed(requiredType, new ShortValue((short) num));
                                        }
                                }
                                err.SyntaxException(exp + " cannot be converted into " + requiredType, exp.line_col());
                                return null;
                        }
                } else if (exp instanceof VariableDef) {
                        // variable def
                        // putField/putStatic/TStore
                        v = parseValueFromVariableDef((VariableDef) exp, scope);
                } else if (exp instanceof AST.Destruct) {
                        v = parseValueFromDestruct((AST.Destruct) exp, scope);
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
                        v = parseValueFromExpression(asType.exp, getTypeWithAccess(asType.type, genericTypeMap, imports), scope);
                } else if (exp instanceof AST.Access) {
                        // parse access
                        if (((AST.Access) exp).name == null) {
                                // name is null, then only parse the expression
                                v = parseValueFromExpression(((AST.Access) exp).exp, requiredType, scope);
                        } else {
                                // the result can be getField/getStatic/xload/invokeStatic(dynamically get field)
                                v = parseValueFromAccess((AST.Access) exp, scope, false);
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
                        STypeDef t = getTypeWithAccess(((AST.TypeOf) exp).type, genericTypeMap, imports);
                        if (t.fullName().equals("lt.lang.Unit")) {
                                t = VoidType.get();
                        }
                        v = new Ins.GetClass(
                                t, (SClassDef) getTypeWithName(
                                "java.lang.Class",
                                LineCol.SYNTHETIC
                        ));
                } else if (exp instanceof AST.AnnoExpression) {
                        SAnno anno = new SAnno();
                        AST.Anno astAnno = ((AST.AnnoExpression) exp).anno;
                        STypeDef type = getTypeWithAccess(astAnno.anno, genericTypeMap, imports);
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
                        assert scope != null;
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
                } else if (exp instanceof AST.New) {
                        v = parseValueFromNew((AST.New) exp, scope);
                } else if (exp instanceof AST.GeneratorSpec) {
                        v = parseValueFromGeneratorSpec((AST.GeneratorSpec) exp, requiredType, scope);
                } else if (exp instanceof AST.PatternMatching) {
                        v = parseValueFromPatternMatching((AST.PatternMatching) exp, requiredType, scope);
                } else {
                        throw new LtBug("unknown expression " + exp);
                }
                return cast(requiredType, v, scope == null ? null : scope.type(), exp.line_col());
        }

        public Value parseValueFromPatternMatching(AST.PatternMatching pm,
                                                   STypeDef requiredType,
                                                   SemanticScope scope) throws SyntaxException {
                // no patterns
                if (pm.patternsToStatements.size() == 0) {
                        // simply expand the value and return an Unit
                        ValuePack vp = new ValuePack(true);

                        // store the value
                        Value valueToMatch = parseValueFromExpression(pm.expToMatch, null, scope);
                        if (valueToMatch instanceof Instruction) {
                                vp.instructions().add((Instruction) valueToMatch);
                        }
                        vp.instructions().add(invoke_Unit_get(LineCol.SYNTHETIC));
                        return vp;
                }

                String fileName = pm.line_col().fileName;

                // transform the `PatternMatching` into `Procedure`
                // and invoke parseValueFromProcedure
                List<Statement> stmts = new ArrayList<Statement>();
                AST.Procedure procedure = new AST.Procedure(stmts, pm.line_col());

                // store the expToMatch
                // vExp = expToMatch
                Expression exp = pm.expToMatch;
                VariableDef vExp = new VariableDef(scope.generateTempName(), Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC_WITH_FILE(fileName));
                vExp.setInit(exp);
                procedure.statements.add(vExp);

                // define some inner methods
                // def patternMatching${hashCode}${count}
                List<MethodDef> patternMethods = new ArrayList<MethodDef>();
                int index = -1;
                for (AST.PatternCondition ignore : pm.patternsToStatements.keySet()) {
                        ++index;
                        MethodDef methodDef = new MethodDef(
                                "patternMatching$" + Integer.toHexString(Math.abs(pm.hashCode())) + "$" + index,
                                Collections.<Modifier>emptySet(),
                                null,
                                Collections.singletonList(
                                        new VariableDef("**", Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC_WITH_FILE(fileName))
                                ),
                                Collections.<AST.Anno>emptySet(),
                                new ArrayList<Statement>(),
                                LineCol.SYNTHETIC_WITH_FILE(fileName)
                        );
                        patternMethods.add(methodDef);
                }
                for (int i = patternMethods.size() - 1; i >= 0; --i) {
                        procedure.statements.add(patternMethods.get(i));
                }

                // parse these methods
                index = -1;
                for (Map.Entry<AST.PatternCondition, List<Statement>> patternListEntry : pm.patternsToStatements.entrySet()) {
                        ++index;
                        MethodDef currentMethod = patternMethods.get(index);
                        MethodDef nextMethod = null;
                        if (index + 1 < patternMethods.size()) {
                                nextMethod = patternMethods.get(index + 1);
                        }
                        parsePatternMatchingMethod(patternListEntry, currentMethod, nextMethod, pm.line_col().fileName);
                }

                // invoke first method
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(null, patternMethods.get(0).name, LineCol.SYNTHETIC_WITH_FILE(fileName)),
                        Collections.<Expression>singletonList(new AST.Access(null, vExp.getName(), LineCol.SYNTHETIC_WITH_FILE(fileName))),
                        false, pm.line_col()
                );
                procedure.statements.add(invocation);

                return parseValueFromProcedure(procedure, requiredType, scope);
        }

        private interface PatternMatchingParser {
                List<Statement> parse(
                        AST.Pattern pattern,
                        List<Statement> statements,
                        AST.If.IfPair anElse,
                        String varName,
                        String fileName
                );
        }

        private class PatternMatchingDefaultParser implements PatternMatchingParser {
                @Override
                public List<Statement> parse(AST.Pattern pattern, List<Statement> statements, AST.If.IfPair anElse, String varName, String fileName) {
                        return statements;
                }
        }

        private class PatternMatchingTypeParser implements PatternMatchingParser {
                @Override
                public List<Statement> parse(AST.Pattern pattern, List<Statement> statements, AST.If.IfPair anElse, String varName, String fileName) {
                        AST.If.IfPair testClass = new AST.If.IfPair(
                                new TwoVariableOperation(
                                        "is",
                                        new AST.Access(null, varName, LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                        new AST.TypeOf(
                                                ((AST.Pattern_Type) pattern).type,
                                                LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                        LineCol.SYNTHETIC_WITH_FILE(fileName)
                                ), statements, LineCol.SYNTHETIC_WITH_FILE(fileName)
                        );
                        AST.If anIf = new AST.If(Arrays.asList(testClass, anElse), LineCol.SYNTHETIC_WITH_FILE(fileName));
                        return Collections.<Statement>singletonList(anIf);
                }
        }

        private class PatternMatchingValueParser implements PatternMatchingParser {
                @Override
                public List<Statement> parse(AST.Pattern pattern, List<Statement> statements, AST.If.IfPair anElse, String varName, String fileName) {
                        AST.If.IfPair testValue = new AST.If.IfPair(
                                new TwoVariableOperation(
                                        "is",
                                        new AST.Access(null, varName, LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                        ((AST.Pattern_Value) pattern).exp,
                                        LineCol.SYNTHETIC_WITH_FILE(fileName)
                                ), statements, LineCol.SYNTHETIC_WITH_FILE(fileName)
                        );
                        AST.If anIf = new AST.If(Arrays.asList(testValue, anElse), LineCol.SYNTHETIC_WITH_FILE(fileName));
                        return Collections.<Statement>singletonList(anIf);
                }
        }

        private class PatternMatchingDefineParser implements PatternMatchingParser {
                @Override
                public List<Statement> parse(AST.Pattern pattern, List<Statement> statements, AST.If.IfPair anElse, String varName, String fileName) {
                        // define value and assign it with **
                        List<Statement> theList;
                        if (!((AST.Pattern_Define) pattern).name.equals(varName)) {
                                // val v = **
                                VariableDef v = new VariableDef(((AST.Pattern_Define) pattern).name,
                                        Collections.singleton(new Modifier(Modifier.Available.VAL, LineCol.SYNTHETIC_WITH_FILE(fileName))),
                                        Collections.<AST.Anno>emptySet(), LineCol.SYNTHETIC_WITH_FILE(fileName));
                                v.setType(((AST.Pattern_Define) pattern).type);
                                v.setInit(new AST.Access(null, varName, LineCol.SYNTHETIC_WITH_FILE(fileName)));
                                theList = new ArrayList<Statement>();
                                theList.add(v);
                                theList = new BindList<Statement>(theList, statements);
                        } else {
                                theList = statements;
                        }

                        if (((AST.Pattern_Define) pattern).type != null) {
                                AST.If.IfPair testClass = new AST.If.IfPair(
                                        new TwoVariableOperation(
                                                "is",
                                                new AST.Access(null, varName, LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                                new AST.TypeOf(
                                                        ((AST.Pattern_Define) pattern).type,
                                                        LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                                LineCol.SYNTHETIC_WITH_FILE(fileName)
                                        ), theList, LineCol.SYNTHETIC_WITH_FILE(fileName)
                                );
                                AST.If anIf = new AST.If(Arrays.asList(testClass, anElse), LineCol.SYNTHETIC_WITH_FILE(fileName));
                                return Collections.<Statement>singletonList(anIf);
                        } else {
                                return statements;
                        }
                }
        }

        private class PatternMatchingDestructParser implements PatternMatchingParser {
                private final int initialCount;

                private PatternMatchingDestructParser(int initialCount) {
                        this.initialCount = initialCount;
                }

                @Override
                public List<Statement> parse(AST.Pattern pattern, List<Statement> statements, AST.If.IfPair anElse, String varName, String fileName) {
                        // check if null
                        List<Statement> stmtsInCheckNull = new ArrayList<Statement>();
                        AST.If checkNull = new AST.If(Arrays.asList(
                                new AST.If.IfPair(
                                        new TwoVariableOperation(
                                                "not",
                                                new AST.Access(null, varName, LineCol.SYNTHETIC),
                                                new AST.Null(LineCol.SYNTHETIC),
                                                LineCol.SYNTHETIC
                                        ),
                                        stmtsInCheckNull,
                                        LineCol.SYNTHETIC
                                ),
                                anElse
                        ), LineCol.SYNTHETIC);

                        // use destruct <-
                        AST.Pattern_Destruct patternDestruct = (AST.Pattern_Destruct) pattern;
                        // construct a destruct without type def
                        // arg index
                        int count = initialCount;
                        // tmpName count
                        int tmpNameCount = 0;
                        List<AST.Pattern> tmpList = new ArrayList<AST.Pattern>();
                        for (AST.Pattern p : patternDestruct.subPatterns) {
                                ++count;
                                ++tmpNameCount;

                                if (p.patternType == AST.PatternType.TYPE) {
                                        tmpList.add(new AST.Pattern_Define("**" + count, null));
                                        continue;
                                }
                                if (p.patternType == AST.PatternType.DEFINE && ((AST.Pattern_Define) p).type != null) {
                                        tmpList.add(new AST.Pattern_Define(((AST.Pattern_Define) p).name, null));
                                        continue;
                                }
                                if (p.patternType == AST.PatternType.VALUE) {
                                        tmpList.add(new AST.Pattern_Define("**" + count, null));
                                        continue;
                                }
                                if (p.patternType == AST.PatternType.DESTRUCT) {
                                        tmpList.add(new AST.Pattern_Define("**" + count, null));
                                        continue;
                                }

                                --tmpNameCount;
                                tmpList.add(p);
                        }
                        AST.Pattern_Destruct patternDestructWithoutTypeDef = new AST.Pattern_Destruct(
                                patternDestruct.type,
                                tmpList
                        );

                        AST.Destruct doDestruct = new AST.Destruct(Collections.<Modifier>emptySet(), Collections.<AST.Anno>emptySet(),
                                patternDestructWithoutTypeDef, new AST.Access(null, varName, LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                        List<Statement> stmtsInDestruct;
                        if (tmpNameCount > 0) {
                                stmtsInDestruct = new ArrayList<Statement>();
                        } else {
                                stmtsInDestruct = statements;
                        }
                        AST.If destructIf = new AST.If(
                                Arrays.asList(
                                        new AST.If.IfPair(doDestruct, stmtsInDestruct, LineCol.SYNTHETIC),
                                        anElse
                                ), LineCol.SYNTHETIC
                        );
                        stmtsInCheckNull.add(destructIf);

                        if (tmpNameCount > 0) {
                                // apply patterns on these variables
                                List<Statement> currentStmts = stmtsInDestruct;
                                for (int i = 0; i < patternDestruct.subPatterns.size(); ++i) {
                                        AST.Pattern sub = patternDestruct.subPatterns.get(i);
                                        AST.Pattern x = patternDestructWithoutTypeDef.subPatterns.get(i);
                                        String tmpName = null;
                                        if (x instanceof AST.Pattern_Define) {
                                                tmpName = ((AST.Pattern_Define) x).name;
                                        }
                                        PatternMatchingParser parser;
                                        if (sub.patternType == AST.PatternType.TYPE) {
                                                parser = new PatternMatchingTypeParser();
                                        } else if (sub.patternType == AST.PatternType.DEFINE) {
                                                AST.Access type = ((AST.Pattern_Define) sub).type;
                                                if (type == null) continue;
                                                parser = new PatternMatchingDefineParser();
                                        } else if (sub.patternType == AST.PatternType.VALUE) {
                                                parser = new PatternMatchingValueParser();
                                        } else if (sub.patternType == AST.PatternType.DESTRUCT) {
                                                parser = new PatternMatchingDestructParser(count);
                                        } else continue;

                                        // handle
                                        assert tmpName != null;
                                        --tmpNameCount;

                                        List<Statement> list;
                                        if (tmpNameCount == 0) {
                                                list = statements;
                                        } else {
                                                list = new ArrayList<Statement>();
                                        }
                                        currentStmts.addAll(parser.parse(
                                                sub, list, anElse, tmpName, fileName
                                        ));

                                        currentStmts = list;
                                }
                        }

                        return Collections.<Statement>singletonList(checkNull);
                }
        }

        private void parsePatternMatchingMethod(Map.Entry<AST.PatternCondition, List<Statement>> patternListEntry,
                                                MethodDef currentMethod,
                                                MethodDef nextMethod,
                                                String fileName) throws SyntaxException {
                AST.PatternCondition p = patternListEntry.getKey();
                List<Statement> statements = patternListEntry.getValue();

                AST.If.IfPair anElse;
                if (nextMethod == null) {
                        // throw when else
                        anElse = new AST.If.IfPair(
                                null,
                                Collections.<Statement>singletonList(
                                        new AST.Throw(
                                                new AST.Invocation(
                                                        new AST.Access(
                                                                new AST.PackageRef("lt::runtime", LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                                                "MatchError",
                                                                LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                                        Collections.<Expression>emptyList(),
                                                        false,
                                                        LineCol.SYNTHETIC_WITH_FILE(fileName)
                                                ), LineCol.SYNTHETIC_WITH_FILE(fileName)
                                        )
                                ),
                                LineCol.SYNTHETIC_WITH_FILE(fileName));
                } else {
                        // invoke nextMethod
                        anElse = new AST.If.IfPair(
                                null,
                                Collections.<Statement>singletonList(
                                        new AST.Return(
                                                new AST.Invocation(
                                                        new AST.Access(null, nextMethod.name, LineCol.SYNTHETIC_WITH_FILE(fileName)),
                                                        Collections.<Expression>singletonList(
                                                                new AST.Access(null, "**", LineCol.SYNTHETIC_WITH_FILE(fileName))
                                                        ),
                                                        false,
                                                        LineCol.SYNTHETIC_WITH_FILE(fileName)
                                                ),
                                                LineCol.SYNTHETIC_WITH_FILE(fileName)
                                        )
                                ),
                                LineCol.SYNTHETIC_WITH_FILE(fileName));
                }
                // statements should be packed into `if condition` if the condition exists
                if (p.condition != null) {
                        statements = Collections.<Statement>singletonList(
                                new AST.If(
                                        Arrays.asList(
                                                new AST.If.IfPair(p.condition, statements, p.condition.line_col()),
                                                anElse
                                        ), LineCol.SYNTHETIC_WITH_FILE(fileName)
                                )
                        );
                }

                PatternMatchingParser patternMatchingParser;
                switch (p.pattern.patternType) {
                        case DEFAULT:
                                patternMatchingParser = new PatternMatchingDefaultParser();
                                break;
                        case TYPE:
                                patternMatchingParser = new PatternMatchingTypeParser();
                                break;
                        case VALUE:
                                patternMatchingParser = new PatternMatchingValueParser();
                                break;
                        case DEFINE:
                                patternMatchingParser = new PatternMatchingDefineParser();
                                break;
                        case DESTRUCT:
                                patternMatchingParser = new PatternMatchingDestructParser(-1);
                                break;
                        default:
                                throw new LtBug("unknown pattern matching type " + p.pattern.patternType);
                }
                currentMethod.body.addAll(patternMatchingParser.parse(
                        p.pattern, statements, anElse, "**", fileName
                ));
        }

        public Value parseValueFromGeneratorSpec(AST.GeneratorSpec gs, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
                STypeDef type = getTypeWithAccess(gs.type, getGenericMap(scope.type()), fileNameToImport.get(gs.line_col().fileName));
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
                        SourceGenerator sourceGen;
                        try {
                                sourceGen = (SourceGenerator) con.newInstance();
                        } catch (InvocationTargetException t) {
                                if (t.getTargetException() instanceof SyntaxException) {
                                        throw (SyntaxException) t.getTargetException();
                                } else {
                                        throw new LtBug("caught exception in generator", t);
                                }
                        } catch (Throwable e) {
                                throw new LtBug("caught exception in generator", e);
                        }
                        sourceGen.init(gs.ast, this, scope, gs.line_col(), err);

                        if (sourceGen.resultType() == SourceGenerator.EXPRESSION) {
                                Expression exp = (Expression) sourceGen.generate();
                                return parseValueFromExpression(exp, requiredType, scope);
                        } else if (sourceGen.resultType() == SourceGenerator.SERIALIZE) {
                                Serializable ser = (Serializable) sourceGen.generate();
                                String serStr = serializeObjectToString(ser);
                                return decodeSerExp(serStr, scope, gs.line_col());
                        } else if (sourceGen.resultType() == SourceGenerator.VALUE) {
                                return (Value) sourceGen.generate();
                        } else {
                                throw new LtBug("unknown resultType in " + c);
                        }
                } else {
                        err.SyntaxException("Generator should be a class", gs.line_col());
                        return null;
                }
        }

        private String serializeObjectToString(Serializable ser) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                        oos = new ObjectOutputStream(baos);
                        oos.writeObject(ser);
                } catch (IOException e) {
                        throw new LtBug(e);
                } finally {
                        if (oos != null) {
                                try {
                                        oos.close();
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
                return byte2hex(baos.toByteArray());
        }

        public Value decodeSerExp(String serStr, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                String ObjectInputStream_Name = ObjectInputStream.class.getName();
                String InputStream_Name = InputStream.class.getName();
                String ByteArrayInputStream_Name = ByteArrayInputStream.class.getName();
                SClassDef ObjectInputStream = (SClassDef) getTypeWithName(ObjectInputStream_Name, lineCol);
                assert ObjectInputStream != null;
                STypeDef InputStream = getTypeWithName(InputStream_Name, lineCol);
                assert InputStream != null;
                SClassDef ByteArrayInputStream = (SClassDef) getTypeWithName(ByteArrayInputStream_Name, lineCol);
                assert ByteArrayInputStream != null;
                STypeDef byte1Array = getTypeWithName("[B", lineCol);
                assert byte1Array != null;
                SClassDef StringCls = (SClassDef) getTypeWithName("java.lang.String", lineCol);
                assert StringCls != null;
                SClassDef SemanticProcessor = (SClassDef) getTypeWithName(lt.compiler.SemanticProcessor.class.getName(), lineCol);
                assert SemanticProcessor != null;

                SConstructorDef ObjectInputStream_con = null;
                // find constructor
                for (SConstructorDef con : ObjectInputStream.constructors()) {
                        if (con.getParameters().size() == 1 && con.getParameters().get(0).type().equals(InputStream)) {
                                // found constructor
                                ObjectInputStream_con = con;
                                break;
                        }
                }
                if (ObjectInputStream_con == null)
                        throw new LtBug(ObjectInputStream_Name + "(" + InputStream_Name + ") should exist");
                SMethodDef readObject = null;
                // find readObject method
                for (SMethodDef method : ObjectInputStream.methods()) {
                        if (method.name().equals("readObject") && method.getParameters().size() == 0) {
                                readObject = method;
                                break;
                        }
                }
                if (readObject == null) throw new LtBug(ObjectInputStream_Name + ".readObject() should exist");
                SConstructorDef ByteArrayInputStream_con = null;
                // find constructor
                for (SConstructorDef con : ByteArrayInputStream.constructors()) {
                        if (con.getParameters().size() == 1 && con.getParameters().get(0).type().equals(byte1Array)) {
                                ByteArrayInputStream_con = con;
                                break;
                        }
                }
                if (ByteArrayInputStream_con == null)
                        throw new LtBug(ByteArrayInputStream_Name + "(byte[]) should exist");
                // find hex2byte(String)
                SMethodDef hex2byte = null;
                for (SMethodDef method : SemanticProcessor.methods()) {
                        if (method.name().equals("hex2byte") && method.getParameters().size() == 1 && method.getParameters().get(0).type().equals(StringCls)) {
                                hex2byte = method;
                                break;
                        }
                }
                if (hex2byte == null)
                        throw new LtBug(lt.compiler.SemanticProcessor.class.getName() + ".hex2byte should exist");

                // do handling
                ValuePack vp = new ValuePack(true);
                // theSerValue
                StringConstantValue theSerValue = new StringConstantValue(serStr);
                theSerValue.setType(StringCls);
                // hex2byte(theSerValue)
                Ins.InvokeStatic invokeHex2byte = new Ins.InvokeStatic(hex2byte, lineCol);
                invokeHex2byte.arguments().add(theSerValue);
                // new ByteArrayInputStream(...)
                Ins.New aNew1 = new Ins.New(ByteArrayInputStream_con, lineCol);
                aNew1.args().add(invokeHex2byte);
                // new ObjectInputStream(...)
                Ins.New aNew2 = new Ins.New(ObjectInputStream_con, lineCol);
                aNew2.args().add(aNew1);
                // store
                LocalVariable local = new LocalVariable(aNew2.type(), false);
                scope.putLeftValue(scope.generateTempName(), local);
                Ins.TStore store = new Ins.TStore(local, aNew2, scope, lineCol, err);

                vp.instructions().add(store);

                // x.readObject()
                Ins.InvokeVirtual invokeReadObject = new Ins.InvokeVirtual(
                        new Ins.TLoad(local, scope, lineCol), readObject, lineCol);
                vp.instructions().add(invokeReadObject);
                return vp;
        }

        /**
         * parse value from new
         *
         * @param aNew  new
         * @param scope semantic scope
         * @return constructing a new object
         * @throws SyntaxException compiling error
         */
        public Value parseValueFromNew(AST.New aNew, SemanticScope scope) throws SyntaxException {
                SClassDef type;
                try {
                        type = (SClassDef) getTypeWithAccess((AST.Access) aNew.invocation.exp,
                                getGenericMap(scope.type()),
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
                List<Value> argList = new ArrayList<Value>();
                for (Expression e : aNew.invocation.args) {
                        argList.add(parseValueFromExpression(e, null, scope));
                }
                return constructingNewInst(type, argList, scope, aNew.line_col());
        }

        /**
         * {@link LtRuntime#getField(Object, String, Class)}
         */
        private SMethodDef Lang_require = null;

        /**
         * @return {@link LtRuntime#getField(Object, String, Class)}
         * @throws SyntaxException exception
         */
        public SMethodDef getLang_require() throws SyntaxException {
                if (Lang_require == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;

                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("require")) {
                                        Lang_require = m;
                                        break;
                                }
                        }
                }
                if (Lang_require == null)
                        throw new LtBug("lt.runtime.LtRuntime.require(Class,String) should exist");
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
        public ValuePack parseValueFromInvocationWithNames(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                STypeDef theType;
                try {
                        theType = getTypeWithAccess((AST.Access) invocation.exp,
                                getGenericMap(scope.type()),
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

                int count = 0;
                List<Value> constructingArgs = new ArrayList<Value>();
                for (Expression exp : invocation.args) {
                        if (!(exp instanceof VariableDef)
                                || ((VariableDef) exp).getInit() == null) {
                                ++count;
                                constructingArgs.add(
                                        parseValueFromExpression(exp, null, scope)
                                );
                        }
                }

                ValuePack valuePack = new ValuePack(true);
                valuePack.setType(theType);

                LocalVariable local = new LocalVariable(theType, false);
                String name = scope.generateTempName();
                scope.putLeftValue(name, local);

                Value aNew = constructingNewInst(classType, constructingArgs, scope, invocation.line_col());
                Ins.TStore store = new Ins.TStore(local, aNew, scope, invocation.line_col(), err);
                valuePack.instructions().add(store);

                for (int i = count; i < invocation.args.size(); ++i) {
                        Expression exp = invocation.args.get(i);
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
         * get interfaces defined in package `lt.lang.function`
         *
         * @param argCount argument count
         * @param lineCol  line-col
         * @return Function0 to Function 26
         * @throws SyntaxException exception
         */
        public SInterfaceDef getDefaultLambdaFunction(int argCount, LineCol lineCol) throws SyntaxException {
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
        public boolean getMethodForLambda(STypeDef requiredType,
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
                                        List<SClassDef> classes = new ArrayList<SClassDef>();
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
                                                Set<SInterfaceDef> interfaces = new HashSet<SInterfaceDef>();
                                                Queue<SInterfaceDef> q = new ArrayDeque<SInterfaceDef>();
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
                        Set<SInterfaceDef> interfaces = new HashSet<SInterfaceDef>();
                        Queue<SInterfaceDef> q = new ArrayDeque<SInterfaceDef>();
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
         * it creates a new class for the lambda<br>
         * <pre>
         * class LambdaClassName extends SomeFunctionalAbstractClass { // may be implements SomeFunctionalInterface
         *         Object o;
         *         List local;
         *         LambdaClassName(Object o, List local){
         *                 this.o=o;
         *                 this.local=local;
         *         }
         *         public Object theAbstractMethodToImpl(Object x,Object y) {
         *                 return o.someMethod(x, y, local.get(0), local.get(1), ... )
         *         }
         * }
         * </pre>
         *
         * @param lambda       lambda
         * @param requiredType required type
         * @param scope        current scope
         * @return return the new instance
         * @throws SyntaxException compile error
         * @see #buildAClassForLambda(STypeDef, boolean, SMethodDef, SConstructorDef, SInterfaceDef, boolean, int, SMethodDef)
         */
        public Value parseValueFromLambda(AST.Lambda lambda, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
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
                        Collections.<Modifier>emptySet(),
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
                        Collections.<AST.Anno>emptySet(),
                        lambda.statements,
                        LineCol.SYNTHETIC
                );
                SMethodDef innerMethod = parseInnerMethod(methodDef, scope, true);
                assert innerMethod != null;
                innerMethod.modifiers().remove(SModifier.PRIVATE); // it should be package access
                methodToStatements.put(innerMethod, lambda.statements);

                List<Ins.TLoad> args = new ArrayList<Ins.TLoad>();
                for (LeftValue l : scope.getLeftValues(innerMethod.getParameters().size() - lambda.params.size(), true)) {
                        args.add(new Ins.TLoad(l, scope, LineCol.SYNTHETIC));
                }

                if (constructorWithZeroParamAndCanAccess == null && !(requiredType instanceof SInterfaceDef))
                        throw new LtBug("constructorWithZeroParamAndCanAccess should not be null");
                // construct a class
                // class XXX(methodHandle:MethodHandle, o:Object, local:List) : C
                //     public self = this
                //     methodToOverride(xxx)
                //         local.add(?)
                //         ...
                //         methodHandle.invokeExact(o,local)

                // constructor arguments
                List<Value> consArgs = new ArrayList<Value>();
                // o
                if (scope.getThis() != null) consArgs.add(scope.getThis());
                // local
                Ins.NewMap newMap = new Ins.NewMap(getTypeWithName(HashMap.class.getName(), LineCol.SYNTHETIC));
                for (int index = 0; index < args.size(); ++index) {
                        Value arg = args.get(index);
                        if (!innerMethod.getParameters().get(index).isUsed()) {
                                continue;
                        }

                        // mark the variable as `captured by lambda`
                        scope.getMeta().pointerLocalVar.add(((Ins.TLoad) arg).value());

                        if (arg.type() instanceof PrimitiveTypeDef) {
                                arg = boxPrimitive(arg, LineCol.SYNTHETIC);
                        }
                        newMap.initValues().put(boxPrimitive(new IntValue(index), LineCol.SYNTHETIC), arg);
                }
                consArgs.add(newMap);

                boolean isInterface = requiredType instanceof SInterfaceDef;
                SClassDef builtClass = buildAClassForLambda(
                        scope.type(), scope.getThis() == null, methodToOverride,
                        constructorWithZeroParamAndCanAccess,
                        isInterface ? (SInterfaceDef) requiredType : null,
                        isInterface, args.size(),
                        innerMethod);

                SConstructorDef cons = builtClass.constructors().get(0);
                Ins.New aNew = new Ins.New(cons, LineCol.SYNTHETIC);
                aNew.args().addAll(consArgs);

                return aNew;
        }

        private SClassDef Object_Class;

        public SClassDef getObject_Class() throws SyntaxException {
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
         * @param localVarCount    local variable count
         * @param innerMethod      the inner method for the lambda to invoke
         * @return generated class (SClassDef form)
         * @throws SyntaxException compile error
         */
        public SClassDef buildAClassForLambda(STypeDef lambdaClassType, boolean isStatic, SMethodDef methodToOverride,
                                              SConstructorDef superConstructor,
                                              SInterfaceDef interfaceType,
                                              boolean isInterface, int localVarCount,
                                              SMethodDef innerMethod) throws SyntaxException {
                // class
                SClassDef sClassDef = new SClassDef(SClassDef.NORMAL, LineCol.SYNTHETIC);
                typeDefSet.add(sClassDef);
                SemanticScope scope = new SemanticScope(sClassDef, null);

                if (isInterface) {
                        sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        sClassDef.superInterfaces().add(interfaceType);
                } else {
                        sClassDef.setParent((SClassDef) methodToOverride.declaringType());
                }
                sClassDef.setPkg(lambdaClassType.pkg());
                String className = lambdaClassType.fullName() + "$Latte$Lambda$";
                int i = 0;
                while (typeExists(className + i)) ++i;
                className += i;
                sClassDef.setFullName(className);
                types.put(className, sClassDef);

                sClassDef.modifiers().add(SModifier.PUBLIC);

                // fields
                // o
                SFieldDef f2 = null;
                if (!isStatic) {
                        f2 = new SFieldDef(LineCol.SYNTHETIC);
                        f2.setName("o");
                        f2.setType(lambdaClassType);
                        sClassDef.fields().add(f2);
                        f2.setDeclaringType(sClassDef);
                }
                // local
                SFieldDef f3 = new SFieldDef(LineCol.SYNTHETIC);
                f3.setName("local");
                f3.setType(getTypeWithName("java.util.Map", LineCol.SYNTHETIC));
                sClassDef.fields().add(f3);
                f3.setDeclaringType(sClassDef);
                // self
                SFieldDef f4 = new SFieldDef(LineCol.SYNTHETIC);
                f4.setName("self");
                f4.setType(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                sClassDef.fields().add(f4);
                f4.setDeclaringType(sClassDef);
                f4.modifiers().add(SModifier.PUBLIC);

                // constructor
                SConstructorDef con = new SConstructorDef(LineCol.SYNTHETIC);
                sClassDef.constructors().add(con);
                con.setDeclaringType(sClassDef);
                SemanticScope conScope = new SemanticScope(scope, con.meta());
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
                // self = this
                con.statements().add(new Ins.PutField(
                        f4, new Ins.This(sClassDef),
                        new Ins.This(sClassDef), LineCol.SYNTHETIC, err));
                con.modifiers().add(SModifier.PUBLIC);
                // p2
                if (!isStatic) {
                        SParameter p2 = new SParameter();
                        p2.setType(lambdaClassType);
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
                p3.setType(getTypeWithName("java.util.Map", LineCol.SYNTHETIC));
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
                SemanticScope meScope = new SemanticScope(scope, theMethod.meta());
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
                // the lambda USED to be using MethodHandle to invoke the method
                // now it directly invokes that method
                List<Value> capturedValues = new ArrayList<Value>();
                // add local variables in list
                for (int index = 0; index < localVarCount; ++index) {
                        Ins.InvokeInterface ii = new Ins.InvokeInterface(
                                new Ins.GetField(f3, meScope.getThis(), LineCol.SYNTHETIC),
                                getMap_get(), LineCol.SYNTHETIC
                        );
                        ii.arguments().add(boxPrimitive(new IntValue(index), LineCol.SYNTHETIC));
                        Ins.CheckCast cc = new Ins.CheckCast(ii, innerMethod.getParameters().get(index).type(), LineCol.SYNTHETIC);
                        capturedValues.add(cc);
                }
                List<Value> methodArgs = new ArrayList<Value>();
                // add parameters
                for (SParameter p : theMethod.getParameters()) {
                        methodArgs.add(new Ins.TLoad(
                                p, meScope, LineCol.SYNTHETIC
                        ));
                }
                // add the functional object it self
                methodArgs.add(new Ins.GetField(f4, meScope.getThis(), LineCol.SYNTHETIC));
                Instruction theStmt;
                if (isStatic) {
                        Ins.InvokeWithCapture iwc = new Ins.InvokeWithCapture(null, innerMethod, true, LineCol.SYNTHETIC);
                        iwc.capturedArguments().addAll(capturedValues);
                        iwc.arguments().addAll(methodArgs);
                        theStmt = iwc;
                } else {
                        Ins.InvokeWithCapture iwc = new Ins.InvokeWithCapture(
                                new Ins.GetField(f2, meScope.getThis(), LineCol.SYNTHETIC),
                                innerMethod,
                                false, LineCol.SYNTHETIC
                        );
                        iwc.capturedArguments().addAll(capturedValues);
                        iwc.arguments().addAll(methodArgs);
                        theStmt = iwc;
                }
                // return if it's not `void`
                if (!theMethod.getReturnType().equals(VoidType.get())) {
                        theStmt = new Ins.TReturn((Value) theStmt, LineCol.SYNTHETIC);
                }
                theMethod.statements().add(theStmt);

                return sClassDef;
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
        public Value parseValueFromProcedure(AST.Procedure procedure, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
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
                        methodName, Collections.<Modifier>emptySet(),
                        new AST.Access(
                                requiredType.pkg() == null ? null : new AST.PackageRef(requiredType.pkg(), LineCol.SYNTHETIC),
                                requiredType.fullName().contains(".")
                                        ? requiredType.fullName().substring(requiredType.fullName().lastIndexOf('.') + 1)
                                        : requiredType.fullName(),
                                LineCol.SYNTHETIC
                        )
                        , Collections.<VariableDef>emptyList(), Collections.<AST.Anno>emptySet(),
                        procedure.statements, procedure.line_col()
                );
                parseInnerMethod(methodDef, scope, false);
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(null, methodName, procedure.line_col()), Collections.<Expression>emptyList(), false, procedure.line_col());
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
        public Value parseValueFromMapExp(AST.MapExp mapExp, SemanticScope scope) throws SyntaxException {
                Ins.NewMap newMap = new Ins.NewMap(getTypeWithName("java.util.LinkedHashMap", mapExp.line_col()));

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
        public Value parseValueFromArrayExp(AST.ArrayExp arrayExp, STypeDef requiredType, SemanticScope scope) throws SyntaxException {
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
                                getTypeWithName("java.util.LinkedList", arrayExp.line_col())
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
        public ValuePack parseValueFromAssignment(AST.Assignment exp, SemanticScope scope) throws SyntaxException {
                ValuePack pack = new ValuePack(true); // value pack
                // assignment
                // =/+=/-=/*=//=/%=
                Value assignFrom = parseValueFromExpression(exp.assignFrom, null, scope);
                Value assignTo;
                if (exp.assignTo.name == null) {
                        assignTo = parseValueFromExpression(exp.assignTo, null, scope);
                } else {
                        assignTo = __parseValueFromAccess(exp.assignTo, scope, true);
                }
                if (!exp.op.equals("=")) {
                        assignFrom = parseValueFromTwoVarOp(
                                parseValueFromExpression(exp.assignTo, null, scope), // the `assignTo` is calculated, so it should be final extracted value.
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
         * {@link Unit#get()}
         */
        private SMethodDef Unit_get;

        /**
         * invoke {@link Unit#get()}
         *
         * @param lineCol caller's line and column
         * @return InvokeStatic
         * @throws SyntaxException exception
         */
        public Ins.InvokeStatic invoke_Unit_get(LineCol lineCol) throws SyntaxException {
                if (Unit_get == null) {
                        SClassDef UndefiendClass = (SClassDef) getTypeWithName("lt.lang.Unit", lineCol);
                        assert UndefiendClass != null;
                        for (SMethodDef m : UndefiendClass.methods()) {
                                if (m.name().equals("get")) {
                                        Unit_get = m;
                                        break;
                                }
                        }
                        Unit_get.setReturnType(getTypeWithName("lt.lang.Unit", lineCol));
                }
                return new Ins.InvokeStatic(Unit_get, lineCol);
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
        public Value parseValueFromTwoVarOp(TwoVariableOperation tvo, SemanticScope scope) throws SyntaxException {
                String op = tvo.operator();
                Value left = parseValueFromExpression(tvo.expressions().get(0), null, scope);
                Value right = parseValueFromExpression(tvo.expressions().get(1), null, scope);
                return parseValueFromTwoVarOp(left, op, right, scope, tvo.line_col());
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
        public Value invokeMethodWithArgs(LineCol lineCol, STypeDef targetType, Value invokeOn, String methodName, List<Value> args, SemanticScope scope) throws SyntaxException {
                List<SMethodDef> methods = new ArrayList<SMethodDef>();
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
                        return invoke_Dynamic_invoke(
                                targetType,
                                invokeOn,
                                false,
                                NullValue.get(),
                                scope.type(),
                                methodName,
                                args, false,
                                lineCol
                        );
                } else {
                        SMethodDef method = findBestMatch(args, methods, lineCol);
                        args = castArgsForMethodInvoke(args, method.getParameters(), lineCol);
                        if (method.modifiers().contains(SModifier.STATIC)) {
                                // invoke static
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                                invokeStatic.arguments().addAll(args);

                                if (invokeStatic.type().equals(VoidType.get())) {
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
        public Value parseValueFromTwoVarOpILFD(Value left, int baseOp, String methodName, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (left.type() instanceof PrimitiveTypeDef) {
                        if (right.type() instanceof PrimitiveTypeDef) {
                                if (left.type().equals(DoubleTypeDef.get()) || right.type().equals(DoubleTypeDef.get())) {
                                        // cast to double
                                        Value a = cast(DoubleTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(DoubleTypeDef.get(), right, scope.type(), lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 3, DoubleTypeDef.get(), lineCol);
                                } else if (left.type().equals(FloatTypeDef.get()) || right.type().equals(FloatTypeDef.get())) {
                                        // cast to float
                                        Value a = cast(FloatTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(FloatTypeDef.get(), right, scope.type(), lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 2, FloatTypeDef.get(), lineCol);
                                } else if (left.type().equals(LongTypeDef.get()) || right.type().equals(LongTypeDef.get())) {
                                        // cast to long
                                        Value a = cast(LongTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(LongTypeDef.get(), right, scope.type(), lineCol);
                                        return new Ins.TwoVarOp(a, b, baseOp + 1, LongTypeDef.get(), lineCol);
                                } else {
                                        if ((baseOp == Ins.TwoVarOp.Iand || baseOp == Ins.TwoVarOp.Ior || baseOp == Ins.TwoVarOp.Ixor)
                                                && left.type().equals(BoolTypeDef.get()) && right.type().equals(BoolTypeDef.get())) {
                                                return new Ins.TwoVarOp(left, right, baseOp, BoolTypeDef.get(), lineCol);
                                        } else {
                                                // cast to int
                                                Value a = cast(IntTypeDef.get(), left, scope.type(), lineCol);
                                                Value b = cast(IntTypeDef.get(), right, scope.type(), lineCol);
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
                        List<Value> args = new ArrayList<Value>();
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
        public SMethodDef getLang_compare() throws SyntaxException {
                if (Lang_compare == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
        public SMethodDef getComparable_compareTo() throws SyntaxException {
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
         * @param compare_mode compare_mode {@link #COMPARE_MODE_EQ} {@link #COMPARE_MODE_GT} {@link #COMPARE_MODE_LT}
         * @param methodName   if requires method invocation, use this method to invoke
         * @param right        the value of the right of the operator
         * @param scope        current scope
         * @param lineCol      line column info
         * @return the result
         * @throws SyntaxException compile error
         */
        public Value parseValueFromTwoVarOpCompare(Value left, int compare_mode, String methodName, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (left.type() instanceof PrimitiveTypeDef) {
                        if (right.type() instanceof PrimitiveTypeDef) {
                                Ins.TwoVarOp twoVarOp;
                                if (left.type().equals(DoubleTypeDef.get()) || right.type().equals(DoubleTypeDef.get())) {
                                        // cast to double
                                        Value a = cast(DoubleTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(DoubleTypeDef.get(), right, scope.type(), lineCol);
                                        twoVarOp = new Ins.TwoVarOp(a, b, Ins.TwoVarOp.Dcmpg, IntTypeDef.get(), lineCol);
                                } else if (left.type().equals(FloatTypeDef.get()) || right.type().equals(FloatTypeDef.get())) {
                                        // cast to float
                                        Value a = cast(FloatTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(FloatTypeDef.get(), right, scope.type(), lineCol);
                                        twoVarOp = new Ins.TwoVarOp(a, b, Ins.TwoVarOp.Fcmpg, IntTypeDef.get(), lineCol);
                                } else {
                                        // cast to long
                                        Value a = cast(LongTypeDef.get(), left, scope.type(), lineCol);
                                        Value b = cast(LongTypeDef.get(), right, scope.type(), lineCol);
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
                                if (right.type() instanceof PrimitiveTypeDef) {
                                        right = boxPrimitive(right, lineCol);
                                }
                                invokeInterface.arguments().add(right);

                                // LtRuntime.compare(left.compareTo(right), compare_mode)
                                SMethodDef compare = getLang_compare();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(compare, lineCol);
                                invokeStatic.arguments().add(invokeInterface);
                                invokeStatic.arguments().add(new IntValue(compare_mode));
                                return invokeStatic;
                        } else {
                                List<Value> args = new ArrayList<Value>();
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
        public SMethodDef getLang_compareRef() throws SyntaxException {
                if (Lang_compareRef == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
         * if a &gt; b then return true.
         */
        public static final int COMPARE_MODE_GT = 1; // 0b001
        /**
         * if a == b then return true.
         */
        public static final int COMPARE_MODE_EQ = 2; // 0b010
        /**
         * if a &lt; b then return true.
         */
        public static final int COMPARE_MODE_LT = 4; // 0b100

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
        public Value parseValueFromTwoVarOp(Value left, String op, Value right, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (op.equals(":::")) {
                        List<Value> arg = new ArrayList<Value>();
                        arg.add(right);
                        return invokeMethodWithArgs(lineCol, left.type(), left, "concat", arg, scope);
                } else if (op.equals("^^")) {
                        List<Value> arg;
                        arg = new ArrayList<Value>();
                        arg.add(right);
                        if (left.type() instanceof PrimitiveTypeDef) {
                                left = boxPrimitive(left, LineCol.SYNTHETIC);
                        }
                        return invokeMethodWithArgs(lineCol, left.type(), left, "pow", arg, scope);
                } else if (op.equals("*")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Imul, "multiply", right, scope, lineCol);
                } else if (op.equals("/")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Idiv, "divide", right, scope, lineCol);
                } else if (op.equals("%")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Irem, "remainder", right, scope, lineCol);
                } else if (op.equals("+")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iadd, "add", right, scope, lineCol);
                } else if (op.equals("-")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Isub, "subtract", right, scope, lineCol);
                } else if (op.equals("<<")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishl, "shiftLeft", right, scope, lineCol);
                } else if (op.equals(">>")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishr, "shiftRight", right, scope, lineCol);
                } else if (op.equals(">>>")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iushr, "unsignedShiftRight", right, scope, lineCol);
                } else if (op.equals(">")) {
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_GT, "gt", right, scope, lineCol);
                } else if (op.equals("<")) {
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_LT, "lt", right, scope, lineCol);
                } else if (op.equals(">=")) {
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_GT | COMPARE_MODE_EQ, "ge", right, scope, lineCol);
                } else if (op.equals("<=")) {
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_LT | COMPARE_MODE_EQ, "le", right, scope, lineCol);
                } else if (op.equals("==")) {// null check
                        if (left.equals(NullValue.get()) || right.equals(NullValue.get())) {
                                if (right.equals(NullValue.get())) {
                                        Value tmp = left;
                                        left = right;
                                        right = tmp;
                                }
                                return parseValueFromTwoVarOp(left, "is", right, scope, lineCol);
                        }
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_EQ, "eq", right, scope, lineCol);
                } else if (op.equals("!=")) {// null check
                        if (left.equals(NullValue.get()) || right.equals(NullValue.get())) {
                                if (right.equals(NullValue.get())) {
                                        Value tmp = left;
                                        left = right;
                                        right = tmp;
                                }
                                return parseValueFromTwoVarOp(left, "not", right, scope, lineCol);
                        }
                        return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_GT | COMPARE_MODE_LT, "ne", right, scope, lineCol);
                } else if (op.equals("===")) {
                        if (left.type() instanceof PrimitiveTypeDef && right.type() instanceof PrimitiveTypeDef) {
                                return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_EQ, null, right, scope, lineCol);
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
                } else if (op.equals("!==")) {
                        if (left.type() instanceof PrimitiveTypeDef && right.type() instanceof PrimitiveTypeDef) {
                                return parseValueFromTwoVarOpCompare(left, COMPARE_MODE_LT | COMPARE_MODE_GT, null, right, scope, lineCol);
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
                } else if (op.equals("is")) {
                        if (right instanceof Ins.GetClass) {
                                // is type XXX
                                // use instance of
                                if (left.type() instanceof PrimitiveTypeDef) {
                                        left = boxPrimitive(left, lineCol);
                                }
                                return new Ins.InstanceOf(left, (Ins.GetClass) right, lineCol);
                        } else {
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
                } else if (op.equals("not")) {
                        if (right instanceof Ins.GetClass) {
                                // not type XXX
                                // use instanceof
                                if (left.type() instanceof PrimitiveTypeDef) {
                                        left = boxPrimitive(left, lineCol);
                                }
                                return new Ins.TwoVarOp(
                                        new Ins.TwoVarOp(
                                                new Ins.InstanceOf(left, (Ins.GetClass) right, lineCol),
                                                new IntValue(1),
                                                Ins.TwoVarOp.Iand, BoolTypeDef.get(),
                                                lineCol),
                                        new IntValue(1),
                                        Ins.TwoVarOp.Ixor,
                                        BoolTypeDef.get(),
                                        lineCol);
                        } else {
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
                } else if (op.equals("in")) {
                        List<Value> args = new ArrayList<Value>();
                        args.add(left);
                        if (right.type() instanceof PrimitiveTypeDef) {
                                right = boxPrimitive(right, lineCol);
                        }
                        return invokeMethodWithArgs(lineCol, right.type(), right, "contains", args, scope);
                } else if (op.equals("&")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iand, "and", right, scope, lineCol);
                } else if (op.equals("^")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ixor, "xor", right, scope, lineCol);
                } else if (op.equals("|")) {
                        return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ior, "or", right, scope, lineCol);
                } else if (op.equals("&&") || op.equals("and")) {// logic and with short cut
                        return new Ins.LogicAnd(
                                cast(BoolTypeDef.get(), left, scope.type(), lineCol),
                                cast(BoolTypeDef.get(), right, scope.type(), lineCol),
                                lineCol);
                } else if (op.equals("||") || op.equals("or")) {// logic or with short cut
                        if (left.type() instanceof PrimitiveTypeDef) {
                                left = boxPrimitive(left, lineCol);
                        }
                        if (right.type() instanceof PrimitiveTypeDef) {
                                right = boxPrimitive(right, lineCol);
                        }
                        return new Ins.LogicOr(
                                getLang_castToBool(),
                                left, right,
                                getCommonParent(left.type(), right.type()),
                                lineCol
                        );
                } else if (op.equals(":=")) {
                        List<Value> args;// assign
                        args = new ArrayList<Value>();
                        args.add(right);
                        return invokeMethodWithArgs(lineCol, left.type(), left, "assign", args, scope);
                } else {
                        err.SyntaxException("unknown two variable operator " + op, lineCol);
                        return null;
                }
        }

        /**
         * get common parent type
         *
         * @param type1 type1
         * @param type2 type2
         * @return the common super type of type1 and type2. the super type is type1 or type2 or java.lang.Object or super class of type1/type2
         * @throws SyntaxException compiling error
         */
        public STypeDef getCommonParent(STypeDef type1, STypeDef type2) throws SyntaxException {
                if (type1.equals(type2)) return type1;
                if (type1.isAssignableFrom(type2)) return type1;
                if (type2.isAssignableFrom(type1)) return type2;
                if (type1 instanceof SClassDef && type2 instanceof SClassDef) {
                        SClassDef c1 = (SClassDef) type1;
                        SClassDef c2 = (SClassDef) type2;
                        while (c1 != null) {
                                if (c1.isAssignableFrom(c2)) return c1;
                                c1 = c1.parent();
                        }
                }
                return getTypeWithName("java.lang.Object", LineCol.SYNTHETIC);
        }

        /**
         * {@link LtRuntime#is(Object, Object, Class)}
         */
        private SMethodDef Lang_is;

        /**
         * @return {@link LtRuntime#is(Object, Object, Class)}
         * @throws SyntaxException exception
         */
        public SMethodDef getLang_is() throws SyntaxException {
                if (Lang_is == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
        public SMethodDef getLang_not() throws SyntaxException {
                if (Lang_not == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
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
        public Value parseSelfOneVarOp(Operation exp, SemanticScope scope) throws SyntaxException {
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

                                Value v = parseValueFromAccess((AST.Access) e, scope, true);
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

                                Value v = parseValueFromAccess((AST.Access) e, scope, true);
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
        public Value parseValueFromOneVarOp(Operation exp, SemanticScope scope) throws SyntaxException {
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
                                v = cast(BoolTypeDef.get(), v, scope.type(), exp.line_col());
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
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "logicNot", new ArrayList<Value>(), scope);
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
                                v = cast(IntTypeDef.get(), v, scope.type(), exp.line_col());
                                return new Ins.TwoVarOp(
                                        v,
                                        new IntValue(-1),
                                        Ins.TwoVarOp.Ixor,
                                        IntTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof LongTypeDef || v.type().fullName().equals("java.lang.Long")) {
                                v = cast(LongTypeDef.get(), v, scope.type(), exp.line_col());
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
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "not", new ArrayList<Value>(), scope);
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
                                v = cast(IntTypeDef.get(), v, scope.type(), exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Ineg,
                                        IntTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof LongTypeDef || v.type().fullName().equals("java.lang.Long")) {
                                v = cast(LongTypeDef.get(), v, scope.type(), exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Lneg,
                                        LongTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof FloatTypeDef || v.type().fullName().equals("java.lang.Float")) {
                                v = cast(FloatTypeDef.get(), v, scope.type(), exp.line_col());
                                return new Ins.OneVarOp(
                                        v,
                                        Ins.OneVarOp.Fneg,
                                        FloatTypeDef.get(),
                                        exp.line_col());
                        } else if (v.type() instanceof DoubleTypeDef || v.type().fullName().equals("java.lang.Double")) {
                                v = cast(DoubleTypeDef.get(), v, scope.type(), exp.line_col());
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
                                return invokeMethodWithArgs(exp.line_col(), v.type(), v, "negate", new ArrayList<Value>(), scope);
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
        public List<Value> parseArguments(List<Expression> args, SemanticScope scope) throws SyntaxException {
                List<Value> list = new ArrayList<Value>();
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
        public Value parseValueFromIndex(AST.Index index, SemanticScope scope) throws SyntaxException {
                Value v = parseValueFromExpression(index.exp, null, scope);
                assert v != null;

                List<Value> list = parseArguments(index.args, scope);

                assert list != null;
                if (v.type() instanceof SArrayTypeDef && !list.isEmpty()) {
                        try {
                                Value result = v;
                                for (int ii = 0; ii < list.size(); ++ii) {
                                        Value i = cast(IntTypeDef.get(), list.get(ii), scope.type(), index.args.get(ii).line_col());
                                        result = new Ins.TALoad(result, i, index.line_col(), getTypes());
                                }
                                return result;
                        } catch (Throwable ignore) {
                                // cast failed
                        }
                }
                // not array
                // do invoke
                return invokeMethodWithArgs(
                        index.line_col(),
                        v.type(),
                        v,
                        "get",
                        list,
                        scope);
        }

        /**
         * parse value from access (the access represents a type).
         *
         * @param access      the type
         * @param imports     imports
         * @param currentType caller type
         * @return New instruction or get_static
         * @throws SyntaxException exception
         */
        public Value parseValueFromAccessType(AST.Access access, List<Import> imports, STypeDef currentType) throws SyntaxException {
                SClassDef type = (SClassDef) getTypeWithAccess(access, getGenericMap(currentType), imports);
                assert type != null;
                SConstructorDef zeroParamCons = null;
                for (SConstructorDef c : type.constructors()) {
                        if (c.getParameters().isEmpty()) {
                                if (c.modifiers().contains(SModifier.PRIVATE)) {
                                        if (!type.equals(currentType) && type.classType() != SClassDef.OBJECT) continue;
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
                } else if (type.classType() == SClassDef.OBJECT) {
                        SFieldDef singletonInstance = null;
                        for (SFieldDef f : type.fields()) {
                                if (f.name().equals(CompileUtil.SingletonFieldName)) {
                                        singletonInstance = f;
                                        break;
                                }
                        }
                        if (singletonInstance == null)
                                throw new LtBug("object class should have field " + CompileUtil.SingletonFieldName);
                        return new Ins.GetStatic(singletonInstance, access.line_col());
                } else {
                        return new Ins.New(zeroParamCons, access.line_col());
                }
        }

        /**
         * proxy for parsing value from access {@link #__parseValueFromAccess(AST.Access, SemanticScope, boolean)}
         *
         * @param access           access
         * @param scope            scope
         * @param isTryingToAssign isTryingToAssign
         * @return result that will extract value from the object if it's a pointer
         * @throws SyntaxException compiling error
         */
        public Value parseValueFromAccess(AST.Access access, SemanticScope scope, boolean isTryingToAssign) throws SyntaxException {
                Value v = __parseValueFromAccess(access, scope, isTryingToAssign);
                assert v != null;
                if (isPointerType(v.type())) {
                        v = invokePointerGet(v, access.line_col());
                }
                return v;
        }

        /**
         * parse value from access object<br>
         * the access object can be : (null,fieldName),(null,localVariableName),(this,fieldName),(Type,fieldName),((Type,this),fieldName),(exp,fieldName)
         *
         * @param access           access object
         * @param scope            scope that contains localvariables
         * @param isTryingToAssign the value is retrieved to assign new value
         * @return retrieved value can be getField/getStatic/TLoad/arraylength
         * @throws SyntaxException compiling error
         */
        private Value __parseValueFromAccess(AST.Access access, SemanticScope scope, boolean isTryingToAssign) throws SyntaxException {
                // get generic type map
                Map<String, STypeDef> genericTypeMap = scope == null ? Collections.<String, STypeDef>emptyMap() : getGenericMap(scope.type());
                // handle
                access = transformAccess(access);
                List<Import> imports = fileNameToImport.get(access.line_col().fileName);
                if (access.exp == null) {
                        // Access(null,name)
                        assert scope != null;
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
                                                                getTypeWithAccess(im.access, genericTypeMap, imports),
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
                                if (isTryingToAssign) {
                                        // check whether it's final
                                        if (!v.canChange()) {
                                                err.SyntaxException("cannot assign an immutable variable", access.line_col());
                                        }
                                }
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
                                        assert scope != null;
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
                                        STypeDef type = getTypeWithAccess((AST.Access) access1.exp, genericTypeMap, imports);
                                        assert type != null;

                                        assert scope != null;
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
                                        assert scope != null;
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
                                        type = getTypeWithAccess((AST.Access) access.exp, genericTypeMap, imports);
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
                                        else {
                                                assert scope != null;
                                                return invokeGetField(v, access.name, scope.type(), access.line_col());
                                        }
                                } else {
                                        // check primitive
                                        if (v.type() instanceof PrimitiveTypeDef) {
                                                v = boxPrimitive(v, access.line_col());
                                        }
                                        assert scope != null;
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
        public Ins.InvokeStatic invokeGetField(Value target, String name, STypeDef callerClass, LineCol lineCol) throws SyntaxException {
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
        public SMethodDef getLang_getField() throws SyntaxException {
                if (Lang_getField == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
                        assert Lang != null;

                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("getField")) {
                                        Lang_getField = m;
                                        break;
                                }
                        }
                }
                if (Lang_getField == null)
                        throw new LtBug("lt.runtime.LtRuntime.getField(Object,String,Class) should exist");
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
        public SFieldDef findFieldFromTypeDef(String fieldName, STypeDef targetType, STypeDef callerType, int mode, boolean checkSuper) {
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
        public SFieldDef findFieldFromClassDef(String fieldName, SClassDef theClass, STypeDef type, int mode, boolean checkSuper) {
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
        public SFieldDef findFieldFromInterfaceDef(String fieldName, SInterfaceDef theInterface, boolean checkSuper) {
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
         * @param callerClass  callerClass
         * @param lineCol      file_line_col
         * @return casted value
         * @throws SyntaxException exception
         */
        public Value cast(STypeDef requiredType, Value v, STypeDef callerClass, LineCol lineCol) throws SyntaxException {
                if (requiredType != null && isPointerType(requiredType)) {
                        requiredType = getPointingType(requiredType);
                }

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
                                resultVal = castObjToObj(requiredType, v, callerClass, lineCol);
                        } else {
                                // cast object to object
                                resultVal = castObjToObj(requiredType, v, callerClass, lineCol);
                        }
                }
                return new Ins.CheckCast(resultVal, requiredType, lineCol);
        }

        /**
         * invoke {@link LtRuntime#cast(Object, Class, Class)}<br>
         * note that the result object is always `java.lang.Object` when compiling,<br>
         * use {@link lt.compiler.semantic.Ins.CheckCast} to cast to required type to avoid some error when runtime validates the class file
         *
         * @param type        2nd arg
         * @param v           1st arg
         * @param callerClass 3rd arg
         * @param lineCol     line column info
         * @return casted value
         * @throws SyntaxException exception
         */
        public Value castObjToObj(STypeDef type, Value v, STypeDef callerClass, LineCol lineCol) throws SyntaxException {
                SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", lineCol);
                assert Lang != null;

                SMethodDef method = null;
                for (SMethodDef m : Lang.methods()) {
                        if (m.name().equals("cast")) {
                                method = m;
                                break;
                        }
                }
                if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToInt(Object,Class,Class) should exist");
                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                invokeStatic.arguments().add(v);
                invokeStatic.arguments().add(
                        new Ins.GetClass(
                                type,
                                (SClassDef) getTypeWithName("java.lang.Class", lineCol))
                );
                invokeStatic.arguments().add(
                        callerClass == null
                                ? NullValue.get()
                                : new Ins.GetClass(callerClass, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC))
                );
                return invokeStatic;
        }

        /**
         * invoke castToX methods defined in lt.runtime.LtRuntime
         *
         * @param type    the primitive type
         * @param v       value to cast
         * @param lineCol line and column info
         * @return casted value
         * @throws SyntaxException exception
         */
        public Value castObjToPrimitive(PrimitiveTypeDef type, Value v, LineCol lineCol) throws SyntaxException {
                SClassDef Lang = (SClassDef) getTypeWithName("lt.runtime.LtRuntime", LineCol.SYNTHETIC);
                assert Lang != null;

                SMethodDef method = null;
                if (type instanceof IntTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToInt")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToInt(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToLong(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToShort(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToByte(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToFloat(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToDouble(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else if (type instanceof BoolTypeDef) {
                        method = getLang_castToBool();
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToBool(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.runtime.LtRuntime.castToChar(Object) should exist");
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
         * @throws SyntaxException compiling error
         */
        public boolean whetherTheMethodIsOverriddenByMethodsInTheList(SMethodDef method, List<SMethodDef> methodList) throws SyntaxException {
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
        public static final int FIND_MODE_ANY = 0;
        /**
         * only search for static
         */
        public static final int FIND_MODE_STATIC = 1;
        /**
         * only search for non-static
         */
        public static final int FIND_MODE_NON_STATIC = 2;

        /**
         * find method from interface and it's super interfaces
         *
         * @param name           method name
         * @param argList        argument list
         * @param sInterfaceDef  interface definition(where to find method from)
         * @param mode           find_method_mode {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param matchedMethods matched methods
         * @param checkSuper     whether to check super types
         * @throws SyntaxException compiling error
         */
        public void findMethodFromInterfaceWithArguments(String name, List<Value> argList, SInterfaceDef sInterfaceDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
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
         * @throws SyntaxException compiling error
         */
        public void findMethodFromClassWithArguments(String name, List<Value> argList, STypeDef invokeOn, SClassDef sClassDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
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
        public void findMethodFromTypeWithArguments(LineCol lineCol, String name, List<Value> argList, STypeDef invokeOn, STypeDef sTypeDef, int mode, List<SMethodDef> matchedMethods, boolean checkSuper) throws SyntaxException {
                if (name.equals("clone") && sTypeDef.fullName().equals("java.lang.Object")) {
                        // ignore any invocation on `Object#clone` at compile time
                        return;
                }
                if (sTypeDef instanceof SClassDef) {
                        findMethodFromClassWithArguments(name, argList, invokeOn, (SClassDef) sTypeDef, mode, matchedMethods, checkSuper);
                } else if (sTypeDef instanceof SInterfaceDef) {
                        findMethodFromInterfaceWithArguments(name, argList, (SInterfaceDef) sTypeDef, mode, matchedMethods, checkSuper);
                } else if (sTypeDef instanceof SAnnoDef) {
                        if (argList.size() != 0) {
                                err.SyntaxException("invoking methods in annotation should contain no arguments", lineCol);
                                return;
                        }
                        for (SAnnoField f : ((SAnnoDef) sTypeDef).annoFields()) {
                                if (f.name().equals(name)) {
                                        matchedMethods.add(f);
                                }
                        }
                } else throw new LtBug("sTypeDef can only be SClassDef or SInterfaceDef or SAnnoDef");
        }

        /**
         * constructing new instances
         *
         * @param classDef class definition
         * @param argList  argument list
         * @param scope    current scope
         * @param lineCol  line col
         * @return {@link lt.compiler.semantic.Ins.New} or {@link lt.compiler.semantic.Ins.InvokeStatic}
         * @throws SyntaxException exception
         */
        public Value constructingNewInst(SClassDef classDef, List<Value> argList, SemanticScope scope, LineCol lineCol) throws SyntaxException {
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
                return invoke_Dynamic_construct(classDef, scope.type(), argList, lineCol);
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
        public Value parseValueFromInvocationFunctionalObject(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                Expression exp = invocation.exp;
                Value possibleFunctionalObject = parseValueFromExpression(exp, null, scope);

                List<Value> arguments = new ArrayList<Value>();
                for (Expression e : invocation.args) {
                        arguments.add(parseValueFromExpression(e, null, scope));
                }

                return callFunctionalObject(possibleFunctionalObject, scope.type(), arguments, invocation.line_col());
        }

        private SClassDef DYNAMIC_CLASS;

        public SClassDef getDynamicClass() throws SyntaxException {
                if (DYNAMIC_CLASS == null) {
                        DYNAMIC_CLASS = (SClassDef) getTypeWithName(DYNAMIC_CLASS_NAME, LineCol.SYNTHETIC);
                }
                return DYNAMIC_CLASS;
        }

        private SMethodDef DYNAMIC_callFunctionalObject;

        public SMethodDef getDYNAMIC_callFunctionalObject() throws SyntaxException {
                if (DYNAMIC_callFunctionalObject == null) {
                        SClassDef DYNAMIC = getDynamicClass();
                        for (SMethodDef m : DYNAMIC.methods()) {
                                if (m.name().equals("callFunctionalObject") && m.getParameters().size() == 3) {
                                        DYNAMIC_callFunctionalObject = m;
                                        break;
                                }
                        }
                }
                return DYNAMIC_callFunctionalObject;
        }

        private SMethodDef DYNAMIC_invoke;

        public SMethodDef getDYNAMIC_invoke() throws SyntaxException {
                if (DYNAMIC_invoke == null) {
                        SClassDef DYNAMIC = getDynamicClass();
                        for (SMethodDef m : DYNAMIC.methods()) {
                                if (m.name().equals("invoke") && m.getParameters().size() == 9) {
                                        DYNAMIC_invoke = m;
                                        break;
                                }
                        }
                }
                return DYNAMIC_invoke;
        }

        private SMethodDef DYNAMIC_construct;

        public SMethodDef getDYNAMIC_construct() throws SyntaxException {
                if (DYNAMIC_construct == null) {
                        SClassDef DYNAMIC = getDynamicClass();
                        for (SMethodDef m : DYNAMIC.methods()) {
                                if (m.name().equals("construct") && m.getParameters().size() == 4) {
                                        DYNAMIC_construct = m;
                                        break;
                                }
                        }
                }
                return DYNAMIC_construct;
        }

        /**
         * call a functional object
         *
         * @param object      the functional object
         * @param callerClass caller class
         * @param arguments   arguments
         * @param lineCol     lineCol
         * @return the invocation
         * @throws SyntaxException compiling error
         */
        public Value callFunctionalObject(Value object, STypeDef callerClass, List<Value> arguments, LineCol lineCol) throws SyntaxException {
                Ins.InvokeStatic is = new Ins.InvokeStatic(
                        getDYNAMIC_callFunctionalObject(), lineCol
                );
                is.arguments().add(object);
                is.arguments().add(new Ins.GetClass(callerClass, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                is.arguments().add(packListValuesIntoObjectArray(arguments));
                return is;
        }

        /**
         * pack a list of values into []Object
         *
         * @param values values to pack
         * @return a value represents the object array
         * @throws SyntaxException compiling error
         */
        private Value packListValuesIntoObjectArray(List<Value> values) throws SyntaxException {
                Ins.ANewArray aNewArray = new Ins.ANewArray(
                        (SArrayTypeDef) getTypeWithName("[Ljava.lang.Object;", LineCol.SYNTHETIC),
                        getTypeWithName("java.lang.Object", LineCol.SYNTHETIC),
                        new IntValue(values.size()));
                for (Value v : values) {
                        if (v.type() instanceof PrimitiveTypeDef) {
                                v = boxPrimitive(v, LineCol.SYNTHETIC);
                        }
                        aNewArray.initValues().add(v);
                }
                return aNewArray;
        }

        /**
         * pack a list of values into []boolean
         *
         * @param values values to pack
         * @return a value represents the object array
         * @throws SyntaxException compiling error
         */
        private Value packListValuesIntoBooleanArray(List<Value> values) throws SyntaxException {
                Ins.NewArray newArray = new Ins.NewArray(
                        new IntValue(values.size()), Ins.NewArray.NewBoolArray, Ins.TAStore.BASTORE,
                        getTypeWithName("[B", LineCol.SYNTHETIC)
                );
                newArray.initValues().addAll(values);
                return newArray;
        }

        private Ins.InvokeStatic invoke_Dynamic_invoke(STypeDef targetClass, Value o, boolean isStatic, Value functionalObject,
                                                       STypeDef invoker, String method, List<Value> args, boolean canInvokeImport,
                                                       LineCol lineCol) throws SyntaxException {
                Ins.InvokeStatic is = new Ins.InvokeStatic(
                        getDYNAMIC_invoke(), lineCol
                );
                is.arguments().add(new Ins.GetClass(targetClass, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                is.arguments().add(o);
                is.arguments().add(new BoolValue(isStatic));
                is.arguments().add(functionalObject);
                is.arguments().add(new Ins.GetClass(invoker, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                is.arguments().add(new StringConstantValue(method));
                List<Value> primitives = new ArrayList<Value>();
                for (Value a : args) {
                        primitives.add(new BoolValue(a.type() instanceof PrimitiveTypeDef));
                }
                is.arguments().add(packListValuesIntoBooleanArray(primitives));
                is.arguments().add(packListValuesIntoObjectArray(args));
                is.arguments().add(new BoolValue(canInvokeImport));
                return is;
        }

        private Ins.InvokeStatic invoke_Dynamic_construct(STypeDef targetClass, STypeDef invoker,
                                                          List<Value> args,
                                                          LineCol lineCol) throws SyntaxException {
                Ins.InvokeStatic is = new Ins.InvokeStatic(
                        getDYNAMIC_construct(), lineCol
                );
                is.arguments().add(new Ins.GetClass(targetClass, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                is.arguments().add(new Ins.GetClass(invoker, (SClassDef) getTypeWithName("java.lang.Class", LineCol.SYNTHETIC)));
                List<Value> primitives = new ArrayList<Value>();
                for (Value v : args) {
                        primitives.add(new BoolValue(v.type() instanceof PrimitiveTypeDef));
                }
                is.arguments().add(packListValuesIntoBooleanArray(primitives));
                is.arguments().add(packListValuesIntoObjectArray(args));
                return is;
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
         * <li>construct an object -- {@link #getTypeWithAccess(AST.Access, Map, List)}</li>
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
        public Value parseValueFromInvocation(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                // generic type map
                Map<String, STypeDef> genericTypeMap = getGenericMap(scope.type());
                // parse args
                List<Value> argList = new ArrayList<Value>();
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

                List<SMethodDef> methodsToInvoke = new ArrayList<SMethodDef>();
                SemanticScope.MethodRecorder innerMethod = null; // inner method ?
                Value target = null;
                // get method and target
                // get import
                List<Import> imports = fileNameToImport.get(invocation.line_col().fileName);
                AST.Access access = (AST.Access) invocation.exp;
                access = transformAccess(access);
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

                                                STypeDef type = getTypeWithAccess(im.access, genericTypeMap, imports);
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

                                                STypeDef type = getTypeWithAccess((AST.Access) access1.exp, genericTypeMap, imports);
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
                                                        type = getTypeWithAccess((AST.Access) access.exp, genericTypeMap, imports);
                                                } catch (Throwable ignore) {
                                                }
                                                if (type != null) {
                                                        if (type instanceof SClassDef && ((SClassDef) type).classType() == SClassDef.OBJECT) {
                                                                findMethodFromTypeWithArguments(
                                                                        access.line_col(),
                                                                        access.name,
                                                                        argList,
                                                                        scope.type(),
                                                                        type,
                                                                        FIND_MODE_NON_STATIC,
                                                                        methodsToInvoke,
                                                                        true);
                                                        } else {
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
                                                        // try to get static field
                                                        // which could be functional object
                                                        SFieldDef field = findFieldFromTypeDef(access.name, type, scope.type(), FIND_MODE_STATIC, true);

                                                        return invoke_Dynamic_invoke(
                                                                type, NullValue.get(),
                                                                true,
                                                                field == null
                                                                        ? NullValue.get()
                                                                        : new Ins.GetStatic(field, invocation.line_col()),
                                                                scope.type(),
                                                                access.name,
                                                                argList, false,
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
                                type = getTypeWithAccess(access, genericTypeMap, imports);
                        } catch (Throwable ignore) {
                                // not found or not type format
                        }
                        if (type instanceof SClassDef) {
                                // only SClassDef have constructors
                                // check whether the type is a function
                                if (((SClassDef) type).classType() == SClassDef.FUN) {
                                        return callFunctionalObject(
                                                constructingNewInst((SClassDef) type, Collections.<Value>emptyList(), scope, invocation.line_col()),
                                                scope.type(),
                                                argList,
                                                invocation.line_col()
                                        );
                                } else {
                                        return constructingNewInst((SClassDef) type, argList, scope, invocation.line_col());
                                }
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
                        STypeDef targetClass = target == null ? scope.type() : target.type();
                        Value o;
                        if (target == null) {
                                if (scope.getThis() == null)
                                        o = NullValue.get();
                                else
                                        o = scope.getThis();
                        } else {
                                o = target;
                        }

                        Value functionalObject;
                        if (access.exp == null) {
                                // invoking method on functional objects
                                functionalObject = findObjectInCurrentScopeWithName(access.name, scope);
                        } else {
                                if (target == null) {
                                        throw new LtBug("code should not reach here");
                                } else {
                                        // xx.method(...)
                                        SFieldDef field = findFieldFromTypeDef(access.name, target.type(), scope.type(), FIND_MODE_ANY, true);
                                        if (field == null) {
                                                functionalObject = NullValue.get();
                                        } else {
                                                if (field.modifiers().contains(SModifier.STATIC)) {
                                                        functionalObject = new Ins.GetStatic(field, invocation.line_col());
                                                } else {
                                                        functionalObject = new Ins.GetField(field, target, invocation.line_col());
                                                }
                                        }
                                }
                        }

                        return invoke_Dynamic_invoke(targetClass, o, scope.getThis() == null, functionalObject, scope.type(), access.name, argList, access.exp == null, invocation.line_col());
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
                                List<Value> values = new ArrayList<Value>();
                                List<Ins.TLoad> capturedValues = new ArrayList<Ins.TLoad>();
                                int inc = innerMethod.method.getParameters().size() - innerMethod.paramCount;
                                for (int i = 0; i < argList.size(); ++i) {
                                        STypeDef requiredType = innerMethod.method.getParameters().get(i + inc).type();
                                        values.add(cast(
                                                requiredType,
                                                argList.get(i),
                                                scope.type(),
                                                invocation.line_col()
                                        ));
                                }

                                Ins.InvokeWithCapture invoke;
                                if (innerMethod.method.modifiers().contains(SModifier.STATIC)) {
                                        invoke = new Ins.InvokeWithCapture(null, innerMethod.method, true, invocation.line_col());
                                } else {
                                        invoke = new Ins.InvokeWithCapture(scope.getThis(), innerMethod.method, false, invocation.line_col());
                                }
                                int requiredLocalVariableCount = innerMethod.method.getParameters().size() - innerMethod.paramCount;
                                List<LeftValue> leftValues = scope.getLeftValues(requiredLocalVariableCount, false);
                                if (leftValues.size() != requiredLocalVariableCount)
                                        throw new LtBug("require " + requiredLocalVariableCount + " local variable(s), got " + leftValues.size());

                                for (LeftValue v : leftValues) {
                                        capturedValues.add(new Ins.TLoad(v, scope, LineCol.SYNTHETIC));
                                }
                                invoke.arguments().addAll(values);
                                invoke.capturedArguments().addAll(capturedValues);
                                for (int i = 0; i < innerMethod.method.getParameters().size() - innerMethod.paramCount; ++i) {
                                        SParameter p = innerMethod.method.getParameters().get(i);
                                        assert p.isCapture();
                                        if (p.isUsed()) {
                                                Ins.TLoad v = capturedValues.get(i);
                                                scope.getMeta().pointerLocalVar.add(v.value());
                                        }
                                }

                                if (invoke.type().equals(VoidType.get()))
                                        return new ValueAnotherType(
                                                getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                        getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                        getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
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
                                                        getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
                                                        invokeSpecial, invokeSpecial.line_col()
                                                );

                                        return invokeSpecial;
                                } else {
                                        // invoke virtual
                                        if (target == null) {
                                                STypeDef declaringType = methodToInvoke.declaringType();
                                                if (declaringType instanceof SClassDef && ((SClassDef) declaringType).classType() == SClassDef.OBJECT) {
                                                        target = parseValueFromAccess((AST.Access) access.exp, scope, false);
                                                } else {
                                                        err.SyntaxException("invoke virtual should have an invoke target", invocation.line_col());
                                                        return null;
                                                }
                                        }
                                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(target, methodToInvoke, invocation.line_col());
                                        invokeVirtual.arguments().addAll(argList);

                                        if (invokeVirtual.type().equals(VoidType.get()))
                                                return new ValueAnotherType(
                                                        getTypeWithName("lt.lang.Unit", LineCol.SYNTHETIC),
                                                        invokeVirtual, invokeVirtual.line_col()
                                                );

                                        return invokeVirtual;
                                }
                        }
                }
        }

        /**
         * check whether the `target` is <code>invokeStatic lt.runtime.LtRuntime.getField</code>
         *
         * @param target target
         * @return true or false
         */
        public boolean isGetFieldAtRuntime(Value target) {
                if (target instanceof Ins.InvokeStatic) {
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) target;
                        if (invokeStatic.invokable() instanceof SMethodDef) {
                                SMethodDef m = (SMethodDef) invokeStatic.invokable();
                                if (
                                        (
                                                m.name().equals("getField")
                                        )
                                                && m.declaringType().fullName().equals("lt.runtime.LtRuntime")) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        /**
         * check whether the `target` is <code>invokeStatic {@link #DYNAMIC_CLASS_NAME}</code>
         *
         * @param target target
         * @return true or false
         */
        public boolean isInvokeAtRuntime(Value target) {
                if (target instanceof Ins.InvokeStatic) {
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) target;
                        if (invokeStatic.invokable() instanceof SMethodDef) {
                                SMethodDef m = (SMethodDef) invokeStatic.invokable();
                                if (
                                        (m.name().equals("invoke"))
                                                && m.declaringType().fullName().equals(DYNAMIC_CLASS_NAME)
                                        ) {
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
        public static final int SWAP_NONE = 0;
        /**
         * do swap
         */
        public static final int SWAP_SWAP = 1;
        /**
         * do not swap
         */
        public static final int SWAP_NO_SWAP = 2;

        /**
         * find best match
         *
         * @param argList argList
         * @param methods methods to choose from
         * @param lineCol file_line_col
         * @return selected method
         * @throws SyntaxException exception
         */
        public SMethodDef findBestMatch(List<Value> argList, List<SMethodDef> methods, LineCol lineCol) throws SyntaxException {
                if (null == methods || methods.isEmpty()) return null;
                Iterator<SMethodDef> it = methods.iterator();
                SMethodDef method = it.next();
                while (it.hasNext()) {
                        int swap = SWAP_NONE;
                        SMethodDef methodCurrent = it.next();

                        for (int i = 0; i < argList.size(); ++i) {
                                SParameter paramLast = method.getParameters().get(i);
                                SParameter paramCurrent = methodCurrent.getParameters().get(i);

                                if (!paramLast.type().equals(paramCurrent.type())) {
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
        public List<Value> castArgsForMethodInvoke(List<Value> args, List<SParameter> parameters, LineCol lineCol) throws SyntaxException {
                List<Value> result = new ArrayList<Value>();

                for (int i = 0; i < parameters.size(); ++i) {
                        Value v = args.get(i);
                        SParameter param = parameters.get(i);

                        result.add(cast(param.type(), v, null, lineCol));
                }

                return result;
        }

        public Value findObjectInCurrentScopeWithName(String name, SemanticScope scope) throws SyntaxException {
                Value v = __findObjectInCurrentScopeWithName(name, scope);
                if (isPointerType(v.type())) {
                        return invokePointerGet(v, LineCol.SYNTHETIC);
                } else return v;
        }

        /**
         * get object in current scope.
         *
         * @param name  the local variable or field's name
         * @param scope current scope
         * @return TLoad or GetField or GetStatic or NullValue
         */
        public Value __findObjectInCurrentScopeWithName(String name, SemanticScope scope) {
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
         * change primitive into its box type
         *
         * @param primitive primitive value
         * @param lineCol   file_line_col
         * @return InvokeStatic (all operations must invoke box type's static valueOf(..) method)
         * @throws SyntaxException exception
         */
        public Ins.InvokeStatic boxPrimitive(Value primitive, LineCol lineCol) throws SyntaxException {
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
        public String unescape(String s, LineCol lineCol) throws SyntaxException {
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
         * generate string that concat expressions inside the string which is surrounded by `${ }`
         *
         * @param str     raw string
         * @param scope   semantic scope
         * @param lineCol lineCol info
         * @return result from {@link #concatValuesToString(List, SemanticScope, LineCol)}
         * @throws SyntaxException compiling error
         */
        public Value generateStringConcat(String str, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                char[] chars = str.toCharArray();
                SClassDef STRING = (SClassDef) getTypeWithName("java.lang.String", lineCol);

                List<Value> elemsToConcat = new ArrayList<Value>();
                StringBuilder sb = new StringBuilder();
                StringBuilder evalStr = new StringBuilder();

                Stack<Object> evaluatingStack = new Stack<Object>();
                boolean isEvaluating = false;
                Object flag = new Object();
                for (int i = 0; i < chars.length; i++) {
                        char c = chars[i];
                        if (c == '$' && i < chars.length - 1 && chars[i + 1] == '{') {
                                evaluatingStack.push(flag);
                                ++i;
                        } else if (c == '}') {
                                if (!evaluatingStack.isEmpty()) evaluatingStack.pop();
                        }

                        if (evaluatingStack.isEmpty()) {
                                if (isEvaluating) {
                                        String expStr = evalStr.toString();
                                        evalStr.delete(0, evalStr.length());
                                        List<Statement> statements;
                                        try {
                                                ErrorManager subErr = new ErrorManager(true);
                                                Scanner scanner = new ScannerSwitcher("eval",
                                                        new StringReader(expStr), new Properties(), subErr);
                                                Parser parser = new Parser(scanner.scan(), subErr);
                                                statements = parser.parse();
                                        } catch (IOException e) {
                                                // this can never happen for the reader is a StringReader
                                                throw new LtBug(e);
                                        }
                                        if (statements.size() != 1 || !(statements.get(0) instanceof Expression)) {
                                                err.SyntaxException("the string can only concat an expression, but got " + statements, lineCol);
                                                throw new LtBug("code won't reach here");
                                        }
                                        Expression exp = (Expression) statements.get(0);
                                        elemsToConcat.add(parseValueFromExpression(exp, null, scope));
                                } else {
                                        sb.append(c);
                                }
                                isEvaluating = false;
                        } else {
                                if (isEvaluating) {
                                        if (c == '\n' || c == '\t' || c == '\r' || c == '\"' || c == '\'' || c == '\\') {
                                                err.SyntaxException("invalid char " + c + " for expression in string " + str, lineCol);
                                                throw new LtBug("code won't reach here");
                                        }
                                        evalStr.append(c);
                                } else {
                                        StringConstantValue s = new StringConstantValue(sb.toString());
                                        s.setType(STRING);
                                        elemsToConcat.add(s);
                                        sb.delete(0, sb.length());
                                }
                                isEvaluating = true;
                        }
                }
                if (sb.length() != 0 || elemsToConcat.isEmpty()) {
                        // append last piece of value to the list
                        StringConstantValue s = new StringConstantValue(sb.toString());
                        s.setType(STRING);
                        elemsToConcat.add(s);
                }

                return concatValuesToString(elemsToConcat, scope, lineCol);
        }

        /**
         * concat the values as one string
         *
         * @param values  value list
         * @param scope   scope
         * @param lineCol lineCol
         * @return StringConstantValue or 'xx+yy+zz+...'
         * @throws SyntaxException compiling error
         */
        public Value concatValuesToString(List<Value> values, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                if (values.size() == 1 && values.get(0) instanceof StringConstantValue) {
                        // plain string
                        return values.get(0);
                } else {
                        if (!(values.get(0) instanceof StringConstantValue) && !(values.get(1) instanceof StringConstantValue)) {
                                StringConstantValue emptyString = new StringConstantValue("");
                                emptyString.setType((SClassDef) getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                                values.add(0, emptyString);
                        }
                        Iterator<Value> it = values.iterator();
                        Value finalValue = it.next();
                        while (it.hasNext()) {
                                finalValue = parseValueFromTwoVarOp(finalValue, "+", it.next(), scope, lineCol);
                        }
                        STypeDef STRING = getTypeWithName("java.lang.String", LineCol.SYNTHETIC);
                        if (finalValue.type().equals(STRING)) {
                                return finalValue;
                        } else {
                                return new Ins.CheckCast(finalValue, STRING, LineCol.SYNTHETIC);
                        }
                }
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
        public Value parseValueFromObject(Object o) throws SyntaxException {
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
                        Map<SAnnoField, Value> map = new HashMap<SAnnoField, Value>();
                        for (SAnnoField f : a.type().annoFields()) {
                                try {
                                        Object obj = invokeAnnotationMethod((Annotation) o, annoCls.getMethod(f.name()));
                                        Value v = parseValueFromObject(obj);
                                        v = checkAndCastAnnotationValues(v, LineCol.SYNTHETIC);
                                        map.put(f, v);
                                } catch (Exception e) {
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
                } else if (o instanceof DummyValue) {
                        return (Value) o;
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
        public void recordAbstractMethodsForOverrideCheck_interface(SInterfaceDef i,
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
        public void recordAbstractMethodsForOverrideCheck_class(SClassDef c,
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
        public void recordAbstractMethodsForOverrideCheck(SClassDef c, List<SMethodDef> abstractMethods) throws SyntaxException {
                List<SMethodDef> visitedMethods = new ArrayList<SMethodDef>();
                Set<SInterfaceDef> visitedTypes = new HashSet<SInterfaceDef>();
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
        public void checkFinalAndOverride(SMethodDef method, SMethodDef overriddenMethod) throws SyntaxException {
                if (overriddenMethod.modifiers().contains(SModifier.FINAL)) {
                        err.SyntaxException(overriddenMethod + " cannot be overridden", method.line_col());
                        return;
                }

                if (!overriddenMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                        err.SyntaxException("Trying to override " + overriddenMethod + " but return type mismatch", method.line_col());
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
        public void checkOverride_class(SMethodDef method,
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
        public void checkOverride_interface(SMethodDef method,
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
        public void checkOverride(STypeDef sTypeDef) throws SyntaxException {
                if (sTypeDef instanceof SClassDef) {
                        for (SMethodDef m : ((SClassDef) sTypeDef).methods()) {
                                checkOverride_class(m, ((SClassDef) sTypeDef).parent(), new HashSet<STypeDef>());
                                for (SInterfaceDef i : ((SClassDef) sTypeDef).superInterfaces()) {
                                        checkOverride_interface(m, i, new HashSet<STypeDef>());
                                }
                        }
                } else if (sTypeDef instanceof SInterfaceDef) {
                        for (SMethodDef m : ((SInterfaceDef) sTypeDef).methods()) {
                                for (SInterfaceDef i : ((SInterfaceDef) sTypeDef).superInterfaces()) {
                                        checkOverride_interface(m, i, new HashSet<STypeDef>());
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
        public void checkInterfaceCircularInheritance(final SInterfaceDef toCheck, List<SInterfaceDef> current, List<SInterfaceDef> recorder) throws SyntaxException {
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
        public SMethodDef findMethodWithSameSignature(SMethodDef method,
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

        public Map<SAnno, AST.Anno> annotationRecorder = new HashMap<SAnno, AST.Anno>();

        /**
         * parse the annotations<br>
         * but the <tt>annotationFields</tt> won't be set
         *
         * @param annos                 a set of annotations
         * @param annotationPresentable the annotation is presented on the object
         * @param imports               imports
         * @param type                  the annotation accepts element type
         * @param checkTheseWhenFail    if type not matches, then check this list, if contains then it won't throw an exception
         * @throws SyntaxException exception
         */
        public void parseAnnos(Set<AST.Anno> annos,
                               SAnnotationPresentable annotationPresentable,
                               List<Import> imports,
                               ElementType type,
                               List<ElementType> checkTheseWhenFail) throws SyntaxException {
                for (AST.Anno anno : annos) {
                        SAnnoDef annoType;
                        if (annotationPresentable instanceof SMember) {
                                annoType = (SAnnoDef) getTypeWithAccess(anno.anno, getGenericMap(((SMember) annotationPresentable).declaringType()), imports);
                        } else {
                                annoType = (SAnnoDef) getTypeWithAccess(anno.anno, Collections.<String, STypeDef>emptyMap(), imports);
                        }
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
         * @param variableDefList     a list of parameters (in the form of VariableDef)
         * @param i                   parameter length (invokes the method with i+1 parameters)
         * @param invokable           the parameters belong to this object
         * @param imports             imports
         * @param allowAccessModifier allow access modifier
         * @throws SyntaxException exceptions
         */
        public void parseParameters(List<VariableDef> variableDefList, int i, SInvokable invokable, List<Import> imports,
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
                                type = getTypeWithAccess(v.getType(), getGenericMap(invokable.declaringType()), imports);
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
                                        case NONNULL:
                                                param.setNotNull(true);
                                                break;
                                        case NONEMPTY:
                                                param.setNotEmpty(true);
                                                break;
                                        default:
                                                err.UnexpectedTokenException("valid modifier for parameters (val)", m.toString(), m.line_col());
                                                return;
                                }
                        }

                        parseAnnos(v.getAnnos(), param, imports, ElementType.PARAMETER,
                                // the modifier may be field
                                allowAccessModifier
                                        ? Collections.singletonList(ElementType.FIELD)
                                        : Collections.<ElementType>emptyList()
                        );

                        invokable.getParameters().add(param);
                }
        }

        /**
         * parse fields from Destruct
         *
         * @param d        destruct ast object
         * @param type     the type to add
         * @param isStatic whether is static
         * @throws SyntaxException exception
         */
        public void parseFieldsFromDestruct(AST.Destruct d, SRefTypeDef type, boolean isStatic) throws SyntaxException {
                for (AST.Pattern p : d.pattern.subPatterns) {
                        if (p instanceof AST.Pattern_Default) continue;
                        assert p instanceof AST.Pattern_Define;

                        AST.Pattern_Define pd = (AST.Pattern_Define) p;
                        SFieldDef fieldDef = new SFieldDef(LineCol.SYNTHETIC);
                        fieldDef.setName(pd.name);
                        fieldDef.setType(getObject_Class());
                        fieldDef.setDeclaringType(type);
                        // modifiers
                        parseFieldModifiers(fieldDef, d.modifiers, type instanceof SInterfaceDef, isStatic, false);
                        // annos
                        parseAnnos(d.annos, fieldDef, fileNameToImport.get(type.line_col().fileName),
                                ElementType.FIELD, Collections.<ElementType>emptyList());

                        type.fields().add(fieldDef);
                }
        }

        private void parseFieldModifiers(SFieldDef fieldDef, Set<Modifier> modifiers,
                                         boolean isInterface, boolean isStatic, boolean isParam) throws SyntaxException {
                // try to get access flags
                boolean hasAccessModifier = false;
                for (Modifier m : modifiers) {
                        if (m.modifier.equals(Modifier.Available.PUBLIC)
                                || m.modifier.equals(Modifier.Available.PRIVATE)
                                || m.modifier.equals(Modifier.Available.PROTECTED)
                                || m.modifier.equals(Modifier.Available.PKG)) {
                                hasAccessModifier = true;
                        }
                }
                if (!hasAccessModifier) {
                        if (isInterface) {
                                fieldDef.modifiers().add(SModifier.PUBLIC);
                                fieldDef.modifiers().add(SModifier.STATIC);
                        } else {
                                if (isStatic) {
                                        fieldDef.modifiers().add(SModifier.PUBLIC); // default modifier for static field is public
                                } else {
                                        fieldDef.modifiers().add(SModifier.PRIVATE); // default modifier for instance field is private
                                }
                        }
                }
                // modifiers
                for (Modifier m : modifiers) {
                        switch (m.modifier) {
                                case PUBLIC:
                                        fieldDef.modifiers().add(SModifier.PUBLIC);
                                        break;
                                case PRIVATE:
                                        if (isInterface) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString().toLowerCase(), m.line_col());
                                                return;
                                        }
                                        fieldDef.modifiers().add(SModifier.PRIVATE);
                                        break;
                                case PROTECTED:
                                        if (isInterface) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString().toLowerCase(), m.line_col());
                                                return;
                                        }
                                        fieldDef.modifiers().add(SModifier.PROTECTED);
                                        break;
                                case PKG: // no need to assign modifier
                                        if (isInterface) {
                                                err.UnexpectedTokenException("valid modifier for interface fields (public|val)", m.toString().toLowerCase(), m.line_col());
                                                return;
                                        }
                                        break;
                                case VAL:
                                        fieldDef.modifiers().add(SModifier.FINAL);
                                        break;
                                case VAR:
                                        if (!isInterface) break;
                                case NONNULL:
                                        if (isParam) break;
                                case NONEMPTY:
                                        if (isParam) break;
                                default:
                                        err.UnexpectedTokenException("valid modifier for fields (class:(public|private|protected|internal|val)|interface:(pub|val))", m.toString().toLowerCase(), m.line_col());
                                        return;
                        }
                }
                if (isInterface && !fieldDef.modifiers().contains(SModifier.FINAL)) {
                        fieldDef.modifiers().add(SModifier.FINAL);
                }
                if (isStatic && !isInterface) {
                        fieldDef.modifiers().add(SModifier.STATIC);
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
         * @param isParam  the field is parsed from param
         * @throws SyntaxException exception
         */
        public void parseField(VariableDef v, STypeDef type, List<Import> imports, int mode, boolean isStatic, boolean isParam) throws SyntaxException {
                // field, set its name|type|modifier|annos|declaringClass
                SFieldDef fieldDef = new SFieldDef(v.line_col());
                fieldDef.setName(v.getName()); // name

                // type
                fieldDef.setType(
                        v.getType() == null
                                ? getTypeWithName("java.lang.Object", v.line_col())
                                : getTypeWithAccess(v.getType(), getGenericMap(type), imports)
                );
                fieldDef.setDeclaringType(type); // declaringClass

                // modifiers
                parseFieldModifiers(fieldDef, v.getModifiers(), mode == PARSING_INTERFACE, isStatic, isParam);
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
                                err.DuplicateVariableNameException(v.getName(), v.line_col());
                                return;
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
        public void parseMethod(MethodDef m, int i, STypeDef type, SMethodDef lastMethod, List<Import> imports, int mode, boolean isStatic) throws SyntaxException {
                // method name|declaringType|returnType|parameters|modifier|anno
                SMethodDef methodDef = new SMethodDef(m.line_col());
                methodDef.setName(m.name);
                methodDef.setDeclaringType(type);
                methodDef.setReturnType(
                        m.returnType == null
                                ? getTypeWithName("java.lang.Object", m.line_col())
                                : getRealReturnType(getTypeWithAccess(m.returnType, getGenericMap(type), imports), true)
                );
                parseParameters(m.params, i, methodDef, imports, false);

                // modifier
                // try to get access flags
                boolean hasAccessModifier = false;
                // implicit modifier
                boolean isImplicit = false;
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
                                case SYNCHRONIZED:
                                        methodDef.modifiers().add(SModifier.SYNCHRONIZED);
                                        break;
                                case DEF:
                                        // a flag for defining a method
                                        break;
                                case IMPLICIT:
                                        isImplicit = true;
                                        break;
                                default:
                                        err.UnexpectedTokenException("valid modifier for methods (class:(public|private|protected|internal|val)|interface:(pub|val))", m.toString(), m.line_col());
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
                parseAnnos(m.annos, methodDef, imports, ElementType.METHOD, Collections.<ElementType>emptyList());

                if (isImplicit) {
                        checkAndAddImplicitAnno(methodDef);
                }

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
                        Map<SInvokable, Expression> invoke = new HashMap<SInvokable, Expression>();
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
         * get Import/ClassDef/InterfaceDef/FunDef from Statement object
         *
         * @param stmt           the statement
         * @param imports        import list
         * @param classDefs      classDef list
         * @param interfaceDefs  interfaceDef list
         * @param funDefs        functionDef list
         * @param objectDefs     objectDef list
         * @param annotationDefs annotationDef list
         * @throws UnexpectedTokenException the statement is not import/class/interface
         */
        public void select_import_class_interface_fun_object(Statement stmt,
                                                             List<Import> imports,
                                                             List<ClassDef> classDefs,
                                                             List<InterfaceDef> interfaceDefs,
                                                             List<FunDef> funDefs,
                                                             List<ObjectDef> objectDefs,
                                                             List<AnnotationDef> annotationDefs) throws UnexpectedTokenException {
                if (stmt instanceof Import) {
                        imports.add((Import) stmt);
                } else if (stmt instanceof ClassDef) {
                        classDefs.add((ClassDef) stmt);
                } else if (stmt instanceof InterfaceDef) {
                        interfaceDefs.add((InterfaceDef) stmt);
                } else if (stmt instanceof FunDef) {
                        funDefs.add((FunDef) stmt);
                } else if (stmt instanceof ObjectDef) {
                        objectDefs.add((ObjectDef) stmt);
                } else if (stmt instanceof AnnotationDef) {
                        annotationDefs.add((AnnotationDef) stmt);
                } else {
                        err.UnexpectedTokenException("class/interface/object definition or import", stmt.toString(), stmt.line_col());
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
        public STypeDef getTypeWithName(String clsName, LineCol lineCol) throws SyntaxException {
                return getTypeWithName(clsName, Collections.<STypeDef>emptyList(), false, lineCol);
        }

        private String buildTemplateAppliedName(String clsName, List<? extends STypeDef> generics) {
                if (generics.isEmpty()) {
                        return clsName;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(clsName);
                for (STypeDef t : generics) {
                        sb.append(Consts.GENERIC_NAME_SPLIT).append(t.fullName().replaceAll("\\.", "_"));
                }
                return sb.toString();
        }

        /**
         * get type by class name
         *
         * @param clsName        class name
         * @param generics       a list of generic types
         * @param allowException if true, then no syntax exception would be thrown
         * @param lineCol        file_line_col
         * @return STypeDef (not null)
         * @throws SyntaxException exception
         */
        public STypeDef getTypeWithName(String clsName, List<? extends STypeDef> generics, boolean allowException, LineCol lineCol) throws SyntaxException {
                clsName = buildTemplateAppliedName(clsName, generics);
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
                                        if (cls.isAnnotationPresent(GenericTemplate.class)) {
                                                // it's a generic template
                                                // retrieve and record the AST
                                                Field astField;
                                                try {
                                                        astField = cls.getField(Consts.AST_FIELD);
                                                } catch (NoSuchFieldException e) {
                                                        throw new LtBug("the generic template doesn't have " + Consts.AST_FIELD + " field");
                                                }
                                                Definition defi;
                                                try {
                                                        defi = (Definition) astField.get(null);
                                                } catch (IllegalAccessException e) {
                                                        throw new LtBug("the generic template field is not public", e);
                                                } catch (ClassCastException e) {
                                                        throw new LtBug("the generic template field is not a definition", e);
                                                }
                                                if (!(defi instanceof ClassDef) && !(defi instanceof InterfaceDef) && !(defi instanceof ObjectDef)
                                                        && !(defi instanceof FunDef) && !(defi instanceof AnnotationDef)) {
                                                        throw new LtBug("invalid definition for generic template type: " + defi);
                                                }
                                                String pkg;
                                                if (cls.getPackage() != null) {
                                                        pkg = cls.getPackage().getName();
                                                } else {
                                                        pkg = cls.getName();
                                                        pkg = pkg.substring(pkg.lastIndexOf('.'));
                                                }
                                                if (defi instanceof ClassDef) {
                                                        recordClass((ClassDef) defi, pkg, Collections.<STypeDef>emptyList());
                                                } else if (defi instanceof InterfaceDef) {
                                                        recordInterface((InterfaceDef) defi, pkg, Collections.<STypeDef>emptyList());
                                                } else if (defi instanceof ObjectDef) {
                                                        recordObject((ObjectDef) defi, pkg, Collections.<STypeDef>emptyList());
                                                } else if (defi instanceof FunDef) {
                                                        recordFun((FunDef) defi, pkg, Collections.<STypeDef>emptyList());
                                                } else /*if (defi instanceof AnnotationDef)*/ {
                                                        recordAnnotation((AnnotationDef) defi, pkg, Collections.<STypeDef>emptyList());
                                                }
                                        }

                                        List<SModifier> modifiers; // modifiers
                                        STypeDef typeDef;
                                        if (cls.isAnnotation()) {
                                                SAnnoDef a = new SAnnoDef(LineCol.SYNTHETIC);
                                                a.setFullName(clsName);

                                                typeDef = a;
                                                modifiers = a.modifiers();
                                        } else if (cls.isInterface()) {
                                                SInterfaceDef i = new SInterfaceDef(LineCol.SYNTHETIC);
                                                i.setFullName(clsName);

                                                typeDef = i;
                                                modifiers = i.modifiers();
                                        } else { // class
                                                // check class type (normal/fun/object)
                                                int classType;
                                                if (cls.isAnnotationPresent(LatteFun.class)) classType = SClassDef.FUN;
                                                else if (cls.isAnnotationPresent(LatteObject.class))
                                                        classType = SClassDef.OBJECT;
                                                else classType = SClassDef.NORMAL;
                                                SClassDef c = new SClassDef(classType, LineCol.SYNTHETIC);
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
                                                        assert annoM.getParameterTypes().length == 0;
                                                        SAnnoField annoField = new SAnnoField();
                                                        annoField.setName(annoM.getName());
                                                        annoField.setType(getTypeWithName(annoM.getReturnType().getName(), lineCol));

                                                        annoDef.annoFields().add(annoField);
                                                }
                                        }
                                        return typeDef;
                                }
                        } catch (ClassNotFoundException e) {
                                if (!allowException) {
                                        err.SyntaxException("undefined class " + clsName, lineCol);
                                }
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
        public void getAnnotationFromAnnotatedElement(AnnotatedElement elem, SAnnotationPresentable presentable) throws SyntaxException {
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
                                        assert m.getParameterTypes().length == 0;
                                        try {
                                                sAnno.alreadyCompiledAnnotationValueMap().put(m.getName(), invokeAnnotationMethod(a, m));
                                        } catch (Throwable e) {
                                                // the exception should never occur
                                                throw new LtBug(e);
                                        }
                                }
                        }
                }
        }

        private Object invokeAnnotationMethod(Annotation a, Method m) throws InvocationTargetException, SyntaxException {
                try {
                        return m.invoke(a);
                } catch (IllegalAccessException e) {
                        // annotation methods are public
                        // the exception won't occur only when java 9 module did not export the type
                        // simply ignore the exception and replace the result with a dummy value
                        return new DummyValue(
                                getTypeWithName(m.getReturnType().getName(), LineCol.SYNTHETIC)
                        );
                }
        }

        /**
         * get modifier from class object<br>
         * and add them into the list
         *
         * @param cls       class object
         * @param modifiers modifiers
         */
        public void getModifierFromClass(Class<?> cls, List<SModifier> modifiers) {
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
        public void getModifierFromMember(Member member, SMember sMember) {
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
        public void getSuperInterfaceFromClass(Class<?> cls, List<SInterfaceDef> interfaces) throws SyntaxException {
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
        public void getFieldsAndMethodsFromClass(Class<?> cls, STypeDef declaringType, List<SFieldDef> fields, List<SMethodDef> methods) throws SyntaxException {
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
                                methodDef.setReturnType(
                                        getRealReturnType(getTypeWithName(m.getReturnType().getName(), LineCol.SYNTHETIC), true));
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
        public void getParameterFromClassArray(Class<?>[] paramTypes, SInvokable invokable) throws SyntaxException {
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
        public boolean typeExists(String type) {
                if (!types.containsKey(type)) {
                        try {
                                loadClass(type);
                        } catch (ClassNotFoundException e) {
                                return false;
                        } catch (NoClassDefFoundError e) {
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
         * @throws SyntaxException compile error
         */
        public String getClassNameFromAccess(AST.Access access) throws SyntaxException {
                access = transformAccess(access);
                String pre;
                if (access.exp instanceof AST.Access) {
                        pre = getClassNameFromAccess((AST.Access) access.exp) + "$";
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
         * @throws SyntaxException compile error
         */
        public String findClassNameWithImport(String name, List<Import> imports) throws SyntaxException {
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
         * @param access     access object
         * @param genericMap generic types stored in a map of name to type
         * @param imports    import list
         * @return retrieved STypeDef (not null)
         * @throws SyntaxException exception
         */
        public STypeDef getTypeWithAccess(AST.Access access, Map<String, STypeDef> genericMap, List<Import> imports) throws SyntaxException {
                return getTypeWithAccess(access, genericMap, imports, false);
        }

        /**
         * get type with access
         *
         * @param access         access object
         * @param genericMap     generic info
         * @param imports        import list
         * @param allowException if true, then syntax exception would be ignored
         * @return retrieved STypeDef (not null)
         * @throws SyntaxException exception
         */
        public STypeDef getTypeWithAccess(AST.Access access, Map<String, STypeDef> genericMap, List<Import> imports, boolean allowException) throws SyntaxException {
                assert access.exp == null || access.exp instanceof AST.Access
                        || access.exp instanceof AST.PackageRef || "[]".equals(access.name) || "*".equals(access.name);

                access = transformAccess(access);
                boolean isPointer = false;
                if ("*".equals(access.name)) {
                        if (access.exp == null) {
                                return new PointerType(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        }

                        isPointer = true;
                        assert access.exp instanceof AST.Access;
                        access = (AST.Access) access.exp;
                }

                STypeDef resultType;
                if ("[]".equals(access.name)) {
                        assert access.exp instanceof AST.Access;
                        STypeDef type = getTypeWithAccess((AST.Access) access.exp, genericMap, imports);
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
                                resultType = types.get(a.fullName());
                        } else {
                                putNameAndTypeDef(a, access.line_col());
                                resultType = a;
                        }
                } else {
                        String className = accessToClassName(access, genericMap, imports, allowException);
                        if (className == null) {
                                // if the classname not found
                                if (!allowException) {
                                        err.SyntaxException("Type not found: " + access, access.line_col());
                                }
                                return null;
                        }
                        resultType = getTypeWithName(className, Collections.<STypeDef>emptyList(), allowException, access.line_col());
                }

                if (isPointer) {
                        resultType = new PointerType(resultType);
                }
                return resultType;
        }

        /**
         * put {fileName:TypeDef} into the map
         *
         * @param type    STypeDef object
         * @param lineCol file_line_col
         * @throws SyntaxException exception
         */
        public void putNameAndTypeDef(STypeDef type, LineCol lineCol) throws SyntaxException {
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
        public Class<?> loadClass(String name) throws ClassNotFoundException {
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

        /**
         * the given type is a pointer type (SClassDef lt.lang.Pointer) or (PointerType)
         *
         * @param type type
         * @return true/false
         */
        public boolean isPointerType(STypeDef type) {
                return type instanceof PointerType || "lt.lang.Pointer".equals(type.fullName());
        }

        /**
         * cast the `return` type to "void" if type is Unit and doCast is true
         *
         * @param type   type
         * @param doCast do cast if type is Unit
         * @return the final result
         */
        public STypeDef getRealReturnType(STypeDef type, boolean doCast) {
                if (doCast && type.fullName().equals("lt.lang.Unit")) {
                        return VoidType.get();
                }
                return type;
        }

        /**
         * transform the AST.Access object<br>
         * the part that may represent a `type` will be converted into package ref and typeSimpleName<br>
         * e.g. java.lang.Object => (((null, java), lang), Object) => ((java::lang), Object)<br>
         * <br>
         * this method doesn't care about whether it's a generic type.
         *
         * @param access access object
         * @return original access object or a new one depending on the input
         */
        private AST.Access transformAccess(AST.Access access) throws SyntaxException {
                if (null == access) return null;
                if (null == access.exp) return access;
                if (!(access.exp instanceof AST.Access)) return access;
                // use ArrayList for that it requires random access
                ArrayList<String> maybePackages = new ArrayList<String>();
                ArrayList<LineCol> lineColInfo = new ArrayList<LineCol>();
                maybePackages.add(access.name);
                lineColInfo.add(access.line_col());
                AST.Access tmp = (AST.Access) access.exp;
                while (true) {
                        maybePackages.add(tmp.name);
                        lineColInfo.add(tmp.line_col());
                        Expression exp = tmp.exp;
                        if (null == exp) break;
                        if (!(exp instanceof AST.Access)) return access;
                        tmp = (AST.Access) exp;
                }
                Collections.reverse(maybePackages);
                Collections.reverse(lineColInfo);
                for (int i = 1; i < maybePackages.size(); ++i) {
                        StringBuilder clsNameBuilder = new StringBuilder();
                        for (int j = 0; j <= i; ++j) {
                                if (j != 0) {
                                        clsNameBuilder.append(".");
                                }
                                clsNameBuilder.append(maybePackages.get(j));
                        }
                        String clsName = clsNameBuilder.toString();
                        STypeDef type = getTypeWithName(clsName, Collections.<STypeDef>emptyList(), true, access.line_col());
                        if (null == type) {
                                continue;
                        }
                        // type found, build a new access object
                        String fullName = type.fullName();
                        String packageName;
                        if (fullName.contains(".")) {
                                packageName = fullName.substring(0, fullName.lastIndexOf(".")).replace(".", "::");
                        } else {
                                packageName = fullName;
                        }
                        AST.Access result = new AST.Access(new AST.PackageRef(
                                packageName, lineColInfo.get(i - 1)
                        ), maybePackages.get(i), lineColInfo.get(i));
                        for (int j = i + 1; j < maybePackages.size(); ++j) {
                                result = new AST.Access(result, maybePackages.get(j), lineColInfo.get(j));
                        }
                        return result;
                }

                // cannot be a package.type.*
                return access;
        }

        private String accessToClassName(AST.Access access, Map<String, STypeDef> genericMap, List<Import> imports, boolean allowException) throws SyntaxException {
                String className;
                if (access.exp instanceof AST.Access) {
                        boolean withPackage = false;
                        AST.Access tmp = access;
                        StringBuilder innerClassName = new StringBuilder(); // only used if it's inner class ref
                        while (tmp.exp != null) {
                                assert tmp.exp instanceof AST.Access || tmp.exp instanceof AST.PackageRef;
                                if (tmp.exp instanceof AST.PackageRef) {
                                        withPackage = true;
                                        break;
                                }
                                innerClassName.insert(0, "$" + tmp.name);
                                tmp = (AST.Access) tmp.exp;
                        }

                        if (withPackage) {
                                className = getClassNameFromAccess(access);
                                if (null == className || !typeExists(className)) {
                                        if (!allowException) {
                                                err.SyntaxException("type " + className + " not defined", access.line_col());
                                        }
                                        return null;
                                }
                        } else {
                                className = findClassNameWithImport(tmp.name, imports) + innerClassName;
                                if (!typeExists(className)) {
                                        if (!allowException) {
                                                err.SyntaxException("type " + innerClassName + " not defined", access.line_col());
                                        }
                                        return null;
                                }
                        }
                } else {
                        assert access.exp == null || access.exp instanceof AST.PackageRef;

                        AST.PackageRef pkg = (AST.PackageRef) access.exp;
                        String name = access.name;

                        if (pkg == null) {
                                if (genericMap.containsKey(name)) {
                                        className = genericMap.get(name).fullName();
                                } else {
                                        className = findClassNameWithImport(name, imports);
                                }
                        } else {
                                className = pkg.pkg.replace("::", ".") + "." + name;
                        }

                        if (null == className || !typeExists(className)) {
                                if (!allowException) {
                                        err.SyntaxException("type " + name + " not defined", access.line_col());
                                }
                                return null;
                        }
                }
                // check the access.generics
                if (access.generics.isEmpty()) {
                        return className;
                } else {
                        List<AST.Access> genericASTList = access.generics;
                        List<STypeDef> genericTypeList = new ArrayList<STypeDef>();
                        for (AST.Access e : genericASTList) {
                                STypeDef t = getTypeWithAccess(e, genericMap, imports, allowException);
                                genericTypeList.add(t);
                        }
                        return buildTemplateAppliedName(className, genericTypeList);
                }
        }

        private Map<String, STypeDef> getGenericMap(STypeDef t) throws SyntaxException {
                if (t instanceof SClassDef) {
                        return getGenericMap((SClassDef) t);
                } else if (t instanceof SInterfaceDef) {
                        return getGenericMap((SInterfaceDef) t);
                } else if (t instanceof SAnnoDef) {
                        return Collections.emptyMap();
                } else {
                        throw new LtBug("unknown STypeDef " + t);
                }
        }

        private Map<String, STypeDef> getGenericMap(SClassDef c) {
                List<STypeDef> genericArgs;
                List<AST.Access> genericParams;
                if (c.classType() == SClassDef.NORMAL) {
                        ASTGHolder<ClassDef> holder = originalClasses.get(c.fullName());
                        ClassDef classDef = holder.s;
                        genericArgs = holder.generics;
                        genericParams = classDef.generics;
                } else if (c.classType() == SClassDef.OBJECT) {
                        ASTGHolder<ObjectDef> holder = originalObjects.get(c.fullName());
                        ObjectDef objectDef = holder.s;
                        genericArgs = holder.generics;
                        genericParams = objectDef.generics;
                } else if (c.classType() == SClassDef.FUN) {
                        return Collections.emptyMap();
                } else {
                        throw new LtBug("unknown classType " + c.classType());
                }
                LinkedHashMap<String, STypeDef> ret = new LinkedHashMap<String, STypeDef>();
                for (int i = 0; i < genericParams.size(); ++i) {
                        ret.put(genericParams.get(i).name, genericArgs.get(i));
                }
                return ret;
        }

        private Map<String, STypeDef> getGenericMap(SInterfaceDef interf) {
                ASTGHolder<InterfaceDef> holder = originalInterfaces.get(interf.fullName());
                InterfaceDef interfaceDef = holder.s;
                List<STypeDef> genericArgs = holder.generics;
                List<AST.Access> genericParams = interfaceDef.generics;
                LinkedHashMap<String, STypeDef> ret = new LinkedHashMap<String, STypeDef>();
                for (int i = 0; i < genericParams.size(); ++i) {
                        ret.put(genericParams.get(i).name, genericArgs.get(i));
                }
                return ret;
        }

        private boolean isGenericTemplateType(STypeDef t) {
                if (t instanceof SClassDef) {
                        SClassDef c = (SClassDef) t;
                        if (c.classType() == SClassDef.NORMAL) {
                                ASTGHolder<ClassDef> holder = originalClasses.get(t.fullName());
                                return holder.generics.isEmpty() && !holder.s.generics.isEmpty();
                        } else if (c.classType() == SClassDef.OBJECT) {
                                ASTGHolder<ObjectDef> holder = originalObjects.get(t.fullName());
                                return holder.generics.isEmpty() && !holder.s.generics.isEmpty();
                        } else if (c.classType() == SClassDef.FUN) {
                                // functions are never generic template types
                                return false;
                        } else {
                                throw new LtBug("unknown classType " + c.classType());
                        }
                } else if (t instanceof SInterfaceDef) {
                        ASTGHolder<InterfaceDef> holder = originalInterfaces.get(t.fullName());
                        return holder.generics.isEmpty() && !holder.s.generics.isEmpty();
                } else if (t instanceof SAnnoDef) {
                        // annotations are never generic template types
                        return false;
                } else {
                        throw new LtBug("unknown STypeDef " + t);
                }
        }
}
