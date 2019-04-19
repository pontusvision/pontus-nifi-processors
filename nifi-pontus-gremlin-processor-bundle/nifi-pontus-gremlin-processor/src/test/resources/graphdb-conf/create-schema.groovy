import com.pontusvision.gdpr.App
import org.apache.tinkerpop.gremlin.server.Settings

try {
  App.settings = Settings.read("target/test-classes/graphdb-conf/gremlin-mem.yml");

//  App.graph = graph;
//
//
//  App.g = g;
    System.out.println('\n\n\n\nABOUT TO LOAD target/test-classes/graphdb-conf/conf2/gdpr-schema.json\n\n\n\n\n')
    String retVal = loadSchema(graph,'target/test-classes/graphdb-conf/conf2/gdpr-schema.json')
    
    System.out.println("results after loading target/test-classes/graphdb-conf/conf2/gdpr-schema.json: ${retVal}\n\n\n\n\n")

} catch (e) {
    e.printStackTrace()
}
