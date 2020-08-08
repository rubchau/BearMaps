import java.util.List;
import java.util.ArrayList;

/**
 * This class provides a main method for experimenting with GraphDB construction.
 * You could also use MapServer, but this class lets you play around with
 * GraphDB in isolation from all the rest of the parts of this assignment.
 */
public class GraphDBLauncher {
    private static final String OSM_DB_PATH = "data/berkeley-2018.osm.xml";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        GraphDB g = new GraphDB(OSM_DB_PATH);
        List<Long> vertices = new ArrayList<>();
        for (long v : g.vertices()) {
            vertices.add(v);
        }

//        System.out.println("There are " + vertices.size() + " vertices in the graph.");
//
//        System.out.println("The first 10 vertices are:");
//        for (int i = 0; i < 10; i += 1) {
//            if (i < vertices.size()) {
//                System.out.println(vertices.get(i));
//            }
//        }
//      lon=-122.274291, lat=37.852623
        long v = g.closest(-122.274291, 37.852623);
//        System.out.print("The vertex number closest to -122.274291,37.852623 is "
//        + v + ", which");
//        System.out.println(" has longitude, latitude of: " + g.lon(v)
//                + ", " + g.lat(v) + " and expected ID is " +
//                "2422464346, your ID is " + v);
//
//        System.out.println("To get started, uncomment print statements in GraphBuildingHandler.");
        long endTime = System.currentTimeMillis();
        long totalRunTime = endTime - startTime;
        System.out.println("Your program took " + totalRunTime + " ms to complete.");
    }
}
