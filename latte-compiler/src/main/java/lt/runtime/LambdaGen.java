package lt.runtime;

import lt.compiler.LtBug;
import lt.dependencies.asm.ClassWriter;
import lt.dependencies.asm.FieldVisitor;
import lt.dependencies.asm.MethodVisitor;
import lt.dependencies.asm.Opcodes;
import lt.lang.function.Function;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * it's used to generate for an implementation for a functional interface or abstract class
 */
public class LambdaGen {
        private static final String FUNC_FIELD_NAME = "func";
        private static final String F_FIELD_NAME = "f";
        private static final String FIELD_DESC = "Ljava/lang/reflect/Field;";

        public static Map.Entry<String, byte[]> gen(Function f, Class<?> targetType) {
                Method abstractMethod = Dynamic.findAbstractMethod(targetType);
                Method funcMethod = f.getClass().getDeclaredMethods()[0];

                final String className = getLambdaName(targetType);
                String functionInternal = typeToInternalName(f.getClass().getInterfaces()[0]);
                String functionDesc = "L" + functionInternal + ";";

                // class X
                final ClassWriter classVisitor = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                String superClass;
                String[] interfaces;
                if (targetType.isInterface()) {
                        superClass = "java/lang/Object";
                        interfaces = new String[]{typeToInternalName(targetType)};
                } else {
                        superClass = typeToInternalName(targetType);
                        interfaces = new String[0];
                }
                classVisitor.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, className, null, superClass, interfaces);

                // func (field)
                FieldVisitor funcVisitor = classVisitor.visitField(Opcodes.ACC_PRIVATE, FUNC_FIELD_NAME, functionDesc, null, null);
                funcVisitor.visitEnd();

                // f (field Field)
                FieldVisitor fVisitor = classVisitor.visitField(Opcodes.ACC_PRIVATE, F_FIELD_NAME, typeToDesc(Field.class), null, null);
                fVisitor.visitEnd();

                MethodVisitor constructorVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + functionDesc + ")V", null, null);
                constructorVisitor.visitCode();
                // this
                visitThis(constructorVisitor);
                // invoke super
                constructorVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClass, "<init>", "()V", false);
                // this
                visitThis(constructorVisitor);
                // func (local)
                constructorVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                // this.func = func
                constructorVisitor.visitFieldInsn(Opcodes.PUTFIELD, className, FUNC_FIELD_NAME, functionDesc);
                // invoke var1.getClass()
                constructorVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                constructorVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                // getField('self')
                constructorVisitor.visitLdcInsn("self");
                constructorVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                // this.f = (field)
                visitThis(constructorVisitor);
                constructorVisitor.visitInsn(Opcodes.SWAP);
                constructorVisitor.visitFieldInsn(Opcodes.PUTFIELD, className, F_FIELD_NAME, FIELD_DESC);
                // return
                constructorVisitor.visitInsn(Opcodes.RETURN);
                constructorVisitor.visitMaxs(0, 0);
                constructorVisitor.visitEnd();

                // method
                MethodVisitor implMethod = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, abstractMethod.getName(), getDescFromMethod(abstractMethod), null, null);
                implMethod.visitCode();
                // this.f
                visitThis(implMethod);
                implMethod.visitFieldInsn(Opcodes.GETFIELD, className, F_FIELD_NAME, FIELD_DESC);
                // setAccessible(true)
                implMethod.visitLdcInsn(true);
                try {
                        implMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typeToInternalName(Field.class), "setAccessible", getDescFromMethod(Field.class.getMethod("setAccessible", boolean.class)), false);
                } catch (NoSuchMethodException e) {
                        throw new LtBug(e);
                }
                // this.f
                visitThis(implMethod);
                implMethod.visitFieldInsn(Opcodes.GETFIELD, className, F_FIELD_NAME, FIELD_DESC);
                // set(this.func, this)
                visitThis(implMethod);
                implMethod.visitFieldInsn(Opcodes.GETFIELD, className, FUNC_FIELD_NAME, functionDesc);
                visitThis(implMethod);
                try {
                        implMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typeToInternalName(Field.class), "set", getDescFromMethod(Field.class.getMethod("set", Object.class, Object.class)), false);
                } catch (NoSuchMethodException e) {
                        throw new LtBug(e);
                }

                // this.func
                visitThis(implMethod);
                implMethod.visitFieldInsn(Opcodes.GETFIELD, className, FUNC_FIELD_NAME, functionDesc);
                // each param
                for (int i = 1; i <= abstractMethod.getParameterTypes().length; ++i) {
                        visitLocal(implMethod, abstractMethod, i);
                        Class<?> param = abstractMethod.getParameterTypes()[i - 1];
                        if (param.isPrimitive()) {
                                try {
                                        boxPrimitive(param, implMethod);
                                } catch (NoSuchMethodException e) {
                                        throw new LtBug(e);
                                }
                        }
                }
                // invoke
                implMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, functionInternal, funcMethod.getName(), getDescFromMethod(funcMethod), true);
                // return?void?
                if (abstractMethod.getReturnType() == void.class) {
                        // void
                        implMethod.visitInsn(Opcodes.POP);
                        implMethod.visitInsn(Opcodes.RETURN);
                } else {
                        // return
                        if (abstractMethod.getReturnType().isPrimitive()) {
                                Class<?> returnType = abstractMethod.getReturnType();
                                castToPrimitive(returnType, implMethod);
                                if (returnType == long.class) {
                                        implMethod.visitInsn(Opcodes.LRETURN);
                                } else if (returnType == float.class) {
                                        implMethod.visitInsn(Opcodes.FRETURN);
                                } else if (returnType == double.class) {
                                        implMethod.visitInsn(Opcodes.DRETURN);
                                } else {
                                        implMethod.visitInsn(Opcodes.IRETURN);
                                }
                        } else {
                                // check type
                                if (abstractMethod.getReturnType().equals(Object.class)) {
                                        implMethod.visitInsn(Opcodes.ARETURN);
                                } else {
                                        checkcast(abstractMethod.getReturnType(), implMethod);
                                        implMethod.visitInsn(Opcodes.ARETURN);
                                }
                        }
                }
                implMethod.visitMaxs(0, 0);
                implMethod.visitEnd();

                classVisitor.visitEnd();
                return new Map.Entry<String, byte[]>() {
                        @Override
                        public String getKey() {
                                return className;
                        }

                        @Override
                        public byte[] getValue() {
                                return classVisitor.toByteArray();
                        }

                        @Override
                        public byte[] setValue(byte[] bytes) {
                                throw new UnsupportedOperationException();
                        }
                };
        }

        private static void castToPrimitive(Class<?> c, MethodVisitor visitor) throws NoSuchElementException {
                String name = "castTo";
                if (c == int.class) {
                        name += "Int";
                } else if (c == short.class) {
                        name += "Short";
                } else if (c == byte.class) {
                        name += "Byte";
                } else if (c == boolean.class) {
                        name += "Bool";
                } else if (c == float.class) {
                        name += "Float";
                } else if (c == long.class) {
                        name += "Long";
                } else if (c == double.class) {
                        name += "Double";
                } else if (c == char.class) {
                        name += "Char";
                } else {
                        throw new LtBug("unknown primitive " + c);
                }
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        typeToInternalName(LtRuntime.class), name,
                        "(Ljava/lang/Object;)" + typeToDesc(c), false);
        }

        private static void checkcast(Class<?> c, MethodVisitor visitor) {
                visitor.visitTypeInsn(Opcodes.CHECKCAST, typeToDesc(c));
        }

        private static void boxPrimitive(Class<?> c, MethodVisitor visitor) throws NoSuchMethodException {
                String owner;
                String name = "valueOf";
                String desc;
                if (c == int.class) {
                        owner = "java/lang/Integer";
                        desc = getDescFromMethod(Integer.class.getMethod(name, int.class));
                } else if (c == float.class) {
                        owner = "java/lang/Float";
                        desc = getDescFromMethod(Float.class.getMethod(name, float.class));
                } else if (c == long.class) {
                        owner = "java/lang/Long";
                        desc = getDescFromMethod(Long.class.getMethod(name, long.class));
                } else if (c == double.class) {
                        owner = "java/lang/Double";
                        desc = getDescFromMethod(Double.class.getMethod(name, double.class));
                } else if (c == byte.class) {
                        owner = "java/lang/Byte";
                        desc = getDescFromMethod(Byte.class.getMethod(name, byte.class));
                } else if (c == short.class) {
                        owner = "java/lang/Short";
                        desc = getDescFromMethod(Short.class.getMethod(name, short.class));
                } else if (c == boolean.class) {
                        owner = "java/lang/Boolean";
                        desc = getDescFromMethod(Boolean.class.getMethod(name, boolean.class));
                } else if (c == char.class) {
                        owner = "java/lang/Character";
                        desc = getDescFromMethod(Character.class.getMethod(name, char.class));
                } else {
                        throw new LtBug("unknown primitive type " + c);
                }
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
        }

        private static String typeToInternalName(Class<?> type) {
                return type.getName().replace('.', '/');
        }

        private static String typeToDesc(Class<?> type) {
                StringBuilder sb = new StringBuilder();
                if (type.isPrimitive()) {
                        if (type == int.class) sb.append("I");
                        else if (type == long.class) sb.append("J");
                        else if (type == short.class) sb.append("S");
                        else if (type == byte.class) sb.append("B");
                        else if (type == boolean.class) sb.append("Z");
                        else if (type == float.class) sb.append("F");
                        else if (type == double.class) sb.append("D");
                        else if (type == char.class) sb.append("C");
                        else if (type == void.class) sb.append("V"); // java 1.8 void is primitive
                        else throw new LtBug("unknown primitive: " + type);
                } else if (type.isArray()) {
                        Class<?> tmp = type;
                        while (tmp.isArray()) {
                                sb.append("[");
                                tmp = tmp.getComponentType();
                        }
                        sb.append(typeToDesc(tmp));
                } else if (type == void.class) {
                        sb.append("V"); // java 1.6 void is not primitive
                } else {
                        // object L...;
                        sb.append("L").append(typeToInternalName(type)).append(";");
                }
                return sb.toString();
        }

        private static String getDescFromMethod(Method method) {
                StringBuilder sb = new StringBuilder("(");
                for (Class<?> cls : method.getParameterTypes()) {
                        sb.append(typeToDesc(cls));
                }
                sb.append(")").append(typeToDesc(method.getReturnType()));
                return sb.toString();
        }

        private static void visitLocal(MethodVisitor visitor, Method method, int local) {
                Class<?> c = method.getParameterTypes()[local - 1];
                if (c.isPrimitive()) {
                        int code;
                        if (c == float.class) {
                                code = Opcodes.FLOAD;
                        } else if (c == double.class) {
                                code = Opcodes.DLOAD;
                        } else if (c == long.class) {
                                code = Opcodes.LLOAD;
                        } else {
                                code = Opcodes.ILOAD;
                        }
                        visitor.visitVarInsn(code, local);
                } else {
                        visitor.visitVarInsn(Opcodes.ALOAD, local);
                }
        }

        private static void visitThis(MethodVisitor visitor) {
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
        }

        private static synchronized String getLambdaName(Class<?> targetType) {
                int i = 0;
                while (true) {
                        try {
                                Class.forName(targetType.getSimpleName() + "$Latte$lambda$" + i);
                        } catch (ClassNotFoundException e) {
                                break;
                        }
                }
                return targetType.getSimpleName() + "$Latte$lambda$" + i;
        }
}
