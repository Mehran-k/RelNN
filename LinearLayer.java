import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;

public class LinearLayer extends Layer{

	RelNeuron[] relNeurons;
	HashMap<String, HashMap<String, String>> data;
	
	public LinearLayer(RelNeuron[] relNeurons){
		this.relNeurons = relNeurons;
		this.layerType = "linear";
	}
	
	@Override
	public void set_data(HashMap<String, HashMap<String, String>> data) {
		// TODO Auto-generated method stub
		this.data = data;
	}
	
	@Override
	public void set_inputs(HashMap<String, HashMap<String, String>> inputs) {
		// TODO Auto-generated method stub
		this.inputs = new HashMap<String, HashMap<String, String>>();
		this.inputs.putAll(inputs);
		this.inputs.putAll(this.data);
	}

	@Override
	public HashMap<String, HashMap<String, String>> calc_output() {
		// TODO Auto-generated method stub
		this.output = new HashMap<String, HashMap<String, String>>();
		for(RelNeuron relNeuron : this.relNeurons){
			this.output.put(relNeuron.name(), relNeuron.evaluate_all(this.inputs));
		}
		return this.output;
	}

	@Override
	public void calc_parameters_d(HashMap<String, HashMap<String, Double>> coming_error) {
		// TODO Auto-generated method stub
		for(RelNeuron relNeuron : this.relNeurons){
			relNeuron.calc_weights_d(coming_error.get(relNeuron.name()));
		}
	}

	@Override
	public void update_parameters() {
		// TODO Auto-generated method stub
		for(RelNeuron relNeuron : this.relNeurons){
			relNeuron.update_weights();
		}
	}
	
	@Override
	public HashMap<String, HashMap<String, Double>> calc_inputs_d(HashMap<String, HashMap<String, Double>> coming_error) {
		// TODO Auto-generated method stub
		this.inputs_d = new HashMap<String, HashMap<String, Double>>();
		for(RelNeuron relNeuron : this.relNeurons){
			for(WeightedFormula wf : relNeuron.wfs){
				if(!wf.isBase()){
					for(Literal literal : wf.literals){
						if(!literal.prv.type.equals("observed_input")){
							HashMap<String, Double> eval_excluding =  wf.evaluate_excluding(this.inputs, coming_error.get(relNeuron.name()), relNeuron.child, literal.logvars(), literal.name());
							if(!this.inputs_d.containsKey(literal.name()))
								this.inputs_d.put(literal.name(), new HashMap<String, Double>());
							for(String assignment : eval_excluding.keySet()){
								double previous_value = this.inputs_d.get(literal.name()).getOrDefault(assignment, 0.0);
								this.inputs_d.get(literal.name()).put(assignment, previous_value + eval_excluding.get(assignment));
							}
						}
					}
				}
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
		for(RelNeuron relNeuron : this.relNeurons){
			for(WeightedFormula wf : relNeuron.wfs){
				wf.weight = Math.random() * GlobalParams.maxRandom - GlobalParams.maxRandom / 2;
			}
		}
	}

	@Override
	public void save_params() {
		// TODO Auto-generated method stub
		for(RelNeuron relNeuron : this.relNeurons){
			for(WeightedFormula wf : relNeuron.wfs){
				wf.best_weight = wf.weight;
			}
		}
	}

	@Override
	public void use_best_params() {
		// TODO Auto-generated method stub
		for(RelNeuron relNeuron : this.relNeurons){
			for(WeightedFormula wf : relNeuron.wfs){
				wf.weight = wf.best_weight;
			}
		}
	}

	@Override
	public String my2String() {
		// TODO Auto-generated method stub
		String output = "This is a linear layer with the following relational neurons:\n";
		output += String.join("\n", Arrays.stream(this.relNeurons).map(relNeuron -> relNeuron.my2String()).collect(Collectors.toList()));
		return output;
	}
}
