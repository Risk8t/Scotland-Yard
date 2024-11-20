package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.units.qual.A;
import uk.ac.bris.cs.scotlandyard.model.*;

public class Cicada implements Ai {
	@Nonnull @Override public String name() { return "Cicada3301"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation


		// Finding all moves that MrX can do
		ArrayList<Move> mrXAvailableMoves = new ArrayList<>(board.getAvailableMoves().stream().filter(x-> x.commencedBy().isMrX()).collect(Collectors.toList()));

		// Finding all stations that MrX can move to
		ImmutableList<Integer> mrXAvailableNodes = getMrXAvailableMovesInt(mrXAvailableMoves);

		// Rating of every move that MrX can do
		HashMap<Integer, Integer> distancesToNodes = new HashMap<>();

		// Finding current location of each detective
		List<Integer> detectiveLocations = getDetectiveLocations(board);

		for (Integer detLocation : detectiveLocations) {
			for (Integer mrXLocation : mrXAvailableNodes) {
				// Creating a list of distances from a detective to every station
				HashMap<Integer, Integer> distanceFromBeginningNode = new HashMap<>();
				for (Integer station : board.getSetup().graph.nodes()) {
					distanceFromBeginningNode.put(station, Integer.MAX_VALUE);
				}

				distanceFromBeginningNode.put(detLocation, 0);
				ArrayList<Integer> visitedNodes = new ArrayList<>();
				Integer totalDistance = 1;

				distancesToNodes.put(
						mrXLocation,
						djikstra(board,
								 detLocation,
								 mrXLocation,
								 distanceFromBeginningNode,
								 totalDistance,
								 visitedNodes)
				);
			}
		}

		return furthestMoveFromDetectives(distancesToNodes, mrXAvailableMoves);
	}


//	 Dijkstra's algorithm, returns a distance between two nodes
	public Integer djikstra(Board board,
						Integer current,
						Integer end,
						HashMap<Integer, Integer> distanceFromBeginningNode,
						Integer totalDistance,
						ArrayList<Integer> visitedNodes) {

		if (current.equals(end)) {
			return distanceFromBeginningNode.get(end);
		}

		// Updating distance to all adjacent stations
		for (Integer currentStation : board.getSetup().graph.adjacentNodes(current)) {
			if (!visitedNodes.contains(currentStation)) {
				if (distanceFromBeginningNode.get(currentStation) + 1 < distanceFromBeginningNode.getOrDefault(currentStation, Integer.MAX_VALUE)) {
					distanceFromBeginningNode.put(currentStation, totalDistance);
				}
			}
		}
		visitedNodes.add(current);

		//Check if the end station was reached
		if (visitedNodes.contains(end)) {
			return distanceFromBeginningNode.get(end);
		}

		totalDistance++;

		//Next current is selected for recursive call
		Integer nextNode = null;
		for (Integer node : distanceFromBeginningNode.keySet()) {
			if (!visitedNodes.contains(node)){

				nextNode = distanceFromBeginningNode.get(node);
			}
		}

		if (nextNode != null) {
			return djikstra(board, nextNode, end, distanceFromBeginningNode, totalDistance, visitedNodes);
		} else {
			return distanceFromBeginningNode.getOrDefault(end, Integer.MAX_VALUE);
		}
	}

	//Find mrX's available move that would result in the furthest distance from the closest detective
	public Move furthestMoveFromDetectives(HashMap<Integer, Integer> distanceFromBeginningNode, ArrayList<Move> mrXAvailableMoves) {
		if (distanceFromBeginningNode.isEmpty()) {
			throw new IllegalArgumentException("distanceFromBeginningNode is empty");
		}

		List<Integer> distanceFromBeginningNodeValues = new ArrayList<>(distanceFromBeginningNode.values());
		List<Integer> distanceFromBeginningNodeKeys = new ArrayList<>(distanceFromBeginningNode.keySet());
		Integer furthestMoveFromDetectives = distanceFromBeginningNodeValues.stream().max(Comparator.naturalOrder()).orElse(0);
		int index = distanceFromBeginningNodeKeys .indexOf(furthestMoveFromDetectives);
		int key = distanceFromBeginningNodeKeys.get(index);

		for (Move move : mrXAvailableMoves) {
			if (move.commencedBy().isMrX()) {
				if (move instanceof Move.SingleMove singleMove) {
					if (key == singleMove.destination) {
						return move;
					}
				} else if (move instanceof Move.DoubleMove doubleMove) {
					if (key == doubleMove.destination2) {
						return move;
					}
				}
			}
		}

		throw new IllegalArgumentException("furthestMoveFromDetectiveNotFound");
	}

	public static List<Integer> getDetectiveLocations(Board board) {

		ArrayList<Move> detectivesAvailableMoves = new ArrayList<>(board.getAvailableMoves().stream().filter(x-> x.commencedBy().isDetective()).collect(Collectors.toList()));

		if (detectivesAvailableMoves.isEmpty()) {
			throw new IllegalArgumentException("detectiveAvailableMoves are empty");
		}

		return detectivesAvailableMoves.stream()
				.filter(x -> x.commencedBy().isDetective())
				.filter(x -> x instanceof Move.SingleMove)
				.map(x -> ((Move.SingleMove) x).source())
				.distinct().collect(Collectors.toList());
	}


	public static ImmutableList<Integer> getMrXAvailableMovesInt(ArrayList<Move> mrXAvailableMoves) {
		List<Integer> availableMovesNodes = new ArrayList<>();
		for (Move move : mrXAvailableMoves) {
			if (move.commencedBy().isMrX()) {
				if (move instanceof Move.SingleMove singleMove) {
					availableMovesNodes.add(singleMove.destination);
				} else if (move instanceof Move.DoubleMove doubleMove) {
					availableMovesNodes.add(doubleMove.destination2);
				}
			}
		}
		return ImmutableList.copyOf(availableMovesNodes);
	}

}
