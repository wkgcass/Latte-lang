package lt.compiler.cases;

import lt.compiler.*;
import lt.compiler.semantic.*;
import lt.compiler.semantic.builtin.*;
import lt.compiler.semantic.builtin.ClassValue;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * semantic
 */
public class TestSemantic {
        private Set<STypeDef> parse(Map<String, String> fileMap) throws IOException, SyntaxException {
                Map<String, List<Statement>> map = new HashMap<>();
                for (String fileName : fileMap.keySet()) {
                        lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test", new StringReader(fileMap.get(fileName)), 4);
                        Parser syntacticProcessor = new Parser(lexicalProcessor.parse());
                        map.put(fileName, syntacticProcessor.parse());
                }
                SemanticProcessor semanticProcessor = new SemanticProcessor(map);
                return semanticProcessor.parse();
        }

        @Test
        public void testClass() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals("test.A", classDef.fullName());
        }

        @Test
        public void testClassExtends() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> java::util::_\n" +
                        "class A:ArrayList");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals("test.A", classDef.fullName());
                assertTrue(classDef.modifiers().contains(SModifier.PUBLIC));
                assertEquals("java.util.ArrayList", classDef.parent().fullName());
        }

        @Test
        public void testClassImplements() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> java::util::_\n" +
                        "abs class A:List");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals("test.A", classDef.fullName());
                assertEquals(1, classDef.superInterfaces().size());
                assertTrue(classDef.modifiers().contains(SModifier.PUBLIC) && classDef.modifiers().contains(SModifier.ABSTRACT));
                assertEquals("java.util.List", classDef.superInterfaces().get(0).fullName());
        }

        @Test
        public void testArrayType() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    arr:[]String");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());
                SClassDef classDef = (SClassDef) set.iterator().next();

                assertEquals("test.A", classDef.fullName());
                assertEquals(1, classDef.fields().size());

                SFieldDef f = classDef.fields().get(0);
                assertEquals("arr", f.name());
                assertEquals("[Ljava.lang.String;", f.type().fullName());
        }

        @Test
        public void testArrayType2Dimension() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    arr:[][]String");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());
                SClassDef classDef = (SClassDef) set.iterator().next();

                assertEquals("test.A", classDef.fullName());
                assertEquals(1, classDef.fields().size());

                SFieldDef f = classDef.fields().get(0);
                assertEquals("arr", f.name());
                assertEquals("[[Ljava.lang.String;", f.type().fullName());
        }

        @Test
        public void testClassNotFound() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A:B");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testCircularInheritance_2_Classes() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A:B\n" +
                        "class B:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testCircularInheritance_3_Classes() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A:B\n" +
                        "class B:C\n" +
                        "class C:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testCircularInheritance_2_Interfaces() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "interface A:B\n" +
                        "interface B:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testCircularInheritance_3_Interfaces() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "interface A:B\n" +
                        "interface B:C\n" +
                        "interface C:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testNotOverridden() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A:java::util::List\n");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testNotOverridden2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "interface A\n" +
                        "    method()=...\n" +
                        "class B:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testNotOverriddenFromClass() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "abs class A\n" +
                        "    abs method():Unit\n" +
                        "class B:A");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testImport() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#>  java::awt::_\n" +
                        "    java::util::List\n" +
                        "    java::util::Collections._\n" +
                        "class A(ls:List)\n" +
                        "    emptyList()\n" +
                        "    EMPTY_LIST");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SParameter p = classDef.constructors().get(0).getParameters().get(0);
                assertEquals("java.util.List", p.type().fullName());

                Instruction i1 = classDef.constructors().get(0).statements().get(1 + 1);
                Instruction i2 = classDef.constructors().get(0).statements().get(2 + 1);

                assertTrue(i1 instanceof Ins.InvokeStatic);
                assertTrue(i2 instanceof Ins.GetStatic);

                assertEquals("emptyList", ((SMethodDef) ((Ins.InvokeStatic) i1).invokable()).name());
                assertEquals("EMPTY_LIST", ((Ins.GetStatic) i2).field().name());

                assertEquals("java.util.Collections", ((Ins.InvokeStatic) i1).invokable().declaringType().fullName());
                assertEquals("java.util.Collections", ((Ins.GetStatic) i2).field().declaringType().fullName());
        }

        @Test
        public void testAnno() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> lt::compiler::_\n" +
                        "@MyAnno(str='',i=1)\n" +
                        "class A");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals("test.A", classDef.fullName());
                assertTrue(classDef.modifiers().contains(SModifier.PUBLIC));
                assertEquals(1, classDef.annos().size());

                SAnno anno = classDef.annos().get(0);
                assertEquals(2, anno.type().annoFields().size());
                anno.type().annoFields().stream().filter(f -> f.name().equals("i")).forEach(f -> assertEquals(new IntValue(100), f.defaultValue()));
        }

        @Test
        public void testAnnoAll() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> lt::compiler::_\n" +
                        "@AnnotationTest(str='\\t\\n')\n" +
                        "class A");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.annos().size());

                SAnno anno = classDef.annos().get(0);
                SAnnoDef annoDef = anno.type();
                assertEquals(14, annoDef.annoFields().size());

                for (SAnnoField f : anno.values().keySet()) {
                        Value v = anno.values().get(f);
                        if (f.name().equals("str")) {
                                assertEquals(new StringConstantValue("\t\n"), v);
                        }
                }

                for (SAnnoField f : annoDef.annoFields()) {
                        if (f.name().equals("clsArr")) {
                                Value v = f.defaultValue();
                                assertTrue(v instanceof SArrayValue);
                                SArrayValue arr = (SArrayValue) v;
                                assertEquals(1, arr.dimension());
                                assertEquals(2, arr.length());
                                assertEquals("java.lang.Class", arr.type().type().fullName());
                                Value[] values = arr.values();
                                assertTrue(values[0] instanceof ClassValue);
                                assertTrue(values[1] instanceof ClassValue);

                                assertEquals("java.lang.Class", ((ClassValue) values[0]).className());
                                assertEquals("java.lang.String", ((ClassValue) values[1]).className());
                        } else if (f.name().equals("strArr")) {
                                Value v = f.defaultValue();
                                assertTrue(v instanceof SArrayValue);
                                SArrayValue arr = (SArrayValue) v;
                                assertEquals(1, arr.dimension());
                                assertEquals(2, arr.length());
                                assertEquals("java.lang.String", arr.type().type().fullName());
                                Value[] values = arr.values();
                                assertTrue(values[0] instanceof StringConstantValue);
                                assertTrue(values[1] instanceof StringConstantValue);

                                assertEquals("a", new String(((StringConstantValue) values[0]).getByte()));
                                assertEquals("b", new String(((StringConstantValue) values[1]).getByte()));
                        } else if (f.name().equals("e")) {
                                Value v = f.defaultValue();
                                assertTrue(v instanceof EnumValue);
                                assertEquals("lt.compiler.semantic.SModifier", v.type().fullName());
                                assertEquals("PUBLIC", ((EnumValue) v).enumStr());
                        } else if (f.name().equals("cls")) {
                                Value v = f.defaultValue();
                                assertTrue(v instanceof ClassValue);
                                assertEquals("java.lang.Class", v.type().fullName());
                                assertEquals("java.lang.String", ((ClassValue) v).className());
                        } else if (f.name().equals("str")) {
                                Value v = f.defaultValue();
                                assertEquals(new StringConstantValue("str"), v);
                        } else if (f.name().equals("i")) {
                                Value v = f.defaultValue();
                                assertEquals(new IntValue(100), v);
                        } else if (f.name().equals("l")) {
                                Value v = f.defaultValue();
                                assertEquals(new LongValue(100), v);
                        } else if (f.name().equals("s")) {
                                Value v = f.defaultValue();
                                assertEquals(new ShortValue((short) 100), v);
                        } else if (f.name().equals("b")) {
                                Value v = f.defaultValue();
                                assertEquals(new ByteValue((byte) 100), v);
                        } else if (f.name().equals("bo")) {
                                Value v = f.defaultValue();
                                assertEquals(new BoolValue(true), v);
                        } else if (f.name().equals("c")) {
                                Value v = f.defaultValue();
                                assertEquals(new CharValue('a'), v);
                        } else if (f.name().equals("f")) {
                                Value v = f.defaultValue();
                                assertEquals(new FloatValue(100), v);
                        } else if (f.name().equals("d")) {
                                Value v = f.defaultValue();
                                assertEquals(new DoubleValue(100), v);
                        } else if (f.name().equals("anno")) {
                                Value v = f.defaultValue();
                                assertTrue(v instanceof SAnno);
                                SAnno a = (SAnno) v;
                                for (SAnnoField ff : a.values().keySet()) {
                                        Value ffv = a.values().get(ff);
                                        if (ff.name().equals("str")) {
                                                assertEquals(new StringConstantValue("a"), ffv);
                                        } else if (ff.name().equals("i")) {
                                                assertEquals(new IntValue(100), ffv);
                                        }
                                }
                        }
                }
        }

        @Test
        public void testConstructorParamInitValue() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a,b:int=1,c:int=2)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(3, classDef.constructors().size());

                SConstructorDef lastConstructor = null;
                for (SConstructorDef con : classDef.constructors()) {
                        if (null == lastConstructor) assertEquals(3, con.getParameters().size());
                        else assertEquals(lastConstructor.getParameters().size() - 1, con.getParameters().size());

                        if (null != lastConstructor) {
                                Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) con.statements().get(0);
                                assertEquals(lastConstructor, invokeSpecial.invokable()); // invoke last constructor
                                assertEquals(invokeSpecial.invokable().getParameters().size(), invokeSpecial.arguments().size()); // param size
                                assertEquals("a", ((SParameter) invokeSpecial.arguments().get(0)).name());
                                if (invokeSpecial.arguments().size() == 2) {
                                        assertEquals(new IntValue(1), invokeSpecial.arguments().get(1));
                                }
                                if (invokeSpecial.arguments().size() == 3) {
                                        assertEquals("b", ((SParameter) invokeSpecial.arguments().get(1)).name());
                                        assertEquals(new IntValue(2), invokeSpecial.arguments().get(2));
                                }
                        }

                        lastConstructor = con;
                }
        }

        @Test
        public void testMethodParamInitValue() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method(a,b:int=1,c:double=2)=pass");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(3, classDef.methods().size());

                SMethodDef lastMethod = null;
                for (SMethodDef m : classDef.methods()) {
                        if (null == lastMethod) assertEquals(3, m.getParameters().size());
                        else assertEquals(lastMethod.getParameters().size() - 1, m.getParameters().size());

                        if (null != lastMethod) {
                                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) m.statements().get(0);
                                assertEquals(lastMethod, invokeVirtual.invokable()); // invoke last method
                                assertEquals(invokeVirtual.invokable().getParameters().size(), invokeVirtual.arguments().size()); // param size
                                assertEquals("a", ((SParameter) invokeVirtual.arguments().get(0)).name());

                                assertTrue(invokeVirtual.arguments().size() == 2 || invokeVirtual.arguments().size() == 3);
                                if (invokeVirtual.arguments().size() == 2) {
                                        assertEquals(new IntValue(1), invokeVirtual.arguments().get(1));
                                }
                                if (invokeVirtual.arguments().size() == 3) {
                                        assertEquals("b", ((SParameter) invokeVirtual.arguments().get(1)).name());
                                        assertEquals(new DoubleValue(2), invokeVirtual.arguments().get(2));
                                }
                        }

                        lastMethod = m;
                }
        }

        @Test
        public void testMethodInvoke_$null_methodName$() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()=pass\n" +
                        "    method() ; invoke"); // invoke
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.methods().size());

                assertEquals("method", classDef.methods().get(0).name());
                assertEquals(0, classDef.methods().get(0).getParameters().size());

                SMethodDef methodDef = classDef.methods().get(0);

                assertEquals(2, classDef.constructors().get(0).statements().size());
                assertTrue(classDef.constructors().get(0).statements().get(1) instanceof Ins.InvokeVirtual);
                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) classDef.constructors().get(0).statements().get(1);

                assertTrue(methodDef == invokeVirtual.invokable());
        }

        @Test
        public void testMethodInvoke_$this_methodName$() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()=pass\n" +
                        "    this.method() ; invoke this"); // invoke this
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.methods().size());

                assertEquals("method", classDef.methods().get(0).name());
                assertEquals(0, classDef.methods().get(0).getParameters().size());

                SMethodDef methodDef = classDef.methods().get(0);

                assertEquals(2, classDef.constructors().get(0).statements().size());
                assertTrue(classDef.constructors().get(0).statements().get(1) instanceof Ins.InvokeVirtual);
                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) classDef.constructors().get(0).statements().get(1);

                assertTrue(methodDef == invokeVirtual.invokable());
        }

        @Test
        public void testMethodInvoke_$superClass_$this_methodName$$() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    Object.this.hashCode() ; invoke special"); // invoke special
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                assertEquals(2, classDef.constructors().get(0).statements().size());
                assertTrue(classDef.constructors().get(0).statements().get(1) instanceof Ins.InvokeSpecial);
                Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) classDef.constructors().get(0).statements().get(1);
                assertEquals("hashCode", ((SMethodDef) invokeSpecial.invokable()).name());
        }

        @Test
        public void testMethodInvoke_$null_methodName$but_method_is_static() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        method()=...\n" +
                        "    method() ; invoke static"); // invoke static
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.methods().size());

                assertEquals("method", classDef.methods().get(0).name());
                assertEquals(0, classDef.methods().get(0).getParameters().size());

                SMethodDef methodDef = classDef.methods().get(0);
                assertTrue(methodDef.modifiers().contains(SModifier.STATIC));

                assertEquals(2, classDef.constructors().get(0).statements().size());
                assertTrue(classDef.constructors().get(0).statements().get(1) instanceof Ins.InvokeStatic);
                Ins.InvokeStatic invokeVirtual = (Ins.InvokeStatic) classDef.constructors().get(0).statements().get(1);

                assertTrue(methodDef == invokeVirtual.invokable());
        }

        @Test
        public void testMethodInvoke_$Type_methodName$() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        method()=...\n" +
                        "    A.method() ; invoke static"); // invoke static
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.methods().size());

                assertEquals("method", classDef.methods().get(0).name());
                assertEquals(0, classDef.methods().get(0).getParameters().size());

                SMethodDef methodDef = classDef.methods().get(0);
                assertTrue(methodDef.modifiers().contains(SModifier.STATIC));

                assertEquals(2, classDef.constructors().get(0).statements().size());
                assertTrue(classDef.constructors().get(0).statements().get(1) instanceof Ins.InvokeStatic);
                Ins.InvokeStatic invokeVirtual = (Ins.InvokeStatic) classDef.constructors().get(0).statements().get(1);

                assertTrue(methodDef == invokeVirtual.invokable());
        }

        @Test
        public void testMethodInvoke_$value_methodName$() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    Integer(1).toString()");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.InvokeVirtual);
                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) ins;
                SInvokable sInvokable = invokeVirtual.invokable();
                assertTrue(sInvokable instanceof SMethodDef);
                SMethodDef sMethodDef = (SMethodDef) sInvokable;
                assertEquals("toString", sMethodDef.name());
        }

        private static Value parseValueFromExpression(SemanticProcessor processor, Expression exp, STypeDef requiredType, SemanticScope scope) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
                Method parseValueFromExpression = processor.getClass().getDeclaredMethod("parseValueFromExpression", Expression.class, STypeDef.class, SemanticScope.class);
                parseValueFromExpression.setAccessible(true);
                return (Value) parseValueFromExpression.invoke(processor, exp, requiredType, scope);
        }

        private static STypeDef getTypeWithName(SemanticProcessor processor, String className, LineCol lineCol) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
                Method getTypeWithName = processor.getClass().getDeclaredMethod("getTypeWithName", String.class, LineCol.class);
                getTypeWithName.setAccessible(true);
                return (STypeDef) getTypeWithName.invoke(processor, className, lineCol);
        }

        @Test
        public void testVariableWithNoLimit() throws Exception {
                SemanticProcessor processor = new SemanticProcessor(new HashMap<>());
                processor.parse();

                assertEquals(new IntValue(1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), null, null));
                assertEquals(new DoubleValue(1.2), parseValueFromExpression(processor, new NumberLiteral("1.2", LineCol.SYNTHETIC), null, null));
                assertEquals(new CharValue('c'), parseValueFromExpression(processor, new StringLiteral("'c'", LineCol.SYNTHETIC), null, null));
                assertEquals(new StringConstantValue("s"), parseValueFromExpression(processor, new StringLiteral("\"s\"", LineCol.SYNTHETIC), null, null));
                assertEquals(new StringConstantValue("str"), parseValueFromExpression(processor, new StringLiteral("\"str\"", LineCol.SYNTHETIC), null, null));
                assertEquals(new StringConstantValue("str"), parseValueFromExpression(processor, new StringLiteral("'str'", LineCol.SYNTHETIC), null, null));
                assertEquals(new BoolValue(true), parseValueFromExpression(processor, new BoolLiteral("true", LineCol.SYNTHETIC), null, null));
        }

        @Test
        public void testPrimitiveVariableWithLimit() throws Exception {
                SemanticProcessor processor = new SemanticProcessor(new HashMap<>());
                processor.parse();

                assertEquals(new CharValue('c'), parseValueFromExpression(processor, new StringLiteral("\"c\"", LineCol.SYNTHETIC), CharTypeDef.get(), null));
                assertEquals(new IntValue(1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), IntTypeDef.get(), null));
                assertEquals(new ShortValue((short) 1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), ShortTypeDef.get(), null));
                assertEquals(new ByteValue((byte) 1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), ByteTypeDef.get(), null));
                assertEquals(new LongValue(1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), LongTypeDef.get(), null));
                assertEquals(new FloatValue(1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), FloatTypeDef.get(), null));
                assertEquals(new DoubleValue(1), parseValueFromExpression(processor, new NumberLiteral("1", LineCol.SYNTHETIC), DoubleTypeDef.get(), null));

                assertEquals(new FloatValue(1.2f), parseValueFromExpression(processor, new NumberLiteral("1.2", LineCol.SYNTHETIC), FloatTypeDef.get(), null));
                assertEquals(new DoubleValue(1.2), parseValueFromExpression(processor, new NumberLiteral("1.2", LineCol.SYNTHETIC), DoubleTypeDef.get(), null));
        }

        @Test
        public void testCharAndString() throws Exception {
                SemanticProcessor processor = new SemanticProcessor(new HashMap<>());
                processor.parse();

                STypeDef charSequenceType = getTypeWithName(processor, "java.lang.CharSequence", LineCol.SYNTHETIC);
                assertEquals(new StringConstantValue("s"), parseValueFromExpression(processor, new StringLiteral("'s'", LineCol.SYNTHETIC), charSequenceType, null));

                STypeDef objectType = getTypeWithName(processor, "java.lang.Object", LineCol.SYNTHETIC);
                Value v = parseValueFromExpression(processor, new StringLiteral("'c'", LineCol.SYNTHETIC), objectType, null);
                assertTrue(v instanceof Ins.InvokeStatic);
                assertEquals("java.lang.Character", v.type().fullName());
                assertEquals(new CharValue('c'), ((Ins.InvokeStatic) v).arguments().get(0));

                Value v2 = parseValueFromExpression(processor, new StringLiteral("\"s\"", LineCol.SYNTHETIC), objectType, null);
                assertEquals(new StringConstantValue("s"), v2);
        }

        @Test
        public void testInvokeConstructor() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    Object()");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);

                assertTrue(ins instanceof Ins.New);
                Ins.New aNew = (Ins.New) ins;
                assertEquals("java.lang.Object", aNew.constructor().declaringType().fullName());
        }

        @Test
        public void testMethodBestMatch1() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method(i:Integer)=pass\n" +
                        "    method(i:Object)=pass\n" +
                        "    method(Integer(1))");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(2, classDef.methods().size());

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.InvokeVirtual);
                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) ins;

                assertEquals("java.lang.Integer", invokeVirtual.invokable().getParameters().get(0).type().fullName());
        }

        @Test
        public void testMethodBestMatch2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method(i:Integer,j:Integer)=pass\n" +
                        "    method(i:Object,j:Number)=pass\n" +
                        "    method(Integer(1),Integer(1))");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(2, classDef.methods().size());

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.InvokeVirtual);
                Ins.InvokeVirtual invokeVirtual = (Ins.InvokeVirtual) ins;

                assertEquals("java.lang.Integer", invokeVirtual.invokable().getParameters().get(0).type().fullName());
                assertEquals("java.lang.Integer", invokeVirtual.invokable().getParameters().get(1).type().fullName());
        }

        @Test
        public void testMethodBestMatch3() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method(i:Integer,j:Object)=pass\n" +
                        "    method(i:Object,j:Number)=pass\n" +
                        "    method(Integer(1),Integer(1))");
                try {
                        parse(map);
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testArgAutoCast() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    Long(1)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.New);
                Ins.New aNew = (Ins.New) ins;

                Value v = aNew.args().get(0);
                assertTrue(v instanceof Ins.Cast);
                Ins.Cast c = (Ins.Cast) v;

                assertEquals(Ins.Cast.CAST_INT_TO_LONG, c.castMode());
                assertEquals(new IntValue(1), c.value());
        }

        @Test
        public void testAsType_parsed_into_short_and_the_value_would_be_ignored() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    1 as short");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                assertEquals(1, classDef.constructors().get(0).statements().size());
        }

        @Test
        public void testAsType() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    Short(1 as short)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.New);
                Ins.New aNew = (Ins.New) ins;

                assertEquals(new ShortValue((short) 1), aNew.args().get(0));
        }

        @Test
        public void testLocalField() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(i)\n" +
                        "    i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1 + 1);
                assertTrue(ins instanceof Ins.TLoad);
                Ins.TLoad tLoad = (Ins.TLoad) ins;
                assertEquals(1, tLoad.getIndex());
                assertEquals(Ins.TLoad.Aload, tLoad.mode());
                assertTrue(tLoad.value() instanceof SParameter);
                assertEquals("i", ((SParameter) tLoad.value()).name());
        }

        @Test
        public void testGetField() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    i:Object\n" +
                        "    i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.GetField);
                Ins.GetField getField = (Ins.GetField) ins;
                assertEquals("i", getField.field().name());
        }

        @Test
        public void testGetStatic() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        i:Object\n" +
                        "    i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.GetStatic);
                Ins.GetStatic getStatic = (Ins.GetStatic) ins;
                assertEquals("i", getStatic.field().name());
        }

        @Test
        public void testGetStaticWithType() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        i:Object\n" +
                        "    A.i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                SClassDef classDef = (SClassDef) set.iterator().next();

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.GetStatic);
                Ins.GetStatic getStatic = (Ins.GetStatic) ins;
                assertEquals("i", getStatic.field().name());
        }

        @Test
        public void testSuperThisField1() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    pro i:int\n" +
                        "class B:A\n" +
                        "    pro i:String\n" +
                        "    i");
                Set<STypeDef> set = parse(map);
                assertEquals(2, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                if (classDef.fullName().equals("test.A")) {
                        classDef = (SClassDef) it.next();
                }

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.GetField);
                Ins.GetField getField = (Ins.GetField) ins;
                assertEquals("i", getField.field().name());

                assertEquals(classDef, getField.field().declaringType());
        }

        @Test
        public void testSuperThisField2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    pro i:int\n" +
                        "class B:A\n" +
                        "    pro i:String\n" +
                        "    A.this.i");
                Set<STypeDef> set = parse(map);
                assertEquals(2, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                if (classDef.fullName().equals("test.A")) {
                        classDef = (SClassDef) it.next();
                }

                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.GetField);
                Ins.GetField getField = (Ins.GetField) ins;
                assertEquals("i", getField.field().name());

                assertEquals("test.A", getField.field().declaringType().fullName());
        }

        @Test
        public void testArrayLength() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    i:[]int\n" +
                        "    i.length");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.ArrayLength);
                Ins.ArrayLength arrayLength = (Ins.ArrayLength) ins;
                assertTrue(arrayLength.arrayValue() instanceof Ins.GetField);
                Ins.GetField gf = (Ins.GetField) arrayLength.arrayValue();
                assertEquals("i", gf.field().name());
        }

        @Test
        public void testIndex() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    i:[]int\n" +
                        "    i[0]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.TALoad);
                Ins.TALoad TALoad = (Ins.TALoad) ins;
                assertTrue(TALoad.arr() instanceof Ins.GetField);
                assertEquals("i", ((Ins.GetField) TALoad.arr()).field().name());
                assertEquals(new IntValue(0), TALoad.index());
                assertEquals(Ins.TALoad.Iaload, TALoad.mode());
        }

        @Test
        public void testIndexGet() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#>java::util::_\n" +
                        "class A\n" +
                        "    i:List\n" +
                        "    i[0]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.InvokeInterface);
                Ins.InvokeInterface invokeInterface = (Ins.InvokeInterface) ins;
                assertTrue(invokeInterface.invokable() instanceof SMethodDef);
                assertEquals("get", ((SMethodDef) invokeInterface.invokable()).name());
                assertEquals(new IntValue(0), invokeInterface.arguments().get(0));
        }

        @Test
        public void testIndexGetDynamic() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    i:Object\n" +
                        "    i[0]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction ins = classDef.constructors().get(0).statements().get(1);
                assertTrue(ins instanceof Ins.InvokeDynamic);
                Ins.InvokeDynamic invokeDynamic = (Ins.InvokeDynamic) ins;
                assertEquals("get", invokeDynamic.methodName());
                assertTrue(invokeDynamic.arguments().get(0) instanceof Ins.GetField); // i
                // Integer.valueOf(0)
                assertEquals(new IntValue(0), invokeDynamic.arguments().get(1)); // [0]
        }

        @Test
        public void testIndexAllPrimitiveTest() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    intArr:[]int\n" +
                        "    boolArr:[]bool\n" +
                        "    byteArr:[]byte\n" +
                        "    shortArr:[]short\n" +
                        "    longArr:[]long\n" +
                        "    floatArr:[]float\n" +
                        "    doubleArr:[]double\n" +
                        "    objArr:[]Object\n" +
                        "    arrArr:[][]int\n" +

                        "    intArr[0]\n" +
                        "    boolArr[0]\n" +
                        "    byteArr[0]\n" +
                        "    shortArr[0]\n" +
                        "    longArr[0]\n" +
                        "    floatArr[0]\n" +
                        "    doubleArr[0]\n" +
                        "    objArr[0]\n" +
                        "    arrArr[0]\n");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                List<Instruction> instructions = classDef.constructors().get(0).statements();
                Ins.TALoad intArr = (Ins.TALoad) instructions.get(1);
                Ins.TALoad boolArr = (Ins.TALoad) instructions.get(2);
                Ins.TALoad byteArr = (Ins.TALoad) instructions.get(3);
                Ins.TALoad shortArr = (Ins.TALoad) instructions.get(4);
                Ins.TALoad longArr = (Ins.TALoad) instructions.get(5);
                Ins.TALoad floatArr = (Ins.TALoad) instructions.get(6);
                Ins.TALoad doubleArr = (Ins.TALoad) instructions.get(7);
                Ins.TALoad objArr = (Ins.TALoad) instructions.get(8);
                Ins.TALoad arrArr = (Ins.TALoad) instructions.get(9);

                assertEquals(Ins.TALoad.Iaload, intArr.mode());
                assertEquals(Ins.TALoad.Baload, boolArr.mode());
                assertEquals(Ins.TALoad.Baload, byteArr.mode());
                assertEquals(Ins.TALoad.Saload, shortArr.mode());
                assertEquals(Ins.TALoad.Laload, longArr.mode());
                assertEquals(Ins.TALoad.Faload, floatArr.mode());
                assertEquals(Ins.TALoad.Daload, doubleArr.mode());
                assertEquals(Ins.TALoad.Aaload, objArr.mode());
                assertEquals(Ins.TALoad.Aaload, arrArr.mode());
        }

        @Test
        public void testMethod() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method(i):Unit\n" +
                        "        i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(1, classDef.methods().size());
                SMethodDef method = classDef.methods().get(0);
                assertEquals(1, method.statements().size());
                Instruction ins = method.statements().get(0);
                assertTrue(ins instanceof Ins.TLoad);
                Ins.TLoad tLoad = (Ins.TLoad) ins;
                assertEquals("i", ((SParameter) tLoad.value()).name());
        }

        @Test
        public void testStaticMethod() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        method(i):Unit\n" +
                        "            i");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(1, classDef.methods().size());
                SMethodDef method = classDef.methods().get(0);
                assertEquals(1, method.statements().size());
                Instruction ins = method.statements().get(0);
                assertTrue(ins instanceof Ins.TLoad);
                Ins.TLoad tLoad = (Ins.TLoad) ins;
                assertEquals("i", ((SParameter) tLoad.value()).name());
        }

        @Test
        public void testStatic() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        a:Object\n" +
                        "        a");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction ins = classDef.staticStatements().get(0);
                assertTrue(ins instanceof Ins.GetStatic);
                Ins.GetStatic getStatic = (Ins.GetStatic) ins;
                assertEquals("a", getStatic.field().name());
        }

        @Test
        public void testVariableDef() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()\n" +
                        "        a=1");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(1, classDef.methods().size());
                SMethodDef method = classDef.methods().get(0);
                assertEquals(1, method.statements().size());

                Instruction ins = ((ValuePack) method.statements().get(0)).instructions().get(0); // ValuePack(TStore,TLoad)
                assertTrue(ins instanceof Ins.TStore);
                Ins.TStore TStore = (Ins.TStore) ins;

                assertEquals(1, TStore.index());
                assertTrue(TStore.newValue() instanceof Ins.InvokeStatic);
                Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) TStore.newValue();
                assertEquals(new IntValue(1), invokeStatic.arguments().get(0));
        }

        @Test
        public void testVariableDefPutField() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a=1");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(1, classDef.constructors().size());
                SConstructorDef constructor = classDef.constructors().get(0);
                assertEquals(2, constructor.statements().size());

                Instruction ins = ((ValuePack) constructor.statements().get(1)).instructions().get(0);
                assertTrue(ins instanceof Ins.PutField);
                Ins.PutField putField = (Ins.PutField) ins;

                assertTrue(putField.obj() instanceof Ins.This);

                assertTrue(putField.value() instanceof Ins.InvokeStatic);
                Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) putField.value();
                assertEquals(new IntValue(1), invokeStatic.arguments().get(0));
        }

        @Test
        public void testVariableDefPutStatic() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    static\n" +
                        "        a=1");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(1, classDef.staticStatements().size());

                Instruction ins = ((ValuePack) classDef.staticStatements().get(0)).instructions().get(0);
                assertTrue(ins instanceof Ins.PutStatic);
                Ins.PutStatic putStatic = (Ins.PutStatic) ins;

                assertTrue(putStatic.value() instanceof Ins.InvokeStatic);
                Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) putStatic.value();
                assertEquals(new IntValue(1), invokeStatic.arguments().get(0));
        }

        private void testTwoVarOp(String code, Value a, int op, Value b) throws IOException, SyntaxException {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    " + code);
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();

                Instruction instruction = classDef.constructors().get(0).statements().get(1);
                assertTrue(instruction instanceof Ins.TwoVarOp);
                Ins.TwoVarOp tvo = (Ins.TwoVarOp) instruction;
                assertEquals(op, tvo.op());
                assertEquals(a, tvo.a());
                assertEquals(b, tvo.b());
        }

        @Test
        public void testTwoVarOpInt() throws Exception {
                testTwoVarOp("1+2", new IntValue(1), Ins.TwoVarOp.Iadd, new IntValue(2));
                testTwoVarOp("1-2", new IntValue(1), Ins.TwoVarOp.Isub, new IntValue(2));
                testTwoVarOp("1*2", new IntValue(1), Ins.TwoVarOp.Imul, new IntValue(2));
                testTwoVarOp("1/2", new IntValue(1), Ins.TwoVarOp.Idiv, new IntValue(2));
                testTwoVarOp("1%2", new IntValue(1), Ins.TwoVarOp.Irem, new IntValue(2));
                testTwoVarOp("1<<2", new IntValue(1), Ins.TwoVarOp.Ishl, new IntValue(2));
                testTwoVarOp("1>>2", new IntValue(1), Ins.TwoVarOp.Ishr, new IntValue(2));
                testTwoVarOp("1>>>2", new IntValue(1), Ins.TwoVarOp.Iushr, new IntValue(2));
                testTwoVarOp("1|2", new IntValue(1), Ins.TwoVarOp.Ior, new IntValue(2));
                testTwoVarOp("1&2", new IntValue(1), Ins.TwoVarOp.Iand, new IntValue(2));
                testTwoVarOp("1^2", new IntValue(1), Ins.TwoVarOp.Ixor, new IntValue(2));
        }

        @Test
        public void testTwoVarOpLong() throws Exception {
                testTwoVarOp("1 as long + 2 as long", new LongValue(1), Ins.TwoVarOp.Ladd, new LongValue(2));
                testTwoVarOp("1 as long - 2 as long", new LongValue(1), Ins.TwoVarOp.Lsub, new LongValue(2));
                testTwoVarOp("1 as long * 2 as long", new LongValue(1), Ins.TwoVarOp.Lmul, new LongValue(2));
                testTwoVarOp("1 as long / 2 as long", new LongValue(1), Ins.TwoVarOp.Ldiv, new LongValue(2));
                testTwoVarOp("1 as long % 2 as long", new LongValue(1), Ins.TwoVarOp.Lrem, new LongValue(2));
                testTwoVarOp("1 as long << 2 as long", new LongValue(1), Ins.TwoVarOp.Lshl, new LongValue(2));
                testTwoVarOp("1 as long >> 2 as long", new LongValue(1), Ins.TwoVarOp.Lshr, new LongValue(2));
                testTwoVarOp("1 as long >>> 2 as long", new LongValue(1), Ins.TwoVarOp.Lushr, new LongValue(2));
                testTwoVarOp("1 as long | 2 as long", new LongValue(1), Ins.TwoVarOp.Lor, new LongValue(2));
                testTwoVarOp("1 as long & 2 as long", new LongValue(1), Ins.TwoVarOp.Land, new LongValue(2));
                testTwoVarOp("1 as long ^ 2 as long", new LongValue(1), Ins.TwoVarOp.Lxor, new LongValue(2));
        }

        @Test
        public void testTwoVarOpDouble() throws Exception {
                testTwoVarOp("1.0+2.0", new DoubleValue(1), Ins.TwoVarOp.Dadd, new DoubleValue(2));
                testTwoVarOp("1.0-2.0", new DoubleValue(1), Ins.TwoVarOp.Dsub, new DoubleValue(2));
                testTwoVarOp("1.0*2.0", new DoubleValue(1), Ins.TwoVarOp.Dmul, new DoubleValue(2));
                testTwoVarOp("1.0/2.0", new DoubleValue(1), Ins.TwoVarOp.Ddiv, new DoubleValue(2));
                testTwoVarOp("1.0%2.0", new DoubleValue(1), Ins.TwoVarOp.Drem, new DoubleValue(2));
        }

        @Test
        public void testTwoVarOpFloat() throws Exception {
                testTwoVarOp("1 as float + 2 as float", new FloatValue(1), Ins.TwoVarOp.Fadd, new FloatValue(2));
                testTwoVarOp("1 as float - 2 as float", new FloatValue(1), Ins.TwoVarOp.Fsub, new FloatValue(2));
                testTwoVarOp("1 as float * 2 as float", new FloatValue(1), Ins.TwoVarOp.Fmul, new FloatValue(2));
                testTwoVarOp("1 as float / 2 as float", new FloatValue(1), Ins.TwoVarOp.Fdiv, new FloatValue(2));
                testTwoVarOp("1 as float % 2 as float", new FloatValue(1), Ins.TwoVarOp.Frem, new FloatValue(2));
        }

        @Test
        public void testIf() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()\n" +
                        "        if true\n" +
                        "            <1\n" +
                        "        elseif false\n" +
                        "            <2\n" +
                        "        elseif true\n" +
                        "            <3\n" +
                        "        else\n" +
                        "            <4");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SMethodDef method = classDef.methods().get(0);

                Instruction i1 = method.statements().get(0); // if true
                Instruction i2 = method.statements().get(1); // if false
                Instruction i3 = method.statements().get(2); // if true
                Instruction i4 = method.statements().get(3); // goto

                Instruction i5 = method.statements().get(4); // return 1
                Instruction i6 = method.statements().get(5); // goto

                Instruction i7 = method.statements().get(6); // return 2
                Instruction i8 = method.statements().get(7); // goto

                Instruction i9 = method.statements().get(8); // return 3
                Instruction i10 = method.statements().get(9); // goto

                Instruction i11 = method.statements().get(10); // return 4
                Instruction i12 = method.statements().get(11); // goto

                Instruction i13 = method.statements().get(12); // nop

                assertTrue(i1 instanceof Ins.IfNe);
                assertEquals(i5, ((Ins.IfNe) i1).gotoIns());
                assertTrue(i2 instanceof Ins.IfNe);
                assertEquals(i7, ((Ins.IfNe) i2).gotoIns());
                assertTrue(i3 instanceof Ins.IfNe);
                assertEquals(i9, ((Ins.IfNe) i3).gotoIns());
                assertTrue(i4 instanceof Ins.Goto);
                assertEquals(i11, ((Ins.Goto) i4).gotoIns());

                assertTrue(i5 instanceof Ins.TReturn);
                assertTrue(i6 instanceof Ins.Goto);

                assertTrue(i7 instanceof Ins.TReturn);
                assertTrue(i8 instanceof Ins.Goto);

                assertTrue(i9 instanceof Ins.TReturn);
                assertTrue(i10 instanceof Ins.Goto);

                assertTrue(i11 instanceof Ins.TReturn);
                assertTrue(i12 instanceof Ins.Goto);

                assertTrue(i13 instanceof Ins.Nop);
        }

        @Test
        public void testWhile() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    while true\n" +
                        "        a=1\n" +
                        "    a=2");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1); // if eq true
                Instruction i2 = ((ValuePack) con.statements().get(2)).instructions().get(0); // ValuePack(PutField,GetField)
                Instruction i3 = con.statements().get(3); // goto
                Instruction i4 = con.statements().get(4); // Nop
                Instruction i5 = ((ValuePack) con.statements().get(5)).instructions().get(0); // ValuePack(PutField,GetField)

                assertTrue(i1 instanceof Ins.IfEq);
                assertTrue(i2 instanceof Ins.PutField);
                assertTrue(i3 instanceof Ins.Goto);
                assertTrue(i4 instanceof Ins.Nop);
                assertTrue(i5 instanceof Ins.PutField);

                assertEquals(i4, ((Ins.IfEq) i1).gotoIns());
                assertEquals(i1, ((Ins.Goto) i3).gotoIns());
        }

        @Test
        public void testDoWhile() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    do\n" +
                        "        a=1\n" +
                        "    while true\n" +
                        "    a=2");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                ValuePack gotoHere = (ValuePack) con.statements().get(1);

                Instruction i1 = ((ValuePack) con.statements().get(1)).instructions().get(0); // ValuePack(PutField,GetField)
                Instruction i2 = con.statements().get(2); // IfNe
                Instruction i3 = ((ValuePack) con.statements().get(3)).instructions().get(0); // ValuePack(PutField,GetField)

                assertTrue(i1 instanceof Ins.PutField);
                assertTrue(i2 instanceof Ins.IfNe);
                assertTrue(i3 instanceof Ins.PutField);

                assertEquals(gotoHere, ((Ins.IfNe) i2).gotoIns());
        }

        @Test
        public void testFor() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(ls)\n" +
                        "    for i in ls\n" +
                        "        a=1\n" +
                        "    a=2");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1 + 1); // TStore aload invokestatic tstore
                Instruction i2 = con.statements().get(2 + 1); // IfEq aload invokevirtual(hasNext) ifeq
                Instruction i3 = con.statements().get(3 + 1); // TStore aload invokevirtual(next) tstore
                Instruction i4 = ((ValuePack) con.statements().get(4 + 1)).instructions().get(0); // ValuePack(PutField,GetField)
                Instruction i5 = con.statements().get(5 + 1); // Goto
                Instruction i6 = con.statements().get(6 + 1); // Nop
                Instruction i7 = ((ValuePack) con.statements().get(7 + 1)).instructions().get(0); // ValuePack(PutField,GetField)

                assertTrue(i1 instanceof Ins.TStore);
                assertTrue(i2 instanceof Ins.IfEq);
                assertTrue(i3 instanceof Ins.TStore);
                assertTrue(i4 instanceof Ins.PutField);
                assertTrue(i5 instanceof Ins.Goto);
                assertTrue(i6 instanceof Ins.Nop);
                assertTrue(i7 instanceof Ins.PutField);

                assertEquals(i6, ((Ins.IfEq) i2).gotoIns());
                assertEquals(i2, ((Ins.Goto) i5).gotoIns());
        }

        @Test
        public void testThrow() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    throw RuntimeException()");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1);
                assertTrue(i1 instanceof Ins.AThrow);
                assertTrue(((Ins.AThrow) i1).exception() instanceof Ins.New);
        }

        @Test
        public void testTryCatch() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    try\n" +
                        "        throw RuntimeException()\n" +
                        "    catch e\n" +
                        "        RuntimeException,Exception\n" +
                        "            e.printStackTrace()\n" +
                        "        Error\n" +
                        "            e.printStackTrace()\n" +
                        "        Throwable\n" +
                        "            e.printStackTrace()");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1); // throw
                Instruction i2 = con.statements().get(2); // goto
                Instruction i3 = con.statements().get(3); // exStore (RuntimeException)
                Instruction i4 = con.statements().get(4); // invokeVirtual
                Instruction i5 = con.statements().get(5); // goto
                Instruction i6 = con.statements().get(6); // exStore (Exception)
                Instruction i7 = con.statements().get(7); // invokeVirtual
                Instruction i8 = con.statements().get(8); // goto
                Instruction i9 = con.statements().get(9); // exStore (Error)
                Instruction i10 = con.statements().get(10); // invokeVirtual
                Instruction i11 = con.statements().get(11); // goto
                Instruction i12 = con.statements().get(12); // exStore (Throwable)
                Instruction i13 = con.statements().get(13); // invokeVirtual
                Instruction i14 = con.statements().get(14); // goto
                Instruction i15 = con.statements().get(15); // exStore (Throwable) exceptionFinally
                Instruction i16 = con.statements().get(16); // throw
                Instruction i17 = con.statements().get(17); // nop

                assertTrue(i1 instanceof Ins.AThrow);
                assertTrue(i2 instanceof Ins.Goto);
                assertTrue(i3 instanceof Ins.ExStore);
                assertTrue(i4 instanceof Ins.InvokeVirtual);
                assertTrue(i5 instanceof Ins.Goto);
                assertTrue(i6 instanceof Ins.ExStore);
                assertTrue(i7 instanceof Ins.InvokeVirtual);
                assertTrue(i8 instanceof Ins.Goto);
                assertTrue(i9 instanceof Ins.ExStore);
                assertTrue(i10 instanceof Ins.InvokeVirtual);
                assertTrue(i11 instanceof Ins.Goto);
                assertTrue(i12 instanceof Ins.ExStore);
                assertTrue(i13 instanceof Ins.InvokeVirtual);
                assertTrue(i14 instanceof Ins.Goto);
                assertTrue(i15 instanceof Ins.ExStore);
                assertTrue(i16 instanceof Ins.AThrow);
                assertTrue(i17 instanceof Ins.Nop);
                assertEquals(18, con.statements().size());

                assertEquals(i17, ((Ins.Goto) i2).gotoIns());
                assertEquals(i17, ((Ins.Goto) i5).gotoIns());
                assertEquals(i17, ((Ins.Goto) i8).gotoIns());
                assertEquals(i17, ((Ins.Goto) i11).gotoIns());
                assertEquals(i17, ((Ins.Goto) i14).gotoIns());

                ExceptionTable tbl0 = con.exceptionTables().get(0); // 1-2 goto 3   NullPointerException
                ExceptionTable tbl1 = con.exceptionTables().get(1); // 3-5 goto 15  any
                ExceptionTable tbl2 = con.exceptionTables().get(2); // 1-2 goto 6   Exception
                ExceptionTable tbl3 = con.exceptionTables().get(3); // 6-8 goto 15  any
                ExceptionTable tbl4 = con.exceptionTables().get(4); // 1-2 goto 9   Error
                ExceptionTable tbl5 = con.exceptionTables().get(5); // 9-11goto 15  any
                ExceptionTable tbl6 = con.exceptionTables().get(6); // 1-2 goto 12  Throwable
                ExceptionTable tbl7 = con.exceptionTables().get(7); // 12-14goto15  any
                assertEquals(8, con.exceptionTables().size());

                assertEquals(i1, tbl0.getFrom());
                assertEquals(i1, tbl2.getFrom());
                assertEquals(i1, tbl4.getFrom());
                assertEquals(i1, tbl6.getFrom());

                assertEquals(i3, tbl1.getFrom());
                assertEquals(i6, tbl3.getFrom());
                assertEquals(i9, tbl5.getFrom());
                assertEquals(i12, tbl7.getFrom());

                assertEquals(i2, tbl0.getTo());
                assertEquals(i2, tbl2.getTo());
                assertEquals(i2, tbl4.getTo());
                assertEquals(i2, tbl6.getTo());

                assertEquals(i5, tbl1.getTo());
                assertEquals(i8, tbl3.getTo());
                assertEquals(i11, tbl5.getTo());
                assertEquals(i14, tbl7.getTo());

                assertEquals(i3, tbl0.getTarget());
                assertEquals(i6, tbl2.getTarget());
                assertEquals(i9, tbl4.getTarget());
                assertEquals(i12, tbl6.getTarget());

                assertEquals(i15, tbl1.getTarget());
                assertEquals(i15, tbl3.getTarget());
                assertEquals(i15, tbl5.getTarget());
                assertEquals(i15, tbl7.getTarget());

                assertEquals("java.lang.RuntimeException", tbl0.getType().fullName());
                assertEquals("java.lang.Exception", tbl2.getType().fullName());
                assertEquals("java.lang.Error", tbl4.getType().fullName());
                assertEquals("java.lang.Throwable", tbl6.getType().fullName());

                assertNull(tbl1.getType());
                assertNull(tbl3.getType());
                assertNull(tbl5.getType());
                assertNull(tbl7.getType());
        }

        @Test
        public void testTryCatchWithoutThrowable() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    try\n" +
                        "        throw RuntimeException()\n" +
                        "    catch e\n" +
                        "        RuntimeException,Exception\n" +
                        "            e.printStackTrace()\n" +
                        "        Error\n" +
                        "            e.printStackTrace()");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1); // throw
                Instruction i2 = con.statements().get(2); // goto
                Instruction i3 = con.statements().get(3); // exStore (RuntimeException)
                Instruction i4 = con.statements().get(4); // invokeVirtual
                Instruction i5 = con.statements().get(5); // goto
                Instruction i6 = con.statements().get(6); // exStore (Exception)
                Instruction i7 = con.statements().get(7); // invokeVirtual
                Instruction i8 = con.statements().get(8); // goto
                Instruction i9 = con.statements().get(9); // exStore (Error)
                Instruction i10 = con.statements().get(10); // invokeVirtual
                Instruction i11 = con.statements().get(11); // goto
                Instruction i12 = con.statements().get(12); // exStore (Throwable) exceptionFinally
                Instruction i13 = con.statements().get(13); // throw
                Instruction i14 = con.statements().get(14); // nop

                assertTrue(i1 instanceof Ins.AThrow);
                assertTrue(i2 instanceof Ins.Goto);
                assertTrue(i3 instanceof Ins.ExStore);
                assertTrue(i4 instanceof Ins.InvokeVirtual);
                assertTrue(i5 instanceof Ins.Goto);
                assertTrue(i6 instanceof Ins.ExStore);
                assertTrue(i7 instanceof Ins.InvokeVirtual);
                assertTrue(i8 instanceof Ins.Goto);
                assertTrue(i9 instanceof Ins.ExStore);
                assertTrue(i10 instanceof Ins.InvokeVirtual);
                assertTrue(i11 instanceof Ins.Goto);
                assertTrue(i12 instanceof Ins.ExStore);
                assertTrue(i13 instanceof Ins.AThrow);
                assertTrue(i14 instanceof Ins.Nop);
                assertEquals(15, con.statements().size());

                assertEquals(i14, ((Ins.Goto) i2).gotoIns());
                assertEquals(i14, ((Ins.Goto) i5).gotoIns());
                assertEquals(i14, ((Ins.Goto) i8).gotoIns());
                assertEquals(i14, ((Ins.Goto) i11).gotoIns());

                ExceptionTable tbl0 = con.exceptionTables().get(0); // 1-2 goto 3   NullPointerException
                ExceptionTable tbl1 = con.exceptionTables().get(1); // 3-5 goto 15  any
                ExceptionTable tbl2 = con.exceptionTables().get(2); // 1-2 goto 6   Exception
                ExceptionTable tbl3 = con.exceptionTables().get(3); // 6-8 goto 15  any
                ExceptionTable tbl4 = con.exceptionTables().get(4); // 1-2 goto 9   Error
                ExceptionTable tbl5 = con.exceptionTables().get(5); // 9-11goto 15  any
                ExceptionTable tbl6 = con.exceptionTables().get(6); // 1-2 goto 12  any
                assertEquals(7, con.exceptionTables().size());

                assertEquals(i1, tbl0.getFrom());
                assertEquals(i1, tbl2.getFrom());
                assertEquals(i1, tbl4.getFrom());
                assertEquals(i1, tbl6.getFrom());

                assertEquals(i3, tbl1.getFrom());
                assertEquals(i6, tbl3.getFrom());
                assertEquals(i9, tbl5.getFrom());

                assertEquals(i2, tbl0.getTo());
                assertEquals(i2, tbl2.getTo());
                assertEquals(i2, tbl4.getTo());
                assertEquals(i2, tbl6.getTo());

                assertEquals(i5, tbl1.getTo());
                assertEquals(i8, tbl3.getTo());
                assertEquals(i11, tbl5.getTo());

                assertEquals(i3, tbl0.getTarget());
                assertEquals(i6, tbl2.getTarget());
                assertEquals(i9, tbl4.getTarget());
                assertEquals(i12, tbl6.getTarget());

                assertEquals(i12, tbl1.getTarget());
                assertEquals(i12, tbl3.getTarget());
                assertEquals(i12, tbl5.getTarget());

                assertEquals("java.lang.RuntimeException", tbl0.getType().fullName());
                assertEquals("java.lang.Exception", tbl2.getType().fullName());
                assertEquals("java.lang.Error", tbl4.getType().fullName());

                assertNull(tbl1.getType());
                assertNull(tbl3.getType());
                assertNull(tbl5.getType());
                assertNull(tbl6.getType());
        }

        @Test
        public void testUndefined() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a=undefined");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = ((ValuePack) con.statements().get(1)).instructions().get(0);
                assertTrue(i1 instanceof Ins.PutField);
                assertTrue(((Ins.PutField) i1).value() instanceof Ins.InvokeStatic);
                assertTrue(((Ins.InvokeStatic) ((Ins.PutField) i1).value()).invokable() instanceof SMethodDef);
                SMethodDef m = (SMethodDef) ((Ins.InvokeStatic) ((Ins.PutField) i1).value()).invokable();
                assertEquals("get", m.name());
                assertEquals("lt.lang.Undefined", m.declaringType().fullName());
        }

        @Test
        public void testUnaryInc() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a:int)\n" +
                        "    ++a");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1 + 1);
                assertTrue(i1 instanceof ValuePack);
                assertTrue(((ValuePack) i1).instructions().get(0) instanceof Ins.TStore);
                assertTrue(((ValuePack) i1).instructions().get(1) instanceof Ins.TLoad);
                assertTrue(((Ins.TStore) ((ValuePack) i1).instructions().get(0)).newValue() instanceof Ins.TwoVarOp);
        }

        @Test
        public void testSelfInc() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a:int)\n" +
                        "    a++");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i = con.statements().get(1 + 1);
                assertTrue(i instanceof ValuePack);
                ValuePack valuePack = (ValuePack) i;

                Instruction i0 = valuePack.instructions().get(0); // TLoad
                Instruction i1 = valuePack.instructions().get(1); // TStore
                Instruction i2 = valuePack.instructions().get(2); // TLoad
                Instruction i3 = valuePack.instructions().get(3); // pop

                assertTrue(i0 instanceof Ins.TLoad);
                assertTrue(i1 instanceof Ins.TStore);
                assertTrue(i2 instanceof Ins.TLoad);
                assertTrue(i3 instanceof Ins.Pop);
        }

        @Test
        public void testArrayAssign() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a:[]int)\n" +
                        "    a[1]=10");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i = con.statements().get(1 + 1);
                assertTrue(i instanceof ValuePack);
                ValuePack valuePack = (ValuePack) i;

                Instruction i0 = valuePack.instructions().get(0); // IAStore
                Instruction i1 = valuePack.instructions().get(1); // IALoad

                assertTrue(i0 instanceof Ins.TAStore);
                assertTrue(i1 instanceof Ins.TALoad);

                Ins.TAStore TAStore = (Ins.TAStore) i0;
                assertEquals(Ins.TAStore.IASTORE, TAStore.mode());
                Ins.TALoad TALoad = (Ins.TALoad) i1;
                assertEquals(Ins.TALoad.Iaload, TALoad.mode());
        }

        @Test
        public void testIndexAccessAssignInvoke() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> java::util::_\n" +
                        "class A(a:List)\n" +
                        "    a[1]=10");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i = con.statements().get(1 + 1);
                assertTrue(i instanceof ValuePack);
                ValuePack valuePack = (ValuePack) i;

                Instruction i0 = valuePack.instructions().get(0); // InvokeDynamic
                Instruction i1 = valuePack.instructions().get(1); // InvokeInterface

                assertTrue(i0 instanceof Ins.InvokeInterface);
                assertTrue(i1 instanceof Ins.InvokeInterface);

                Ins.InvokeInterface in0 = (Ins.InvokeInterface) i0;
                assertEquals("set", ((SMethodDef) in0.invokable()).name());
                assertEquals(2, in0.arguments().size());
                assertTrue(in0.arguments().get(0) instanceof IntValue);
                assertTrue(in0.arguments().get(1) instanceof Ins.InvokeStatic);

                Ins.InvokeInterface in1 = (Ins.InvokeInterface) i1;
                assertEquals("get", ((SMethodDef) in1.invokable()).name());
        }

        @Test
        public void testIndexAccessAssignInvoke2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a)\n" +
                        "    a[1]=10");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i = con.statements().get(1 + 1);
                assertTrue(i instanceof ValuePack);
                ValuePack valuePack = (ValuePack) i;

                Instruction i0 = valuePack.instructions().get(0); // InvokeDynamic
                Instruction i1 = valuePack.instructions().get(1); // InvokeDynamic

                assertTrue(i0 instanceof Ins.InvokeDynamic);
                assertTrue(i1 instanceof Ins.InvokeDynamic);

                Ins.InvokeDynamic in0 = (Ins.InvokeDynamic) i0;
                assertEquals("set", in0.methodName());
                assertEquals(3, in0.arguments().size());
                assertTrue(in0.arguments().get(0) instanceof Ins.TLoad);
                assertTrue(in0.arguments().get(1) instanceof IntValue);
                assertTrue(in0.arguments().get(2) instanceof IntValue);

                Ins.InvokeDynamic in1 = (Ins.InvokeDynamic) i1;
                assertEquals("get", in1.methodName());
                assertEquals(2, in1.arguments().size());
        }

        @Test
        public void testNewArray() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a:[]int=[10,20]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Ins.PutField putField = (Ins.PutField) ((ValuePack) con.statements().get(1)).instructions().get(0);
                Ins.NewArray newArray = (Ins.NewArray) putField.value();
                assertEquals(2, newArray.count().getValue());
                assertEquals(Ins.NewArray.NewIntArray, newArray.mode());

                assertEquals(10, ((IntValue) newArray.initValues().get(0)).getValue());
                assertEquals(20, ((IntValue) newArray.initValues().get(1)).getValue());
        }

        @Test
        public void testANewArray() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a:[]Object=[10,20]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Ins.PutField putField = (Ins.PutField) ((ValuePack) con.statements().get(1)).instructions().get(0);
                Ins.ANewArray newArray = (Ins.ANewArray) putField.value();
                assertEquals(2, newArray.count().getValue());
                assertEquals("java.lang.Object", newArray.componentType().fullName());

                assertTrue(newArray.initValues().get(0) instanceof Ins.InvokeStatic);
                assertTrue(newArray.initValues().get(1) instanceof Ins.InvokeStatic);
        }

        @Test
        public void testNewList() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a=[10,20]");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Ins.PutField putField = (Ins.PutField) ((ValuePack) con.statements().get(1)).instructions().get(0);
                Ins.NewList newList = (Ins.NewList) putField.value();
                assertEquals("java.util.LinkedList", newList.type().fullName());

                assertTrue(newList.initValues().get(0) instanceof Ins.InvokeStatic);
                assertTrue(newList.initValues().get(1) instanceof Ins.InvokeStatic);
        }

        @Test
        public void testNewMap() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    a={\n" +
                        "        \"a\":1\n" +
                        "        \"b\":2\n" +
                        "        \"c\":3\n" +
                        "        \"d\":4\n" +
                        "    }");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Ins.PutField putField = (Ins.PutField) ((ValuePack) con.statements().get(1)).instructions().get(0);
                Ins.NewMap newMap = (Ins.NewMap) putField.value();

                Iterator<Map.Entry<Value, Value>> entries = newMap.initValues().entrySet().iterator();
                Map.Entry<Value, Value> entry = entries.next();
                assertEquals("a", ((StringConstantValue) entry.getKey()).getStr());
                assertTrue(entry.getValue() instanceof Ins.InvokeStatic);

                entry = entries.next();
                assertEquals("b", ((StringConstantValue) entry.getKey()).getStr());
                assertTrue(entry.getValue() instanceof Ins.InvokeStatic);

                entry = entries.next();
                assertEquals("c", ((StringConstantValue) entry.getKey()).getStr());
                assertTrue(entry.getValue() instanceof Ins.InvokeStatic);

                entry = entries.next();
                assertEquals("d", ((StringConstantValue) entry.getKey()).getStr());
                assertTrue(entry.getValue() instanceof Ins.InvokeStatic);
        }

        @Test
        public void testSync() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a,b)\n" +
                        "    sync(a,b)\n" +
                        "        a=2\n" +
                        "        b=3");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1 + 2); // monitor enter
                Instruction i2 = con.statements().get(2 + 2); // monitor enter
                Instruction i3 = con.statements().get(3 + 2); // ValuePack(PutField,GetField)
                Instruction i4 = con.statements().get(4 + 2); // ValuePack(PutField,GetField)
                Instruction i5 = con.statements().get(5 + 2); // monitor exit
                Instruction i6 = con.statements().get(6 + 2); // monitor exit
                Instruction i7 = con.statements().get(7 + 2); // goto
                Instruction i8 = con.statements().get(8 + 2); // astore
                Instruction i9 = con.statements().get(9 + 2); // monitor exit
                Instruction i10 = con.statements().get(10 + 2); // monitor exit
                Instruction i11 = con.statements().get(11 + 2); // athrow

                assertTrue(i1 instanceof Ins.MonitorEnter);
                assertTrue(i2 instanceof Ins.MonitorEnter);
                assertTrue(i3 instanceof ValuePack);
                assertTrue(i4 instanceof ValuePack);
                assertTrue(i5 instanceof Ins.MonitorExit);
                assertTrue(i6 instanceof Ins.MonitorExit);
                assertTrue(i7 instanceof Ins.Goto);
                assertTrue(i8 instanceof Ins.ExStore);
                assertTrue(i9 instanceof Ins.MonitorExit);
                assertTrue(i10 instanceof Ins.MonitorExit);
                assertTrue(i11 instanceof Ins.AThrow);

                ValuePack p3 = (ValuePack) i3;
                ValuePack p4 = (ValuePack) i4;

                Instruction i30 = p3.instructions().get(0);
                assertTrue(i30 instanceof Ins.TStore);
                Instruction i40 = p4.instructions().get(0);
                assertTrue(i40 instanceof Ins.TStore);
        }

        @Test
        public void testSyncReturn() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a,b)\n" +
                        "    sync(a,b)\n" +
                        "        a=2\n" +
                        "        b=3\n" +
                        "        <");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1 + 2); // monitor enter
                Instruction i2 = con.statements().get(2 + 2); // monitor enter
                Instruction i3 = con.statements().get(3 + 2); // ValuePack(PutField,GetField)
                Instruction i4 = con.statements().get(4 + 2); // ValuePack(PutField,GetField)
                Instruction i5 = con.statements().get(5 + 2); // monitor exit
                Instruction i6 = con.statements().get(6 + 2); // monitor exit
                Instruction i7 = con.statements().get(7 + 2); // TReturn
                Instruction i8 = con.statements().get(8 + 2); // monitor exit
                Instruction i9 = con.statements().get(9 + 2); // monitor exit
                Instruction i10 = con.statements().get(10 + 2); // goto
                Instruction i11 = con.statements().get(11 + 2); // astore
                Instruction i12 = con.statements().get(12 + 2); // monitor exit
                Instruction i13 = con.statements().get(13 + 2); // monitor exit
                Instruction i14 = con.statements().get(14 + 2); // athrow

                assertTrue(i1 instanceof Ins.MonitorEnter);
                assertTrue(i2 instanceof Ins.MonitorEnter);
                assertTrue(i3 instanceof ValuePack);
                assertTrue(i4 instanceof ValuePack);
                assertTrue(i5 instanceof Ins.MonitorExit);
                assertTrue(i6 instanceof Ins.MonitorExit);
                assertTrue(i7 instanceof Ins.TReturn);
                assertTrue(i8 instanceof Ins.MonitorExit);
                assertTrue(i9 instanceof Ins.MonitorExit);
                assertTrue(i10 instanceof Ins.Goto);
                assertTrue(i11 instanceof Ins.ExStore);
                assertTrue(i12 instanceof Ins.MonitorExit);
                assertTrue(i13 instanceof Ins.MonitorExit);
                assertTrue(i14 instanceof Ins.AThrow);

                ValuePack p3 = (ValuePack) i3;
                ValuePack p4 = (ValuePack) i4;

                Instruction i30 = p3.instructions().get(0);
                assertTrue(i30 instanceof Ins.TStore);
                Instruction i40 = p4.instructions().get(0);
                assertTrue(i40 instanceof Ins.TStore);
        }

        @Test
        public void testTryCatchFinally() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    try\n" +
                        "        System.out.println('hello')\n" +
                        "    catch e\n" +
                        "        RuntimeException\n" +
                        "            e.printStackTrace()\n" +
                        "    finally\n" +
                        "        System.out.println(' world')");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1); // InvokeVirtual
                Instruction i2 = con.statements().get(2); // goto (normal finally)
                Instruction i3 = con.statements().get(3); // exStore
                Instruction i4 = con.statements().get(4); // invokeVirtual
                Instruction i5 = con.statements().get(5); // goto (normal finally)
                Instruction i6 = con.statements().get(6); // exStore (exception finally)
                Instruction i7 = con.statements().get(7); // InvokeVirtual
                Instruction i8 = con.statements().get(8); // aThrow
                Instruction i9 = con.statements().get(9); // InvokeVirtual (normal finally)
                assertEquals(10, con.statements().size());

                assertTrue(i1 instanceof Ins.InvokeVirtual);
                assertTrue(i2 instanceof Ins.Goto);
                assertTrue(i3 instanceof Ins.ExStore);
                assertTrue(i4 instanceof Ins.InvokeVirtual);
                assertTrue(i5 instanceof Ins.Goto);
                assertTrue(i6 instanceof Ins.ExStore);
                assertTrue(i7 instanceof Ins.InvokeVirtual);
                assertTrue(i8 instanceof Ins.AThrow);
                assertTrue(i9 instanceof Ins.InvokeVirtual);

                assertEquals(i9, ((Ins.Goto) i2).gotoIns());
                assertEquals(i9, ((Ins.Goto) i5).gotoIns());

                ExceptionTable tbl0 = con.exceptionTables().get(0); // 1-2 goto 3  RuntimeException
                ExceptionTable tbl1 = con.exceptionTables().get(1); // 3-5 goto 6  any
                ExceptionTable tbl2 = con.exceptionTables().get(2); // 1-2 goto 6  any
                assertEquals(3, con.exceptionTables().size());

                assertEquals(i1, tbl0.getFrom());
                assertEquals(i2, tbl0.getTo());
                assertEquals(i3, tbl0.getTarget());
                assertEquals("java.lang.RuntimeException", tbl0.getType().fullName());

                assertEquals(i3, tbl1.getFrom());
                assertEquals(i5, tbl1.getTo());
                assertEquals(i6, tbl1.getTarget());
                assertNull(tbl1.getType());

                assertEquals(i1, tbl2.getFrom());
                assertEquals(i2, tbl2.getTo());
                assertEquals(i6, tbl2.getTarget());
                assertNull(tbl2.getType());
        }

        @Test
        public void testConstructorField() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A(a,b)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SConstructorDef con = classDef.constructors().get(0);

                Instruction i1 = con.statements().get(1);
                Instruction i2 = con.statements().get(2);

                assertTrue(i1 instanceof Ins.PutField);
                assertTrue(i2 instanceof Ins.PutField);

                assertEquals(2, classDef.fields().size());
                assertEquals("a", classDef.fields().get(0).name());
                assertEquals("b", classDef.fields().get(1).name());
        }

        @Test
        public void testInnerMethod1() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()\n" +
                        "        inner():Unit");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(2, classDef.methods().size());
                SMethodDef method = classDef.methods().get(0);
                SMethodDef innerMethod = classDef.methods().get(1);

                assertEquals("method", method.name());
                assertEquals("inner$LessTyping$InnerMethod$0", innerMethod.name());
                assertEquals(0, innerMethod.getParameters().size());
        }

        @Test
        public void testInnerMethod2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method()\n" +
                        "        i:int=1\n" +
                        "        inner():Unit\n" +
                        "        j=2");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                assertEquals(2, classDef.methods().size());
                SMethodDef method = classDef.methods().get(0);
                SMethodDef innerMethod = classDef.methods().get(1);

                assertEquals("method", method.name());
                assertEquals("inner$LessTyping$InnerMethod$0", innerMethod.name());

                assertEquals(1, innerMethod.getParameters().size());
                assertTrue(innerMethod.getParameters().get(0).type().equals(IntTypeDef.get()));
                assertEquals("i", innerMethod.getParameters().get(0).name());
        }

        @Test
        public void testProcedure1() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    method():int\n" +
                        "        <(<1)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SMethodDef method = classDef.methods().get(0);
                Instruction i0 = method.statements().get(0);

                assertTrue(i0 instanceof Ins.TReturn);
                assertTrue(((Ins.TReturn) i0).value() instanceof Ins.InvokeSpecial);
                Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) ((Ins.TReturn) i0).value();

                assertTrue(invokeSpecial.invokable() instanceof SMethodDef);
                assertEquals("procedure$0$LessTyping$InnerMethod$0", ((SMethodDef) invokeSpecial.invokable()).name());

                assertEquals(new IntValue(1), ((Ins.TReturn) invokeSpecial.invokable().statements().get(0)).value());
        }

        @Test
        public void testProcedure2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    i:int=(<1)");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                Instruction i1 = ((ValuePack) classDef.constructors().get(0).statements().get(1)).instructions().get(0);

                assertTrue(i1 instanceof Ins.PutField);
                assertTrue(((Ins.PutField) i1).value() instanceof Ins.InvokeSpecial);
                Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) ((Ins.PutField) i1).value();

                assertTrue(invokeSpecial.invokable() instanceof SMethodDef);
                assertEquals("procedure$0$LessTyping$InnerMethod$0", ((SMethodDef) invokeSpecial.invokable()).name());

                assertEquals(new IntValue(1), ((Ins.TReturn) invokeSpecial.invokable().statements().get(0)).value());
        }

        @Test
        public void testLambda1() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> java::util::function::_\n" +
                        "class A\n" +
                        "    func:Function=(o)->\n" +
                        "        <1");
                Set<STypeDef> set = parse(map);
                assertEquals(2, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                if (!classDef.fullName().equals("test.A")) classDef = (SClassDef) it.next();
                Instruction i1 = ((ValuePack) classDef.constructors().get(0).statements().get(1)).instructions().get(0);

                assertTrue(i1 instanceof Ins.PutField);
                Value v = ((Ins.PutField) i1).value();
                assertTrue(v instanceof Ins.New);
                Ins.New aNew = (Ins.New) v;
                assertEquals("test.A$LessTyping$Lambda$0", aNew.constructor().declaringType().fullName());
                assertEquals(3, aNew.args().size());
        }

        @Test
        public void testLambda2() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> java::util::function::_\n" +
                        "class A(a)\n" +
                        "    func:Function=(o)->\n" +
                        "        <1");
                Set<STypeDef> set = parse(map);
                assertEquals(2, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                if (!classDef.fullName().equals("test.A")) classDef = (SClassDef) it.next();
                Instruction i1 = ((ValuePack) classDef.constructors().get(0).statements().get(1 + 1)).instructions().get(0);

                assertTrue(i1 instanceof Ins.PutField);
                Value v = ((Ins.PutField) i1).value();
                assertTrue(v instanceof Ins.New);
                Ins.New aNew = (Ins.New) v;
                assertEquals("test.A$LessTyping$Lambda$0", aNew.constructor().declaringType().fullName());
                assertEquals(3, aNew.args().size());
        }

        @Test
        public void testLambda3() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "#> lt::compiler::_\n" +
                        "class A(a)\n" +
                        "    func:TestLambdaFunc=(o)->\n" +
                        "        <1");
                Set<STypeDef> set = parse(map);
                assertEquals(2, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();
                SClassDef testA = null;
                SClassDef lambda = null;
                if (classDef.fullName().equals("test.A")) {
                        testA = classDef;
                } else {
                        lambda = classDef;
                }
                classDef = (SClassDef) it.next();
                if (classDef.fullName().equals("test.A")) {
                        testA = classDef;
                } else {
                        lambda = classDef;
                }

                // test.A
                assert testA != null;
                Instruction i1 = ((ValuePack) testA.constructors().get(0).statements().get(1 + 1)).instructions().get(0);

                assertTrue(i1 instanceof Ins.PutField);
                Value v = ((Ins.PutField) i1).value();
                assertTrue(v instanceof Ins.New);
                Ins.New aNew = (Ins.New) v;
                assertEquals(3, aNew.args().size());

                // lambda
                assert lambda != null;
                assertEquals("methodHandle", lambda.fields().get(0).name());
                assertEquals("o", lambda.fields().get(1).name());
                assertEquals("local", lambda.fields().get(2).name());

                i1 = lambda.constructors().get(0).statements().get(1); // put field
                Instruction i2 = lambda.constructors().get(0).statements().get(2); // put field
                Instruction i3 = lambda.constructors().get(0).statements().get(3); // put field
                assertTrue(i1 instanceof Ins.PutField);
                assertTrue(i2 instanceof Ins.PutField);
                assertTrue(i3 instanceof Ins.PutField);

                assertEquals("apply", lambda.methods().get(0).name());
                assertEquals(1, lambda.methods().get(0).getParameters().size());
                Instruction i0 = lambda.methods().get(0).statements().get(0); // tstore new LinkedList(local)
                i1 = lambda.methods().get(0).statements().get(1); // invoke virtual (add x)
                i2 = lambda.methods().get(0).statements().get(2); // invoke virtual (add 0,o)
                i3 = lambda.methods().get(0).statements().get(3); // return(invoke virtual (tload))

                assertTrue(i0 instanceof Ins.TStore);
                assertTrue(i1 instanceof Ins.InvokeVirtual);
                assertTrue(i2 instanceof Ins.InvokeVirtual);
                assertTrue(i3 instanceof Ins.TReturn);
        }

        @Test
        public void testType() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("test", "" +
                        "# test\n" +
                        "class A\n" +
                        "    o=type A");
                Set<STypeDef> set = parse(map);
                assertEquals(1, set.size());

                Iterator<STypeDef> it = set.iterator();

                SClassDef classDef = (SClassDef) it.next();

                Value v = ((Ins.PutField) ((ValuePack) classDef.constructors().get(0).statements().get(1)).instructions().get(0)).value();
                assertTrue(v instanceof Ins.GetClass);
                assertEquals(classDef, ((Ins.GetClass) v).targetType());
        }
}
