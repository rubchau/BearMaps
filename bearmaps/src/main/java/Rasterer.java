
/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /** The max image depth level. */
    public static final int MAX_DEPTH = 7;

    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     *     <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     *     possible, while still covering less than or equal to the amount of longitudinal distance
     *     per pixel in the query box for the user viewport size.</li>
     *     <li>Contains all tiles that intersect the query bounding box that fulfill the above
     *     condition.</li>
     *     <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */
    public RasterResultParams getMapRaster(RasterRequestParams params) {

        //check if the query box makes sense by comparing upper corner with lower corner
        if (params.ullon >= params.lrlon || params.lrlat >= params.ullat) {
            System.out.println("Query box coordinates invalid.");
            return RasterResultParams.queryFailed();
        }

        //check upper corner in relation to lower root of the whole region and
        //lower corner in relation to the upper root of the whole region
        if (params.ullat < MapServer.ROOT_LRLAT || params.ullon > MapServer.ROOT_LRLON
                || params.lrlat > MapServer.ROOT_ULLAT || params.lrlon < MapServer.ROOT_ULLON) {
            return RasterResultParams.queryFailed();
        }

        int depth = getDepth(params);
        double latPerSide = getLatPerSide(depth);
        double lonPerSide = getLonPerSide(depth);

        //gridCoords 0 = startW, 1 = startH, 2 = stopW, 3 = stopH
        int[] gridCoords = getGridHelper(params.ullat, params.ullon,
                                params.lrlat, params.lrlon, depth);
        int h = gridCoords[3] - gridCoords[1];
        int w = gridCoords[2] - gridCoords[0];
        String[][] grid = getGrid(w, h, gridCoords[1], gridCoords[0], depth);
        double rasterUllat;
        double rasterUllon;
        double rasterLrlat;
        double rasterLrlon;

        if (params.ullat > MapServer.ROOT_ULLAT) {
            rasterUllat = MapServer.ROOT_ULLAT;
        } else {
            rasterUllat = MapServer.ROOT_ULLAT - (latPerSide * gridCoords[0]);
        }

        if (params.ullon < MapServer.ROOT_ULLON) {
            rasterUllon = MapServer.ROOT_ULLON;
        } else {
            rasterUllon = MapServer.ROOT_ULLON + (lonPerSide * gridCoords[1]);
        }

        if (params.lrlat < MapServer.ROOT_LRLAT) {
            rasterLrlat = MapServer.ROOT_LRLAT;
        } else {
            rasterLrlat = MapServer.ROOT_ULLAT - (latPerSide * gridCoords[2]);
        }

        if (params.lrlon > MapServer.ROOT_LRLON) {
            rasterLrlon = MapServer.ROOT_LRLON;
        } else {
            rasterLrlon = MapServer.ROOT_ULLON + (lonPerSide * gridCoords[3]);
        }

        RasterResultParams.Builder result = new RasterResultParams.Builder();
        result.setRasterUlLon(rasterUllon);
        result.setRasterUlLat(rasterUllat);
        result.setRasterLrLat(rasterLrlat);
        result.setRasterLrLon(rasterLrlon);
        result.setDepth(depth);
        result.setRenderGrid(grid);
        result.setQuerySuccess(true);

        return result.create();
    }

    /**
     * Calculates the lonDPP of an image or query box
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }

    private static double getSLon(double latitude) {
        double sLon = Math.cos(latitude) * 69.172 * 5280;
        return sLon;
    }

    /**
     * Calculate the required depth of tiles to be rendered
     * based on the longitudinal distance per pixel specified
     * by the request.
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return depth
     */
    private int getDepth(RasterRequestParams params) {
        double lonDPPQuery = lonDPP(params.lrlon, params.ullon, params.w);

        for (int i = 0; i < MAX_DEPTH; i++) {
            double currLonDPP = MapServer.ROOT_LONDPP / getDepthHelper(i + 1);
            if (currLonDPP <= lonDPPQuery) {
                return i;
            }
        }
        return MAX_DEPTH;
    }

    private int getDepthHelper(int d) {
        int result = 1;
        for (int i = 0; i < d - 1; i++) {
            result = result * 2;
        }
        return result;
    }

    //get number of boxes that the whole region is divided into based on depth
    private int getBoxesPerSide(int depth) {
        return (int) Math.pow(2, depth);
    }

    //helper to get latitude degrees per side of a box based on depth
    private double getLatPerSide(int depth) {
        return (MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT) / (double) getBoxesPerSide(depth);
    }

    //helper to get longitude degrees per side of box based on depth
    private double getLonPerSide(int depth) {
        return (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON) / (double) getBoxesPerSide(depth);
    }


    private int[] getGridHelper(double ullat, double ullon,
                                double lrlat, double lrlon,
                                int depth) {
        // int array with:
        // 0 = startW, 1 = startH, 2 = stopW, 3 = stopH
        if (ullat > MapServer.ROOT_ULLAT) {
            ullat = MapServer.ROOT_ULLAT;
        }
        if (ullon < MapServer.ROOT_ULLON) {
            ullon = MapServer.ROOT_ULLON;
        }
        if (lrlon > MapServer.ROOT_LRLON) {
            lrlon = MapServer.ROOT_LRLON;
        }
        if (lrlat < MapServer.ROOT_LRLAT) {
            lrlat = MapServer.ROOT_LRLAT;
        }

        double latPerSide = getLatPerSide(depth);
        double lonPerSide = getLonPerSide(depth);
        int maxBox = getBoxesPerSide(depth + 1);
        int[] args = new int[4];
        int i;
        int j;
        // Offset rasterUllat in order to pass edge case; depth  == 7
        for (i = 0; i < maxBox; i++) {
            if (MapServer.ROOT_ULLAT - i * latPerSide < ullat) {
                args[0] = i - 1;
                break;
            }
        }
        for (j = 0; j < maxBox; j++) {
            if (j * lonPerSide + MapServer.ROOT_ULLON > ullon) {
                args[1] = j - 1;
                break;
            }
        }
        // Offset rasterLrlat in order to pass edge case; depth  == 7
        for (; i < maxBox; i++) {
            if (MapServer.ROOT_ULLAT - i * latPerSide < lrlat) {
                args[2] = i;
                break;
            }
        }
        for (; j < maxBox; j++) {
            if (j * lonPerSide + MapServer.ROOT_ULLON > lrlon) {
                args[3] = j;
                break;
            }
        }
        return args;
    }

    //method to return filled string matrix based on dimensions passed and starts/stops
    private String[][] getGrid(int h, int w, int startH, int startW, int depth) {
        String[][] grid = new String[h][w];
        int tempW = startH;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                grid[i][j] = "d" + Integer.toString(depth) + "_x" + Integer.toString(tempW)
                        + "_y" + Integer.toString(startW) + ".png";
                tempW += 1;
            }
            tempW = startH;
            startW += 1;
        }
        return grid;
    }


}
