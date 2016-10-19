package org.lattelang.compiler.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * generate shell command when doing mvn package
 */
public class ShellCommandGenerator {
        public static void main(String[] args) throws IOException {
                String version = args[0];
                String output = args[1];
                String jarDir = args[2];

                String fullName = "latte-build-" + version + "-jar-with-dependencies.jar";

                String bat = "" +
                        "@echo off\n" +
                        "\n" +
                        "setlocal EnableDelayedExpansion\n" +
                        "\n" +
                        ":getArg\n" +
                        "if \"%1%\"==\"\" goto end\n" +
                        "set input=!input! %1%\n" +
                        "shift\n" +
                        "goto getArg\n" +
                        ":end\n" +
                        "\n" +
                        "java -jar \"" + jarDir + "\\" + fullName + "\" !input!\n" +
                        "\n" +
                        "endlocal EnableDelayedExpansion";

                File latte_bat = new File(output + File.separator + "latte.bat");
                if (!latte_bat.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        latte_bat.createNewFile();
                }
                FileOutputStream latte_bat_fos = new FileOutputStream(latte_bat);
                latte_bat_fos.write(bat.getBytes());
                latte_bat_fos.flush();
                latte_bat_fos.close();

                File latte_sh = new File(output + File.separator + "latte");
                if (!latte_sh.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        latte_sh.createNewFile();
                }
                FileOutputStream latte_sh_fos = new FileOutputStream(latte_sh);
                InputStream latte_sh_is = ShellCommandGenerator.class.getClassLoader().getResourceAsStream("latte.sh.template");
                latte_sh_fos.write("#!/bin/bash\n".getBytes());
                latte_sh_fos.write(("LATTE_JAR=\"" + jarDir + File.separator + fullName + "\"\n").getBytes());
                byte[] buf = new byte[1024];
                int c;
                while (-1 != (c = latte_sh_is.read(buf))) {
                        latte_sh_fos.write(buf, 0, c);
                }
                latte_sh_fos.flush();
                latte_sh_fos.close();
        }
}
