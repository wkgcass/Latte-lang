package lt.compiler.syntactic.pre;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Pre;

/**
 * declare current package
 */
public class PackageDeclare implements Pre {
        public final AST.PackageRef pkg;
        private final LineCol lineCol;

        public PackageDeclare(AST.PackageRef pkg, LineCol lineCol) {
                this.pkg = pkg;
                this.lineCol = lineCol;
        }

        @Override
        public String toString() {
                return "(# " + pkg + ")";
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                PackageDeclare that = (PackageDeclare) o;

                return pkg.equals(that.pkg);
        }

        @Override
        public int hashCode() {
                return pkg.hashCode();
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
