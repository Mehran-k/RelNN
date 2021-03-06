import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class MovieLensGender {
	ArrayList<String> train;
	ArrayList<String> test;
	HashMap<String, User> users;
	HashMap<String, Movie> movies;
	HashMap<String, String> similar;
	
	public MovieLensGender(){
		users = new HashMap<String, User>();
		movies = new HashMap<String, Movie>();
		similar = new HashMap<String, String>();
	}
	
	public void setHyperParams(){
		//These are not the best values for hyper-parameters. 
		//Find the best values over a validation set. 
		GlobalParams.ethaForWeights = 0.01; //Learning rate for weights
		GlobalParams.ethaForHiddens = 0.03; //Learning rate for numeric latent properties
		GlobalParams.lambdaForWeights = 0.0002; //Regularization hyper-parameter for weights
		GlobalParams.lambdaForHiddens = 0.0002; //Regularization hyper-parameter for numeric latent properties
		GlobalParams.lambdaForMean = 0.85; //Regularization towards the mean
		GlobalParams.numIteration = 300; //Maximum number of iterations
		GlobalParams.numRandomWalk = 1; //Initialize the network K times and pick the best initialization 
		GlobalParams.splitPercentage = 0.80; //Split data for train/test
		GlobalParams.regularizationTypeForWeights = "L1"; //Type of regularization for weights
		GlobalParams.regularizationTypeForHiddens = "L1"; //Type of regularization for numeric latent properties
		GlobalParams.maxRandom = 0.01; //When generating random numbers for initialization, this is the maximum value
	}
	
	public void createTrainTestForCV(int fold){
		List<String> user_ids = Arrays.asList(users.keySet().toArray(new String[users.keySet().size()]));
		train = new ArrayList<String>();
		test = new ArrayList<String>();
		
		int numUsersInTest = user_ids.size() / GlobalParams.numFolds;
		
		double index = 0;
		for(String user_id : user_ids){
			if(index < fold * numUsersInTest || index > (fold + 1) * numUsersInTest)
				train.add(user_id);
			else{
				test.add(user_id);
			}
			index++;
		}
	}
	
	public void readFile() throws FileNotFoundException{
		BufferedReader br = new BufferedReader(new FileReader(new File("datasets/ml-1m/users.dat")));
		
		for(String nextLine : br.lines().toArray(String[]::new)){
			String[] line = nextLine.split("::");
			User user = new User(line[0]);
			user.gender = line[1];
			user.age = line[2];
			user.occupation = line[3];
			users.put(line[0], user);
		}
		
		br = new BufferedReader(new FileReader(new File("datasets/ml-1m/movies.dat")));
		for(String nextLine : br.lines().toArray(String[]::new)){
			String[] line = nextLine.split("::");
			Movie movie = new Movie(line[0]);
			for(String genre : line[2].split(",")){
				movie.genre.put(genre, "true");
			}
			movies.put(line[0], movie);
		}
		br = new BufferedReader(new FileReader(new File("datasets/ml-1m/ratings.dat")));
		for(String nextLine : br.lines().toArray(String[]::new)){
			String[] line = nextLine.split("::");
			users.get(line[0]).moviesRated.put(line[1], "yes");
			users.get(line[0]).moviesRatedVector.addElement(line[1]);
			movies.get(line[1]).ratedBy.put(line[0], "yes");
		}
	}
	
	public void learnModel(){
		//This version has two numeric latent properties and one hidden layer. The final version used in the paper has two hidden layers.
		HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
		String targetPRV = "Gender";
		String targetValue = "M";
		
		String[] genres = {"Action", "Drama"};
		String[] occupations = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"};
		String[] ages = {"1", "18", "25", "35", "45", "50", "56"};
		String[] rates = {"yes"};
		
		for(String genre : genres){
			data.put(genre, new HashMap<String, String>());
		}
		data.put("Age", new HashMap<String, String>());
		data.put("Gender", new HashMap<String, String>());
		data.put("Occupation", new HashMap<String, String>());
		data.put("Rate", new HashMap<String, String>());
		data.put("RatePrime", new HashMap<String, String>());
		data.put("FeatSim", new HashMap<String, String>());
		
		for(String movie_id : movies.keySet()){
			for(String genre : genres){
				data.get(genre).put(movie_id, movies.get(movie_id).genre.getOrDefault(genre, "false"));
			}
		}
		
		for(String key : this.similar.keySet()){
			data.get("FeatSim").put(key, this.similar.get(key));
		}
		
		double probTargetClass = 0.0;
		for(String user_id : train){
			if(users.get(user_id).gender.equals(targetValue))
				probTargetClass++;
			
			data.get("Age").put(user_id, users.get(user_id).age);
			data.get("Gender").put(user_id, users.get(user_id).gender);
			data.get("Occupation").put(user_id, users.get(user_id).occupation);
			for(String movie_id : users.get(user_id).moviesRated.keySet()){
				data.get("Rate").put(user_id + "," + movie_id, users.get(user_id).moviesRated.get(movie_id));
				data.get("RatePrime").put(user_id + "," + movie_id, users.get(user_id).moviesRated.get(movie_id));
			}
		}
		probTargetClass /= train.size();
		
		Set<String> movie_ids = movies.keySet();
		Set<String> user_ids = users.keySet();
		LogVar m = new LogVar("m", movie_ids.toArray(new String[movie_ids.size()]), "movie");
		LogVar m_prime = new LogVar("m_prime", movie_ids.toArray(new String[movie_ids.size()]), "movie");
		LogVar p = new LogVar("p", user_ids.toArray(new String[user_ids.size()]), "people");
		
		PRV rate = new PRV("Rate", new LogVar[]{p, m}, "observed_input");
		PRV rate_prime = new PRV("RatePrime", new LogVar[]{p, m_prime}, "observed_input");
		PRV[] genre_prvs = new PRV[genres.length];
		for(int i = 0; i < genre_prvs.length; i++)
			genre_prvs[i] = new PRV(genres[i], new LogVar[]{m}, "observed_input");
		
		PRV gender = new PRV("Gender", new LogVar[]{p}, "observed_input");
		PRV age = new PRV("Age", new LogVar[]{p}, "observed_input");
		PRV occupation = new PRV("Occupation", new LogVar[]{p}, "observed_input");
		PRV feat1_m = new PRV("Feat1_m", new LogVar[]{m}, "unobserved_input");
		data.put("Feat1_m", feat1_m.randomValues());
		PRV feat2_m = new PRV("Feat2_m", new LogVar[]{m}, "unobserved_input");
		data.put("Feat2_m", feat2_m.randomValues());
		
		PRV[][] rates_genres_prvs = new PRV[rates.length][genres.length];
		PRV[] hidden_input1_prvs = new PRV[rates.length];
		PRV[] hidden_input2_prvs = new PRV[rates.length];
		
		for(int i = 0; i < rates.length; i++){
			for(int j = 0; j < genres.length; j++){
				rates_genres_prvs[i][j] = new PRV("H" + i + "_" + j, new LogVar[]{p}, "hidden");
			}
			hidden_input1_prvs[i] = new PRV("HI1_" + i, new LogVar[]{p}, "hidden");
			hidden_input2_prvs[i] = new PRV("HI2_" + i, new LogVar[]{p}, "hidden");
		}
		
		WeightedFormula base = new WeightedFormula(new Literal[]{}, 1);
		WeightedFormula[][] rates_genres_wfs = new WeightedFormula[rates.length][genres.length];
		WeightedFormula[] hidden_input1_wfs = new WeightedFormula[rates.length];
		WeightedFormula[] hidden_input2_wfs = new WeightedFormula[rates.length];
		
		WeightedFormula[][] hidden_layer1_rates_genres_wfs = new WeightedFormula[rates.length][genres.length];
		WeightedFormula[] hidden_layer1_hidden_input1_wfs = new WeightedFormula[rates.length];
		WeightedFormula[] hidden_layer1_hidden_input2_wfs = new WeightedFormula[rates.length];
		
		for(int i = 0; i < rates.length; i++){
			for(int j = 0; j < genres.length; j++){
				rates_genres_wfs[i][j] = new WeightedFormula(new Literal[]{rate.lit(rates[i]), genre_prvs[j].lit("true")}, 1);
				hidden_layer1_rates_genres_wfs[i][j] = new WeightedFormula(new Literal[]{rates_genres_prvs[i][j].lit("NA!")}, 1);
			}
			hidden_input1_wfs[i] = new WeightedFormula(new Literal[]{rate.lit(rates[i]), feat1_m.lit("NA!")}, 1);
			hidden_input2_wfs[i] = new WeightedFormula(new Literal[]{rate.lit(rates[i]), feat2_m.lit("NA!")}, 1);
			hidden_layer1_hidden_input1_wfs[i] = new WeightedFormula(new Literal[]{hidden_input1_prvs[i].lit("NA!")}, 1);
			hidden_layer1_hidden_input2_wfs[i] = new WeightedFormula(new Literal[]{hidden_input2_prvs[i].lit("NA!")}, 1);
		}
		
		WeightedFormula[] occupation_wfs = new WeightedFormula[occupations.length];
		for(int i = 0; i < occupation_wfs.length; i++){
			occupation_wfs[i] = new WeightedFormula(new Literal[]{occupation.lit(occupations[i])}, 1);
		}
		
		WeightedFormula[] age_wfs = new WeightedFormula[ages.length];
		for(int i = 0; i < age_wfs.length; i++){
			age_wfs[i] = new WeightedFormula(new Literal[]{age.lit(ages[i])}, 1);
		}
		
		RelNeuron[][] rates_genres_rns = new RelNeuron[rates.length][genres.length];
		for(int i = 0; i < rates.length; i++){
			for(int j = 0; j < genres.length; j++){
				rates_genres_rns[i][j] = new RelNeuron(rates_genres_prvs[i][j], new WeightedFormula[]{rates_genres_wfs[i][j], base});
			}
		}
		
		RelNeuron[] hidden_input1_rns = new RelNeuron[rates.length];
		for(int i = 0; i < rates.length; i++){
			hidden_input1_rns[i] = new RelNeuron(hidden_input1_prvs[i], new WeightedFormula[]{hidden_input1_wfs[i], base});
		}
		
		RelNeuron[] hidden_input2_rns = new RelNeuron[rates.length];
		for(int i = 0; i < rates.length; i++){
			hidden_input2_rns[i] = new RelNeuron(hidden_input2_prvs[i], new WeightedFormula[]{hidden_input2_wfs[i], base});
		}
		
		int num_hidden_inputs = 2;
		WeightedFormula[] gender_wfs = new WeightedFormula[1 + occupation_wfs.length + age_wfs.length + rates.length * genres.length + num_hidden_inputs * rates.length];
		int index = 0;
		gender_wfs[index++] = new WeightedFormula(base);
		for(int i = 0; i < occupation_wfs.length; i++)
			gender_wfs[index++] = occupation_wfs[i];
		for(int i = 0; i < age_wfs.length; i++)
			gender_wfs[index++] = age_wfs[i];
		for(int i = 0; i < rates.length; i++)
			for(int j = 0; j < genres.length; j++)
				gender_wfs[index++] = hidden_layer1_rates_genres_wfs[i][j];
		for(int i = 0; i < hidden_input1_wfs.length; i++)
			gender_wfs[index++] = hidden_layer1_hidden_input1_wfs[i];
		for(int i = 0; i < hidden_input2_wfs.length; i++)
			gender_wfs[index++] = hidden_layer1_hidden_input2_wfs[i];
		
		RelNeuron gender_rn = new RelNeuron(gender, gender_wfs);
			
		RelNeuron[] layer1_rns = new RelNeuron[rates.length * genres.length + num_hidden_inputs * rates.length];
		index = 0;
		for(int i = 0; i < rates.length; i++)
			for(int j = 0; j < genres.length; j++)
				layer1_rns[index++] = rates_genres_rns[i][j];
		for(int i = 0; i < rates.length; i++)
			layer1_rns[index++] = hidden_input1_rns[i];
		for(int i = 0; i < rates.length; i++)
			layer1_rns[index++] = hidden_input2_rns[i];
		
		Layer linear_layer1 = new LinearLayer(layer1_rns);
		Layer sig_layer1 = new SigmoidLayer();
		Layer linear_layer2 = new LinearLayer(new RelNeuron[]{gender_rn});
		Layer sig_layer2 = new SigmoidLayer();
		Layer sum_sqr_error_layer = new SumSquaredErrorLayer(targetValue);
		
		RNN rnn = new RNN(new Layer[]{linear_layer1, sig_layer1, linear_layer2, sig_layer2, sum_sqr_error_layer});
		
		double train_error = rnn.train(data, data.get(targetPRV));
		System.out.println("The final error on train data: " + train_error);
		
		//calculating the performance on train data
		System.out.println("Performance on train data");
		HashMap<String, String> predictions = rnn.test(data).get(targetPRV);
		Measures measures = new Measures(data.get(targetPRV), predictions, targetValue);
		System.out.println("Accuracy: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
		
		//calculating the performance on test data
		GlobalParams.learningStatus = "test";
		System.out.println("Performance on test data");
		predictions = new HashMap<String, String>();
		HashMap<String, String> targets = new HashMap<String, String>();
		
		//Adding n user in each run
		System.out.println(test.size());
		for(int i = 0; i < test.size(); i += GlobalParams.testBatch){
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				targets.put(test.get(j), users.get(test.get(j)).gender);
			}
			for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
				data.get("Gender").put(test.get(j), users.get(test.get(j)).gender);
				data.get("Age").put(test.get(j), users.get(test.get(j)).age);
				data.get("Occupation").put(test.get(j), users.get(test.get(j)).occupation);
				for(String movie_id : users.get(test.get(j)).moviesRated.keySet()){
					data.get("Rate").put(test.get(j) + "," + movie_id, users.get(test.get(j)).moviesRated.get(movie_id));
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
				data.get("Age").remove(test.get(j));
				data.get("Occupation").remove(test.get(j));
				for(String movie_id : users.get(test.get(j)).moviesRated.keySet()){
					data.get("Rate").remove(test.get(j) + "," + movie_id);
				}
			}
		}
		
		measures = new Measures(targets, predictions, targetValue);
		System.out.println("Accuracy with 0.5 boundary: " + measures.accuracy(0.5));
		System.out.println("MAE: " + measures.MAE());
		System.out.println("MSE: " + measures.MSE());
		System.out.println("ACLL: " + measures.ACLL());
		
//		This part of the code was used for the experiment on extrapolating to unseen cases and addressing the population size issue
//		int[] Qs = {75, 100};
//		int[] Qs = {0, 1, 2, 3, 4, 5, 7, 10, 15, 20, 30, 40, 50, 75, 100, 125, 150, 200, 250, 300, 400, 500};
//		for(int q = 0; q < Qs.length; q++){
//			predictions = new HashMap<String, String>();
//			targets = new HashMap<String, String>();
//			for(int i = 0; i < test.size(); i += GlobalParams.testBatch){
//				for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
//					targets.put(test.get(j), users.get(test.get(j)).gender);
//				}
//				for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
//					data.get("Gender").put(test.get(j), users.get(test.get(j)).gender);
//					data.get("Age").put(test.get(j), users.get(test.get(j)).age);
//					data.get("Occupation").put(test.get(j), users.get(test.get(j)).occupation);
//					for(int k = 0; k < Qs[q] && k < users.get(test.get(j)).moviesRatedVector.size(); k++){
//						String movie_id = users.get(test.get(j)).moviesRatedVector.elementAt(k);
//						data.get("Rate").put(test.get(j) + "," + movie_id, "yes");
//					}
//					// for(String movie_id : users.get(test.get(j)).moviesRated.keySet()){
//					// 	data.get("Rate").put(test.get(j) + "," + movie_id, users.get(test.get(j)).moviesRated.get(movie_id));
//					// }
//				}
//				
//				HashMap<String, String> rnn_preds = rnn.test(data).get(targetPRV);
//				for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
//					double userPred = Double.parseDouble(rnn_preds.get(test.get(j)));
//					userPred = GlobalParams.lambdaForMean * userPred + (1 - GlobalParams.lambdaForMean) * 0.71;
//					predictions.put(test.get(j), "" + userPred);
//				}
//				
//				for(int j = i; j < i + GlobalParams.testBatch && j < test.size(); j++){
//					data.get("Gender").remove(test.get(j));
//					data.get("Age").remove(test.get(j));
//					data.get("Occupation").remove(test.get(j));
//					for(int k = 0; k < Qs[q] && k < users.get(test.get(j)).moviesRatedVector.size(); k++){
//						String movie_id = users.get(test.get(j)).moviesRatedVector.elementAt(k);
//						data.get("Rate").remove(test.get(j) + "," + movie_id);
//					}
//					// for(String movie_id : users.get(test.get(j)).moviesRated.keySet()){
//					// 	data.get("Rate").remove(test.get(j) + "," + movie_id);
//					// }
//				}
//			}
//			System.out.println("Q = " + Qs[q]);
//			measures = new Measures(targets, predictions, targetValue);
//			System.out.println("Accuracy with 0.5 boundary: " + measures.accuracy(0.5));
//			System.out.println("MSE: " + measures.MSE());
//			System.out.println("ACLL: " + measures.ACLL());
//		}
	
		
	}

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("MovieLensGenderLarge");
		for(int i = 0; i < GlobalParams.numRuns; i++){
			MovieLensGender mlg = new MovieLensGender();
			mlg.setHyperParams();
			mlg.readFile();
			mlg.createTrainTestForCV(0);
			mlg.learnModel();
		}
	}
}


class User {
	String id;
	String age;
	String gender;
	String occupation;
	HashMap<String, String> moviesRated;
	Vector<String> moviesRatedVector;
	
	public User(String id){
		this.id = id;
		moviesRated = new HashMap<String, String>();
		moviesRatedVector = new Vector<String>();
	}
}

class Movie {
	String id;
	HashMap<String, String> genre;
	HashMap<String, String> ratedBy;
	
	public Movie(String id){
		this.id = id;
		ratedBy = new HashMap<String, String>();
		genre = new HashMap<String, String>();
	}
}