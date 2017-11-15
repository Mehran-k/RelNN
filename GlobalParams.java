public class GlobalParams {
	public static String learningStatus = "train";
	public static Boolean debugMode = false;
	public static double ethaForWeights = 0.001; //Learning rate for weights
	public static double ethaForHiddens = 0.01; //Learning rate for numeric latent properties
	public static double lambdaForWeights = 0.0; //Regularization hyper-parameter for weights
	public static double lambdaForHiddens = 0.0; //Regularization hyper-parameter for numeric latent properties
	public static double lambdaForMean = 0.8; //Regularization towards the mean
	public static int numIteration = 1000; //Maximum number of iterations
	public static int numRandomWalk = 1; //Initialize the network K times and pick the best initialization 
	public static double splitPercentage = 0.80; //Split data for train/test
	public static String regularizationTypeForWeights = "L2"; //Type of regularization for weights
	public static String regularizationTypeForHiddens = "L1"; //Type of regularization for numeric latent properties
	public static int numFolds = 5; //number of folds in CV
	public static int numRandomRestarts = 1; //Training K models each starting from a random start point, and then picking the best
	public static int numRuns = 1; //Running K times, calculating the accuracies, and then taking the average to report more precise accuracies
	public static double maxRandom = 0.01; //When generating random numbers for initialization, this is the maximum value
	public static int testBatch = 50; //When testing, K new samples are added to the data and a prediction is made for them. If K is too large, it affects the statistics/counts.
}
