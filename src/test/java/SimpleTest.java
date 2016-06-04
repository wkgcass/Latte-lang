import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wkgcass on 16/6/4.
 */
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                ErrorManager err = new ErrorManager(true);

                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(SimpleTest.class.getResourceAsStream("/lang-demo/statements.lts")));
                sb.append("class hehe\n");
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
        }
}
