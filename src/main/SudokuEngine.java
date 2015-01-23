package main;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections4.queue.*;

import com.sun.org.apache.bcel.internal.generic.SWAP;
import com.sun.org.apache.xml.internal.security.utils.HelperNodeList;

import exception.WrongSudokuNumberException;
import exception.WrongSudokuSizeException;

public class SudokuEngine {

	private SudokuBoard currentState;
	private SudokuBoard currentBest;
	boolean isUsedAspirationCriterion;
	private int size(){
		return 9;
	}
	private int boxSize(){
		return 3;
	}
	
	private Queue<Movement> shortTermTabuList;
	
	private Set<Coordinates> blockedPositions = new HashSet<Coordinates>();
	
	public SudokuEngine(int [][] initialBoard, int shortTermTabuListSize) throws WrongSudokuSizeException, WrongSudokuNumberException{
		
		if (!Helpers.hasProperSize(initialBoard))
			throw new WrongSudokuSizeException("In SudokuEngine constructor");
		if (!Helpers.hasProperNumbers(initialBoard))
			throw new WrongSudokuNumberException("In SudokuEngine constructor");
		
		shortTermTabuList = new CircularFifoQueue<Movement>(shortTermTabuListSize);
		currentState = new SudokuBoard(initialBoard);
		for (int i = 0; i < size(); i++) {
			for (int j = 0; j < size(); j++) {
				if(initialBoard[i][j] != 0)
					blockedPositions.add(new Coordinates(i,j));
			}
		}
	}
	
	public int [][] getCurrentState(){
		return currentState.getBoard();
	}
	
	public Set<Coordinates> getBlockedPositions(){
		return blockedPositions;
	}
	
	private void fillBox(int x, int y){
		
		Set<Integer> numbersInBox = new HashSet<Integer>();
		List<Coordinates> positionsToFill = new ArrayList<Coordinates>();
		
		for (int i = x * boxSize(); i < (x+1) * boxSize(); i++) {
			for (int j = y * boxSize(); j < (y+1) * boxSize(); j++) {
				int number = currentState.getBoard()[i][j];
				if(number != 0){
					numbersInBox.add(number);
				}
				else
					positionsToFill.add(new Coordinates(i, j));
			}
		}
		
		Set<Integer> allNumbers = new HashSet<Integer>(Arrays.asList(Helpers.getNumbers(1, 9)));
		allNumbers.removeAll(numbersInBox);
		
		Integer [] numbersToInsert = allNumbers.toArray(new Integer[allNumbers.size()]);
		
		for (int i = 0; i < numbersToInsert.length; i++) {
			currentState.insert(positionsToFill.get(i), numbersToInsert[i]);
		}
	}
	
	// filling with random numbers
	// but preserving numbers from initial board
	// and making sure there are no conflicts in boxes
	public void fillBoard(){
		for (int i = 0; i < boxSize(); i++) {
			for (int j = 0; j < boxSize(); j++) {
				fillBox(i,j);
			}
		}
	}
	
	// checking how many positions are conflicting
	// we don't check boxes assuming that boxes don't contain conflicts
	public int getCurrentConflictsNumber(){
		return Helpers.getCurrentConflictsNumber(getCurrentState());
	}
	
	public List<Tuple<Movement,Integer>> generateNeighborhood(){
		List<Movement> movements =  generateNotBlockedMovements();
		List<Tuple<Movement,Integer>> result = new LinkedList<Tuple<Movement,Integer>>();
		
		for (Movement movement : movements) {
			SudokuBoard current = new SudokuBoard(currentState.getBoard());
			current.swapNumbersByCoordinates(movement.from, movement.to);
			Integer F = Helpers.getCurrentConflictsNumber(current.getBoard());
			result.add(new Tuple<Movement, Integer>(movement, F));
			current.swapNumbersByCoordinates(movement.to, movement.from);
		}
		 
		return result;
	}

	public List<Movement> generateNotBlockedMovements(){
		List<Movement> movements = new LinkedList<Movement>(); 
		for (int i = 0; i < boxSize(); i++) {
			for (int j = 0; j < boxSize(); j++) {
				movements.addAll(Helpers.generateAllPossibleMovements(i, j));
			}
		}
		
		
		Iterator<Movement> iter = movements.iterator();
		while (iter.hasNext()) {
			Movement theMovement = iter.next();
			if (blockedPositions.contains(theMovement.from) || blockedPositions.contains(theMovement.to))
			{
			  iter.remove();
			}
		}
		
		return movements;
	}
	
	public void runTabuSearch() {
		fillBoard();
		currentBest = new SudokuBoard(currentState.getBoard());
		
		while(getCurrentConflictsNumber() != 0) {
			List<Tuple<Movement,Integer>> neighborhood = generateNeighborhood();
	
			Collections.sort(neighborhood, new Comparator<Tuple<Movement,Integer>>() {
				public int compare(Tuple<Movement,Integer> t1, Tuple<Movement,Integer> t2) {
					return t1.y.compareTo(t2.y);
				}
			});
			for (int i = 0; i < neighborhood.size(); i++) {
				Movement movement = neighborhood.get(i).x;
				
				if (!shortTermTabuList.contains(movement) ||
						(isUsedAspirationCriterion &&
								neighborhood.get(i).y < Helpers.getCurrentConflictsNumber(currentBest.getBoard()))) {
					currentState.swapNumbersByCoordinates(movement.from, movement.to);
					shortTermTabuList.add(movement);
					break;
				}				
			}

			if (getCurrentConflictsNumber() < Helpers.getCurrentConflictsNumber(currentBest.getBoard())) {
				currentBest = new SudokuBoard(currentState.getBoard());
			}
		}
	}
	
}
