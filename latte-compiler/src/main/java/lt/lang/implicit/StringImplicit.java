package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LatteObject;

/**
 * implicit casts for string
 */
@Implicit
@LatteObject
public class StringImplicit {
        public static final StringImplicit singletonInstance = new StringImplicit();

        private StringImplicit() {
        }

        @Implicit
        public final RichString cast(String s) {
                return new RichString(s);
        }
}
