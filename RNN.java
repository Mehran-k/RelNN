import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

public class RNN {
	
	Layer[] layers;
	HashMap<String, HashMap<String, String>> savedState;
	
	public RNN(Layer[] layers){
		this.layers = layers;
		savedState = new HashMap<String, HashMap<String, String>>();
	}
	
	public void saveCurrentState(HashMap<String, HashMap<String, String>> data){
		for(Layer layer : this.layers)
			layer.save_params();
		savedState = new HashMap<String, HashMap<String, String>>();
		for(String prv_name : data.keySet()){
			if(prv_name.startsWith("Feat"))
				savedState.put(prv_name, new HashMap<String, String>(data.get(prv_name)));
		}
	}
	
	public void randomInitialize(HashMap<String, HashMap<String, String>> data){
		for(Layer layer : this.layers)
			layer.assign_random_params();
		for(String prv_name : data.keySet()){
			if(prv_name.startsWith("Feat"))
				for(String assignment : data.get(prv_name).keySet())
					data.get(prv_name).replace(assignment, "" + (Math.random() * GlobalParams.maxRandom - GlobalParams.maxRandom / 2));
		}
	}
	
	public void loadState(HashMap<String, HashMap<String, String>> data){
		for(Layer layer : this.layers)
			layer.use_best_params();
		for(String prv_name : savedState.keySet())
			data.put(prv_name, new HashMap<String, String>(savedState.get(prv_name)));
	}
	
	public double random_walk_initialize(HashMap<String, HashMap<String, String>> data){
		double best_error = 999999999;
		for(int i = 0; i < GlobalParams.numRandomWalk; i++){
			this.randomInitialize(data);
			
			HashMap<String, HashMap<String, String>> output = new HashMap<String, HashMap<String, String>>();
			for(Layer layer : this.layers){
				layer.set_data(data);
				layer.set_inputs(output);
				output = layer.calc_output();
			}
			
			String prvName = (String) output.keySet().toArray()[0];
			double error = output.get(prvName).values().stream().map(value -> Double.parseDouble(value)).mapToDouble(Double::doubleValue).sum();
			
			if(error < best_error){
				System.out.println("Found a better initialization with error: " + error);
				best_error = error;
				this.saveCurrentState(data);
			}
		}
		this.loadState(data);
		return best_error;
	}
	
	public void update_unobserved_inputs(HashMap<String, HashMap<String, String>> data, HashMap<String, HashMap<String, Double>> coming_error){
		for(String prv_name : data.keySet()){
			if(prv_name.startsWith("Feat") && coming_error.containsKey(prv_name)){
				for(String assignment : data.get(prv_name).keySet()){
					double curValue = Double.parseDouble(data.get(prv_name).get(assignment));
					double newValue;
					if(GlobalParams.regularizationTypeForHiddens.equals("L2")){
						newValue = (curValue - GlobalParams.ethaForHiddens * (coming_error.get(prv_name).getOrDefault(assignment, 0.0) + GlobalParams.lambdaForHiddens * curValue));
					}else{
						newValue = (curValue - GlobalParams.ethaForHiddens * coming_error.get(prv_name).getOrDefault(assignment, 0.0));
						newValue = Helper.softMax(newValue, GlobalParams.lambdaForHiddens);
					}
					data.get(prv_name).replace(assignment, "" + newValue);
				}
			}
		}
	}
	
	public double train(HashMap<String, HashMap<String, String>> data, HashMap<String, String> targets){
		this.layers[this.layers.length - 1].set_targets(targets);
		double best_error = 999999999;
		
		for(int q = 0; q < GlobalParams.numRandomRestarts; q++){
			System.out.println("Random Restart #" + q);
			this.randomInitialize(data);
			
			for(int i = 1; i <= GlobalParams.numIteration; i++){
				HashMap<String, HashMap<String, String>> output = new HashMap<String, HashMap<String, String>>();
				for(Layer layer : this.layers){
					layer.set_data(data);
					layer.set_inputs(output);
					output = layer.calc_output();
					if(GlobalParams.debugMode){
						System.out.println(layer.my2String());
						System.out.println(output.toString());
					}
				}
				
				String prvName = (String) output.keySet().toArray()[0];
				double error = output.get(prvName).values().stream().map(value -> Double.parseDouble(value)).mapToDouble(Double::doubleValue).sum();
				System.out.println("Error in iteration #" + i + ": " + error);
				if(error < best_error){
					this.saveCurrentState(data);
					best_error = error;
				}
				
				if(GlobalParams.debugMode)
					System.out.println("Starting the back prop");
				
				HashMap<String, HashMap<String, Double>> coming_error = new HashMap<String, HashMap<String, Double>>();
				for(int j = this.layers.length - 1; j >= 0; j--){
					this.layers[j].calc_parameters_d(coming_error);
					coming_error = this.layers[j].calc_inputs_d(coming_error);
					this.layers[j].update_parameters();
					if(GlobalParams.debugMode){
						System.out.println(this.layers[j].my2String());
						System.out.println(coming_error.toString());
					}
					if(layers[j].layerType.equals("linear"))
						update_unobserved_inputs(data, coming_error);
				}
			}
		}
		
		this.loadState(data);
		return best_error;
	}
	
	public HashMap<String, HashMap<String, String>> test(HashMap<String, HashMap<String, String>> data){
		HashMap<String, HashMap<String, String>> output = new HashMap<String, HashMap<String, String>>();
		for(int i = 0; i < this.layers.length - 1; i++){
			this.layers[i].set_data(data);
			this.layers[i].set_inputs(output);
			output = this.layers[i].calc_output();
		}
		return output;
	}
	
	public void print(){
		for(Layer layer : this.layers){
			System.out.println(layer.my2String());
		}
	}
}
