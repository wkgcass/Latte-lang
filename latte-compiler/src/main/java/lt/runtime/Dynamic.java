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

package lt.runtime;

import lt.compiler.LtBug;
import lt.lang.FunctionalAbstractClass;
import lt.lang.FunctionalInterface;
import lt.lang.Unit;
import lt.lang.function.Function;
import lt.lang.function.Function1;

import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.Map;

/**
 * invoke dynamic
 */
public class Dynamic {
        private static final int PRIMITIVE_BOX_CAST_BASE = 233;
        private static final int COLLECTION_OBJECT_CAST_BASE = 2333;

        /**
         * check cast when parameter and argument are both primitive
         * the primitive argument would be cast to box type when handled
         * so add box types into list
         */
        private static final Map<Class<?>, Set<Class<?>>> primitiveCast = new HashMap<Class<?>, Set<Class<?>>>() {{
                final Map<Class<?>, Set<Class<?>>> self = this;
                put(int.class, new HashSet<Class<?>>() {{
                        add(Byte.class);
                        add(Short.class);
                        add(Character.class);
                        add(Integer.class);
                }});
                put(long.class, new HashSet<Class<?>>() {{
                        addAll(self.get(int.class));
                        add(Long.class);
                }});
                put(float.class, new HashSet<Class<?>>() {{
                        addAll(self.get(long.class));
                        add(Float.class);
                }});
                put(double.class, new HashSet<Class<?>>() {{
                        addAll(self.get(long.class));
                        addAll(self.get(float.class));
                        add(Double.class);
                }});
                put(byte.class, new HashSet<Class<?>>() {{
                        add(Byte.class);
                }});
                put(short.class, new HashSet<Class<?>>() {{
                        add(Short.class);
                }});
                put(char.class, new HashSet<Class<?>>() {{
                        add(Character.class);
                }});
                put(boolean.class, new HashSet<Class<?>>() {{
                        add(Boolean.class);
                }});
        }};

        private Dynamic() {
        }

        /**
         * check whether the parameters' declaring type can be candidate of invoking by these arguments.
         *
         * @param params     parameters
         * @param args       arguments
         * @param primitives whether the arg is a primitive
         * @return true or false
         */
        private static boolean canBeCandidate(Class<?>[] params, Object[] args, boolean[] primitives) {
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

                                if (cls.isPrimitive() && primitives[i]) {
                                        if (!primitiveCast.get(cls).contains(obj.getClass())) {
                                                return false;
                                        }
                                        continue;
                                }
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
                List<Method> methodList = new ArrayList<Method>();

                Queue<Class<?>> interfaces = new ArrayDeque<Class<?>>();
                Class<?> c = chooseType(targetType, target);
                while (c != null) {
                        Collections.addAll(interfaces, c.getInterfaces());
                        fillMethodCandidates(c, invoker, method, primitives, args, methodList, target == null);
                        c = c.getSuperclass();
                }
                c = chooseType(targetType, target);

                Collections.addAll(interfaces, c.getInterfaces());
                while (!interfaces.isEmpty()) {
                        Class<?> i = interfaces.remove();
                        fillMethodCandidates(i, invoker, method, primitives, args, methodList, target == null);
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
                                                 boolean[] primitives,
                                                 Object[] args,
                                                 List<Method> methodList,
                                                 boolean onlyStatic) {
                for (Method m : c.getDeclaredMethods()) {
                        if (!m.getName().equals(method)) continue;
                        if (m.getParameterTypes().length != args.length) continue;

                        // access check
                        if (!LtRuntime.haveAccess(m.getModifiers(), c, invoker)) continue;

                        if (onlyStatic) {
                                if (!Modifier.isStatic(m.getModifiers())) continue;
                        }

                        // else public
                        // do nothing

                        if (canBeCandidate(m.getParameterTypes(), args, primitives)) {
                                methodList.add(m);
                        }
                }
        }

        /**
         * overridden methods
         * (superClass method) => (subClass method)
         */
        private static Map<Method, Set<Method>> overriddenMethods = new WeakHashMap<Method, Set<Method>>();
        /**
         * classes already done override analysis
         */
        private static Map<Class<?>, Object> classesDoneOverrideAnalysis = new WeakHashMap<Class<?>, Object>();
        /**
         * functional abstract classes
         */
        private static Map<Class<?>, Object> functionalAbstractClasses = new WeakHashMap<Class<?>, Object>();
        /**
         * not functional abstract classes
         */
        private static Map<Class<?>, Object> notFunctionalAbstractClass = new WeakHashMap<Class<?>, Object>();
        /**
         * functional interfaces
         */
        private static Map<Class<?>, Object> functionalInterfaces = new WeakHashMap<Class<?>, Object>();
        /**
         * not functional interfaces
         */
        private static Map<Class<?>, Object> notFunctionalInterfaces = new WeakHashMap<Class<?>, Object>();
        /**
         * abstract method of a functional interface/abstract class
         */
        private static Map<Class<?>, Method> abstractMethod = new WeakHashMap<Class<?>, Method>();

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
                        if (subM.getParameterTypes().length == parentM.getParameterTypes().length) {
                                for (int i = 0; i < subM.getParameterTypes().length; ++i) {
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
                                set = new HashSet<Method>();
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

                Set<Class<?>> visited = new HashSet<Class<?>>();
                Queue<Class<?>> interfaces = new ArrayDeque<Class<?>>();

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

                Set<Class<?>> visited = new HashSet<Class<?>>();

                boolean found = false;
                Queue<Class<?>> interfaces = new ArrayDeque<Class<?>>();
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
                                if (con.getParameterTypes().length == 0) {
                                        containsPublicZeroParamConstructor = true;
                                        break;
                                }
                        }
                }

                if (!containsPublicZeroParamConstructor) return false;

                analyseClassOverride(c);

                Set<Class<?>> visited = new HashSet<Class<?>>();

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

                Queue<Class<?>> interfaces = new ArrayDeque<Class<?>>();
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
                if (current.isArray() && required.isArray()) {
                        return bfsSearch(current.getComponentType(), required.getComponentType());
                }
                Set<Class<?>> visited = new HashSet<Class<?>>();
                Queue<Class<?>> queue = new ArrayDeque<Class<?>>();
                List<Class<?>> ready = new LinkedList<Class<?>>();
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

                        args[i] = LtRuntime.cast(o, c, null);
                }
        }

        /**
         * get number type primitives cast depth
         *
         * @param from from type
         * @param to   to type
         * @return depth
         */
        private static int getNumberPrimitiveCastDepth(Class<?> from, Class<?> to) {
                if (from == Integer.class) from = int.class;
                if (from == Float.class) from = float.class;
                if (from == Long.class) from = long.class;
                if (from == Double.class) from = double.class;
                if (from == Short.class) from = short.class;
                if (from == Byte.class) from = byte.class;
                if (from == Character.class) from = char.class;

                if (from == to) return 0;
                if (from == byte.class || from == short.class || from == char.class) {
                        if (to == int.class) {
                                return 1;
                        } else if (to == long.class) {
                                return 2;
                        } else if (to == float.class) {
                                return 3;
                        } else if (to == double.class) {
                                return 4;
                        } else throw new LtBug("should not reach here, from: " + from + " to: " + to);
                } else if (from == int.class) {
                        if (to == long.class) {
                                return 1;
                        } else if (to == float.class) {
                                return 2;
                        } else if (to == double.class) {
                                return 3;
                        } else throw new LtBug("should not reach here, from: " + from + " to: " + to);
                } else if (from == long.class) {
                        if (to == float.class) {
                                return 1;
                        } else if (to == double.class) {
                                return 2;
                        } else throw new LtBug("should not reach here, from: " + from + " to: " + to);
                } else if (from == float.class) {
                        if (to == double.class) {
                                return 1;
                        } else throw new LtBug("should not reach here, from: " + from + " to: " + to);
                } else throw new LtBug("should not reach here, from: " + from);
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
        private static <T> T findBestMatch(List<T> methodList, Object[] args, boolean[] primitives) {
                // calculate every method's cast steps
                Map<T, int[]> steps = new HashMap<T, int[]>();
                for (T m : methodList) {
                        int[] step = new int[args.length];
                        for (int i = 0; i < args.length; ++i) {
                                Class<?> type;
                                if (m instanceof Method) {
                                        type = ((Method) m).getParameterTypes()[i];
                                } else {
                                        type = ((Constructor) m).getParameterTypes()[i];
                                }
                                if (primitives[i] && type.isPrimitive()) {
                                        if (args[i] instanceof Number || args[i] instanceof Character) {
                                                step[i] = getNumberPrimitiveCastDepth(args[i].getClass(), type);
                                        } else {
                                                // for non number type (bool)
                                                step[i] = 0;
                                        }
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
                List<Constructor<?>> candidates = new ArrayList<Constructor<?>>();
                for (Constructor<?> con : constructors) {
                        if (!LtRuntime.haveAccess(con.getModifiers(), targetType, invoker)) continue;

                        if (con.getParameterTypes().length == args.length) {
                                if (canBeCandidate(con.getParameterTypes(), args, primitives)) {
                                        candidates.add(con);
                                }
                        }
                }

                if (candidates.isEmpty()) {
                        StringBuilder sb = new StringBuilder().append(targetType.getName()).append("(");
                        buildErrorMessageArgsPart(sb, args);
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
         * @param isStatic         whether the invocation is static
         * @param functionalObject the object to invoke functional method on if method not found
         * @param invoker          from which class invokes the method
         * @param method           method name
         * @param primitives       whether the argument is primitive
         * @param args             the arguments
         * @param canInvokeImport  the method is invoked directly by the method's name, which could be invoking an import static method
         * @return the method result (void methods' results are <tt>Unit</tt>)
         * @throws Throwable exception
         */
        public static Object invoke(InvocationState invocationState,
                                    Class<?> targetClass,
                                    Object o,
                                    boolean isStatic,
                                    Object functionalObject,
                                    Class<?> invoker,
                                    String method,
                                    boolean[] primitives,
                                    Object[] args,
                                    boolean canInvokeImport) throws Throwable {

                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");
                Method methodToInvoke = findMethod(invoker, targetClass, o, method, primitives, args);
                // method found ?
                if (null != methodToInvoke) {
                        try {
                                return invokeMethod(methodToInvoke, o, args);
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

                                return invoke(invocationState, targetClass, res, isStatic, null, invoker, "get", bs, as, canInvokeImport);
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

                                        return invoke(invocationState, targetClass, elem, isStatic, null, invoker, "set", bs, as, canInvokeImport);
                                }
                        } else {
                                ec.add("Target is array but method is not get(int)");
                                ec.add("Target is array but method is not set(int, ...)");
                        }
                } else {
                        // null string append
                        if (!isStatic && o == null && method.equals("add") && args.length == 1 && args[0] instanceof String) {
                                return "null" + args[0];
                        }
                        // implicit cast
                        if (o != null && invoker.isAnnotationPresent(ImplicitImports.class)) {
                                Class<?>[] implicitClasses = invoker.getAnnotation(ImplicitImports.class).implicitImports();
                                if (implicitClasses.length == 0) {
                                        ec.add("No implicit casts enabled");
                                } else {
                                        for (Class<?> ic : implicitClasses) {
                                                if (!ic.isAnnotationPresent(LatteObject.class)) continue;
                                                Method[] methods = ic.getDeclaredMethods();
                                                for (Method m : methods) {
                                                        if (m.isAnnotationPresent(Implicit.class) && m.getParameterTypes().length == 1 && m.getReturnType() != void.class) {
                                                                Class<?> inputType = m.getParameterTypes()[0];
                                                                if (inputType.isInstance(o)) {
                                                                        Class<?> outputType = m.getReturnType();
                                                                        Method foundMethod = findMethod(invoker, outputType, o, method, primitives, args);
                                                                        if (foundMethod == null) {
                                                                                ec.add("Still cannot find method if casting " + o.getClass().getName() + " to " + m.getReturnType());
                                                                                continue;
                                                                        }
                                                                        // get object instance
                                                                        Object implicitInstance = ic.getField("singletonInstance").get(null);
                                                                        // invoke method
                                                                        m.setAccessible(true);
                                                                        Object castInstance = m.invoke(implicitInstance, o);
                                                                        return invokeMethod(foundMethod, castInstance, args);
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } else {
                                ec.add("No implicit casts enabled");
                        }

                        if (method.equals("set")) {
                                return invoke(invocationState, targetClass, o, isStatic, functionalObject, invoker, "put", primitives, args, canInvokeImport);
                        } else {
                                ec.add("Is not set/put transform");
                        }
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

                // dynamically get field `o.methodName`
                // if it's not `null` and not `Unit` then invoke the retrieved object
                if (!invocationState.fromField && o != null) {
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

                // check import static
                if (canInvokeImport) {
                        if (invoker.isAnnotationPresent(StaticImports.class)) {
                                StaticImports si = invoker.getAnnotation(StaticImports.class);
                                Class<?>[] classes = si.staticImports();
                                for (Class<?> cls : classes) {
                                        Method m = findMethod(invoker, cls, null, method, primitives, args);
                                        if (m == null) continue;
                                        return invokeMethod(m, null, args);
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
                buildErrorMessageArgsPart(sb, args);
                sb.append(")");
                ec.throwIfNotEmpty("Cannot find method to invoke: " + sb.toString(), new Function1<Throwable, String>() {
                        @Override
                        public Throwable apply(String s) {
                                return new LtRuntimeException(s);
                        }
                });
                // code won't reach here
                throw new LtBug("code won't reach here");
        }

        private static Object invokeMethod(Method m, Object target, Object[] args) throws InvocationTargetException, IllegalAccessException {
                m.setAccessible(true);
                Object res = m.invoke(target, args);
                if (m.getReturnType().equals(void.class)) return Unit.get();
                return res;
        }

        private static void buildErrorMessageArgsPart(StringBuilder sb, Object[] args) {
                boolean isFirst = true;
                for (Object arg : args) {
                        if (isFirst) isFirst = false;
                        else sb.append(", ");
                        sb.append(arg == null ? "null" : arg.getClass().getName());
                }
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
        public static Object callFunctionalObject(Object functionalObject,
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
        public static Object callFunctionalObject(InvocationState invocationState,
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
                                functionalObject.getClass(), functionalObject, false, null, callerClass, "apply", new boolean[args.length], args, false);
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
         * @param o                invoke the method on the object (or null if invoke static)\
         * @param isStatic         whether the invocation is static
         * @param functionalObject the object to invoke functional method on if method not found
         * @param invoker          from which class invokes the method
         * @param method           method name
         * @param primitives       whether the argument is primitive
         * @param args             the arguments
         * @param canInvokeImport  whether the invocation is allowed to invoke methods from import static
         * @return the method result (void methods' results are <tt>Unit</tt>)
         * @throws Throwable exception
         */
        @SuppressWarnings("unused")
        public static Object invoke(Class<?> targetClass, Object o, boolean isStatic, Object functionalObject, Class<?> invoker,
                                    String method, boolean[] primitives, Object[] args, boolean canInvokeImport) throws Throwable {
                return invoke(new InvocationState(), targetClass, o, isStatic, functionalObject, invoker, method, primitives, args, canInvokeImport);
        }
}
