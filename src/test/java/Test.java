/**
 * Created by wkgcass on 16/5/4.
 */
public class Test {
        public static void main(String[] args) {
                try {
                        Class<?> c = Test.class.getClassLoader().loadClass("java.util.List");
                        System.out.println(c);
                } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                }
        }
}
