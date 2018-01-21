package lt.compiler.semantic.helper;

import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;

import java.util.List;

public class ASTGHolder<S extends Statement> {
        public final S s;
        public final List<STypeDef> generics;


        public ASTGHolder(S s, List<STypeDef> generics) {
                this.s = s;
                this.generics = generics;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ASTGHolder<?> that = (ASTGHolder<?>) o;

                if (!s.equals(that.s)) return false;
                //
                return generics.equals(that.generics);
        }

        @Override
        public int hashCode() {
                int result = s.hashCode();
                result = 31 * result + generics.hashCode();
                return result;
        }
}
