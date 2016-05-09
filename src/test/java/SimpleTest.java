import lt.compiler.CoreCompiler;
import lt.lang.Utils;

import java.lang.reflect.Method;

/**
 * Created by wkgcass on 16/5/9.
 */
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                CoreCompiler coreCompiler = new CoreCompiler();
                ClassLoader loader = coreCompiler.compile(Utils.filesInDirectory("/Volumes/PROJECTS/test/LessTyping"));
                Class<?> Main = loader.loadClass("lt.demo.test.Main");
                Method main = Main.getMethod("main", String[].class);
                main.invoke(null, new Object[]{null});
        }
}
