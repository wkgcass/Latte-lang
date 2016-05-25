/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.repl;

import lt.compiler.*;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;
import lt.lang.Wrapper;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * a compiler with full functions provided. lt.repl.Compiler is a small set of this compiler<br>
 * first record all necessary info<br>
 * then creates a ThreadPool to run Scanner and Parser<br>
 * then summaries these AST to do semantic analysis in a single thread<br>
 * finally creates a ThreadPool to run Code Generation and write files to disk (or store these byte code for loading)<br>
 * if requires loading, then load all these generated classes
 */
public class Compiler {
        private static final int availableProcessors = Runtime.getRuntime().availableProcessors();

        public static class Config {
                /**
                 * class-path
                 */
                public List<URL> classpath = new ArrayList<>();

                /**
                 * configuration about each step use how many threads
                 */
                public static class Threads {
                        /**
                         * thread count for scanners
                         */
                        public int scanner = availableProcessors;
                        /**
                         * thread count for parsers
                         */
                        public int parser = availableProcessors;
                        /**
                         * thread count for codeGen
                         */
                        public int codeGen = availableProcessors;
                }

                /**
                 * thread configuration
                 */
                public Threads threads = new Threads();

                /**
                 * configuration about coding
                 */
                public static class Code {
                        /**
                         * these imports will be added when doing semantic analysis
                         */
                        public List<String> autoImport = new ArrayList<>();
                        /**
                         * indentation of the source code
                         */
                        public int indentation = 4;
                        /**
                         * line base
                         */
                        public int lineBase = 0;
                        /**
                         * column base
                         */
                        public int columnBase = 0;
                }

                /**
                 * code configuration
                 */
                public Code code = new Code();

                /**
                 * out configuration
                 */
                public ErrorManager.Out out = new ErrorManager.Out();

                /**
                 * throw exception immediately when meets a syntax exception
                 */
                public boolean fastFail = true;

                /**
                 * configuration about the result
                 */
                public static class Result {
                        /**
                         * output directory
                         */
                        public File outputDir = null;
                        /**
                         * generate html statistic about this compiling process<br>
                         * only enabled when outputDir is not null
                         */
                        public boolean statistic = false;
                }

                /**
                 * result configuration
                 */
                public Result result = new Result();
        }

        private final Config config = new Config();
        private final ClassLoader baseLoader;

        /**
         * construct the compiler
         */
        public Compiler() {
                baseLoader = ClassLoader.getSystemClassLoader();
        }

        /**
         * construct the compiler with base class loader
         *
         * @param baseLoader the base class loader
         */
        public Compiler(ClassLoader baseLoader) {
                this.baseLoader = baseLoader;
        }

        /**
         * add a jarFile to class-path
         *
         * @param dirOrJar directory or jar file location
         * @return the compiler itself
         * @throws IOException exceptions
         */
        public Compiler add(String dirOrJar) throws IOException {
                return add(new File(dirOrJar));
        }

        /**
         * add a jarFile to class-path
         *
         * @param file the file representing url
         * @return the compiler itself
         * @throws IOException exceptions
         */
        public Compiler add(File file) throws IOException {
                return add(new URL(file.toURI().toString()));
        }

        /**
         * add a jarFile to class-path
         *
         * @param url the url
         * @return the compiler itself
         */
        public Compiler add(URL url) {
                config.classpath.add(url);
                return this;
        }

        /**
         * configure using given config
         *
         * @param config a map containing configuration
         * @return this
         * @throws Exception all kinds of exceptions
         */
        public Compiler configure(Map config) throws Exception {
                if (config != null) {

                        List<URL> classpathToSet = new ArrayList<>();
                        Config.Threads threads = new Config.Threads();
                        Config.Code code = new Config.Code();
                        ErrorManager.Out out = new ErrorManager.Out();
                        boolean fastFail = true;
                        Config.Result result = new Config.Result();

                        if (config.containsKey("classpath")) {
                                final String exMsg = "config.classpath should be a list of JarFile/File/String";

                                Object o = config.get("classpath");
                                if (o instanceof List) {
                                        for (Object e : (List) o) {
                                                if (e instanceof String) {
                                                        addIntoClasspathList(classpathToSet, (String) e);
                                                } else if (e instanceof File) {
                                                        addIntoClasspathList(classpathToSet, (File) e);
                                                } else if (e instanceof URL) {
                                                        addIntoClasspathList(classpathToSet, (URL) e);
                                                } else throw new IllegalArgumentException(exMsg);
                                        }
                                } else throw new IllegalArgumentException(exMsg);
                        }
                        if (config.containsKey("threads")) {
                                Object o = config.get("threads");
                                if (o instanceof Map) {
                                        Map t = (Map) o;
                                        if (t.containsKey("scanner")) {
                                                Object scanner = t.get("scanner");
                                                if (scanner instanceof Integer && ((int) scanner) >= 1) {
                                                        threads.scanner = (int) scanner;
                                                } else throw new IllegalArgumentException("config.threads.scanner should be Integer and >=1");
                                        }
                                        if (t.containsKey("parser")) {
                                                Object parser = t.get("parser");
                                                if (parser instanceof Integer && ((int) parser) >= 1) {
                                                        threads.parser = (int) parser;
                                                } else throw new IllegalArgumentException("config.threads.parser should be Integer and >= 1");
                                        }
                                        if (t.containsKey("codeGen")) {
                                                Object codeGen = t.get("codeGen");
                                                if (codeGen instanceof Integer && ((int) codeGen) >= 1) {
                                                        threads.codeGen = (int) codeGen;
                                                } else throw new IllegalArgumentException("config.threads.codeGen should be Integer and >=1");
                                        }
                                } else throw new IllegalArgumentException("config.threads should be {scanner:?, parser:?, codeGen:?}");
                        }
                        if (config.containsKey("code")) {
                                Object o = config.get("code");
                                if (o instanceof Map) {
                                        Map c = (Map) o;
                                        if (c.containsKey("autoImport")) {
                                                Object ai = c.get("autoImport");
                                                final String exMsg = "config.code.autoImport should be List of strings";

                                                if (ai instanceof List) {
                                                        for (Object autoImport : (List) ai) {
                                                                if (autoImport instanceof String) {
                                                                        code.autoImport.add((String) autoImport);
                                                                } else throw new IllegalArgumentException(exMsg);
                                                        }

                                                } else throw new IllegalArgumentException(exMsg);
                                        }
                                        if (c.containsKey("indentation")) {
                                                Object i = c.get("indentation");
                                                if (i instanceof Integer && ((int) i) >= 1) {
                                                        code.indentation = (int) i;
                                                } else throw new IllegalArgumentException("config.code.indentation should be Integer and >=1");
                                        }
                                        if (c.containsKey("lineBase")) {
                                                Object l = c.get("lineBase");
                                                if (l instanceof Integer) {
                                                        code.lineBase = (int) l;
                                                } else throw new IllegalArgumentException("config.code.lineBase should be Integer");
                                        }
                                        if (c.containsKey("columnBase")) {
                                                Object co = c.get("columnBase");
                                                if (co instanceof Integer) {
                                                        code.columnBase = (int) co;
                                                } else throw new IllegalArgumentException("config.code.columnBase should be Integer");
                                        }
                                } else throw new IllegalArgumentException("config.code should be {autoImport:?, indentation:?, lineBase:?, columnBase:?}");
                        }
                        if (config.containsKey("out")) {
                                Object o = config.get("out");
                                if (o instanceof Map) {
                                        Map ou = (Map) o;
                                        if (ou.containsKey("debug")) {
                                                Object debug = ou.get("debug");
                                                if (debug instanceof PrintStream) {
                                                        out.debug = (PrintStream) debug;
                                                } else throw new IllegalArgumentException("config.out.debug should be PrintStream");
                                        }
                                        if (ou.containsKey("info")) {
                                                Object info = ou.get("info");
                                                if (info instanceof PrintStream) {
                                                        out.info = (PrintStream) info;
                                                } else throw new IllegalArgumentException("config.out.info should be PrintStream");
                                        }
                                        if (ou.containsKey("warn")) {
                                                Object warn = ou.get("warn");
                                                if (warn instanceof PrintStream) {
                                                        out.warn = (PrintStream) warn;
                                                } else throw new IllegalArgumentException("config.out.warn should be PrintStream");
                                        }
                                        if (ou.containsKey("err")) {
                                                Object error = ou.get("err");
                                                if (error instanceof PrintStream) {
                                                        out.err = (PrintStream) error;
                                                } else throw new IllegalArgumentException("config.out.err should be PrintStream");
                                        }
                                } else throw new IllegalArgumentException("config.out should be {debug:?, info:?, warn:?, err:?}");
                        }
                        if (config.containsKey("fastFail")) {
                                Object f = config.get("fastFail");
                                if (f instanceof Boolean) {
                                        fastFail = (boolean) f;
                                } else throw new IllegalArgumentException("config.fastFail should be Boolean");
                        }
                        if (config.containsKey("result")) {
                                Object r = config.get("result");
                                if (r instanceof Map) {
                                        Map re = (Map) r;
                                        if (re.containsKey("outputDir")) {
                                                Object o = re.get("outputDir");
                                                if (o instanceof String) {
                                                        File f = new File((String) o);
                                                        if (f.isDirectory()) {
                                                                result.outputDir = f;
                                                        } else throw new IllegalArgumentException("config.result.outputDir should be a directory");
                                                } else if (o instanceof File) {
                                                        if (((File) o).isDirectory()) {
                                                                result.outputDir = (File) o;
                                                        } else throw new IllegalArgumentException("config.result.outputDir should be a directory");
                                                } else throw new IllegalArgumentException("config.result.outputDir should be File/String");
                                        }
                                        if (re.containsKey("statistic")) {
                                                Object o = re.get("statistic");
                                                if (o instanceof Boolean) {
                                                        result.statistic = (boolean) o;
                                                } else throw new IllegalArgumentException("config.result.statistic should be Boolean");
                                        }
                                } else throw new IllegalArgumentException("config.result should be {outputDir:?, statistic:?}");
                        }

                        this.config.classpath = classpathToSet;
                        this.config.threads = threads;
                        this.config.code = code;
                        this.config.out = out;
                        this.config.fastFail = fastFail;
                        this.config.result = result;
                }

                return this;
        }

        private void addIntoClasspathList(List<URL> list, String fileName) throws IOException {
                File f = new File(fileName);
                if (f.exists()) {
                        addIntoClasspathList(list, f);
                } else throw new IllegalArgumentException(fileName + " does not exist");
        }

        private void addIntoClasspathList(List<URL> list, File file) throws IOException {
                if (file.isDirectory()
                        || (file.isFile() && file.getName().endsWith(".jar"))) {
                        addIntoClasspathList(list, new URL(file.toURI().toString()));
                } else throw new IllegalArgumentException("requiring a directory or jarFile, got " + file);
        }

        private void addIntoClasspathList(List<URL> list, URL url) {
                list.add(url);
        }

        /**
         * specify the output directory
         *
         * @param fileDir file directory
         * @return the compiler itself
         */
        public Compiler shiftRight(String fileDir) {
                return shiftRight(new File(fileDir));
        }

        /**
         * specify the output directory
         *
         * @param file the directory
         * @return the compiler itself
         */
        public Compiler shiftRight(File file) {
                if (!file.isDirectory()) throw new IllegalArgumentException(file + " is not a directory");
                config.result.outputDir = file;
                return this;
        }

        /**
         * compile the given files
         *
         * @param fileNameToCode a map containing fileName to source_code/File/InputStream/Reader
         * @return the retrieved class loader
         * @throws Exception the exception occurred when compiling,
         *                   or lt.lang.Wrapper whose object field is a List of {@link lt.compiler.ErrorManager.CompilingError}
         */
        public ClassLoader compile(Map<String, ?> fileNameToCode) throws Exception {
                // validate and transform compile input
                Map<String, Reader> input = new HashMap<>();

                for (Map.Entry<String, ?> entry : fileNameToCode.entrySet()) {
                        String name = entry.getKey();
                        Object v = entry.getValue();
                        if (v instanceof String) {
                                input.put(name, new StringReader((String) v));
                        } else if (v instanceof File) {
                                if (((File) v).isFile()) {
                                        input.put(name, new FileReader((File) v));
                                } else throw new IllegalArgumentException(v + " is not a file");
                        } else if (v instanceof InputStream) {
                                input.put(name, new InputStreamReader((InputStream) v));
                        } else if (v instanceof Reader) {
                                input.put(name, (Reader) v);
                        } else throw new IllegalArgumentException("the mapped values should be String/File/InputStream/Reader");
                }

                // validate configuration
                if (config.threads.codeGen < 1) throw new IllegalArgumentException("config.threads.codeGen should >=1");
                if (config.threads.parser < 1) throw new IllegalArgumentException("config.threads.parser should >=1");
                if (config.threads.scanner < 1) throw new IllegalArgumentException("config.threads.scanner should >=1");

                if (config.code.indentation < 1) throw new IllegalArgumentException("config.code.indentation should >=1");

                if (config.result.outputDir != null && !config.result.outputDir.isDirectory())
                        throw new IllegalArgumentException("config.result.outputDir should be a directory");

                // load jars
                ClassPathLoader classPathLoader = new ClassPathLoader(baseLoader);
                for (URL url : config.classpath) {
                        classPathLoader.load(url);
                }

                // construct thread pool for scanners and parsers
                ExecutorService scannerPool = Executors.newFixedThreadPool(config.threads.scanner);
                ExecutorService parserPool = Executors.newFixedThreadPool(config.threads.parser);

                ErrorManager errorManager = new ErrorManager(config.fastFail);
                errorManager.out = config.out;

                List<Scan> scans = new ArrayList<>();
                lt.compiler.Scanner.Properties properties = new lt.compiler.Scanner.Properties();
                properties._COLUMN_BASE_ = config.code.columnBase;
                properties._INDENTATION_ = config.code.indentation;
                properties._LINE_BASE_ = config.code.lineBase;
                for (Map.Entry<String, Reader> entry : input.entrySet()) {
                        Scan scan = new Scan(entry.getKey(), entry.getValue(), properties, errorManager);
                        scans.add(scan);
                }

                List<Future<FileRoot>> scanRes = scannerPool.invokeAll(scans);
                Map<String, List<Statement>> parseRes = new ConcurrentHashMap<>();
                List<Future<?>> parseState = new Vector<>();

                Exception[] caughtException = new Exception[]{null};

                for (Future<FileRoot> f : scanRes) {
                        new Thread(() -> {
                                try {
                                        FileRoot root;
                                        try {
                                                root = f.get();
                                        } catch (ExecutionException e) {
                                                if (config.out.err != null)
                                                        e.printStackTrace(config.out.err);
                                                caughtException[0] = (Exception) e.getCause();
                                                return;
                                        }
                                        Future<?> future = parserPool.submit(new Parse(root.fileName, root.root, parseRes, errorManager));
                                        parseState.add(future);
                                } catch (InterruptedException ignore) {
                                }
                        }).start();
                }

                // check whether occurred any exceptions
                if (caughtException[0] != null) {
                        scannerPool.shutdown();
                        parserPool.shutdown();

                        throw caughtException[0];
                }

                // wait until all all Parse objects are added into the list
                while (parseState.size() != input.size()) Thread.sleep(1);

                // wait until all parse process finishes
                for (Future<?> f : parseState) {
                        while (!f.isDone()) Thread.sleep(1);
                }

                // all parsing finished

                scannerPool.shutdown();
                parserPool.shutdown();

                if (!errorManager.errorList.isEmpty()) {
                        throw new Wrapper(errorManager.errorList);
                }

                SemanticProcessor processor = new SemanticProcessor(parseRes, classPathLoader);
                Set<STypeDef> types = processor.parse();

                // code gen
                int size = types.size() / config.threads.codeGen + types.size() % config.threads.codeGen;
                List<Set<STypeDef>> toGenerate = new ArrayList<>();
                for (int i = 0; i < size; ++i) toGenerate.add(new HashSet<>());
                int currentCount = 0;
                int whichList = -1;
                for (STypeDef type : types) {
                        if (currentCount % size == 0) {
                                ++whichList;
                        }
                        toGenerate.get(whichList).add(type);
                }

                Map<String, byte[]> byteCodes = new ConcurrentHashMap<>();

                List<Thread> threads = new ArrayList<>();

                // the following process should not throw any exception

                for (Set<STypeDef> toGen : toGenerate) {
                        Thread t = new Thread(() -> {
                                CodeGenerator codeGenerator = new CodeGenerator(toGen);
                                byteCodes.putAll(codeGenerator.generate());
                        });
                        threads.add(t);
                        t.start();
                }

                for (Thread t : threads) {
                        t.join();
                }

                // codes are generated
                ClassLoader loader = new ClassLoader(classPathLoader) {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                if (byteCodes.containsKey(name)) {
                                        byte[] bytes = byteCodes.get(name);
                                        if (bytes == null) throw new ClassNotFoundException(name);
                                        return defineClass(name, bytes, 0, bytes.length);
                                } else throw new ClassNotFoundException(name);
                        }
                };

                if (config.result.outputDir != null) {
                        for (Map.Entry<String, byte[]> result : byteCodes.entrySet()) {
                                String className = result.getKey();
                                byte[] bytes = result.getValue();

                                File outputDir = config.result.outputDir;

                                String dir = outputDir.getAbsolutePath();
                                File path = new File(dir + File.separator + (className.contains(".") ? className.substring(0, className.lastIndexOf('.')).replace(".", File.separator) : ""));
                                if (!path.exists()) if (!path.mkdirs()) throw new IOException("cannot create directory " + path);

                                File newFile = new File(path + File.separator + (className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className) + ".class");
                                if (!newFile.exists()) if (!newFile.createNewFile()) throw new IOException("cannot create file " + newFile);

                                FileOutputStream fos = new FileOutputStream(newFile);
                                fos.write(bytes);
                                fos.close();
                        }
                }

                // TODO statistics

                return loader;
        }

        private class FileRoot {
                public String fileName;
                public ElementStartNode root;
        }

        private class Scan implements Callable<FileRoot> {
                private final String fileName;
                private final Reader reader;
                private final lt.compiler.Scanner.Properties properties;
                private final ErrorManager err;

                private Scan(String fileName, Reader reader, lt.compiler.Scanner.Properties properties, ErrorManager err) {
                        this.fileName = fileName;
                        this.reader = reader;
                        this.properties = properties;
                        this.err = err;
                }

                @Override
                public FileRoot call() throws Exception {
                        lt.compiler.Scanner scanner = new lt.compiler.Scanner(fileName, reader, properties, err);
                        FileRoot fileRoot = new FileRoot();
                        fileRoot.fileName = fileName;
                        fileRoot.root = scanner.scan();
                        return fileRoot;
                }
        }

        private class Parse implements Runnable {
                private final String fileName;
                private final ElementStartNode root;
                private final Map<String, List<Statement>> resultMap;
                private final ErrorManager err;

                private Parse(String fileName, ElementStartNode root, Map<String, List<Statement>> resultMap, ErrorManager err) {
                        this.fileName = fileName;
                        this.root = root;
                        this.resultMap = resultMap;
                        this.err = err;
                }

                @Override
                public void run() {
                        Parser parser = new Parser(root, err);
                        try {
                                resultMap.put(
                                        fileName,
                                        parser.parse()
                                );
                        } catch (SyntaxException ignore) {
                        }
                }
        }
}
