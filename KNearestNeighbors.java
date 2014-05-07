
import java.util.*;
import java.io.IOException;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.*;

public class KNearestNeighbors {

	// Just look at 200 images for now
	public static int numberOfImagesToDebugWith;
	// Tracks the number of images processed in the testing set.
	public static double countOfImagesAnalyzed = 0;
	// Tracks the number of images correctly identified in the testing set.
	public static double countOfCorrectImagesAnalyzed = 0;
	// Tracks running time of the hidden layer construction
	public static long executionTime;
	// The number of input nodes will be equal to the number of pixels in the image
	public static int numberOfInputNodes;
	// Create array of Nodes in first layer and associate done that points to the correct output
	public static ArrayList<ArrayList<Double>> hiddenLayerNodes = new ArrayList<ArrayList<Double>>();
	public static ArrayList<Integer> hiddenLayerToOutput = new ArrayList<Integer>();
	public static ArrayList<Double> hiddenLayerDottedOutputValues = new ArrayList<Double>();
	public static ArrayList<Double> hiddenLayerDottedOutputValues2 = new ArrayList<Double>();
	public static ArrayList<Double> hiddenLayerDottedOutputValues3 = new ArrayList<Double>();
	public static ArrayList<Double> hiddenLayerDottedOutputValues4 = new ArrayList<Double>();


	// Tracks the number of images processed in the testing set.
	public static double countOfImagesAnalyzed2 = 0;
	// Tracks the number of images correctly identified in the testing set.
	public static double countOfCorrectImagesAnalyzed2 = 0;

	// Tracks the number of images processed in the testing set.
	public static double countOfImagesAnalyzed3 = 0;
	// Tracks the number of images correctly identified in the testing set.
	public static double countOfCorrectImagesAnalyzed3 = 0;


	// Tracks the number of images processed in the testing set.
	public static double countOfImagesAnalyzed4 = 0;
	// Tracks the number of images correctly identified in the testing set.
	public static double countOfCorrectImagesAnalyzed4 = 0;


	public static ArrayList<DigitImage> testingData = new ArrayList<DigitImage>();

	//How many nearest Neighbors to use
	public static int k;
	// Is true if the input into the network consists of binary images. False if Grayscale.
	public static boolean binaryInput;
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// usePriorWeights=Boolean.parseBolean(args[4]);
		// String trainingImages=args[7];
		// String testingImages=args[8];
		// String trainingLabels=args[9];
		// String testingLabels=args[10];
		// int k = Integer.parseInt(args[11]); 

		// These are hard coded versions of the above
		String trainingImages = "Training-Images";
		String testingImages = "Testing-images";
		String trainingLabels = "Training-Labels";
		String testingLabels = "Testing-Labels";
		k = 3;
		binaryInput=false;
		numberOfImagesToDebugWith = 200;
		// Trains the network
		initializeKNearestNeighbours(trainingImages, trainingLabels);

		long startTime = System.currentTimeMillis();

		// Loads test data for the K-Nearest Neighbors Network
		testKNearestNeighbours(testingImages, testingLabels);

		Runnable r1 = new Runnable() {
			public void run() {
				//Tests the first quarter of the input data
				solveTestingData(testingData, k);
			}
		};
		Runnable r2 = new Runnable() {
			public void run() {
				//Tests the second fourth of the input data
				solveTestingData2(testingData, k);
			}
		};
		Runnable r3 = new Runnable() {
			public void run() {
				//Tests the third fourth of the input data
				solveTestingData3(testingData, k);
			}
		};
		Runnable r4 = new Runnable() {
			public void run() {
				//Tests the last fourth of the input data
				solveTestingData4(testingData, k);
			}
		};

		Thread thr1 = new Thread(r1);
		Thread thr2 = new Thread(r2);
		Thread thr3 = new Thread(r3);
		Thread thr4 = new Thread(r4);
		thr1.start();
		thr2.start();
		thr3.start();
		thr4.start();
		try {
			thr1.join();
			thr2.join();
			thr3.join();
			thr4.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		double percentCorrect = ((countOfCorrectImagesAnalyzed+countOfCorrectImagesAnalyzed2+countOfCorrectImagesAnalyzed3+countOfCorrectImagesAnalyzed4) / (countOfImagesAnalyzed+countOfImagesAnalyzed2+countOfImagesAnalyzed3+countOfImagesAnalyzed4)) * 100;
		System.out.println("Analyzed " + (countOfImagesAnalyzed+countOfImagesAnalyzed2+countOfImagesAnalyzed3+countOfImagesAnalyzed4) + " images with " + percentCorrect + " percent accuracy.");
		System.out.println("Solution time: " + executionTime + " milliseconds");
		System.out.println("# Correct: " + (countOfCorrectImagesAnalyzed+countOfCorrectImagesAnalyzed2+countOfCorrectImagesAnalyzed3+countOfCorrectImagesAnalyzed4));


	}

	public static void initializeKNearestNeighbours(String trainingImages, String trainingLabels) throws IOException {

		// Loads training and testing data sets
		DigitImageLoadingService train = new DigitImageLoadingService(trainingLabels, trainingImages,binaryInput);
		ArrayList<DigitImage> trainingData = new ArrayList<DigitImage>();
		try {
			// Our data structure holds the training data
			trainingData = train.loadDigitImages();
			// Alters data into proper form
			for (int i = 0; i < trainingData.size(); i++) {
				trainingData.get(i).vectorizeTrainingData();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Looks at a representation of an image
		// and determines how many pixels and thus how many input nodes are needed
		// (one per pixel)
		numberOfInputNodes = trainingData.get(0).getData().length;

		long startTime = System.currentTimeMillis();
		// Initialize weights with values corresponding to the binary pixel value for all nodes in the first hidden layer.
		// Currently dividing by 2 to only use a half of the training set so we don't run out of memory. We likely don't need that many anyway.
		for (int i = 0; i < trainingData.size()/3; i++) {
			ArrayList<Double> weights = new ArrayList<Double>(numberOfInputNodes);
			weights = trainingData.get(i).getArrayListData();
			hiddenLayerNodes.add(weights);
			hiddenLayerToOutput.add((int) trainingData.get(i).getLabel());
		}

		long endTime = System.currentTimeMillis();
		executionTime = endTime - startTime;
		System.out.println("Training time: " + executionTime + " milliseconds");

	}

	public static void testKNearestNeighbours(String testingImages, String testingLabels) throws IOException {

		// Loads testing data set
		DigitImageLoadingService test = new DigitImageLoadingService(testingLabels, testingImages,binaryInput);
		testingData = new ArrayList<DigitImage>();
		try {
			// Our data structure holds the testing data
			testingData = test.loadDigitImages();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/*
	 * Returns the output from a given node after the input has been summed.It takes the layer that the node is in, the index of the node in the
	 * layer, and the output from the previous layer
	 */
	public static double nodeOutput(ArrayList<ArrayList<Double>> layerOfNodes, ArrayList<Double> outputFromPreviousLayer, int indexOfNodeinlayer) {
		double sum = 0;
		for (int i = 0; i < outputFromPreviousLayer.size(); i++) {
			double output= Math.abs((layerOfNodes.get(indexOfNodeinlayer).get(i) - outputFromPreviousLayer.get(i)));
			if(output<=20){
				output=1;
			}else{
				output=0;
			}
			sum = sum + output ;
		}
		return sum;
	}

	/* This returns an array representing the output of all nodes in the given layer */
	public static ArrayList<Double> outPutOfLayer(ArrayList<ArrayList<Double>> currentLayer, ArrayList<Double> outputFromPreviousLayer) {
		ArrayList<Double> outputOfCurrentlayer = new ArrayList<Double>();
		for (int i = 0; i < currentLayer.size(); i++) {
			double output;
			if(!binaryInput){
				output = nodeOutput(currentLayer, outputFromPreviousLayer, i);
			}else{
				output = nodeOutputBinary(currentLayer, outputFromPreviousLayer, i);
			}
			outputOfCurrentlayer.add(output);
		}
		return outputOfCurrentlayer;
	}

	public static void solveTestingData(ArrayList<DigitImage> networkInputData, int k) {
		//	long startTime = System.currentTimeMillis();
		for (int i = 0; i <= (numberOfImagesToDebugWith/4)-1; i++) {
			ArrayList<Double> temp = networkInputData.get(i).getArrayListData();
			hiddenLayerDottedOutputValues = outPutOfLayer(hiddenLayerNodes, temp);


			//I IF K=1 just run the commented out code as it is faster.	
			double output = 0;
			if(k==1){	


				//Find which node has the maximum output and then
				//return the number that is at that node position in the associated output array.

				double currentMax = 0;
				for (int j = 0; j < hiddenLayerDottedOutputValues.size(); j++) {
					if (hiddenLayerDottedOutputValues.get(j) > currentMax) {
						currentMax = hiddenLayerDottedOutputValues.get(j);
						output = hiddenLayerToOutput.get(j);
					}
				}
			}
			else{

				int[] indicesOfDottedOutputList = new int[hiddenLayerDottedOutputValues.size()];
				ArrayList<Integer> bestKOutputs = new ArrayList<Integer>();


				initializeIndices(indicesOfDottedOutputList);
				parallelSorting(indicesOfDottedOutputList, hiddenLayerDottedOutputValues);
				findBestKOutputs(indicesOfDottedOutputList, hiddenLayerToOutput, bestKOutputs, k);
				output = findMostCommonOccurrenceAmongKOutputs(bestKOutputs);
			}
			System.out.println("Guess using the closest match: " + output);
			double number = networkInputData.get(i).getLabel();
			System.out.println("Correct answer1: " + number);

			countOfImagesAnalyzed++;
			if (number == output) {
				countOfCorrectImagesAnalyzed++;
				System.out.println("Network was Correct");
			} else {
				System.out.println(" Network was Wrong");
			}
			System.out.println(" ");
		}

	}

	// Initialize the ordered indicies for the hiddenLayerDottedOuput list
	public static void initializeIndices (int[] indicesArray) {
		for (int index = 0; index < indicesArray.length; index++) {
			indicesArray[index] = index;
		}
	}

	public static void parallelSorting(int[] indicesToBeSorted, ArrayList<Double> listToBeSorted) {
		for (int i = 0; i < listToBeSorted.size(); i++) {
			for (int j = i + 1; j < listToBeSorted.size(); j++) {
				// Swap so that bigger numbers go in the front.
				if (listToBeSorted.get(j) > listToBeSorted.get(i)) {
					Double temp = new Double(listToBeSorted.get(i));
					listToBeSorted.set(i, listToBeSorted.get(j));
					listToBeSorted.set(j, temp);
					int tempIndex = i;
					indicesToBeSorted[i] = j; 
					indicesToBeSorted[j] = tempIndex; 
				}
			}
		}
	}

	// The bestKOutputsList is constructed from the sorted hiddenLaYerDottedOutput lists's indices and the 
	// values of hiddenLayerToOutput list at the corresponding indices. 	
	public static void findBestKOutputs(int[] sortedIndices, ArrayList<Integer> outputsList, ArrayList<Integer> bestKOutputsList, int k) {
		for (int i = 0; i < k; i++) {
			bestKOutputsList.add(outputsList.get(sortedIndices[i]));
		}
	}

	// This method finds the most commonly occurred output among the best K outputs.
	public static int findMostCommonOccurrenceAmongKOutputs (ArrayList<Integer> bestKOutputsList) {
		//This is simpler:
		int[]  holder=new int[10];
		for (int m = 0; m < holder.length; m++) {
			holder[m]=0;
		}
		for (int m = 0; m < bestKOutputsList.size(); m++) {
			holder[bestKOutputsList.get(m)]++;	
		}
		int mostCommonValue=0;
		int max=0;
		for (int m = 0; m < holder.length; m++) {
			if(holder[m]>max){
				max=holder[m];
				mostCommonValue=m;
			}
		}
		return mostCommonValue;


	}


	public static void solveTestingData2(ArrayList<DigitImage> networkInputData, int k) {


		//long startTime = System.currentTimeMillis();
		for (int i = (numberOfImagesToDebugWith/2) -1; i >=numberOfImagesToDebugWith/4 ; i--) {
			ArrayList<Double> temp = networkInputData.get(i).getArrayListData();
			hiddenLayerDottedOutputValues2 = outPutOfLayer(hiddenLayerNodes, temp);


			//I IF K=1 just run the commented out code as it is faster.	
			double output = 0;
			if(k==1){	


				//Find which node has the maximum output and then
				//return the number that is at that node position in the associated output array.

				double currentMax = 0;
				for (int j = 0; j < hiddenLayerDottedOutputValues.size(); j++) {
					if (hiddenLayerDottedOutputValues.get(j) > currentMax) {
						currentMax = hiddenLayerDottedOutputValues.get(j);
						output = hiddenLayerToOutput.get(j);
					}
				}
			}
			else{

				int[] indicesOfDottedOutputList = new int[hiddenLayerDottedOutputValues2.size()];
				ArrayList<Integer> bestKOutputs = new ArrayList<Integer>();


				initializeIndices(indicesOfDottedOutputList);
				parallelSorting(indicesOfDottedOutputList, hiddenLayerDottedOutputValues2);
				findBestKOutputs(indicesOfDottedOutputList, hiddenLayerToOutput, bestKOutputs, k);
				output = findMostCommonOccurrenceAmongKOutputs(bestKOutputs);

				System.out.println("Guess using the closest match: " + output);
			}
			double number = networkInputData.get(i).getLabel();
			System.out.println("Correct answer2: " + number);


			countOfImagesAnalyzed2++;
			if (number == output) {
				countOfCorrectImagesAnalyzed2++;
				System.out.println("Network was Correct");
			} else {
				System.out.println(" Network was Wrong");
			}
			System.out.println(" ");
		}
	}



	public static void solveTestingData3(ArrayList<DigitImage> networkInputData, int k) {
		//long startTime = System.currentTimeMillis();
		for (int i =( (numberOfImagesToDebugWith*3)/4)-1; i >=numberOfImagesToDebugWith/2 ; i--) {
			ArrayList<Double> temp = networkInputData.get((int)i).getArrayListData();
			hiddenLayerDottedOutputValues3 = outPutOfLayer(hiddenLayerNodes, temp);


			//I IF K=1 just run the commented out code as it is faster.	
			double output = 0;
			if(k==1){	


				//Find which node has the maximum output and then
				//return the number that is at that node position in the associated output array.

				double currentMax = 0;
				for (int j = 0; j < hiddenLayerDottedOutputValues.size(); j++) {
					if (hiddenLayerDottedOutputValues.get(j) > currentMax) {
						currentMax = hiddenLayerDottedOutputValues.get(j);
						output = hiddenLayerToOutput.get(j);
					}
				}
			}
			else{

				int[] indicesOfDottedOutputList = new int[hiddenLayerDottedOutputValues3.size()];
				ArrayList<Integer> bestKOutputs = new ArrayList<Integer>();


				initializeIndices(indicesOfDottedOutputList);
				parallelSorting(indicesOfDottedOutputList, hiddenLayerDottedOutputValues3);
				findBestKOutputs(indicesOfDottedOutputList, hiddenLayerToOutput, bestKOutputs, k);
				output = findMostCommonOccurrenceAmongKOutputs(bestKOutputs);
			}
			System.out.println("Guess using the closest match: " + output);
			double number = networkInputData.get((int)i).getLabel();
			System.out.println("Correct answer3: " + number);

			countOfImagesAnalyzed3++;
			if (number == output) {
				countOfCorrectImagesAnalyzed3++;
				System.out.println("Network was Correct");
			} else {
				System.out.println(" Network was Wrong");
			}
			System.out.println(" ");
		}
	}




	public static void solveTestingData4(ArrayList<DigitImage> networkInputData, int k) {

		//long startTime = System.currentTimeMillis();
		for (int i = numberOfImagesToDebugWith; i >= (numberOfImagesToDebugWith*3)/4 ; i--) {
			ArrayList<Double> temp = networkInputData.get(i).getArrayListData();
			hiddenLayerDottedOutputValues4 = outPutOfLayer(hiddenLayerNodes, temp);


			//I IF K=1 just run the commented out code as it is faster.	
			double output = 0;
			if(k==1){	


				//Find which node has the maximum output and then
				//return the number that is at that node position in the associated output array.

				double currentMax = 0;
				for (int j = 0; j < hiddenLayerDottedOutputValues.size(); j++) {
					if (hiddenLayerDottedOutputValues.get(j) > currentMax) {
						currentMax = hiddenLayerDottedOutputValues.get(j);
						output = hiddenLayerToOutput.get(j);
					}
				}
			}
			else{

				int[] indicesOfDottedOutputList = new int[hiddenLayerDottedOutputValues4.size()];
				ArrayList<Integer> bestKOutputs = new ArrayList<Integer>();


				initializeIndices(indicesOfDottedOutputList);
				parallelSorting(indicesOfDottedOutputList, hiddenLayerDottedOutputValues4);
				findBestKOutputs(indicesOfDottedOutputList, hiddenLayerToOutput, bestKOutputs, k);
				output = findMostCommonOccurrenceAmongKOutputs(bestKOutputs);
			}
			System.out.println("Guess using the closest match: " + output);
			double number = networkInputData.get(i).getLabel();
			System.out.println("Correct answer4: " + number);

			countOfImagesAnalyzed4++;
			if (number == output) {
				countOfCorrectImagesAnalyzed4++;
				System.out.println("Network was Correct");
			} else {
				System.out.println(" Network was Wrong");
			}
			System.out.println(" ");
		}
	}

	/*
	 * Returns the output from a given node after the input has been summed.It takes the layer that the node is in, the index of the node in the
	 * layer, and the output from the previous layer
	 */
	public static double nodeOutputBinary(ArrayList<ArrayList<Double>> layerOfNodes, ArrayList<Double> outputFromPreviousLayer, int indexOfNodeinlayer) {
		double sum = 0;
		for (int i = 0; i < outputFromPreviousLayer.size(); i++) {
			//This searches for a match with any adjacent pixels
			//(increases likely hood to match binary images) (like the diffuse border on grayscale images)
			double blob=0;
			blob = blob + outputFromPreviousLayer.get(i);

			if(!(i<=30)){
				blob = blob + outputFromPreviousLayer.get(i-1);
				blob = blob + outputFromPreviousLayer.get(i-2);
				blob = blob + outputFromPreviousLayer.get(i-26);
				blob = blob + outputFromPreviousLayer.get(i-27);
				blob = blob + outputFromPreviousLayer.get(i-28);
				blob = blob + outputFromPreviousLayer.get(i-29);
				blob = blob + outputFromPreviousLayer.get(i-30);


			}
			if(!(i>=754)){
				blob = blob + outputFromPreviousLayer.get(i+1);
				blob = blob + outputFromPreviousLayer.get(i+2);
				blob = blob + outputFromPreviousLayer.get(i+26);
				blob = blob + outputFromPreviousLayer.get(i+27);
				blob = blob + outputFromPreviousLayer.get(i+28); 
				blob = blob + outputFromPreviousLayer.get(i+29);
				blob = blob + outputFromPreviousLayer.get(i+30);
			}


			if(blob>=1){
				blob=1;
			}
			double output= Math.abs((layerOfNodes.get(indexOfNodeinlayer).get(i) - blob));

			if(output==0){  
				output=1;
			}else{
				output=0;
			}

			sum = sum + output ;

		}
		return sum;
	}

}
