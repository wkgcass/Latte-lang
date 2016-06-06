package lt.compiler;

import lt.compiler.semantic.SModifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * test all kinds of annos
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestAllKindsOfAnnos {
        String str();

        String[] strArr();

        int i();

        int[] iArr();

        short s();

        short[] sArr();

        byte b();

        byte[] bArr();

        char c();

        char[] cArr();

        boolean bo();

        boolean[] boArr();

        long l();

        long[] lArr();

        float f();

        float[] fArr();

        double d();

        double[] dArr();

        Class<?> cls();

        Class<?>[] clsArr();

        SModifier en();

        SModifier[] enArr();

        MyAnno anno();

        MyAnno[] annos();
}
