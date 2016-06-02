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

import lt.compiler.LtBug;
import lt.lang.function.Function;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

/**
 * invoke dynamic
 */
public class Dynamic {
        @SuppressWarnings("unused")
        public static final int GET_FIELD = 1;
        @SuppressWarnings("unused")
        public static final int GET_STATIC = 2;
        @SuppressWarnings("unused")
        public static final int PUT_FIELD = 3;
        @SuppressWarnings("unused")
        public static final int PUT_STATIC = 4;
        @SuppressWarnings("unused")
        public static final int INVOKE_VIRTUAL = 5;
        public static final int INVOKE_STATIC = 6;
        public static final int INVOKE_SPECIAL = 7;
        @SuppressWarnings("unused")
        public static final int NEW_INVOKE_SPECIAL = 8;
        @SuppressWarnings("unused")
        public static final int INVOKE_INTERFACE = 9;

        private static final int PRIMITIVE_BOX_CAST_BASE = 233;

        private Dynamic() {
        }

        private static MethodHandle methodHandle;

        static {
                try {
                        methodHandle = MethodHandles.lookup().findStatic(Dynamic.class, "invoke", MethodType.methodType(
                                Object.class, Class.class, Object.class, Class.class, String.class, boolean[].class, Object[].class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                }
        }

        /**
         * the bootstrap method for Latte Dynamic
         *
         * @param lookup     lookup
         * @param methodName method name
         * @param methodType methodType (class, object, arguments)
         * @return generated call site
         * @throws NoSuchMethodException  exception
         * @throws IllegalAccessException exception
         */
        @SuppressWarnings("unused")
        public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
                boolean[] primitives = new boolean[methodType.parameterCount() - 2];
                for (int i = 0; i < primitives.length; ++i) {
                        primitives[i] = methodType.parameterType(i + 2).isPrimitive();
                }
                MethodHandle mh = MethodHandles.insertArguments(methodHandle, 2, lookup.lookupClass(), methodName, primitives);
                mh = mh.asCollector(Object[].class, methodType.parameterCount() - 2).asType(methodType);

                MutableCallSite mCallSite = new MutableCallSite(mh);
                mCallSite.setTarget(mh);
                return mCallSite;
        }

        private static void fillMethodCandidates(Class<?> c, Class<?> invoker, String method, Object[] args, List<Method> methodList, boolean onlyStatic) {
                out:
                for (Method m : c.getDeclaredMethods()) {
                        if (!m.getName().equals(method)) continue;
                        if (m.getParameterCount() != args.length) continue;

                        // access check
                        if (Modifier.isPrivate(m.getModifiers())) {
                                // private
                                if (!invoker.equals(c)) continue;
                        } else if (Modifier.isProtected(m.getModifiers())) {
                                // protected
                                if (!c.getPackage().equals(invoker.getPackage()) && !c.isAssignableFrom(invoker)) continue;
                        } else if (!Modifier.isPublic(m.getModifiers())) {
                                // package access
                                if (!c.getPackage().equals(invoker.getPackage())) continue;
                        }

                        if (onlyStatic) {
                                if (!Modifier.isStatic(m.getModifiers())) continue;
                        }

                        // else public
                        // do nothing

                        for (int i = 0; i < args.length; ++i) {
                                if (!m.getParameterTypes()[i].isInstance(args[i]) &&
                                        (
                                                // null can be assigned to any reference type
                                                m.getParameterTypes()[i].isPrimitive()
                                                        ||
                                                        args[i] != null
                                        )) {
                                        // is not instance
                                        // check primitive
                                        Class<?> cls = m.getParameterTypes()[i];

                                        Object obj = args[i];
                                        if (null == obj) continue;

                                        if (cls.equals(int.class)) {
                                                if (!(obj instanceof Integer)) continue out;
                                        } else if (cls.equals(short.class)) {
                                                if (!(obj instanceof Short)) continue out;
                                        } else if (cls.equals(byte.class)) {
                                                if (!(obj instanceof Byte)) continue out;
                                        } else if (cls.equals(boolean.class)) {
                                                if (!(obj instanceof Boolean)) continue out;
                                        } else if (cls.equals(char.class)) {
                                                if (!(obj instanceof Character)) continue out;
                                        } else if (cls.equals(long.class)) {
                                                if (!(obj instanceof Long)) continue out;
                                        } else if (cls.equals(double.class)) {
                                                if (!(obj instanceof Double)) continue out;
                                        } else if (cls.equals(float.class)) {
                                                if (!(obj instanceof Float)) continue out;
                                        } else if (cls.isArray()) {
                                                if (!(obj instanceof java.util.List)) continue out;
                                        } else if (cls.isInterface() && isFunctionalInterface(cls)) {
                                                if (!(obj instanceof Function)) continue out;
                                        } else if (!cls.isAnnotation() && !cls.isAnonymousClass() && !cls.isArray() &&
                                                !cls.isEnum() && !cls.isLocalClass() && !cls.isMemberClass()
                                                && !cls.isPrimitive() && !cls.isSynthetic() &&
                                                isFunctionalAbstractClass(cls)) {
                                                if (!(obj instanceof Function)) continue out;
                                        } else {
                                                notFunctionalAbstractClass.put(cls, null);
                                                notFunctionalInterfaces.put(cls, null);
                                                continue out;
                                        }
                                }
                        }

                        methodList.add(m);
                }
        }

        /**
         * overridden methods
         * (superClass method) => (subClass method)
         */
        private static Map<Method, Set<Method>> overriddenMethods = new WeakHashMap<>();
        /**
         * classes already done override analysis
         */
        private static Map<Class<?>, Object> classesDoneOverrideAnalysis = new WeakHashMap<>();
        /**
         * functional abstract classes
         */
        private static Map<Class<?>, Object> functionalAbstractClasses = new WeakHashMap<>();
        /**
         * not functional abstract classes
         */
        private static Map<Class<?>, Object> notFunctionalAbstractClass = new WeakHashMap<>();
        /**
         * functional interfaces
         */
        private static Map<Class<?>, Object> functionalInterfaces = new WeakHashMap<>();
        /**
         * not functional interfaces
         */
        private static Map<Class<?>, Object> notFunctionalInterfaces = new WeakHashMap<>();
        /**
         * abstract method of a functional interface/abstract class
         */
        private static Map<Class<?>, Method> abstractMethod = new WeakHashMap<>();

        private static boolean signaturesAreTheSame(Method subM, Method parentM) {
                String name = parentM.getName();

                if (subM.getName().equals(name)) {
                        if (subM.getParameterCount() == parentM.getParameterCount()) {
                                for (int i = 0; i < subM.getParameterCount(); ++i) {
                                        Class<?> subP = subM.getParameterTypes()[i];
                                        Class<?> parentP = parentM.getParameterTypes()[i];
                                        if (!subP.equals(parentP)) return false;
                                }
                        }
                        // parentM is overridden by subM
                        Set<Method> set;
                        if (overriddenMethods.containsKey(parentM)) {
                                set = overriddenMethods.get(parentM);
                        } else {
                                set = new HashSet<>();
                                overriddenMethods.put(parentM, set);
                        }
                        set.add(subM);
                        return true;
                }

                return false;
        }

        private static void analyseClassOverride(Class<?> c) {
                if (classesDoneOverrideAnalysis.containsKey(c)) return;

                classesDoneOverrideAnalysis.put(c, null);

                if (!c.isInterface()) {
                        // classes should check super classes
                        // interfaces don't have super classes (except java.lang.Object)

                        // find overridden abstract methods
                        Class<?> parent = c.getSuperclass();

                        if (parent != null) {

                                for (Method parentM : parent.getDeclaredMethods()) {
                                        for (Method subM : c.getDeclaredMethods()) {
                                                if (signaturesAreTheSame(subM, parentM)) break;
                                        }
                                }

                                analyseClassOverride(parent);
                        }
                }

                // check interfaces
                for (Class<?> i : c.getInterfaces()) {
                        for (Method iM : i.getDeclaredMethods()) {
                                for (Method cM : c.getDeclaredMethods()) {
                                        if (signaturesAreTheSame(cM, iM)) break;
                                }
                        }

                        analyseClassOverride(i);
                }
        }

        private static boolean isOverriddenInClass(Method parentM, Class<?> sub) {
                Set<Method> methods = overriddenMethods.get(parentM);
                if (methods == null) return false;
                for (Method m : methods) {
                        if (m.getDeclaringClass().equals(sub)) return true;
                        if (isOverriddenInClass(m, sub)) return true;
                }
                return false;
        }

        /**
         * find one abstract method in the class
         *
         * @param c the class to retrieve method from
         * @return the retrieved abstract method
         * @throws RuntimeException no abstract method found
         */
        public static Method findAbstractMethod(Class<?> c) {
                if (abstractMethod.containsKey(c)) return abstractMethod.get(c);

                // find in current class
                for (Method m : c.getDeclaredMethods()) {
                        if (Modifier.isAbstract(m.getModifiers())) {
                                abstractMethod.put(c, m);
                                return m;
                        }
                }

                if (!c.isInterface()) {
                        // check super class
                        Class<?> tmp = c.getSuperclass();
                        while (tmp != null) {
                                if (!Modifier.isAbstract(tmp.getModifiers())) break;

                                for (Method method : tmp.getDeclaredMethods()) {
                                        if (Modifier.isAbstract(method.getModifiers())) {
                                                if (isOverriddenInClass(method, c)) continue;

                                                abstractMethod.put(c, method);
                                                return method;
                                        }
                                }
                                tmp = tmp.getSuperclass();
                        }
                }

                // check interfaces

                Set<Class<?>> visited = new HashSet<>();
                Queue<Class<?>> interfaces = new ArrayDeque<>();

                Collections.addAll(interfaces);

                while (!interfaces.isEmpty()) {
                        Class<?> ii = interfaces.remove();
                        if (visited.contains(ii)) continue;
                        for (Method m : ii.getDeclaredMethods()) {
                                if (Modifier.isAbstract(m.getModifiers())) {
                                        if (isOverriddenInClass(m, c)) continue;

                                        abstractMethod.put(c, m);
                                        return m;
                                }
                        }

                        visited.add(ii);
                        Collections.addAll(interfaces, ii.getInterfaces());
                }

                throw new RuntimeException("cannot find abstract method in " + c);
        }

        /**
         * check whether the interface is a <tt>functional interface</tt>
         *
         * @param i the interface to be checked
         * @return true/false
         */
        public static boolean isFunctionalInterface(Class<?> i) {
                if (i.isAnnotationPresent(FunctionalInterface.class)) return true;

                if (functionalInterfaces.containsKey(i)) return true;
                if (notFunctionalInterfaces.containsKey(i)) return false;

                analyseClassOverride(i);

                Set<Class<?>> visited = new HashSet<>();

                boolean found = false;
                Queue<Class<?>> interfaces = new ArrayDeque<>();
                interfaces.add(i);

                while (!interfaces.isEmpty()) {
                        Class<?> ii = interfaces.remove();
                        if (visited.contains(ii)) continue;
                        for (Method m : ii.getDeclaredMethods()) {
                                if (Modifier.isAbstract(m.getModifiers())) {
                                        if (isOverriddenInClass(m, i)) continue;

                                        if (found) return false;
                                        found = true;
                                }
                        }

                        visited.add(ii);
                        Collections.addAll(interfaces, ii.getInterfaces());
                }

                if (found)
                        functionalInterfaces.put(i, null);
                return found;
        }

        /**
         * check whether the class is a <tt>functional abstract class</tt>
         *
         * @param c the class to be checked
         * @return true/false
         */
        public static boolean isFunctionalAbstractClass(Class<?> c) {
                if (!Modifier.isAbstract(c.getModifiers())) return false;

                if (c.isAnnotationPresent(FunctionalAbstractClass.class)) return true;

                if (functionalAbstractClasses.containsKey(c)) return true;
                if (notFunctionalAbstractClass.containsKey(c)) return false;

                Constructor<?>[] cons = c.getDeclaredConstructors();
                boolean containsPublicZeroParamConstructor = false;
                for (Constructor<?> con : cons) {
                        if (Modifier.isPublic(con.getModifiers())) {
                                if (con.getParameterCount() == 0) {
                                        containsPublicZeroParamConstructor = true;
                                        break;
                                }
                        }
                }

                if (!containsPublicZeroParamConstructor) return false;

                analyseClassOverride(c);

                Set<Class<?>> visited = new HashSet<>();

                boolean found = false;

                Class<?> tmpCls = c;
                while (tmpCls != null) {
                        for (Method m : tmpCls.getDeclaredMethods()) {
                                if (Modifier.isAbstract(m.getModifiers())) {
                                        if (isOverriddenInClass(m, c)) continue;

                                        if (found) return false;
                                        found = true;
                                }
                        }

                        visited.add(tmpCls);
                        tmpCls = tmpCls.getSuperclass();
                }

                Queue<Class<?>> interfaces = new ArrayDeque<>();
                Collections.addAll(interfaces);

                while (!interfaces.isEmpty()) {
                        Class<?> ii = interfaces.remove();
                        if (visited.contains(ii)) continue;
                        for (Method m : ii.getDeclaredMethods()) {
                                if (Modifier.isAbstract(m.getModifiers())) {
                                        if (isOverriddenInClass(m, c)) continue;

                                        if (found) return false;
                                        found = true;
                                }
                        }

                        visited.add(ii);
                        Collections.addAll(interfaces, ii.getInterfaces());
                }

                if (found)
                        functionalAbstractClasses.put(c, null);
                return found;
        }

        /**
         * bfs search the required type
         *
         * @param current  current
         * @param required required
         * @return cast steps - 0 means no cast
         */
        private static int bfsSearch(Class<?> current, Class<?> required) {
                Set<Class<?>> visited = new HashSet<>();
                Queue<Class<?>> queue = new ArrayDeque<>();
                List<Class<?>> ready = new LinkedList<>();
                queue.add(current);
                visited.add(current);
                int count = 0;
                while (!queue.isEmpty() || !ready.isEmpty()) {
                        if (queue.isEmpty()) {
                                queue.addAll(ready);
                                ready.clear();
                                ++count;
                        }

                        Class<?> c = queue.remove();
                        if (c.equals(required)) return count;
                        // fill in super
                        if (c.getSuperclass() != null
                                &&
                                !visited.contains(c.getSuperclass())) {

                                ready.add(c.getSuperclass());
                                visited.add(c.getSuperclass());
                        }
                        for (Class<?> i : c.getInterfaces()) {
                                if (!visited.contains(i)) {
                                        ready.add(i);
                                        visited.add(i);
                                }
                        }
                }
                throw new LtBug(required + " is not assignable from " + current);
        }


        private static void transToRequiredType(Object[] args, Class<?>[] params) throws Exception {
                for (int i = 0; i < params.length; ++i) {
                        Class<?> c = params[i];
                        if (c.isPrimitive()) continue;
                        Object o = args[i];

                        if (o == null || c.isInstance(o)) continue;

                        args[i] = LtRuntime.cast(o, c);
                }
        }

        public static Object invoke(Class<?> targetClass, Object o, Class<?> invoker, String method, boolean[] primitives, Object[] args) throws Throwable {
                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");
                List<Method> methodList = new ArrayList<>();

                Queue<Class<?>> interfaces = new ArrayDeque<>();
                Class<?> c = o == null ? targetClass : o.getClass();
                while (c != null) {
                        Collections.addAll(interfaces, c.getInterfaces());
                        fillMethodCandidates(c, invoker, method, args, methodList, o == null);
                        c = c.getSuperclass();
                }
                c = o == null ? targetClass : o.getClass();

                Collections.addAll(interfaces, c.getInterfaces());
                while (!interfaces.isEmpty()) {
                        Class<?> i = interfaces.remove();
                        fillMethodCandidates(i, invoker, method, args, methodList, o == null);
                        Collections.addAll(interfaces, i.getInterfaces());
                }

                if (methodList.isEmpty()) {
                        if (c.isArray()) {
                                if (method.equals("get") && args.length == 1 && args[0] instanceof Integer) {
                                        return Array.get(o, (Integer) args[0]);
                                } else if (method.equals("set") && args.length == 2 && args[0] instanceof Integer) {
                                        Array.set(o, (Integer) args[0], args[1]);
                                        return args[1];
                                }
                        } else {
                                if (args.length == 1 && isBoxType(c) && isBoxType(args[0].getClass())) {
                                        return invokePrimitive(o, method, args[0]);
                                } else if (args.length == 0 && isBoxType(c)) {
                                        return invokePrimitive(o, method);
                                } else if (method.equals("add")
                                        && args.length == 1
                                        && (args[0] instanceof String || o instanceof String)
                                        && !(o instanceof Undefined)
                                        && !(args[0] instanceof Undefined)) {
                                        // string add
                                        return String.valueOf(o) + String.valueOf(args[0]);
                                } else if (method.equals("set")) {
                                        return invoke(targetClass, o, invoker, "put", primitives, args);
                                }
                        }
                        StringBuilder sb = new StringBuilder().append(targetClass.getName()).append(".").append(method).append("(");
                        boolean isFirst = true;
                        for (Object arg : args) {
                                if (isFirst) isFirst = false;
                                else sb.append(", ");
                                sb.append(arg.getClass().getName());
                        }
                        sb.append(")");
                        throw new RuntimeException("cannot find method to invoke " + sb.toString());
                }

                // calculate every method's cast steps
                Map<Method, int[]> steps = new HashMap<>();
                for (Method m : methodList) {
                        int[] step = new int[args.length];
                        for (int i = 0; i < args.length; ++i) {
                                Class<?> type = m.getParameterTypes()[i];
                                if (primitives[i] && type.isPrimitive()) {
                                        step[i] = 0;
                                } else if (primitives[i]) {
                                        // param is not primitive
                                        step[i] = PRIMITIVE_BOX_CAST_BASE; // first cast to box type
                                        step[i] += bfsSearch(args[i].getClass(), type);
                                } else if (type.isPrimitive()) {
                                        // arg is not primitive
                                        step[i] = PRIMITIVE_BOX_CAST_BASE; // cast to primitive
                                } else {
                                        // both not primitive
                                        // check null
                                        if (args[i] == null) step[i] = 0;
                                        else {
                                                if (type.isAssignableFrom(args[i].getClass()))
                                                        step[i] = bfsSearch(args[i].getClass(), type);
                                                else {
                                                        if (type.isArray()
                                                                || isFunctionalAbstractClass(type)
                                                                || isFunctionalInterface(type)) {
                                                                step[i] = 1;
                                                        } else throw new LtBug("unsupported type cast");
                                                }
                                        }
                                }
                        }
                        steps.put(m, step);
                }

                // choose the best match
                Method methodToInvoke = null;
                int[] step = null;
                for (Map.Entry<Method, int[]> entry : steps.entrySet()) {
                        if (methodToInvoke == null) {
                                methodToInvoke = entry.getKey();
                                step = entry.getValue();
                        } else {
                                int[] newStep = entry.getValue();
                                boolean isBetter = false;
                                boolean isWorse = false;
                                for (int i = 0; i < step.length; ++i) {
                                        if (step[i] == newStep[i]) continue;
                                        else if (step[i] > newStep[i]) isBetter = true;
                                        else if (step[i] < newStep[i]) isWorse = true;

                                        if (isBetter && isWorse)
                                                throw new RuntimeException(
                                                        "cannot decide which method to invoke:\n"
                                                                + methodToInvoke + ":" + Arrays.toString(step) + "\n"
                                                                + entry.getKey() + ":" + Arrays.toString(newStep));
                                }

                                if (isBetter) {
                                        methodToInvoke = entry.getKey();
                                        step = entry.getValue();
                                }
                        }
                }

                assert methodToInvoke != null;

                transToRequiredType(args, methodToInvoke.getParameterTypes());

                methodToInvoke.setAccessible(true);

                try {
                        Object res = methodToInvoke.invoke(o, args);
                        if (methodToInvoke.getReturnType() == void.class) return Undefined.get();
                        else return res;
                } catch (InvocationTargetException e) {
                        throw e.getCause();
                }
        }

        private static Object invokePrimitive(Object o, String op) {
                switch (op) {
                        case "not":
                                o = prepareNumber(o);
                                if (o == null) break;
                                if (o instanceof Integer) return ~((Number) o).intValue();
                                if (o instanceof Long) return ~((Number) o).longValue();
                                break;
                        case "negate":
                                o = prepareNumber(o);
                                if (o == null) break;
                                if (o instanceof Integer) return -((Number) o).intValue();
                                if (o instanceof Float) return -((Number) o).floatValue();
                                if (o instanceof Long) return -((Number) o).longValue();
                                if (o instanceof Double) return -((Number) o).doubleValue();
                                break;
                        default:
                                throw new RuntimeException("unknown one variable operation method " + op);
                }
                throw new RuntimeException("cannot invoke " + o + "." + op);
        }

        /**
         * cast integer/short/byte into Integer<br>
         * let Long/Float/Double stay their types<br>
         * cast other Number into Double
         *
         * @param n any type to be prepared
         * @return Integer/Long/Float/Double or null if it's not number
         */
        private static Object prepareNumber(Object n) {
                if (n instanceof Integer
                        || n instanceof Short
                        || n instanceof Byte) return ((Number) n).intValue();
                else if (n instanceof Character) return (int) (Character) n;
                else if (n instanceof Long || n instanceof Float || n instanceof Double) return n;
                else if (n instanceof Number) return ((Number) n).doubleValue();
                else return null;
        }

        /**
         * get top type<br>
         * Integer and Integer ==> Integer<br>
         * Integer and Float ==> Float<br>
         * Integer and Long ==> Long<br>
         * <br>
         * Float and Float ==> Float<br>
         * Float and Long ==> Double<br>
         * <br>
         * Long and Long ==> Long<br>
         * <br>
         * any and Double ==> Double<br>
         *
         * @param a Integer/Long/Float/Double
         * @param b Integer/Long/Float/Double
         * @return 1 for Integer, 2 for Float, 3 for Long, 4 for Double
         */
        private static int topType(Object a, Object b) {
                if (a instanceof Double || b instanceof Double) return 4;
                if (a instanceof Long || b instanceof Long) {
                        if (a instanceof Float || b instanceof Float) return 4;
                        return 3;
                }
                if (a instanceof Float || b instanceof Float) return 2;
                return 1;
        }

        private static Object invokePrimitive(Object a, String op, Object b) {
                switch (op) {
                        case LtRuntime.add:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                int topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() + ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() + ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() + ((Number) b).longValue();
                                return ((Number) a).doubleValue() + ((Number) b).doubleValue();
                        case LtRuntime.and:
                                if (a instanceof Boolean && b instanceof Boolean)
                                        return (Boolean) a & (Boolean) b;
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() & ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() & ((Number) b).longValue();
                                break;
                        case LtRuntime.or:
                                if (a instanceof Boolean && b instanceof Boolean)
                                        return (Boolean) a | (Boolean) b;
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() | ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() | ((Number) b).longValue();
                                break;
                        case LtRuntime.divide:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() / ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() / ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() / ((Number) b).longValue();
                                return ((Number) a).doubleValue() / ((Number) b).doubleValue();
                        case LtRuntime.ge:
                                double x;
                                double y;
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x >= y;
                        case LtRuntime.gt:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x > y;
                        case LtRuntime.le:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x <= y;
                        case LtRuntime.lt:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x < y;
                        case LtRuntime.multiply:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() * ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() * ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() * ((Number) b).longValue();
                                return ((Number) a).doubleValue() * ((Number) b).doubleValue();
                        case LtRuntime.remainder:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() % ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() % ((Number) b).longValue();
                                break;
                        case LtRuntime.shiftLeft:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() << ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() << ((Number) b).longValue();
                                break;
                        case LtRuntime.shiftRight:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() >> ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() >> ((Number) b).longValue();
                                break;
                        case LtRuntime.subtract:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() - ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() - ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() - ((Number) b).longValue();
                                return ((Number) a).doubleValue() - ((Number) b).doubleValue();
                        case LtRuntime.unsignedShiftRight:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() >>> ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() >>> ((Number) b).longValue();
                                break;
                        case LtRuntime.xor:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() ^ ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() ^ ((Number) b).longValue();
                                break;
                        default:
                                throw new RuntimeException("unknown two-variable operation method " + op);
                }
                throw new RuntimeException("cannot invoke " + a + " " + op + " " + b);
        }

        private static boolean isBoxType(Class<?> cls) {
                return cls.equals(Integer.class)
                        || cls.equals(Short.class)
                        || cls.equals(Byte.class)
                        || cls.equals(Boolean.class)
                        || cls.equals(Long.class)
                        || cls.equals(Character.class)
                        || cls.equals(Float.class)
                        || cls.equals(Double.class);
        }

}