package lt.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * test
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE_PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface MyAnno {
        String str();

        int i() default 100;
}
