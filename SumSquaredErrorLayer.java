import java.util.HashMap;

public class SumSquaredErrorLayer extends Layer{

	HashMap<String, String> targets;
	String value_to_predict;
	
	public SumSquaredErrorLayer(String value_to_predict){
		this.value_to_predict = value_to_predict;
		this.layerType = "sum_squared_error";
	}
	
	@Override
	public void set_data(HashMap<String, HashMap<String, String>> data) {
		// TODO Auto-generated method stub
	}

	@Override
	public void set_inputs(HashMap<String, HashMap<String, String>> inputs) {
		// TODO Auto-generated method stub
		this.inputs = inputs;
	}

	@Override
	public HashMap<String, HashMap<String, String>> calc_output() {
		// TODO Auto-generated method stub
		this.output = new HashMap<String, HashMap<String, String>>();
		String prvName = (String) this.inputs.keySet().toArray()[0];
		this.output.put(prvName, new HashMap<String, String>());
		
		if(value_to_predict.equals("NA!")){
			for(String assignment : this.inputs.get(prvName).keySet()){
				double error = 0.5 * Math.pow(Double.parseDouble(this.inputs.get(prvName).get(assignment)) - Double.parseDouble(this.targets.get(assignment)), 2);
				this.output.get(prvName).put(assignment, "" + error);
			}
		}else{
			for(String assignment : this.inputs.get(prvName).keySet()){
				double error = 0.5 * Math.pow((Double.parseDouble(this.inputs.get(prvName).get(assignment)) - (this.targets.getOrDefault(assignment, "false").equals(this.value_to_predict) ? 1.0 : 0.0)), 2);
				this.output.get(prvName).put(assignment, "" + error);
			}
		}
		
		return this.output;
	}

	@Override
	public void calc_parameters_d(HashMap<String, HashMap<String, Double>> coming_error) {
		// TODO Auto-generated method stub
	}

	@Override
	public void update_parameters() {
		// TODO Auto-generated method stub
	}

	@Override
	public HashMap<String, HashMap<String, Double>> calc_inputs_d(HashMap<String, HashMap<String, Double>> coming_error) {
		// TODO Auto-generated method stub
		this.inputs_d = new HashMap<String, HashMap<String, Double>>();
		String prvName = (String) this.inputs.keySet().toArray()[0];
		this.inputs_d.put(prvName, new HashMap<String, Double>());
		
		if(value_to_predict.equals("NA!")){
			for(String assignment : this.inputs.get(prvName).keySet()){
				double error = Double.parseDouble(this.inputs.get(prvName).get(assignment)) - Double.parseDouble(this.targets.get(assignment));
				this.inputs_d.get(prvName).put(assignment, error);
			}
		}else{
			for(String assignment : this.inputs.get(prvName).keySet()){
				double error = Double.parseDouble(this.inputs.get(prvName).get(assignment)) - (this.targets.getOrDefault(assignment, "false").equals(this.value_to_predict) ? 1.0 : 0.0);
				this.inputs_d.get(prvName).put(assignment, error);
			}
		}
		
		return this.inputs_d;
	}

	@Override
	public void set_targets(HashMap<String, String> targets) {
		// TODO Auto-generated method stub
		this.targets = targets;
	}

	@Override
	public void assign_random_params() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void save_params() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void use_best_params() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String my2String() {
		// TODO Auto-generated method stub
		return "This is a sum of squared errors layer!\n";
	}

}
