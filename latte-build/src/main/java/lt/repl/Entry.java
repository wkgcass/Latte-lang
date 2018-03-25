package lt.repl;

import lt.compiler.SyntaxException;
import lt.repl.scripting.EvalEntry;
import lt.util.Utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Entry {
        public static void main(String[] args) throws Exception {
                if (args == null || args.length > 0) {
                        runCommands(args);
                } else {
                        StringReader reader;
                        try {
                                Class.forName("jline.console.ConsoleReader"); // check exists
                                reader = (StringReader) Class.forName("lt.repl.JLineStringReader")
                                        .getConstructor().newInstance();
                        } catch (ClassNotFoundException ignore) {
                                reader = new SimpleStringReader();
                        }

                        REPL repl = new REPL(reader, new IO(System.in, System.out, System.err),
                                new CtrlCHandler.ExitCallback() {
                                        @Override
                                        public void exit() {
                                                System.exit(0);
                                        }
                                });
                        repl.handleCtrlC();
                        repl.start();
                }
        }

        /**
         * run the program with arguments
         *
         * @param args arguments
         */
        private static void runCommands(String[] args) throws Exception {
                String command = args[0];
                if (command.equals("help") || command.equals("-help") || command.equals("--help") || command.equals("-h") || command.equals("/h") || command.equals("/help")) {// help
                        System.out.println("" +
                                "usage: -s <script-location> [arguments [,...]]\n" +
                                "       -c <source-directory> [-r] [-o <output-directory>] [-cp <classpath[:...]>]\n" +
                                "       ClassName [-cp <classpath[:...]>]\n" +
                                "       -gb <project-directory>\n" +
                                "       -repl\n" +
                                "       -v | -version\n" +
                                "       -e <statements>\n" +
                                "\n" +
                                "-s       Specify the script location and run the script\n" +
                                "-c       Specify the source file directory and compile *.lt files\n" +
                                "-r       [option] Add sub directory files to compiling list.\n" +
                                "-o       [option] Specify the output directory. (the source-directory/target/classes/ as default)\n" +
                                "-cp      [option] The classpath. use ':' to separate the class-paths\n" +
                                "-repl    Start the repl (or run the program with 0 arguments)\n" +
                                "-gb      Generate build.lts and run.lts in the given directory\n" +
                                "-e       Evaluate the given statement and print the result\n" +
                                "-version Show current version\n");

                } else if (command.equals("-v") || command.equals("-version")) {
                        System.out.println("Latte-lang " + VersionRetriever.version());

                } else if (command.equals("-s")) {// run scripts
                        // -s ?
                        if (args.length < 2) {
                                System.err.println("invalid command -s. the script file location should be specified\n" +
                                        "see --help");
                                return;
                        }
                        String path = args[1];
                        File f = new File(path);
                        ScriptCompiler s = new ScriptCompiler(ClassLoader.getSystemClassLoader());
                        try {
                                ScriptCompiler.Script script = s.compile(f);
                                String[] scriptArgs = new String[args.length - 2];
                                System.arraycopy(args, 2, scriptArgs, 0, args.length - 2);
                                script.run(scriptArgs);
                        } catch (Throwable e) {
                                if (e instanceof SyntaxException) {
                                        System.err.println("[ERROR] " + e.getMessage());
                                } else {
                                        e.printStackTrace();
                                }
                        }

                } else if (command.equals("-c")) {// compile
                        // -c ?
                        if (args.length < 2) {
                                System.err.println("invalid command -c. the source directory should be specified\n" +
                                        "see --help");
                                return;
                        }
                        String sourceDir = args[1].trim();
                        if (sourceDir.endsWith(File.separator)) {
                                sourceDir = sourceDir.substring(0, sourceDir.length() - File.separator.length());
                        }

                        boolean recursive = false;
                        String outputDir = sourceDir + File.separator + "target" + File.separator + "classes";
                        List<URL> classPaths = new ArrayList<URL>();

                        for (int i = 2; i < args.length; ++i) {
                                String cmd = args[i];
                                if (cmd.equals("-r")) {
                                        recursive = true;

                                } else if (cmd.equals("-o")) {
                                        if (args.length - 1 == i) {
                                                System.err.println("invalid option -o. the output directory should be specified");
                                                System.err.println("see --help");
                                                return;
                                        }
                                        outputDir = args[++i];

                                } else if (cmd.equals("-cp")) {
                                        if (args.length - 1 == i) {
                                                System.err.println("invalid option -cp. the class-path should be specified");
                                                System.err.println("see --help");
                                                return;
                                        }
                                        String[] class_paths = args[++i].split(":");
                                        for (String class_path : class_paths) {
                                                try {
                                                        classPaths.add(new URL(new File(class_path).toURI().toString()));
                                                } catch (MalformedURLException e) {
                                                        System.err.println("[ERROR] " + e.getMessage());
                                                        return;
                                                }
                                        }


                                } else {
                                        System.err.println("unknown option " + cmd);
                                        System.err.println("see --help");
                                        return;
                                }
                        }

                        Compiler compiler = new Compiler();
                        File outputDirFile = new File(outputDir);
                        if (!outputDirFile.exists()) //noinspection ResultOfMethodCallIgnored
                                outputDirFile.mkdirs();
                        compiler.config.result.outputDir = outputDirFile;
                        compiler.config.classpath = classPaths;

                        try {
                                compiler.compile(Utils.filesInDirectory(sourceDir, ".*\\.(lt|latte)", recursive));
                        } catch (Exception e) {
                                if (e instanceof SyntaxException) {
                                        System.err.println("[ERROR] " + e.getMessage());
                                } else {
                                        e.printStackTrace();
                                }
                        }

                } else if (command.equals("-gb")) {
                        final List<String> theFilesToBeGenerated = Arrays.asList("build.lts", "run.lts");

                        if (args.length != 2) {
                                System.err.println("invalid command -gb.");
                                System.err.println("see --help");
                                return;
                        }
                        String projectDir = args[1].trim();
                        if (projectDir.endsWith(File.separator)) {
                                projectDir = projectDir.substring(0, projectDir.length() - File.separator.length());
                        }

                        String core = String.valueOf(Runtime.getRuntime().availableProcessors());
                        String separator = File.separator;

                        for (String theFile : theFilesToBeGenerated) {
                                String filePath = projectDir + File.separator + theFile;
                                File file = new File(filePath);
                                if (file.exists()) {
                                        System.out.println("[INFO] " + filePath + " exists");
                                } else {
                                        try {
                                                //noinspection ResultOfMethodCallIgnored
                                                file.createNewFile();
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }
                                }

                                try {
                                        FileWriter fw = new FileWriter(file);
                                        BufferedReader br = new BufferedReader(new InputStreamReader(REPL.class.getClassLoader().getResourceAsStream(theFile + ".template")));

                                        String ss;
                                        while ((ss = br.readLine()) != null) {
                                                ss = ss.replace("${core}", core)
                                                        .replace("${dir}", projectDir.replace("\\", "\\\\"))
                                                        .replace("${separator}", separator) + "\n";
                                                fw.write(ss.toCharArray());
                                        }
                                        fw.flush();
                                        fw.close();
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }

                } else if (command.equals("-repl")) {// repl
                        main(new String[0]);
                } else if (command.equals("-e")) {// eval
                        if (args.length == 1 || args[1] == null || args[1].trim().isEmpty()) {
                                System.err.println("missing statements to eval");
                                return;
                        }
                        String statements = args[1].trim();
                        Evaluator e = new Evaluator(new ClassPathLoader(Thread.currentThread().getContextClassLoader()));
                        EvalEntry entry = e.eval(statements);
                        System.out.println(entry.result);

                } else {// run
                        List<URL> urls = new ArrayList<URL>();
                        try {
                                String url = new File("").toURI().toString();
                                urls.add(new URL(url));
                        } catch (MalformedURLException e) {
                                System.err.println("[ERROR] " + e.getMessage());
                                return;
                        }
                        String[] runArgs = new String[0];
                        for (int i = 1; i < args.length; ++i) {
                                String cmd = args[i];
                                if (cmd.equals("-cp")) {
                                        if (i == args.length - 1) {
                                                System.err.println("invalid option -cp. the class-path should be specified");
                                                System.err.println("see --help");
                                                return;
                                        }
                                        String cps = args[++i];
                                        for (String cp : cps.split(":")) {
                                                try {
                                                        urls.add(new URL(new File(cp).toURI().toString()));
                                                } catch (MalformedURLException e) {
                                                        System.err.println("[ERROR] " + e.getMessage());
                                                        return;
                                                }
                                        }

                                } else if (cmd.equals("-args")) {
                                        runArgs = new String[args.length - 1 - i];
                                        System.arraycopy(args, i + 1, runArgs, 0, runArgs.length);
                                        break;
                                } else {
                                        System.err.println("unknown option " + cmd);
                                        System.err.println("see --help");
                                        return;
                                }
                        }

                        try {
                                // run the class
                                Run run = new Run(urls, command);
                                run.exec(runArgs);
                        } catch (Throwable t) {
                                t.printStackTrace();
                        }
                }
        }
}
