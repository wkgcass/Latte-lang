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

package lt.compiler.semantic;

import lt.compiler.LineCol;
import lt.compiler.SemanticScope;
import lt.compiler.SyntaxException;
import lt.compiler.semantic.builtin.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * abstract instructions
 */
public class Ins {
        private Ins() {
        }

        /**
         * a new array
         */
        public static class ANewArray implements Value {
                private final SArrayTypeDef arrayType;
                private final STypeDef componentType;
                private final IntValue count;
                private final List<Value> initValues = new ArrayList<>();

                public ANewArray(SArrayTypeDef arrayType, STypeDef componentType, IntValue count) {
                        this.arrayType = arrayType;
                        this.componentType = componentType;
                        this.count = count;
                }

                @Override
                public SArrayTypeDef type() {
                        return arrayType;
                }

                public STypeDef componentType() {
                        return componentType;
                }

                public IntValue count() {
                        return count;
                }

                public List<Value> initValues() {
                        return initValues;
                }
        }

        /**
         * arrayLength
         */
        public static class ArrayLength implements Value, Instruction, ReadOnly {
                private final LineCol lineCol;
                private final Value arrayValue;

                public ArrayLength(Value arrayValue, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.arrayValue = arrayValue;
                }

                public Value arrayValue() {
                        return arrayValue;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return IntTypeDef.get();
                }
        }

        /**
         * throw
         */
        public static class AThrow implements Instruction {
                private final Value exception;
                private final LineCol lineCol;

                public AThrow(Value exception, LineCol lineCol) {
                        this.exception = exception;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value exception() {
                        return exception;
                }
        }

        /**
         * cast
         */
        public static class Cast implements Value, Instruction {
                public static final int CAST_INT_TO_LONG = 0x85;
                public static final int CAST_INT_TO_FLOAT = 0x86;
                public static final int CAST_INT_TO_DOUBLE = 0x87;

                public static final int CAST_LONG_TO_INT = 0x88;
                public static final int CAST_LONG_TO_FLOAT = 0x89;
                public static final int CAST_LONG_TO_DOUBLE = 0x8A;

                public static final int CAST_FLOAT_TO_INT = 0x8B;
                public static final int CAST_FLOAT_TO_LONG = 0x8C;
                public static final int CAST_FLOAT_TO_DOUBLE = 0x8D;

                public static final int CAST_DOUBLE_TO_INT = 0x8E;
                public static final int CAST_DOUBLE_TO_LONG = 0x8F;
                public static final int CAST_DOUBLE_TO_FLOAT = 0x90;

                public static final int CAST_INT_TO_BYTE = 0x91;
                public static final int CAST_INT_TO_CHAR = 0x92;
                public static final int CAST_INT_TO_SHORT = 0x93;

                private final Value value;
                private final STypeDef type;
                private final int castMode;

                private final LineCol lineCol;

                public Cast(STypeDef type, Value value, int castMode, LineCol lineCol) {
                        this.type = type;
                        this.value = value;
                        this.castMode = castMode;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return type;
                }

                public Value value() {
                        return value;
                }

                public int castMode() {
                        return castMode;
                }
        }

        /**
         * cast the object to another type
         */
        public static class CheckCast implements Instruction, Value {
                private final Value theValueToCheck;
                private final STypeDef requiredType;
                private final LineCol lineCol;

                public CheckCast(Value theValueToCheck, STypeDef requiredType, LineCol lineCol) {
                        this.theValueToCheck = theValueToCheck;
                        this.requiredType = requiredType;
                        this.lineCol = lineCol;
                }

                public Value theValueToCheck() {
                        return theValueToCheck;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return requiredType;
                }
        }

        /**
         * exception store<br>
         * this storage will be parsed into <code>AStore index</code>, the exception pushed by jvm will be stored into local variable table
         */
        public static class ExStore implements Instruction {
                private final int index;

                public ExStore(LeftValue ex, SemanticScope scope) {
                        index = scope.getIndex(ex);
                }

                @Override
                public LineCol line_col() {
                        return null;
                }

                public int index() {
                        return index;
                }
        }

        /**
         * XXX.class
         */
        public static class GetClass implements Value, ReadOnly {
                private final STypeDef targetType;
                private final SClassDef classClassDef;

                /**
                 * @param targetType    the represented type
                 * @param classClassDef java.lang.Class
                 */
                public GetClass(STypeDef targetType, SClassDef classClassDef) {
                        this.targetType = targetType;
                        this.classClassDef = classClassDef;
                }

                /**
                 * this value type
                 *
                 * @return java.lang.Class
                 */
                @Override
                public STypeDef type() {
                        return classClassDef;
                }

                /**
                 * something like <code>targetType.class</code>
                 *
                 * @return STypeDef
                 */
                public STypeDef targetType() {
                        return targetType;
                }
        }

        /**
         * get non-static field
         */
        public static class GetField implements Value, Instruction, ReadOnly {
                private final LineCol lineCol;
                private final SFieldDef field;
                private final Value object;

                public GetField(SFieldDef field, Value object, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.field = field;
                        this.object = object;
                }

                public Value object() {
                        return object;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return field.type();
                }

                public SFieldDef field() {
                        return field;
                }
        }

        /**
         * get static field
         */
        public static class GetStatic implements Value, Instruction, ReadOnly {
                private final LineCol lineCol;
                private final SFieldDef field;

                public GetStatic(SFieldDef field, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.field = field;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return field.type();
                }

                public SFieldDef field() {
                        return field;
                }
        }

        /**
         * goto
         */
        public static class Goto implements Instruction {
                private final Instruction gotoIns;

                public Goto(Instruction gotoIns) {
                        this.gotoIns = gotoIns;
                }

                @Override
                public LineCol line_col() {
                        return null;
                }

                public Instruction gotoIns() {
                        return gotoIns;
                }
        }

        /**
         * ifEq (i == 0)
         */
        public static class IfEq implements Instruction {
                private final Value condition;
                private final Instruction gotoIns;
                private final LineCol lineCol;

                public IfEq(Value condition, Instruction gotoIns, LineCol lineCol) {
                        this.condition = condition;
                        this.gotoIns = gotoIns;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value condition() {
                        return condition;
                }

                public Instruction gotoIns() {
                        return gotoIns;
                }
        }

        /**
         * ifNe (if i != 0)
         */
        public static class IfNe implements Instruction {
                private final Value condition;
                private Instruction gotoIns;
                private final LineCol lineCol;

                public IfNe(Value condition, Instruction gotoIns, LineCol lineCol) {
                        this.condition = condition;
                        this.gotoIns = gotoIns;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value condition() {
                        return condition;
                }

                public Instruction gotoIns() {
                        return gotoIns;
                }

                public void setGotoIns(Instruction gotoIns) {
                        this.gotoIns = gotoIns;
                }
        }

        /**
         * invoke
         */
        public abstract static class Invoke implements Instruction, Value {
                private final LineCol lineCol;

                private SInvokable invokable;
                private final List<Value> arguments = new ArrayList<>();

                public Invoke(SInvokable invokable, LineCol lineCol) {
                        setInvokable(invokable);
                        this.lineCol = lineCol;
                }

                public void setInvokable(SInvokable invokable) {
                        this.invokable = invokable;
                }

                public SInvokable invokable() {
                        return invokable;
                }

                public List<Value> arguments() {
                        return arguments;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return invokable().getReturnType();
                }
        }

        /**
         * invoke dynamic
         */
        public static class InvokeDynamic extends Invoke {
                private final List<Value> indyArgs = new ArrayList<>();
                private final String methodName;
                private final STypeDef returnType;
                private int indyType;

                /**
                 * @param bootstrapMethod bootstrap method
                 * @param methodName      name of the method to invoke
                 * @param args            argument types
                 * @param returnType      method return type
                 * @param indyType        indy type
                 * @param lineCol         file line column
                 */
                public InvokeDynamic(
                        SInvokable bootstrapMethod,
                        String methodName,
                        List<Value> args,
                        STypeDef returnType,
                        int indyType,
                        LineCol lineCol) {

                        super(bootstrapMethod, lineCol);
                        this.methodName = methodName;
                        this.indyType = indyType;
                        this.arguments().addAll(args);
                        this.returnType = returnType;
                }

                @SuppressWarnings("unused")
                public List<Value> indyArgs() {
                        return indyArgs;
                }

                public int indyType() {
                        return indyType;
                }

                public String methodName() {
                        return methodName;
                }

                public STypeDef returnType() {
                        return returnType;
                }

                @Override
                public STypeDef type() {
                        return returnType();
                }
        }

        /**
         * invokeInterface
         */
        public static class InvokeInterface extends InvokeWithTarget {
                public InvokeInterface(Value target, SInvokable invokable, LineCol lineCol) {
                        super(target, invokable, lineCol);
                }
        }

        /**
         * invoke special
         */
        public static class InvokeSpecial extends InvokeWithTarget {
                public InvokeSpecial(Value target, SInvokable invokable, LineCol lineCol) {
                        super(target, invokable, lineCol);
                }
        }

        /**
         * invoke static
         */
        public static class InvokeStatic extends Invoke {
                public InvokeStatic(SInvokable invokable, LineCol lineCol) {
                        super(invokable, lineCol);
                }
        }

        /**
         * invoke special
         */
        public static class InvokeVirtual extends InvokeWithTarget {
                public InvokeVirtual(Value target, SInvokable invokable, LineCol lineCol) {
                        super(target, invokable, lineCol);
                }
        }

        /**
         * invoke
         */
        public abstract static class InvokeWithTarget extends Invoke implements Instruction, Value {
                private Value target;

                public InvokeWithTarget(Value target, SInvokable invokable, LineCol lineCol) {
                        super(invokable, lineCol);
                        this.target = target;
                }

                public void setTarget(Value target) {
                        this.target = target;
                }

                public Value target() {
                        return target;
                }
        }

        /**
         * logic and
         */
        public static class LogicAnd implements Value, Instruction {
                private final LineCol lineCol;
                private final Value b1;
                private final Value b2;

                public LogicAnd(Value b1, Value b2, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.b1 = b1;
                        this.b2 = b2;
                }

                public Value b1() {
                        return b1;
                }

                public Value b2() {
                        return b2;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return BoolTypeDef.get();
                }
        }

        /**
         * logic or
         */
        public static class LogicOr implements Value, Instruction {
                private final LineCol lineCol;
                private final Value b1;
                private final Value b2;

                public LogicOr(Value b1, Value b2, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.b1 = b1;
                        this.b2 = b2;
                }

                public Value b1() {
                        return b1;
                }

                public Value b2() {
                        return b2;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return BoolTypeDef.get();
                }
        }

        /**
         * monitor entry (synchronized)
         */
        public static class MonitorEnter implements Instruction {
                private final LineCol lineCol;
                private final Value valueToMonitor;
                private final int index;

                public MonitorEnter(Value valueToMonitor, SemanticScope scope, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.valueToMonitor = valueToMonitor;
                        LocalVariable localVariable = new LocalVariable(valueToMonitor.type(), false);
                        scope.putLeftValue(scope.generateTempName(), localVariable);
                        this.index = scope.getIndex(localVariable);
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value valueToMonitor() {
                        return valueToMonitor;
                }

                public int storeIndex() {
                        return index;
                }
        }

        /**
         * monitor entry (synchronized)
         */
        public static class MonitorExit implements Instruction {
                private final LineCol lineCol;
                private final MonitorEnter enterInstruction;

                public MonitorExit(MonitorEnter enterInstruction) {
                        this.lineCol = enterInstruction.line_col();
                        this.enterInstruction = enterInstruction;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public MonitorEnter enterInstruction() {
                        return enterInstruction;
                }
        }

        /**
         * create an object and construct it
         */
        public static class New implements Value, Instruction {
                private final SConstructorDef constructor;
                private final List<Value> args = new ArrayList<>();

                private final LineCol lineCol;

                public New(SConstructorDef constructor, LineCol lineCol) {
                        this.constructor = constructor;
                        this.lineCol = lineCol;
                }

                public SConstructorDef constructor() {
                        return constructor;
                }

                public List<Value> args() {
                        return args;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return constructor.declaringType();
                }
        }

        /**
         * create a new array
         */
        public static class NewArray implements Value {
                public static final int NewBoolArray = 4;
                public static final int NewCharArray = 5;
                public static final int NewFloatArray = 6;
                public static final int NewDoubleArray = 7;
                public static final int NewByteArray = 8;
                public static final int NewShortArray = 9;
                public static final int NewIntArray = 10;
                public static final int NewLongArray = 11;

                private final int mode;
                private final IntValue count;
                private final STypeDef type;
                private final int storeMode;
                private final List<Value> initValues = new ArrayList<>();

                public NewArray(IntValue count, int mode, int storeMode, STypeDef type) {
                        this.count = count;
                        this.mode = mode;
                        this.storeMode = storeMode;
                        this.type = type;
                }

                @Override
                public STypeDef type() {
                        return type;
                }

                public int mode() {
                        return mode;
                }

                public IntValue count() {
                        return count;
                }

                public int storeMode() {
                        return storeMode;
                }

                public List<Value> initValues() {
                        return initValues;
                }
        }

        /**
         * create an array list
         */
        public static class NewList implements Value {
                private final STypeDef type;
                private List<Value> initValues = new ArrayList<>();

                public NewList(STypeDef linkedListType) {
                        this.type = linkedListType;
                }

                @Override
                public STypeDef type() {
                        return type;
                }

                public List<Value> initValues() {
                        return initValues;
                }
        }

        /**
         * new map
         */
        public static class NewMap implements Value {
                private final STypeDef linkedListClass;
                private final LinkedHashMap<Value, Value> initValues = new LinkedHashMap<>();

                public NewMap(STypeDef linkedListClass) {
                        this.linkedListClass = linkedListClass;
                }

                @Override
                public STypeDef type() {
                        return linkedListClass;
                }

                public LinkedHashMap<Value, Value> initValues() {
                        return initValues;
                }
        }

        /**
         * nop
         */
        public static class Nop implements Instruction {
                @Override
                public LineCol line_col() {
                        return null;
                }
        }

        /**
         * one variable operation
         */
        public static class OneVarOp implements Value, Instruction {
                public static final int Ineg = 0x74;
                public static final int Lneg = 0x75;
                public static final int Fneg = 0x76;
                public static final int Dneg = 0x77;

                private final LineCol lineCol;
                private final int op;
                private final Value value;
                private final STypeDef type;

                public OneVarOp(Value value, int op, STypeDef type, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.op = op;
                        this.value = value;
                        this.type = type;
                }

                public Value value() {
                        return value;
                }

                public int op() {
                        return op;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return type;
                }
        }

        /**
         * pop from stack
         */
        public static class Pop implements Instruction {
                @Override
                public LineCol line_col() {
                        return null;
                }
        }

        /**
         * put non-static field
         */
        public static class PutField implements Instruction {
                private final Value value;
                private final SFieldDef field;
                private final Value obj;
                private final LineCol lineCol;

                public PutField(SFieldDef field, Value obj, Value value, LineCol lineCol) throws SyntaxException {
                        if (field.alreadyAssigned() && !field.canChange()) throw new SyntaxException(field + " cannot be assigned", lineCol);

                        this.field = field;
                        this.obj = obj;
                        this.value = value;
                        this.lineCol = lineCol;

                        field.assign();
                }

                public Value value() {
                        return value;
                }

                public SFieldDef field() {
                        return field;
                }

                public Value obj() {
                        return obj;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * put static field
         */
        public static class PutStatic implements Instruction {
                private final Value value;
                private final SFieldDef field;
                private final LineCol lineCol;

                public PutStatic(SFieldDef field, Value value, LineCol lineCol) throws SyntaxException {
                        if (field.alreadyAssigned() && !field.canChange()) throw new SyntaxException(field + " cannot be assigned", lineCol);

                        this.field = field;
                        this.value = value;
                        this.lineCol = lineCol;

                        field.assign();
                }

                public Value value() {
                        return value;
                }

                public SFieldDef field() {
                        return field;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * TALoad
         */
        public static class TALoad implements Value, Instruction, ReadOnly {
                public static int Baload = 0;
                public static int Saload = 0;
                public static int Iaload = 0;
                public static int Laload = 0;
                public static int Faload = 0;
                public static int Daload = 0;
                public static int Caload = 0;
                public static int Aaload = 0;

                private final LineCol lineCol;
                private final int mode;
                private final Value index;
                private final Value arr;

                public TALoad(Value arr, Value index, LineCol lineCol) {
                        this.arr = arr;
                        this.index = index;
                        this.lineCol = lineCol;

                        STypeDef type = ((SArrayTypeDef) arr.type()).type();
                        if (type instanceof BoolTypeDef || type instanceof ByteTypeDef) mode = Baload;
                        else if (type instanceof ShortTypeDef) mode = Saload;
                        else if (type instanceof IntTypeDef) mode = Iaload;
                        else if (type instanceof LongTypeDef) mode = Laload;
                        else if (type instanceof FloatTypeDef) mode = Faload;
                        else if (type instanceof DoubleTypeDef) mode = Daload;
                        else if (type instanceof CharTypeDef) mode = Caload;
                        else mode = Aaload;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return ((SArrayTypeDef) arr.type()).type();
                }

                public Value arr() {
                        return arr;
                }

                public int mode() {
                        return mode;
                }

                public Value index() {
                        return index;
                }
        }

        /**
         * tastore
         */
        public static class TAStore implements Instruction {
                public static final int IASTORE = 0x4f;
                public static final int LASTORE = 0x50;
                public static final int FASTORE = 0x51;
                public static final int DASTORE = 0x52;
                public static final int AASTORE = 0x53;
                public static final int BASTORE = 0x54;
                public static final int CASTORE = 0x55;
                public static final int SASTORE = 0x56;

                private final int mode;
                private final Value index;
                private final Value array;
                private final Value value;
                private final LineCol lineCol;

                public TAStore(Value array, int mode, Value index, Value value, LineCol lineCol) {
                        this.array = array;
                        this.mode = mode;
                        this.index = index;
                        this.value = value;
                        this.lineCol = lineCol;
                }

                public int mode() {
                        return mode;
                }

                public Value index() {
                        return index;
                }

                public Value array() {
                        return array;
                }

                public Value value() {
                        return value;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * this
         */
        public static class This implements Value, ReadOnly {
                private final STypeDef type;

                public This(STypeDef type) {
                        this.type = type;
                }

                @Override
                public STypeDef type() {
                        return type;
                }
        }

        /**
         * tload instruction
         */
        public static class TLoad implements Value, Instruction, ReadOnly {
                public static int Iload = 0x15;
                public static int Lload = 0x16;
                public static int Fload = 0x17;
                public static int Dload = 0x18;
                public static int Aload = 0x19;

                private final LineCol lineCol;
                private final LeftValue value;
                private final int mode;
                private final int index;

                public TLoad(LeftValue value, int index, LineCol lineCol) {
                        this.value = value;
                        this.lineCol = lineCol;

                        if (value.type() instanceof IntTypeDef || IntTypeDef.get().isAssignableFrom(value.type())) {
                                mode = Iload;
                        } else if (value.type() instanceof FloatTypeDef) {
                                mode = Fload;
                        } else if (value.type() instanceof LongTypeDef) {
                                mode = Lload;
                        } else if (value.type() instanceof DoubleTypeDef) {
                                mode = Dload;
                        } else if (value.type() instanceof BoolTypeDef) {
                                mode = Iload;
                        } else
                                mode = Aload;

                        this.index = index;
                }

                public TLoad(LeftValue value, SemanticScope scope, LineCol lineCol) {
                        this(value, scope.getIndex(value), lineCol);
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return value.type();
                }

                public LeftValue value() {
                        return value;
                }

                public int mode() {
                        return mode;
                }

                public int getIndex() {
                        return index;
                }
        }

        /**
         * return
         */
        public static class TReturn implements Instruction {
                public static final int Return = 0xb1;
                public static final int AReturn = 0xb0;
                public static final int DReturn = 0xaf;
                public static final int FReturn = 0xae;
                public static final int IReturn = 0xac;
                public static final int LReturn = 0xad;

                private Value value;
                private final int returnIns;
                private final LineCol lineCol;

                public TReturn(Value value, LineCol lineCol) {
                        this.value = value;
                        if (value == null) returnIns = Return;
                        else {

                                STypeDef returnType = value.type();
                                if (returnType.equals(IntTypeDef.get()) || returnType.equals(ShortTypeDef.get())
                                        || returnType.equals(ByteTypeDef.get()) || returnType.equals(BoolTypeDef.get())
                                        || returnType.equals(CharTypeDef.get())) this.returnIns = IReturn;
                                else if (returnType.equals(LongTypeDef.get())) this.returnIns = LReturn;
                                else if (returnType.equals(FloatTypeDef.get())) this.returnIns = FReturn;
                                else if (returnType.equals(DoubleTypeDef.get())) this.returnIns = DReturn;
                                else this.returnIns = AReturn;
                        }

                        this.lineCol = lineCol;
                }

                public void setReturnValue(Value value) {
                        this.value = value;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value value() {
                        return value;
                }

                public int returnIns() {
                        return returnIns;
                }
        }

        /**
         * ?store
         */
        public static class TStore implements Instruction {
                public static final int Astore = 0x3a;
                public static final int Dstore = 0x39;
                public static final int Fstore = 0x38;
                public static final int Lstore = 0x37;
                public static final int Istore = 0x36;

                private final LeftValue leftValue;
                private final Value newValue;
                private final int mode;
                private final LineCol lineCol;
                private final int index;

                public TStore(LeftValue leftValue, Value newValue, SemanticScope scope, LineCol lineCol) throws SyntaxException {
                        if (leftValue.alreadyAssigned() && !leftValue.canChange()) throw new SyntaxException(leftValue + " cannot be assigned", lineCol);

                        this.leftValue = leftValue;
                        this.lineCol = lineCol;
                        this.newValue = newValue;
                        if (newValue.type() instanceof IntTypeDef || IntTypeDef.get().isAssignableFrom(newValue.type())) {
                                mode = Istore;
                        } else if (newValue.type() instanceof FloatTypeDef) {
                                mode = Fstore;
                        } else if (newValue.type() instanceof LongTypeDef) {
                                mode = Lstore;
                        } else if (newValue.type() instanceof DoubleTypeDef) {
                                mode = Dstore;
                        } else if (newValue.type() instanceof BoolTypeDef) {
                                mode = Istore;
                        } else
                                mode = Astore;

                        index = scope.getIndex(leftValue);

                        leftValue.assign();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                public Value newValue() {
                        return newValue;
                }

                @SuppressWarnings("unused")
                public LeftValue leftValue() {
                        return leftValue;
                }

                public int mode() {
                        return mode;
                }

                public int index() {
                        return index;
                }
        }

        /**
         * add
         */
        public static class TwoVarOp implements Value, Instruction {
                public static final int Iadd = 0x60;
                public static final int Ladd = 0x61;
                public static final int Fadd = 0x62;
                public static final int Dadd = 0x63;

                public static final int Isub = 0x64;
                public static final int Lsub = 0x65;
                public static final int Fsub = 0x66;
                public static final int Dsub = 0x67;

                public static final int Imul = 0x68;
                public static final int Lmul = 0x69;
                public static final int Fmul = 0x6a;
                public static final int Dmul = 0x6b;

                public static final int Idiv = 0x6c;
                public static final int Ldiv = 0x6d;
                public static final int Fdiv = 0x6e;
                public static final int Ddiv = 0x6f;

                public static final int Irem = 0x70;
                public static final int Lrem = 0x71;
                public static final int Frem = 0x72;
                public static final int Drem = 0x73;

                public static final int Ishl = 0x78;
                public static final int Ishr = 0x7a;
                public static final int Iushr = 0x7c;
                public static final int Lshl = 0x79;
                public static final int Lshr = 0x7b;
                public static final int Lushr = 0x7d;

                public static final int Ior = 0x80;
                public static final int Lor = 0x81;

                public static final int Iand = 0x7e;
                public static final int Land = 0x7f;

                public static final int Ixor = 0x82;
                public static final int Lxor = 0x83;

                public static final int Dcmpg = 0x98;
                @SuppressWarnings("unused")
                public static final int Dcmpl = 0x97;
                public static final int Fcmpg = 0x96;
                @SuppressWarnings("unused")
                public static final int Fcmpl = 0x95;
                public static final int Lcmp = 0x94;

                private final LineCol lineCol;
                private final STypeDef resultType;
                private final Value a;
                private final Value b;
                private final int op;

                public TwoVarOp(Value a, Value b, int op, STypeDef resultType, LineCol lineCol) {
                        this.lineCol = lineCol;
                        this.resultType = resultType;
                        this.a = a;
                        this.b = b;
                        this.op = op;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public STypeDef type() {
                        return resultType;
                }

                public Value a() {
                        return a;
                }

                public Value b() {
                        return b;
                }

                public int op() {
                        return op;
                }
        }
}
