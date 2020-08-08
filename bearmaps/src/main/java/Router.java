import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Objects;

/**
 * This class provides a <code>shortestPath</code> method and <code>routeDirections</code> for
 * finding routes between two points on the map.
 */
public class Router {

    /**
     * Radius of the Earth in miles.
     */
    private static final int R = 3963;

    /**
     * Return a <code>List</code> of vertex IDs corresponding to the shortest path from a given
     * starting coordinate and destination coordinate.
     * @param g <code>GraphDB</code> data source.
     * @param stlon The longitude of the starting coordinate.
     * @param stlat The latitude of the starting coordinate.
     * @param destlon The longitude of the destination coordinate.
     * @param destlat The latitude of the destination coordinate.
     * @return The <code>List</code> of vertex IDs corresponding to the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g,
                                          double stlon, double stlat,
                                          double destlon, double destlat) {
        Set<Long> visited = new HashSet<>();
        Long startID = g.getID(stlon, stlat);
        Long destID = g.getID(destlon, destlat);
        PriorityQueue<ANode> Q = new PriorityQueue<>();
        Map<Long, ANode> paths = new HashMap<>();
        LinkedList<Long> result = new LinkedList<>();
        // start is initialized with distance from start = 0
        ANode start = new ANode(startID, null);
        start.dist = 0.0;
        start.priority = 0.0;
        Q.add(start);

        while (!Q.isEmpty()) {
            ANode temp = Q.poll();
            if (visited.contains(temp.id)) {
                continue;
            }
            visited.add(temp.id);
            paths.put(temp.id, temp);
            if (temp.id == destID) {
                break;
            }
            // add neighbors of temp into Q
            for (Long neighbor : g.adjacent(temp.getId())) {
                ANode b  = new ANode(neighbor, temp.getId());

                double dist = temp.dist + distance(g.lon(temp.id), g.lat(temp.id),
                        g.lon(neighbor), g.lat(neighbor));
                b.setDist(dist);

                b.setPriority(g.lon(neighbor), g.lat(neighbor), destlon, destlat);

                Q.add(b);
            }
        }
        ANode tempNode;
        Long tempID = destID;
        if (paths.containsKey(destID)) {
            result.addFirst(destID);
            while (!tempID.equals(startID)) {
                tempNode = paths.get(tempID);
                tempID = tempNode.parentID;
                result.addFirst(tempID);
            }
            return result;
        }
        return Collections.emptyList();
    }

    // Copied from GraphBD
    public static double distance(double stlon, double stlat, double destlon, double destlat) {
        double phi1 = Math.toRadians(stlat);
        double phi2 = Math.toRadians(destlat);
        double dphi = Math.toRadians(destlat - stlat);
        double dlambda = Math.toRadians(destlon - stlon);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // A class to keep track of paths from the start node and assign
    // priority based on the heuristic distance.
    static class ANode implements Comparable<ANode> {

        private Long id;
        private double dist;
        private Long parentID;
        private double priority;

        ANode(Long id, Long parent) {
            this.id = id;
            this.parentID = parent;
            this.priority = Double.POSITIVE_INFINITY;
        }

        public void setDist(double dist) {
            this.dist = dist;
        }

        // Start lon/lat are given by the lon and lat of the Vertex; Dest lat/lon are
        // lat and lon of the destination point.
        public void setPriority(double stlon, double stlat, double destlon, double destlat) {
            priority = dist + distance(stlon, stlat, destlon, destlat);
        }

        // --- GETTERS ---
        public Long getId() {
            return id;
        }

        public double getDist() {
            return dist;
        }

        public Long getParentID() {
            return parentID;
        }

        public double getPriority() {
            return priority;
        }

        // compareTo returns -1 when the priority of this ANode object is less that
        // the other; 1 otherwise (because priority is higher with min value)
        @Override
        public int compareTo(ANode o) {
            if (this.priority > o.priority) {
                return 1;
            } else if (this.priority < o.priority) {
                return -1;
            }
            return 0;
        }
    }

    /**
     * Given a <code>route</code> of vertex IDs, return a <code>List</code> of
     * <code>NavigationDirection</code> objects representing the travel directions in order.
     * @param g <code>GraphDB</code> data source.
     * @param route The shortest-path route of vertex IDs.
     * @return A new <code>List</code> of <code>NavigationDirection</code> objects.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        return Collections.emptyList();
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0, STRAIGHT = 1, SLIGHT_LEFT = 2, SLIGHT_RIGHT = 3,
                RIGHT = 4, LEFT = 5, SHARP_LEFT = 6, SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction represented.*/
        int direction;
        /** The name of this way. */
        String way;
        /** The distance along this way. */
        double distance = 0.0;

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Returns a new <code>NavigationDirection</code> from a string representation.
         * @param dirAsString <code>String</code> instructions for a navigation direction.
         * @return A new <code>NavigationDirection</code> based on the string, or <code>null</code>
         * if unable to parse.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // Not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                        && way.equals(((NavigationDirection) o).way)
                        && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
