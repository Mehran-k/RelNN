import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class RelNeuron {
	PRV child;
	WeightedFormula[] wfs;
	double[] weights_d;
	
	public RelNeuron(PRV child, WeightedFormula[] wfs){ 
		this.child = child;
		this.wfs = new WeightedFormula[wfs.length];
		int index = 0;
		for(int i = 0; i < wfs.length; i++){//to make sure the base is the last WF, and create new WFs.
			if(wfs[i].isBase())
				this.wfs[wfs.length - 1] = new WeightedFormula(wfs[i]);
			else
				this.wfs[index++] = new WeightedFormula(wfs[i]);
		}
		for(WeightedFormula wf : this.wfs){
			String childLVNames = String.join(",", Helper.lvNames(child.logvars));
			wf.setAggregates(childLVNames);
		}
	}
	
	public LogVar[] logvars(){
		return child.logvars;
	}
	
	public String name(){
		return child.name;
	}
	
	public HashMap<String, String> evaluate_all(HashMap<String, HashMap<String, String>> data_hash){
		HashMap<String, String> output = new HashMap<String, String>();
		
		for(WeightedFormula wf : this.wfs){
			if(wf.isBase()){
				for(String assignment : output.keySet()){
					double newValue = Double.parseDouble(output.get(assignment)) + wf.weight;
					output.replace(assignment, newValue + "");
				}
			}else{
				HashMap<String, Double> wf_eval = wf.unweighted_evaluate(data_hash, this.logvars());
				for(String assignment : wf_eval.keySet()){
					double newValue = Double.parseDouble(output.getOrDefault(assignment, "0.0")) + wf_eval.get(assignment) * wf.weight;
					output.put(assignment, newValue + "");
				}
			}
		}
		return output;
	}
	
	public HashMap<String, String> evaluate_all_tanh(HashMap<String, HashMap<String, String>> data_hash, double[] c){
		HashMap<String, String> output = new HashMap<String, String>();
		
		int index = 0;
		for(WeightedFormula wf : this.wfs){
			if(wf.isBase()){
				double tanh = Math.tanh(c[index]);
				for(String assignment : output.keySet()){
					double newValue = Double.parseDouble(output.get(assignment)) + tanh * wf.weight;
					output.replace(assignment, newValue + "");
				}
			}else{
				HashMap<String, Double> wf_eval = wf.unweighted_evaluate(data_hash, this.logvars());
				for(String assignment : wf_eval.keySet()){
					double newValue = Double.parseDouble(output.getOrDefault(assignment, "0.0")) + Math.tanh(c[index] * wf_eval.get(assignment)) * wf.weight;
					output.put(assignment, newValue + "");
				}
			}
			index++;
		}
		return output;
	}
	
	public void calc_weights_d(HashMap<String, Double> coming_error){
		this.weights_d = new double[this.wfs.length];
		
		for(String assignment : coming_error.keySet()){
			double assignment_coming_error = coming_error.get(assignment);
			for(int j = 0; j < this.wfs.length; j++){
				if(!this.wfs[j].isBase()){
					this.weights_d[j] += this.wfs[j].cache.getOrDefault(assignment, 0.0) * assignment_coming_error;
				}else{
					this.weights_d[j] += assignment_coming_error;
				}
			}
		}
	}
	
	public void calc_weights_d_tanh(HashMap<String, Double> coming_error, double[] c){
		this.weights_d = new double[this.wfs.length];
		
		for(String assignment : coming_error.keySet()){
			double assignment_coming_error = coming_error.get(assignment);
			for(int j = 0; j < this.wfs.length; j++){
				if(!this.wfs[j].isBase()){
					this.weights_d[j] += Math.tanh(c[j] * this.wfs[j].cache.getOrDefault(assignment, 0.0)) * assignment_coming_error;
				}else{
					this.weights_d[j] += Math.tanh(c[j]) * assignment_coming_error;
				}
			}
		}
	}
	
	public void update_weights(){
		for(int i = 0; i < this.wfs.length; i++){
			if(GlobalParams.regularizationTypeForWeights.equals("L2"))
				this.wfs[i].weight -= GlobalParams.ethaForWeights * (this.weights_d[i] + GlobalParams.lambdaForWeights * this.wfs[i].weight);
			else{
				this.wfs[i].weight -= GlobalParams.ethaForWeights * this.weights_d[i];
				this.wfs[i].weight = Helper.softMax(this.wfs[i].weight, GlobalParams.lambdaForWeights);
			}
		}
	}
	
	public String my2String(){
		String result = "CondProb for PRV " + child.my2String() + " has the following WFs: \n";
		result += String.join("\n", Arrays.stream(this.wfs).map(wf -> wf.my2String()).collect(Collectors.toList()));
		return result;
	}
}
