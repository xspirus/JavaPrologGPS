/**
 * Penalty coefficient for lanes.
 * If only 1 lane then we have a big
 * penalty lowering it for 2,3 and
 * 4 respectively.
 */
lane_penalty(1, Temp, Penalty) :-
  Penalty is Temp + 0.5.
lane_penalty(2, Temp, Penalty) :-
  Penalty is Temp + 0.3.
lane_penalty(3, Temp, Penalty) :-
  Penalty is Temp + 0.2.
lane_penalty(4, Temp, Penalty) :-
  Penalty is Temp.

/**
 * Computes the penalty depending on
 * the traffic at this time of the day.
 * Also uses the lanes.
 */
find_penalty(low, _, Penalty) :-
  Penalty is 1.
find_penalty(medium, Lanes, Penalty) :-
  Temp is 1.5,
  lane_penalty(Lanes, Temp, Penalty).
find_penalty(high, Lanes, Penalty) :-
  Temp is 2.5,
  lane_penalty(Lanes, Temp, Penalty).

/**
 * Computes the traffic coefficient.
 */
traffic_penalty(Line, Time, Penalty) :-
  Time >= 900,
  Time =< 1100,
  traffic(Line, Traffic, _, _),
  line(Line, _, _, Lanes, _, _),
  find_penalty(Traffic, Lanes, Penalty), !.
traffic_penalty(Line, Time, Penalty) :-
  Time >= 1300,
  Time =< 1500,
  traffic(Line, _, Traffic, _),
  line(Line, _, _, Lanes, _, _),
  find_penalty(Traffic, Lanes, Penalty), !.
traffic_penalty(Line, Time, Penalty) :-
  Time >= 1700,
  Time =< 1900,
  traffic(Line, _, _, Traffic),
  line(Line, _, _, Lanes, _, _),
  find_penalty(Traffic, Lanes, Penalty), !.
traffic_penalty(Line, _, Penalty) :-
  line(Line, _, _, Lanes, _, _),
  find_penalty(low, Lanes, Penalty), !.

penalty(Line, Penalty) :-
  client(_, _, Time, _, _, _),
  line(Line, _, _, _, MaxSpeed, _),
  traffic_penalty(Line, Time, Temp),
  Penalty is Temp * 50 / MaxSpeed.

/**
 * Finds eligible taxis, considering
 * capacity, language and availability.
 */
eligible_taxi(TaxiID, NodeID, X, Y, Rating) :-
  taxi(TaxiID, X, Y),
  once(node(NodeID, X, Y)),
  taxiInfo(TaxiID, 1, Max, both, _, Rating),
  client(_, _, _, Persons, _, _),
  Max >= Persons.
eligible_taxi(TaxiID, NodeID, X, Y, Rating) :-
  taxi(TaxiID, X, Y),
  once(node(NodeID, X, Y)),
  taxiInfo(TaxiID, 1, Max, Lang, _, Rating),
  client(_, _, _, Persons, Lang, _),
  Max >= Persons.

/**
 * Finds the client coordinates
 * as well as the nodeID.
 */
client_node(NodeID, X, Y) :-
  client(X, Y, _, _, _, _),
  once(node(NodeID, X, Y)).

goal_node(NodeID, X, Y) :-
  goal(X, Y),
  once(node(NodeID, X, Y)).

/**
 * Figures out if line is good
 * for the hour of need. In the
 * night hours we want lit streets
 * and no tolls. In the morning we
 * do not care.
 */
accept_line(Line, Time) :-
  Time =< 700,
  line(Line, _, 1, _, _, 0), !.
accept_line(Line, Time) :-
  Time >= 1900,
  line(Line, _, 1, _, _, 0), !.
accept_line(Line, _) :-
  line(Line, _, _, _, _, _).

/**
 * Finds line of neighbors and
 * calls accept_line.
 */
accept_neighbor(Parent, Neighbor, Line, Time) :-
  belongsTo(Parent, Line),
  belongsTo(Neighbor, Line),
  accept_line(Line, Time).

/**
 * Finds eligible neighbors.
 */
neighbors(Parent, Neighbor, Line, X, Y) :-
  next(Parent, Neighbor),
  client(_, _, Time, _, _, _),
  accept_neighbor(Parent, Neighbor, Line, Time),
  once(node(Neighbor, X, Y)).
