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
import lt.compiler.util.Flags;
import lt.compiler.util.LocalVariables;
import lt.dependencies.asm.*;
import lt.lang.Pointer;

import java.util.*;

/**
 * The code generator, generate byte code from STypeDef.
 */
public class CodeGenerator {
        private final Set<STypeDef> types;
        private final Map<String, STypeDef> typeDefMap;
        private static final int VERSION = Opcodes.V1_6;

        /**
         * create the code generator with types to generate
         *
         * @param types      types
         * @param typeDefMap a map of type name to its representing object
         */
        public CodeGenerator(Set<STypeDef> types, Map<String, STypeDef> typeDefMap) {
                this.types = types;
                this.typeDefMap = typeDefMap;
        }

        /**
         * visit line number attribute
         *
         * @param methodVisitor method visitor
         * @param lineCol       the line col (can be null)
         * @param label         the label to visit
         */
        private void VisitLineNumber(MethodVisitor methodVisitor, LineCol lineCol, Label label) {
                if (lineCol == null || lineCol.line <= 0) return;
                methodVisitor.visitLineNumber(lineCol.line, label);
        }

        /**
         * generate modifiers
         *
         * @param modifiers modifiers
         * @return an integer representing modifiers
         */
        private int acc(List<SModifier> modifiers) {
                int acc = 0;
                for (SModifier m : modifiers) {
                        acc |= m.flag;
                }
                return acc;
        }

        /**
         * get type descriptor of the given type.<br>
         * e.g. <code>Ljava/lang/Object;</code>
         *
         * @param type type
         * @return type descriptor
         */
        private String typeToDesc(STypeDef type) {
                StringBuilder sb = new StringBuilder();
                if (type instanceof PrimitiveTypeDef) {
                        if (type.equals(IntTypeDef.get())) sb.append("I");
                        else if (type.equals(LongTypeDef.get())) sb.append("J");
                        else if (type.equals(ShortTypeDef.get())) sb.append("S");
                        else if (type.equals(ByteTypeDef.get())) sb.append("B");
                        else if (type.equals(BoolTypeDef.get())) sb.append("Z");
                        else if (type.equals(FloatTypeDef.get())) sb.append("F");
                        else if (type.equals(DoubleTypeDef.get())) sb.append("D");
                        else if (type.equals(CharTypeDef.get())) sb.append("C");
                        else throw new LtBug("unknown primitive: " + type);
                } else if (type instanceof SArrayTypeDef) {
                        SArrayTypeDef arr = (SArrayTypeDef) type;
                        for (int i = 0; i < arr.dimension(); ++i) sb.append("[");
                        sb.append(typeToDesc(arr.type()));
                } else if (type.equals(VoidType.get())) {
                        sb.append("V");
                } else {
                        // object L...;
                        sb.append("L").append(typeToInternalName(type)).append(";");
                }
                return sb.toString();
        }

        /**
         * get internal name of a type.<br>
         * e.g. <code>java/lang/Object</code>
         *
         * @param type type
         * @return internal name
         */
        private String typeToInternalName(STypeDef type) {
                if (type.equals(NullTypeDef.get())) return "java/lang/Object";
                if (type instanceof SArrayTypeDef) {
                        return typeToDesc(type);
                } else return type.fullName().replace(".", "/");
        }

        /**
         * get method descriptor.<br>
         * e.g. <code>(II)Ljava/lang/Runnable;</code>
         *
         * @param returnType return type
         * @param parameters parameter types
         * @return method descriptor
         */
        private String methodDesc(STypeDef returnType, List<STypeDef> parameters) {
                StringBuilder desc = new StringBuilder();
                desc.append("(");
                for (STypeDef t : parameters) {
                        desc.append(typeToDesc(t));
                }
                desc.append(")").append(typeToDesc(returnType));
                return desc.toString();
        }

        private String methodDescWithParameters(STypeDef returnType, List<SParameter> parameters) {
                List<STypeDef> types = new ArrayList<STypeDef>();
                for (SParameter p : parameters) {
                        if (p.isCapture() && !p.isUsed()) {
                                // ignore captured but not used params
                                continue;
                        }
                        types.add(p.type());
                }
                return methodDesc(returnType, types);
        }

        /**
         * start the generating process.
         *
         * @return Map&lt;FileName, byte[]&gt;
         */
        public Map<String, byte[]> generate() {
                Map<String, byte[]> result = new HashMap<String, byte[]>();
                for (STypeDef type : types) {
                        ClassWriter classWriter = new SClassWriter(ClassWriter.COMPUTE_FRAMES, typeDefMap);

                        List<SModifier> modifiers;                // modifier
                        List<Instruction> staticIns;              // <clinit>
                        List<ExceptionTable> exceptionTables;     // exception table for <clinit>
                        InvokableMeta staticMeta; // meta
                        List<SConstructorDef> constructors = null;// constructor
                        List<SFieldDef> fields;                   // field
                        List<SMethodDef> methods;                 // method
                        List<SAnno> annos = type.annos();         // annotations
                        SClassDef superClass = null;              // super class
                        List<SInterfaceDef> superInterfaces;      // super interface
                        String fileName = type.line_col().fileName; // file name

                        classWriter.visitSource(fileName, fileName);

                        if (type instanceof SClassDef) {
                                modifiers = ((SClassDef) type).modifiers();
                                staticIns = ((SClassDef) type).staticStatements();
                                exceptionTables = ((SClassDef) type).staticExceptionTable();
                                staticMeta = ((SClassDef) type).staticMeta();
                                constructors = ((SClassDef) type).constructors();
                                fields = ((SClassDef) type).fields();
                                methods = ((SClassDef) type).methods();
                                superInterfaces = ((SClassDef) type).superInterfaces();
                                superClass = ((SClassDef) type).parent();
                        } else if (type instanceof SInterfaceDef) {
                                modifiers = ((SInterfaceDef) type).modifiers();
                                staticIns = ((SInterfaceDef) type).staticStatements();
                                exceptionTables = ((SInterfaceDef) type).staticExceptionTable();
                                staticMeta = ((SInterfaceDef) type).staticMeta();
                                fields = ((SInterfaceDef) type).fields();
                                methods = ((SInterfaceDef) type).methods();
                                superInterfaces = ((SInterfaceDef) type).superInterfaces();
                        } else {
                                // generate annotation
                                generateAnnotation(classWriter, (SAnnoDef) type);
                                classWriter.visitEnd();
                                result.put(type.fullName(), classWriter.toByteArray());
                                continue;
                        }

                        String[] interfaces = new String[superInterfaces.size()];
                        for (int i = 0; i < interfaces.length; ++i) {
                                interfaces[i] = typeToInternalName(superInterfaces.get(i));
                        }

                        classWriter.visit(VERSION, acc(modifiers) | (type instanceof SClassDef ? 0 : Opcodes.ACC_INTERFACE),
                                typeToInternalName(type), null, superClass == null ? "java/lang/Object" : typeToInternalName(superClass), interfaces);

                        // annotations
                        for (SAnno anno : annos) {
                                AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }

                        buildStatic(classWriter, staticIns, exceptionTables, staticMeta);
                        buildField(classWriter, fields);
                        if (constructors != null) {
                                buildConstructor(classWriter, constructors);
                        }
                        buildMethod(classWriter, methods);
                        classWriter.visitEnd();

                        result.put(type.fullName(), classWriter.toByteArray());
                }
                return result;
        }

        private void generateAnnotation(ClassWriter classWriter, SAnnoDef sAnnoDef) {
                classWriter.visit(VERSION,
                        Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC,
                        typeToInternalName(sAnnoDef), null, "java/lang/Object",
                        new String[]{"java/lang/annotation/Annotation"});

                // annontations
                for (SAnno anno : sAnnoDef.annos()) {
                        AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(typeToDesc(anno.type()),
                                annotationIsVisible(anno));
                        buildAnnotation(annotationVisitor, anno);
                }

                // annotation fields (which are generated as methods on jvm)
                for (SAnnoField f : sAnnoDef.annoFields()) {
                        int acc = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;
                        MethodVisitor methodVisitor = classWriter.visitMethod(
                                acc, f.name(), methodDesc(f.type(), Collections.<STypeDef>emptyList()),
                                null, new String[0]
                        );
                        if (f.defaultValue() != null) {
                                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotationDefault();
                                buildAnnotationValue(annotationVisitor, f.name(), f.defaultValue());
                        }
                        methodVisitor.visitEnd();
                }
        }

        /**
         * build new<.br>
         * <br>
         * <code>
         * new TYPE<br>
         * dup<br>
         * InvokeSpecial
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param aNew          Ins.New
         */
        private void buildNew(MethodVisitor methodVisitor, CodeInfo info, Ins.New aNew) {
                methodVisitor.visitTypeInsn(Opcodes.NEW, typeToInternalName(aNew.type()));
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);
                for (Value v : aNew.args()) {
                        buildValueAccess(methodVisitor, info, v, true);
                }

                Label label = new Label();
                methodVisitor.visitLabel(label);

                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        typeToInternalName(aNew.type()),
                        "<init>",
                        methodDescWithParameters(
                                VoidType.get(),
                                aNew.constructor().getParameters()),
                        false);
                info.pop(1 + aNew.args().size());

                VisitLineNumber(methodVisitor, aNew.line_col(), label);
        }

        /**
         * build cast.<br>
         * <br>
         * <code>
         * buildValueAccess ------- original value<br>
         * castIns
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param cast          Ins.Cast
         */
        private void buildCast(MethodVisitor methodVisitor, CodeInfo info, Ins.Cast cast) {
                buildValueAccess(methodVisitor, info, cast.value(), true);

                Label label = new Label();
                methodVisitor.visitLabel(label);

                methodVisitor.visitInsn(cast.castMode());
                if (cast.castMode() == Ins.Cast.CAST_FLOAT_TO_DOUBLE
                        || cast.castMode() == Ins.Cast.CAST_INT_TO_DOUBLE
                        || cast.castMode() == Ins.Cast.CAST_LONG_TO_DOUBLE) {
                        info.pop(1);
                        info.push(CodeInfo.Size._2);
                } else {
                        info.pop(1);
                        info.push(CodeInfo.Size._1);
                }

                VisitLineNumber(methodVisitor, cast.line_col(), label);
        }

        /**
         * build two variable operation.<br>
         * <br>
         * <code>
         * buildValueAccess ------ a<br>
         * buildValueAccess ------ b<br>
         * twoVarOpIns ----------- operator
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param twoVarOp      Ins.TwoVarOp
         */
        private void buildTwoVarOp(MethodVisitor methodVisitor, CodeInfo info, Ins.TwoVarOp twoVarOp) {
                buildValueAccess(methodVisitor, info, twoVarOp.a(), true);
                buildValueAccess(methodVisitor, info, twoVarOp.b(), true);
                methodVisitor.visitInsn(twoVarOp.op());
                info.pop(2);
                if (twoVarOp.op() == Ins.TwoVarOp.Dadd
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Ddiv
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Dmul
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Drem
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Dsub
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Ladd
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Land
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Ldiv
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lmul
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lor
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lrem
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lshl
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lshr
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lsub
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lushr
                        ||
                        twoVarOp.op() == Ins.TwoVarOp.Lxor)

                        info.push(CodeInfo.Size._2);
                else info.push(CodeInfo.Size._1);
        }

        /**
         * build logicAND.<br>
         * <br>
         * <code>
         * buildValueAccess<br>
         * IfEq flag ----------- false<br>
         * buildValueAccess<br>
         * IfEq flag ----------- false<br>
         * IConst_1<br>
         * goto nop<br>
         * flag: IConst_0<br>
         * nop
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param logicAnd      Ins.LogicAnd
         */
        private void buildLogicAnd(MethodVisitor methodVisitor, CodeInfo info, Ins.LogicAnd logicAnd) {
                /*
                 * if eq b1 goto flag (if b1==false goto flag)
                 * if eq b2 goto flag (if b2==false goto flag)
                 * true               (push true into stack)
                 * goto nop           (goto nop, skip `else` branch)
                 * flag: false        (flag: push false into stack)
                 * nop                (nop)
                 */
                // b1
                buildValueAccess(methodVisitor, info, logicAnd.b1(), true);
                // if eq goto flag
                Label flag = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, flag);
                info.pop(1);
                // b2
                buildValueAccess(methodVisitor, info, logicAnd.b2(), true);
                // if eq goto flag
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, flag);
                info.pop(1);
                // true
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                // goto nop
                Label nop = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, nop);
                // flag: false
                methodVisitor.visitLabel(flag);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                // nop
                methodVisitor.visitLabel(nop);
                methodVisitor.visitInsn(Opcodes.NOP);

                info.push(CodeInfo.Size._1); // push true or false into stack
        }

        /**
         * eval `left` and cast to bool
         * check if it's true
         * if true then return the `left` value
         * else ignore the `left`
         * do eval and return `right`
         * <p>
         * <p>
         * v1 (stack is [v1])
         * dup ([v1 v1]
         * castToBool ([v1 I])
         * if ne goto nop ([v1]) // I is 1
         * pop ([])
         * v2 ([v2])
         * nop (v1/v2)
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param logicOr       Ins.LogicOr
         */
        private void buildLogicOr(MethodVisitor methodVisitor, CodeInfo info, Ins.LogicOr logicOr) {
                buildValueAccess(methodVisitor, info, logicOr.v1(), true); // [v1]
                methodVisitor.visitInsn(Opcodes.DUP); // dup [v1 v1]
                info.push(CodeInfo.Size._1);
                // invoke castToBool
                SMethodDef castToBool = logicOr.getCastToBool();
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        typeToInternalName(castToBool.declaringType()),
                        castToBool.name(),
                        methodDescWithParameters(
                                castToBool.getReturnType(),
                                castToBool.getParameters()),
                        false);
                info.pop(1);
                info.push(CodeInfo.Size._1);
                // [v1 I]
                // if ne goto flag
                Label flag = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNE, flag); // goto flag(nop)
                info.pop(1);
                // pop
                methodVisitor.visitInsn(Opcodes.POP); // [(empty stack)]
                info.pop(1);
                // v2
                buildValueAccess(methodVisitor, info, logicOr.v2(), true); // v2
                // flag: nop
                methodVisitor.visitLabel(flag);
                methodVisitor.visitInsn(Opcodes.NOP);
        }

        /**
         * build TALoad.<br>
         * <br>
         * <code>
         * buildValueAccess<br>
         * buildValueAccess -------- the array<br>
         * buildValueAccess -------- the index<br>
         * TALoad
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param TALoad        Ins.TALoad
         */
        private void buildTALoad(MethodVisitor methodVisitor, CodeInfo info, Ins.TALoad TALoad) {
                buildValueAccess(methodVisitor, info, TALoad.arr(), true);
                buildValueAccess(methodVisitor, info, TALoad.index(), true);

                Label label = new Label();
                methodVisitor.visitLabel(label);

                methodVisitor.visitInsn(TALoad.mode());
                info.pop(2);
                info.push(CodeInfo.Size._1);

                VisitLineNumber(methodVisitor, TALoad.line_col(), label);
        }

        /**
         * build OneVarOp.<br>
         * <br>
         * <code>
         * buildValueAccess ------ the value<br>
         * OneVarOp
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param oneVarOp      Ins.OneVarOp
         */
        private void buildOneVarOp(MethodVisitor methodVisitor, CodeInfo info, Ins.OneVarOp oneVarOp) {
                buildValueAccess(methodVisitor, info, oneVarOp.value(), true);
                methodVisitor.visitInsn(oneVarOp.op());

                if (oneVarOp.op() == Ins.OneVarOp.Dneg
                        || oneVarOp.op() == Ins.OneVarOp.Lneg) {
                        info.pop(1);
                        info.push(CodeInfo.Size._2);
                } else {
                        info.pop(1);
                        info.push(CodeInfo.Size._1);
                }
        }

        /**
         * build NewArray.<br>
         * <br>
         * <code>
         * buildPrimitive ------ array length<br>
         * NewArray<br>
         * foreach v in initValues<br>
         * &nbsp;&nbsp;DUP<br>
         * &nbsp;&nbsp;Ldc index<br>
         * &nbsp;&nbsp;buildValueAccess ---- the value to assign in<br>
         * &nbsp;&nbsp;TAStore
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param newArray      Ins.NewArray
         */
        private void buildNewArray(MethodVisitor methodVisitor, CodeInfo info, Ins.NewArray newArray) {
                buildPrimitive(methodVisitor, info, newArray.count());
                methodVisitor.visitIntInsn(Opcodes.NEWARRAY, newArray.mode());
                // pop 1 and push 1
                int i = 0;
                for (Value v : newArray.initValues()) {
                        methodVisitor.visitInsn(Opcodes.DUP); // array ref
                        info.push(CodeInfo.Size._1);
                        methodVisitor.visitLdcInsn(i); // index
                        info.push(CodeInfo.Size._1);
                        buildValueAccess(methodVisitor, info, v, true); // value

                        methodVisitor.visitInsn(newArray.storeMode());
                        info.pop(3);
                        ++i;
                }
                // stack have one element (the array)
        }

        /**
         * build ANewArray.<br>
         * <br>
         * <code>
         * buildPrimitive ---- array length<br>
         * ANewArray Type<br>
         * foreach v in initValues<br>
         * &nbsp;&nbsp;DUP<br>
         * &nbsp;&nbsp;Ldc ---- index<br>
         * &nbsp;&nbsp;buildValueAccess ---- the value to set<br>
         * &nbsp;&nbsp;AAStore
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param aNewArray     Ins.ANewArray
         */
        private void buildANewArray(MethodVisitor methodVisitor, CodeInfo info, Ins.ANewArray aNewArray) {
                buildPrimitive(methodVisitor, info, aNewArray.count());
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, typeToInternalName(aNewArray.componentType()));
                // pop 1 and push 1
                int i = 0;
                for (Value v : aNewArray.initValues()) {
                        methodVisitor.visitInsn(Opcodes.DUP); // array ref
                        info.push(CodeInfo.Size._1);
                        methodVisitor.visitLdcInsn(i); // index
                        info.push(CodeInfo.Size._1);
                        buildValueAccess(methodVisitor, info, v, true); // value

                        methodVisitor.visitInsn(Opcodes.AASTORE);
                        info.pop(3);
                        ++i;
                }
                // stack have one element (the array)
        }

        /**
         * build NewList.<br>
         * <br>
         * <code>
         * NEW Type<br>
         * DUP<br>
         * InvokeSpecial init<br>
         * foreach v in initValues<br>
         * &nbsp;&nbsp;DUP<br>
         * &nbsp;&nbsp;buildValueAccess ----- value to add<br>
         * &nbsp;&nbsp;InvokeVirtual add(Object)
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param newList       Ins.NewList
         */
        private void buildNewList(MethodVisitor methodVisitor, CodeInfo info, Ins.NewList newList) {
                // newList is LinkedList
                methodVisitor.visitTypeInsn(Opcodes.NEW, typeToInternalName(newList.type()));
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeToInternalName(newList.type()), "<init>", "()V", false);
                info.pop(1);

                for (Value v : newList.initValues()) {
                        methodVisitor.visitInsn(Opcodes.DUP); // list ref
                        info.push(CodeInfo.Size._1);
                        buildValueAccess(methodVisitor, info, v, true); // arg
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                        info.pop(2);
                        info.push(CodeInfo.Size._1); // the add result (boolean add(Object))
                        methodVisitor.visitInsn(Opcodes.POP);
                        info.pop(1);
                }
                // stack have one element (the list)
        }

        /**
         * build NewMap.<br>
         * <br>
         * <code>
         * New Type<br>
         * DUP<br>
         * InvokeSpecial init<br>
         * foreach k,v in initValues<br>
         * &nbsp;&nbsp;DUP<br>
         * &nbsp;&nbsp;buildValueAccess ---- key<br>
         * &nbsp;&nbsp;buildValueAccess ---- value<br>
         * &nbsp;&nbsp;InvokeVirtual ---- put(Object,Object)
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param newMap        Ins.NewMap
         */
        private void buildNewMap(MethodVisitor methodVisitor, CodeInfo info, Ins.NewMap newMap) {
                methodVisitor.visitTypeInsn(Opcodes.NEW, typeToInternalName(newMap.type()));
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);

                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        typeToInternalName(newMap.type()),
                        "<init>",
                        "()V", false);
                info.pop(1);

                for (Map.Entry<Value, Value> entry : newMap.initValues().entrySet()) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        info.push(CodeInfo.Size._1);
                        buildValueAccess(methodVisitor, info, entry.getKey(), true);
                        buildValueAccess(methodVisitor, info, entry.getValue(), true);

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                "java/util/Map",
                                "put",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                        info.pop(3);
                        info.push(CodeInfo.Size._1); // the put result (Object put(Object,Object))
                        methodVisitor.visitInsn(Opcodes.POP);
                        info.pop(1);
                }
                // stack have one element (the map)
        }

        /**
         * build value pack
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param value         the value pack
         * @param requireValue  require a value
         */
        private void buildValuePack(MethodVisitor methodVisitor, CodeInfo info, ValuePack value, boolean requireValue) {
                int depth = info.getCurrentStackDepth();
                List<Instruction> instructions = value.instructions();
                for (int i = 0; i < instructions.size() - 1; ++i) {
                        buildOneIns(methodVisitor, info, instructions.get(i), true);

                        if (value.autoPop()) {
                                while (info.getCurrentStackDepth() != depth) {
                                        if (info.peekSize() == CodeInfo.Size._1) {
                                                methodVisitor.visitInsn(Opcodes.POP);
                                        } else {
                                                methodVisitor.visitInsn(Opcodes.POP2);
                                        }
                                        info.pop(1);
                                }
                        }
                }

                Instruction ins = instructions.get(instructions.size() - 1);
                boolean buildLastStmt;
                if (!value.autoPop()) {
                        // not auto pop
                        buildLastStmt = true;
                } else {
                        // auto pop
                        if (requireValue) {
                                // require a value
                                buildLastStmt = true;
                        } else {
                                // doesn't require a value
                                if (ins instanceof ReadOnly) {
                                        // read only
                                        buildLastStmt = false;
                                } else {
                                        // not read only
                                        // check LtRuntime.getField and Unit.get
                                        if (ins instanceof Ins.InvokeStatic) {
                                                Ins.InvokeStatic is = (Ins.InvokeStatic) ins;
                                                SMethodDef theMethod = (SMethodDef) is.invokable();
                                                if (
                                                        theMethod.name().equals("getField")
                                                                &&
                                                                theMethod.declaringType().fullName().equals("lt.runtime.LtRuntime")) {
                                                        // lt.runtime.LtRuntime.getField
                                                        buildLastStmt = false;
                                                } else if (
                                                        theMethod.name().equals("get")
                                                                &&
                                                                theMethod.declaringType().fullName().equals("lt.lang.Unit")) {
                                                        // lt.lang.Unit.get
                                                        buildLastStmt = false;
                                                } else {
                                                        buildLastStmt = true;
                                                }
                                        } else {
                                                buildLastStmt = true;
                                        }
                                }
                        }
                }

                if (buildLastStmt) {
                        buildOneIns(methodVisitor, info, ins, requireValue);
                }
        }

        private int calculateIndexForLocalVariable(LeftValue theVar, SemanticScope scope, CodeInfo info) {
                return LocalVariables.calculateIndexForLocalVariable(theVar, scope, info.isStatic());
        }

        /**
         * build Value.
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param value         the value to build
         * @param requireValue  requires a value when generating {@link ValuePack}
         * @see Value
         * @see lt.compiler.semantic.Ins.This
         * @see lt.compiler.semantic.Ins.GetStatic
         * @see lt.compiler.semantic.Ins.TLoad
         * @see StringConstantValue
         * @see PrimitiveValue
         * @see lt.compiler.semantic.Ins.Invoke
         * @see lt.compiler.semantic.Ins.New
         * @see lt.compiler.semantic.Ins.Cast
         * @see lt.compiler.semantic.Ins.TwoVarOp
         * @see ValuePack
         * @see lt.compiler.semantic.Ins.GetField
         * @see lt.compiler.semantic.Ins.LogicAnd
         * @see lt.compiler.semantic.Ins.LogicOr
         * @see lt.compiler.semantic.Ins.GetClass
         * @see lt.compiler.semantic.Ins.TALoad
         * @see NullValue
         * @see lt.compiler.semantic.Ins.ArrayLength
         * @see lt.compiler.semantic.Ins.OneVarOp
         * @see lt.compiler.semantic.Ins.NewArray
         * @see lt.compiler.semantic.Ins.ANewArray
         * @see lt.compiler.semantic.Ins.NewList
         * @see lt.compiler.semantic.Ins.NewMap
         * @see lt.compiler.semantic.Ins.CheckCast
         */
        private void buildValueAccess(MethodVisitor methodVisitor, CodeInfo info, Value value, boolean requireValue) {
                if (value instanceof Ins.This) {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof Ins.GetStatic) {
                        // getStatic
                        Ins.GetStatic getStatic = (Ins.GetStatic) value;

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                typeToInternalName(getStatic.field().declaringType()),
                                getStatic.field().name(), typeToDesc(getStatic.field().type()));

                        if (getStatic.field().type().equals(DoubleTypeDef.get())
                                || (getStatic.field().type().equals(LongTypeDef.get())))
                                info.push(CodeInfo.Size._2);
                        else info.push(CodeInfo.Size._1);

                        VisitLineNumber(methodVisitor, ((Ins.GetStatic) value).line_col(), label);
                } else if (value instanceof Ins.TLoad) {
                        // tLoad
                        Ins.TLoad tLoad = (Ins.TLoad) value;

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        int index = calculateIndexForLocalVariable(tLoad.value(), tLoad.getScope(), info);
                        methodVisitor.visitVarInsn(tLoad.mode(), index);
                        if (tLoad.mode() == Ins.TLoad.Dload || tLoad.mode() == Ins.TLoad.Lload)
                                info.push(CodeInfo.Size._2);
                        else info.push(CodeInfo.Size._1);

                        VisitLineNumber(methodVisitor, ((Ins.TLoad) value).line_col(), label);
                } else if (value instanceof StringConstantValue) {
                        methodVisitor.visitLdcInsn(((StringConstantValue) value).getStr());
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof PrimitiveValue) {
                        buildPrimitive(methodVisitor, info, (PrimitiveValue) value);
                } else if (value instanceof Ins.Invoke) {
                        buildInvoke(methodVisitor, info, (Ins.Invoke) value, requireValue);
                } else if (value instanceof Ins.New) {
                        buildNew(methodVisitor, info, (Ins.New) value);
                } else if (value instanceof Ins.Cast) {
                        buildCast(methodVisitor, info, (Ins.Cast) value);
                } else if (value instanceof Ins.TwoVarOp) {
                        buildTwoVarOp(methodVisitor, info, (Ins.TwoVarOp) value);
                } else if (value instanceof ValuePack) {
                        buildValuePack(methodVisitor, info, (ValuePack) value, requireValue);
                } else if (value instanceof Ins.GetField) {
                        buildGetField(methodVisitor, info, (Ins.GetField) value);
                } else if (value instanceof Ins.LogicAnd) {
                        buildLogicAnd(methodVisitor, info, (Ins.LogicAnd) value);
                } else if (value instanceof Ins.LogicOr) {
                        buildLogicOr(methodVisitor, info, (Ins.LogicOr) value);
                } else if (value instanceof Ins.GetClass) {
                        STypeDef targetType = ((Ins.GetClass) value).targetType();

                        if (targetType.equals(VoidType.get())) {
                                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
                        } else if (targetType instanceof PrimitiveTypeDef) {
                                String TYPE = "TYPE";
                                String CLASS = "Ljava/lang/Class;";
                                String OWNER;

                                if (targetType.equals(IntTypeDef.get())) {
                                        OWNER = "java/lang/Integer";
                                } else if (targetType.equals(ShortTypeDef.get())) {
                                        OWNER = "java/lang/Short";
                                } else if (targetType.equals(ByteTypeDef.get())) {
                                        OWNER = "java/lang/Byte";
                                } else if (targetType.equals(BoolTypeDef.get())) {
                                        OWNER = "java/lang/Boolean";
                                } else if (targetType.equals(CharTypeDef.get())) {
                                        OWNER = "java/lang/Character";
                                } else if (targetType.equals(LongTypeDef.get())) {
                                        OWNER = "java/lang/Long";
                                } else if (targetType.equals(FloatTypeDef.get())) {
                                        OWNER = "java/lang/Float";
                                } else if (targetType.equals(DoubleTypeDef.get())) {
                                        OWNER = "java/lang/Double";
                                } else throw new LtBug("unknown primitive type " + targetType);

                                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, OWNER, TYPE, CLASS);
                        } else {
                                methodVisitor.visitLdcInsn(Type.getObjectType(
                                        typeToInternalName(targetType)
                                ));
                        }
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof Ins.TALoad) {
                        buildTALoad(methodVisitor, info, (Ins.TALoad) value);
                } else if (value instanceof NullValue) {
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof Ins.ArrayLength) {
                        buildValueAccess(methodVisitor, info, ((Ins.ArrayLength) value).arrayValue(), true);
                        methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
                        // pop 1 and push 1
                } else if (value instanceof Ins.OneVarOp) {
                        buildOneVarOp(methodVisitor, info, (Ins.OneVarOp) value);
                } else if (value instanceof Ins.NewArray) {
                        buildNewArray(methodVisitor, info, (Ins.NewArray) value);
                } else if (value instanceof Ins.ANewArray) {
                        buildANewArray(methodVisitor, info, (Ins.ANewArray) value);
                } else if (value instanceof Ins.NewList) {
                        buildNewList(methodVisitor, info, (Ins.NewList) value);
                } else if (value instanceof Ins.NewMap) {
                        buildNewMap(methodVisitor, info, (Ins.NewMap) value);
                } else if (value instanceof Ins.CheckCast) {
                        buildValueAccess(methodVisitor, info, ((Ins.CheckCast) value).theValueToCheck(), true);

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, typeToInternalName(value.type()));
                        info.pop(1);
                        info.push(CodeInfo.Size._1);

                        VisitLineNumber(methodVisitor, ((Ins.CheckCast) value).line_col(), label);

                } else if (value instanceof Ins.InstanceOf) {
                        buildValueAccess(methodVisitor, info, ((Ins.InstanceOf) value).object(), true);
                        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF,
                                typeToInternalName(((Ins.InstanceOf) value).aClass().targetType()));
                        info.pop(1);
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof ValueAnotherType) {
                        buildValueAccess(methodVisitor, info, ((ValueAnotherType) value).value(), requireValue);
                } else if (value instanceof Ins.PointerGetCastHelper) {
                        buildPointerCastHelper(methodVisitor, info, (Ins.PointerGetCastHelper) value, requireValue);
                } else {
                        throw new LtBug("unknown value " + value);
                }
        }

        private void buildPointerCastHelper(MethodVisitor methodVisitor, CodeInfo info, Ins.PointerGetCastHelper h, boolean requireValue) {
                if (!requireValue) {
                        // ignore the invocation if not requiring a value
                        return;
                }

                if (canOptimizePointerRetrieving(h.before(), info)) {
                        buildValueAccess(methodVisitor, info, h.before(), true);
                } else {
                        buildValueAccess(methodVisitor, info, h.after(), true);
                }
        }

        /**
         * build Primitive.
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param p             primitive value
         * @see PrimitiveValue
         * @see IntValue
         * @see ShortValue
         * @see ByteValue
         * @see BoolValue
         * @see CharValue
         * @see LongValue
         * @see FloatValue
         * @see DoubleValue
         */
        private void buildPrimitive(MethodVisitor methodVisitor, CodeInfo info, PrimitiveValue p) {
                if (p instanceof IntValue) {
                        methodVisitor.visitLdcInsn(((IntValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof ShortValue) {
                        methodVisitor.visitLdcInsn(((ShortValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof ByteValue) {
                        methodVisitor.visitLdcInsn(((ByteValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof BoolValue) {
                        methodVisitor.visitLdcInsn(((BoolValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof CharValue) {
                        methodVisitor.visitLdcInsn(((CharValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof LongValue) {
                        methodVisitor.visitLdcInsn(((LongValue) p).getValue());
                        info.push(CodeInfo.Size._2);
                } else if (p instanceof FloatValue) {
                        methodVisitor.visitLdcInsn(((FloatValue) p).getValue());
                        info.push(CodeInfo.Size._1);
                } else if (p instanceof DoubleValue) {
                        methodVisitor.visitLdcInsn(((DoubleValue) p).getValue());
                        info.push(CodeInfo.Size._2);
                } else throw new LtBug("unknown primitive value " + p);
        }

        /**
         * build unit if invokable return a void type
         *
         * @param invokable the invokable object
         * @param info      code info
         */
        private void buildUnitWhenInvokeVoid(SInvokable invokable, CodeInfo info) {
                if (!invokable.getReturnType().equals(VoidType.get())) {
                        STypeDef typeDef = invokable.getReturnType();
                        if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                info.push(CodeInfo.Size._2);
                        else info.push(CodeInfo.Size._1);
                }
        }

        private void _buildOptimizedPointerTLoad(MethodVisitor methodVisitor, CodeInfo info, int index, STypeDef type) {
                if (type instanceof PrimitiveTypeDef) {
                        if (type.equals(DoubleTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.DLOAD, index);
                                info.push(CodeInfo.Size._2);
                        } else if (type.equals(LongTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.LLOAD, index);
                                info.push(CodeInfo.Size._2);
                        } else if (type.equals(FloatTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.FLOAD, index);
                                info.push(CodeInfo.Size._1);
                        } else {
                                methodVisitor.visitVarInsn(Opcodes.ILOAD, index);
                                info.push(CodeInfo.Size._1);
                        }
                } else {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, index);
                        info.push(CodeInfo.Size._1);
                }
        }

        private void _buildUnbox(MethodVisitor methodVisitor, CodeInfo info, Value v) {
                if (v.type() instanceof PrimitiveTypeDef) {
                        buildValueAccess(methodVisitor, info, v, true);
                } else if (v instanceof Ins.InvokeStatic && ((Ins.InvokeStatic) v).invokable() instanceof SMethodDef &&
                        ((SMethodDef) ((Ins.InvokeStatic) v).invokable()).name().equals("valueOf") &&
                        Arrays.asList(
                                "java.lang.Double",
                                "java.lang.Long",
                                "java.lang.Float",
                                "java.lang.Integer",
                                "java.lang.Short",
                                "java.lang.Byte",
                                "java.lang.Character",
                                "java.lang.Boolean"
                        ).contains(((Ins.InvokeStatic) v).invokable().declaringType().fullName())) {

                        Value primitiveValue = ((Ins.InvokeStatic) v).arguments().get(0);
                        buildValueAccess(methodVisitor, info, primitiveValue, true);
                } else {
                        buildValueAccess(methodVisitor, info, v, true);
                        if (v.type().fullName().equals("java.lang.Double")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Double", "doubleValue", "()D", false);
                                info.push(CodeInfo.Size._2);
                        } else if (v.type().fullName().equals("java.lang.Long")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Long", "longValue", "()L", false);
                                info.push(CodeInfo.Size._2);
                        } else if (v.type().fullName().equals("java.lang.Float")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Float", "floatValue", "()F", false);
                                info.push(CodeInfo.Size._1);
                        } else if (v.type().fullName().equals("java.lang.Integer")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Integer", "intValue", "()I", false);
                                info.push(CodeInfo.Size._1);
                        } else if (v.type().fullName().equals("java.lang.Short")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Short", "shortValue", "()S", false);
                                info.push(CodeInfo.Size._1);
                        } else if (v.type().fullName().equals("java.lang.Byte")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Byte", "byteValue", "()B", false);
                                info.push(CodeInfo.Size._1);
                        } else if (v.type().fullName().equals("java.lang.Character")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Character", "charValue", "()C", false);
                                info.push(CodeInfo.Size._1);
                        } else if (v.type().fullName().equals("java.lang.Boolean")) {
                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                        "java/lang/Boolean", "booleanValue", "()Z", false);
                                info.push(CodeInfo.Size._1);
                        } else {
                                throw new LtBug("unknown boxing type " + v.type());
                        }
                }
        }

        private void _buildOptimizedPointerTStore(MethodVisitor methodVisitor, CodeInfo info, int index, STypeDef type, Value v) {
                if (type instanceof PrimitiveTypeDef) {
                        _buildUnbox(methodVisitor, info, v);
                        if (type.equals(DoubleTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.DSTORE, index);
                        } else if (type.equals(LongTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.LSTORE, index);
                        } else if (type.equals(FloatTypeDef.get())) {
                                methodVisitor.visitVarInsn(Opcodes.FSTORE, index);
                        } else {
                                methodVisitor.visitVarInsn(Opcodes.ISTORE, index);
                        }
                        info.pop(1);
                } else {
                        buildValueAccess(methodVisitor, info, v, true);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, index);
                        info.pop(1);
                }
        }

        private boolean canOptimizePointerRetrieving(Ins.InvokeVirtual invoke, CodeInfo info) {
                Ins.TLoad target = (Ins.TLoad) invoke.target();
                return !info.getMeta().pointerLocalVar.contains(target.value()) && target.type().fullName().equals(Pointer.class.getName());
        }

        private boolean isParameterWrappingPointer(LeftValue v) {
                return LocalVariables.isParameterWrappingPointer(v);
        }

        /**
         * build invoke.
         *
         * @param methodVisitor method visitor
         * @param info          info
         * @param invoke        Ins.Invoke
         * @param requireValue  requires a value
         * @see lt.compiler.semantic.Ins.Invoke
         * @see lt.compiler.semantic.Ins.InvokeSpecial
         * @see lt.compiler.semantic.Ins.InvokeVirtual
         * @see lt.compiler.semantic.Ins.InvokeStatic
         * @see lt.compiler.semantic.Ins.InvokeInterface
         */
        private void buildInvoke(MethodVisitor methodVisitor, CodeInfo info, Ins.Invoke invoke, boolean requireValue) {
                Label label = new Label();
                if (invoke instanceof Ins.InvokeSpecial) {
                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeSpecial) invoke).target(), true);

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v, true);
                        }

                        // invoke special
                        Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) invoke;
                        SInvokable invokable = invokeSpecial.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDescWithParameters(
                                        invokable.getReturnType(),
                                        invokable.getParameters());
                        } else if (invokable instanceof SConstructorDef) {
                                name = "<init>";
                                desc = methodDescWithParameters(VoidType.get(), invokable.getParameters());
                        } else throw new LtBug("cannot invoke special on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESPECIAL,
                                owner,
                                name, desc, false);
                        info.pop(1 + invoke.arguments().size());
                        buildUnitWhenInvokeVoid(invokable, info);

                } else if (invoke instanceof Ins.InvokeVirtual) {
                        if (Flags.match(((Ins.InvokeVirtual) invoke).flag, Flags.IS_POINTER_GET)) {
                                // pointer get
                                if (!requireValue) {
                                        return;
                                }
                                if (canOptimizePointerRetrieving((Ins.InvokeVirtual) invoke, info)) {
                                        Ins.TLoad target = (Ins.TLoad) ((Ins.InvokeVirtual) invoke).target();
                                        int index;
                                        if (isParameterWrappingPointer(target.value())) {
                                                index = calculateIndexForLocalVariable(((LocalVariable) target.value()).getWrappingParam(), target.getScope(), info);
                                        } else {
                                                index = calculateIndexForLocalVariable(target.value(), target.getScope(), info);
                                        }
                                        // simplify to tLoad
                                        _buildOptimizedPointerTLoad(methodVisitor, info, index, ((PointerType) target.type()).getPointingType());
                                        return;
                                }
                        } else if (Flags.match(((Ins.InvokeVirtual) invoke).flag, Flags.IS_POINTER_SET)) {
                                // pointer set

                                if (((Ins.InvokeVirtual) invoke).target() instanceof Ins.TLoad) {
                                        Ins.TLoad target = (Ins.TLoad) ((Ins.InvokeVirtual) invoke).target();
                                        if (!info.getMeta().pointerLocalVar.contains(target.value()) && target.type().fullName().equals(Pointer.class.getName())) {
                                                // simplify to tStore
                                                int index;
                                                if (isParameterWrappingPointer(target.value())) {
                                                        index = calculateIndexForLocalVariable(((LocalVariable) target.value()).getWrappingParam(), target.getScope(), info);
                                                } else {
                                                        index = calculateIndexForLocalVariable(target.value(), target.getScope(), info);
                                                }
                                                _buildOptimizedPointerTStore(methodVisitor, info, index, ((PointerType) target.type()).getPointingType(), invoke.arguments().get(0));
                                                return;
                                        }
                                } else {
                                        // else it should be New
                                        assert ((Ins.InvokeVirtual) invoke).target() instanceof Ins.New;
                                }
                        }

                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeVirtual) invoke).target(), true);

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v, true);
                        }

                        // invoke special
                        Ins.InvokeVirtual invokeSpecial = (Ins.InvokeVirtual) invoke;
                        SInvokable invokable = invokeSpecial.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDescWithParameters(
                                        invokable.getReturnType(),
                                        invokable.getParameters());
                        } else throw new LtBug("cannot invoke virtual on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                owner,
                                name, desc, false);
                        info.pop(1 + invoke.arguments().size());
                        buildUnitWhenInvokeVoid(invokable, info);

                } else if (invoke instanceof Ins.InvokeStatic) {
                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v, true);
                        }

                        // invoke special
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) invoke;
                        SInvokable invokable = invokeStatic.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDescWithParameters(
                                        invokable.getReturnType(),
                                        invokable.getParameters());
                        } else throw new LtBug("cannot invoke static on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                owner,
                                name, desc, false);
                        info.pop(invoke.arguments().size());
                        buildUnitWhenInvokeVoid(invokable, info);

                } else if (invoke instanceof Ins.InvokeInterface) {
                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeInterface) invoke).target(), true);

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v, true);
                        }

                        // invoke interface
                        Ins.InvokeInterface invokeInterface = (Ins.InvokeInterface) invoke;
                        SInvokable invokable = invokeInterface.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDescWithParameters(
                                        invokable.getReturnType(),
                                        invokable.getParameters());
                        } else throw new LtBug("cannot invoke interface on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                owner,
                                name, desc, true);
                        info.pop(1 + invoke.arguments().size());
                        buildUnitWhenInvokeVoid(invokable, info);

                } else if (invoke instanceof Ins.InvokeWithCapture) {
                        Ins.InvokeWithCapture ic = (Ins.InvokeWithCapture) invoke;
                        // push target object if not static
                        if (!ic.isStatic()) {
                                buildValueAccess(methodVisitor, info, ic.target(), true);
                        }
                        int argCount = 0;
                        // push captured args
                        for (int i = 0; i < ic.capturedArguments().size(); ++i) {
                                Value v = ic.capturedArguments().get(i);
                                SParameter param = ic.invokable().getParameters().get(i);
                                if (param.isCapture() && !param.isUsed()) {
                                        // ignore captured but not used values
                                        continue;
                                }
                                buildValueAccess(methodVisitor, info, v, true);
                                ++argCount;
                        }
                        // push args
                        for (Value v : ic.arguments()) {
                                buildValueAccess(methodVisitor, info, v, true);
                                ++argCount;
                        }
                        // invoke
                        SInvokable invokable = ic.invokable();
                        if (!(invokable instanceof SMethodDef)) {
                                throw new LtBug("invokable in InvokeWithCapture should not be " + invokable);
                        }
                        String name;
                        String desc;
                        name = ((SMethodDef) invokable).name();
                        desc = methodDescWithParameters(
                                invokable.getReturnType(),
                                invokable.getParameters()
                        );
                        String owner = typeToInternalName(invokable.declaringType());
                        methodVisitor.visitLabel(label);
                        if (ic.isStatic()) {
                                // invoke static
                                methodVisitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        owner,
                                        name, desc, false
                                );
                                info.pop(argCount);
                        } else if (invokable.modifiers().isEmpty()) {
                                // invoke virtual (package access)
                                methodVisitor.visitMethodInsn(
                                        Opcodes.INVOKEVIRTUAL,
                                        owner,
                                        name, desc, false
                                );
                                info.pop(argCount + 1);
                        } else {
                                // invoke special
                                methodVisitor.visitMethodInsn(
                                        Opcodes.INVOKESPECIAL,
                                        owner,
                                        name, desc, false
                                );
                                info.pop(argCount + 1);
                        }
                        buildUnitWhenInvokeVoid(invokable, info);

                } else throw new LtBug("unknown invoke type " + invoke);

                // line number
                VisitLineNumber(methodVisitor, invoke.line_col(), label);

                if (invoke.invokable().getReturnType().equals(VoidType.get()) && requireValue) {
                        // void methods
                        // push Unit into stack
                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "lt/lang/Unit",
                                "get",
                                "()Llt/lang/Unit;",
                                false);
                        info.push(CodeInfo.Size._1);
                }
        }

        /**
         * build Return.<br>
         * <br>
         * <code>
         * RETURN<br>
         * or<br>
         * buildValueAccess ---- the value to return<br>
         * TReturn
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          info
         * @param tReturn       Ins.TReturn
         */
        private void buildReturn(MethodVisitor methodVisitor, CodeInfo info, Ins.TReturn tReturn) {
                if (tReturn.returnIns() == Ins.TReturn.Return) {
                        methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                        buildValueAccess(methodVisitor, info, tReturn.value(), true);
                        methodVisitor.visitInsn(tReturn.returnIns());
                        info.pop(1);
                }
        }

        /**
         * build TStore.<br>
         * <br>
         * <code>
         * buildValueAccess ---- the value to store<br>
         * TStore index
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param tStore        Ins.TStore
         */
        private void buildTStore(MethodVisitor methodVisitor, CodeInfo info, Ins.TStore tStore) {
                if (tStore.flag == Flags.IS_POINTER_NEW) {
                        if (!info.getMeta().pointerLocalVar.contains(tStore.leftValue())) {
                                STypeDef pointerType = tStore.leftValue().type();
                                STypeDef type = ((PointerType) pointerType).getPointingType();
                                if (isParameterWrappingPointer(tStore.leftValue())) {
                                        // ignore the value
                                        return;
                                }
                                int index = calculateIndexForLocalVariable(tStore.leftValue(), tStore.getScope(), info);
                                Value newValue = tStore.newValue();
                                if (newValue instanceof Ins.New && newValue.type().fullName().equals(Pointer.class.getName())) {
                                        // constructing a new pointer object but not assigning any value
                                        return; // do nothing
                                } else if (newValue instanceof Ins.InvokeVirtual) {
                                        //                                        pointer.set(           ?            )
                                        Value valueToBuild = ((Ins.InvokeVirtual) tStore.newValue()).arguments().get(0);
                                        if (type instanceof PrimitiveTypeDef) {
                                                _buildUnbox(methodVisitor, info, valueToBuild);
                                                if (type instanceof DoubleTypeDef) {
                                                        methodVisitor.visitVarInsn(Opcodes.DSTORE, index);
                                                } else if (type instanceof LongTypeDef) {
                                                        methodVisitor.visitVarInsn(Opcodes.LSTORE, index);
                                                } else if (type instanceof FloatTypeDef) {
                                                        methodVisitor.visitVarInsn(Opcodes.FSTORE, index);
                                                } else {
                                                        methodVisitor.visitVarInsn(Opcodes.ISTORE, index);
                                                }
                                        } else {
                                                buildValueAccess(methodVisitor, info, valueToBuild, true);
                                                methodVisitor.visitVarInsn(Opcodes.ASTORE, index);
                                        }
                                        info.pop(1);
                                        info.registerLocal(index);
                                        return;
                                } else throw new LtBug("should not each here");
                        }
                }

                buildValueAccess(methodVisitor, info, tStore.newValue(), true);
                int index = calculateIndexForLocalVariable(tStore.leftValue(), tStore.getScope(), info);
                methodVisitor.visitVarInsn(tStore.mode(), index);
                info.pop(1);
                info.registerLocal(index);
        }

        /**
         * build PutField.<br>
         * <br>
         * <code>
         * buildValueAccess ---- the instance<br>
         * buildValueAccess ---- the value to be put<br>
         * PutField Field
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param putField      Ins.PutField
         */
        private void buildPutField(MethodVisitor methodVisitor, CodeInfo info, Ins.PutField putField) {
                buildValueAccess(methodVisitor, info, putField.obj(), true);
                buildValueAccess(methodVisitor, info, putField.value(), true);
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTFIELD,
                        typeToInternalName(putField.field().declaringType()),
                        putField.field().name(),
                        typeToDesc(putField.field().type()));
                info.pop(2);
        }

        /**
         * build GetField.<br>
         * <br>
         * <code>
         * buildValueAccess ---- the instance<br>
         * GetField Field
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param getField      Ins.GetField
         */
        private void buildGetField(MethodVisitor methodVisitor, CodeInfo info, Ins.GetField getField) {
                buildValueAccess(methodVisitor, info, getField.object(), true);

                Label label = new Label();
                methodVisitor.visitLabel(label);

                methodVisitor.visitFieldInsn(
                        Opcodes.GETFIELD,
                        typeToInternalName(getField.field().declaringType()),
                        getField.field().name(),
                        typeToDesc(getField.type()));
                info.pop(1);

                if (getField.field().type().equals(DoubleTypeDef.get())
                        ||
                        getField.field().type().equals(LongTypeDef.get())) {
                        info.push(CodeInfo.Size._2);
                } else info.push(CodeInfo.Size._1);

                VisitLineNumber(methodVisitor, getField.line_col(), label);
        }

        /**
         * build PutStatic.<br>
         * <br>
         * <code>
         * buildValueAccess ---- the value to be put<br>
         * PutStatic Field
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param putStatic     Ins.PutStatic
         */
        private void buildPutStatic(MethodVisitor methodVisitor, CodeInfo info, Ins.PutStatic putStatic) {
                buildValueAccess(methodVisitor, info, putStatic.value(), true);
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        typeToInternalName(putStatic.field().declaringType()),
                        putStatic.field().name(),
                        typeToDesc(putStatic.field().type()));
                info.pop(1);
        }

        /**
         * build TAStore.<br>
         * <br>
         * <code>
         * buildValueAccess ---- array<br>
         * buildValueAccess ---- index to store<br>
         * buildValueAccess ---- the value to store<br>
         * TAStore
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param TAStore       Ins.TAStore
         */
        private void buildTAStore(MethodVisitor methodVisitor, CodeInfo info, Ins.TAStore TAStore) {
                buildValueAccess(methodVisitor, info, TAStore.array(), true); // array
                buildValueAccess(methodVisitor, info, TAStore.index(), true); // index
                buildValueAccess(methodVisitor, info, TAStore.value(), true); // value

                methodVisitor.visitInsn(TAStore.mode());
                info.pop(3);
        }

        /**
         * build MonitorEnter.<br>
         * <br>
         * <code>
         * buildValueAccess ---- the object to be monitored<br>
         * DUP<br>
         * AStore index<br>
         * MonitorEnter
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param monitorEnter  Ins.MonitorEnter
         */
        private void buildMonitorEnter(MethodVisitor methodVisitor, CodeInfo info, Ins.MonitorEnter monitorEnter) {
                buildValueAccess(methodVisitor, info, monitorEnter.valueToMonitor(), true);
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, calculateIndexForLocalVariable(monitorEnter.leftValue(), monitorEnter.getScope(), info));
                info.pop(1);
                methodVisitor.visitInsn(Opcodes.MONITORENTER);
                info.pop(1);
        }

        /**
         * build MonitorExit.<br>
         * <br>
         * ALoad ---- the object that was monitored<br>
         * MonitorExit
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param monitorExit   Ins.MonitorExit
         */
        private void buildMonitorExit(MethodVisitor methodVisitor, CodeInfo info, Ins.MonitorExit monitorExit) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, calculateIndexForLocalVariable(monitorExit.enterInstruction().leftValue(), monitorExit.enterInstruction().getScope(), info));
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.MONITOREXIT);
                info.pop(1);
        }

        /**
         * build one Instruction.
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param ins           Instruction
         * @param requireValue  requires a value when generating {@link ValuePack}
         * @see Instruction
         * @see Value
         * @see lt.compiler.semantic.Ins.TReturn
         * @see lt.compiler.semantic.Ins.TStore
         * @see lt.compiler.semantic.Ins.PutField
         * @see lt.compiler.semantic.Ins.PutStatic
         * @see lt.compiler.semantic.Ins.IfNe
         * @see lt.compiler.semantic.Ins.IfEq
         * @see lt.compiler.semantic.Ins.IfNonNull
         * @see lt.compiler.semantic.Ins.IfACmpNe
         * @see lt.compiler.semantic.Ins.Goto
         * @see lt.compiler.semantic.Ins.Nop
         * @see lt.compiler.semantic.Ins.AThrow
         * @see lt.compiler.semantic.Ins.ExStore
         * @see lt.compiler.semantic.Ins.Pop
         * @see lt.compiler.semantic.Ins.TAStore
         * @see lt.compiler.semantic.Ins.MonitorEnter
         * @see lt.compiler.semantic.Ins.MonitorExit
         */
        private void buildOneIns(MethodVisitor methodVisitor, CodeInfo info, Instruction ins, boolean requireValue) {
                CodeInfo.Container container;
                if (info.insToLabel.containsKey(ins)) {
                        // fill label
                        container = info.insToLabel.get(ins);
                } else {
                        // add label
                        container = new CodeInfo.Container(new Label());
                        info.insToLabel.put(ins, container);
                }
                if (!container.isVisited) {
                        methodVisitor.visitLabel(container.label);
                        container.isVisited = true;
                }

                if (ins instanceof Value) {
                        buildValueAccess(methodVisitor, info, (Value) ins, requireValue);
                } else if (ins instanceof Ins.TReturn) {
                        buildReturn(methodVisitor, info, (Ins.TReturn) ins);
                } else if (ins instanceof Ins.TStore) {
                        buildTStore(methodVisitor, info, (Ins.TStore) ins);
                } else if (ins instanceof Ins.PutField) {
                        buildPutField(methodVisitor, info, (Ins.PutField) ins);
                } else if (ins instanceof Ins.PutStatic) {
                        buildPutStatic(methodVisitor, info, (Ins.PutStatic) ins);
                } else if (ins instanceof Ins.IfNe) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfNe) ins).condition(), true);
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfNe) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfNe) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfNe) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IFNE, l);
                        info.pop(1);
                } else if (ins instanceof Ins.IfEq) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfEq) ins).condition(), true);
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfEq) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfEq) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfEq) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, l);
                        info.pop(1);
                } else if (ins instanceof Ins.IfNonNull) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfNonNull) ins).object(), true);
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfNonNull) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfNonNull) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfNonNull) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IFNONNULL, l);
                        info.pop(1);
                } else if (ins instanceof Ins.IfNull) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfNull) ins).object(), true);
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfNull) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfNull) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfNull) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IFNULL, l);
                        info.pop(1);
                } else if (ins instanceof Ins.IfACmpNe) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfACmpNe) ins).value1(), true);
                        buildValueAccess(methodVisitor, info, ((Ins.IfACmpNe) ins).value2(), true);
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfACmpNe) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfACmpNe) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfACmpNe) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, l);
                        info.pop(2);
                } else if (ins instanceof Ins.Goto) {
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.Goto) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.Goto) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.Goto) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        // methodVisitor.visitLdcInsn(0);
                        // info.push(1);
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, l);
                        // info.pop(1);
                } else if (ins instanceof Ins.Nop) {
                        methodVisitor.visitInsn(Opcodes.NOP);
                } else if (ins instanceof Ins.AThrow) {
                        buildValueAccess(methodVisitor, info, ((Ins.AThrow) ins).exception(), true);

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitInsn(Opcodes.ATHROW);
                        info.pop(1);

                        VisitLineNumber(methodVisitor, ins.line_col(), label);

                } else if (ins instanceof Ins.ExStore) {
                        info.push(CodeInfo.Size._1);
                        int index = calculateIndexForLocalVariable(((Ins.ExStore) ins).leftValue(), ((Ins.ExStore) ins).getScope(), info);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, index);
                        info.registerLocal(index);
                        info.pop(1);
                } else if (ins instanceof Ins.Pop) {
                        methodVisitor.visitInsn(Opcodes.POP);
                        info.pop(1);
                } else if (ins instanceof Ins.TAStore) {
                        buildTAStore(methodVisitor, info, (Ins.TAStore) ins);
                } else if (ins instanceof Ins.MonitorEnter) {
                        buildMonitorEnter(methodVisitor, info, (Ins.MonitorEnter) ins);
                } else if (ins instanceof Ins.MonitorExit) {
                        buildMonitorExit(methodVisitor, info, (Ins.MonitorExit) ins);
                } else {
                        throw new LtBug("unknown ins " + ins);
                }
        }

        /**
         * build instructions for method/constructor/staticScope.<br>
         * <ol>
         * <li>build instructions</li>
         * <li>
         * add Return to the method ensures that the method would return normally<br>
         * for short/byte/bool/char => IConst_0 and IReturn<br>
         * for long => L_Const_0 and LReturn<br>
         * for float => FConst_0 and FReturn<br>
         * for double =>DConst_0 and DReturn<br>
         * for void => Return<br>
         * other => AConst_Null and AReturn
         * </li>
         * <li>build exception table</li>
         * </ol>
         *
         * @param methodVisitor  method visitor
         * @param info           method info
         * @param instructions   instructions to be parsed into jvm byte codes
         * @param exceptionTable exception tables that records exception info (start,end,handle,type)
         * @param returnType     method return type
         */
        private void buildInstructions(
                MethodVisitor methodVisitor,
                CodeInfo info,
                List<Instruction> instructions,
                List<ExceptionTable> exceptionTable,
                STypeDef returnType) {

                methodVisitor.visitCode();
                for (Instruction ins : instructions) {
                        buildOneIns(methodVisitor, info, ins, false);
                        while (info.getCurrentStackDepth() != 0) {
                                CodeInfo.Size peek = info.peekSize();
                                if (peek == CodeInfo.Size._1) methodVisitor.visitInsn(Opcodes.POP);
                                else methodVisitor.visitInsn(Opcodes.POP2);
                                info.pop(1);
                        }
                }
                // guarantee the method can return
                if (returnType.equals(VoidType.get())) {
                        methodVisitor.visitInsn(Opcodes.RETURN);
                } else if (returnType instanceof PrimitiveTypeDef) {
                        if (returnType.equals(IntTypeDef.get())
                                || returnType.equals(ShortTypeDef.get())
                                || returnType.equals(ByteTypeDef.get())
                                || returnType.equals(BoolTypeDef.get())
                                || returnType.equals(CharTypeDef.get())) {
                                methodVisitor.visitInsn(Opcodes.ICONST_0);
                                info.push(CodeInfo.Size._1);
                                methodVisitor.visitInsn(Opcodes.IRETURN);
                        } else if (returnType.equals(LongTypeDef.get())) {
                                methodVisitor.visitInsn(Opcodes.LCONST_0);
                                info.push(CodeInfo.Size._2);
                                methodVisitor.visitInsn(Opcodes.LRETURN);
                        } else if (returnType.equals(FloatTypeDef.get())) {
                                methodVisitor.visitInsn(Opcodes.FCONST_0);
                                info.push(CodeInfo.Size._1);
                                methodVisitor.visitInsn(Opcodes.FRETURN);
                        } else if (returnType.equals(DoubleTypeDef.get())) {
                                methodVisitor.visitInsn(Opcodes.DCONST_0);
                                info.push(CodeInfo.Size._2);
                                methodVisitor.visitInsn(Opcodes.DRETURN);
                        } else throw new LtBug("unknown primitive:" + returnType);
                } else {
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        info.push(CodeInfo.Size._1);
                        methodVisitor.visitInsn(Opcodes.ARETURN);
                }

                for (CodeInfo.Container c : info.insToLabel.values()) {
                        if (!c.isVisited) throw new LtBug("not all labels are visited");
                }

                // exception
                for (ExceptionTable tbl : exceptionTable) {
                        methodVisitor.visitTryCatchBlock(
                                info.insToLabel.get(tbl.getFrom()).label,
                                info.insToLabel.get(tbl.getTo()).label,
                                info.insToLabel.get(tbl.getTarget()).label,
                                tbl.getType() == null ? null : typeToInternalName(tbl.getType()));
                }

                methodVisitor.visitMaxs(info.getMaxStack(), info.getMaxLocal());
        }

        /**
         * build static block (clinit).
         *
         * @param classWriter    class writer
         * @param staticIns      static instructions
         * @param exceptionTable exception table
         */
        private void buildStatic(ClassWriter classWriter, List<Instruction> staticIns, List<ExceptionTable> exceptionTable, InvokableMeta meta) {
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                buildInstructions(methodVisitor, new CodeInfo(0, null, meta), staticIns, exceptionTable, VoidType.get());
                methodVisitor.visitEnd();
        }

        /**
         * build constructor and its annotations.
         *
         * @param classWriter  class writer
         * @param constructors the constructors to build
         */
        private void buildConstructor(ClassWriter classWriter, List<SConstructorDef> constructors) {
                for (SConstructorDef cons : constructors) {
                        MethodVisitor methodVisitor = classWriter.visitMethod(
                                acc(cons.modifiers()),
                                "<init>",
                                methodDescWithParameters(
                                        VoidType.get(),
                                        cons.getParameters()),
                                null, null);

                        // annotations
                        for (SAnno anno : cons.annos()) {
                                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }
                        buildParameter(methodVisitor, cons.getParameters());

                        buildInstructions(
                                methodVisitor,
                                new CodeInfo(1 + cons.getParameters().size(), cons, cons.meta()),
                                cons.statements(), cons.exceptionTables(), VoidType.get());

                        methodVisitor.visitEnd();
                }
        }

        /**
         * build field and its annotations.
         *
         * @param classWriter class writer
         * @param fields      the fields to build
         */
        private void buildField(ClassWriter classWriter, List<SFieldDef> fields) {
                for (SFieldDef field : fields) {
                        FieldVisitor fieldVisitor = classWriter.visitField(acc(field.modifiers()), field.name(), typeToDesc(field.type()), null, null);

                        // annotations
                        for (SAnno anno : field.annos()) {
                                AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }

                        fieldVisitor.visitEnd();
                }
        }

        /**
         * build method and its annotations.
         *
         * @param classWriter class writer
         * @param methods     methods to build
         */
        private void buildMethod(ClassWriter classWriter, List<SMethodDef> methods) {
                for (SMethodDef method : methods) {
                        MethodVisitor methodVisitor = classWriter.visitMethod(
                                acc(method.modifiers()),
                                method.name(),
                                methodDescWithParameters(
                                        method.getReturnType(),
                                        method.getParameters()),
                                null, null);

                        // annotations
                        for (SAnno anno : method.annos()) {
                                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(
                                        typeToDesc(anno.type()),
                                        annotationIsVisible(anno)
                                );
                                buildAnnotation(annotationVisitor, anno);
                        }
                        buildParameter(methodVisitor, method.getParameters());

                        if (method.modifiers().contains(SModifier.ABSTRACT)) {
                                if (!method.statements().isEmpty())
                                        throw new LtBug("statements for abstract method should be empty");
                        } else {

                                buildInstructions(
                                        methodVisitor,
                                        new CodeInfo(
                                                (method.modifiers().contains(SModifier.STATIC) ? 0 : 1) + method.getParameters().size(),
                                                method,
                                                method.meta()),
                                        method.statements(), method.exceptionTables(), method.getReturnType());


                        }
                        methodVisitor.visitEnd();
                }
        }

        /**
         * build parameter annotations
         *
         * @param methodVisitor method visitor
         * @param params        parameters
         */
        private void buildParameter(MethodVisitor methodVisitor, List<SParameter> params) {
                for (int i = 0; i < params.size(); i++) {
                        SParameter param = params.get(i);

                        if (param.isCapture() && !param.isUsed()) {
                                // ignore those captured but not used params
                                continue;
                        }

                        methodVisitor.visitParameter(param.name(), param.canChange() ? 0 : Opcodes.ACC_FINAL);
                        for (SAnno anno : param.annos()) {
                                AnnotationVisitor annotationVisitor = methodVisitor.visitParameterAnnotation(
                                        i, typeToDesc(anno.type()), annotationIsVisible(anno)
                                );
                                buildAnnotation(annotationVisitor, anno);
                        }
                }
        }

        /**
         * check whether the annotation should be visible at runtime.
         *
         * @param anno annotation to check
         * @return true if visible at runtime, false otherwise
         */
        private boolean annotationIsVisible(SAnno anno) {
                SAnnoDef annoType = anno.type();
                for (SAnno a : annoType.annos()) {
                        if (a.type().fullName().equals("java.lang.annotation.Retention")) {
                                for (Map.Entry<SAnnoField, Value> entry : a.values().entrySet()) {
                                        if (entry.getKey().name().equals("value")) {
                                                Value v = entry.getValue();
                                                if (v instanceof EnumValue) {
                                                        if (v.type().fullName().equals("java.lang.annotation.RetentionPolicy")) {
                                                                return ((EnumValue) v).enumStr().equals("RUNTIME");
                                                        }
                                                }
                                                throw new LtBug(
                                                        "value of java.lang.annotation.Retention.value() " +
                                                                "should be instance of " +
                                                                "java.lang.annotation.RetentionPolicy, " +
                                                                "but got " + v
                                                );
                                        }
                                }
                                break;
                        }
                }
                return false;
        }

        private void buildAnnotationValue(AnnotationVisitor annotationVisitor, String name, Value v) {
                if (v instanceof EnumValue) {
                        annotationVisitor.visitEnum(
                                name,
                                "L" + (v.type().fullName().replace(".", "/")) + ";",
                                ((EnumValue) v).enumStr());
                } else if (v instanceof SArrayValue && !(((SArrayValue) v).type().type() instanceof PrimitiveTypeDef)) {
                        AnnotationVisitor visitor = annotationVisitor.visitArray(name);
                        for (Value arrValue : ((SArrayValue) v).values()) {
                                if (arrValue instanceof EnumValue) {
                                        visitor.visitEnum(null,
                                                "L" + (arrValue.type().fullName().replace(".", "/")) + ";",
                                                ((EnumValue) arrValue).enumStr());
                                } else if (arrValue instanceof SAnno) {
                                        AnnotationVisitor annoVisitor = visitor.visitAnnotation(null, typeToDesc(arrValue.type()));
                                        buildAnnotation(annoVisitor, (SAnno) arrValue);
                                } else {
                                        visitor.visit(null, parseValueIntoASMObject(arrValue));
                                }
                        }
                        visitor.visitEnd();
                } else if (v instanceof SAnno) {
                        AnnotationVisitor visitor = annotationVisitor.visitAnnotation(name, typeToDesc(v.type()));
                        buildAnnotation(visitor, (SAnno) v);
                } else {
                        // primitives
                        annotationVisitor.visit(name, parseValueIntoASMObject(v));
                }
        }

        /**
         * build annotation.
         *
         * @param annotationVisitor annotation visitor
         * @param anno              the annotation to build
         */
        private void buildAnnotation(AnnotationVisitor annotationVisitor, SAnno anno) {
                for (Map.Entry<SAnnoField, Value> entry : anno.values().entrySet()) {
                        String name = entry.getKey().name();
                        Value v = entry.getValue();

                        buildAnnotationValue(annotationVisitor, name, v);
                }
                annotationVisitor.visitEnd();
        }

        /**
         * transform {@link Value} into asm Object.<br>
         * the Value can only be {@link PrimitiveValue}, {@link StringConstantValue}, {link ClassValue}, {@link SArrayValue}
         *
         * @param value the value
         * @return parsed object
         */
        private Object parseValueIntoASMObject(Value value) {
                if (value instanceof IntValue) return ((IntValue) value).getValue();
                else if (value instanceof LongValue) return ((LongValue) value).getValue();
                else if (value instanceof CharValue) return (char) ((CharValue) value).getValue();
                else if (value instanceof ShortValue) return (short) ((ShortValue) value).getValue();
                else if (value instanceof ByteValue) return (byte) ((ByteValue) value).getValue();
                else if (value instanceof BoolValue) return ((BoolValue) value).getValue() != 0;
                else if (value instanceof FloatValue) return ((FloatValue) value).getValue();
                else if (value instanceof DoubleValue) return ((DoubleValue) value).getValue();
                else if (value instanceof StringConstantValue) return ((StringConstantValue) value).getStr();
                else if (value instanceof Ins.GetClass) return Type.getObjectType(((Ins.GetClass) value).targetType()
                        .fullName().replace(".", "/"));
                else if (value instanceof SArrayValue && ((SArrayValue) value).type().type() instanceof PrimitiveTypeDef) {
                        PrimitiveTypeDef primitiveType = (PrimitiveTypeDef) ((SArrayValue) value).type().type();
                        int length = ((SArrayValue) value).length();
                        if (primitiveType.equals(IntTypeDef.get())) {
                                int[] arr = new int[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Integer) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(LongTypeDef.get())) {
                                long[] arr = new long[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Long) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(CharTypeDef.get())) {
                                char[] arr = new char[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Character) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(ShortTypeDef.get())) {
                                short[] arr = new short[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Short) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(ByteTypeDef.get())) {
                                byte[] arr = new byte[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Byte) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(BoolTypeDef.get())) {
                                boolean[] arr = new boolean[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Boolean) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(FloatTypeDef.get())) {
                                float[] arr = new float[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Float) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(DoubleTypeDef.get())) {
                                double[] arr = new double[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (Double) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else throw new LtBug("unknown primitive type " + primitiveType);
                }
                throw new LtBug(value + " is not supported");
        }
}
