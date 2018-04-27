import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class AStar {

    /**
     * A* needed data structures.
     */
    private HashSet<Long>                       closedSet;
    private PrioQueue<Long>                     openSet;
    private HashMap<Coordinates, Coordinates>   cameFrom;
    private HashMap<Long, Double>               gScore;
    private HashMap<Long, Double>               fScore;
    private HashMap<Integer, Double>            finalScore;

    /**
     * Result printing data structures.
     */
    private TreeMap<Double, Integer>            normalisedDistance;
    private TreeMap<Double, Integer>            trueDistance;
    private TreeMap<Double, Integer>            ratings;
    private Double                              routeScore, trueScore;

    /**
     * Class needed variables.
     */
    private JIPEngine                           jip;
    private int                                 limit;
    private PrintWriter                         writer;

    public AStar(JIPEngine jip, String limit) {
        this.jip        = jip;
        this.limit      = Integer.parseInt(limit);
        this.routeScore = Double.MAX_VALUE;
        this.trueScore  = Double.MAX_VALUE;
        this.writer     = null;
    }

    public ArrayList<Coordinates> solveClient() {

        closedSet = new HashSet<>();
        cameFrom = new HashMap<>();
        gScore = new HashMap<>();
        fScore = new HashMap<>();

        ArrayList<Coordinates> result = null;

        String file = "astar" + limit + ".csv";

        try {
            writer = new PrintWriter(file);
            writer.println("ID, Max openSet Size, A* Steps, Normalised Distance");
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.exit(1);
        }

        final Comparator<Long> qComparator = new Comparator<Long>() {
            @Override
            public int compare(Long t1, Long t2) {
                if (fScore.get(t1) < fScore.get(t2))
                    return -1;
                return 1;
            }
        };

        openSet = new PrioQueue<>(this.limit, qComparator);

        JIPTermParser parser = jip.getTermParser();
        JIPQuery clientQuery, goalQuery, nodes, penalty;
        JIPTerm client, goal, node;
        Coordinates current, neighbor, goalCo;
        Long nodeID, neighborID, goalID;
        int lineID;
        int max, steps;

        goalQuery = jip.openSynchronousQuery(parser.parseTerm("goal_node(NodeID, X, Y)."));
        goal = goalQuery.nextSolution();

        goalCo = new Coordinates(Double.parseDouble(goal.getVariablesTable().get("X").toString()),
                Double.parseDouble(goal.getVariablesTable().get("Y").toString()));
        goalID = Double.valueOf(goal.getVariablesTable().get("NodeID").toString()).longValue();

        clientQuery = jip.openSynchronousQuery(parser.parseTerm("client_node(NodeID, X, Y)."));
        client = clientQuery.nextSolution();

        current = new Coordinates(Double.parseDouble(client.getVariablesTable().get("X").toString()),
                Double.parseDouble(client.getVariablesTable().get("Y").toString()));
        nodeID = Double.valueOf(client.getVariablesTable().get("NodeID").toString()).longValue();

        cameFrom.put(current, null);
        gScore.put(nodeID, 0.0);
        fScore.put(nodeID, current.distance(goalCo));
        openSet.add(nodeID);
        max = 1;
        steps = 0;

        while(!openSet.isEmpty()) {

            steps++;
            nodeID = openSet.pollFirst();

            nodes = jip.openSynchronousQuery(parser.parseTerm("node(" + nodeID + ", X, Y)."));
            node = nodes.nextSolution();
            current = new Coordinates(Double.parseDouble(node.getVariablesTable().get("X").toString()),
                    Double.parseDouble(node.getVariablesTable().get("Y").toString()));

//                System.out.println("\nCurrent " + nodeID + ", " + current);

            /**
             * Client found.
             * Reconstruct path.
             */
            if (nodeID.equals(goalID)) {
                result = reconstructPath(current, cameFrom);
                routeScore = fScore.get(nodeID);
                writer.println("0, " + max + ", " + steps + ", " + fScore.get(nodeID));
                break;
            }

            closedSet.add(nodeID);
            Double scoreSoFar = gScore.get(nodeID);
            Double prio;

            /**
             * Find neighbors.
             */
            nodes = jip.openSynchronousQuery(parser.parseTerm("neighbors(" + nodeID + ", NeighborID, LineID, X, Y)."));

//                System.out.println("With neighbors:");
            /**
             * For each neighbor do necessary
             * operations (score, cameFrom etc.).
             */
            while((node = nodes.nextSolution()) != null) {

                neighborID = Double.valueOf(node.getVariablesTable().get("NeighborID").toString()).longValue();
                lineID = Integer.parseInt(node.getVariablesTable().get("LineID").toString());
                neighbor = new Coordinates(Double.parseDouble(node.getVariablesTable().get("X").toString()),
                        Double.parseDouble(node.getVariablesTable().get("Y").toString()));

                if (closedSet.contains(neighborID))
                    continue;

                penalty = jip.openSynchronousQuery(parser.parseTerm("penalty(" + lineID + ", Penalty)."));
                prio = Double.parseDouble(penalty.nextSolution().getVariablesTable().get("Penalty").toString());

                Double tGScore = scoreSoFar + prio * current.distance(neighbor);

//                    System.out.print(neighborID + ", ");
//                    System.out.print(neighbor);
//                    System.out.print(" with gScore " + tGScore);
//                    System.out.println(" with fScore " + (tGScore + neighbor.distance(goalCo)));

                if (gScore.containsKey(neighborID)) {
                    if (tGScore < gScore.get(neighborID)) {
                        if (openSet.contains(neighborID)) {
                            gScore.put(neighborID, tGScore);
                            fScore.put(neighborID, (tGScore + prio * neighbor.distance(goalCo)));
                            openSet.remove(neighborID);
                            openSet.add(neighborID);
                            cameFrom.put(neighbor, current);
                            //System.out.println("Added after removing");
                        } else {
                            double prevFScore = fScore.get(neighborID);
                            fScore.put(neighborID, (tGScore + prio * neighbor.distance(goalCo)));
                            if (openSet.add(neighborID)) {
                                gScore.put(neighborID, tGScore);
                                cameFrom.put(neighbor, current);
                                //System.out.println("Added with better score");
                            } else {
                                fScore.put(neighborID, prevFScore);
                            }
                        }
                    }
                } else {
                    fScore.put(neighborID, (tGScore + prio * neighbor.distance(goalCo)));
                    if (openSet.add(neighborID)) {
                        gScore.put(neighborID, tGScore);
                        cameFrom.put(neighbor, current);
                        //System.out.println("Added normally");
                    } else {
                        fScore.remove(neighborID);
                    }
                }

            }

            if (openSet.size() > max)
                max = openSet.size();

        }

        return result;

    }

    public TreeMap< Integer, ArrayList<Coordinates> > solveTaxis() {

        closedSet = new HashSet<>();
        cameFrom = new HashMap<>();
        gScore = new HashMap<>();
        fScore = new HashMap<>();
        finalScore = new HashMap<>();

        normalisedDistance = new TreeMap<>();
        trueDistance = new TreeMap<>();
        ratings = new TreeMap<>();

        final Comparator<Long> qComparator = new Comparator<Long>() {
            @Override
            public int compare(Long t1, Long t2) {
                if (fScore.get(t1) < fScore.get(t2))
                    return -1;
                return 1;
            }
        };

        final Comparator<Integer> tComparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer t1, Integer t2) {
                if (finalScore.get(t1) < finalScore.get(t2))
                    return -1;
                return 1;
            }
        };

        openSet = new PrioQueue<>(this.limit, qComparator);
        TreeMap< Integer, ArrayList<Coordinates> > result = new TreeMap<>(tComparator);

        JIPTermParser parser = jip.getTermParser();
        JIPQuery taxis, goalQuery;
        JIPTerm taxi, goal;
        Coordinates clientco;
        Long goalID;
        Double prio;

        goalQuery = jip.openSynchronousQuery(parser.parseTerm("client_node(NodeID, X, Y)."));
        goal = goalQuery.nextSolution();

        clientco = new Coordinates(Double.parseDouble(goal.getVariablesTable().get("X").toString()),
                Double.parseDouble(goal.getVariablesTable().get("Y").toString()));
        goalID = Long.parseLong(goal.getVariablesTable().get("NodeID").toString());

//        System.out.println("Goal coordinates " + clientco);

        taxis = jip.openSynchronousQuery(parser.parseTerm("eligible_taxi(TaxiID, NodeID, X, Y, Rating)."));

        while ((taxi = taxis.nextSolution()) != null) {

            JIPQuery nodes, penalty;
            JIPTerm node;
            int taxiID, lineID;
            int max, steps;
            Long nodeID, neighborID;
            Coordinates current, neighbor;
            Double rating;

            closedSet.clear();
            openSet.clear();
            cameFrom.clear();
            gScore.clear();
            fScore.clear();

            taxiID = Integer.parseInt(taxi.getVariablesTable().get("TaxiID").toString());
            nodeID = Double.valueOf(taxi.getVariablesTable().get("NodeID").toString()).longValue();
            current = new Coordinates(Double.parseDouble(taxi.getVariablesTable().get("X").toString()),
                    Double.parseDouble(taxi.getVariablesTable().get("Y").toString()));
            rating = Double.parseDouble(taxi.getVariablesTable().get("Rating").toString());

//            System.out.println();
//            System.out.println(current);

            cameFrom.put(current, null);
            gScore.put(nodeID, 0.0);
            fScore.put(nodeID, current.distance(clientco));
            openSet.add(nodeID);
            max = 1;
            steps = 0;

            while(!openSet.isEmpty()) {

                steps++;
                nodeID = openSet.pollFirst();

                nodes = jip.openSynchronousQuery(parser.parseTerm("node(" + nodeID + ", X, Y)."));
                node = nodes.nextSolution();
                current = new Coordinates(Double.parseDouble(node.getVariablesTable().get("X").toString()),
                        Double.parseDouble(node.getVariablesTable().get("Y").toString()));

//                System.out.println("\nCurrent " + nodeID + ", " + current);

                /**
                 * Client found.
                 * Reconstruct path.
                 */
                if (nodeID.equals(goalID)) {
                    finalScore.put(taxiID, fScore.get(nodeID));
                    normalisedDistance.put(fScore.get(nodeID), taxiID);
                    result.put(taxiID, reconstructTaxiPath(taxiID, current, cameFrom));
                    ratings.put(rating, taxiID);
                    writer.println(taxiID + ", " + max + ", " + steps + ", " + fScore.get(nodeID));
                    break;
                }

                closedSet.add(nodeID);
                Double scoreSoFar = gScore.get(nodeID);

                /**
                 * Find neighbors.
                 */
                nodes = jip.openSynchronousQuery(parser.parseTerm("neighbors(" + nodeID + ", NeighborID, LineID, X, Y)."));

//                System.out.println("With neighbors:");
                /**
                 * For each neighbor do necessary
                 * operations (score, cameFrom etc.).
                 */
                while((node = nodes.nextSolution()) != null) {

                    neighborID = Double.valueOf(node.getVariablesTable().get("NeighborID").toString()).longValue();
                    lineID = Integer.parseInt(node.getVariablesTable().get("LineID").toString());
                    neighbor = new Coordinates(Double.parseDouble(node.getVariablesTable().get("X").toString()),
                            Double.parseDouble(node.getVariablesTable().get("Y").toString()));

                    if (closedSet.contains(neighborID))
                        continue;

                    penalty = jip.openSynchronousQuery(parser.parseTerm("penalty(" + lineID + ", Penalty)."));
                    prio = Double.parseDouble(penalty.nextSolution().getVariablesTable().get("Penalty").toString());

                    Double tGScore = scoreSoFar + prio * current.distance(neighbor);
//                    System.out.print(neighborID + ", ");
//                    System.out.print(neighbor);
//                    System.out.print(" with gScore " + tGScore);
//                    System.out.println(" with fScore " + (tGScore + neighbor.distance(clientco)));

                    if (gScore.containsKey(neighborID)) {
                        if (tGScore < gScore.get(neighborID)) {
                            if (openSet.contains(neighborID)) {
                                gScore.put(neighborID, tGScore);
                                fScore.put(neighborID, (tGScore + prio * neighbor.distance(clientco)));
                                openSet.remove(neighborID);
                                openSet.add(neighborID);
                                cameFrom.put(neighbor, current);
                                //System.out.println("Added after removing");
                            } else {
                                double prevFScore = fScore.get(neighborID);
                                fScore.put(neighborID, (tGScore + prio * neighbor.distance(clientco)));
                                if (openSet.add(neighborID)) {
                                    gScore.put(neighborID, tGScore);
                                    cameFrom.put(neighbor, current);
                                    //System.out.println("Added with better score");
                                } else {
                                    fScore.put(neighborID, prevFScore);
                                }
                            }
                        }
                    } else {
                        fScore.put(neighborID, (tGScore + prio * neighbor.distance(clientco)));
                        if (openSet.add(neighborID)) {
                            gScore.put(neighborID, tGScore);
                            cameFrom.put(neighbor, current);
                            //System.out.println("Added normally");
                        } else {
                            fScore.remove(neighborID);
                        }
                    }

                }

                if (openSet.size() > max)
                    max = openSet.size();

            }

        }

        writer.flush();
        writer.close();
        return result;

    }

    /**
     * Reconstructs the path of the route.
     * Finds all the previous vertices through the
     * cameFrom map, and returns all the parents in
     * an <code>ArrayList</code> of vertices.
     * @param goal
     * @param cameFrom
     * @return
     */
    private ArrayList<Coordinates> reconstructPath(Coordinates goal, HashMap<Coordinates, Coordinates> cameFrom) {
        ArrayList<Coordinates> result = new ArrayList<>();
        Coordinates temp;
        trueScore = 0.0;
        result.add(goal);
        while ((temp = cameFrom.get(goal)) != null) {
            trueScore += goal.distance(temp);
            goal = temp;
            result.add(goal);
        }
        return result;
    }

    /**
     * Reconstructs the path of the route.
     * Finds all the previous vertices through the
     * cameFrom map, and returns all the parents in
     * an <code>ArrayList</code> of vertices.
     * @param taxiID
     * @param goal
     * @param cameFrom
     * @return
     */
    private ArrayList<Coordinates> reconstructTaxiPath(Integer taxiID, Coordinates goal, HashMap<Coordinates, Coordinates> cameFrom) {
        ArrayList<Coordinates> result = new ArrayList<>();
        Coordinates temp;
        Double distance = 0.0;
        result.add(goal);
        while ((temp = cameFrom.get(goal)) != null) {
            distance += goal.distance(temp);
            goal = temp;
            result.add(goal);
        }
        trueDistance.put(distance, taxiID);
        return result;
    }

    public void printResults() {

        int i;

        System.out.println("* * * R E S U L T S * * *");
        System.out.println("Normalised Route Distance\t: " + this.routeScore);
        System.out.println("True Route Distance\t\t\t: " + this.trueScore);
        System.out.println();
        System.out.println("* * * T A X I S * * *");

        System.out.println("N O R M A L I S E D");
        System.out.println("TaxiID\t: Normalised Distance");
        i = 0;
        while (!normalisedDistance.isEmpty() && i < 5) {

            System.out.println(normalisedDistance.firstEntry().getValue() + "\t\t: " + normalisedDistance.firstEntry().getKey());
            normalisedDistance.pollFirstEntry();
            i++;

        }

        System.out.println("T R U E");
        System.out.println("TaxiID\t: True Distance");
        i = 0;
        while (!trueDistance.isEmpty() && i < 5) {

            System.out.println(trueDistance.firstEntry().getValue() + "\t\t: " + trueDistance.firstEntry().getKey());
            trueDistance.pollFirstEntry();
            i++;

        }

        System.out.println("R A T I N G S");
        System.out.println("TaxiID\t: Rating");
        while (!ratings.isEmpty()) {

            System.out.println(ratings.lastEntry().getValue() + "\t\t: " + ratings.lastEntry().getKey());
            ratings.pollLastEntry();

        }

        System.out.println();

    }

}
