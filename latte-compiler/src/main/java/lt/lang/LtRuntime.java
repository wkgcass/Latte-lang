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

package lt.lang;

import lt.lang.function.Function;
import lt.lang.function.Function1;
import lt.repl.ScriptCompiler;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Defines Latte Runtime behavior.
 * The Runtime provides type cast, field getting and setting, comparison, ref comparison,
 * <b>is</b> and <b>not</b> operator behavior, wrapping object for throwing, hashCode retrieving.
 */
public class LtRuntime {
        /**
         * multiply
         */
        public static final String multiply = "multiply";
        /**
         * divide
         */
        public static final String divide = "divide";
        /**
         * remainder
         */
        public static final String remainder = "remainder";
        /**
         * add
         */
        public static final String add = "add";
        /**
         * subtract
         */
        public static final String subtract = "subtract";
        /**
         * shiftLeft
         */
        public static final String shiftLeft = "shiftLeft";
        /**
         * shiftRight
         */
        public static final String shiftRight = "shiftRight";
        /**
         * unsignedShiftRight
         */
        public static final String unsignedShiftRight = "unsignedShiftRight";
        /**
         * gt
         */
        public static final String gt = "gt";
        /**
         * lt
         */
        public static final String lt = "lt";
        /**
         * ge
         */
        public static final String ge = "ge";
        /**
         * le
         */
        public static final String le = "le";
        /**
         * equal
         */
        public static final String equal = "equal";
        /**
         * notEqual
         */
        public static final String notEqual = "notEqual";
        /**
         * and
         */
        public static final String and = "and";
        /**
         * xor
         */
        public static final String xor = "xor";
        /**
         * or
         */
        public static final String or = "or";
        /**
         * the lambda function map. maps "required class" to "create the required object"
         */
        private static final Map<Class<?>, Function1<Object, Object>> lambdaFunctionMap = new WeakHashMap<>();

        /**
         * Check whether the given type is {@link Integer} {@link Short}
         * {@link Byte} {@link Character} {@link Long} {@link Boolean}
         * {@link Float} {@link Double}.
         *
         * @param type the type to check.
         * @return true if it's a box type.
         */
        private static boolean isBoxType(Class<?> type) {
                return type.equals(Integer.class) || type.equals(Short.class) || type.equals(Byte.class) || type.equals(Character.class)
                        || type.equals(Long.class) || type.equals(Boolean.class) || type.equals(Float.class) || type.equals(Double.class);
        }

        /**
         * Cast the object to a {@link Throwable}. If the object is instance of throwable,
         * it's directly returned. Otherwise, a {@link Wrapper} object would be returned.
         *
         * @param o the object to cast.
         * @return a Throwable object.
         */
        public static Throwable castToThrowable(Object o) {
                if (o instanceof Throwable) return (Throwable) o;
                return new Wrapper(o);
        }

        /**
         * generate {@link ClassCastException} for throwing.
         *
         * @param o    the object to cast.
         * @param type the target type.
         * @return {@link ClassCastException}
         */
        private static ClassCastException generateClassCastException(Object o, Class<?> type) {
                return new ClassCastException("Cannot cast " +
                        (o == null ? "null" : o.getClass().getName()) +
                        " to " + type.getName());
        }

        /**
         * Cast the object to given type.
         *
         * @param o          the object to cast.
         * @param targetType the type that the object cast to.
         * @return the casting result.
         * @throws Exception maybe {@link ClassCastException} if the cast fails,
         *                   or some errors when casting.
         */
        public static Object cast(Object o, Class<?> targetType) throws Throwable {
                if (targetType.isInstance(o)) return o;

                if (o == null) {
                        if (targetType.isPrimitive()) {
                                throw generateClassCastException(null, targetType);
                        } else {
                                return null;
                        }
                }

                if (isBoxType(targetType)) {
                        if (targetType.equals(Integer.class)) {
                                return castToInt(o);
                        } else if (targetType.equals(Short.class)) {
                                return castToShort(o);
                        } else if (targetType.equals(Byte.class)) {
                                return castToByte(o);
                        } else if (targetType.equals(Character.class)) {
                                return castToChar(o);
                        } else if (targetType.equals(Long.class)) {
                                return castToLong(o);
                        } else if (targetType.equals(Boolean.class)) {
                                return castToBool(o);
                        } else if (targetType.equals(Float.class)) {
                                return castToFloat(o);
                        } else if (targetType.equals(Double.class)) {
                                return castToDouble(o);
                        } else throw new RuntimeException("unknown box type " + targetType);
                }
                if (targetType.equals(int.class)) return castToInt(o);
                if (targetType.equals(short.class)) return castToShort(o);
                if (targetType.equals(byte.class)) return castToByte(o);
                if (targetType.equals(char.class)) return castToChar(o);
                if (targetType.equals(long.class)) return castToLong(o);
                if (targetType.equals(boolean.class)) return castToBool(o);
                if (targetType.equals(float.class)) return castToFloat(o);
                if (targetType.equals(double.class)) return castToDouble(o);
                if (targetType.isAnnotationPresent(Implicit.class)) {
                        Constructor<?> con = targetType.getConstructors()[0];
                        Class<?> paramType = con.getParameterTypes()[0];
                        if (paramType.isInstance(o)) {
                                return con.newInstance(o);
                        }
                }
                if (targetType.isArray()) {
                        if (o instanceof java.util.List) {
                                Class<?> component = targetType.getComponentType();
                                java.util.List<?> list = (java.util.List<?>) o;
                                Object arr = Array.newInstance(component, list.size());

                                for (int cursor = 0; cursor < list.size(); ++cursor) {
                                        Object elem = list.get(cursor);
                                        Array.set(arr, cursor, cast(elem, component));
                                }

                                return arr;
                        }
                } else if (o instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<Object, Object> map = (java.util.Map) o;
                        boolean canInject = true;
                        for (Object tmp : map.keySet()) {
                                if (!(tmp instanceof String)) {
                                        canInject = false;
                                        break;
                                }
                        }
                        if (canInject) {
                                Object targetNewInstance = null;
                                try {
                                        targetNewInstance = targetType.newInstance();
                                } catch (Exception ignore) {
                                }
                                if (targetNewInstance != null) {
                                        for (Map.Entry entry : map.entrySet()) {
                                                String k = (String) entry.getKey();
                                                Object v = entry.getValue();
                                                putField(targetNewInstance, k, v, LtRuntime.class);
                                        }
                                        return targetNewInstance;
                                }
                        }
                } else if (o instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) o;
                        Object targetNewInstance = null;
                        try {
                                targetNewInstance = targetType.newInstance();
                        } catch (Exception ignore) {
                        }
                        if (targetNewInstance != null) {
                                for (Object item : list) {
                                        Dynamic.invoke(new Dynamic.InvocationState(), targetType, targetNewInstance, null,
                                                LtRuntime.class, "add", new boolean[]{false}, new Object[]{item});
                                }
                                return targetNewInstance;
                        }
                } else if (Dynamic.isFunctionalAbstractClass(targetType)
                        || Dynamic.isFunctionalInterface(targetType)) {
                        if (lambdaFunctionMap.containsKey(targetType)) {
                                return lambdaFunctionMap.get(targetType).apply(o);
                        }

                        if (o instanceof Function) {
                                Method method = Dynamic.findAbstractMethod(targetType);
                                Method funcMethod = o.getClass().getDeclaredMethods()[0];
                                if (method.getParameterCount() == funcMethod.getParameterCount()) {
                                        int i = 0;
                                        while (true) {
                                                try {
                                                        Class.forName(targetType.getSimpleName() + "$Latte$lambda$" + i);
                                                } catch (ClassNotFoundException e) {
                                                        break;
                                                }
                                        }
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("class ").append(targetType.getSimpleName()).append("$Latte$lambda$").append(i)
                                                .append("(func:").append(
                                                getLatteTypeName(funcMethod.getDeclaringClass().getInterfaces()[0].getName()))
                                                .append("):").append(
                                                getLatteTypeName(targetType.getName())).append("\n")
                                                .append("    ").append(method.getName()).append("(");
                                        boolean isFirst = true;
                                        int index = 0;
                                        for (Class<?> param : method.getParameterTypes()) {
                                                if (isFirst) isFirst = false;
                                                else sb.append(",");
                                                sb.append("p").append(index++).append(":").append(
                                                        getLatteTypeName(param.getName()));
                                        }
                                        sb.append("):").append(
                                                getLatteTypeName(method.getReturnType().getName())).append("\n")
                                                .append("        func.self = this\n").append("        ");
                                        if (method.getReturnType() != void.class) sb.append("return ");
                                        sb.append("func.apply(");
                                        isFirst = true;
                                        for (int j = 0; j < index; ++j) {
                                                if (isFirst) isFirst = false;
                                                else sb.append(",");
                                                sb.append("p").append(j);
                                        }
                                        sb.append(")\n");

                                        ClassLoader targetTypeCL = targetType.getClassLoader();
                                        if (targetTypeCL == null) {
                                                targetTypeCL = Thread.currentThread().getContextClassLoader();
                                        }
                                        @SuppressWarnings("unchecked")
                                        Class<?> cls = ((java.util.List<Class<?>>) Utils.eval(
                                                targetTypeCL,
                                                sb.toString())).get(0);
                                        Constructor<?> con = cls.getConstructor(funcMethod.getDeclaringClass().getInterfaces()[0]);
                                        Function1<Object, Object> func = con::newInstance;
                                        lambdaFunctionMap.put(targetType, func); // put into map
                                        return func.apply(o);
                                }
                        }
                }// else throw new LtBug("unsupported type cast (targetType:" + targetType.getName() + ", o:" + o.getClass().getName() + ")");
                throw generateClassCastException(o, targetType);
        }

        private static String getLatteTypeName(String javaTypeName) {
                if (javaTypeName.equals("boolean")) return "bool";
                if (javaTypeName.equals("void")) return "Unit";
                return javaTypeName.replace(".", "::");
        }

        /**
         * Cast the object to int value. Only {@link Number} can be cast to int.
         *
         * @param o the object to cast.
         * @return int value.
         */
        public static int castToInt(Object o) {
                if (o instanceof Number) return ((Number) o).intValue();
                if (o instanceof Character) return (Character) o;
                throw generateClassCastException(o, int.class);
        }

        /**
         * Cast the object to long value. Only {@link Number} can be cast to long.
         *
         * @param o the object to cast.
         * @return long value.
         */
        public static long castToLong(Object o) {
                if (o instanceof Number) return ((Number) o).longValue();
                throw generateClassCastException(o, long.class);
        }

        /**
         * Cast the object to short value. Only {@link Number} can be cast to short.
         *
         * @param o the object to cast.
         * @return short value.
         */
        public static short castToShort(Object o) {
                if (o instanceof Number) return ((Number) o).shortValue();
                throw generateClassCastException(o, short.class);
        }

        /**
         * Cast the object to byte value. Only {@link Number} can be cast to byte.
         *
         * @param o the object to cast.
         * @return byte value.
         */
        public static byte castToByte(Object o) {
                if (o instanceof Number) return ((Number) o).byteValue();
                throw generateClassCastException(o, byte.class);
        }

        /**
         * Cast the object to float value. Only {@link Number} can be cast to float.
         *
         * @param o the object to cast.
         * @return float value.
         */
        public static float castToFloat(Object o) {
                if (o instanceof Number) return ((Number) o).floatValue();
                throw generateClassCastException(o, float.class);
        }

        /**
         * Cast the object to double value. Only {@link Number} can be cast to double.
         *
         * @param o the object to cast.
         * @return double value.
         */
        public static double castToDouble(Object o) {
                if (o instanceof Number) return ((Number) o).doubleValue();
                throw generateClassCastException(o, double.class);
        }

        /**
         * Cast the object to boolean value. All types can be cast to bool.
         * If the object is null or Unit, the result is false.
         * Else if the object is {@link Boolean}, the result is the unboxing value.
         * Else if the object is {@link Number}, the result is number != 0.
         * Else the result is true.
         *
         * @param o the object to cast.
         * @return bool value.
         * @throws Throwable exceptions
         */
        public static boolean castToBool(Object o) throws Throwable {
                // check null and Unit
                if (o == null || o instanceof Unit) return false;
                // check Boolean object
                if (o instanceof Boolean) return (Boolean) o;
                // check number not 0
                if (o instanceof Number) return ((Number) o).doubleValue() != 0;
                // check Character
                if (o instanceof Character) return (Character) o != 0;
                // check `isEmpty()`
                try {
                        Method m = o.getClass().getMethod("isEmpty");
                        if (m.getReturnType().equals(boolean.class) || m.getReturnType().equals(Boolean.class)) {
                                try {
                                        Object res = m.invoke(o);
                                        return res != null && !(boolean) res;
                                } catch (InvocationTargetException e) {
                                        throw e.getTargetException();
                                }
                        }
                } catch (NoSuchMethodException ignore) {
                }
                // otherwise return true
                return true;
        }

        /**
         * Cast the object to char.
         * If the object is {@link Number}, get the {@link Number#intValue()} and cast to char.
         * Else if the object is {@link Character}, the result is the unboxing value.
         * Else if the object is CharSequence and the length is 1, the result is {@link CharSequence#charAt(int)}(0).
         * Else throw a {@link ClassCastException}.
         *
         * @param o the object to cast.
         * @return char value.
         */
        public static char castToChar(Object o) {
                if (o instanceof Number) return (char) ((Number) o).intValue();
                if (o instanceof Character) return (Character) o;
                if (o instanceof CharSequence && ((CharSequence) o).length() == 1) return ((CharSequence) o).charAt(0);
                throw generateClassCastException(o, char.class);
        }

        private static void throwNonRuntime(Dynamic.InvocationState state, Throwable t) throws Throwable {
                if (state.methodFound && !(t instanceof LtRuntimeException)) throw t;
        }

        /**
         * get field value.<br>
         * if field not found , then the method would try to invoke get(fieldName)<br>
         * or the method would return <tt>Unit</tt>
         *
         * @param o           object
         * @param fieldName   field name
         * @param callerClass caller class
         * @return the value or Unit
         * @throws Throwable exceptions
         */
        public static Object getField(Object o, String fieldName, Class<?> callerClass) throws Throwable {
                if (o == null) throw new NullPointerException("null." + fieldName + " not exist");
                if (o.equals(Unit.get())) throw new IllegalArgumentException("Unit." + fieldName + " not exist");
                if (o.getClass().isArray()) {
                        if (fieldName.equals("length")) {
                                return Array.getLength(o);
                        } else if (fieldName.startsWith("_")) {
                                try {
                                        int index = Integer.parseInt(fieldName.substring(1));
                                        return Array.get(o, index);
                                } catch (NumberFormatException ignore) {
                                }
                        }
                        return Unit.get();
                }

                ExceptionContainer ec = new ExceptionContainer();
                // try to get field
                try {
                        Field f = o.getClass().getDeclaredField(fieldName);
                        if (haveAccess(f.getModifiers(), o.getClass(), callerClass)) {
                                f.setAccessible(true);
                                return f.get(o);
                        } else {
                                ec.add("Cannot access " + o.getClass().getName() + "#" + fieldName + " from " + callerClass);
                        }
                } catch (Throwable ignore) {
                        ec.add("Cannot find field " + o.getClass().getName() + "#" + fieldName);
                }

                Dynamic.InvocationState invocationState = new Dynamic.InvocationState();
                invocationState.fromField = true;

                // try to find `fieldName()`
                try {
                        return Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass, fieldName, new boolean[0], new Object[0]);
                } catch (Throwable t) {
                        throwNonRuntime(invocationState, t);
                        ec.add("Cannot invoke method " + o.getClass().getName() + "#" + fieldName + "()\n\t" + t.getMessage());
                }
                // try to find `getFieldName()`
                String getter = null;
                try {
                        getter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        return Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass, getter, new boolean[0], new Object[0]);
                } catch (Throwable t) {
                        throwNonRuntime(invocationState, t);
                        assert getter != null;
                        ec.add("Cannot invoke method " + o.getClass().getName() + "#" + getter + "()\n\t" + t.getMessage());
                }
                // try _number
                if (fieldName.startsWith("_")) {
                        try {
                                int i = Integer.parseInt(fieldName.substring(1));
                                try {
                                        return Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass, "get", new boolean[]{true}, new Object[]{i});
                                } catch (Throwable t) {
                                        throwNonRuntime(invocationState, t);
                                        ec.add("Cannot invoke method " + o.getClass().getName() + "#get(" + i + ")\n\t" + t.getMessage());
                                }
                        } catch (NumberFormatException ignore) {
                                ec.add("Field name is not `_{int}`, cannot be transformed into #get({int})");
                        }
                }
                // try to find `get(fieldName)`
                try {
                        return Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass, "get", new boolean[]{false}, new Object[]{fieldName});
                } catch (Throwable t) {
                        throwNonRuntime(invocationState, t);
                        ec.add("Cannot invoke method " + o.getClass().getName() + "#get(" + fieldName + ")\n\t" + t.getMessage());
                }
                ec.throwIfNotEmpty(fieldName, NoSuchFieldException::new);
                // won't reach here
                return null;
        }

        /**
         * retrieve package name of the class object.
         * first try to get name from {@link Class#getPackage()}, if the result is null
         * then extract the package name from the class name
         *
         * @param c class object
         * @return package name
         */
        private static String getPackage(Class<?> c) {
                if (c.getPackage() == null) {
                        String clsName = c.getName();
                        if (clsName.contains(".")) {
                                return clsName.substring(clsName.lastIndexOf("."));
                        } else {
                                return "";
                        }
                } else {
                        return c.getPackage().getName();
                }
        }

        /**
         * check whether the caller can have access to the class(or its members).
         *
         * @param modifiers modifiers
         * @param target    target class
         * @param caller    caller class
         * @return true if can access, false otherwise
         */
        public static boolean haveAccess(int modifiers, Class<?> target, Class<?> caller) {
                if (Modifier.isPrivate(modifiers)) {
                        // private
                        if (!caller.equals(target)) return false;
                } else if (Modifier.isProtected(modifiers)) {
                        // protected
                        if (!getPackage(target).equals(getPackage(caller)) && !target.isAssignableFrom(caller))
                                return false;
                } else if (!Modifier.isPublic(modifiers)) {
                        // package access
                        if (!getPackage(target).equals(getPackage(caller))) return false;
                }
                return true;
        }

        /**
         * put field.<br>
         * if field not found , then the method would try to invoke set(fieldName, value)<br>
         * the method calls {@link Dynamic#invoke(Class, Object, Object, Class, String, boolean[], Object[])}, and <code>set(fieldName,value)</code> may be changed to <code>put(fieldName, value)</code>
         *
         * @param o           object
         * @param fieldName   field name
         * @param value       the value to set
         * @param callerClass caller class
         * @throws Throwable exceptions
         */
        public static void putField(Object o, String fieldName, Object value, Class<?> callerClass) throws Throwable {
                if (o == null) throw new NullPointerException("null." + fieldName + " not exist");
                if (o.equals(Unit.get())) throw new IllegalArgumentException("Unit." + fieldName + " not exist");
                // try to put field
                ExceptionContainer ec = new ExceptionContainer();
                try {
                        Field f = o.getClass().getDeclaredField(fieldName);
                        if (haveAccess(f.getModifiers(), o.getClass(), callerClass)) {
                                f.setAccessible(true);
                                f.set(o, cast(value, f.getType()));
                                return;
                        } else {
                                ec.add("Cannot access " + o.getClass().getName() + "#" + fieldName + " from " + callerClass);
                        }
                } catch (Throwable ignore) {
                        ec.add("Cannot find field " + o.getClass().getName() + "#" + fieldName);
                }

                Dynamic.InvocationState invocationState = new Dynamic.InvocationState();
                invocationState.fromField = true;

                // try `setFieldName(value)`
                String setter = null;
                try {
                        setter = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass, setter, new boolean[]{false}, new Object[]{value});
                } catch (Throwable t) {
                        throwNonRuntime(invocationState, t);
                        assert setter != null;
                        ec.add("Cannot invoke method " + o.getClass().getName() + "#" + setter + "(...)\n\t" + t.getMessage());
                        // try to find `set(fieldName,value)`
                        // invoke dynamic would try to find set then try to find put
                        try {
                                Dynamic.invoke(invocationState, o.getClass(), o, null, callerClass,
                                        "set",
                                        new boolean[]{false, false},
                                        new Object[]{fieldName, value});
                        } catch (Throwable t2) {
                                throwNonRuntime(invocationState, t2);
                                ec.add("Cannot invoke method " + o.getClass().getName() + "#set(" + fieldName + ",...)\n\t" + t2.getMessage());
                                ec.throwIfNotEmpty(fieldName, NoSuchFieldException::new);
                        }
                }
        }

        /**
         * if a &gt; b then return true.
         */
        public static final int COMPARE_MODE_GT = 0b001;
        /**
         * if a == b then return true.
         */
        public static final int COMPARE_MODE_EQ = 0b010;
        /**
         * if a &lt; b then return true.
         */
        public static final int COMPARE_MODE_LT = 0b100;

        /**
         * change compare result into boolean
         *
         * @param result compare result.
         * @param mode   {@link #COMPARE_MODE_EQ} {@link #COMPARE_MODE_GT} {@link #COMPARE_MODE_LT}, can be linked with + or | operator
         * @return boolean value
         */
        public static boolean compare(int result, int mode) {
                // mode is EQ
                if ((mode & COMPARE_MODE_EQ) == COMPARE_MODE_EQ && result == 0) return true;
                // mode is GT
                if ((mode & COMPARE_MODE_GT) == COMPARE_MODE_GT && result > 0) return true;
                // mode is LT
                if ((mode & COMPARE_MODE_LT) == COMPARE_MODE_LT && result < 0) return true;
                // otherwise
                return false;
        }

        /**
         * compare two refs
         *
         * @param a a
         * @param b b
         * @return true or false
         */
        public static boolean compareRef(Object a, Object b) {
                if (a == b) return true;
                //
                return false;
        }

        /**
         * <code>is</code> operator<br>
         *
         * @param a           a
         * @param b           b
         * @param callerClass caller class
         * @return true/false
         * @throws Throwable exceptions
         */
        public static boolean is(Object a, Object b, @SuppressWarnings("unused") Class<?> callerClass) throws Throwable {
                if (a == null && b == null) return true;
                // not both a and b are null
                if (a == null || b == null) return false;
                // a and b are not null
                if (a == b || a.equals(b)) return true;
                // a!=b and a.equals(b) is false
                return false;
        }

        /**
         * <code>not</code> operator
         *
         * @param a           a
         * @param b           b
         * @param callerClass caller class
         * @return true/false
         * @throws Throwable exceptions
         */
        public static boolean not(Object a, Object b, @SuppressWarnings("unused") Class<?> callerClass) throws Throwable {
                if (a == null && b == null) return false;
                // not both a and b are null
                if (a == null || b == null) return true;
                // a and b are not null
                if (a == b || a.equals(b)) return false;
                // a!=b and a.equals(b) is false
                return true;
        }

        /**
         * get wrapped object in {@link Wrapper#object} or simply return the throwable object
         *
         * @param t the wrapper object or other throwable
         * @return the retrieved object
         */
        public static Object throwableWrapperObject(Throwable t) {
                if (t instanceof Wrapper) return ((Wrapper) t).object;
                return t;
        }

        /**
         * get hash code of a object
         *
         * @param o the object
         * @return hash code of the object (or 0 if it's null)
         */
        @SuppressWarnings("unused")
        public static int getHashCode(Object o) {
                if (o == null) return 0;
                return o.hashCode();
        }

        /**
         * destruct the object into a list of values
         *
         * @param count         destruct result size count
         * @param destructClass the class that defines method `unapply(o)`
         * @param o             the object to destruct
         * @param invoker       the caller class
         * @return a list of values
         * @throws Throwable any exceptions
         */
        public static List<?> destruct(int count, Class<?> destructClass, Object o, Class<?> invoker) throws Throwable {
                if (o == null) throw new LtRuntimeException("null cannot be destructed");
                if (destructClass == null) destructClass = o.getClass();
                Method method = Dynamic.findMethod(invoker, destructClass, null, "unapply", new boolean[1], new Object[]{o});
                if (method == null)
                        throw new LtRuntimeException("cannot unapply " + o.getClass().getName() + " with " + destructClass.getName());
                if (!List.class.isAssignableFrom(method.getReturnType()) && !method.getReturnType().isAssignableFrom(List.class))
                        throw new LtRuntimeException("unapply result should be java::util::List");
                List<?> res;
                try {
                        Object r = method.invoke(null, o);
                        if (r instanceof List) {
                                res = (List<?>) r;
                        } else throw new LtRuntimeException("unapply result is not List");
                } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                }
                if (res.size() != count) return null;
                return res;
        }

        private static final Map<String, Object> requiredObjects = new HashMap<>();

        /**
         * run a script and retrieve the script result. One file would only be run for only once.
         * The result value would be recorded, and the value would be retrieved when required.
         *
         * @param callerClass caller class
         * @param file        file. use cp:xx to retrieve from ClassPath
         * @return the script result
         * @throws Throwable throwable
         */
        public static Object require(Class<?> callerClass, String file) throws Throwable {
                ScriptCompiler sc = new ScriptCompiler(callerClass.getClassLoader());
                file = file.trim();

                String tmp = file;

                // `file` format
                if (file.startsWith("cp:")) {
                        tmp = tmp.substring("cp:".length()).trim();
                        if (tmp.startsWith("/")) {
                                tmp = tmp.substring(1);
                        }
                        file = "cp:" + tmp;
                }

                // get reader
                Reader r;
                if (file.startsWith("cp:")) {
                        // get from recorder
                        if (requiredObjects.containsKey(file)) return requiredObjects.get(file);

                        // get reader
                        ClassLoader loader = callerClass.getClassLoader();
                        InputStream is = loader.getResourceAsStream(tmp);
                        if (is == null) {
                                throw new RuntimeException("cannot find " + file + " in class loader " + loader);
                        }
                        r = new InputStreamReader(is);
                } else {
                        // get from recorder
                        if (requiredObjects.containsKey(file)) return requiredObjects.get(file);

                        // get reader
                        r = new FileReader(file);
                }

                // get script file name
                if (tmp.contains("/")) {
                        tmp = tmp.substring(tmp.indexOf("/") + 1);
                }
                if (tmp.contains("\\")) {
                        tmp = tmp.substring(tmp.indexOf("\\") + 1);
                }

                // compile and run
                Object o = sc.compile(tmp, r)
                        .run().getResult();
                requiredObjects.put(file, o);
                return o;
        }
}
