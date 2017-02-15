package lt.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * retrieves the version
 */
class VersionRetriever {
        static String version() throws IOException {
                InputStream is = VersionRetriever.class.getClassLoader().getResourceAsStream("version");
                InputStreamReader reader = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(reader);
                String ver = br.readLine();
                if (ver == null) return "UNKNOWN";
                return ver.trim();
        }
}
