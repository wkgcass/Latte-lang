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
import lt.compiler.semantic.builtin.ClassValue;
import lt.dependencies.asm.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * code generator, generate byte code from STypeDef
 */
public class CodeGenerator {
        private Set<STypeDef> types;

        /**
         * create the code generator with types to generate
         *
         * @param types types
         */
        public CodeGenerator(Set<STypeDef> types) {
                this.types = types;
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
         * get type descriptor of the given type<br>
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
         * get internal name of a type<br>
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
         * get method descriptor<br>
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

        /**
         * start the generating process
         *
         * @return Map&lt;FileName, byte[]&gt;
         */
        public Map<String, byte[]> generate() {
                Map<String, byte[]> result = new HashMap<>();
                for (STypeDef type : types) {
                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                        List<SModifier> modifiers;                // modifier
                        List<Instruction> staticIns;              // <clinit>
                        List<ExceptionTable> exceptionTables;     // exception table for <clinit>
                        List<SConstructorDef> constructors = null;// constructor
                        List<SFieldDef> fields;                   // field
                        List<SMethodDef> methods;                 // method
                        List<SAnno> annos = type.annos();         // annotations
                        SClassDef superClass = null;              // super class
                        List<SInterfaceDef> superInterfaces;      // super interface
                        String fileName = type.line_col().fileName; // file name

                        if (type instanceof SClassDef) {
                                modifiers = ((SClassDef) type).modifiers();
                                staticIns = ((SClassDef) type).staticStatements();
                                exceptionTables = ((SClassDef) type).staticExceptionTable();
                                constructors = ((SClassDef) type).constructors();
                                fields = ((SClassDef) type).fields();
                                methods = ((SClassDef) type).methods();
                                superInterfaces = ((SClassDef) type).superInterfaces();
                                superClass = ((SClassDef) type).parent();
                        } else {
                                modifiers = ((SInterfaceDef) type).modifiers();
                                staticIns = ((SInterfaceDef) type).staticStatements();
                                exceptionTables = ((SInterfaceDef) type).staticExceptionTable();
                                fields = ((SInterfaceDef) type).fields();
                                methods = ((SInterfaceDef) type).methods();
                                superInterfaces = ((SInterfaceDef) type).superInterfaces();
                        }

                        classWriter.visitSource(fileName, fileName);

                        String[] interfaces = new String[superInterfaces.size()];
                        for (int i = 0; i < interfaces.length; ++i) {
                                interfaces[i] = typeToInternalName(superInterfaces.get(i));
                        }

                        classWriter.visit(Opcodes.V1_8, acc(modifiers) | (type instanceof SClassDef ? 0 : Opcodes.ACC_INTERFACE),
                                typeToInternalName(type), null, superClass == null ? "java/lang/Object" : typeToInternalName(superClass), interfaces);

                        // annotations
                        for (SAnno anno : annos) {
                                AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }

                        buildStatic(classWriter, staticIns, exceptionTables);
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

        /**
         * build new<br>
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
                        buildValueAccess(methodVisitor, info, v);
                }
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        typeToInternalName(aNew.type()),
                        "<init>",
                        methodDesc(
                                VoidType.get(),
                                aNew.constructor().getParameters().stream().map(SParameter::type).collect(Collectors.toList())),
                        false);
                info.pop(1 + aNew.args().size());
        }

        /**
         * build cast<br>
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
                buildValueAccess(methodVisitor, info, cast.value());
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
        }

        /**
         * build two variable operation<br>
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
                buildValueAccess(methodVisitor, info, twoVarOp.a());
                buildValueAccess(methodVisitor, info, twoVarOp.b());
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
         * build logicAND<br>
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
                buildValueAccess(methodVisitor, info, logicAnd.b1());
                // if eq goto flag
                Label flag = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, flag);
                info.pop(1);
                // b2
                buildValueAccess(methodVisitor, info, logicAnd.b2());
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
         * build logicOr<br>
         * <br>
         * <code>
         * buildValueAccess<br>
         * IfNe flag ---------- true<br>
         * buildValueAccess<br>
         * IfNe flag ---------- true<br>
         * IConst_0<br>
         * goto nop<br>
         * flag: IConst1<br>
         * nop
         * </code>
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param logicOr       Ins.LogicOr
         */
        private void buildLogicOr(MethodVisitor methodVisitor, CodeInfo info, Ins.LogicOr logicOr) {
                /*
                 * if ne b1 goto flag (if b1==false goto flag)
                 * if ne b2 goto flag (if b2==false goto flag)
                 * false              (push true into stack)
                 * goto nop           (goto nop, skip `else` branch)
                 * flag: true         (flag: push false into stack)
                 * nop                (nop)
                 */
                buildValueAccess(methodVisitor, info, logicOr.b1());
                // if ne goto flag
                Label flag = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNE, flag);
                info.pop(1);
                // b2
                buildValueAccess(methodVisitor, info, logicOr.b2());
                // if ne goto flag
                methodVisitor.visitJumpInsn(Opcodes.IFNE, flag);
                info.pop(1);
                // false
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                // goto nop
                Label nop = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, nop);
                // flag: true
                methodVisitor.visitLabel(flag);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                // nop
                methodVisitor.visitLabel(nop);
                methodVisitor.visitInsn(Opcodes.NOP);

                info.push(CodeInfo.Size._1); // push true or false into stack
        }

        /**
         * build TALoad<br>
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
                buildValueAccess(methodVisitor, info, TALoad.arr());
                buildValueAccess(methodVisitor, info, TALoad.index());
                methodVisitor.visitInsn(TALoad.mode());
                info.pop(2);
                info.push(CodeInfo.Size._1);
        }

        /**
         * build OneVarOp<br>
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
                buildValueAccess(methodVisitor, info, oneVarOp.value());
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
         * build NewArray<br>
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
                        buildValueAccess(methodVisitor, info, v); // value

                        methodVisitor.visitInsn(newArray.storeMode());
                        info.pop(3);
                        ++i;
                }
                // stack have one element (the array)
        }

        /**
         * build ANewArray<br>
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
                        buildValueAccess(methodVisitor, info, v); // value

                        methodVisitor.visitInsn(Opcodes.AASTORE);
                        info.pop(3);
                        ++i;
                }
                // stack have one element (the array)
        }

        /**
         * build NewList<br>
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
                        buildValueAccess(methodVisitor, info, v); // arg
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                        info.pop(2);
                        info.push(CodeInfo.Size._1); // the add result (boolean add(Object))
                        methodVisitor.visitInsn(Opcodes.POP);
                        info.pop(1);
                }
                // stack have one element (the list)
        }

        /**
         * build NewMap<br>
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
                methodVisitor.visitTypeInsn(Opcodes.NEW, "java/util/LinkedHashMap");
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);

                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        "java/util/LinkedHashMap",
                        "<init>",
                        "()V", false);
                info.pop(1);

                for (Map.Entry<Value, Value> entry : newMap.initValues().entrySet()) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        info.push(CodeInfo.Size._1);
                        buildValueAccess(methodVisitor, info, entry.getKey());
                        buildValueAccess(methodVisitor, info, entry.getValue());

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                "java/util/LinkedHashMap",
                                "put",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                        info.pop(3);
                        info.push(CodeInfo.Size._1); // the put result (Object put(Object,Object))
                        methodVisitor.visitInsn(Opcodes.POP);
                        info.pop(1);
                }
                // stack have one element (the map)
        }

        /**
         * build Value
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param value         the value to build
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
         * @see MethodHandleValue
         * @see lt.compiler.semantic.Ins.CheckCast
         */
        private void buildValueAccess(MethodVisitor methodVisitor, CodeInfo info, Value value) {
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

                        methodVisitor.visitLineNumber(((Ins.GetStatic) value).line_col().line, label);
                } else if (value instanceof Ins.TLoad) {
                        // tLoad
                        Ins.TLoad tLoad = (Ins.TLoad) value;

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitVarInsn(tLoad.mode(), tLoad.getIndex());
                        if (tLoad.mode() == Ins.TLoad.Dload || tLoad.mode() == Ins.TLoad.Lload)
                                info.push(CodeInfo.Size._2);
                        else info.push(CodeInfo.Size._1);

                        methodVisitor.visitLineNumber(((Ins.TLoad) value).line_col().line, label);
                } else if (value instanceof StringConstantValue) {
                        methodVisitor.visitLdcInsn(((StringConstantValue) value).getStr());
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof PrimitiveValue) {
                        buildPrimitive(methodVisitor, info, (PrimitiveValue) value);
                } else if (value instanceof Ins.Invoke) {
                        buildInvoke(methodVisitor, info, (Ins.Invoke) value);
                } else if (value instanceof Ins.New) {
                        buildNew(methodVisitor, info, (Ins.New) value);
                } else if (value instanceof Ins.Cast) {
                        buildCast(methodVisitor, info, (Ins.Cast) value);
                } else if (value instanceof Ins.TwoVarOp) {
                        buildTwoVarOp(methodVisitor, info, (Ins.TwoVarOp) value);
                } else if (value instanceof ValuePack) {
                        int depth = info.getCurrentStackDepth();
                        List<Instruction> instructions = ((ValuePack) value).instructions();
                        for (int i = 0; i < instructions.size() - 1; ++i) {
                                buildOneIns(methodVisitor, info, instructions.get(i));

                                if (((ValuePack) value).autoPop()) {
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
                        buildOneIns(methodVisitor, info, ins);
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
                        buildValueAccess(methodVisitor, info, ((Ins.ArrayLength) value).arrayValue());
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
                } else if (value instanceof MethodHandleValue) {
                        methodVisitor.visitLdcInsn(getHandle((MethodHandleValue) value));
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof Ins.CheckCast) {
                        buildValueAccess(methodVisitor, info, ((Ins.CheckCast) value).theValueToCheck());
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, typeToInternalName(value.type()));
                        info.pop(1);
                        info.push(CodeInfo.Size._1);
                } else if (value instanceof ValueAnotherType) {
                        buildValueAccess(methodVisitor, info, ((ValueAnotherType) value).value());
                } else {
                        throw new LtBug("unknown value " + value);
                }
        }

        /**
         * build Primitive
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
         * build invoke
         *
         * @param methodVisitor method visitor
         * @param info          info
         * @param invoke        Ins.Invoke
         * @see lt.compiler.semantic.Ins.Invoke
         * @see lt.compiler.semantic.Ins.InvokeSpecial
         * @see lt.compiler.semantic.Ins.InvokeVirtual
         * @see lt.compiler.semantic.Ins.InvokeStatic
         * @see lt.compiler.semantic.Ins.InvokeInterface
         * @see lt.compiler.semantic.Ins.InvokeDynamic
         */
        private void buildInvoke(MethodVisitor methodVisitor, CodeInfo info, Ins.Invoke invoke) {
                if (invoke instanceof Ins.InvokeSpecial) {
                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeSpecial) invoke).target());

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v);
                        }

                        // invoke special
                        Ins.InvokeSpecial invokeSpecial = (Ins.InvokeSpecial) invoke;
                        SInvokable invokable = invokeSpecial.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDesc(invokable.getReturnType(),
                                        invokable.getParameters().stream().map(SParameter::type).collect(Collectors.toList()));
                        } else if (invokable instanceof SConstructorDef) {
                                name = "<init>";
                                desc = methodDesc(VoidType.get(), invokable.getParameters().stream().map(SParameter::type).collect(Collectors.toList()));
                        } else throw new LtBug("cannot invoke special on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESPECIAL,
                                owner,
                                name, desc, false);
                        info.pop(1 + invoke.arguments().size());
                        if (!invoke.invokable().getReturnType().equals(VoidType.get())) {
                                STypeDef typeDef = invoke.invokable().getReturnType();
                                if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                        info.push(CodeInfo.Size._2);
                                else info.push(CodeInfo.Size._1);
                        }

                        methodVisitor.visitLineNumber(invoke.line_col().line, label);
                } else if (invoke instanceof Ins.InvokeVirtual) {
                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeVirtual) invoke).target());

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v);
                        }

                        // invoke special
                        Ins.InvokeVirtual invokeSpecial = (Ins.InvokeVirtual) invoke;
                        SInvokable invokable = invokeSpecial.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDesc(invokable.getReturnType(),
                                        invokable.getParameters().stream().map(SParameter::type).collect(Collectors.toList()));
                        } else throw new LtBug("cannot invoke virtual on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                owner,
                                name, desc, false);
                        info.pop(1 + invoke.arguments().size());
                        if (!invoke.invokable().getReturnType().equals(VoidType.get())) {
                                STypeDef typeDef = invoke.invokable().getReturnType();
                                if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                        info.push(CodeInfo.Size._2);
                                else info.push(CodeInfo.Size._1);
                        }

                        methodVisitor.visitLineNumber(invoke.line_col().line, label);
                } else if (invoke instanceof Ins.InvokeStatic) {
                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v);
                        }

                        // invoke special
                        Ins.InvokeStatic invokeStatic = (Ins.InvokeStatic) invoke;
                        SInvokable invokable = invokeStatic.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDesc(invokable.getReturnType(),
                                        invokable.getParameters().stream().map(SParameter::type).collect(Collectors.toList()));
                        } else throw new LtBug("cannot invoke static on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                owner,
                                name, desc, false);
                        info.pop(invoke.arguments().size());
                        if (!invoke.invokable().getReturnType().equals(VoidType.get())) {
                                STypeDef typeDef = invoke.invokable().getReturnType();
                                if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                        info.push(CodeInfo.Size._2);
                                else info.push(CodeInfo.Size._1);
                        }

                        methodVisitor.visitLineNumber(invoke.line_col().line, label);
                } else if (invoke instanceof Ins.InvokeInterface) {
                        // push target object
                        buildValueAccess(methodVisitor, info, ((Ins.InvokeInterface) invoke).target());

                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v);
                        }

                        // invoke special
                        Ins.InvokeInterface invokeInterface = (Ins.InvokeInterface) invoke;
                        SInvokable invokable = invokeInterface.invokable();

                        String name;
                        String desc;
                        if (invokable instanceof SMethodDef) {
                                name = ((SMethodDef) invokable).name();
                                desc = methodDesc(invokable.getReturnType(),
                                        invokable.getParameters().stream().map(SParameter::type).collect(Collectors.toList()));
                        } else throw new LtBug("cannot invoke interface on " + invokable);

                        String owner = typeToInternalName(invokable.declaringType());

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                owner,
                                name, desc, true);
                        info.pop(1 + invoke.arguments().size());
                        if (!invoke.invokable().getReturnType().equals(VoidType.get())) {
                                STypeDef typeDef = invoke.invokable().getReturnType();
                                if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                        info.push(CodeInfo.Size._2);
                                else info.push(CodeInfo.Size._1);
                        }

                        methodVisitor.visitLineNumber(invoke.line_col().line, label);
                } else if (invoke instanceof Ins.InvokeDynamic) {
                        // push parameters
                        for (Value v : invoke.arguments()) {
                                buildValueAccess(methodVisitor, info, v);
                        }

                        // invoke dynamic
                        Ins.InvokeDynamic invokeDynamic = (Ins.InvokeDynamic) invoke;
                        SInvokable bootstrapMethod = invokeDynamic.invokable();

                        Label label = new Label();
                        methodVisitor.visitLabel(label);

                        methodVisitor.visitInvokeDynamicInsn(
                                invokeDynamic.methodName(),
                                methodDesc(
                                        invokeDynamic.returnType(),
                                        invokeDynamic.arguments().stream().map(Value::type).collect(Collectors.toList())),
                                new Handle(
                                        invokeDynamic.indyType(), // tag
                                        typeToInternalName(bootstrapMethod.declaringType()), // bootstrap method owner
                                        ((SMethodDef) bootstrapMethod).name(), // bootstrap method name
                                        methodDesc( // desc
                                                bootstrapMethod.getReturnType(),
                                                bootstrapMethod.getParameters().stream().map(SParameter::type).collect(Collectors.toList())),
                                        false),
                                getIndyArgs(invokeDynamic));
                        info.pop(invoke.arguments().size());
                        if (!invoke.invokable().getReturnType().equals(VoidType.get())) {
                                STypeDef typeDef = invoke.invokable().getReturnType();
                                if (typeDef.equals(DoubleTypeDef.get()) || typeDef.equals(LongTypeDef.get()))
                                        info.push(CodeInfo.Size._2);
                                else info.push(CodeInfo.Size._1);
                        }

                        methodVisitor.visitLineNumber(invoke.line_col().line, label);
                } else throw new LtBug("unknown invoke type " + invoke);
                if (invoke.invokable().getReturnType().equals(VoidType.get())) {
                        // void methods
                        // push Undefined into stack
                        methodVisitor.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "lt/lang/Undefined",
                                "get",
                                "()Llt/lang/Undefined;",
                                false);
                        info.push(CodeInfo.Size._1);
                }
        }

        /**
         * get asm {@link Handle} object from {@link MethodHandleValue}
         *
         * @param handle {@link MethodHandleValue}
         * @return {@link Handle}
         */
        private Handle getHandle(MethodHandleValue handle) {
                return new Handle(
                        handle.mode(),
                        typeToInternalName(handle.method().declaringType()),
                        handle.method().name(),
                        methodDesc(
                                handle.method().getReturnType(),
                                handle.method().getParameters().stream().map(SParameter::type).collect(Collectors.toList())),
                        handle.method().declaringType() instanceof SInterfaceDef
                );
        }

        /**
         * get invoke dynamic arguments (bootstrap arguments)
         *
         * @param invokeDynamic invoke dynamic
         * @return Object array containing required objects
         */
        private Object[] getIndyArgs(Ins.InvokeDynamic invokeDynamic) {
                Object[] args = new Object[invokeDynamic.indyArgs().size()];
                for (int i = 0; i < args.length; ++i) {
                        Value v = invokeDynamic.indyArgs().get(i);
                        Object o;
                        if (v instanceof MethodHandleValue) {
                                MethodHandleValue handle = (MethodHandleValue) v;
                                o = getHandle(handle);
                        } else if (v instanceof MethodTypeValue) {
                                Type[] types = new Type[((MethodTypeValue) v).parameters().size()];
                                for (int j = 0; j < types.length; ++j) {
                                        Type t = Type.getType(typeToDesc(((MethodTypeValue) v).parameters().get(j)));
                                        types[j] = t;
                                }
                                o = Type.getMethodType(
                                        Type.getType(typeToDesc(((MethodTypeValue) v).returnType())),
                                        types);
                        } else throw new LtBug("unsupported indy arg type " + v.type());
                        args[i] = o;
                }
                return args;
        }

        /**
         * build Return<br>
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
                        buildValueAccess(methodVisitor, info, tReturn.value());
                        methodVisitor.visitInsn(tReturn.returnIns());
                        info.pop(1);
                }
        }

        /**
         * build TStore<br>
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
                buildValueAccess(methodVisitor, info, tStore.newValue());
                methodVisitor.visitVarInsn(tStore.mode(), tStore.index());
                info.pop(1);
                info.registerLocal(tStore.index());
        }

        /**
         * build PutField<br>
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
                buildValueAccess(methodVisitor, info, putField.obj());
                buildValueAccess(methodVisitor, info, putField.value());
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTFIELD,
                        typeToInternalName(putField.field().declaringType()),
                        putField.field().name(),
                        typeToDesc(putField.field().type()));
                info.pop(2);
        }

        /**
         * build GetField<br>
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
                buildValueAccess(methodVisitor, info, getField.object());
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
        }

        /**
         * build PutStatic<br>
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
                buildValueAccess(methodVisitor, info, putStatic.value());
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        typeToInternalName(putStatic.field().declaringType()),
                        putStatic.field().name(),
                        typeToDesc(putStatic.field().type()));
                info.pop(1);
        }

        /**
         * build TAStore<br>
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
                buildValueAccess(methodVisitor, info, TAStore.array()); // array
                buildValueAccess(methodVisitor, info, TAStore.index()); // index
                buildValueAccess(methodVisitor, info, TAStore.value()); // value

                methodVisitor.visitInsn(TAStore.mode());
                info.pop(3);
        }

        /**
         * build MonitorEnter<br>
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
                buildValueAccess(methodVisitor, info, monitorEnter.valueToMonitor());
                methodVisitor.visitInsn(Opcodes.DUP);
                info.push(CodeInfo.Size._1);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, monitorEnter.storeIndex());
                info.pop(1);
                methodVisitor.visitInsn(Opcodes.MONITORENTER);
                info.pop(1);
        }

        /**
         * build MonitorExit<br>
         * <br>
         * ALoad ---- the object that was monitored<br>
         * MonitorExit
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param monitorExit   Ins.MonitorExit
         */
        private void buildMonitorExit(MethodVisitor methodVisitor, CodeInfo info, Ins.MonitorExit monitorExit) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, monitorExit.enterInstruction().storeIndex());
                info.push(CodeInfo.Size._1);
                methodVisitor.visitInsn(Opcodes.MONITOREXIT);
                info.pop(1);
        }

        /**
         * build one Instruction
         *
         * @param methodVisitor method visitor
         * @param info          method info
         * @param ins           Instruction
         * @see Instruction
         * @see Value
         * @see lt.compiler.semantic.Ins.TReturn
         * @see lt.compiler.semantic.Ins.TStore
         * @see lt.compiler.semantic.Ins.PutField
         * @see lt.compiler.semantic.Ins.PutStatic
         * @see lt.compiler.semantic.Ins.IfNe
         * @see lt.compiler.semantic.Ins.IfEq
         * @see lt.compiler.semantic.Ins.Goto
         * @see lt.compiler.semantic.Ins.Nop
         * @see lt.compiler.semantic.Ins.AThrow
         * @see lt.compiler.semantic.Ins.ExStore
         * @see lt.compiler.semantic.Ins.Pop
         * @see lt.compiler.semantic.Ins.TAStore
         * @see lt.compiler.semantic.Ins.MonitorEnter
         * @see lt.compiler.semantic.Ins.MonitorExit
         */
        private void buildOneIns(MethodVisitor methodVisitor, CodeInfo info, Instruction ins) {
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
                        buildValueAccess(methodVisitor, info, (Value) ins);
                } else if (ins instanceof Ins.TReturn) {
                        buildReturn(methodVisitor, info, (Ins.TReturn) ins);
                } else if (ins instanceof Ins.TStore) {
                        buildTStore(methodVisitor, info, (Ins.TStore) ins);
                } else if (ins instanceof Ins.PutField) {
                        buildPutField(methodVisitor, info, (Ins.PutField) ins);
                } else if (ins instanceof Ins.PutStatic) {
                        buildPutStatic(methodVisitor, info, (Ins.PutStatic) ins);
                } else if (ins instanceof Ins.IfNe) {
                        buildValueAccess(methodVisitor, info, ((Ins.IfNe) ins).condition());
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
                        buildValueAccess(methodVisitor, info, ((Ins.IfEq) ins).condition());
                        Label l;
                        if (info.insToLabel.containsKey(((Ins.IfEq) ins).gotoIns())) {
                                l = info.insToLabel.get(((Ins.IfEq) ins).gotoIns()).label;
                        } else {
                                l = new Label();
                                info.insToLabel.put(((Ins.IfEq) ins).gotoIns(), new CodeInfo.Container(l));
                        }
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, l);
                        info.pop(1);
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
                        buildValueAccess(methodVisitor, info, ((Ins.AThrow) ins).exception());
                        methodVisitor.visitInsn(Opcodes.ATHROW);
                        info.pop(1);
                } else if (ins instanceof Ins.ExStore) {
                        info.push(CodeInfo.Size._1);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, ((Ins.ExStore) ins).index());
                        info.registerLocal(((Ins.ExStore) ins).index());
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
         * build instructions for method/constructor/staticScope<br>
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
                        buildOneIns(methodVisitor, info, ins);
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
         * build static block (clinit)
         *
         * @param classWriter    class writer
         * @param staticIns      static instructions
         * @param exceptionTable exception table
         */
        private void buildStatic(ClassWriter classWriter, List<Instruction> staticIns, List<ExceptionTable> exceptionTable) {
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                buildInstructions(methodVisitor, new CodeInfo(0), staticIns, exceptionTable, VoidType.get());
                methodVisitor.visitEnd();
        }

        /**
         * build constructor and its annotations
         *
         * @param classWriter  class writer
         * @param constructors the constructors to build
         */
        private void buildConstructor(ClassWriter classWriter, List<SConstructorDef> constructors) {
                for (SConstructorDef cons : constructors) {
                        // annotations
                        for (SAnno anno : cons.annos()) {
                                AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }

                        MethodVisitor methodVisitor = classWriter.visitMethod(
                                acc(cons.modifiers()),
                                "<init>",
                                methodDesc(
                                        VoidType.get(),
                                        cons.getParameters().stream().map(SParameter::type).collect(Collectors.toList())),
                                null, null);
                        buildInstructions(
                                methodVisitor,
                                new CodeInfo(1 + cons.getParameters().size()),
                                cons.statements(), cons.exceptionTables(), VoidType.get());

                        methodVisitor.visitEnd();
                }
        }

        /**
         * build field and its annotations
         *
         * @param classWriter class writer
         * @param fields      the fields to build
         */
        private void buildField(ClassWriter classWriter, List<SFieldDef> fields) {
                for (SFieldDef field : fields) {
                        // annotations
                        for (SAnno anno : field.annos()) {
                                AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(typeToDesc(anno.type()),
                                        annotationIsVisible(anno));
                                buildAnnotation(annotationVisitor, anno);
                        }

                        FieldVisitor fieldVisitor = classWriter.visitField(acc(field.modifiers()), field.name(), typeToDesc(field.type()), null, null);
                        fieldVisitor.visitEnd();
                }
        }

        /**
         * build method and its annotations
         *
         * @param classWriter class writer
         * @param methods     methods to build
         */
        private void buildMethod(ClassWriter classWriter, List<SMethodDef> methods) {
                for (SMethodDef method : methods) {
                        MethodVisitor methodVisitor = classWriter.visitMethod(
                                acc(method.modifiers()),
                                method.name(),
                                methodDesc(
                                        method.getReturnType(),
                                        method.getParameters().stream().map(SParameter::type).collect(Collectors.toList())),
                                null, null);

                        // annotations
                        for (SAnno anno : method.annos()) {
                                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(
                                        typeToDesc(anno.type()),
                                        annotationIsVisible(anno)
                                );
                                buildAnnotation(annotationVisitor, anno);
                        }

                        if (method.modifiers().contains(SModifier.ABSTRACT)) {
                                if (!method.statements().isEmpty()) throw new LtBug("statements for abstract method should be empty");
                        } else {

                                buildInstructions(
                                        methodVisitor,
                                        new CodeInfo(
                                                (method.modifiers().contains(SModifier.STATIC) ? 0 : 1) + method.getParameters().size()
                                        ),
                                        method.statements(), method.exceptionTables(), method.getReturnType());


                        }
                        methodVisitor.visitEnd();
                }
        }

        /**
         * check whether the annotation should be visible at runtime
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
                                                        if (v.type().fullName().equals("java.lang.annotation.RetentionPolicy")
                                                                && ((EnumValue) v).enumStr().equals("RUNTIME"))
                                                                return true;
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

        /**
         * build annotation
         *
         * @param annotationVisitor annotation visitor
         * @param anno              the annotation to build
         */
        private void buildAnnotation(AnnotationVisitor annotationVisitor, SAnno anno) {
                for (Map.Entry<SAnnoField, Value> entry : anno.values().entrySet()) {
                        String name = entry.getKey().name();
                        Value v = entry.getValue();

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
                                                visitor.visit(null, parseValueIntoASMObject(v));
                                        }
                                }
                                visitor.visitEnd();
                        } else if (v instanceof SAnno) {
                                AnnotationVisitor visitor = annotationVisitor.visitAnnotation(name, typeToInternalName(v.type()));
                                buildAnnotation(visitor, (SAnno) v);
                        } else {
                                // primitives
                                annotationVisitor.visit(name, parseValueIntoASMObject(v));
                        }
                }
                annotationVisitor.visitEnd();
        }

        /**
         * transform {@link Value} into asm Object<br>
         * the Value can only be {@link PrimitiveValue}, {@link StringConstantValue}, {@link ClassValue}, {@link SArrayValue}
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
                else if (value instanceof ClassValue) return Type.getObjectType(((ClassValue) value).className().replace(".", "/"));
                else if (value instanceof SArrayValue && ((SArrayValue) value).type().type() instanceof PrimitiveTypeDef) {
                        PrimitiveTypeDef primitiveType = (PrimitiveTypeDef) ((SArrayValue) value).type().type();
                        int length = ((SArrayValue) value).length();
                        if (primitiveType.equals(IntTypeDef.get())) {
                                int[] arr = new int[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (int) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(LongTypeDef.get())) {
                                long[] arr = new long[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (long) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(CharTypeDef.get())) {
                                char[] arr = new char[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (char) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(ShortTypeDef.get())) {
                                short[] arr = new short[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (short) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(ByteTypeDef.get())) {
                                byte[] arr = new byte[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (byte) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(BoolTypeDef.get())) {
                                boolean[] arr = new boolean[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (boolean) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(FloatTypeDef.get())) {
                                float[] arr = new float[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (float) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else if (primitiveType.equals(DoubleTypeDef.get())) {
                                double[] arr = new double[length];
                                for (int i = 0; i < length; ++i) {
                                        arr[i] = (double) parseValueIntoASMObject(((SArrayValue) value).values()[i]);
                                }
                                return arr;
                        } else throw new LtBug("unknown primitive type " + primitiveType);
                }
                throw new LtBug(value + " is not supported");
        }
}
