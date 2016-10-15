package lt.compiler;

import lt.compiler.lexical.ElementStartNode;

import java.io.IOException;
import java.io.Reader;

/**
 * the entrance of scanners. it will read the first line of the file to determine which scanner to use.
 */
public class ScannerSwitcher implements Scanner {
        private final Scanner scanner;

        public ScannerSwitcher(String fileName, Reader reader, Properties properties, ErrorManager err) throws IOException, SyntaxException {
                PushLineBackReader plbr;
                if (reader instanceof PushLineBackReader) {
                        plbr = (PushLineBackReader) reader;
                } else {
                        plbr = new PushLineBackReader(reader);
                }

                String firstLine = plbr.readLine();
                plbr.push(firstLine);
                String text = firstLine.trim();
                if (text.startsWith(";;")) {
                        text = text.substring(2).trim();
                } else {
                        text = "";
                }
                switch (text) {
                        case "brace":
                                scanner = new BraceScanner(fileName, plbr, properties, err);
                                break;
                        case "indent":
                        default:
                                scanner = new IndentScanner(fileName, plbr, properties, err);
                }
        }

        @Override
        public ElementStartNode scan() throws IOException, SyntaxException {
                return scanner.scan();
        }
}