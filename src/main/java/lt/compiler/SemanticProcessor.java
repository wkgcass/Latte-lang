package lt.compiler;

import lt.compiler.semantic.*;
import lt.compiler.semantic.builtin.*;
import lt.compiler.semantic.builtin.ClassValue;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.InterfaceDef;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.lang.Dynamic;
import lt.lang.Lang;
import lt.lang.LtIterator;
import lt.lang.Undefined;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
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
         * a set of types that should be return value of {@link #parse()} method<br>
         * these types are to be compiled into byte codes
         */
        private final Set<STypeDef> typeDefSet = new HashSet<>();
        /**
         * retrieve existing classes from this class loader
         */
        private final ClassLoader classLoader;

        /**
         * initialize the Processor
         *
         * @param mapOfStatements a map of fileName to statements
         * @param classLoader
         */
        public SemanticProcessor(Map<String, List<Statement>> mapOfStatements, ClassLoader classLoader) {
                this.mapOfStatements = mapOfStatements;
                this.classLoader = classLoader;
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
         * the parsing process are divided into 4 steps<br>
         * <ol>
         * <li><b>recording</b> : scan all classes and record them</li>
         * <li><b>signatures</b> : parse parents/superInterfaces, members, annotations, but don't parse statements or annotation values</li>
         * <li><b>validate</b> : check circular inheritance and override, check overload with super classes/interfaces and check whether the class overrides all methods from super interfaces/super abstract classes.</li>
         * <li><b>parse</b> : parse annotation values and statements</li>
         * </ol>
         *
         * @return parsed types, including all inside members and statements
         * @throws SyntaxException compile error
         */
        public Set<STypeDef> parse() throws SyntaxException {
                Map<String, List<ClassDef>> fileNameToClassDef = new HashMap<>();
                Map<String, List<InterfaceDef>> fileNameToInterfaceDef = new HashMap<>();
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

                        // put into map
                        fileNameToImport.put(fileName, imports);
                        fileNameToClassDef.put(fileName, classDefs);
                        fileNameToInterfaceDef.put(fileName, interfaceDefs);

                        if (statementIterator.hasNext()) {
                                Statement statement = statementIterator.next();
                                if (statement instanceof PackageDeclare) {
                                        PackageDeclare p = (PackageDeclare) statement;
                                        pkg = p.pkg.pkg.replace("::", ".") + ".";
                                } else {
                                        pkg = "";
                                        select_import_class_interface(statement, imports, classDefs, interfaceDefs);
                                }
                                while (statementIterator.hasNext()) {
                                        Statement stmt = statementIterator.next();
                                        select_import_class_interface(stmt, imports, classDefs, interfaceDefs);
                                }
                        } else {
                                // no statements,then continue
                                continue;
                        }

                        // add package into import list at index 0
                        imports.add(0, new Import(Collections.singletonList(new Import.ImportDetail(new AST.PackageRef(
                                pkg.endsWith(".")
                                        ? pkg.substring(0, pkg.length() - 1).replace(".", "::")
                                        : pkg
                                , LineCol.SYNTHETIC), null, true)), LineCol.SYNTHETIC));
                        // add java.lang into import list
                        // java::lang::_
                        // lt::lang::_
                        // lt::lang::Utils._
                        imports.add(new Import(Collections.singletonList(new Import.ImportDetail(new AST.PackageRef("java::lang", LineCol.SYNTHETIC), null, true)), LineCol.SYNTHETIC));
                        imports.add(new Import(Collections.singletonList(new Import.ImportDetail(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), null, true)), LineCol.SYNTHETIC));
                        imports.add(new Import(Collections.singletonList(new Import.ImportDetail(null, new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Utils", LineCol.SYNTHETIC), true)), LineCol.SYNTHETIC));

                        fileNameToPackageName.put(fileName, pkg);

                        Set<String> importSimpleNames = new HashSet<>();
                        for (Import i : imports) {
                                for (Import.ImportDetail detail : i.importDetails) {
                                        if (detail.pkg == null) {
                                                String className = getClassNameFromAccess(detail.access);
                                                // check existence
                                                if (!typeExists(className)) {
                                                        throw new SyntaxException(className + " does not exist", i.line_col());
                                                }
                                                // simple fileName are the same
                                                if (importSimpleNames.contains(detail.access.name)) {
                                                        throw new SyntaxException("duplicate imports", i.line_col());
                                                }
                                                importSimpleNames.add(detail.access.name);
                                        }
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
                                        throw new SyntaxException("duplicate type names " + className, c.line_col());
                                }

                                SClassDef sClassDef = new SClassDef(c.line_col());
                                sClassDef.setFullName(className);
                                sClassDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                                sClassDef.modifiers().add(SModifier.PUBLIC);

                                for (Modifier m : c.modifiers) {
                                        switch (m.modifier) {
                                                case "abs":
                                                        sClassDef.modifiers().add(SModifier.ABSTRACT);
                                                        break;
                                                case "val":
                                                        sClassDef.modifiers().add(SModifier.FINAL);
                                                        break;
                                                case "pub":
                                                case "pri":
                                                case "pro":
                                                case "pkg":
                                                        // pub|pri|pro|pkg are for constructors
                                                        break;
                                                default:
                                                        throw new UnexpectedTokenException("valid modifier for class (val|abs)", m.toString(), m.line_col());
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
                                        throw new SyntaxException("duplicate type names " + interfaceName, i.line_col());
                                }

                                SInterfaceDef sInterfaceDef = new SInterfaceDef(i.line_col());
                                sInterfaceDef.setFullName(interfaceName);
                                sInterfaceDef.setPkg(pkg.endsWith(".") ? pkg.substring(0, pkg.length() - 1) : pkg);
                                sInterfaceDef.modifiers().add(SModifier.PUBLIC);
                                sInterfaceDef.modifiers().add(SModifier.ABSTRACT);

                                for (Modifier m : i.modifiers) {
                                        switch (m.modifier) {
                                                case "abs":
                                                        // can only be abstract
                                                        break;
                                                default:
                                                        throw new UnexpectedTokenException("valid modifier for interface (abs)", m.toString(), m.line_col());
                                        }
                                }

                                types.put(interfaceName, sInterfaceDef); // record the interface
                                originalInterfaces.put(interfaceName, i);
                                typeDefSet.add(sInterfaceDef);
                        }

                        // all classes occurred in the parsing process will be inside `types` map or is already defined
                }

                // ======= step 2 =======
                // build fields,methods,constructors,parameters,parent-classes,super-interfaces,annotations.
                // but no details (annotation's values|method statements|constructor statements won't be parsed)
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
                                                } else
                                                        throw new SyntaxException(mightBeClassAccess.toString() + " is not class or interface",
                                                                mightBeClassAccess.line_col());
                                        }
                                } else {
                                        // super class
                                        AST.Access access = classDef.superWithInvocation.access;
                                        STypeDef tmp = getTypeWithAccess(access, imports);
                                        if (tmp instanceof SClassDef) {
                                                sClassDef.setParent((SClassDef) tmp);
                                        } else {
                                                throw new SyntaxException(access.toString() + " is not class or interface",
                                                        access.line_col());
                                        }
                                        superWithoutInvocationAccess = classDef.superWithoutInvocation.iterator();
                                }
                                // interfaces to be parsed
                                while (superWithoutInvocationAccess != null && superWithoutInvocationAccess.hasNext()) {
                                        AST.Access interfaceAccess = superWithoutInvocationAccess.next();
                                        STypeDef tmp = getTypeWithAccess(interfaceAccess, imports);
                                        if (tmp instanceof SInterfaceDef) {
                                                sClassDef.superInterfaces().add((SInterfaceDef) tmp);
                                        } else throw new SyntaxException(interfaceAccess.toString() + " is not interface",
                                                interfaceAccess.line_col());
                                }

                                // annos
                                parseAnnos(classDef.annos, sClassDef, imports, ElementType.TYPE);

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

                                        boolean hasAccessModifier = false;
                                        for (Modifier m : classDef.modifiers) {
                                                if (m.modifier.equals("pub") || m.modifier.equals("pri") || m.modifier.equals("pro") || m.modifier.equals("pkg")) {
                                                        hasAccessModifier = true;
                                                }
                                        }
                                        if (!hasAccessModifier) {
                                                constructor.modifiers().add(SModifier.PUBLIC);
                                        }
                                        for (Modifier m : classDef.modifiers) {
                                                switch (m.modifier) {
                                                        case "pub":
                                                                constructor.modifiers().add(SModifier.PUBLIC);
                                                                break;
                                                        case "pri":
                                                                constructor.modifiers().add(SModifier.PRIVATE);
                                                                break;
                                                        case "pro":
                                                                constructor.modifiers().add(SModifier.PROTECTED);
                                                                break;
                                                        case "val":
                                                        case "pkg":
                                                        case "abs":
                                                                // val and abs are presented on class
                                                                break; // pkg don't need to sign modifier
                                                        default:
                                                                throw new UnexpectedTokenException("valid constructor modifier (pub|pri|pro|pkg)", m.toString(), m.line_col());
                                                }
                                        }

                                        // parameters
                                        parseParameters(classDef.params, i, constructor, imports, true);

                                        constructor.setDeclaringType(sClassDef);
                                        sClassDef.constructors().add(constructor);

                                        if (lastConstructor != null) {
                                                // invoke another constructor
                                                Ins.InvokeSpecial invoke = new Ins.InvokeSpecial(new Ins.This(sClassDef), lastConstructor, LineCol.SYNTHETIC);
                                                for (SParameter p : constructor.getParameters()) {
                                                        invoke.arguments().add(p);
                                                }
                                                List<SParameter> paramsOfLast = lastConstructor.getParameters();
                                                invoke.arguments().add(parseValueFromExpression(classDef.params.get(i).getInit(), paramsOfLast.get(paramsOfLast.size() - 1).type(), null));

                                                constructor.statements().add(invoke);
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
                                parseAnnos(interfaceDef.annos, sInterfaceDef, imports, ElementType.TYPE);

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
                                                throw new SyntaxException("interfaces don't have initiators", stmt.line_col());
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
                                                        throw new SyntaxException("interfaces don't have initiators", stmt.line_col());
                                                }
                                        }
                                }
                        }
                }

                // ========step 3========
                // check circular inheritance
                for (STypeDef sTypeDef : typeDefSet) {
                        if (sTypeDef instanceof SClassDef) {
                                List<STypeDef> circularRecorder = new ArrayList<>();
                                SClassDef parent = ((SClassDef) sTypeDef).parent();
                                while (parent != null) {
                                        circularRecorder.add(parent);
                                        if (parent.equals(sTypeDef)) {
                                                throw new SyntaxException("circular inheritance " + circularRecorder, LineCol.SYNTHETIC);
                                        }
                                        parent = parent.parent();
                                }
                                circularRecorder.clear();
                        } else if (sTypeDef instanceof SInterfaceDef) {
                                SInterfaceDef i = (SInterfaceDef) sTypeDef;
                                checkInterfaceCircularInheritance(i, i.superInterfaces(), new ArrayList<>());
                        } else {
                                throw new IllegalArgumentException("wrong STypeDefType " + sTypeDef.getClass());
                        }
                }
                // check override and overload with super methods
                for (STypeDef sTypeDef : typeDefSet) {
                        checkOverride(sTypeDef);

                        // check whether overrides all methods from super
                        if (sTypeDef instanceof SClassDef) {
                                // first mark all methods with possible override and overridden
                                SClassDef c = (SClassDef) sTypeDef;
                                if (c.modifiers().contains(SModifier.ABSTRACT)) continue;

                                // parent class
                                SClassDef parent = c.parent();
                                while (parent != null && parent.modifiers().contains(SModifier.ABSTRACT)) {
                                        // is abstract
                                        if (parent.parent() != null && parent.parent().modifiers().contains(SModifier.ABSTRACT)) {
                                                checkOverride(parent);
                                        }
                                        parent = parent.parent();
                                }

                                // super interfaces
                                Queue<SInterfaceDef> q = new ArrayDeque<>();
                                q.addAll(c.superInterfaces());
                                while (!q.isEmpty()) {
                                        SInterfaceDef i = q.remove();
                                        checkOverride(i);
                                        q.addAll(i.superInterfaces());
                                }

                                // then check all abstract methods
                                parent = c.parent();
                                while (parent != null && parent.modifiers().contains(SModifier.ABSTRACT)) {
                                        for (SMethodDef m : parent.methods()) {
                                                if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                                        if (m.overridden().isEmpty())
                                                                throw new SyntaxException(m + " is not overridden in " + c, c.line_col());
                                                }
                                        }
                                        parent = parent.parent();
                                }
                                q.clear();
                                q.addAll(c.superInterfaces());
                                while (!q.isEmpty()) {
                                        SInterfaceDef i = q.remove();
                                        for (SMethodDef m : i.methods()) {
                                                if (m.modifiers().contains(SModifier.ABSTRACT)) {
                                                        if (m.overridden().isEmpty())
                                                                throw new SyntaxException(m + " is not overridden in " + c, c.line_col());
                                                }
                                        }
                                        q.addAll(i.superInterfaces());
                                }
                        }
                }

                // ========step 4========
                // first parse anno types
                // the annotations presented on these anno types will also be parsed
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

                                // initiate scope
                                SemanticScope scope = new SemanticScope(sTypeDef);

                                // parse constructor
                                // initiate constructor scope
                                SConstructorDef constructorToFillStatements = null;
                                for (SConstructorDef cons : sClassDef.constructors()) {
                                        if (cons.statements().isEmpty()) constructorToFillStatements = cons;
                                }
                                assert constructorToFillStatements != null;
                                SemanticScope constructorScope = new SemanticScope(scope);
                                constructorScope.setThis(new Ins.This(sTypeDef)); // set `this`
                                for (SParameter param : constructorToFillStatements.getParameters()) {
                                        constructorScope.putLeftValue(param.name(), param);
                                }

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
                                if (null == invokeConstructor)
                                        throw new SyntaxException("no suitable super constructor to invoke in " + sClassDef, sClassDef.line_col());
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
                                                new Ins.TLoad(param, constructorScope, LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                                        constructorToFillStatements.statements().add(putField);
                                }

                                // parse this constructor
                                for (Statement stmt : astClass.statements) {
                                        parseStatement(
                                                stmt,
                                                VoidType.get(),
                                                constructorScope,
                                                constructorToFillStatements.statements(),
                                                constructorToFillStatements.exceptionTables(),
                                                true);
                                }

                                // parse method
                                int methodSize = sClassDef.methods().size();
                                List<SMethodDef> methods = sClassDef.methods();
                                for (int i = 0; i < methodSize; i++) {
                                        SMethodDef method = methods.get(i);
                                        parseAnnoValues(method.annos());
                                        parseMethod(method, methodToStatements.get(method), scope);
                                }

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
                                                                true);
                                                }
                                        }
                                }
                        } else if (sTypeDef instanceof SInterfaceDef) {
                                SInterfaceDef sInterfaceDef = (SInterfaceDef) sTypeDef;
                                InterfaceDef astInterface = originalInterfaces.get(sInterfaceDef.fullName());

                                parseAnnoValues(sInterfaceDef.annos());

                                SemanticScope scope = new SemanticScope(sInterfaceDef);
                                // parse method
                                for (SMethodDef method : sInterfaceDef.methods()) {
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
                                                true);
                                }
                        } else throw new IllegalArgumentException("wrong STypeDefType " + sTypeDef.getClass());
                }
                return typeDefSet;
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
                if (!methodDef.statements().isEmpty() || methodDef.modifiers().contains(SModifier.ABSTRACT)) return;
                SemanticScope scope = new SemanticScope(superScope);
                if (!methodDef.modifiers().contains(SModifier.STATIC)) {
                        scope.setThis(new Ins.This(scope.type()));
                }
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
                                false);
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
                                                        map.put(f, v);
                                                        continue out;
                                                }
                                        }
                                }
                                // not found, check defaultValue
                                if (f.defaultValue() != null) {
                                        map.put(f, f.defaultValue());
                                } else
                                        throw new SyntaxException(f.name() + " missing",
                                                anno == null ? LineCol.SYNTHETIC : anno.line_col());
                        }
                        sAnno.values().putAll(map);
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
                return unescape(str, lineCol).length() == 1;
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
         * </ul>
         *
         * @param statement           instructions
         * @param scope               scope that contains local variables and local methods
         * @param instructions        currently parsing {@link SInvokable} object instructions
         * @param exceptionTable      the exception table (start,end,handle,type)
         * @param doNotParseMethodDef the methodDef should not be parsed( in this case, they should be outer methods instead of inner methods)
         * @throws SyntaxException compile error
         */
        private void parseStatement(Statement statement, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions, List<ExceptionTable> exceptionTable, boolean doNotParseMethodDef) throws SyntaxException {
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
                        parseInstructionFromIf((AST.If) statement, methodReturnType, scope, instructions, exceptionTable);
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
                        if (!getTypeWithName("java.lang.Throwable", LineCol.SYNTHETIC).isAssignableFrom(throwable.type())) {
                                // cast to throwable
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(getLang_castToThrowable(), LineCol.SYNTHETIC);
                                invokeStatic.arguments().add(throwable);
                                throwable = invokeStatic;
                        }
                        Ins.AThrow aThrow = new Ins.AThrow(throwable, statement.line_col());
                        instructions.add(aThrow);
                } else if (statement instanceof AST.Try) {
                        parseInstructionFromTry((AST.Try) statement, methodReturnType, scope, instructions, exceptionTable);
                } else if (statement instanceof AST.Synchronized) {
                        parseInstructionFromSynchronized((AST.Synchronized) statement, methodReturnType, scope, instructions, exceptionTable);
                } else if (statement instanceof MethodDef) {
                        if (!doNotParseMethodDef)
                                parseInnerMethod((MethodDef) statement, scope);
                } else if (!(statement instanceof AST.StaticScope || statement instanceof AST.Pass)) {
                        throw new LtBug("unknown statement " + statement);
                }
        }

        /**
         * {@link Lang#castToThrowable(Object)}
         */
        private SMethodDef Lang_castToThrowable;

        /**
         * @return {@link Lang#castToThrowable(Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_castToThrowable() throws SyntaxException {
                if (Lang_castToThrowable == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
         * method parameters would capture all existing local variables, ahead of params that the inner method requires<br>
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
                // check method name
                if (scope.containsInnerMethod(methodDef.name))
                        throw new SyntaxException("duplicate inner method name", methodDef.line_col());

                // inner method cannot have modifiers or annotations
                if (!methodDef.modifiers.isEmpty()) throw new SyntaxException("inner method cannot have modifiers", methodDef.line_col());
                if (!methodDef.annos.isEmpty()) throw new SyntaxException("inner method cannot have annotations", methodDef.line_col());

                // check param names, see if it's already used
                // also, init values are not allowed
                for (VariableDef v : methodDef.params) {
                        if (null != scope.getLeftValue(v.getName())) {
                                throw new SyntaxException(v.getName() + " is already used", v.line_col());
                        }
                        if (v.getInit() != null) {
                                throw new SyntaxException("parameters of inner methods cannot have default value", v.line_col());
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
                String generatedMethodName = methodDef.name + "$LessTyping$InnerMethod$";
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
                scope.addMethodDef(name, new SemanticScope.MethodRecorder(m, paramCount));

                parseMethod(m, newMethodDef.body, scope.parent);

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
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromSynchronized(
                AST.Synchronized aSynchronized,
                STypeDef methodReturnType,
                SemanticScope scope,
                List<Instruction> instructions,
                List<ExceptionTable> exceptionTable) throws SyntaxException {

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
                        parseStatement(stmt, methodReturnType, subScope, instructionList, exceptionTable, false);
                }
                if (instructionList.size() == 0) instructionList.add(new Ins.Nop());

                int returnCount = 0;
                for (Instruction ins : instructionList) if (ins instanceof Ins.TReturn) ++returnCount;

                List<Ins.MonitorExit> exits = new ArrayList<>(stack.size());
                List<Ins.MonitorExit> exits2 = new ArrayList<>(stack.size());
                List<List<Ins.MonitorExit>> listOfMonitorExits = new ArrayList<>();
                for (int i = 0; i < returnCount; ++i) listOfMonitorExits.add(new ArrayList<>());

                while (!stack.empty()) {
                        Ins.MonitorEnter monitorEnter = stack.pop();
                        exits.add(new Ins.MonitorExit(monitorEnter));
                        exits2.add(new Ins.MonitorExit(monitorEnter));
                        for (List<Ins.MonitorExit> list : listOfMonitorExits) {
                                list.add(new Ins.MonitorExit(monitorEnter));
                        }
                }

                returnCount = 0;
                for (int i = 0; i < instructionList.size(); ++i) {
                        Instruction ins = instructionList.get(i);

                        if (ins instanceof Ins.TReturn) {
                                i += insertInstructionsBeforeReturn(instructionList, i, listOfMonitorExits.get(returnCount++), subScope);
                        }
                }

                instructions.addAll(instructionList);
                instructions.addAll(exits);

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
                instructions.addAll(exits2);
                instructions.add(aThrow); // athrow
                instructions.add(nop); // nop

                ExceptionTable table = new ExceptionTable(instructionList.get(0), exits.get(0), exStore, null);
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
        private int insertInstructionsBeforeReturn(List<Instruction> instructions, int returnIndex, List<? extends Instruction> toInsert, SemanticScope scope) {
                Ins.TReturn tReturn = (Ins.TReturn) instructions.remove(returnIndex); // get return
                Value returnValue = tReturn.value();
                if (returnValue != null) {
                        LocalVariable tmp = new LocalVariable(returnValue.type(), false);
                        scope.putLeftValue(scope.generateTempName(), tmp);

                        Ins.TStore TStore = new Ins.TStore(tmp, returnValue, scope, LineCol.SYNTHETIC); // store the value
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
         * {@link Lang#throwableWrapperObject(Throwable)}
         */
        private SMethodDef Lang_throwableWrapperObject;

        /**
         * @return {@link Lang#throwableWrapperObject(Throwable)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_throwableWrapperObject() throws SyntaxException {
                if (Lang_throwableWrapperObject == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromTry(AST.Try aTry, STypeDef methodReturnType, SemanticScope scope, List<Instruction> instructions, List<ExceptionTable> exceptionTable) throws SyntaxException {
                // try ...
                SemanticScope scopeA = new SemanticScope(scope);
                List<Instruction> insA = new ArrayList<>(); // instructions in scope A
                for (Statement stmt : aTry.statements) {
                        parseStatement(stmt, methodReturnType, scopeA, insA, exceptionTable, false);
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
                                if (i instanceof Ins.TReturn) {
                                        startToEnd.put(start, insA.get(i1 - 1));
                                        start = null;
                                }
                        }
                }
                // put last pair
                if (start != null) startToEnd.put(start, insA.get(insA.size() - 1));
                if (startToEnd.isEmpty() && insA.size() == 1) {
                        assert insA.get(0) instanceof Ins.TReturn;
                        insA.add(0, new Ins.Nop());
                        startToEnd.put(insA.get(0), insA.get(0));
                }

                // the map preparation is done

                // build normal finally (D1)
                List<Instruction> normalFinally = new ArrayList<>();
                for (Statement stmt : aTry.fin) {
                        parseStatement(stmt, methodReturnType, new SemanticScope(scope), normalFinally, exceptionTable, false);
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
                        parseStatement(stmt, methodReturnType, exceptionFinallyScope, exceptionFinally, exceptionTable, false);
                }
                exceptionFinally.add(aThrow);

                // add D into every position before return in insA
                for (int i = 0; i < insA.size(); ++i) {
                        Instruction ins = insA.get(i);
                        if (ins instanceof Ins.TReturn) {
                                List<Instruction> list = new ArrayList<>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType,
                                                new SemanticScope(scopeA),
                                                // it shouldn't be sub scope of A
                                                // because in the code , it can't access A block values
                                                list, exceptionTable, false);
                                }
                                i += insertInstructionsBeforeReturn(insA, i, list, scopeA);
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
                                        if (tmp instanceof Ins.Goto) {
                                                exclusive = tmp;
                                        } else if (tmp instanceof Ins.TStore) {
                                                exclusive = insA.get(++cursor);
                                        } else throw new LtBug("the instruction after should only be Goto or TStore");
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
                        LineCol.SYNTHETIC
                );
                insCatch.add(storeUnwrapped);

                for (Statement stmt : aTry.catchStatements) {
                        parseStatement(stmt, methodReturnType, catchScope, insCatch, exceptionTable, false);
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
                                if (i instanceof Ins.TReturn) {
                                        // i is return
                                        catch_startToEnd.put(catch_start, insCatch.get(i1 - 1));
                                        catch_start = null;
                                }
                        }
                }
                // record last pair
                if (catch_start != null) catch_startToEnd.put(catch_start, insCatch.get(insCatch.size() - 1));

                // check return and add finally
                for (int i = 0; i < insCatch.size(); ++i) {
                        Instruction ins = insCatch.get(i);
                        if (ins instanceof Ins.TReturn) {
                                List<Instruction> list = new ArrayList<>();
                                for (Statement stmt : aTry.fin) {
                                        parseStatement(stmt, methodReturnType, new SemanticScope(catchScope), list, exceptionTable, false);
                                }
                                i += insertInstructionsBeforeReturn(insCatch, i, list, catchScope);
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
                                        if (tmp instanceof Ins.Goto) {
                                                exclusive = tmp;
                                        } else if (tmp instanceof Ins.TStore) {
                                                exclusive = insCatch.get(++cursor);
                                        } else throw new LtBug("the instruction after should only be Goto or TStore");
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
         * goto here
         * B
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
                getIterator.arguments().add(parseValueFromExpression(aFor.exp, null, scope));
                // aStore
                LocalVariable localVariable = new LocalVariable(getTypeWithName("lt.lang.LtIterator", LineCol.SYNTHETIC), false);
                scope.putLeftValue(scope.generateTempName(), localVariable);
                Ins.TStore tStore = new Ins.TStore(localVariable, getIterator, scope, LineCol.SYNTHETIC);
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
                Ins.TStore tStore1 = new Ins.TStore(newLocal, next, subScope, LineCol.SYNTHETIC);
                instructions.add(tStore1); // name = it.next()
                for (Statement stmt : aFor.body) {
                        parseStatement(
                                stmt,
                                methodReturnType,
                                subScope,
                                instructions,
                                exceptionTable, false);
                }
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
                List<Instruction> ins = new ArrayList<>();
                for (Statement stmt : aWhile.statements) {
                        parseStatement(
                                stmt,
                                methodReturnType,
                                scope,
                                ins,
                                exceptionTable, false);
                }

                Value condition = parseValueFromExpression(aWhile.condition, BoolTypeDef.get(), scope);

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
                         * if B goto A
                         * C
                         */
                        instructions.addAll(ins); // A
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
                         * goto while
                         * C
                         */
                        Ins.Nop nop = new Ins.Nop(); // end of loop (C)
                        Ins.IfEq ifEq; // if B == 0 (false) goto C
                        ifEq = new Ins.IfEq(condition, nop, aWhile.line_col());
                        instructions.add(ifEq); // if not B goto C

                        instructions.addAll(ins); // A
                        instructions.add(new Ins.Goto(ifEq)); // goto while
                        instructions.add(nop); // C
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
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromIf(AST.If anIf,
                                            STypeDef methodReturnType,
                                            SemanticScope scope,
                                            List<Instruction> instructions,
                                            List<ExceptionTable> exceptionTable) throws SyntaxException {
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
                                        exceptionTable, false);
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
                        tReturn = new Ins.TReturn(null, Ins.TReturn.Return, ret.line_col());
                } else {
                        Value v =
                                parseValueFromExpression(ret.exp, methodReturnType, scope);
                        STypeDef type = v.type();
                        int ins;
                        if (type.equals(IntTypeDef.get())
                                || type.equals(ShortTypeDef.get())
                                || type.equals(ByteTypeDef.get())
                                || type.equals(BoolTypeDef.get())
                                || type.equals(CharTypeDef.get())) {
                                ins = Ins.TReturn.IReturn;
                        } else if (type.equals(LongTypeDef.get())) {
                                ins = Ins.TReturn.LReturn;
                        } else if (type.equals(FloatTypeDef.get())) {
                                ins = Ins.TReturn.FReturn;
                        } else if (type.equals(DoubleTypeDef.get())) {
                                ins = Ins.TReturn.DReturn;
                        } else {
                                ins = Ins.TReturn.AReturn;
                        }

                        tReturn = new Ins.TReturn(v, ins, ret.line_col());
                }
                instructions.add(tReturn);
        }

        /**
         * {@link Lang#putField(Object, String, Object, Class)}
         */
        private SMethodDef Lang_putField;

        /**
         * @return {@link Lang#putField(Object, String, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_putField() throws SyntaxException {
                if (null == Lang_putField) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
         * it's NOT directly invoked in {@link #parseStatement(Statement, STypeDef, SemanticScope, List, List, boolean)}<br>
         * but in {@link #parseValueFromAssignment(AST.Assignment, SemanticScope)}<br>
         * the instructions should be {@link ValuePack} instruction list
         *
         * @param assignment   Assignment
         * @param scope        current scope
         * @param instructions instruction list
         * @throws SyntaxException compile error
         */
        private void parseInstructionFromAssignment(AST.Assignment assignment, SemanticScope scope, List<Instruction> instructions) throws SyntaxException {
                // []= means Tastore or <set(?,?) or put(?,?)> ==> (reflectively invoke)
                // []+= means TALoad then Tastore, or get(?) then <set(?,?) or put(?,?)> ==> then set/put step would be invoked reflectively
                String op = assignment.op;
                if (!op.equals("=")) {
                        switch (op) {
                                case "+=": {
                                        // change to Assign(assignTo,TwoVariableOperation(assignTo,"+",assignFrom))
                                        AST.Assignment newAssign = new AST.Assignment(assignment.assignTo, "=", new TwoVariableOperation("+", assignment.assignTo, assignment.assignFrom, assignment.line_col()), assignment.line_col());
                                        parseInstructionFromAssignment(newAssign, scope, instructions);
                                        return;
                                }
                                case "-=": {
                                        // change to Assign(assignTo,TwoVariableOperation(assignTo,"-",assignFrom))
                                        AST.Assignment newAssign = new AST.Assignment(assignment.assignTo, "=", new TwoVariableOperation("-", assignment.assignTo, assignment.assignFrom, assignment.line_col()), assignment.line_col());
                                        parseInstructionFromAssignment(newAssign, scope, instructions);
                                        return;
                                }
                                case "*=": {
                                        // change to Assign(assignTo,TwoVariableOperation(assignTo,"*",assignFrom))
                                        AST.Assignment newAssign = new AST.Assignment(assignment.assignTo, "=", new TwoVariableOperation("*", assignment.assignTo, assignment.assignFrom, assignment.line_col()), assignment.line_col());
                                        parseInstructionFromAssignment(newAssign, scope, instructions);
                                        return;
                                }
                                case "/=": {
                                        // change to Assign(assignTo,TwoVariableOperation(assignTo,"/",assignFrom))
                                        AST.Assignment newAssign = new AST.Assignment(assignment.assignTo, "=", new TwoVariableOperation("/", assignment.assignTo, assignment.assignFrom, assignment.line_col()), assignment.line_col());
                                        parseInstructionFromAssignment(newAssign, scope, instructions);
                                        return;
                                }
                                case "%=": {
                                        // change to Assign(assignTo,TwoVariableOperation(assignTo,"%",assignFrom))
                                        AST.Assignment newAssign = new AST.Assignment(assignment.assignTo, "=", new TwoVariableOperation("%", assignment.assignTo, assignment.assignFrom, assignment.line_col()), assignment.line_col());
                                        parseInstructionFromAssignment(newAssign, scope, instructions);
                                        return;
                                }
                                default:
                                        throw new SyntaxException("unknown assign operator " + op, assignment.line_col());
                        }
                }
                // else
                // simply assign `assignFrom` to `assignTo`

                Value assignTo = parseValueFromExpression(assignment.assignTo, null, scope);
                if (!(assignTo instanceof Instruction)) throw new SyntaxException(
                        "cannot assign value to " + assignment.assignTo,
                        assignment.assignTo.line_col());
                Value assignFrom = parseValueFromExpression(assignment.assignFrom, null, scope);
                // the following actions would be assign work
                if (assignTo instanceof Ins.GetField) {
                        // field
                        instructions.add(new Ins.PutField(
                                ((Ins.GetField) assignTo).field(),
                                ((Ins.GetField) assignTo).object(),
                                cast(((Ins.GetField) assignTo).field().type(), assignFrom, ((Ins.GetField) assignTo).line_col()),
                                ((Ins.GetField) assignTo).line_col()));
                } else if (assignTo instanceof Ins.GetStatic) {
                        // static
                        instructions.add(new Ins.PutStatic(((Ins.GetStatic) assignTo).field(), assignFrom, ((Ins.GetStatic) assignTo).line_col()));
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
                                        cast(arrayType.type(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(IntTypeDef.get())) {
                                // int[] IASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.IASTORE,
                                        TALoad.index(),
                                        cast(IntTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(LongTypeDef.get())) {
                                // long[] LASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.LASTORE,
                                        TALoad.index(),
                                        cast(LongTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(ShortTypeDef.get())) {
                                // short[] SASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.SASTORE,
                                        TALoad.index(),
                                        cast(ShortTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(ByteTypeDef.get()) || arrayType.type().equals(BoolTypeDef.get())) {
                                // byte[]/boolean[] BASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.BASTORE,
                                        TALoad.index(),
                                        cast(ByteTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(FloatTypeDef.get())) {
                                // float[] FASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.FASTORE,
                                        TALoad.index(),
                                        cast(FloatTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(DoubleTypeDef.get())) {
                                // double[] DASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.DASTORE,
                                        TALoad.index(),
                                        cast(DoubleTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else if (arrayType.type().equals(CharTypeDef.get())) {
                                // char[] CASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.CASTORE,
                                        TALoad.index(),
                                        cast(CharTypeDef.get(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        } else {
                                // object[] AASTORE
                                instructions.add(new Ins.TAStore(
                                        TALoad.arr(),
                                        Ins.TAStore.AASTORE,
                                        TALoad.index(),
                                        cast(arrayType.type(), assignFrom, assignment.line_col()),
                                        assignment.line_col()));
                        }
                } else if (assignTo instanceof Ins.TLoad) {
                        // local variable
                        instructions.add(new Ins.TStore(
                                ((Ins.TLoad) assignTo).value(),
                                cast(
                                        assignTo.type(),
                                        assignFrom,
                                        assignment.line_col()),
                                scope, ((Ins.TLoad) assignTo).line_col()));
                } else if (assignTo instanceof Ins.InvokeStatic) {
                        // assignTo should be lt.lang.Lang.getField(o,name)
                        // which means
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) assignTo;
                        if (invokeStatic.invokable() instanceof SMethodDef) {
                                SMethodDef method = (SMethodDef) invokeStatic.invokable();
                                if (method.declaringType().fullName().equals("lt.lang.Lang")) {
                                        if (method.name().equals("getField")) {
                                                // dynamically get field
                                                // invoke lt.lang.Lang.putField(o,name,value)
                                                SMethodDef putField = getLang_putField();
                                                Ins.InvokeStatic invoke = new Ins.InvokeStatic(putField, ((Ins.InvokeStatic) assignTo).line_col());
                                                invoke.arguments().add(invokeStatic.arguments().get(0));
                                                invoke.arguments().add(invokeStatic.arguments().get(1));
                                                invoke.arguments().add(assignFrom);
                                                invoke.arguments().add(
                                                        new Ins.GetClass(scope.type(),
                                                                (SClassDef) getTypeWithName("java.lang.Class", assignment.line_col())));

                                                // add into instructin list
                                                instructions.add(invoke);

                                                return;
                                        }
                                }
                        }
                        throw new SyntaxException("cannot assign value to " + assignment.assignTo, assignment.assignTo.line_col());
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
                                Value target = it.next();
                                while (it.hasNext()) list.add(it.next());
                                list.add(assignFrom);

                                instructions.add(invokeMethodWithArgs(
                                        assignment.line_col(),
                                        target,
                                        "set",
                                        list,
                                        scope));
                                return;
                        }
                        throw new SyntaxException("cannot assign value to " + assignment.assignTo, assignment.assignTo.line_col());
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
                                        instructions.add(invokeMethodWithArgs(
                                                assignment.line_col(),
                                                invoke.target(),
                                                "set",
                                                list,
                                                scope));
                                        return;
                                }
                        }
                        throw new SyntaxException("cannot assign value to " + assignment.assignTo, assignment.assignTo.line_col());
                } else {
                        throw new SyntaxException(assignment.assignTo + " cannot be left value", assignment.assignTo.line_col());
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

                if (field == null) {
                        // field is null, define local variable
                        if (scope.getLeftValue(variableDef.getName()) == null) {
                                boolean canChange = true;
                                for (Modifier m : variableDef.getModifiers()) {
                                        if (m.modifier.equals("val")) {
                                                canChange = false;
                                        }
                                }
                                LocalVariable localVariable = new LocalVariable(type, canChange);
                                scope.putLeftValue(variableDef.getName(), localVariable);
                        } else
                                throw new SyntaxException(variableDef.getName() + " is already defined", variableDef.line_col());
                }

                if (variableDef.getInit() != null) {
                        // variable def with a init value

                        Value v = parseValueFromExpression(
                                variableDef.getInit(),
                                type,
                                scope);

                        ValuePack pack = new ValuePack(true);
                        if (null != field) {
                                if (field.modifiers().contains(SModifier.STATIC)) {
                                        // putStatic
                                        pack.instructions().add(new Ins.PutStatic(field, v, variableDef.line_col()));
                                        Ins.GetStatic getStatic = new Ins.GetStatic(field, variableDef.line_col());
                                        pack.instructions().add(getStatic);
                                } else {
                                        // putField
                                        pack.instructions().add(new Ins.PutField(field, scope.getThis(), v, variableDef.line_col()));
                                        Ins.GetField getField = new Ins.GetField(field, scope.getThis(), variableDef.line_col());
                                        pack.instructions().add(getField);
                                }
                        } else {
                                // else field not found
                                // local variable
                                LocalVariable localVariable = (LocalVariable) scope.getLeftValue(variableDef.getName());
                                pack.instructions().add(new Ins.TStore(localVariable, v, scope, variableDef.line_col()));
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
         * <li>{@link lt.compiler.syntactic.AST.ArrayExp} =&gt; array/java.util.LinkedList</li>
         * <li>{@link lt.compiler.syntactic.AST.MapExp} =&gt; java.util.LinkedHashMap</li>
         * <li>{@link lt.compiler.syntactic.AST.Procedure}</li>
         * <li>{@link lt.compiler.syntactic.AST.Lambda}</li>
         * <li>{@link lt.compiler.syntactic.AST.TypeOf} =&gt; {@link lt.compiler.semantic.Ins.GetClass}</li>
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
                                        throw new SyntaxException(exp + " cannot be converted into " + requiredType.fullName(), exp.line_col());
                                }
                        } catch (NumberFormatException e) {
                                throw new SyntaxException(exp + " is not valid " + requiredType, exp.line_col());
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
                                                throw new IllegalArgumentException(literal + " for bool literal");
                                }
                                BoolValue boolValue = new BoolValue(b);
                                if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                        return boolValue;
                                } else {
                                        return boxPrimitive(boolValue, exp.line_col());
                                }
                        } else throw new SyntaxException(exp + " cannot be converted into " + requiredType, exp.line_col());
                } else if (exp instanceof StringLiteral) {
                        String str = ((StringLiteral) exp).literal();
                        // remove "" or ''
                        str = str.substring(1);
                        str = str.substring(0, str.length() - 1);
                        str = unescape(str, exp.line_col());
                        if (isChar(requiredType, (StringLiteral) exp, exp.line_col())) {
                                if (str.length() == 1) {
                                        CharValue charValue = new CharValue(str.charAt(0));
                                        if (requiredType == null || requiredType instanceof PrimitiveTypeDef) {
                                                return charValue;
                                        } else {
                                                return boxPrimitive(charValue, exp.line_col());
                                        }
                                } else
                                        throw new SyntaxException(exp + " cannot be converted into char, char must hold one character", exp.line_col());
                        } else if (requiredType == null || requiredType.isAssignableFrom(getTypeWithName("java.lang.String", exp.line_col()))) {
                                StringConstantValue s = new StringConstantValue(str);
                                s.setType((SClassDef) getTypeWithName("java.lang.String", exp.line_col()));
                                return s;
                        } else throw new SyntaxException(exp + " cannot be converted into " + requiredType, exp.line_col());
                } else if (exp instanceof VariableDef) {
                        // variable def
                        // putField/putStatic/TStore
                        return parseInsFromVariableDef((VariableDef) exp, scope);
                } else if (exp instanceof AST.Invocation) {
                        // parse invocation
                        // the result can be invokeXXX or new
                        v = parseValueFromInvocation((AST.Invocation) exp, scope);
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
                } else {
                        throw new LtBug("unknown expression " + exp);
                }
                return cast(requiredType, v, exp.line_col());
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
                if (argCount > 26) throw new SyntaxException("too may arguments for a lambda expression, maximum arg count is 26", lineCol);
                String className = "lt.lang.function.Function" + argCount;
                return (SInterfaceDef) getTypeWithName(className, lineCol);
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
                SMethodDef methodToOverride = null;
                SConstructorDef constructorWithZeroParamAndCanAccess = null;
                if (requiredType == null || requiredType.fullName().equals("java.lang.Object")) {
                        SInterfaceDef interfaceDef = getDefaultLambdaFunction(lambda.params.size(), lambda.line_col());
                        requiredType = interfaceDef;
                        methodToOverride = interfaceDef.methods().get(0);
                } else {
                        // examine whether it's a functional interface
                        // or it's an abstract class with only one unimplemented method and accessible constructor with no params
                        boolean valid = false;
                        if (requiredType instanceof SClassDef) {
                                if (((SClassDef) requiredType).modifiers().contains(SModifier.ABSTRACT)) {
                                        SClassDef c = (SClassDef) requiredType;
                                        // check constructors
                                        for (SConstructorDef con : c.constructors()) {
                                                if (con.getParameters().size() == 0) {
                                                        // 0 param
                                                        if (con.modifiers().contains(SModifier.PROTECTED)
                                                                || con.modifiers().contains(SModifier.PUBLIC)
                                                                ||
                                                                (!con.modifiers().contains(SModifier.PRIVATE)
                                                                        && // is package access
                                                                        con.declaringType().pkg().equals(scope.type().pkg()))) {

                                                                // constructor can access
                                                                constructorWithZeroParamAndCanAccess = con;
                                                                break;
                                                        }
                                                }
                                        }
                                        if (constructorWithZeroParamAndCanAccess != null) {

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
                                                                                if (classes.contains(o.declaringType())) {
                                                                                        isOverridden = true;
                                                                                        break;
                                                                                }
                                                                        }
                                                                        if (!isOverridden) {
                                                                                ++count;
                                                                                if (count > 1) break out;
                                                                                methodToOverride = m;
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
                                                                                        methodToOverride = m;
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                                if (count == 1) valid = true;
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
                                                                if (interfaces.contains(o.declaringType())) {
                                                                        // overridden
                                                                        isOverridden = true;
                                                                }
                                                        }
                                                        if (!isOverridden) {
                                                                ++count;
                                                                if (count > 1) break out;
                                                                methodToOverride = m;
                                                        }
                                                }
                                        }
                                }
                                if (count == 1) valid = true;
                        }
                        if (!valid)
                                throw new SyntaxException("lambda should be subtype of functional interface or abstract class with only one unimplemented method and a constructor with no params, but got " + requiredType, lambda.line_col());
                }

                if (methodToOverride == null) throw new LtBug("methodToOverride should not be null");
                if (methodToOverride.getParameters().size() != lambda.params.size())
                        throw new SyntaxException("lambda parameter count differs from " + methodToOverride, lambda.line_col());

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

                List<Value> args = new ArrayList<>();
                for (LeftValue l : scope.getLeftValues(method.getParameters().size() - lambda.params.size())) {
                        args.add(new Ins.TLoad(l, scope, LineCol.SYNTHETIC));
                }

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
                SClassDef sClassDef = new SClassDef(LineCol.SYNTHETIC);
                typeDefSet.add(sClassDef);
                SemanticScope scope = new SemanticScope(sClassDef);

                if (isInterface) {
                        sClassDef.setParent((SClassDef) getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        sClassDef.superInterfaces().add(interfaceType);
                } else {
                        sClassDef.setParent((SClassDef) methodToOverride.declaringType());
                }
                sClassDef.setPkg(lambdaClassType.pkg());
                String className = lambdaClassType.fullName() + "$LessTyping$Lambda$";
                int i = 0;
                while (types.containsKey(className + i)) ++i;
                className += i;
                sClassDef.setFullName(className);
                types.put(className, sClassDef);

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
                                ((SClassDef) getTypeWithName("java.lang.Object", LineCol.SYNTHETIC)).constructors().get(0),
                                LineCol.SYNTHETIC
                        ));
                } else {
                        con.statements().add(new Ins.InvokeSpecial(
                                conScope.getThis(),
                                superConstructor,
                                LineCol.SYNTHETIC
                        ));
                }
                // p1
                SParameter p1 = new SParameter();
                p1.setType(getMethodHandle_Class());
                con.getParameters().add(p1);
                conScope.putLeftValue("p1", p1);
                con.statements().add(new Ins.PutField(
                        f1,
                        conScope.getThis(),
                        new Ins.TLoad(p1, conScope, LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC
                ));
                // p2
                if (!isStatic) {
                        SParameter p2 = new SParameter();
                        p2.setType(getTypeWithName("java.lang.Object", LineCol.SYNTHETIC));
                        con.getParameters().add(p2);
                        conScope.putLeftValue("p2", p2);
                        con.statements().add(new Ins.PutField(
                                f2,
                                conScope.getThis(),
                                new Ins.TLoad(p2, conScope, LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC
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
                        LineCol.SYNTHETIC
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
                        meScope.putLeftValue(param.name(), mp);
                }
                // new ArrayList
                SClassDef LinkedList_Type = (SClassDef) getTypeWithName("java.util.LinkedList", LineCol.SYNTHETIC);
                SConstructorDef LinkedList_Con = null;
                for (SConstructorDef co : LinkedList_Type.constructors()) {
                        if (co.getParameters().size() == 1 && co.getParameters().get(0).type().fullName().equals("java.util.Collection")) {
                                LinkedList_Con = co;
                                break;
                        }
                }
                if (LinkedList_Con == null) throw new LtBug("java.util.LinkedList should have constructor LinkedList(Collection)");
                Ins.New aNew = new Ins.New(LinkedList_Con, LineCol.SYNTHETIC);
                aNew.args().add(new Ins.GetField(f3, meScope.getThis(), LineCol.SYNTHETIC));
                LocalVariable localVariable = new LocalVariable(LinkedList_Type, false);
                meScope.putLeftValue("list", localVariable);
                theMethod.statements().add(
                        new Ins.TStore(localVariable, aNew, meScope, LineCol.SYNTHETIC)
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
                for (SParameter param : methodToOverride.getParameters()) {
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
                        if (LinkedList_add2 == null) throw new LtBug("java.util.LinkedList should have method add(int,Object)");
                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(
                                new Ins.TLoad(localVariable, meScope, LineCol.SYNTHETIC),
                                LinkedList_add2,
                                LineCol.SYNTHETIC
                        );
                        invokeVirtual.arguments().add(new IntValue(0));
                        invokeVirtual.arguments().add(new Ins.GetField(f2, meScope.getThis(), LineCol.SYNTHETIC));
                        theMethod.statements().add(invokeVirtual);
                }
                // invoke the method handle
                SMethodDef MethodHandle_invokeWithArguments = null;
                for (SMethodDef m : ((SClassDef) getTypeWithName("java.lang.invoke.MethodHandle", LineCol.SYNTHETIC)).methods()) {
                        if (m.name().equals("invokeWithArguments")
                                && m.getParameters().size() == 1
                                && m.getParameters().get(0).type().fullName().equals("java.util.List")) {
                                MethodHandle_invokeWithArguments = m;
                                break;
                        }
                }
                if (MethodHandle_invokeWithArguments == null) throw new LtBug("java.lang.invoke.MethodHandle should have method invokeWithArguments(Object[])");
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
                        int mode;
                        if (theMethod.getReturnType().equals(IntTypeDef.get())
                                ||
                                theMethod.getReturnType().equals(ByteTypeDef.get())
                                ||
                                theMethod.getReturnType().equals(BoolTypeDef.get())
                                ||
                                theMethod.getReturnType().equals(ShortTypeDef.get())
                                ||
                                theMethod.getReturnType().equals(CharTypeDef.get())
                                ) {
                                mode = Ins.TReturn.IReturn;
                        } else if (theMethod.getReturnType().equals(DoubleTypeDef.get())) {
                                mode = Ins.TReturn.DReturn;
                        } else if (theMethod.getReturnType().equals(FloatTypeDef.get())) {
                                mode = Ins.TReturn.FReturn;
                        } else if (theMethod.getReturnType().equals(LongTypeDef.get())) {
                                mode = Ins.TReturn.LReturn;
                        } else {
                                mode = Ins.TReturn.AReturn;
                        }
                        theMethod.statements().add(
                                new Ins.TReturn(
                                        cast(theMethod.getReturnType(),
                                                invokeMethodHandle,
                                                LineCol.SYNTHETIC),
                                        mode,
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
                        new AST.Access(null, methodName, procedure.line_col()), new Expression[]{}, procedure.line_col());
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
                Ins.NewMap newMap = new Ins.NewMap(getTypeWithName("java.util.LinkedHashMap", mapExp.line_col()));

                SClassDef Object_type = (SClassDef) getTypeWithName("java.lang.Object", mapExp.line_col());
                for (Map.Entry<Expression, Expression> expEntry : mapExp.map.entrySet()) {
                        Value key = parseValueFromExpression(expEntry.getKey(), Object_type, scope);
                        Value value = parseValueFromExpression(expEntry.getValue(), Object_type, scope);
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
                                getTypeWithName("java.util.LinkedList", arrayExp.line_col())
                        );
                        SClassDef Object_type = (SClassDef) getTypeWithName("java.lang.Object", arrayExp.line_col());
                        // init values
                        for (Expression exp : arrayExp.list) {
                                newList.initValues().add(
                                        parseValueFromExpression(exp, Object_type, scope)
                                );
                        }
                        return newList;
                }
        }

        /**
         * parse assignment<br>
         * generate a ValuePack, then invoke {@link #parseInstructionFromAssignment(AST.Assignment, SemanticScope, List)}<br>
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
                parseInstructionFromAssignment(exp, scope, pack.instructions());
                Value assignTo = parseValueFromExpression((exp).assignTo, null, scope);
                assert assignTo instanceof Instruction;
                pack.instructions().add((Instruction) assignTo);
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
         * @param invokeOn   invokeOn (a)
         * @param methodName name of the method
         * @param args       arguments
         * @param scope      current scope
         * @return a subclass of {@link lt.compiler.semantic.Ins.Invoke}
         * @throws SyntaxException compile error
         */
        private Ins.Invoke invokeMethodWithArgs(LineCol lineCol, Value invokeOn, String methodName, List<Value> args, SemanticScope scope) throws SyntaxException {
                List<SMethodDef> methods = new ArrayList<>();
                findMethodFromTypeWithArguments(lineCol, methodName, args, scope.type(), invokeOn.type(), FIND_MODE_NON_STATIC, methods, true);
                if (methods.isEmpty()) {
                        // invoke dynamic
                        args.add(0, invokeOn);
                        return new Ins.InvokeDynamic(
                                getInvokeDynamicBootstrapMethod(),
                                methodName,
                                args,
                                getTypeWithName("java.lang.Object", lineCol)
                                , Dynamic.INVOKE_STATIC, lineCol);
                } else {
                        SMethodDef method = findBestMatch(args, methods, lineCol);
                        args = castArgsForMethodInvoke(args, method.getParameters(), lineCol);
                        if (method.modifiers().contains(SModifier.PRIVATE)) {
                                // invoke special
                                Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(invokeOn, method, lineCol);
                                invokeSpecial.arguments().addAll(args);
                                return invokeSpecial;
                        } else if (method.declaringType() instanceof SInterfaceDef) {
                                // invoke interface
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(invokeOn, method, lineCol);
                                invokeInterface.arguments().addAll(args);
                                return invokeInterface;
                        } else {
                                // invoke virtual
                                Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(invokeOn, method, lineCol);
                                invokeVirtual.arguments().addAll(args);
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
                        return invokeMethodWithArgs(lineCol, left, methodName, args, scope);
                }
        }

        /**
         * {@link Lang#compare(int, int)}
         */
        private SMethodDef Lang_compare;

        /**
         * @return {@link Lang#compare(int, int)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_compare() throws SyntaxException {
                if (Lang_compare == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
         * @param compare_mode compare_mode {@link Lang#COMPARE_MODE_EQ} {@link Lang#COMPARE_MODE_GT} {@link Lang#COMPARE_MODE_LT}
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
                        if (comparable.isAssignableFrom(left.type())) { // Comparable
                                // left.compareTo(right)
                                SMethodDef m = getComparable_compareTo();
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(left, m, lineCol);
                                invokeInterface.arguments().add(right);

                                // Lang.compare(left.compareTo(right), compare_mode)
                                SMethodDef compare = getLang_compare();
                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(compare, lineCol);
                                invokeStatic.arguments().add(invokeInterface);
                                invokeStatic.arguments().add(new IntValue(compare_mode));
                                return invokeStatic;
                        } else {
                                List<Value> args = new ArrayList<>();
                                args.add(right);
                                return invokeMethodWithArgs(lineCol, left, methodName, args, scope);
                        }
                }
        }

        /**
         * {@link Lang#compareRef(Object, Object)}
         */
        private SMethodDef Lang_compareRef;

        /**
         * @return {@link Lang#compareRef(Object, Object)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_compareRef() throws SyntaxException {
                if (Lang_compareRef == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
                        case "..": {
                                Value intLeft = cast(IntTypeDef.get(), left, lineCol);
                                Value intRight = cast(IntTypeDef.get(), right, lineCol);

                                Ins.New n = new Ins.New(getRangeListCons(), lineCol);
                                n.args().add(intLeft);
                                n.args().add(intRight);
                                n.args().add(new BoolValue(true)); // end_inclusive
                                return n;
                        }
                        case ".:": {
                                Value intLeft = cast(IntTypeDef.get(), left, lineCol);
                                Value intRight = cast(IntTypeDef.get(), right, lineCol);

                                Ins.New n = new Ins.New(getRangeListCons(), lineCol);
                                n.args().add(intLeft);
                                n.args().add(intRight);
                                n.args().add(new BoolValue(false)); // end_exclusive
                                return n;
                        }
                        case "^^":
                                if (left.type() instanceof PrimitiveTypeDef ||
                                        getTypeWithName("java.lang.Number", LineCol.SYNTHETIC)
                                                .isAssignableFrom(left.type())) {
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
                                        return invokeMethodWithArgs(lineCol, left, "pow", args, scope);
                                }
                        case "*":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Imul, Lang.multiply, right, scope, lineCol);
                        case "/":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Idiv, Lang.divide, right, scope, lineCol);
                        case "%":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Irem, Lang.remainder, right, scope, lineCol);
                        case "+":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iadd, Lang.add, right, scope, lineCol);
                        case "-":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Isub, Lang.subtract, right, scope, lineCol);
                        case "<<":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishl, Lang.shiftLeft, right, scope, lineCol);
                        case ">>":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ishr, Lang.shiftRight, right, scope, lineCol);
                        case ">>>":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iushr, Lang.unsignedShiftRight, right, scope, lineCol);
                        case ">":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_GT, Lang.gt, right, scope, lineCol);
                        case "<":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_LT, Lang.lt, right, scope, lineCol);
                        case ">=":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_GT | Lang.COMPARE_MODE_EQ, Lang.ge, right, scope, lineCol);
                        case "<=":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_LT | Lang.COMPARE_MODE_EQ, Lang.le, right, scope, lineCol);
                        case "==":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_EQ, "equals", right, scope, lineCol);
                        case "!=":
                                Value eq = parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_EQ, "equals", right, scope, lineCol);
                                return parseValueFromTwoVarOpILFD(eq, Ins.TwoVarOp.Ixor, null, new BoolValue(true), scope, lineCol);
                        case "===":
                                if (left.type() instanceof PrimitiveTypeDef && right.type() instanceof PrimitiveTypeDef) {
                                        return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_EQ, null, right, scope, lineCol);
                                } else {
                                        if (left.type() instanceof PrimitiveTypeDef || right.type() instanceof PrimitiveTypeDef) {
                                                throw new SyntaxException("reference type cannot compare to primitive type", lineCol);
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
                                        return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_LT | Lang.COMPARE_MODE_GT, null, right, scope, lineCol);
                                } else {
                                        if (left.type() instanceof PrimitiveTypeDef || right.type() instanceof PrimitiveTypeDef) {
                                                throw new SyntaxException("reference type cannot compare to primitive type", lineCol);
                                        } else {
                                                // Lang.compareRef(left,right)
                                                SMethodDef m = getLang_compareRef();
                                                Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(m, lineCol);
                                                invokeStatic.arguments().add(left);
                                                invokeStatic.arguments().add(right);

                                                // ! Lang.compareRef(left,right)
                                                return parseValueFromTwoVarOpILFD(invokeStatic, Ins.TwoVarOp.Ixor, null, new BoolValue(true), scope, lineCol);
                                        }
                                }
                        case "=:=":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_EQ, Lang.equal, right, scope, lineCol);
                        case "!:=":
                                return parseValueFromTwoVarOpCompare(left, Lang.COMPARE_MODE_EQ, Lang.notEqual, right, scope, lineCol);
                        case "is": {
                                // invoke static Lang.is
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
                                // invoke static Lang.not
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
                                return invokeMethodWithArgs(lineCol, right, "contains", args, scope);
                        case "&":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Iand, Lang.and, right, scope, lineCol);
                        case "^":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ixor, Lang.xor, right, scope, lineCol);
                        case "|":
                                return parseValueFromTwoVarOpILFD(left, Ins.TwoVarOp.Ior, Lang.or, right, scope, lineCol);
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
                        default:
                                throw new SyntaxException("unknown two variable operator " + op, lineCol);
                }
        }

        /**
         * {@link Lang#is(Object, Object, Class)}
         */
        private SMethodDef Lang_is;

        /**
         * @return {@link Lang#is(Object, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_is() throws SyntaxException {
                if (Lang_is == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
         * {@link Lang#not(Object, Object, Class)}
         */
        private SMethodDef Lang_not;

        /**
         * @return {@link Lang#not(Object, Object, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_not() throws SyntaxException {
                if (Lang_not == null) {
                        SClassDef cls = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
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
                        throw new SyntaxException(exp.operator() + " cannot operate on " + e, exp.line_col());
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
                                return invokeMethodWithArgs(exp.line_col(), v, "logicNot", new ArrayList<>(), scope);
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
                                return invokeMethodWithArgs(exp.line_col(), v, "not", new ArrayList<>(), scope);
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
                                return invokeMethodWithArgs(exp.line_col(), v, "negate", new ArrayList<>(), scope);
                        }
                } else throw new SyntaxException("unknown one variable operator " + (unary ? (op + "v") : ("v" + op)), exp.line_col());
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

                List<Value> list = new ArrayList<>();
                for (Expression exp : index.args) {
                        list.add(parseValueFromExpression(exp, null, scope));
                }

                if (v.type() instanceof SArrayTypeDef && list.size() == 1) {
                        try {
                                Value i = cast(IntTypeDef.get(), list.get(0), index.args.get(0).line_col());
                                return new Ins.TALoad(v, i, index.line_col());
                        } catch (SyntaxException ignore) {
                                // cast failed
                        }
                }
                // not array
                // try to find `get`
                List<SMethodDef> methods = new ArrayList<>();
                findMethodFromTypeWithArguments(index.line_col(), "get", list, scope.type(), v.type(), FIND_MODE_NON_STATIC, methods, true);
                if (methods.isEmpty()) {
                        // invoke dynamic
                        list.add(0, v); // add invoke target into list

                        return new Ins.InvokeDynamic(
                                getInvokeDynamicBootstrapMethod(),
                                "get",
                                list,
                                getTypeWithName("java.lang.Object", index.line_col()),
                                Dynamic.INVOKE_STATIC,
                                index.line_col());
                } else {
                        SMethodDef methodDef = findBestMatch(list, methods, index.line_col());
                        if (methodDef.modifiers().contains(SModifier.PRIVATE)) {
                                // invoke special
                                Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(v, methodDef, index.line_col());
                                invokeSpecial.arguments().addAll(list);
                                return invokeSpecial;
                        } else if (methodDef.declaringType() instanceof SInterfaceDef) {
                                // invoke interface
                                Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(v, methodDef, index.line_col());
                                invokeInterface.arguments().addAll(list);
                                return invokeInterface;
                        } else {
                                // invoke virtual
                                Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(v, methodDef, index.line_col());
                                invokeVirtual.arguments().addAll(list);
                                return invokeVirtual;
                        }
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
                                if (scope.getThis() == null)
                                        throw new SyntaxException("static scope do not have `this` to access", access.line_col());
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
                                        for (Import.ImportDetail detail : im.importDetails) {
                                                if (detail.importAll) {
                                                        if (detail.pkg == null) {
                                                                // import static
                                                                f = findFieldFromTypeDef(
                                                                        access.name,
                                                                        getTypeWithAccess(detail.access, imports),
                                                                        scope.type(),
                                                                        FIND_MODE_STATIC,
                                                                        true);
                                                                if (null != f) {
                                                                        return new Ins.GetStatic(f, access.line_col());
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } else {
                                // value is local variable
                                return new Ins.TLoad(v, scope, access.line_col());
                        }

                        // still not found
                        // check whether it's a non-static field
                        if (scope.getThis() == null) {
                                throw new SyntaxException("cannot find static field " + scope.type().fullName() + "." + access.name, access.line_col());
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
                                        if (scope.getThis() == null)
                                                throw new SyntaxException("static methods don't have `this` variable", access1.line_col());
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
                                        if (!type.isAssignableFrom(scope.type())) {
                                                throw new SyntaxException("`SuperClass` in SuperClass.this should be super class of this class", access1.line_col());
                                        }
                                        SFieldDef f = findFieldFromTypeDef(access.name, type, scope.type(), FIND_MODE_NON_STATIC, false);
                                        if (null != f) {
                                                return new Ins.GetField(f, scope.getThis(), access.line_col());
                                        } else
                                                throw new SyntaxException("cannot find static field `" + access.name + "` in " + type, access.line_col());
                                }
                        }
                        // other conditions
                        // the access.exp should be a Type or value
                        // SomeClass.fieldName or value.fieldName
                        boolean isValue = false;
                        Value v = null;
                        // try to get value
                        SyntaxException ex = null; // the exception would be recorded
                        // and would be thrown if `type` can not be found
                        try {
                                v = parseValueFromExpression(access.exp, null, scope);
                                if (null != v) isValue = true;
                        } catch (SyntaxException e) {
                                ex = e;
                        }
                        if (isValue && isGetFieldAtRuntime(v)) {
                                isValue = false;
                        }

                        // try to find type
                        STypeDef type = null;
                        if (!isValue) { // not value, then try to find type
                                if (access.exp instanceof AST.Access) {
                                        try {
                                                type = getTypeWithAccess((AST.Access) access.exp, imports);
                                        } catch (SyntaxException | AssertionError ignore) {
                                                // type not found or wrong Access format
                                        }
                                }
                        }

                        // handle
                        if (type != null) {
                                // SomeClass.fieldName -- getStatic
                                SFieldDef f = findFieldFromTypeDef(access.name, type, scope.type(), FIND_MODE_STATIC, true);
                                if (null != f) {
                                        return new Ins.GetStatic(f, access.line_col());
                                } else
                                        throw new SyntaxException("cannot find accessible static field `" + access.name + "` in " + type, access.line_col());
                        } else if (v != null) {
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
                        } else throw ex;
                }
        }

        /**
         * invoke {@link Lang#getField(Object, String, Class)}
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
         * {@link Lang#getField(Object, String, Class)}
         */
        private SMethodDef Lang_getField = null;

        /**
         * @return {@link Lang#getField(Object, String, Class)}
         * @throws SyntaxException exception
         */
        private SMethodDef getLang_getField() throws SyntaxException {
                if (Lang_getField == null) {
                        SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("getField")) {
                                        Lang_getField = m;
                                        break;
                                }
                        }
                }
                if (Lang_getField == null) throw new LtBug("lt.lang.Lang.getField(Object,String) should exist");
                return Lang_getField;
        }

        /**
         * get SFieldDef from the given type
         *
         * @param fieldName  field name
         * @param theType    type to search
         * @param type       caller type(used to check accessiblility)
         * @param mode       {@link #FIND_MODE_ANY} {@link #FIND_MODE_NON_STATIC} {@link #FIND_MODE_STATIC}
         * @param checkSuper check the super class/interfaces if it's set to true
         * @return retrieved SFieldDef or null if not found
         */
        private SFieldDef findFieldFromTypeDef(String fieldName, STypeDef theType, STypeDef type, int mode, boolean checkSuper) {
                if (theType instanceof SClassDef) {
                        return findFieldFromClassDef(fieldName, (SClassDef) theType, type, mode, checkSuper);
                } else if (theType instanceof SInterfaceDef) {
                        return findFieldFromInterfaceDef(fieldName, (SInterfaceDef) theType, checkSuper);
                } else throw new LtBug("the type to get field from cannot be " + theType);
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
                                else if (f.modifiers().contains(SModifier.PROTECTED)) {
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
                // undefined
                if (v.type().fullName().equals("lt.lang.Undefined")) {
                        resultVal = v;
                }
                if (requiredType instanceof PrimitiveTypeDef) {
                        // requiredType is primitive
                        if (v.type() instanceof PrimitiveTypeDef) {
                                if (v.type().equals(IntTypeDef.get())
                                        || v.type().equals(ShortTypeDef.get())
                                        || v.type().equals(ByteTypeDef.get())
                                        || v.type().equals(CharTypeDef.get())) {

                                        if (requiredType instanceof LongTypeDef) {
                                                // int to long
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_LONG, lineCol);
                                        } else if (requiredType instanceof FloatTypeDef) {
                                                // int to float
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_FLOAT, lineCol);
                                        } else if (requiredType instanceof DoubleTypeDef) {
                                                // int to double
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_INT_TO_DOUBLE, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                // throw new SyntaxException(fileName, "cannot cast from int/short/byte/char to boolean", line, column);
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else throw new LtBug("unknown primitive requiredType " + requiredType);
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
                                                // throw new SyntaxException(fileName, "cannot cast from long to boolean", line, column);
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else throw new LtBug("unknown primitive requiredType " + requiredType);
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
                                                // throw new SyntaxException(fileName, "cannot cast from float to boolean", line, column);
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else throw new LtBug("unknown primitive requiredType " + requiredType);
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
                                        } else if (requiredType instanceof DoubleTypeDef) {
                                                // double to long
                                                return new Ins.Cast(requiredType, v, Ins.Cast.CAST_DOUBLE_TO_LONG, lineCol);
                                        } else if (requiredType instanceof BoolTypeDef) {
                                                // throw new SyntaxException(fileName, "cannot cast from double to boolean", line, column);
                                                return castObjToPrimitive(
                                                        BoolTypeDef.get(),
                                                        boxPrimitive(
                                                                v,
                                                                lineCol),
                                                        lineCol);
                                        } else throw new LtBug("unknown primitive requiredType " + requiredType);
                                } else if (v.type().equals(BoolTypeDef.get())) {
                                        throw new SyntaxException("cannot cast from boolean to other primitives", lineCol);
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
         * invoke {@link Lang#cast(Object, Class)}<br>
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
                SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", lineCol);
                SMethodDef method = null;
                for (SMethodDef m : Lang.methods()) {
                        if (m.name().equals("cast")) {
                                method = m;
                                break;
                        }
                }
                if (method == null) throw new LtBug("lt.lang.Lang.castToInt(Object,Class) should exist");
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
         * invoke castToX methods defined in lt.lang.Lang
         *
         * @param type    the primitive type
         * @param v       value to cast
         * @param lineCol line and column info
         * @return casted value
         * @throws SyntaxException exception
         */
        private Value castObjToPrimitive(PrimitiveTypeDef type, Value v, LineCol lineCol) throws SyntaxException {
                SClassDef Lang = (SClassDef) getTypeWithName("lt.lang.Lang", LineCol.SYNTHETIC);
                SMethodDef method = null;
                if (type instanceof IntTypeDef) {
                        for (SMethodDef m : Lang.methods()) {
                                if (m.name().equals("castToInt")) {
                                        method = m;
                                        break;
                                }
                        }
                        if (method == null) throw new LtBug("lt.lang.Lang.castToInt(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToLong(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToShort(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToByte(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToFloat(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToDouble(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToBool(Object) should exist");
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
                        if (method == null) throw new LtBug("lt.lang.Lang.castToChar(Object) should exist");
                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(method, lineCol);
                        invokeStatic.arguments().add(v);
                        return invokeStatic;
                } else throw new IllegalArgumentException("unknown primitive type " + type);
        }

        /**
         * check whether the method is overridden by list methods
         *
         * @param method     method
         * @param methodList method list
         * @return true/false
         */
        private boolean whetherTheMethodIsOverriddenByMethodsInTheList(SMethodDef method, List<SMethodDef> methodList) {
                // methodList is null
                // return false
                if (methodList == null) return false;
                // foreach m in methodList
                for (SMethodDef m : methodList) {
                        // equals means overridden
                        if (method.equals(m)) return true;
                        // check m.overRide methods
                        if (whetherTheMethodIsOverriddenByMethodsInTheList(method, m.overRide())) return true;
                }
                // not found, return false
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

                                if (!whetherTheMethodIsOverriddenByMethodsInTheList(m, matchedMethods)) {
                                        matchedMethods.add(m);
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
                        if (argList.size() != 0) throw new SyntaxException("invoking methods in annotation should contain no arguments", lineCol);
                        matchedMethods.addAll(
                                ((SAnnoDef) sTypeDef).annoFields().stream().
                                        filter(f -> f.name().equals(name)).
                                        collect(Collectors.toList())
                        );
                } else throw new LtBug("sTypeDef can only be SClassDef or SInterfaceDef or SAnnoDef");
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
         * @param invocation invocation object
         * @param scope      scope that contains local variables and local methods
         * @return Invoke object or New object(represents invokeXXX or new instruction)
         * @throws SyntaxException exceptions
         */
        private Value parseValueFromInvocation(AST.Invocation invocation, SemanticScope scope) throws SyntaxException {
                assert scope.type() instanceof SClassDef;
                // parse args
                List<Value> argList = new ArrayList<>();
                for (Expression arg : invocation.args) {
                        argList.add(parseValueFromExpression(arg, null, scope));
                }

                List<SMethodDef> methodsToInvoke = new ArrayList<>();
                SemanticScope.MethodRecorder innerMethod = null; // inner method ?
                Value target = null;
                // get method and target
                // get import
                List<Import> imports = fileNameToImport.get(invocation.line_col().fileName);
                AST.Access access = invocation.access;
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
                                        for (Import.ImportDetail detail : im.importDetails) {
                                                if (!methodsToInvoke.isEmpty()) break;
                                                if (detail.importAll && detail.pkg == null) {
                                                        // this import type is import static

                                                        STypeDef type = getTypeWithAccess(detail.access, imports);
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
                                                // type should be assignable from scope.type()
                                                if (!type.isAssignableFrom(scope.type())) {
                                                        throw new SyntaxException("invokespecial type should be assignable from current class", access1.line_col());
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
                                        } else
                                                throw new SyntaxException("`Type` in Type.this.methodName should be Class/Interface name", access1.exp.line_col());
                                } else if (!(access.exp instanceof AST.PackageRef)) {
                                        // assume value is value
                                        // Access(value, methodName)
                                        boolean isValue = true;
                                        Throwable throwableWhenTryValue = null;
                                        try {
                                                target = parseValueFromExpression(access.exp, null, scope);
                                        } catch (SyntaxException | LtBug e) {
                                                // parse from value failed
                                                isValue = false;
                                                throwableWhenTryValue = e;
                                        }

                                        if (target == null) isValue = false;
                                        else if (isGetFieldAtRuntime(target)) {
                                                // get field at runtime
                                                // try to get type from access.exp
                                                if (access.exp instanceof AST.Access) {
                                                        try {
                                                                getTypeWithAccess((AST.Access) access.exp, imports);
                                                                isValue = false; // parse Type
                                                        } catch (SyntaxException | AssertionError ignore) {
                                                                // not type or wrong format
                                                        }
                                                }
                                        }

                                        if (isValue) {
                                                if (target.type() instanceof SClassDef || target.type() instanceof SInterfaceDef) {
                                                        findMethodFromTypeWithArguments(
                                                                access.line_col(),
                                                                access.name,
                                                                argList,
                                                                target.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true);
                                                } else if (target.type() instanceof SAnnoDef) {
                                                        if (argList.size() != 0)
                                                                throw new SyntaxException("Annotation don't have methods with non zero parameters", access.exp.line_col());
                                                        findMethodFromTypeWithArguments(
                                                                access.exp.line_col(),
                                                                access.name,
                                                                argList,
                                                                target.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true
                                                        ); // this method will find method from annotation
                                                        if (methodsToInvoke.isEmpty()) {
                                                                throw new SyntaxException("cannot find " + access.name + " in " + target.type(), access.exp.line_col());
                                                        }
                                                } else if (target.type() instanceof PrimitiveTypeDef) {
                                                        // box primitive then invoke
                                                        target = boxPrimitive(target, access.exp.line_col());
                                                        findMethodFromTypeWithArguments(
                                                                access.line_col(),
                                                                access.name,
                                                                argList,
                                                                target.type(),
                                                                target.type(),
                                                                FIND_MODE_NON_STATIC,
                                                                methodsToInvoke,
                                                                true);
                                                } else throw new LtBug("type should not be " + target.type());
                                        } else if (access.exp instanceof AST.Access) {
                                                // is type
                                                STypeDef type = getTypeWithAccess((AST.Access) access.exp, imports);
                                                findMethodFromTypeWithArguments(
                                                        access.line_col(),
                                                        access.name,
                                                        argList,
                                                        scope.type(),
                                                        type,
                                                        FIND_MODE_STATIC,
                                                        methodsToInvoke,
                                                        true);
                                                if (methodsToInvoke.isEmpty())
                                                        throw new SyntaxException("cannot find static method " + invocation, invocation.line_col());
                                        } else {
                                                if (throwableWhenTryValue == null) {
                                                        throw new SyntaxException(
                                                                "method access structure should only be " +
                                                                        "(type,methodName)" +
                                                                        "/((type or null,\"this\"),methodName)" +
                                                                        "/(null,methodName)/(value,methodName) " +
                                                                        "but got " + invocation.access, access.exp.line_col());
                                                } else throw new SyntaxException(throwableWhenTryValue.getMessage(), invocation.line_col());
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
                        } catch (SyntaxException | AssertionError | ClassCastException ignore) {
                                // not found or not type format
                        }
                        if (type instanceof SClassDef) {
                                // only SClassDef have constructors
                                out:
                                for (SConstructorDef con : ((SClassDef) type).constructors()) {
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
                                                Ins.New aNew = new Ins.New(con, invocation.line_col());
                                                argList = castArgsForMethodInvoke(argList, con.getParameters(), invocation.line_col());
                                                aNew.args().addAll(argList);
                                                return aNew;
                                        }
                                }
                        }
                }

                if (target == null) target = scope.getThis();

                if (methodsToInvoke.isEmpty() && innerMethod == null) {
                        // invoke dynamic
                        if (target == null) {
                                if (scope.getThis() == null) {
                                        throw new SyntaxException("invoke dynamic only perform on instances", invocation.line_col());
                                } else
                                        argList.add(0, scope.getThis());
                        } else {
                                argList.add(0, target);
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
                                                } else if (normalIsBetter)
                                                        throw new SyntaxException("cannot choose between " + methodToInvoke + " and " + innerMethod.method + " with args " + argList, invocation.line_col());
                                        } else if (innerT.isAssignableFrom(normalT)) {
                                                if (normalIsBetter == null) {
                                                        normalIsBetter = true;
                                                } else if (!normalIsBetter)
                                                        throw new SyntaxException("cannot choose between " + methodToInvoke + " and " + innerMethod.method + " with args " + argList, invocation.line_col());
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
                                return invoke;
                        } else {
                                argList = castArgsForMethodInvoke(argList, methodToInvoke.getParameters(), invocation.line_col());

                                if (methodToInvoke.modifiers().contains(SModifier.STATIC)) {
                                        // invoke static
                                        Ins.InvokeStatic invokeStatic = new Ins.InvokeStatic(methodToInvoke, invocation.line_col());
                                        invokeStatic.arguments().addAll(argList);
                                        return invokeStatic;
                                } else if (methodToInvoke.declaringType() instanceof SInterfaceDef || methodToInvoke.declaringType() instanceof SAnnoDef) {
                                        // invoke interface
                                        if (target == null) throw new SyntaxException("invoke interface should have an invoke target", invocation.line_col());
                                        Ins.InvokeInterface invokeInterface = new Ins.InvokeInterface(target, methodToInvoke, invocation.line_col());
                                        invokeInterface.arguments().addAll(argList);
                                        return invokeInterface;
                                } else if (doInvokeSpecial || methodToInvoke.modifiers().contains(SModifier.PRIVATE)) {
                                        // invoke special
                                        if (target == null) throw new SyntaxException("invoke special should have an invoke target", invocation.line_col());
                                        Ins.InvokeSpecial invokeSpecial = new Ins.InvokeSpecial(target, methodToInvoke, invocation.line_col());
                                        invokeSpecial.arguments().addAll(argList);
                                        return invokeSpecial;
                                } else {
                                        // invoke virtual
                                        if (target == null) throw new SyntaxException("invoke virtual should have an invoke target", invocation.line_col());
                                        Ins.InvokeVirtual invokeVirtual = new Ins.InvokeVirtual(target, methodToInvoke, invocation.line_col());
                                        invokeVirtual.arguments().addAll(argList);
                                        return invokeVirtual;
                                }
                        }
                }
        }

        /**
         * check whether the `target` is <code>invokeStatic lt.lang.Lang.getField</code>
         *
         * @param target target
         * @return true or false
         */
        private boolean isGetFieldAtRuntime(Value target) {
                if (target instanceof Ins.InvokeStatic) {
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) target;
                        if (invokeStatic.invokable() instanceof SMethodDef) {
                                SMethodDef m = (SMethodDef) invokeStatic.invokable();
                                if (m.name().equals("getField") && m.declaringType().fullName().equals("lt.lang.Lang")) {
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
                                                throw new SyntaxException("cannot choose between " + method + " and " + methodCurrent + " with args " + argList, lineCol);
                                        }
                                } else {
                                        // not assignable
                                        if (swap == SWAP_NONE) {
                                                swap = SWAP_NO_SWAP;
                                        } else if (swap == SWAP_SWAP) {
                                                throw new SyntaxException("cannot choose between " + method + " and " + methodCurrent + " with args " + argList, lineCol);
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
                                else throw new SyntaxException("cannot unescape \\" + anotherChar, lineCol);
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
         * @return Value object<br>
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
         * <li>ClassValue</li>
         * <li>SAnno</li>
         * <li>SArrayValue</li>
         * </ul>
         * @throws SyntaxException exception
         */
        private Value parseValueFromObject(Object o) throws SyntaxException {
                // primitives
                if (o instanceof Integer) {
                        return new IntValue(((Integer) o).intValue());
                } else if (o instanceof Long) {
                        return new LongValue(((Long) o).longValue());
                } else if (o instanceof Character) {
                        return new CharValue(((Character) o).charValue());
                } else if (o instanceof Short) {
                        return new ShortValue(((Short) o).shortValue());
                } else if (o instanceof Byte) {
                        return new ByteValue(((Byte) o).byteValue());
                } else if (o instanceof Boolean) {
                        return new BoolValue(((Boolean) o).booleanValue());
                } else if (o instanceof Float) {
                        return new FloatValue(((Float) o).floatValue());
                } else if (o instanceof Double) {
                        return new DoubleValue(((Double) o).doubleValue());
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
                        ClassValue c = new ClassValue();
                        c.setClassName(((Class) o).getName());
                        c.setType(getTypeWithName("java.lang.Class", LineCol.SYNTHETIC));
                        return c;
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
                } else throw new IllegalArgumentException("cannot parse " + o + " into Value");
        }

        private Set<STypeDef> typesAlreadyDoneOverrideCheck = new HashSet<>();

        /**
         * check whether the class already overrides all abstract methods in super class/interfaces
         *
         * @param sTypeDef type to be checked
         * @throws SyntaxException exception
         */
        private void checkOverride(STypeDef sTypeDef) throws SyntaxException {
                if (typesAlreadyDoneOverrideCheck.contains(sTypeDef)) return;
                typesAlreadyDoneOverrideCheck.add(sTypeDef);
                // queue for BFS
                Queue<SInterfaceDef> q = new ArrayDeque<>();
                // check method override
                if (sTypeDef instanceof SClassDef) {
                        SClassDef c = ((SClassDef) sTypeDef);
                        for (SMethodDef method : c.methods()) {
                                SClassDef parent = c.parent();
                                SMethodDef overriddenMethod = null;
                                // try to find method from parent class
                                while (parent != null) {
                                        overriddenMethod = findMethodWithSameSignature(method, parent.methods());
                                        if (overriddenMethod == null) {
                                                // check parent interfaces
                                                q.clear();
                                                q.addAll(parent.superInterfaces());
                                                while (!q.isEmpty()) {
                                                        SInterfaceDef i = q.remove();
                                                        overriddenMethod = findMethodWithSameSignature(method, i.methods());
                                                        if (overriddenMethod != null) break;
                                                        q.addAll(i.superInterfaces());
                                                }
                                        } else break;
                                        parent = parent.parent();
                                }
                                if (overriddenMethod == null) {
                                        Queue<SInterfaceDef> interfaceDefs = new ArrayDeque<>();
                                        interfaceDefs.addAll(c.superInterfaces());
                                        while (!interfaceDefs.isEmpty()) {
                                                SInterfaceDef i = interfaceDefs.remove();
                                                overriddenMethod = findMethodWithSameSignature(method, i.methods());
                                                if (overriddenMethod != null) {
                                                        overriddenMethod.overridden().add(method);
                                                        method.overRide().add(overriddenMethod);
                                                }
                                                interfaceDefs.addAll(i.superInterfaces());
                                        }
                                }
                        }
                } else if (sTypeDef instanceof SInterfaceDef) {
                        SInterfaceDef i = ((SInterfaceDef) sTypeDef);

                        for (SMethodDef method : i.methods()) {
                                q.clear();
                                q.addAll(i.superInterfaces());
                                while (!q.isEmpty()) {
                                        SInterfaceDef in = q.remove();
                                        SMethodDef m = findMethodWithSameSignature(method, in.methods());
                                        if (m != null) {
                                                method.overRide().add(m);
                                                m.overridden().add(method);
                                        }
                                        q.addAll(in.superInterfaces());
                                }
                        }
                } else {
                        throw new IllegalArgumentException("wrong STypeDefType " + sTypeDef.getClass());
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
                                throw new SyntaxException("circular inheritance " + recorder, LineCol.SYNTHETIC);
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
         * @param method       the method to be checked
         * @param superMethods super methods
         * @return found method or null
         * @throws SyntaxException exception
         */
        private SMethodDef findMethodWithSameSignature(SMethodDef method, List<SMethodDef> superMethods) throws SyntaxException {
                outer:
                for (SMethodDef m : superMethods) {
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
                                                throw new SyntaxException(method + " cannot override " + m, method.line_col());
                                        }

                                        if (!m.getReturnType().isAssignableFrom(method.getReturnType())) {
                                                throw new SyntaxException(m + " return type should be assignable from " + method + " 's", method.line_col());
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
        private void parseAnnos(Set<AST.Anno> annos, SAnnotationPresentable annotationPresentable, List<Import> imports, ElementType type) throws SyntaxException {
                for (AST.Anno anno : annos) {
                        STypeDef annoType = getTypeWithAccess(anno.anno, imports);
                        if (!((SAnnoDef) annoType).canPresentOn(type)) {
                                throw new SyntaxException("annotation " + annoType + " cannot present on " + type, anno.line_col());
                        }
                        SAnno s = new SAnno();
                        s.setAnnoDef((SAnnoDef) annoType);
                        s.setPresent(annotationPresentable);
                        annotationPresentable.annos().add(s);

                        annotationRecorder.put(s, anno);
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
                                        case "val":
                                                param.setCanChange(false);
                                                break;
                                        case "pub":
                                        case "pri":
                                        case "pro":
                                        case "pkg":
                                                if (!allowAccessModifier)
                                                        throw new SyntaxException("access modifiers for parameters are only allowed on class constructing parameters",
                                                                m.line_col());
                                                break;
                                        default:
                                                throw new UnexpectedTokenException("valid modifier for parameters (val)", m.toString(), m.line_col());
                                }
                        }

                        parseAnnos(v.getAnnos(), param, imports, ElementType.PARAMETER);

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
                        if (m.modifier.equals("pub") || m.modifier.equals("pri") || m.modifier.equals("pro") || m.modifier.equals("pkg")) {
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
                                case "pub":
                                        fieldDef.modifiers().add(SModifier.PUBLIC);
                                        break;
                                case "pri":
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        fieldDef.modifiers().add(SModifier.PRIVATE);
                                        break;
                                case "pro":
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        fieldDef.modifiers().add(SModifier.PROTECTED);
                                        break;
                                case "pkg": // no need to assign modifier
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        break;
                                case "val":
                                        fieldDef.modifiers().add(SModifier.FINAL);
                                        break;
                                default:
                                        throw new UnexpectedTokenException("valid modifier for fields (class:(pub|pri|pro|pkg|val)|interface:(pub|val))", m.toString(), m.line_col());
                        }
                }
                if (mode == PARSING_INTERFACE && !fieldDef.modifiers().contains(SModifier.FINAL)) {
                        fieldDef.modifiers().add(SModifier.FINAL);
                }
                if (isStatic && mode == PARSING_CLASS) {
                        fieldDef.modifiers().add(SModifier.STATIC);
                }
                // annos
                parseAnnos(v.getAnnos(), fieldDef, imports, ElementType.FIELD);

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
                                        throw new DuplicateVariableNameException(v.getName(), v.line_col());
                                }
                        }
                }

                // add into class/interface
                if (mode == PARSING_CLASS) {
                        ((SClassDef) type).fields().add(fieldDef);
                } else if (mode == PARSING_INTERFACE) {
                        ((SInterfaceDef) type).fields().add(fieldDef);
                } // no else
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
                        if (mod.modifier.equals("pub") || mod.modifier.equals("pri") || mod.modifier.equals("pro") || mod.modifier.equals("pkg")) {
                                hasAccessModifier = true;
                        }
                }
                if (!hasAccessModifier) {
                        methodDef.modifiers().add(SModifier.PUBLIC); // default modifier is public
                }
                for (Modifier mod : m.modifiers) {
                        switch (mod.modifier) {
                                case "pub":
                                        methodDef.modifiers().add(SModifier.PUBLIC);
                                        break;
                                case "pri":
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        methodDef.modifiers().add(SModifier.PRIVATE);
                                        break;
                                case "pro":
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        methodDef.modifiers().add(SModifier.PROTECTED);
                                        break;
                                case "pkg": // no need to assign modifier
                                        if (mode == PARSING_INTERFACE)
                                                throw new UnexpectedTokenException("valid modifier for interface fields (pub|val)", m.toString(), m.line_col());
                                        break;
                                case "val":
                                        methodDef.modifiers().add(SModifier.FINAL);
                                        break;
                                case "abs":
                                        methodDef.modifiers().add(SModifier.ABSTRACT);
                                        // check method body. it should no have body
                                        if (!m.body.isEmpty()) throw new SyntaxException("abstract methods cannot have body", m.line_col());
                                        break;
                                default:
                                        throw new UnexpectedTokenException("valid modifier for fields (class:(pub|pri|pro|pkg|val)|interface:(pub|val))", m.toString(), m.line_col());
                        }
                }
                if (isStatic) {
                        methodDef.modifiers().add(SModifier.STATIC);
                }
                if (mode == PARSING_INTERFACE && !methodDef.modifiers().contains(SModifier.ABSTRACT) && m.body.isEmpty()) {
                        methodDef.modifiers().add(SModifier.ABSTRACT);
                }

                // annos
                parseAnnos(m.annos, methodDef, imports, ElementType.METHOD);

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
                                        if (!passCheck)
                                                throw new SyntaxException("method signature check failed on " + methodDef, m.line_col());
                                }
                        }
                }

                if (null != lastMethod) {
                        Ins.InvokeWithTarget invoke;
                        if (lastMethod.modifiers().contains(SModifier.PRIVATE)) {
                                invoke = new Ins.InvokeSpecial(new Ins.This(methodDef.declaringType()), lastMethod, LineCol.SYNTHETIC);
                        } else {
                                invoke = new Ins.InvokeVirtual(new Ins.This(methodDef.declaringType()), lastMethod, LineCol.SYNTHETIC);
                        }
                        for (int ii = 0; ii < methodDef.getParameters().size(); ++ii) {
                                invoke.arguments().add(methodDef.getParameters().get(ii));
                        }
                        List<SParameter> lastParams = lastMethod.getParameters();
                        invoke.arguments().add(parseValueFromExpression(m.params.get(i).getInit(), lastParams.get(lastParams.size() - 1).type(), null));
                        methodDef.statements().add(invoke);
                }

                // add into class/interface
                if (mode == PARSING_CLASS) {
                        ((SClassDef) type).methods().add(methodDef);
                } else if (mode == PARSING_INTERFACE) {
                        ((SInterfaceDef) type).methods().add(methodDef);
                } // no else
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
        private void select_import_class_interface(Statement stmt, List<Import> imports, List<ClassDef> classDefs, List<InterfaceDef> interfaceDefs) throws UnexpectedTokenException {
                if (stmt instanceof Import) {
                        imports.add((Import) stmt);
                } else if (stmt instanceof ClassDef) {
                        classDefs.add((ClassDef) stmt);
                } else if (stmt instanceof InterfaceDef) {
                        interfaceDefs.add((InterfaceDef) stmt);
                } else {
                        throw new UnexpectedTokenException("class/interface definition or import", stmt.toString(), stmt.line_col());
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
                                                SClassDef c = new SClassDef(LineCol.SYNTHETIC);
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
                                        } else if (typeDef instanceof SAnnoDef) {
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
                                throw new SyntaxException("undefined class " + clsName, lineCol);
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
                                        } catch (Exception e) {
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
                        for (Import.ImportDetail detail : i.importDetails) {
                                if (!detail.importAll) {
                                        if (detail.access.name.equals(name)) {
                                                // use this name
                                                return getClassNameFromAccess(detail.access);
                                        }
                                }
                        }
                }
                // try to find `import all`
                for (Import i : imports) {
                        for (Import.ImportDetail detail : i.importDetails) {
                                if (detail.importAll && detail.pkg != null) {
                                        String possibleClassName = detail.pkg.pkg.replace("::", ".") + "." + name;
                                        if (typeExists(possibleClassName)) return possibleClassName;
                                }
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

                if (null == className || !typeExists(className))
                        throw new SyntaxException("type " + name + " not defined", access.line_col());
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
                        throw new SyntaxException("duplicate type names " + type.fullName(), lineCol);
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
                try {
                        return Class.forName(name);
                } catch (ClassNotFoundException e) {
                        return classLoader.loadClass(name);
                }
        }
}
