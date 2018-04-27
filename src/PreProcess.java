import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

import java.io.*;
import java.util.ArrayList;

public class PreProcess {

    private PrintWriter prolog = null;
    private JIPEngine jip = null;

    PreProcess(PrintWriter prolog, JIPEngine jip) {
        this.prolog = prolog;
        this.jip = jip;
    }

    void readLines(String linefile) {

        BufferedReader reader = null;
        String split = ",";
        try {
            reader = new BufferedReader(new FileReader(linefile));
            prolog.println("/**\n * line(lineId, oneway, lit, lanes, maxspeed, toll).\n */");
            String line;
            String[] stats;
            int id, oneway, lit, lanes, maxspeed, toll;
            /**
             * Skip first line.
             */
            reader.readLine();
            /**
             * Read all the lines and make prolog rules.
             */
            while ((line = reader.readLine()) != null) {
                stats = line.split(split, -1);      // -1 to keep trailing empty strings;
                if (stats[1].equals("")
                        || stats[1].equals("footway")
                        || stats[1].equals("path")
                        || stats[1].equals("pedestrian")
                        || stats[1].equals("track")
                        || stats[1].equals("steps")
                        || stats[1].equals("service")
                        )
                    continue;
                id = Integer.parseInt(stats[0]);
                oneway = (stats[3].equals("yes")) ? 1 : ((stats[3].equals("-1")) ? -1 : 0 );
                lit = (stats[4].equals("no")) ? 0 : 1;
                lanes = (stats[5].equals("")) ? 1 : Integer.parseInt(stats[5]);
                maxspeed = (stats[6].equals("")) ? 50 : Integer.parseInt(stats[6]);
                toll = (stats[17].equals("yes")) ? 1 : 0;
                prolog.println("line("
                                + id + ", "
                                + oneway + ", "
                                + lit + ", "
                                + lanes + ", "
                                + maxspeed + ", "
                                + toll + ").");
            }
            prolog.println();
            prolog.flush();
            this.jip.consultFile("info.pl");
        } catch (FileNotFoundException e) {
            System.out.println("No " + linefile + " found");
            System.out.println("Exiting...");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println("Exiting...");
            System.exit(-1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close " + linefile);
            }
        }

    }

    void readTraffic(String trafficfile) {

        BufferedReader reader = null;
        String split = ",";
        String value = "\\|";
        JIPTermParser parser = jip.getTermParser();
        JIPQuery query;

        try {
            reader = new BufferedReader(new FileReader(trafficfile));
            prolog.println("/**\n * traffic(lineId, 0900 - 1100, 1300 - 1500, 1700 - 1900).\n */");
            String line;
            String[] stats;
            int id;
            /**
             * Skip first line.
             */
            reader.readLine();
            /**
             * Read all the lines and make prolog rules.
             */
            while ((line = reader.readLine()) != null) {
                stats = line.split(split, -1);      // -1 to keep trailing empty strings;
                id = Integer.parseInt(stats[0]);
                String strTerm = "line(" + id + ", _, _, _, _, _).";
                query = jip.openSynchronousQuery(parser.parseTerm(strTerm));
                if (query.nextSolution() == null)
                    continue;
                if (stats.length < 3) {
                    prolog.println("traffic(" + id + ", low, low, low).");
                    continue;
                }
                if (stats[2].equals("")) {
                    prolog.println("traffic(" + id + ", low, low, low).");
                } else {
                    String[] traffic = stats[2].split(value, -1);
                    if (traffic.length < 3) {
                        prolog.println("traffic(" + id + ", low, low, low).");
                    } else {
                        prolog.println("traffic("
                                        + id + ", "
                                        + traffic[0].substring(12) + ", "
                                        + traffic[1].substring(12) + ", "
                                        + traffic[2].substring(12) + ").");
                    }
                }
            }
            prolog.println();
        } catch (FileNotFoundException e) {
            System.out.println("No " + trafficfile + " found");
            System.out.println("Exiting...");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println("Exiting...");
            System.exit(-1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close " + trafficfile);
            }
        }

    }

    void readNodes(String nodefile) {

        BufferedReader reader = null;
        String split = ",";
        JIPTermParser parser = jip.getTermParser();
        JIPQuery query;
        JIPTerm term;

        try {
            reader = new BufferedReader(new FileReader(nodefile));
            prolog.println("/**");
            prolog.println(" * next(nodeIDfrom, nodeIDto).");
            prolog.println(" */");
            String line;
            String[] stats;
            /**
             * Skip first line.
             */
            reader.readLine();
            /**
             * Read all the lines and make prolog rules.
             */
            int i = 0, oneway = 0;
            ArrayList<Long> nodeID;
            ArrayList<Integer> lineID;
            ArrayList<Double> x, y;
            nodeID = new ArrayList<>();
            lineID = new ArrayList<>();
            x = new ArrayList<>();
            y = new ArrayList<>();
            line = reader.readLine();
            stats = line.split(split, -1);      // -1 to keep trailing empty strings;
            x.add(Double.parseDouble(stats[0]));
            y.add(Double.parseDouble(stats[1]));
            lineID.add(Integer.parseInt(stats[2]));
            nodeID.add(Long.parseLong(stats[3]));
            query = jip.openSynchronousQuery(parser.parseTerm("line("+ lineID.get(i) + ", Oneway, _, _, _, _)."));
            term = query.nextSolution();
            if (term != null)
                oneway = Integer.parseInt(term.getVariablesTable().get("Oneway").toString());
            i++;
            while ((line = reader.readLine()) != null) {
                stats = line.split(split, -1);      // -1 to keep trailing empty strings;
                x.add(Double.parseDouble(stats[0]));
                y.add(Double.parseDouble(stats[1]));
                lineID.add(Integer.parseInt(stats[2]));
                nodeID.add(Long.parseLong(stats[3]));
                if (lineID.get(i).equals(lineID.get(i - 1))) {
                    if (term == null) {
                        i++;
                        continue;
                    }
                    if (oneway == 0) {
                        prolog.println("next("
                                        + nodeID.get(i - 1) + ", "
                                        + nodeID.get(i) + ").");
                        prolog.println("next("
                                        + nodeID.get(i) + ", "
                                        + nodeID.get(i - 1) + ").");
                    } else if (oneway == 1) {
                        prolog.println("next("
                                        + nodeID.get(i - 1) + ", "
                                        + nodeID.get(i) + ").");
                    } else {
                        prolog.println("next("
                                        + nodeID.get(i) + ", "
                                        + nodeID.get(i - 1) + ").");
                    }
                } else {
                    query = jip.openSynchronousQuery(parser.parseTerm("line(" + lineID.get(i) + ", Oneway, _, _, _, _)."));
                    term = query.nextSolution();
                    if (term != null)
                        oneway = Integer.parseInt(term.getVariablesTable().get("Oneway").toString());
                }
                i++;
            }
            prolog.println();

            prolog.println("/**");
            prolog.println(" * belongsTo(nodeID, lineID).");
            prolog.println(" */");
            for (i = 0; i < nodeID.size(); i++) {
                query = jip.openSynchronousQuery(parser.parseTerm("line(" + lineID.get(i) + ", _, _, _, _, _)."));
                if (query.nextSolution() == null)
                    continue;
                prolog.println("belongsTo("
                                + nodeID.get(i) + ", "
                                + lineID.get(i) + ").");
            }
            prolog.println();

            prolog.println("/**");
            prolog.println(" * node(nodeID, X, Y).");
            prolog.println(" */");
            for (i = 0; i < nodeID.size(); i++) {
                query = jip.openSynchronousQuery(parser.parseTerm("line(" + lineID.get(i) + ", _, _, _, _, _)."));
                if (query.nextSolution() == null)
                    continue;
                prolog.println("node("
                                + nodeID.get(i) + ", "
                                + x.get(i) + ", "
                                + y.get(i) + ").");
            }
            prolog.println();
            prolog.flush();
            this.jip.consultFile("info.pl");
        } catch (FileNotFoundException e) {
            System.out.println("No " + nodefile + " found");
            System.out.println("Exiting...");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println("Exiting...");
            System.exit(-1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close " + nodefile);
            }
        }

    }

    void readTaxis(String taxifile) {

        BufferedReader reader = null;
        String split = ",";
        String langsplit = "\\|";
        JIPTermParser parser = jip.getTermParser();
        JIPQuery query, temp;
        JIPTerm term;

        try {
            reader = new BufferedReader(new FileReader(taxifile));
            prolog.println("/**");
            prolog.println(" * taxiInfo(taxiID, available, maxcap, lang, dist, rating).");
            prolog.println(" */");
            String line;
            String[] stats;
            ArrayList<Integer> taxiID;
            ArrayList<Double> x, y;
            taxiID = new ArrayList<>();
            x = new ArrayList<>();
            y = new ArrayList<>();
            int available, cap, dist;
            Double rating;
            String language;
            /**
             * Skip first line.
             */
            reader.readLine();
            /**
             * Read all the lines and make prolog rules.
             */
            int i = 0;
            while ((line = reader.readLine()) != null) {
                stats = line.split(split, -1);
                taxiID.add(Integer.parseInt(stats[2]));
                x.add(Double.parseDouble(stats[0]));
                y.add(Double.parseDouble(stats[1]));
                available = (stats[3].equals("yes")) ? 1 : 0;
                cap = Integer.parseInt(stats[4].substring(2));
                rating = Double.parseDouble(stats[6]);
                dist = (stats[7].equals("yes")) ? 1 : 0;
                String[] lang = stats[5].split(langsplit, -1);
                if (lang.length > 1) {
                    language = "both";
                } else {
                    language = lang[0];
                }
                prolog.println("taxiInfo("
                                + taxiID.get(i) + ", "
                                + available + ", "
                                + cap + ", "
                                + language + ", "
                                + dist + ", "
                                + rating + ").");
                i++;
            }
            prolog.println();

            Coordinates taxico, nodeco, result;
            Double min, distance;
            prolog.println("/**");
            prolog.println(" * taxi(taxiID, X, Y).");
            prolog.println(" */");
            for (i = 0; i < taxiID.size(); i++) {
                min = Double.MAX_VALUE;
                result = null;
                taxico = new Coordinates(x.get(i), y.get(i));
                query = jip.openSynchronousQuery(parser.parseTerm("node(_, X, Y)."));
                while ((term = query.nextSolution()) != null) {
                    nodeco = new Coordinates(Double.parseDouble(term.getVariablesTable().get("X").toString()),
                            Double.parseDouble(term.getVariablesTable().get("Y").toString()));
                    distance = taxico.distance(nodeco);
                    if (distance.compareTo(min) < 0) {
                        min = distance;
                        result = new Coordinates(nodeco);
                    }
                }
                prolog.println("taxi("
                                + taxiID.get(i) + ", "
                                + result + ").");
            }
            prolog.println();
        } catch (FileNotFoundException e) {
            System.out.println("No " + taxifile + " found");
            System.out.println("Exiting...");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println("Exiting...");
            System.exit(-1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close " + taxifile);
            }
        }

    }

    void readClient(String clientfile) {

        BufferedReader reader = null;
        String split = ",";
        String timesplit = ":";
        JIPTermParser parser = jip.getTermParser();
        JIPQuery query, temp;
        JIPTerm term;

        try {
            reader = new BufferedReader(new FileReader(clientfile));
            prolog.println("/**");
            prolog.println(" * client(X, Y, time, persons, language, luggage).");
            prolog.println(" */");
            String line;
            String[] stats, timestamp;
            Double x, y, distance, min = Double.MAX_VALUE;
            Coordinates cl, current, result = null;
            int time, persons, luggage;
            String language;
            /**
             * Skip first line.
             */
            reader.readLine();
            /**
             * Read client.
             */
            line = reader.readLine();
            stats = line.split(split, -1);
            x = Double.parseDouble(stats[0]);
            y = Double.parseDouble(stats[1]);
            cl = new Coordinates(x, y);
            query = jip.openSynchronousQuery(parser.parseTerm("node(_, X, Y)."));
            while ((term = query.nextSolution()) != null) {
                current = new Coordinates(Double.parseDouble(term.getVariablesTable().get("X").toString()),
                        Double.parseDouble(term.getVariablesTable().get("Y").toString()));
                distance = cl.distance(current);
                if (distance.compareTo(min) < 0) {
                    min = distance;
                    result = new Coordinates(current);
                }
            }
            timestamp = stats[4].split(timesplit, -1);
            time = Integer.parseInt(timestamp[0]) * 100 + Integer.parseInt(timestamp[1]);
            persons = Integer.parseInt(stats[5]);
            language = stats[6];
            luggage = Integer.parseInt(stats[7]);
            prolog.println("client("
                            + result + ", "
                            + time + ", "
                            + persons + ", "
                            + language + ", "
                            + luggage + ").");
            prolog.println();
            x = Double.parseDouble(stats[2]);
            y = Double.parseDouble(stats[3]);
            min = Double.MAX_VALUE;
            cl = new Coordinates(x, y);
            query = jip.openSynchronousQuery(parser.parseTerm("node(_, X, Y)."));
            while ((term = query.nextSolution()) != null) {
                current = new Coordinates(Double.parseDouble(term.getVariablesTable().get("X").toString()),
                        Double.parseDouble(term.getVariablesTable().get("Y").toString()));
                distance = cl.distance(current);
                if (distance.compareTo(min) < 0) {
                    min = distance;
                    result = new Coordinates(current);
                }
            }
            prolog.println("/**");
            prolog.println(" * goal(X, Y).");
            prolog.println(" */");
            prolog.println("goal(" + result + ").");
        } catch (FileNotFoundException e) {
            System.out.println("No " + clientfile + " found");
            System.out.println("Exiting...");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println("Exiting...");
            System.exit(-1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close " + clientfile);
            }
        }

    }

}
