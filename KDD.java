import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class KDD {
	HashMap<String, Item> items;
	ArrayList<String> c1;
	ArrayList<String> c2;
	ArrayList<String> c3;
	ArrayList<String> c4;
	HashMap<String, KDDUser> users;
	ArrayList<String> train;
	ArrayList<String> test;
	
	public KDD(){
		items = new HashMap<String, Item>();
		c1 = new ArrayList<String>();
		c2 = new ArrayList<String>();
		c3 = new ArrayList<String>();
		c4 = new ArrayList<String>();
		users = new HashMap<String, KDDUser>();
	}
	
	public void setHyperParams(){
		//These are not the best values for hyper-parameters. 
		//Find the best values over a validation set. 
		GlobalParams.ethaForWeights = 0.001; //Learning rate for weights
		GlobalParams.ethaForHiddens = 0.01; //Learning rate for numeric latent properties
		GlobalParams.lambdaForWeights = 0.0; //Regularization hyper-parameter for weights
		GlobalParams.lambdaForHiddens = 0.0; //Regularization hyper-parameter for numeric latent properties
		GlobalParams.lambdaForMean = 0.8; //Regularization towards the mean
		GlobalParams.numIteration = 150; //Maximum number of iterations
		GlobalParams.numRandomWalk = 1; //Initialize the network K times and pick the best initialization 
		GlobalParams.splitPercentage = 0.80; //Split data for train/test
		GlobalParams.regularizationTypeForWeights = "L2"; //Type of regularization for weights
		GlobalParams.regularizationTypeForHiddens = "L1"; //Type of regularization for numeric latent properties
		GlobalParams.maxRandom = 0.01; //When generating random numbers for initialization, this is the maximum value
	}
	
	public void readFile() throws FileNotFoundException{
		Scanner scanner = new Scanner(new File("KDD15_123.db"));
		
		while(scanner.hasNext()){
			String line = scanner.nextLine();
			if(line.startsWith("item(")){
				String itemID = line.substring(5, line.length() - 1);
				items.put(itemID, new Item(itemID));
			}
			else if(line.startsWith("user(")){
				String userID = line.substring(5, line.length() - 1);
				users.put(userID, new KDDUser(userID));
			}
			else if(line.startsWith("time(")){
				String userID = line.substring(5, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				users.get(userID).time = value;
			}
			else if(line.startsWith("gender(")){
				String userID = line.substring(7, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				users.get(userID).gender = value;
			}
			else if(line.startsWith("c1(")){
				String category = line.substring(3, line.length() - 1);
				c1.add(category);
			}
			else if(line.startsWith("c2(")){
				String category = line.substring(3, line.length() - 1);
				c2.add(category);
			}
			else if(line.startsWith("c3(")){
				String category = line.substring(3, line.length() - 1);
				c3.add(category);
			}
			else if(line.startsWith("ic1(")){
				String itemID = line.substring(4, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				items.get(itemID).c1 = value;
			}
			else if(line.startsWith("ic2(")){
				String itemID = line.substring(4, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				items.get(itemID).c2 = value;
			}
			else if(line.startsWith("ic3(")){
				String itemID = line.substring(4, line.indexOf(","));
				String value = line.substring(line.indexOf(",") + 1, line.length() - 1);
				items.get(itemID).c3 = value;
			}
			else if(line.startsWith("viewed(")){
				String userID = line.substring(7, line.indexOf(","));
				String itemID = line.substring(line.indexOf(",") + 1, line.length() - 1);
				users.get(userID).itemsViewed.put(itemID, "T");
				items.get(itemID).viewedBy.put(userID, "T");
			}
		}
	}
	
	public void createTrainTestForCV(int fold){
		List<String> userIDs = Arrays.asList(users.keySet().toArray(new String[users.keySet().size()]));
		train = new ArrayList<String>();
		test = new ArrayList<String>();
		
		int numUserInTest = userIDs.size() / GlobalParams.numFolds;
		double index = 0;
		for(String userID : userIDs){
			if(index < fold * numUserInTest || index > (fold + 1) * numUserInTest)
				train.add(userID);
			else{
				test.add(userID);
			}
			index++;
		}
	}
	
	public void psudoCountForTrain(){ //Assuming there's a male and a female who have viewed all items (added for numeric consistency)
		KDDUser male = new KDDUser("PS1");
		male.gender = "male";
		male.time = "1";
		KDDUser female = new KDDUser("PS2");
		female.gender = "female";
		female.time = "1";
		for(String itemID : items.keySet()){
			male.itemsViewed.put(itemID, "T");
			female.itemsViewed.put(itemID, "T");
			items.get(itemID).viewedBy.put(male.id, "T");
			items.get(itemID).viewedBy.put(female.id, "T");
		}
		users.put(male.id, male);
		users.put(female.id, female);
		train.add(male.id);
		train.add(female.id);
	}
	
	public void learnModel(){
		HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
		String targetPRV = "Gender";
		String targetValue = "male";
		
		data.put("Viewed", new HashMap<String, String>());
		data.put("C1", new HashMap<String, String>());
		data.put("C2", new HashMap<String, String>());
		data.put("C3", new HashMap<String, String>());
		data.put("Time", new HashMap<String, String>());
		data.put("Gender", new HashMap<String, String>());
		
		boolean[] mark = new boolean[539];
		
		double probTargetClass = 0;
		for(String userID : train){
			if(users.get(userID).gender.equals(targetValue))
				probTargetClass++;
			
			data.get("Time").put(userID, users.get(userID).time);
			data.get("Gender").put(userID, users.get(userID).gender);
			for(String itemID : users.get(userID).itemsViewed.keySet()){
				data.get("Viewed").put(userID + "," + itemID, "T");
				mark[Integer.parseInt(itemID.substring(1, itemID.length()))] = true;
			}
		}
		
		for(String itemID : items.keySet()){
			data.get("C1").put(itemID + "," + items.get(itemID).c1, "T");
			data.get("C2").put(itemID + "," + items.get(itemID).c2, "T");
			data.get("C3").put(itemID + "," + items.get(itemID).c3, "T");
		}
		probTargetClass /= train.size();
		
		Set<String> userIDs = users.keySet();
		Set<String> itemIDs = items.keySet();
		LogVar u_lv = new LogVar("u", userIDs.toArray(new String[userIDs.size()]), "users");
		LogVar i_lv = new LogVar("i", itemIDs.toArray(new String[itemIDs.size()]), "reviewers");
		LogVar c1_lv = new LogVar("c1", c1.toArray(new String[c1.size()]), "c1");
		LogVar c2_lv = new LogVar("c2", c2.toArray(new String[c2.size()]), "c2");
		LogVar c3_lv = new LogVar("c3", c3.toArray(new String[c3.size()]), "c3");
		
		PRV viewed_prv = new PRV("Viewed", new LogVar[]{u_lv, i_lv}, "observed_input");
		PRV time_prv = new PRV("Time", new LogVar[]{u_lv}, "observed_input");
		PRV gender_prv = new PRV("Gender", new LogVar[]{u_lv}, "observed_input");
		
		PRV c1_prv = new PRV("C1", new LogVar[]{i_lv, c1_lv}, "observed_input");
		PRV c2_prv = new PRV("C2", new LogVar[]{i_lv, c2_lv}, "observed_input");
		PRV c3_prv = new PRV("C3", new LogVar[]{i_lv, c3_lv}, "observed_input");
		
		PRV feat1_c1 = new PRV("Feat1_c1", new LogVar[]{c1_lv}, "unobserved_input");
		data.put("Feat1_c1", feat1_c1.randomValues());
		PRV feat2_c1 = new PRV("Feat2_c1", new LogVar[]{c1_lv}, "unobserved_input");
		data.put("Feat2_c1", feat2_c1.randomValues());
		
		PRV feat1_c2 = new PRV("Feat1_c2", new LogVar[]{c2_lv}, "unobserved_input");
		data.put("Feat1_c2", feat1_c2.randomValues());
		PRV feat2_c2 = new PRV("Feat2_c2", new LogVar[]{c2_lv}, "unobserved_input");
		data.put("Feat2_c2", feat2_c2.randomValues());
		
		PRV feat1_c3 = new PRV("Feat1_c3", new LogVar[]{c3_lv}, "unobserved_input");
		data.put("Feat1_c3", feat1_c3.randomValues());
		PRV feat2_c3 = new PRV("Feat2_c3", new LogVar[]{c3_lv}, "unobserved_input");
		data.put("Feat2_c3", feat2_c3.randomValues());
		
		PRV score_i_prv = new PRV("Score_i", new LogVar[]{i_lv}, "hidden");
		PRV score_ui_prv = new PRV("Score_ui", new LogVar[]{u_lv}, "hidden");
		PRV stats_i_m_prv = new PRV("Stats_i_m", new LogVar[]{i_lv}, "hidden");
		PRV stats_i_f_prv = new PRV("Stats_i_f", new LogVar[]{i_lv}, "hidden");
		PRV stats_ui_prv = new PRV("Stats_ui", new LogVar[]{u_lv}, "hidden");
		PRV score_uc1_prv = new PRV("Score_uc1", new LogVar[]{u_lv}, "hidden");
		PRV score_uc2_prv = new PRV("Score_uc2", new LogVar[]{u_lv}, "hidden");
		PRV score_uc3_prv = new PRV("Score_uc3", new LogVar[]{u_lv}, "hidden");
		PRV h_time_prv = new PRV("H_time", new LogVar[]{u_lv}, "hidden");
		PRV viewed_count_prv = new PRV("ViewedCount", new LogVar[]{u_lv}, "hidden");
		
		WeightedFormula base_wf = new WeightedFormula(new Literal[]{}, 1);
		WeightedFormula viewed_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T")}, 1);
		WeightedFormula time_wf = new WeightedFormula(new Literal[]{time_prv.lit("NA!")}, 1);
		
		WeightedFormula viewed_m_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), gender_prv.lit("male")}, 1);
		WeightedFormula viewed_f_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), gender_prv.lit("female")}, 1);
		WeightedFormula stats_i_m_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), stats_i_m_prv.lit("NA!")}, 1);
		WeightedFormula stats_i_f_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), stats_i_f_prv.lit("NA!")}, 1);
		WeightedFormula viewed_score_i_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), score_i_prv.lit("NA!")}, 1);
		
		WeightedFormula score_c1_wf = new WeightedFormula(new Literal[]{c1_prv.lit("T"), feat1_c1.lit("NA!")}, 1);
		WeightedFormula score_c2_wf = new WeightedFormula(new Literal[]{c2_prv.lit("T"), feat1_c2.lit("NA!")}, 1);
		WeightedFormula score_c3_wf = new WeightedFormula(new Literal[]{c3_prv.lit("T"), feat1_c3.lit("NA!")}, 1);
		
		WeightedFormula viewed_c1_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), c1_prv.lit("T"), feat2_c1.lit("NA!")}, 1);
		WeightedFormula viewed_c2_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), c2_prv.lit("T"), feat2_c2.lit("NA!")}, 1);
		WeightedFormula viewed_c3_wf = new WeightedFormula(new Literal[]{viewed_prv.lit("T"), c3_prv.lit("T"), feat2_c3.lit("NA!")}, 1);
		
		WeightedFormula score_uc1_wf = new WeightedFormula(new Literal[]{score_uc1_prv.lit("NA!")}, 1);
		WeightedFormula score_uc2_wf = new WeightedFormula(new Literal[]{score_uc2_prv.lit("NA!")}, 1);
		WeightedFormula score_uc3_wf = new WeightedFormula(new Literal[]{score_uc3_prv.lit("NA!")}, 1);
		
		WeightedFormula score_ui_wf = new WeightedFormula(new Literal[]{score_ui_prv.lit("NA!")}, 1);
		WeightedFormula viewed_count_wf = new WeightedFormula(new Literal[]{viewed_count_prv.lit("T")}, 1);
		WeightedFormula stats_ui_wf = new WeightedFormula(new Literal[]{stats_ui_prv.lit("NA!")}, 1);
		WeightedFormula h_time_wf = new WeightedFormula(new Literal[]{h_time_prv.lit("NA!")}, 1);
		
		RelNeuron score_i_rn = new RelNeuron(score_i_prv, new WeightedFormula[]{score_c1_wf, score_c2_wf, score_c3_wf, base_wf});
		RelNeuron stats_i_m_rn = new RelNeuron(stats_i_m_prv, new WeightedFormula[]{viewed_m_wf, base_wf});
		RelNeuron stats_i_f_rn = new RelNeuron(stats_i_f_prv, new WeightedFormula[]{viewed_f_wf, base_wf});
		RelNeuron score_uc1_rn = new RelNeuron(score_uc1_prv, new WeightedFormula[]{viewed_c1_wf, base_wf});
		RelNeuron score_uc2_rn = new RelNeuron(score_uc2_prv, new WeightedFormula[]{viewed_c2_wf, base_wf});
		RelNeuron score_uc3_rn = new RelNeuron(score_uc3_prv, new WeightedFormula[]{viewed_c3_wf, base_wf});
		RelNeuron score_ui_rn = new RelNeuron(score_ui_prv, new WeightedFormula[]{viewed_score_i_wf, base_wf});
		RelNeuron viewed_rn = new RelNeuron(viewed_count_prv, new WeightedFormula[]{viewed_wf, base_wf});
		RelNeuron stats_ui_rn = new RelNeuron(stats_ui_prv, new WeightedFormula[]{stats_i_m_wf, stats_i_f_wf, base_wf});
		RelNeuron h_time_rn = new RelNeuron(h_time_prv, new WeightedFormula[]{time_wf, base_wf});
		RelNeuron gender_rn = new RelNeuron(gender_prv, new WeightedFormula[]{score_uc1_wf, score_uc2_wf, score_uc3_wf, score_ui_wf, viewed_count_wf, stats_ui_wf, h_time_wf, base_wf});
		
		Layer linear_layer1 = new LinearLayer(new RelNeuron[]{score_i_rn, stats_i_m_rn, stats_i_f_rn});
		Layer sig_layer1 = new SigmoidLayer();
		Layer linear_layer2 = new LinearLayer(new RelNeuron[]{score_uc1_rn, score_uc2_rn, score_uc3_rn, score_ui_rn, viewed_rn, stats_ui_rn, h_time_rn});
		Layer sig_layer2 = new SigmoidLayer();
		Layer linear_layer3 = new LinearLayer(new RelNeuron[]{gender_rn});
		Layer sig_layer3 = new SigmoidLayer();
		Layer sum_sqr_error_layer = new SumSquaredErrorLayer(targetValue);
		
		RNN rnn = new RNN(new Layer[]{linear_layer1, sig_layer1, linear_layer2, sig_layer2, linear_layer3, sig_layer3, sum_sqr_error_layer});
		
		double train_error = rnn.train(data, data.get(targetPRV));
		rnn.print();
		System.out.println("The final error on train data: " + train_error);

//		//calculating the performance on train data
		System.out.println("Performance on train data");
		HashMap<String, String> predictions = rnn.test(data).get(targetPRV);
		Measures measures = new Measures(data.get(targetPRV), predictions, targetValue);
		System.out.println("Accuracy: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
		
		
//		//calculating the performance on test data
		GlobalParams.learningStatus = "test";
		targetValue = "Test_" + targetValue;
		System.out.println("Performance on test data");
		predictions = new HashMap<String, String>();
		HashMap<String, String> targets = new HashMap<String, String>();
//		
//		//Adding n user in each run
		double male = 0, female = 0;
		for(int i = 0; i < test.size(); i += GlobalParams.testBatch){
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				targets.put(test.get(j), "Test_" + users.get(test.get(j)).gender);
				if(users.get(test.get(j)).gender.equals("male"))
					male++;
				else
					female++;
			}
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				data.get("Gender").put(test.get(j), "Test_" + users.get(test.get(j)).gender);
				data.get("Time").put(test.get(j), users.get(test.get(j)).time);
				for(String itemID : users.get(test.get(j)).itemsViewed.keySet()){
					data.get("Viewed").put(test.get(j) + "," + itemID, "T");
				}
			}
			
			HashMap<String, String> rnn_preds = rnn.test(data).get(targetPRV);
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				double userPred = Double.parseDouble(rnn_preds.get(test.get(j)));
				userPred = GlobalParams.lambdaForMean * userPred + (1 - GlobalParams.lambdaForMean) * probTargetClass;
				predictions.put(test.get(j), "" + userPred);
			}
			
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				data.get("Gender").remove(test.get(j));
				data.get("Time").remove(test.get(j));
				for(String itemID : users.get(test.get(j)).itemsViewed.keySet()){
					data.get("Viewed").remove(test.get(j) + "," + itemID);
				}
			}
		}
		System.out.println(male + " " + female + " " + male / (male + female));
		
		measures = new Measures(targets, predictions, targetValue);
		System.out.println("Accuracy with 0.5 boundary: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("KDD15");
	
		for(int i = 0; i < GlobalParams.numRuns; i++){
			KDD kdd = new KDD();
			kdd.setHyperParams();
			kdd.readFile();
			kdd.createTrainTestForCV(4);
			kdd.psudoCountForTrain();
			kdd.learnModel();
		}
	}
}

class KDDUser {
	String id;
	String time;
	String gender;
	HashMap<String, String> itemsViewed;
	
	public KDDUser(String id){
		this.id = id;
		itemsViewed = new HashMap<String, String>();
	}
}

class Item {
	String id;
	HashMap<String, String> viewedBy;
	String c1;
	String c2;
	String c3;
	String c4;
	
	public Item(String id){
		this.id = id;
		viewedBy = new HashMap<String, String>();
	}
}
