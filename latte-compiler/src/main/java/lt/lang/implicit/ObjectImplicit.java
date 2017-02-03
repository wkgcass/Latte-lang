package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LatteObject;

/**
 * implicit casts for object
 */
@Implicit
@LatteObject
public class ObjectImplicit {
        public static final ObjectImplicit singletonInstance = new ObjectImplicit();

        private ObjectImplicit() {
        }

        @Implicit
        public final RichObject cast(Object s) {
                return new RichObject(s);
        }
}
