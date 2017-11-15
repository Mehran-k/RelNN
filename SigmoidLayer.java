import java.util.HashMap;

public class SigmoidLayer extends Layer{

	public SigmoidLayer(){
		this.layerType = "sigmoid";
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
		for(String prv_name : this.inputs.keySet()){
			this.output.put(prv_name, new HashMap<String, String>());
			for(String assignment : this.inputs.get(prv_name).keySet()){
				this.output.get(prv_name).put(assignment, "" + Helper.sigmoid(Double.parseDouble(this.inputs.get(prv_name).get(assignment))));
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
		for(String prv_name : this.inputs.keySet()){
			this.inputs_d.put(prv_name, new HashMap<String, Double>());
			for(String assignment : this.inputs.get(prv_name).keySet()){
				double sig = Double.parseDouble(this.output.get(prv_name).get(assignment));
//				System.out.println(prv_name + " " + assignment);
//				System.out.println(coming_error.toString());
//				System.out.println(prv_name);
				this.inputs_d.get(prv_name).put(assignment, sig * (1 - sig) * coming_error.get(prv_name).get(assignment));
			}
		}
		return this.inputs_d;
	}

	@Override
	public void set_targets(HashMap<String, String> targets) {
		// TODO Auto-generated method stub
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
		return "This is a Sigmoid Layer!\n";
	}
}
