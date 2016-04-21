package hw2;

import java.util.*;

public class ThreadLocal {
	private static final int MAX_VAL = 100;
	private int N,M;
	private int[][] matrix;
	private int[] maxValues;
	private Collection<Thread> threads;

	public ThreadLocal(int N, int M) {
		this.N = N;
		this.M = M;
		this.threads = new ArrayList<>();
		populateMatrix();
		this.maxValues = new int[N];
		printMatrix();
	}

	public void populateMatrix(){
		this.matrix = new int[N][M];   // [[],[]]
		for (int i=0; i<N; i++) {
			for (int j=0; j<M; j++) {
				this.matrix[i][j] = (int)(Math.random()*MAX_VAL);
			}
		}
	}

	private void printMatrix(){
		for(int i=0; i<N; i++){
			System.out.println(Arrays.toString(this.matrix[i]));
		}
	}

	public int findMax() throws InterruptedException {
		for(int i=0; i<N; i++){
			MaxFinderT mft = new MaxFinderT(i);
			Thread t = new Thread(mft);
			this.threads.add(t);
			t.start();
		}
		for (Thread t : this.threads){
			t.join();
		}
		int max = maxValues[0];
		for (int i=0; i<maxValues.length; i++){
			if (maxValues[i] > max) max = maxValues[i];
		}
		System.out.println("RowMaxVals: " + Arrays.toString(this.maxValues));
		return max;
	}

	class MaxFinderT implements Runnable {
		private int row;

		public MaxFinderT(int row){
			this.row=row;
		}

		@Override
		public void run(){
			System.out.println("Row " + this.row  + " running...");
			int max = matrix[row][0];
			for (int i=0; i<matrix[row].length; i++){
				if (matrix[row][i] > max) max=matrix[row][i];
			}
			maxValues[row]=max;
		}


	}

	public static void main(String[] args){
		ThreadLocal tl = new ThreadLocal(5,4);
		try {
			int max = tl.findMax();
			System.out.println("Max: " + max);
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}
}