import com.ugos.jiprolog.engine.JIPEngine;

import java.awt.*;
import java.io.*;
import java.util.*;

public class GPS {

    public static void main(String[] args) {

        Long programStart = System.currentTimeMillis();
        Long startTime = null, lineTime = null, trafficTime = null,
                nodeTime = null, taxiTime = null, clientTime = null;
        Long consultStart = null, consultEnd = null;

        PrintWriter prolog = null;
        JIPEngine jip = new JIPEngine();
        AStar astar;

        /**
         * Read only if file does not exist.
         */
        try {

            if (args[6].equals("Yes")) {

                prolog = new PrintWriter("info.pl");

                PreProcess process = new PreProcess(prolog, jip);

                startTime = System.currentTimeMillis();
                process.readLines(args[0]);
                lineTime = System.currentTimeMillis();
                process.readTraffic(args[1]);
                trafficTime = System.currentTimeMillis();
                process.readNodes(args[2]);
                nodeTime = System.currentTimeMillis();
                process.readTaxis(args[3]);
                taxiTime = System.currentTimeMillis();
                process.readClient(args[4]);
                clientTime = System.currentTimeMillis();

                prolog.close();

            }

        } catch (FileNotFoundException e) {
            System.out.println("IOException in making printwriter");
            System.out.println("Exiting...");
            System.exit(-1);
        }

        /**
         * A*
         */
        try {
            consultStart = System.currentTimeMillis();
            jip.consultFile("info.pl");
            jip.consultFile("smart.pl");
            consultEnd = System.currentTimeMillis();
        } catch (IOException e) {
            System.out.println("IOException in JIPEngine");
            System.out.println("Exiting...");
            System.exit(-1);
        }
        ArrayList<Coordinates> route;
        TreeMap< Integer, ArrayList<Coordinates> > paths;
        astar = new AStar(jip, args[5]);
        Long astarStart = System.currentTimeMillis();
        route = astar.solveClient();
        paths = astar.solveTaxis();
        Long astarEnd = System.currentTimeMillis();
        kml(route, paths, args[5]);
        astar.printResults();

        if (startTime != null) {
            System.out.println("* * * R E A D  S T A T S * * *");
            System.out.println("Lines\t: " + (lineTime - startTime) / 1000.0 + "s");
            System.out.println("Traffic\t: " + (trafficTime - lineTime) / 1000.0 + "s");
            System.out.println("Nodes\t: " + (nodeTime - trafficTime) / 1000.0 + "s");
            System.out.println("Taxis\t: " + (taxiTime - nodeTime) / 1000.0 + "s");
            System.out.println("Client\t: " + (clientTime - taxiTime) / 1000.0 + "s");
            System.out.println();
        }

        System.out.println("* * * J I P  S T A T S * * *");
        System.out.println("Consulting\t: " + (consultEnd - consultStart) / 1000.0 + "s");
        System.out.println();

        System.out.println("* * * A  S T A R  S T A T S * * *");
        System.out.println("Total time\t: " + (astarEnd - astarStart) / 1000.0 + "s");
        System.out.println();

        Long programEnd = System.currentTimeMillis();

        System.out.print("T O T A L  R U N T I M E : ");
        System.out.println((programEnd - programStart) / 1000.0 + "s");

    }

    private static void kml(ArrayList<Coordinates> route, TreeMap< Integer, ArrayList<Coordinates> > results, String arg) {

        Random rand = new Random();
        Color green = Color.GREEN.darker(), color;
        Color routeColor = Color.BLACK;
        PrintWriter writer = null;
        int capacity = Integer.parseInt(arg);
        String file = "kml" + capacity + ".kml";

        try {

            writer = new PrintWriter(file);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
            writer.println("<Document>");
            writer.println("<name>Taxi Routes " + capacity + "</name>");
            writer.println("<Style id=\"white\">");
            writer.println("<LineStyle>");
            writer.println("<color>" + Integer.toHexString(routeColor.getRGB()) + "</color>");
            writer.println("<width>4</width>");
            writer.println("</LineStyle>");
            writer.println("</Style>");
            writer.println("<Style id=\"green\">");
            writer.println("<LineStyle>");
            writer.println("<color>" + Integer.toHexString(green.getRGB()) + "</color>");
            writer.println("<width>4</width>");
            writer.println("</LineStyle>");
            writer.println("</Style>");
            int i = 1;
            /**
             *  Make random colors for the
             *  rest of the taxi routes.
             *  Don't make it green.
             */
            while (i < results.size()) {

                int r = rand.nextInt(255);
                int g = rand.nextInt(127);
                int b = rand.nextInt(255);
                color = new Color(r, g, b);

                if (color.getRGB() == green.getRGB())
                    continue;

                writer.println("<Style id=\"taxi" + i + "\">");
                writer.println("<LineStyle>");
                writer.println("<color>" + Integer.toHexString(color.getRGB()) + "</color>");
                writer.println("<width>4</width>");
                writer.println("</LineStyle>");
                writer.println("</Style>");

                i++;

            }

            /**
             * Destination mark.
             */
            writer.println("<Placemark>");
            writer.println("<name>Destination</name>");
            writer.println("<Point>");
            writer.println("<coordinates>");
            writer.println(route.get(0));
            writer.println("</coordinates>");
            writer.println("</Point>");
            writer.println("</Placemark>");
            /**
             * Route.
             */
            writer.println("<Placemark>");
            writer.println("<name>Route</name>");
            writer.println("<styleUrl>#white</styleUrl>");
            writer.println("<LineString>");
            writer.println("<altitudeMode>relative</altitudeMode>");
            writer.println("<coordinates>");
            for (Coordinates co : route)
                writer.println(co);
            writer.println("</coordinates>");
            writer.println("</LineString>");
            writer.println("</Placemark>");

            Integer TaxiID = results.firstEntry().getKey();
            ArrayList<Coordinates> coordinates = results.firstEntry().getValue();
            results.pollFirstEntry();
            /**
             *  This is to make the client point.
             */
            writer.println("<Placemark>");
            writer.println("<name>Client</name>");
            writer.println("<Point>");
            writer.println("<coordinates>");
            writer.println(coordinates.get(0));
            writer.println("</coordinates>");
            writer.println("</Point>");
            writer.println("</Placemark>");
            /**
             *  This is to make the first route
             *  have green color.
             */
            writer.println("<Placemark>");
            writer.println("<name>TaxiID " + TaxiID + "</name>");
            writer.println("<styleUrl>#green</styleUrl>");
            writer.println("<LineString>");
            writer.println("<altitudeMode>relative</altitudeMode>");
            writer.println("<coordinates>");
            for (Coordinates co : coordinates)
                writer.println(co);
            writer.println("</coordinates>");
            writer.println("</LineString>");
            writer.println("</Placemark>");
//            writer.println("<Placemark>");
//            writer.println("<name>TaxiID " + TaxiID + "</name>");
//            writer.println("<Point>");
//            writer.println("<coordinates>");
//            writer.println(co);
//            writer.println("</coordinates>");
//            writer.println("</Point>");
//            writer.println("</Placemark>");

            i = 1;
            /**
             *  Do the same thing for all the rest
             *  of the taxi routes.
             */
            while (!results.isEmpty() && i < 5) {

                TaxiID = results.firstEntry().getKey();
                coordinates = results.firstEntry().getValue();
                results.pollFirstEntry();
                writer.println("<Placemark>");
                writer.println("<name>TaxiID " + TaxiID + "</name>");
                writer.println("<styleUrl>#taxi" + i + "</styleUrl>");
                writer.println("<LineString>");
                writer.println("<altitudeMode>relative</altitudeMode>");
                writer.println("<coordinates>");
                for (Coordinates co : coordinates)
                    writer.println(co);
                writer.println("</coordinates>");
                writer.println("</LineString>");
                writer.println("</Placemark>");
//                writer.println("<Placemark>");
//                writer.println("<name>TaxiID " + current.getId() + "</name>");
//                writer.println("<Point>");
//                writer.println("<coordinates>");
//                writer.println(current.getLocation());
//                writer.println("</coordinates>");
//                writer.println("</Point>");
//                writer.println("</Placemark>");

                i++;

            }

            writer.println("</Document>");
            writer.println("</kml>");

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } finally {
            if (writer != null)
                writer.close();
        }

    }

}
