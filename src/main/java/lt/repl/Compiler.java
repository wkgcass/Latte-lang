package lt.repl;

import lt.compiler.*;
import lt.compiler.Scanner;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

/**
 * the compiler
 */
public class Compiler {
        private JarLoader jarLoader = new JarLoader();

        private int _INDENT = 4;

        private File outputDir;
        private Set<File> inputDirs = new HashSet<>();

        public Compiler add(String jarFile) throws IOException, ClassNotFoundException {
                return add(new JarFile(jarFile));
        }

        public Compiler add(File jarFile) throws IOException, ClassNotFoundException {
                return add(new JarFile(jarFile));
        }

        public Compiler add(JarFile jarFile) throws IOException, ClassNotFoundException {
                jarLoader.loadAll(jarFile);
                return this;
        }

        public Compiler shiftLeft(String fileDir) {
                return shiftLeft(new File(fileDir));
        }

        public Compiler shiftLeft(File file) {
                if (!file.isDirectory()) throw new IllegalArgumentException(file + " is not a directory");
                inputDirs.add(file);
                return this;
        }

        public Compiler shiftRight(String fileDir) {
                return shiftRight(new File(fileDir));
        }

        public Compiler shiftRight(File file) {
                if (!file.isDirectory()) throw new IllegalArgumentException(file + " is not a directory");
                outputDir = file;
                return this;
        }

        public Compiler compile() throws IOException, SyntaxException {
                if (outputDir == null) throw new NullPointerException("output directory is null");

                Map<String, List<Statement>> statements = new HashMap<>();

                for (File dir : inputDirs) {
                        for (File f : dir.listFiles()) {
                                if (f.getName().endsWith(".lt")) {
                                        Reader reader = new FileReader(f);
                                        Scanner scanner = new Scanner(f.getName(), reader, new Scanner.Properties());
                                        ElementStartNode root = scanner.parse();

                                        Parser parser = new Parser(root);
                                        statements.put(f.getName(), parser.parse());
                                }
                        }
                }

                SemanticProcessor processor = new SemanticProcessor(statements, Thread.currentThread().getContextClassLoader());
                Set<STypeDef> types = processor.parse();
                CodeGenerator codeGen = new CodeGenerator(types);
                Map<String, byte[]> results = codeGen.generate();

                for (Map.Entry<String, byte[]> result : results.entrySet()) {
                        String className = result.getKey();
                        byte[] bytes = result.getValue();

                        String dir = outputDir.getAbsolutePath();
                        File path = new File(dir + File.separator + (className.contains(".") ? className.substring(0, className.lastIndexOf('.')).replace(".", File.separator) : ""));
                        if (!path.exists()) if (!path.mkdirs()) throw new IOException("cannot create directory " + path);

                        File newFile = new File(path + File.separator + (className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className) + ".class");
                        if (!newFile.exists()) if (!newFile.createNewFile()) throw new IOException("cannot create file " + newFile);

                        FileOutputStream fos = new FileOutputStream(newFile);
                        fos.write(bytes);
                        fos.close();
                }

                return this;
        }
}
