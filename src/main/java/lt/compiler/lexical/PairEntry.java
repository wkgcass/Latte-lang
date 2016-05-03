package lt.compiler.lexical;

/**
 * pair key and start node
 */
public class PairEntry {
        public final String key;
        public final ElementStartNode startNode;

        public PairEntry(String key, ElementStartNode startNode) {
                this.key = key;
                this.startNode = startNode;
        }
}
