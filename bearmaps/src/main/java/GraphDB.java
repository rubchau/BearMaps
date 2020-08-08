import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {
    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing.
     *
     * @param dbPath Path to the XML file to be parsed.
     */

    // VERTS contains all of the vertices in the map,
    // keeping a mapping from nodeID to Vertex
    Map<Long, Vertex> verts = new HashMap<>();
    Map<String, Edge> edges = new HashMap<>();
    TwoDTree tree = new TwoDTree();

    public GraphDB(String dbPath) {
        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        treeBuilder();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    public void treeBuilder() {
        for (Long key : verts.keySet()) {
            Vertex v = verts.get(key);
            tree.insert(v);
        }
    }

    public Long getID(double lon, double lat) {
        return closest(lon, lat);
    }

    public void addVertex(long nodeID, Double lat, Double lon) {
        Vertex v = new Vertex(nodeID, lat, lon);
        verts.put(nodeID, v);
    }

    public void addEdge(long source, long dest) {
        if (!verts.containsKey(source) || !verts.containsKey(dest)) {
            System.out.println("In addEdge, invalid vertex passed.");
            return;
        }
        if (source == dest) {
            System.out.println("Source and Dest the same in addEdge");
            return;
        }
        Edge e1 = new Edge(source, dest, distance(source, dest));
        Edge e2 = new Edge(dest, source, distance(dest, source));
        verts.get(source).adjVertices.put(dest, e1);
        verts.get(dest).adjVertices.put(source, e2);
        String destToString = Long.toString(dest);
        String srcToString = Long.toString(source);
        String key1 = srcToString + destToString;
        String key2 = destToString + srcToString;
        edges.put(key1, e1);
        edges.put(key2, e2);
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */

    private void clean() {
        Set<Long> keys = verts.keySet();
        Set<Long> disconnected = new HashSet<>();
        for (Long k: keys) {
            if (verts.get(k).adjVertices.isEmpty()) {
                disconnected.add(k);
            }
        }
        for (Long k: disconnected) {
            verts.remove(k);
        }
    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lon(long v) {
        if (!verts.containsKey(v)) {
            return 0.0;
        }
        return verts.get(v).getLon();
    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        if (!verts.containsKey(v)) {
            return 0.0;
        }
        return verts.get(v).getLat();
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     *
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {
        return verts.keySet();
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     *
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        if (!verts.containsKey(v)) {
            return Collections.emptySet();
        }
        return  verts.get(v).getAdjVertices().keySet();
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     *
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */
    public long closest(double lon, double lat) {
        return tree.nearest(lon, lat);
    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Radius of the Earth in miles.
     */
    private static final int R = 3963;
    /**
     * Latitude centered on Berkeley.
     */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /**
     * Longitude centered on Berkeley.
     */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     *
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;

    class Vertex {

        String name;
        private long nodeID;
        private Double lat;
        private Double lon;
        private Map<String, String> attr;
        private Map<Long, Edge> adjVertices;

        Vertex(long nodeID, Double lat, Double lon) {
            this.name = null;
            this.nodeID = nodeID;
            this.lat = lat;
            this.lon = lon;
            this.attr = new HashMap<>();
            this.adjVertices = new HashMap<>();
        }

        // ---- GETTERS ----
        public long getNodeID() {
            return nodeID;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLon() {
            return lon;
        }

        public Map<Long, Edge> getAdjVertices() {
            return adjVertices;
        }

    }

    //Edge object to store data on edges
    public class Edge implements Comparable<Edge> {
        long source;
        long dest;
        double weight;

        public Edge(long s, long d, double w) {
            this.source = s;
            this.dest = d;
            this.weight = w;
        }

        public long getSource() {
            return source;
        }

        public long getDest() {
            return dest;
        }

        public double getWeight() {
            return weight;
        }

        public int compareTo(Edge other) {
            double cmp =  weight - other.weight;
            if (cmp == 0) {
                return 0;
            } else if (cmp < 0) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public int hashCode() {

            return Objects.hash(source, dest, weight);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Edge e = (Edge) o;
            return (source == e.source && dest == e.dest && weight == e.weight)
                    || (source == e.dest && dest == e.source && weight == e.weight);
        }
    }

    public static class TwoDTree {

        private TwoDNode root;
        private int size;

        public TwoDTree() {
            this.root = null;
            this.size = 0;
        }

        private class TwoDNode implements Comparable<TwoDNode> {

            private Long id;
            private double x;
            private double y;
            private final boolean horizontal;
            private TwoDNode left;
            private TwoDNode right;

            private TwoDNode(Vertex item, boolean isHorizontal,
                             TwoDNode right, TwoDNode left) {

                this.id = item.nodeID;
                this.x = projectToX(item.lon, item.lat);
                this.y = projectToY(item.lon, item.lat);
                this.horizontal = isHorizontal;
                this.left = left;
                this.right = right;
            }

            @Override
            public int compareTo(TwoDNode o) {
                if (this.x - o.x < 0) {
                    return -1;
                } else if (this.x - o.x > 0) {
                    return 1;
                }
                return 0;
            }

            public boolean equals(TwoDNode o) {
                if (this.x == o.x
                        && this.y == o.y) {
                    return true;
                }
                return false;
            }

        }

        private static class YCompare implements Comparator<TwoDNode> {

            @Override
            public int compare(TwoDNode o1, TwoDNode o2) {
                if (o1.y - o2.y < 0) {
                    return -1;
                } else if (o1.y - o2.y > 0) {
                    return 1;
                }
                return 0;
            }

            public static Comparator<TwoDNode> yComp() {
                return new YCompare();
            }
        }

        private static double euclidean(double x1, double x2, double y1, double y2) {
            return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        }

        private TwoDNode insert(final TwoDNode node, final Vertex item,
                                final boolean isHorizontal) {
            TwoDNode other = new TwoDNode(item, true, null, null);
            if (node == null) {
                size++;
                return new TwoDNode(item, isHorizontal, null, null);
            }
            if (node.id.equals(other.id)) {
                return node;
            }
            Comparator<TwoDNode> yComp = new YCompare();
            if (node.horizontal && other.compareTo(node) < 0
                    || !isHorizontal && yComp.compare(other, node) < 0) {
                node.left = insert(node.left, item, !node.horizontal);
            } else {
                node.right = insert(node.right, item, !node.horizontal);
            }
            return node;
        }

        public void insert(final Vertex v) {
            root = insert(root, v, true);
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public int size() {
            return size;
        }

        public Long nearest(double lon, double lat) {
            double x = projectToX(lon, lat);
            double y = projectToY(lon, lat);
            if (root.x == x && root.y == y) {
                return root.id;
            }
            double current = euclidean(root.x, x, root.y, y);
            TwoDNode result = nearestHelper(root, x, y, current, root);

            return result.id;
        }

        private TwoDNode nearestHelper(TwoDNode ptr, double x,
                                       double y, double closestDist, TwoDNode closest) {
            if (ptr == null) {
                return closest;
            }
            if (ptr.x == x && ptr.y == y) {
                return ptr;
            }
            double distHere = euclidean(ptr.x, x, ptr.y, y);
            if (distHere < closestDist) {
                closestDist = distHere;
                closest = ptr;
            }
            double planeToPoint;
            if (ptr.horizontal) {
                planeToPoint = euclidean(ptr.x, x, y, y);
            } else {
                planeToPoint = euclidean(x, x, ptr.y, y);
            }

            if (ptr.left == null && ptr.right == null) {
                return closest;
            }

            TwoDNode ptr1 = null;
            TwoDNode ptr2 = null;
            boolean left = false;

            if (ptr.horizontal && x < ptr.x || !ptr.horizontal && y < ptr.y) {
                left = true;
                ptr1 = nearestHelper(ptr.left, x, y, closestDist, closest);
            } else {
                ptr1 = nearestHelper(ptr.right, x, y, closestDist, closest);
            }
            double tempDist = euclidean(ptr1.x, x, ptr1.y, y);
            if (tempDist < closestDist) {
                closestDist = tempDist;
                closest = ptr1;
            }
            if (left) {
                if (ptr.right == null) {
                    return closest;
                }
            } else {
                if (ptr.left == null) {
                    return closest;
                }
            }

            double ptr1Circle = euclidean(ptr1.x, x, ptr1.y, y);
            if (ptr1Circle > planeToPoint) {
                if (left) {
                    ptr2 = nearestHelper(ptr.right, x, y, closestDist, closest);
                } else {
                    ptr2 = nearestHelper(ptr.left, x, y, closestDist, closest);
                }
            } else {
                return closest;
            }
            if (ptr2.equals(ptr)) {
                return closest;
            }
            tempDist = euclidean(ptr2.x, x, ptr2.y, y);
            if (tempDist < closestDist) {
                closestDist = tempDist;
                closest = ptr2;
            }
            return closest;
        }

    }

}


