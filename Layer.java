import java.util.HashMap;

public abstract class Layer {
	String layerType;
	HashMap<String, HashMap<String, String>> inputs;
	HashMap<String, HashMap<String, String>> output;
	HashMap<String, HashMap<String, Double>> inputs_d;
	
	public abstract void set_data(HashMap<String, HashMap<String, String>> data);
	
	public abstract void set_inputs(HashMap<String, HashMap<String, String>> inputs);
	
	public abstract void set_targets(HashMap<String, String> targets);
	
	public abstract HashMap<String, HashMap<String, String>> calc_output();
	
	public abstract void calc_parameters_d(HashMap<String, HashMap<String, Double>> coming_error);
	
	public abstract void update_parameters();
	
	public abstract HashMap<String, HashMap<String, Double>> calc_inputs_d(HashMap<String, HashMap<String, Double>> coming_error);
	
	public abstract void assign_random_params();
	
	public abstract void save_params();
	
	public abstract void use_best_params();
	
	public abstract String my2String();
	
	public void print_output(){
		for(String prv_name : output.keySet()){
			System.out.println("For relational neuron: " + prv_name);
			for(String assignment : output.get(prv_name).keySet()){
				System.out.println(assignment + ": " + output.get(prv_name).get(assignment));
			}
		}
	}
}
