import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class YelpMC {
	HashMap<String, Business> businesses;
	HashMap<String, Reviewer> reviewers;
	ArrayList<String> train;
	ArrayList<String> test;
	
	public YelpMC() throws FileNotFoundException{
		
		businesses = new HashMap<String, Business>();
		reviewers = new HashMap<String, Reviewer>();
	}
	
	public void setHyperParams(){
		//These are not the best values for hyper-parameters. 
		//Find the best values over a validation set. 
		GlobalParams.ethaForWeights = 0.01; //Learning rate for weights
		GlobalParams.ethaForHiddens = 0.05; //Learning rate for numeric latent properties
		GlobalParams.lambdaForWeights = 0.0; //Regularization hyper-parameter for weights
		GlobalParams.lambdaForHiddens = 0.0; //Regularization hyper-parameter for numeric latent properties
		GlobalParams.lambdaForMean = 0.6; //Regularization towards the mean
		GlobalParams.numIteration = 300; //Maximum number of iterations
		GlobalParams.numRandomWalk = 1; //Initialize the network K times and pick the best initialization 
		GlobalParams.splitPercentage = 0.80; //Split data for train/test
		GlobalParams.regularizationTypeForWeights = "L1"; //Type of regularization for weights
		GlobalParams.regularizationTypeForHiddens = "L1"; //Type of regularization for numeric latent properties
		GlobalParams.maxRandom = 0.01; //When generating random numbers for initialization, this is the maximum value
	}
	
	public void readFile() throws FileNotFoundException{
		BufferedReader br = new BufferedReader(new FileReader(new File("datasets/yelp_mc.db")));
		
		for(String line : br.lines().toArray(String[]::new)){
			if(line.startsWith("business(")){
				String busID = line.substring(9, line.length() - 1);
				businesses.put(busID, new Business(busID));
			}
			else if(line.startsWith("type(")){
				String busID = line.substring(5, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				businesses.get(busID).type = value;
			}
			else if(line.startsWith("ff(")){
				String busID = line.substring(3, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				businesses.get(busID).ff = value;
			}
			else if(line.startsWith("sf(")){
				String busID = line.substring(3, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				businesses.get(busID).sf = value;
			}
			else if(line.startsWith("user(")){
				String revID = line.substring(5, line.length() - 1);
				reviewers.put(revID, new Reviewer(revID));
			}
			else if(line.startsWith("reviewed(")){
				String revID = line.substring(9, line.indexOf(","));
				String busID = line.substring(line.indexOf(",") + 1, line.length() - 1);
				reviewers.get(revID).busReviewed.put(busID, "T");
				businesses.get(busID).reviewedBy.put(revID, "T");
			}
		}
	}
	
	public void createTrainTestForCV(int fold) throws IOException{ 
		List<String> busIDs = Arrays.asList(businesses.keySet().toArray(new String[businesses.keySet().size()]));
		train = new ArrayList<String>();
		test = new ArrayList<String>();
		
		int numRevInTest = busIDs.size() / GlobalParams.numFolds;
		
		double index = 0;
		for(String busID : busIDs){
			if(index < fold * numRevInTest || index > (fold + 1) * numRevInTest)
				train.add(busID);
			else
				test.add(busID);
			index++;
		}
	}
	
	public void learnModel(){
		HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
		String targetPRV = "Type";
		String targetValue = "M";
		
		data.put("Reviewed", new HashMap<String, String>());
		data.put("Type", new HashMap<String, String>());
		data.put("FF", new HashMap<String, String>());
		data.put("SF", new HashMap<String, String>());
		
		double probMexicanTrain = 0;
		for(String busID : train){
			if(businesses.get(busID).type.equals(targetValue))
				probMexicanTrain++;
			
			data.get("Type").put(busID, businesses.get(busID).type);
			data.get("FF").put(busID, businesses.get(busID).ff);
			data.get("SF").put(busID, businesses.get(busID).sf);
			for(String revID : businesses.get(busID).reviewedBy.keySet()){
				data.get("Reviewed").put(revID + "," + busID, "T");
			}
		}
		probMexicanTrain /= train.size();
		
		Set<String> busIDs = businesses.keySet();
		Set<String> revIDs = reviewers.keySet();
		LogVar b = new LogVar("b", busIDs.toArray(new String[busIDs.size()]), "businesses");
		LogVar r = new LogVar("r", revIDs.toArray(new String[revIDs.size()]), "reviewers");
		
		PRV reviewed_prv = new PRV("Reviewed", new LogVar[]{r, b}, "observed_input");
		PRV type_prv = new PRV("Type", new LogVar[]{b}, "observed_input");
		PRV ff_prv = new PRV("FF", new LogVar[]{b}, "observed_input");
		PRV sf_prv = new PRV("SF", new LogVar[]{b}, "observed_input");
		
		PRV feat1_r = new PRV("Feat1_r", new LogVar[]{r}, "unobserved_input");
		data.put("Feat1_r", feat1_r.randomValues());
		PRV feat2_r = new PRV("Feat2_r", new LogVar[]{r}, "unobserved_input");
		data.put("Feat2_r", feat2_r.randomValues());
		
		PRV HR_prv = new PRV("HR", new LogVar[]{b}, "hidden");
		PRV HI1_prv = new PRV("HI1", new LogVar[]{b}, "hidden");
		PRV HI2_prv = new PRV("HI2", new LogVar[]{r}, "hidden");
		
		WeightedFormula base_wf = new WeightedFormula(new Literal[]{}, 1);
		WeightedFormula reviewed_wf = new WeightedFormula(new Literal[]{reviewed_prv.lit("T")}, 1);
		WeightedFormula reviewed_HI1_wf = new WeightedFormula(new Literal[]{reviewed_prv.lit("T"), feat1_r.lit("NA!")}, 1);
		WeightedFormula reviewed_HI2_wf = new WeightedFormula(new Literal[]{reviewed_prv.lit("true"), feat2_r.lit("NA!")}, 1);
		WeightedFormula HR_wf = new WeightedFormula(new Literal[]{HR_prv.lit("NA!")}, 1); //NA! indicates its continuous
		WeightedFormula HI1_wf = new WeightedFormula(new Literal[]{HI1_prv.lit("NA!")}, 1); //NA! indicates its continuous
		WeightedFormula HI2_wf = new WeightedFormula(new Literal[]{HI2_prv.lit("NA!")}, 1); //NA! indicates its continuous
		WeightedFormula ff_wf = new WeightedFormula(new Literal[]{ff_prv.lit("T")}, 1);
		WeightedFormula sf_wf = new WeightedFormula(new Literal[]{sf_prv.lit("T")}, 1);
		
		RelNeuron HR_rn = new RelNeuron(HR_prv, new WeightedFormula[]{reviewed_wf, base_wf});
		RelNeuron HI1_rn = new RelNeuron(HI1_prv, new WeightedFormula[]{reviewed_HI1_wf, base_wf});
		RelNeuron HI2_rn = new RelNeuron(HI2_prv, new WeightedFormula[]{reviewed_HI2_wf, base_wf});
		RelNeuron type_rn = new RelNeuron(type_prv, new WeightedFormula[]{HR_wf, HI1_wf, HI2_wf, ff_wf, sf_wf, base_wf});
		
		Layer linear_layer1 = new LinearLayer(new RelNeuron[]{HR_rn, HI1_rn, HI2_rn});
		Layer sig_layer1 = new SigmoidLayer();
		Layer linear_layer2 = new LinearLayer(new RelNeuron[]{type_rn});
		Layer sig_layer2 = new SigmoidLayer();
		Layer sum_sqr_error_layer = new SumSquaredErrorLayer(targetValue);
		
		RNN rnn = new RNN(new Layer[]{linear_layer1, sig_layer1, linear_layer2, sig_layer2, sum_sqr_error_layer});
		
		double train_error = rnn.train(data, data.get(targetPRV));
		rnn.print();
		System.out.println("The final error on train data: " + train_error);
//		
//		//calculating the performance on train data
		System.out.println("Performance on train data");
		HashMap<String, String> predictions = rnn.test(data).get(targetPRV);
		Measures measures = new Measures(data.get(targetPRV), predictions, targetValue);
		System.out.println("Accuracy: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
		
		rnn.print();
		
//		//calculating the performance on test data
		GlobalParams.learningStatus = "test";
		System.out.println("Performance on test data");
		predictions = new HashMap<String, String>();
		HashMap<String, String> targets = new HashMap<String, String>();
//		
//		//Adding n user in each run
		for(int i = 0; i < test.size(); i += GlobalParams.testBatch){
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				targets.put(test.get(j), businesses.get(test.get(j)).type);
			}
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				data.get("Type").put(test.get(j), businesses.get(test.get(j)).type);
				data.get("FF").put(test.get(j), businesses.get(test.get(j)).ff);
				data.get("SF").put(test.get(j), businesses.get(test.get(j)).sf);
				for(String revID : businesses.get(test.get(j)).reviewedBy.keySet()){
					data.get("Reviewed").put(revID + "," + test.get(j), "T");
				}
			}
			
			HashMap<String, String> rnn_preds = rnn.test(data).get(targetPRV);
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				double busPred = Double.parseDouble(rnn_preds.get(test.get(j)));
				busPred = GlobalParams.lambdaForMean * busPred + (1 - GlobalParams.lambdaForMean) * probMexicanTrain;
				predictions.put(test.get(j), "" + busPred);
			}
			
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				data.get("Type").remove(test.get(j));
				data.get("FF").remove(test.get(j));
				data.get("SF").remove(test.get(j));
				for(String revID : businesses.get(test.get(j)).reviewedBy.keySet()){
					data.get("Reviewed").remove(revID + "," + test.get(j));
				}
			}
		}
		
		measures = new Measures(targets, predictions, targetValue);
		System.out.println("Accuracy with 0.5 boundary: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
	}
	
	public static void main(String[] args) throws IOException {
		YelpMC yelp = new YelpMC();
		yelp.setHyperParams();
		yelp.readFile();
		yelp.createTrainTestForCV(4); //For testing, the whole data is given and the last chunk is used as the test examples. For validation, this part should be removed before running CV. 
		yelp.learnModel();
	}
}


class Business{
	String id;
	String type;
	String ff;
	String sf;
	HashMap<String, String> reviewedBy;
	
	public Business(String id){
		this.id = id;
		reviewedBy = new HashMap<String, String>();
	}
}

class Reviewer{
	String id;
	HashMap<String, String> busReviewed;
	
	public Reviewer(String id){
		this.id = id;
		busReviewed = new HashMap<String, String>();
	}
}
