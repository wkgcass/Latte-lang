package lt.compiler.syntactic.pre;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Pre;

import java.util.List;

/**
 * import
 */
public class Import implements Pre {
        public static class ImportDetail {
                public final AST.PackageRef pkg;
                public final AST.Access access;
                public final boolean importAll;

                public ImportDetail(AST.PackageRef pkg, AST.Access access, boolean importAll) {
                        this.pkg = pkg;
                        this.access = access;
                        this.importAll = importAll;
                }

                @Override
                public String toString() {
                        if (pkg == null) {
                                if (importAll) {
                                        return access.toString() + "._";
                                } else {
                                        return access.toString();
                                }
                        } else {
                                if (importAll) {
                                        return pkg.toString() + "._";
                                } else {
                                        return "(invalid import)";
                                }
                        }
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        ImportDetail that = (ImportDetail) o;

                        if (importAll != that.importAll) return false;
                        if (pkg != null ? !pkg.equals(that.pkg) : that.pkg != null) return false;
                        return !(access != null ? !access.equals(that.access) : that.access != null);
                }

                @Override
                public int hashCode() {
                        int result = pkg != null ? pkg.hashCode() : 0;
                        result = 31 * result + (access != null ? access.hashCode() : 0);
                        result = 31 * result + (importAll ? 1 : 0);
                        return result;
                }
        }

        public final List<ImportDetail> importDetails;
        private final LineCol lineCol;

        public Import(List<ImportDetail> importDetails, LineCol lineCol) {
                this.importDetails = importDetails;
                this.lineCol = lineCol;
        }

        @Override
        public String toString() {
                return "(#> " + importDetails + ")";
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Import anImport = (Import) o;

                return importDetails.equals(anImport.importDetails);
        }

        @Override
        public int hashCode() {
                return importDetails.hashCode();
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
