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
import lt.lang.callback.AsyncResultFunc;
import lt.lang.callback.CallbackFunc;
import lt.lang.function.Function;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.Map;

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
        private static final int COLLECTION_OBJECT_CAST_BASE = 2333;

        private Dynamic() {
        }

        private static MethodHandle methodHandle;

        private static MethodHandle constructorHandle;

        private static MethodHandle callFunctionalObject;

        static {
                try {
                        methodHandle = MethodHandles.lookup().findStatic(Dynamic.class, "invoke", MethodType.methodType(
                                Object.class, Class.class, Object.class, Object.class, Class.class, String.class, boolean[].class, Object[].class));
                        constructorHandle = MethodHandles.lookup().findStatic(Dynamic.class, "construct", MethodType.methodType(
                                Object.class, Class.class, Class.class, boolean[].class, Object[].class));
                        callFunctionalObject = MethodHandles.lookup().findStatic(Dynamic.class, "callFunctionalObject", MethodType.methodType(
                                Object.class, Object.class, Class.class, Object[].class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new LtRuntimeException(e);
                }
        }

        /**
         * the bootstrap method for Latte Dynamic
         *
         * @param lookup     lookup
         * @param methodName method name
         * @param methodType methodType (class, object, arguments)
         * @return generated call site
         */
        @SuppressWarnings("unused")
        public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType) {
                final int actualParamCount = 3;

                boolean[] primitives = new boolean[methodType.parameterCount() - actualParamCount];
                for (int i = 0; i < primitives.length; ++i) {
                        primitives[i] = methodType.parameterType(i + actualParamCount).isPrimitive();
                }
                MethodHandle mh = MethodHandles.insertArguments(methodHandle, actualParamCount, lookup.lookupClass(), methodName, primitives);
                mh = mh.asCollector(Object[].class, methodType.parameterCount() - actualParamCount).asType(methodType);

                MutableCallSite mCallSite = new MutableCallSite(mh);
                mCallSite.setTarget(mh);
                return mCallSite;
        }

        /**
         * the bootstrap method for constructing classes
         *
         * @param lookup     lookup
         * @param methodName method name (this argument will be ignored)
         * @param methodType methodType (class, arguments)
         * @return generated call site
         */
        @SuppressWarnings("unused")
        public static CallSite bootstrapConstructor(MethodHandles.Lookup lookup, String methodName, MethodType methodType) {
                // method name will not be used. it can be any value.
                // but usually it will be "_init_"

                boolean[] primitives = new boolean[methodType.parameterCount()];
                for (int i = 0; i < primitives.length; ++i) {
                        primitives[i] = methodType.parameterType(i).isPrimitive();
                }
                MethodHandle mh = MethodHandles.insertArguments(constructorHandle, 0, methodType.returnType(), lookup.lookupClass(), primitives);
                mh = mh.asCollector(Object[].class, methodType.parameterCount()).asType(methodType);

                MutableCallSite mCallSite = new MutableCallSite(mh);
                mCallSite.setTarget(mh);
                return mCallSite;
        }

        /**
         * the bootstrap method for calling a functional object.
         *
         * @param lookup     lookup
         * @param methodName method name (this argument will be ignored)
         * @param methodType methodType (class, arguments)
         * @return generated call site
         */
        @SuppressWarnings("unused")
        public static CallSite bootstrapCallFunctionalObject(MethodHandles.Lookup lookup, String methodName, MethodType methodType) {
                // method name will not be used. it can be any value.
                // but usually it will be "_call_functional_object_"

                MethodHandle mh = MethodHandles.insertArguments(callFunctionalObject, 1, lookup.lookupClass());
                mh = mh.asCollector(Object[].class, methodType.parameterCount() - 1).asType(methodType);

                MutableCallSite mCallSite = new MutableCallSite(mh);
                mCallSite.setTarget(mh);
                return mCallSite;
        }

        /**
         * check whether the parameters' declaring type can be candidate of invoking by these arguments.
         *
         * @param params parameters
         * @param args   arguments
         * @return true or false
         */
        private static boolean canBeCandidate(Class<?>[] params, Object[] args) {
                for (int i = 0; i < args.length; ++i) {
                        if (!params[i].isInstance(args[i]) &&
                                (
                                        // null can be assigned to any reference type
                                        params[i].isPrimitive()
                                                ||
                                                args[i] != null
                                )) {
                                // is not instance
                                // check primitive
                                Class<?> cls = params[i];

                                Object obj = args[i];
                                if (null == obj) continue;

                                if (cls.equals(int.class)) {
                                        if (!(obj instanceof Integer)) return false;
                                } else if (cls.equals(short.class)) {
                                        if (!(obj instanceof Short)) return false;
                                } else if (cls.equals(byte.class)) {
                                        if (!(obj instanceof Byte)) return false;
                                } else if (cls.equals(boolean.class)) {
                                        if (!(obj instanceof Boolean)) return false;
                                } else if (cls.equals(char.class)) {
                                        if (!(obj instanceof Character)) return false;
                                } else if (cls.equals(long.class)) {
                                        if (!(obj instanceof Long)) return false;
                                } else if (cls.equals(double.class)) {
                                        if (!(obj instanceof Double)) return false;
                                } else if (cls.equals(float.class)) {
                                        if (!(obj instanceof Float)) return false;
                                } else if (cls.isArray()) {
                                        if (!(obj instanceof java.util.List)) return false;
                                } else if (!cls.isArray() && !cls.isInterface() && !cls.isAnonymousClass()
                                        && !cls.isAnnotation() && !cls.isEnum() && !cls.isLocalClass()
                                        && !cls.isMemberClass() && !cls.isPrimitive() && !cls.isSynthetic()
                                        && (obj instanceof java.util.Map || obj instanceof java.util.List)) {
                                        // obj is map
                                        // and cast to a java object
                                        Constructor<?> con;
                                        try {
                                                con = cls.getConstructor();
                                        } catch (Exception e) {
                                                // constructor without parameter
                                                return false;
                                        }
                                        // constructor modifier public
                                        if (!Modifier.isPublic(con.getModifiers())) return false;

                                        if (obj instanceof Map) {
                                                // each key is string
                                                Map map = (Map) obj;
                                                for (Object key : map.keySet()) {
                                                        if (!(key instanceof String)) {
                                                                return false;
                                                        }
                                                }
                                        }

                                        return true;

                                } else if (cls.isInterface() && isFunctionalInterface(cls)) {
                                        if (!(obj instanceof Function)) return false;
                                } else if (!cls.isAnnotation() && !cls.isAnonymousClass() && !cls.isArray() &&
                                        !cls.isEnum() && !cls.isLocalClass() && !cls.isMemberClass()
                                        && !cls.isPrimitive() && !cls.isSynthetic() &&
                                        isFunctionalAbstractClass(cls)) {
                                        if (!(obj instanceof Function)) return false;
                                } else {
                                        notFunctionalAbstractClass.put(cls, null);
                                        notFunctionalInterfaces.put(cls, null);
                                        return false;
                                }
                        }
                }
                return true;
        }

        private static Class<?> chooseType(Class<?> targetType, Object target) {
                if (target == null) return targetType;
                if (targetType.isAnnotationPresent(Implicit.class)) return targetType;
                if (targetType.isInstance(target)) return target.getClass();
                return targetType;
        }

        public static Method findMethod(Class<?> invoker, Class<?> targetType, Object target, String method, boolean[] primitives, Object[] args) throws Throwable {
                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");
                List<Method> methodList = new ArrayList<>();

                Queue<Class<?>> interfaces = new ArrayDeque<>();
                Class<?> c = chooseType(targetType, target);
                while (c != null) {
                        Collections.addAll(interfaces, c.getInterfaces());
                        fillMethodCandidates(c, invoker, method, args, methodList, target == null);
                        c = c.getSuperclass();
                }
                c = chooseType(targetType, target);

                Collections.addAll(interfaces, c.getInterfaces());
                while (!interfaces.isEmpty()) {
                        Class<?> i = interfaces.remove();
                        fillMethodCandidates(i, invoker, method, args, methodList, target == null);
                        Collections.addAll(interfaces, i.getInterfaces());
                }

                if (methodList.isEmpty()) {
                        return null;
                }

                // find best match
                Method methodToInvoke = findBestMatch(methodList, args, primitives);
                // trans to required type
                transToRequiredType(args, methodToInvoke.getParameterTypes());

                return methodToInvoke;
        }

        /**
         * fill in method candidates
         *
         * @param c          class
         * @param invoker    invoker
         * @param method     method
         * @param args       arguments
         * @param methodList method list (fill into this list)
         * @param onlyStatic only find static methods
         */
        private static void fillMethodCandidates(Class<?> c,
                                                 Class<?> invoker,
                                                 String method,
                                                 Object[] args,
                                                 List<Method> methodList,
                                                 boolean onlyStatic) {
                for (Method m : c.getDeclaredMethods()) {
                        if (!m.getName().equals(method)) continue;
                        if (m.getParameterCount() != args.length) continue;

                        // access check
                        if (!LtRuntime.haveAccess(m.getModifiers(), c, invoker)) continue;

                        if (onlyStatic) {
                                if (!Modifier.isStatic(m.getModifiers())) continue;
                        }

                        // else public
                        // do nothing

                        if (canBeCandidate(m.getParameterTypes(), args)) {
                                methodList.add(m);
                        }
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

        /**
         * check signature, whether they are the same.
         *
         * @param subM    the method in sub class
         * @param parentM the method in super class
         * @return true or false
         */
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

        /**
         * analyse the override relation of the methods in the class/interface
         *
         * @param c class object
         */
        private static void analyseClassOverride(Class<?> c) {
                if (classesDoneOverrideAnalysis.containsKey(c)) return;

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

                classesDoneOverrideAnalysis.put(c, null);
        }

        /**
         * check whether the method is overridden in the sub class
         *
         * @param parentM method in parent class
         * @param sub     sub class
         * @return true or false
         */
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
         * @throws LtRuntimeException no abstract method found
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

                throw new LtRuntimeException("cannot find abstract method in " + c);
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

        /**
         * transform the argument into required types. the transformed object will replace elements in the argument array.
         *
         * @param args   arguments
         * @param params required types
         * @throws Exception exception
         */
        private static void transToRequiredType(Object[] args, Class<?>[] params) throws Throwable {
                for (int i = 0; i < params.length; ++i) {
                        Class<?> c = params[i];
                        if (c.isPrimitive()) continue;
                        Object o = args[i];

                        if (o == null || c.isInstance(o)) continue;

                        args[i] = LtRuntime.cast(o, c);
                }
        }

        /**
         * find best match.
         *
         * @param methodList method list (constructors or methods)
         * @param args       arguments
         * @param primitives whether the argument is a primitive
         * @param <T>        {@link Constructor} or {@link Method}
         * @return the found method
         */
        private static <T extends Executable> T findBestMatch(List<T> methodList, Object[] args, boolean[] primitives) {
                // calculate every method's cast steps
                Map<T, int[]> steps = new HashMap<>();
                for (T m : methodList) {
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
                                                        } else if (args[i] instanceof Map || args[i] instanceof List) {
                                                                step[i] = COLLECTION_OBJECT_CAST_BASE;
                                                        } else throw new LtBug("unsupported type cast");
                                                }
                                        }
                                }
                        }
                        steps.put(m, step);
                }

                // choose the best match
                T methodToInvoke = null;
                int[] step = null;
                for (Map.Entry<T, int[]> entry : steps.entrySet()) {
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
                                                throw new LtRuntimeException(
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

                return methodToInvoke;
        }

        /**
         * construct an object.
         *
         * @param targetType the type to instantiate.
         * @param invoker    from which class invokes the method
         * @param primitives whether the argument is primitive
         * @param args       arguments
         * @return the constructed object
         * @throws Throwable exceptions
         */
        public static Object construct(Class<?> targetType, Class<?> invoker, boolean[] primitives, Object[] args) throws Throwable {
                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");

                Constructor<?>[] constructors = targetType.getDeclaredConstructors();

                // select candidates
                List<Constructor<?>> candidates = new ArrayList<>();
                for (Constructor<?> con : constructors) {
                        if (!LtRuntime.haveAccess(con.getModifiers(), targetType, invoker)) continue;

                        if (con.getParameterCount() == args.length) {
                                if (canBeCandidate(con.getParameterTypes(), args)) {
                                        candidates.add(con);
                                }
                        }
                }

                if (candidates.isEmpty()) {
                        StringBuilder sb = new StringBuilder().append(targetType.getName()).append("(");
                        boolean isFirst = true;
                        for (Object arg : args) {
                                if (isFirst) isFirst = false;
                                else sb.append(", ");
                                sb.append(arg == null ? "null" : arg.getClass().getName());
                        }
                        sb.append(")");
                        throw new LtRuntimeException("cannot find constructor " + sb.toString());
                } else {
                        Constructor<?> constructor = findBestMatch(candidates, args, primitives);
                        transToRequiredType(args, constructor.getParameterTypes());

                        constructor.setAccessible(true);

                        try {
                                return constructor.newInstance(args);
                        } catch (InvocationTargetException e) {
                                throw e.getTargetException();
                        }
                }
        }

        /**
         * the invocation state.
         */
        public static class InvocationState {
                /**
                 * whether the method is found
                 */
                public boolean methodFound = false;
                /**
                 * whether it's a reversed invocation
                 */
                public boolean isCallingReverse = false;
                /**
                 * the method that invokes this, is trying to get or put a field
                 */
                public boolean fromField = false;
        }

        /**
         * invoke method with a invocationState.
         *
         * @param invocationState  invocationState
         * @param targetClass      the method is in this class
         * @param o                invoke the method on the object (or null if invoke static)
         * @param functionalObject the object to invoke functional method on if method not found
         * @param invoker          from which class invokes the method
         * @param method           method name
         * @param primitives       whether the argument is primitive
         * @param args             the arguments
         * @return the method result (void methods' results are <tt>Unit</tt>)
         * @throws Throwable exception
         */
        public static Object invoke(InvocationState invocationState,
                                    Class<?> targetClass,
                                    Object o,
                                    Object functionalObject,
                                    Class<?> invoker,
                                    String method,
                                    boolean[] primitives,
                                    Object[] args) throws Throwable {

                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");
                Method methodToInvoke = findMethod(invoker, targetClass, o, method, primitives, args);
                // method found ?
                if (null != methodToInvoke) {
                        methodToInvoke.setAccessible(true);

                        try {
                                Object res = methodToInvoke.invoke(o, args);
                                if (methodToInvoke.getReturnType() == void.class) return Unit.get();
                                else return res;
                        } catch (InvocationTargetException e) {
                                throw e.getTargetException();
                        }
                }

                ExceptionContainer ec = new ExceptionContainer();

                Class<?> c = o == null ? targetClass : o.getClass();
                if (c.isArray()) {
                        if (method.equals("get") && args.length >= 1 && args[0] instanceof Integer) {
                                Object res = Array.get(o, (Integer) args[0]);
                                if (args.length == 1) return res;

                                boolean[] bs = new boolean[primitives.length - 1];
                                Object[] as = new Object[args.length - 1];
                                for (int i = 1; i < args.length; ++i) {
                                        bs[i - 1] = primitives[i];
                                        as[i - 1] = args[i];
                                }

                                return invoke(invocationState, targetClass, res, null, invoker, "get", bs, as);
                        } else if (method.equals("set") && args.length >= 2 && args[0] instanceof Integer) {
                                if (args.length == 2) {
                                        Array.set(o, (Integer) args[0], args[1]);
                                        return args[1];
                                } else {
                                        Object elem = Array.get(o, (Integer) args[0]);

                                        boolean[] bs = new boolean[primitives.length - 1];
                                        Object[] as = new Object[args.length - 1];
                                        for (int i = 1; i < args.length; ++i) {
                                                bs[i - 1] = primitives[i];
                                                as[i - 1] = args[i];
                                        }

                                        return invoke(invocationState, targetClass, elem, null, invoker, "set", bs, as);
                                }
                        } else {
                                ec.add("Target is array but method is not get(int)");
                                ec.add("Target is array but method is not set(int, ...)");
                        }
                } else {
                        // implicit cast
                        if (o != null && invoker.isAnnotationPresent(ImplicitImports.class)) {
                                Class<?>[] implicitClasses = invoker.getAnnotation(ImplicitImports.class).implicitImports();
                                if (implicitClasses.length == 0) {
                                        ec.add("No implicit casts enabled");
                                } else {
                                        for (Class<?> ic : implicitClasses) {
                                                Class<?> type = ic.getConstructors()[0].getParameterTypes()[0];
                                                if (type.isAssignableFrom(o.getClass())) {
                                                        Method m = findMethod(invoker, ic, o, method, primitives, args);
                                                        if (m != null) {
                                                                Object target = ic.getConstructor(type).newInstance(o);
                                                                return m.invoke(target, args);
                                                        } else {
                                                                ec.add("method not found in " + ic.getName());
                                                        }
                                                } else {
                                                        ec.add("Cannot cast " + o.getClass().getName() + " to " + ic.getName());
                                                }
                                        }
                                }
                        } else {
                                ec.add("No implicit casts enabled");
                        }

                        if (args.length == 1 && isBoxType(c) && isBoxType(args[0].getClass())) {
                                return invokePrimitive(o, method, args[0]);
                        } else if (args.length == 0 && isBoxType(c)) {
                                return invokePrimitive(o, method);
                        } else if (method.equals("add")
                                && args.length == 1
                                && (args[0] instanceof String || o instanceof String)) {
                                // string add
                                return String.valueOf(o) + String.valueOf(args[0]);
                        } else if (method.equals("set")) {
                                return invoke(invocationState, targetClass, o, functionalObject, invoker, "put", primitives, args);
                        } else if (method.equals("logicNot") && args.length == 0) {
                                return !LtRuntime.castToBool(o);
                        } else {
                                ec.add("Is not primitive method invocation");
                                ec.add("Is not string concatenation");
                        }
                }

                if (!invocationState.isCallingReverse) {
                        // await
                        if (args.length > 0 && args[args.length - 1] instanceof CallbackFunc) {
                                //noinspection unchecked
                                boolean[] newPrimitives = new boolean[args.length];
                                Object[] newArgs = new Object[args.length];
                                System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);
                                System.arraycopy(primitives, 0, newPrimitives, 0, newPrimitives.length);

                                //noinspection unchecked
                                newArgs[newArgs.length - 1] = new AsyncResultFunc((CallbackFunc) args[args.length - 1]);

                                InvocationState state = new InvocationState();
                                try {
                                        return invoke(state, targetClass, o, functionalObject, invoker, method,
                                                newPrimitives, newArgs);
                                } catch (Throwable t) {
                                        if (state.methodFound) {
                                                throw t;
                                        }
                                        ec.add("Cannot invoke callback function");
                                }
                        } else {
                                ec.add("Is not callback function");
                        }

                        // functional object
                        if (functionalObject != null) {
                                InvocationState callFunctionalState = new InvocationState();
                                try {
                                        return callFunctionalObject(callFunctionalState, functionalObject, invoker, args);
                                } catch (Throwable t) {
                                        if (callFunctionalState.methodFound) throw t;
                                        ec.add("Cannot invoke functional object");
                                }
                        } else {
                                ec.add("No functional object");
                        }

                        invocationState.methodFound = false; // method still not found

                        // call anything
                        // call(Object,String,[]bool,[]Object)
                        Method call = null;
                        try {
                                Class<?> cc = targetClass;
                                if (o != null) {
                                        cc = o.getClass();
                                }
                                call = cc.getMethod("call", Object.class, String.class, boolean[].class, Object[].class);
                                if (Modifier.isStatic(call.getModifiers()) && !call.getReturnType().equals(void.class)) {
                                        invocationState.methodFound = true;
                                }
                        } catch (NoSuchMethodException ignore) {
                                ec.add("Method " + targetClass.getName() + "#call(Object,String,[]bool,[]Object) not found");
                        }

                        if (invocationState.methodFound) {
                                assert call != null;
                                try {
                                        return call.invoke(null, o, method, primitives, args);
                                } catch (InvocationTargetException e) {
                                        throw e.getTargetException();
                                }
                        }
                }

                // dynamically get field `o.methodName`
                // if it's not `null` and not `Unit` then invoke the retrieved object
                if (!invocationState.fromField && !invocationState.isCallingReverse) {
                        Object result = null;
                        boolean fieldFound = false;
                        try {
                                result = LtRuntime.getField(o, method, invoker);
                                fieldFound = true;
                        } catch (NoSuchFieldException e) {
                                ec.add("Cannot get field " + targetClass.getName() + "#" + e.getMessage());
                        }
                        if (fieldFound) {
                                if (result != null && !result.equals(Unit.get())) {
                                        invocationState.methodFound = true;
                                        return callFunctionalObject(result, invoker, args);
                                } else {
                                        ec.add("Field " + targetClass.getName() + "#" + method + " is null or Unit");
                                }
                        }
                }

                // method not found
                // build exception message
                StringBuilder sb = new StringBuilder().append(
                        o == null
                                ? targetClass.getName()
                                : o.getClass().getName()
                ).append("#").append(method).append("(");
                boolean isFirst = true;
                for (Object arg : args) {
                        if (isFirst) isFirst = false;
                        else sb.append(", ");
                        sb.append(arg == null ? "null" : arg.getClass().getName());
                }
                sb.append(")");
                ec.throwIfNotEmpty("Cannot find method to invoke: " + sb.toString(), LtRuntimeException::new);
                // code won't reach here
                throw new LtBug("code won't reach here");
        }

        /**
         * call the functional object. This method is the CallSite method.
         *
         * @param functionalObject the functional object.
         * @param callerClass      caller class
         * @param args             arguments.
         * @return the calling result.
         * @throws Throwable exception when calling the functional object.
         */
        @SuppressWarnings("unused")
        private static Object callFunctionalObject(Object functionalObject,
                                                   Class<?> callerClass,
                                                   Object[] args) throws Throwable {
                return callFunctionalObject(new InvocationState(), functionalObject, callerClass, args);
        }

        /**
         * call the functional object. This method is the actual method which can be directly used by other methods.
         *
         * @param invocationState  invocation state.
         * @param functionalObject the functional object.
         * @param callerClass      caller class
         * @param args             arguments.
         * @return the calling result.
         * @throws Throwable exception when calling the functional object.
         */
        private static Object callFunctionalObject(InvocationState invocationState,
                                                   Object functionalObject,
                                                   Class<?> callerClass,
                                                   Object[] args) throws Throwable {
                if (functionalObject == null) throw new NullPointerException();

                // check whether it's a functional object
                Class<?> cls = functionalObject.getClass();
                Method theMethodToInvoke;
                if (cls.getSuperclass() != null && isFunctionalAbstractClass(cls.getSuperclass())) {
                        theMethodToInvoke = findAbstractMethod(cls.getSuperclass());
                } else if (cls.getInterfaces().length == 1 && isFunctionalInterface(cls.getInterfaces()[0])) {
                        theMethodToInvoke = findAbstractMethod(cls.getInterfaces()[0]);
                } else {
                        // try to invoke apply(...) on this object
                        return invoke(invocationState,
                                functionalObject.getClass(), functionalObject, null, callerClass, "apply", new boolean[args.length], args);
                }

                // continue processing `theMethodToInvoke`
                invocationState.methodFound = true;
                try {
                        transToRequiredType(args, theMethodToInvoke.getParameterTypes());
                        Object theRes = theMethodToInvoke.invoke(functionalObject, args);
                        if (theMethodToInvoke.getReturnType().equals(Void.TYPE)) {
                                return Unit.get();
                        }
                        return theRes;
                } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                }
        }

        /**
         * invoke a method.
         *
         * @param targetClass      the method is in this class
         * @param o                invoke the method on the object (or null if invoke static)
         * @param functionalObject the object to invoke functional method on if method not found
         * @param invoker          from which class invokes the method
         * @param method           method name
         * @param primitives       whether the argument is primitive
         * @param args             the arguments
         * @return the method result (void methods' results are <tt>Unit</tt>)
         * @throws Throwable exception
         */
        @SuppressWarnings("unused")
        private static Object invoke(Class<?> targetClass, Object o, Object functionalObject, Class<?> invoker,
                                     String method, boolean[] primitives, Object[] args) throws Throwable {
                return invoke(new InvocationState(), targetClass, o, functionalObject, invoker, method, primitives, args);
        }

        /**
         * primitive one variable operations.
         *
         * @param o  object
         * @param op operator (not and negate)
         * @return the result
         */
        private static Object invokePrimitive(Object o, String op) throws Throwable {
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
                        case "logicNot":
                                return !LtRuntime.castToBool(o);
                        default:
                                throw new LtRuntimeException("unknown one variable operation method " + op);
                }
                throw new LtRuntimeException("cannot invoke " + o + "." + op);
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

        /**
         * primitive two variable operations
         *
         * @param a  a
         * @param op operator
         * @param b  b
         * @return the reusult
         */
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
                                throw new LtRuntimeException("unknown two-variable operation method " + op);
                }
                throw new LtRuntimeException("cannot invoke " + a + " " + op + " " + b);
        }

        /**
         * check whether the class is a box type
         *
         * @param cls class
         * @return true or false
         */
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