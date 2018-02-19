package lt.compiler.semantic.helper;

import lt.compiler.LtBug;
import lt.compiler.SyntaxException;
import lt.compiler.syntactic.AST.Access;
import lt.lang.function.Function1;

import java.util.ArrayList;
import java.util.List;

public class HalfAppliedTypes {
        private List<Access> types = new ArrayList<Access>();
        private Function1<Void, Access> apply;

        private void doApply(Access type) throws SyntaxException {
                try {
                        apply.apply(type);
                } catch (SyntaxException e) {
                        throw e;
                } catch (Exception e) {
                        throw new LtBug(e);
                }
        }

        /**
         * @param apply function (importList, packageName, type) -&gt; void
         * @throws SyntaxException exception
         */
        public void setApply(Function1<Void, Access> apply) throws SyntaxException {
                this.apply = apply;
                for (Access t : types) {
                        doApply(t);
                }
        }

        public void add(Access type) throws SyntaxException {
                types.add(type);
                if (apply != null) {
                        doApply(type);
                }
        }
}
