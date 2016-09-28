import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by wkgcass on 16/6/4.
 */
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                /*
                ErrorManager err = new ErrorManager(true);

                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(SimpleTest.class.getResourceAsStream("/lang-demo/list-map.lts")));
                sb.append("import lt::util::_" +
                        "\nclass hehe\n");
                sb.append("    method(args:[]String)\n");
                String base = "        ";
                String s;
                while ((s = br.readLine()) != null) {
                        sb.append(base).append(s).append("\n");
                }

                lt.compiler.Scanner lexicalProcessor = new lt.compiler.Scanner("test.lt", new StringReader(sb.toString()), new Scanner.Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types);
                Map<String, byte[]> list = codeGenerator.generate();
                byte[] b = list.get("hehe");
                FileOutputStream fos = new FileOutputStream(new File("/Volumes/PROJECTS/openSource/LessTyping/hehe.class"));
                fos.write(b);
                fos.flush();
                fos.close();
                */

                /*
                URL url = new URL("jar:file:///Volumes/PROJECTS/openSource/LessTyping/latte-compiler/target/latte-compiler-0.0.3-ALPHA.jar!/build.lts.template");
                URLConnection u = url.openConnection();
                System.out.println(u);
                InputStream is = u.getInputStream();
                System.out.println(is);
                BufferedReader reader=new BufferedReader(new InputStreamReader(is));
                String s;
                while(null!=(s=reader.readLine())){
                        System.out.println(s);
                }
                */

                System.out.println("\033[0;32;40mabc");
        }
}
