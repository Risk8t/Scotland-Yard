package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
//@SuppressWarnings("all")
public final class MyGameStateFactory implements Factory<GameState> {

    private final class MyGameState implements GameState {

        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private List<Player> detectives;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;

        // Check that all detectives are in different locations and there are no duplicate pieces
        private void detectivesAreInDifferentLocationsAndNoDuplicatePiecesCheck() {
            int i = 0;
            int j = i + 1;
            int numOfDetectives = this.detectives.size();

            for (Player first : this.detectives.subList(0, numOfDetectives - 1)) {
                for (Player second : this.detectives.subList(j, numOfDetectives)) {
                    if (first.location() == second.location()) {
                        throw new IllegalArgumentException("One location cannot have more than 2 detectives at once!");
                    }
                    if (first.piece().webColour().equals(second.piece().webColour())) {
                        throw new IllegalArgumentException("No duplicate pieces are allowed!");
                    }
                    j++;
                }
                i++;
                j = i + 1;
            }
        }

        // Check if MrX is indeed the black piece
        private void MrXIsTheBlackPieceCheck() {
            if (!mrX.piece().webColour().equals("#000")) {
                throw new IllegalArgumentException("MrX is not the black piece");
            }
        }

        // Check if all detectives are indeed holding detective pieces
        private void allDetectivesHoldDetectivePiecesCheck() {
            if (this.detectives.stream().anyMatch(x -> x.piece().isMrX())) {
                throw new IllegalArgumentException("Some detective holds a non-detective piece!");
            }
        }

        // Check that no detectives have a secret or double ticket
        private void noDetectiveHasSecretOrDoubleTicketCheck() {
            if (this.detectives.stream().anyMatch(x -> x.tickets().get(Ticket.SECRET) + x.tickets().get(Ticket.DOUBLE) != 0)) {
                throw new IllegalArgumentException("Some detective has a secret or a double ticket!");
            }
        }

        // Check if all detectives ran out of tickets -> MrX won
        private void allDetectivesRanOutOfTicketsCheck() {
            if (this.winner.isEmpty()) {
                boolean noTickets = true;
                for (Player player : this.detectives) {
                    if (
                            player.hasAtLeast(Ticket.TAXI, 1)
                                    || player.hasAtLeast(Ticket.BUS, 1)
                                    || player.hasAtLeast(Ticket.UNDERGROUND, 1)
                    )
                    {
                        noTickets = false;
                        break;
                    }
                }
                if (noTickets) {
                    this.winner = ImmutableSet.of(mrX.piece());
                }
            }
        }

        // Check if MrX ran out of tickets -> detectives won
        private void mrXRanOutOfTicketsCheck() {
            if (this.winner.isEmpty() && this.remaining.contains(mrX.piece())) {
                if (! (mrX.hasAtLeast(Ticket.TAXI, 1) || mrX.hasAtLeast(Ticket.BUS, 1) || mrX.hasAtLeast(Ticket.UNDERGROUND, 1) || mrX.hasAtLeast(Ticket.SECRET, 1))) {
                    this.winner = ImmutableSet.copyOf(this.detectives.stream().map(Player::piece).collect(Collectors.toSet()));
                }
            }
        }

        //  check if any detective is on the same location as MrX
        private void mrXCaughtCheck() {
            if (this.winner.isEmpty()) {
                for (Player current : this.detectives ) {
                    if (current.location() == mrX.location()) {
                        this.winner = ImmutableSet.copyOf(this.detectives.stream().map(Player::piece).collect(Collectors.toSet()));
                        break;
                    }
                }
            }
        }

        // If no winner is determined, search for available moves, otherwise no moves are possible.
        private void makeMoves() {
            if (this.winner.isEmpty()) {
                Set<Move> moveSet = new HashSet<>();
                Player realPlayer = null;
                for (Piece currentPiece : this.remaining) {
                    List<Player> playersInGame = new ArrayList<>(this.detectives);
                    playersInGame.add(this.mrX);
                    for (Player currentPlayer : playersInGame) {
                        if (currentPlayer.piece() == currentPiece) {
                            realPlayer = currentPlayer;
                        }
                    }
                    assert realPlayer != null;
                    moveSet.addAll(makeSingleMoves(this.setup, this.detectives, realPlayer, realPlayer.location()));
                    moveSet.addAll(makeDoubleMoves(this.setup, this.detectives, realPlayer, realPlayer.location()));
                }


                this.moves = ImmutableSet.copyOf(moveSet);
            } else {
                this.moves = ImmutableSet.of();
            }
        }

        // Check if MrX has stuck -> detectives won
        public void mrXStuckCheck() {
            if (this.winner.isEmpty() && (this.remaining.contains(mrX.piece()))) {
                boolean mrXStuck = true;
                for (Move current : this.moves) {
                    if (current.commencedBy() == mrX.piece()) {
                        mrXStuck = false;
                    }
                }

                if (mrXStuck) {
                    this.winner = ImmutableSet.copyOf(this.detectives.stream().map(Player::piece).collect(Collectors.toSet()));
                    this.moves = ImmutableSet.of();
                }
            }
        }

        public void allMovesUsedCheck() {
            // Check if all moves have been used -> MrX won
            if (this.winner.isEmpty() && (setup.moves.size() == log.size())) {
                boolean remainingContainsDetectives = false;

                for (Player currentPlayer : this.detectives) {
                    if (this.remaining.contains(currentPlayer.piece())) {
                        remainingContainsDetectives = true;
                        break;
                    }
                }
                if (! remainingContainsDetectives) {
                    this.winner = ImmutableSet.of(mrX.piece());
                    this.moves = ImmutableSet.of();
                }
            }
        }

        // Check if detectives stuck -> mrX won
        private void detectivesStuckCheck() {
            if (this.winner.isEmpty() && !this.remaining.contains(mrX.piece()) && remaining.size() == detectives.size()) {
                if (moves.isEmpty()) {
                    this.winner = ImmutableSet.of(mrX.piece());
                    this.moves = ImmutableSet.of();
                }
            }
        }

        private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives) {

            // Check if any argument is null, throw an exception if so
            if (setup != null || remaining != null || log != null || mrX != null || detectives != null) {
                this.setup = setup;
                this.remaining = remaining;
                this.log = log;
                this.mrX = mrX;
                this.detectives = detectives;
                this.winner = ImmutableSet.of();    // winner is empty at start
            }
            else
                throw new IllegalArgumentException("Invalid arguments!");

            // Check that all detectives are in different locations and there are no duplicate pieces
            detectivesAreInDifferentLocationsAndNoDuplicatePiecesCheck();

            // Check if MrX is indeed the black piece
            MrXIsTheBlackPieceCheck();

            // Check if all detectives are indeed holding detective pieces
            allDetectivesHoldDetectivePiecesCheck();

            // Check that no detectives have a secret or double ticket
            noDetectiveHasSecretOrDoubleTicketCheck();

            // check if the moves is empty, if yes, the game state of move is not valid, therefore throw illegal Exception
            if (setup.moves.isEmpty()) {throw new IllegalArgumentException("No available moves!");}
            if (setup.graph.nodes().isEmpty()) {throw new IllegalArgumentException("No available moves!");}
            if (setup.graph.edges().isEmpty()) {throw new IllegalArgumentException("No available moves!");}

            allDetectivesRanOutOfTicketsCheck();

            mrXRanOutOfTicketsCheck();

            mrXCaughtCheck();

            makeMoves();

            mrXStuckCheck();

            detectivesStuckCheck();

            allMovesUsedCheck();
        }

        /**
         * @return the current game setup
         */
        @Override @Nonnull public GameSetup getSetup () {
            return setup;
        }

        /**
         * @return all players in the game
         */
        @Override @Nonnull public ImmutableSet<Piece> getPlayers () {
            List<Piece> allPlayers = detectives.stream().map(Player::piece).collect(Collectors.toList());
            allPlayers.add(mrX.piece());
            return ImmutableSet.copyOf(allPlayers);
        }

        /**
         * @param detective the detective
         * @return the location of the given detective; empty if the detective is not part of the game
         */
        @Override @Nonnull public Optional<Integer> getDetectiveLocation (Detective detective){
            if (detectives.stream().anyMatch(x -> x.piece() == detective)) {
                return Optional.of(detectives.stream().filter(x -> x.piece() == detective).findFirst().get().location());
            } else return Optional.empty();
        }

        // Implementation of the TicketBoard interface for getPlayerTickets()
        public static class TBoard implements TicketBoard {
            private final ImmutableMap<Ticket, Integer> map;
            public TBoard(int taxi, int bus, int underground, int doubleTicket, int secret) {
                this.map = ImmutableMap.of(
                        Ticket.TAXI, taxi,
                        Ticket.BUS, bus,
                        Ticket.UNDERGROUND, underground,
                        Ticket.DOUBLE, doubleTicket,
                        Ticket.SECRET, secret
                        );
            }
            //Get the number of tickets
            public int getCount(@Nonnull Ticket ticket) {
                return this.map.get(ticket);
            }
        }

        /**
         * @param piece the player piece
         * @return the ticket board of the given player; empty if the player is not part of the game
         */
        @Override @Nonnull public Optional<TicketBoard> getPlayerTickets (Piece piece){
            List<Player> allPlayers = new ArrayList<>(detectives);
            allPlayers.add(mrX);
            if (allPlayers.stream().anyMatch(x -> x.piece() == piece)) {
                Player current = allPlayers.stream().filter(x -> x.piece() == piece).findFirst().get();
                return Optional.of(new TBoard(
                        current.tickets().get(Ticket.TAXI),
                        current.tickets().get(Ticket.BUS),
                        current.tickets().get(Ticket.UNDERGROUND),
                        current.tickets().get(Ticket.DOUBLE),
                        current.tickets().get(Ticket.SECRET)) );
            } else return Optional.empty();
        }

        /**
         * @return MrX's travel log as a list of {@link LogEntry}s.
         */
        @Override @Nonnull public ImmutableList<LogEntry> getMrXTravelLog (){return log;}


        /**
         * @return the winner of this game; empty if the game has no winners yet
         * This is mutually exclusive with {@link #getAvailableMoves()}
         */
        @Override @Nonnull public ImmutableSet<Piece> getWinner () {return winner;}

        /**
         * @return the current available moves of the game.
         * This is mutually exclusive with {@link #getWinner()}
         */

        @Override @Nonnull public ImmutableSet<Move> getAvailableMoves () {return moves;}


        @Override @Nonnull public GameState advance (Move move){

            if (!moves.contains(move))  throw new IllegalArgumentException("Illegal move: " + move);
            ArrayList<Piece> newRemaining = new ArrayList<>(remaining);

            return move.accept(new Visitor<>() {

                public GameState visit(Move.SingleMove move) {
                    List<LogEntry> newLog = new ArrayList<>(log);
                    List<Player> newDetectives = new ArrayList<>(detectives);
                    if (mrX.piece() == move.commencedBy()) {
                        mrX = mrX.use(move.tickets());
                        mrX = mrX.at(move.destination);

                        LogEntry newLogEntry;
                        if (setup.moves.get(log.size())) {
                            newLogEntry = LogEntry.reveal(move.ticket, move.destination);
                        } else {
                            newLogEntry = LogEntry.hidden(move.ticket);
                        }

                        newLog.add(newLogEntry);

                        if (! newRemaining.isEmpty()) {
                            newRemaining.remove(mrX.piece());
                        }
                        for (Player current : detectives) {
                            newRemaining.add(current.piece());
                        }

                    } else {

                        for (Player current : detectives) {
                            if (current.piece() == move.commencedBy()) {
                                try {
                                    Player newCurrent = current.use(move.tickets());
                                    newCurrent = newCurrent.at(move.destination);
                                    mrX = mrX.give(move.tickets());

                                    // Rewrite list of detectives
                                    newRemaining.remove(current.piece());

                                    newDetectives.remove(current);
                                    newDetectives.add(newCurrent);

                                    // If no detective moves are possible anymore, swap to MrX's turn
                                    if (newRemaining.isEmpty()) {
                                        newRemaining.add(mrX.piece());
                                    } else {
                                        MyGameState newGameState = new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, mrX, detectives);
                                        // If no moves are left, return a game state with mrX in remaining.
                                        if(newGameState.moves.isEmpty()){
                                            List<Piece> newRemaining = new ArrayList<>();
                                            newRemaining.add(mrX.piece());
                                            return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, mrX, newDetectives);
                                        }
                                    }

                                } catch (IllegalArgumentException e) {
                                    newRemaining.remove(current.piece());

                                }
                            }
                        }
                    }

                    // If there are no more possible detective moves, swap to MrX's turn
                    if (newRemaining.isEmpty()) {
                        newRemaining.add(mrX.piece());
                    }

                    ImmutableList<LogEntry> logAfterMove = ImmutableList.copyOf(newLog);
                    return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), logAfterMove, mrX, newDetectives);
                }

                public GameState visit(Move.DoubleMove move) {
                    GameSetup newSetup = setup;
                    List<LogEntry> newLog = new ArrayList<>(log);
                    LogEntry newLogEntry;
                    LogEntry newLogEntry2;

                    if (mrX.piece() == move.commencedBy()) {
                        mrX = mrX.use(move.tickets());

                        mrX = mrX.at(move.destination1);
                        if (log.isEmpty()) {
                            newLogEntry = LogEntry.hidden(move.ticket1);
                        } else if (setup.moves.get(log.size())) {
                            newLogEntry = LogEntry.reveal(move.ticket1, mrX.location());
                        } else {
                            newLogEntry = LogEntry.hidden(move.ticket1);
                        }

                        mrX = mrX.at(move.destination2);
                        if (setup.moves.get(log.size() + 1)) {
                            newLogEntry2 = LogEntry.reveal(move.ticket2, mrX.location());
                        } else {
                            newLogEntry2 = LogEntry.hidden(move.ticket2);
                        }


                        newLog.add(newLogEntry);
                        newLog.add(newLogEntry2);

                        newRemaining.remove(mrX.piece());
                        for (Player current : detectives) {
                            newRemaining.add(current.piece());
                        }

                        ImmutableList<LogEntry> logAfterMove = ImmutableList.copyOf(newLog);

                        return new MyGameState(newSetup, ImmutableSet.copyOf(newRemaining), logAfterMove, mrX, detectives);

                    } else {
                        throw new IllegalArgumentException("Only mrX can use double tickets!");
                    }

                }
            });
        }


        private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
            boolean occupied = false;
            // TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
            Set<SingleMove> singleMoves = new HashSet<>();

            for(int destination : setup.graph.adjacentNodes(source)) {
                // TODO find out if destination is occupied by a detective
                //  if the location is occupied, don't add to the collection of moves to return
                for (Player detective : detectives) {
                    if ((detective.location() == destination)) {
                        occupied = true;
                        break;
                    }
                }

                if (! occupied) {
                    for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                        // TODO find out if the player has the required tickets
                        // if it does, construct a SingleMove and add it the collection of moves to return
                        if (player.has(t.requiredTicket())) {
                            singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
                        }
                    }


                    // TODO consider the rules of secret moves here
                    //  add moves to the destination via a secret ticket if there are any left with the player
                    if (player.has(Ticket.SECRET)) {
                        singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
                    }
                }
                occupied = false;
            }

            // TODO return the collection of moves
            return singleMoves;
        }

        private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            HashSet<DoubleMove> doubleMoves = new HashSet<>();

            if (setup.moves.size() < 2){
                return doubleMoves;
            } else {

                if (player.isDetective()) {
                    return Set.of();
                }
                if (player.has(Ticket.DOUBLE)) {
                    player = player.use(Ticket.DOUBLE);
                } else return Set.of();

                Player tempPlayer = player;

                // TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
                HashSet<SingleMove> firstMoves = new HashSet<>(makeSingleMoves(setup, detectives, tempPlayer, source));
                HashSet<SingleMove> secondMoves = new HashSet<>();

                for (SingleMove firstMove : firstMoves) {
                    tempPlayer = player.use(firstMove.ticket);
                    tempPlayer = tempPlayer.at(firstMove.destination);
                    secondMoves.addAll(makeSingleMoves(setup, detectives, tempPlayer, firstMove.destination));
                    for (SingleMove secondMove : secondMoves) {
                        doubleMoves.add(new DoubleMove(player.give(Ticket.DOUBLE).piece(), firstMove.source(), firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination));
                    }
                    secondMoves = new HashSet<>();
                }
                return doubleMoves;
            }
        }
    }

    @Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);
    }
}


