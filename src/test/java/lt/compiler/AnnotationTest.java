package lt.compiler;

import lt.compiler.semantic.SModifier;

/**
 * annotation for testing
 */
public @interface AnnotationTest {
        String str() default "str";

        int i() default 100;

        long l() default 100;

        short s() default 100;

        byte b() default 100;

        boolean bo() default true;

        char c() default 'a';

        float f() default 100;

        double d() default 100;

        MyAnno anno() default @MyAnno(str = "a");

        Class<?> cls() default String.class;

        SModifier e() default SModifier.PUBLIC;

        String[] strArr() default {"a", "b"};

        Class<?>[] clsArr() default {Class.class, String.class};
}
