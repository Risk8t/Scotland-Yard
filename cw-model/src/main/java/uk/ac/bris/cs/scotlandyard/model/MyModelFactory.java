package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private Board.GameState gameState;

	@Nonnull
	@Override
	public Model build(GameSetup setup,
					   Player mrX,
					   ImmutableList<Player> detectives) {

		gameState = new MyGameStateFactory().build(setup, mrX, detectives);
		List<Model.Observer> observers = new ArrayList<>();

		return new Model(){


			@Override
			@Nonnull
			public Board getCurrentBoard(){
				return gameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer){
				if (observer == null) {
					throw new NullPointerException();
				}

				if (! observers.contains(observer)) {
					observers.add(observer);
				} else throw new IllegalArgumentException();}

			@Override
			public void unregisterObserver(@Nonnull Observer observer){
				if (observer == null) {
					throw new NullPointerException();
				}

				if (observers.contains(observer)) {
					observers.remove(observer);
				} else throw new IllegalArgumentException();}

			@Override
			@Nonnull
			public ImmutableSet<Observer> getObservers(){
				return ImmutableSet.copyOf(observers);
			}

			@Override
			public void chooseMove(@Nonnull Move move){
				ImmutableSet<Piece> winners = gameState.getWinner();
				gameState = gameState.advance(move);

				if (! gameState.getWinner().isEmpty()) {
					notifyAllObservers(Observer.Event.GAME_OVER);
				} else if (winners.isEmpty()) {
					notifyAllObservers(Observer.Event.MOVE_MADE);
				}
			}

			private void notifyAllObservers(Observer.Event event) {
				for (Observer observer : observers) {
					observer.onModelChanged(gameState, event);
				}
			}
		};
	}
}
