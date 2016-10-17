package lt.repl;

import java.util.Scanner;

/**
 * scanner line reader
 */
public class ScannerLineReader implements LineReader {
        private Scanner scanner;

        public ScannerLineReader() {
                System.out.println("using java.util.Scanner to read input");
        }

        @Override
        public String readLine() throws Exception {
                if (scanner == null) {
                        scanner = new Scanner(System.in);
                }
                return scanner.nextLine();
        }
}
