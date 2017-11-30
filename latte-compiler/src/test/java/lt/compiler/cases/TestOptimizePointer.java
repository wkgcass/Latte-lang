package lt.compiler.cases;

import lt.dependencies.asm.ClassReader;
import lt.dependencies.asm.ClassVisitor;
import lt.dependencies.asm.MethodVisitor;
import lt.dependencies.asm.Opcodes;
import lt.lang.Pointer;
import org.junit.Test;

import java.util.Arrays;

import static lt.compiler.cases.TestCodeGen.retrieveClass;
import static lt.compiler.cases.TestCodeGen.retrieveByteCode;

import static org.junit.Assert.*;

/**
 * test optimize pointer
 */
public class TestOptimizePointer {
        @Test
        public void testPrimitivePass() throws Exception {
                Class<?> cls = retrieveClass("" +
                                "class TestPrimitive\n" +
                                "  def f_int(i:int)=null\n" +
                                "  def f_float(f:float)=null\n" +
                                "  def f_double(d:double)=null\n" +
                                "  def f_long(l:long)=null\n" +
                                "  def f_bool(b:bool)=null\n" +
                                "  def f_char(c:char)=null\n" +
                                "  def f_byte(b:byte)=null\n" +
                                "  def f_short(s:short)=null\n"
                        , "TestPrimitive");
                for (Class<?> c : Arrays.asList(
                        int.class, float.class, double.class, long.class,
                        boolean.class, char.class, byte.class, short.class
                )) {
                        if (c == boolean.class) {
                                cls.getMethod("f_bool", c);
                        } else {
                                cls.getMethod("f_" + c.getName(), c);
                        }
                }
        }

        private void _testByteCodeNoMethod(byte[] bytes, int methodCount) {
                ClassReader cr = new ClassReader(bytes);
                final Pointer<Integer> count = new Pointer<Integer>(false, false);
                try {
                        count.set(0);
                } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        fail("error");
                }
                cr.accept(new ClassVisitor(Opcodes.ASM5) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                                if (name.contains("$")) {
                                        return null;
                                }
                                if (!name.equals("<init>") && !name.equals("<clinit>")) {
                                        try {
                                                count.set(count.get() + 1);
                                        } catch (Throwable throwable) {
                                                throwable.printStackTrace();
                                                fail("error");
                                        }
                                }
                                return new MethodVisitor(Opcodes.ASM5) {
                                        @Override
                                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                                if (!name.equals("<init>") && !owner.contains("$Latte$Lambda$")) {
                                                        fail();
                                                }
                                        }
                                };
                        }
                }, 0);
                int i_count = count.get();
                assertEquals(methodCount, i_count);
        }

        @Test
        public void testByteCode() throws Exception {
                byte[] bytes = retrieveByteCode("" +
                        "class TestByteCodeInt\n" +
                        "  def f_int(i:int):int=i\n" +
                        "  def f_float(f:float):float=f\n" +
                        "  def f_double(d:double):double=d\n" +
                        "  def f_long(l:long):long=l\n" +
                        "  def f_bool(b:bool):bool=b\n" +
                        "  def f_char(c:char):char=c\n" +
                        "  def f_byte(b:byte):byte=b\n" +
                        "  def f_short(s:short):short=s\n" +
                        "  def f_string(s:String):String=s\n")
                        .get("TestByteCodeInt");
                _testByteCodeNoMethod(bytes, 9);
        }

        @Test
        public void testByteCodeWithLambda() throws Exception {
                byte[] bytes = retrieveByteCode("" +
                        "class TestByteCodeWithLambda\n" +
                        "  def f_int(i:int)=()->1\n" +
                        "  def f_long(l:long)=()->1 as long\n" +
                        "  def f_string(s:String)=()->''\n")
                        .get("TestByteCodeWithLambda");
                _testByteCodeNoMethod(bytes, 3);
        }

        @Test
        public void testByteCodeWithInnerMethod() throws Exception {
                byte[] bytes = retrieveByteCode("" +
                        "class TestByteCodeWithInnerMethod\n" +
                        "  def f_int(i:int)\n" +
                        "    def x=1\n" +
                        "    null\n" +
                        "  def f_long(l:long)\n" +
                        "    def y=2\n" +
                        "    null\n" +
                        "  def f_string(s:String)\n" +
                        "    def z=3\n" +
                        "    null")
                        .get("TestByteCodeWithInnerMethod");
                _testByteCodeNoMethod(bytes, 3);
        }
}
