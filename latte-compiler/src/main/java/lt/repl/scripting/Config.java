package lt.repl.scripting;

/**
 * configurations for source code to compile
 */
public class Config {
        public static final int SCANNER_TYPE_INDENT = 0;
        public static final int SCANNER_TYPE_BRACE = 1;

        private int scannerType;
        private String varNamePrefix;
        private boolean eval;

        public int getScannerType() {
                return scannerType;
        }

        public Config setScannerType(int scannerType) {
                this.scannerType = scannerType;
                return this;
        }

        public String getVarNamePrefix() {
                return varNamePrefix;
        }

        public Config setVarNamePrefix(String varNamePrefix) {
                this.varNamePrefix = varNamePrefix;
                return this;
        }

        public boolean isEval() {
                return eval;
        }

        public Config setEval(boolean eval) {
                this.eval = eval;
                return this;
        }
}
