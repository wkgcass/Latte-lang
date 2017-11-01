package lt.compiler.cases;

import lt.compiler.functionalInterfaces.*;
import lt.lang.Pointer;
import lt.lang.function.Function0;
import lt.lang.function.Function1;
import lt.repl.Compiler;
import lt.repl.scripting.CL;
import lt.runtime.LambdaGen;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * test lambda generator
 */
public class TestLambdaGen {
        @Test
        public void testNoParamReturnUnit() throws Exception {
                final Pointer<String> p = new Pointer<String>(false, false);
                final Pointer<Object> selfPointer = new Pointer<Object>(false, false);
                Function0<Object> f = new Function0<Object>() {
                        public Object self;

                        @Override
                        public Object apply() throws Exception {
                                try {
                                        p.set("hello");
                                        selfPointer.set(this.self);
                                } catch (Throwable throwable) {
                                        throw new AssertionError(throwable);
                                }
                                return null;
                        }
                };
                Map.Entry<String, byte[]> res = LambdaGen.gen(f, NoParamReturnUnit.class);
                String clsName = res.getKey();
                byte[] bytes = res.getValue();
                CL cl = new CL(Thread.currentThread().getContextClassLoader());
                cl.addByteCodes(clsName, bytes);
                @SuppressWarnings("unchecked")
                Class<NoParamReturnUnit> resCls = (Class<NoParamReturnUnit>) cl.loadClass(clsName);
                NoParamReturnUnit o = resCls.getConstructor(Function0.class).newInstance(f);
                o.x();

                assertEquals("hello", p.get());
                assertTrue(selfPointer.get() instanceof NoParamReturnUnit);
        }

        @Test
        public void testPrimitiveInReturnUnit() throws Exception {
                final Pointer<Object> p = new Pointer<Object>(false, false);
                final Pointer<Object> selfPointer = new Pointer<Object>(false, false);
                Function1<Object, Object> f = new Function1<Object, Object>() {
                        public Object self;

                        @Override
                        public Object apply(Object o) throws Exception {
                                try {
                                        selfPointer.set(this.self);
                                        p.set(o);
                                } catch (Throwable throwable) {
                                        throw new AssertionError(throwable);
                                }
                                return null;
                        }
                };
                Class<?>[] cases = new Class[]{
                        IntParamReturnUnit.class,
                        LongParamReturnUnit.class,
                        FloatParamReturnUnit.class,
                        DoubleParamReturnUnit.class,
                        ByteParamReturnUnit.class,
                        BoolParamReturnUnit.class,
                        ShortParamReturnUnit.class,
                        CharParamReturnUnit.class
                };
                for (Class<?> c : cases) {
                        Method x = c.getMethods()[0];
                        Class<?> param = x.getParameterTypes()[0];
                        Object inputArg = generatePrimitive(param);
                        Map.Entry<String, byte[]> res = LambdaGen.gen(f, c);
                        String clsName = res.getKey();
                        byte[] bytes = res.getValue();
                        CL cl = new CL(Thread.currentThread().getContextClassLoader());
                        cl.addByteCodes(clsName, bytes);
                        @SuppressWarnings("unchecked")
                        Class<?> resCls = cl.loadClass(clsName);
                        Object theObj = resCls.getConstructor(Function1.class).newInstance(f);
                        x.invoke(theObj, inputArg);

                        assertEquals(inputArg, p.get());
                        assertTrue(c.isInstance(selfPointer.get()));
                }
        }

        @Test
        public void testIntParamReturnInt() throws Exception {
                final Pointer<Object> selfPointer = new Pointer<Object>(false, false);
                Function1<Object, Object> f = new Function1<Object, Object>() {
                        public Object self;

                        @Override
                        public Object apply(Object o) throws Exception {
                                try {
                                        selfPointer.set(this.self);
                                } catch (Throwable throwable) {
                                        throw new AssertionError(throwable);
                                }
                                return o;
                        }
                };
                Map.Entry<String, byte[]> res = LambdaGen.gen(f, IntParamReturnInt.class);
                String clsName = res.getKey();
                byte[] bytes = res.getValue();
                CL cl = new CL(Thread.currentThread().getContextClassLoader());
                cl.addByteCodes(clsName, bytes);
                @SuppressWarnings("unchecked")
                Class<IntParamReturnInt> resCls = (Class<IntParamReturnInt>) cl.loadClass(clsName);
                IntParamReturnInt o = resCls.getConstructor(Function1.class).newInstance(f);

                assertEquals(22, o.x(22));
                assertTrue(selfPointer.get() instanceof IntParamReturnInt);
        }

        @Test
        public void testNoParamReturnPrimitive() throws Exception {
                Class<?>[] cases = new Class[]{
                        NoParamReturnInt.class,
                        NoParamReturnFloat.class,
                        NoParamReturnLong.class,
                        NoParamReturnDouble.class,
                        NoParamReturnByte.class,
                        NoParamReturnBool.class,
                        NoParamReturnShort.class,
                        NoParamReturnChar.class
                };
                for (Class<?> c : cases) {
                        final Pointer<Object> selfPointer = new Pointer<Object>(false, false);

                        Method x = c.getMethods()[0];
                        final Class<?> returnType = x.getReturnType();
                        Function0<Object> f = new Function0<Object>() {
                                public Object self;

                                @Override
                                public Object apply() throws Exception {
                                        try {
                                                selfPointer.set(this.self);
                                        } catch (Throwable throwable) {
                                                throw new AssertionError(throwable);
                                        }

                                        return generatePrimitive(returnType);
                                }
                        };
                        Map.Entry<String, byte[]> res = LambdaGen.gen(f, c);
                        String clsName = res.getKey();
                        byte[] bytes = res.getValue();
                        CL cl = new CL(Thread.currentThread().getContextClassLoader());
                        cl.addByteCodes(clsName, bytes);
                        @SuppressWarnings("unchecked")
                        Class<?> resCls = cl.loadClass(clsName);
                        Object theObj = resCls.getConstructor(Function0.class).newInstance(f);

                        assertEquals(generatePrimitive(returnType), x.invoke(theObj));
                        assertTrue(c.isInstance(selfPointer.get()));
                }
        }

        private Object generatePrimitive(Class<?> param) {
                Object inputArg;
                if (param == int.class) {
                        inputArg = 22;
                } else if (param == float.class) {
                        inputArg = 33f;
                } else if (param == double.class) {
                        inputArg = 44.0;
                } else if (param == long.class) {
                        inputArg = 55L;
                } else if (param == byte.class) {
                        inputArg = (byte) 66;
                } else if (param == char.class) {
                        inputArg = (char) 77;
                } else if (param == short.class) {
                        inputArg = (short) 88;
                } else if (param == boolean.class) {
                        inputArg = true;
                } else {
                        fail("should not reach here");
                        return null;
                }
                return inputArg;
        }
}
