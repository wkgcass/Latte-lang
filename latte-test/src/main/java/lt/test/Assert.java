package lt.test;

import lt.lang.Dynamic;

/**
 * a functional object which represents the success of test
 */
public class Assert {
        private final Context context;

        public Assert(Context context) {
                this.context = context;
        }

        public static Object call(Object $this, String methodName, boolean[] primitives, Object[] arguments) {
                if ($this == null) throw new IllegalArgumentException("cannot invoke via static");
                Assert ass = (Assert) $this;
                try {
                        return Dynamic.invoke(new Dynamic.InvocationState(), org.junit.Assert.class, null, null,
                                Assert.class, methodName, primitives, arguments);
                } catch (Throwable t) {
                        if (t instanceof AssertionError) {
                                ass.context.assertFail((AssertionError) t);
                        } else {
                                ass.context.assertFail(new AssertionError(t));
                        }
                }
                return null;
        }

        public void apply() {
                context.finished();
        }
}
