/*
 Hand Writing Recognition and Simple CAPTCHA Neural Network
  CS 3425 Final project
  Spring 2014
  Min "Ivy" Xing, Zackery Leman
  This network works by reading in an image and then selecting the number 0-9 that corresponds to the output node with greatest activation.
  It is a feed-forward neural network that uses back propagation.
 */

// NOTE: To run this we had to pass the argument " -Xmx800M"  to the java virtual machine


import java.util.*;
import java.io.IOException;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NeuralNet {
	//Tracks the number of images  processed in the testing set.
	public static double countOfImagesAnalyzed=0;
	//Tracks the number of images correctly identified in the testing set.
	public static double countOfCorrectImagesAnalyzed=0;
	//The number of times the network is trained with the training Data
	public static int epochs;
	//Creates a random number generator
	public static Random random = new Random();
	//The number of input nodes will be equal to the number of pixels in the image
	public static int numberOfInputNodes;
	//Number of hidden layers
	public static int numberOfhiddenLayers;
	//Number of hidden nodes in second layer (first hidden layer) (and currently all other hidden layers. (Need to make this flexible)
	public static int numberOfHiddenNodesInLayer2;
	//Number of output nodes (Currently the network depends on 10 output nodes)
	public static int numberOfOutputNodes;
	//Create array of Nodes in first layer and output layer
	public static ArrayList<ArrayList<Double>> hiddenLayerNodes = new ArrayList<ArrayList<Double>>();
	public static ArrayList<ArrayList<Double>> outputLayerNodes = new ArrayList<ArrayList<Double>>();
	//The learning rate for the network
	public static double learningRate;
	//This array holds all other hidden layers except the first one.
	public static ArrayList<ArrayList<ArrayList<Double>>> hiddenLayers = new ArrayList<ArrayList<ArrayList<Double>>>();
	//For a given training image this array is filled with the output for each layer and then reset for the next image.
	//Prevents duplicate calculations from being performed.
	public static ArrayList<ArrayList<Double>> tempOutput = new ArrayList<ArrayList<Double>>();

	public static void main (String[] args) {

		//numberOfOutputNodes=args[2];
		//numberOfHiddenNodesInLayer2=args[3];
		//epochs = Integer.parseInt(args[4]); //number of epochs to run
		//double learningRate = Double.parseDouble(args[5]); //learning rate
		//numberOfhiddenLayers=Integer.parseInt(args[6]);

		//These are hard coded versions of the above
		numberOfOutputNodes=10;
		numberOfHiddenNodesInLayer2=30;
		numberOfhiddenLayers=1;
		epochs = 30;
		learningRate = 0.3;

		//Initializes all nodes in all other hidden layer except the first hidden layer
		if(numberOfhiddenLayers>1){
			for(int x=1; x<numberOfhiddenLayers; x++){
				//Create array of nodes in a hidden layer
				ArrayList<ArrayList<Double>> hidden = new ArrayList<ArrayList<Double>>();
				//Creates one node at a time, adds to the hidden layer, and then adds the hidden layer to the parent array
				for (int i=0; i<numberOfHiddenNodesInLayer2; i++) { 
					//Create node by creating an array of weights on incoming edges to this node
					ArrayList<Double> weights = new ArrayList<Double>(numberOfHiddenNodesInLayer2); //number of nodes in previous hidden layer
					//Initialize all weights on paths pointing to this node to random values with mean 0 and variance of 1
					for (int j=0; j<numberOfHiddenNodesInLayer2; j++) { 
						weights.add(random.nextGaussian());
					} 
					//Adds node to the hidden layer
					hidden.add(weights); 
				}
				//Adds hidden layer to parent array
				hiddenLayers.add(hidden);
			}
		}



		//Loads training and testing data sets
		DigitImageLoadingService train = new DigitImageLoadingService("train-labels-idx1-ubyte","train-images-idx3-ubyte");
		DigitImageLoadingService test = new DigitImageLoadingService("t10k-labels-idx1-ubyte","t10k-images-idx3-ubyte");
		try {
			//Our data structure holds the testing data
			ArrayList<DigitImage> testingData= test.loadDigitImages();
			//Our data structure holds the training data
			ArrayList<DigitImage> trainingData= train.loadDigitImages();
			//Alters data into proper form
			for(int i=0; i<trainingData.size(); i++){
				trainingData.get(i).vectorizeTrainingData();
			}

			//Looks at a representation of an image
			//and determines how many pixels and thus how many input nodes are needed
			//(one per pixel)
			numberOfInputNodes=trainingData.get(0).getData().length;

			//Initialize weights  with random values for all nodes in the first hidden layer.
			for (int i=0; i<numberOfHiddenNodesInLayer2; i++) { 
				ArrayList<Double> weights = new ArrayList<Double>(numberOfInputNodes); 
				for (int j=0; j<numberOfInputNodes; j++) { 
					weights.add(random.nextGaussian());
				} 
				hiddenLayerNodes.add(weights); 
			}
			//Initialize weights with random values for all nodes in the output layer.
			for (int i=0; i<numberOfOutputNodes; i++) { 
				ArrayList<Double> weights = new ArrayList<Double>(numberOfHiddenNodesInLayer2); 
				for (int j=0; j<numberOfHiddenNodesInLayer2; j++) { 
					weights.add(random.nextGaussian());
				} 
				outputLayerNodes.add(weights); 
			}

			long startTime = System.currentTimeMillis();
			//Trains the network with the training Data
			trainTheNetwork(trainingData);
			long endTime = System.currentTimeMillis();
	    	long executionTime = endTime - startTime;
	    	System.out.println("Training time: " + executionTime + " milliseconds");
			//Tests the network with the testing Data
			ArrayList<OutputVector> result =solveTestingData(testingData);

			//reports network Performance
			double percentCorrect= (countOfCorrectImagesAnalyzed/countOfImagesAnalyzed)*100;
			System.out.println("Analyzed " + countOfImagesAnalyzed+ " images with " +percentCorrect+ " percent accuracy.");

			//Uncomment to write results to file
			write(result);

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/*Returns the output from a given node after the input has been summed and processed by the activation function
	 *It takes the layer that the node is in, the index of the node in the layer, and the output from the previous layer */
	public static double nodeOutput(ArrayList<ArrayList<Double>> layerOfNodes,  ArrayList<Double> outputFromPreviousLayer, int indexOfNodeinlayer) {
		double sum=0;
		for(int i=0;i<outputFromPreviousLayer.size();i++){
			sum=sum+(layerOfNodes.get(indexOfNodeinlayer).get(i) * outputFromPreviousLayer.get(i));
		}
		return activationFunction(sum);


	}

	/*Takes the weighted sum as the parameter and returns the output of the sigmoid activation function*/
	public static double activationFunction(double weightedSum) {
		double output = 1/(1+Math.exp(((-1)*weightedSum)));
		return output;
	}
	/*Returns the derivative of the output of the sigmoid activation function*/
	public static double sigmoidPrime(double input) {
		double temp = 1/(1+Math.exp(((-1)*input)));
		double output=(temp*(1-temp));
		return output;
	}
	/*Returns the derivative of the output of the sigmoid activation function
	 *but takes as a parameter the already computer sigmoid output*/
	public static double sigmoidPrimeDynamicProgramming(double sigmoidPrime) {
		double output=(sigmoidPrime*(1-sigmoidPrime));
		return output;
	}

	/*This returns an array representing the output of all nodes in the given layer*/
	public static  ArrayList<Double> outPutOfLayer(ArrayList<ArrayList<Double>> currentLayer, ArrayList<Double> outputFromPreviousLayer) {
		ArrayList<Double> outputOfCurrentlayer = new ArrayList<Double>();
		for (int i=0; i<currentLayer.size(); i++) { 
			double output=nodeOutput(currentLayer,outputFromPreviousLayer,i);
			outputOfCurrentlayer.add(output);
		}
		return outputOfCurrentlayer;
	}


	/*Returns the summed total error of the output nodes and creats temporary storage for the output of all nodes for a given image*/
	public static double networkOutputError(ArrayList<DigitImage>  networkInputData, int imageNumber) {

		//Creates an Arraylist holding the output of each node in this layer
		ArrayList<Double> rawSingleImageData =networkInputData.get(imageNumber).getArrayListData();
		tempOutput.add(rawSingleImageData);//Stores result to be used later (This will be moved into the "outPutOfLayer" method at some point.)
		ArrayList<Double> hidenLayerOutput=outPutOfLayer(hiddenLayerNodes,rawSingleImageData);
		tempOutput.add(hidenLayerOutput);
		//Just like the others, but the call to hiddenLayersOutput() allows for additional hidden layers to be used.
		ArrayList<Double> outputLayerOutput=outPutOfLayer(outputLayerNodes,hiddenLayersOutput(hidenLayerOutput));
		tempOutput.add(outputLayerOutput);
		double error=0;
		//Adds the error from each output node to an array which is then stored along with the other above arrays to be usedvlater.
		ArrayList<Double> errorLayer= new ArrayList<Double>();

		for(int i=0; i<numberOfOutputNodes; i++){
			double correctOutput=networkInputData.get(imageNumber).getSolutionVector().get(i);
			
			double output=outputLayerOutput.get(i);
			
			double rawError=correctOutput-output;
			double  additionalsquaredError =(Math.pow((rawError), 2)/2);
			
			errorLayer.add(rawError);//Or should it be (additionalsquaredError)?
			
			error=error + additionalsquaredError;
		}
		tempOutput.add(errorLayer);
		return error;

	}

	/*This looks at one image and reports what  number it thinks it is.*/
	public static OutputVector networkSolution(ArrayList<DigitImage>  networkInputData, int imageNumber) {

		ArrayList<Double> rawSingleImageData =networkInputData.get(imageNumber).getArrayListData();
		ArrayList<Double> hidenLayerOutput=outPutOfLayer(hiddenLayerNodes,rawSingleImageData);
		ArrayList<Double> outputLayerOutput=outPutOfLayer(outputLayerNodes,hiddenLayersOutput(hidenLayerOutput));

		double networkOutput=0;
		double correctOutput=networkInputData.get(imageNumber).getLabel();
		int maxInt=0;

		for(int i=0; i<numberOfOutputNodes; i++){
			double output=outputLayerOutput.get(i);
			if(output>networkOutput){
				networkOutput=output;
				maxInt=i;
			}
		}
		if (correctOutput==maxInt){
			System.out.println("The network is correct. The correct number is: " + (int) correctOutput );
			countOfCorrectImagesAnalyzed++;
		} else{
			System.out.println("The network wrongly guessed: " +maxInt+" The correct number was: " + (int) correctOutput );
		}

		OutputVector result = new OutputVector(correctOutput, maxInt);
		countOfImagesAnalyzed++;
		return result;
	}
	/*Takes an image and returns the results of the neural network
	 * on the Testing Data in an object that can then be read and written to a file*/
	public static ArrayList<OutputVector> solveTestingData(ArrayList<DigitImage>  networkInputData) {
		ArrayList<OutputVector> newtworkResults = new ArrayList<OutputVector>(); 
		for(int i=0; i<networkInputData.size(); i++){
			newtworkResults.add(networkSolution(networkInputData,i));
		}
		return newtworkResults;
	}


	/*Calculates the output from the last hidden node considering all hidden layers*/
	public static ArrayList<Double> hiddenLayersOutput(ArrayList<Double> output) {
		//for loop takes care of various number of hidden layers
		if(numberOfhiddenLayers>1){
			ArrayList<Double> hidenLayersOutput= output;
			for(int l=1; l<numberOfhiddenLayers; l++){
				hidenLayersOutput=outPutOfLayer(hiddenLayers.get(l-1),hidenLayersOutput);
			}
			return 	hidenLayersOutput;
		}
		return output;

	}

	/*Writes the output of the Neural Net stored in an array of OutputVectors to a text file*/
	public static void write (ArrayList<OutputVector> x) throws IOException{
		BufferedWriter outputWriter = null;
		String randomString=Double.toString(Math.random());
		File file = new File("/Users/zackeryleman/Desktop/Results"+randomString+".txt");

		// if file does not exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}
		outputWriter = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		for (int i = 0; i < x.size(); i++) {
			outputWriter.write("Correct: "+ x.get(i).getCorrect()+"  ");
			outputWriter.write("Neural net output: "+ Integer.toString(x.get(i).getNeuralNetOutput())+"   ");
			outputWriter.write("Expected output: "+ Double.toString(x.get(i).getExpectedOutput()));
			outputWriter.newLine();
		}
		outputWriter.flush();  
		outputWriter.close();  
	}

	// NODE: This only works for one hidden layer right now
	/*This takes the training data and attempts to train the neural net to learn how to recognize  characters from images.*/
	public static void trainTheNetwork(	ArrayList<DigitImage> trainingData){

		for(int i = 0; i < epochs; i++) { //for each epoch
			
			for(int images=0; images<trainingData.size(); images++){ //for every image in the training file
				
				networkOutputError(trainingData,images);//This returns the summed error from all output nodes (Returned value is not currently used)
				
				//Update the weights to the output nodes
				for(int ii=0; ii<numberOfOutputNodes; ii++){
					for(int j=0; j<hiddenLayerNodes.size(); j++){
						//Grabs the error that was calculated for the output of this output node
						double error=tempOutput.get(tempOutput.size() - 1).get(ii);
						//Update the weight using gradient descent
						outputLayerNodes.get(ii).set(j,outputLayerNodes.get(ii).get(j)
								+(learningRate
										*error
										*sigmoidPrimeDynamicProgramming(tempOutput.get(tempOutput.size() - 2).get(ii))
										*tempOutput.get(tempOutput.size() - 3).get(j)));
					}
				}

				//Update the weights to the nodes going to the hidden nodes
				for(int ii=0; ii<hiddenLayerNodes.size(); ii++){
					for(int j=0; j<numberOfInputNodes; j++){
						//double outputError=tempOutput.get(tempOutput.size() - 1).get(ii);
						double error=0;
						for(int k=0; k<numberOfOutputNodes; k++){
							//This is the summed error for the output layer
							error= error+(sigmoidPrimeDynamicProgramming(tempOutput.get(tempOutput.size() - 2).get(k))
									*tempOutput.get(tempOutput.size() - 1).get(k)
									*outputLayerNodes.get(k).get(ii));

						}
						//Update the weight using gradient descent back propagation
						hiddenLayerNodes.get(ii).set(j,hiddenLayerNodes.get(ii).get(j)
								+(learningRate
										*error
										*tempOutput.get(0).get(j))
										*sigmoidPrimeDynamicProgramming(tempOutput.get(1).get(ii)));
					}
				}

				//Resets temporary data structure
				tempOutput = new ArrayList<ArrayList<Double>>();
			}

		}

	}

}
