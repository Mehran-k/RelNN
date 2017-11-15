import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class WeightedFormula {
	Literal[] literals;
	double weight;
	HashMap <String, Double> cache;
	double best_weight;
	Boolean all_literals_observed;
	Boolean aggregates;
	
	public WeightedFormula(Literal[] literals, double weight){
		this.literals = literals;
		Arrays.sort(this.literals);
		this.weight = weight;
		best_weight = weight;
		cache = new HashMap<String, Double>();
		all_literals_observed = !Arrays.stream(literals).map(lit -> lit.prv.type.equals("observed_input")).collect(Collectors.toList()).contains(false);
	}
	
	public WeightedFormula(WeightedFormula wf){
		this.literals = wf.literals;
		this.weight = wf.weight;
		this.best_weight = weight;
		this.cache = new HashMap<String, Double>();
		this.all_literals_observed = wf.all_literals_observed;
	}
	
	public Boolean isBase(){
		return (literals.length == 0 ? true : false);
	}
	
	public String[] lvNames(){
		if(isBase())
			return (new String[0]); 
		String lvs = "";
		for(Literal literal : this.literals){
			for(LogVar lv : literal.prv.logvars){
				lvs += lv.name + ",";
			}
		}
		lvs = lvs.substring(0, lvs.length() - 1);
		return lvs.split(",");
	}
	
	public void setAggregates(String childLVNames){
		this.aggregates = false;
		for(String name : lvNames()){
			if(childLVNames.indexOf(name) < 0)
				aggregates = true;
		}
	}
	
	public HashMap<String, Double> unweighted_evaluate(HashMap<String, HashMap<String, String>> data_hash, LogVar[] fixed_lvs){//for non-base WFs.
		if(all_literals_observed && !cache.isEmpty() && GlobalParams.learningStatus == "train")
			return cache;
	
		Object[] obj = Helper.join_all(data_hash, this.literals);
		String[] assignments = (String[]) obj[0];
		LogVar[] lvOrder = (LogVar[]) obj[1];
		
		double[] values = this.literals[0].eval_all(data_hash.get(this.literals[0].name()), lvOrder, assignments);
		for(int i = 1; i < this.literals.length; i++){
			values = Helper.dot_product(values, this.literals[i].eval_all(data_hash.get(this.literals[i].name()), lvOrder, assignments));
		}
		int[] group_on_indexes = Helper.get_lv_indexes(lvOrder, fixed_lvs);
		cache = Helper.group_by(assignments, values, lvOrder, group_on_indexes);
		return cache;
	}
	
	public HashMap<String, Double> evaluate(HashMap<String, HashMap<String, String>> data_hash, LogVar[] fixed_lvs){
		HashMap<String, Double> result = new HashMap<String, Double>(unweighted_evaluate(data_hash, fixed_lvs));
		for(String assignment : result.keySet())
			result.replace(assignment, result.get(assignment) * this.weight);
		return result;
	}
	
	public HashMap<String, Double> evaluate_excluding(HashMap<String, HashMap<String, String>> data_hash, HashMap<String, Double> coming_error, PRV child, LogVar[] fixed_lvs, String excluded_prv_name){
		HashMap<String, HashMap<String, String>> joined_data = new HashMap<String, HashMap<String, String>>(data_hash);
		joined_data.put(child.name, Helper.doubleHashMap2String(coming_error));
		
		Literal[] included_literals = new Literal[this.literals.length];
		int index = 1;
		for(Literal literal : this.literals)
			if(!literal.name().equals(excluded_prv_name))
				included_literals[index++] = literal;
		included_literals[0] = child.lit("NA!");
		Arrays.sort(included_literals);
		
		Object[] obj = Helper.join_all(joined_data, included_literals);
		String[] assignments = (String[]) obj[0];
		LogVar[] lv_order = (LogVar[]) obj[1];
		
		double[] values = included_literals[0].eval_all(joined_data.get(included_literals[0].name()), lv_order, assignments);
		for(int i = 1; i < included_literals.length; i++){
			values = Helper.dot_product(values, included_literals[i].eval_all(joined_data.get(included_literals[i].name()), lv_order, assignments));
		}
		
		int[] group_on_indexes = Helper.get_lv_indexes(lv_order, fixed_lvs);
		HashMap<String, Double> result = Helper.group_by(assignments, values, lv_order, group_on_indexes);

		for(String assignment : result.keySet())
			result.replace(assignment, result.get(assignment) * this.weight);
		return result;
	}
	
	public HashMap<String, Double> evaluate_excluding_tanh(HashMap<String, HashMap<String, String>> data_hash, HashMap<String, Double> coming_error, PRV child, LogVar[] fixed_lvs, String excluded_prv_name, double c){
		HashMap<String, HashMap<String, String>> joined_data = new HashMap<String, HashMap<String, String>>(data_hash);
		joined_data.put(child.name, Helper.doubleHashMap2String(coming_error));
		
		HashMap<String, String> cache_values = new HashMap<String, String>();
		for(String assignment : this.cache.keySet()){
			double new_value = Math.pow(Helper.sech(c * cache.get(assignment)), 2);
			cache_values.put(assignment, new_value + "");
		}
		joined_data.put("Sech", cache_values);
		
		Literal[] included_literals = new Literal[this.literals.length + 1];
		included_literals[0] = child.lit("NA!");
		included_literals[1] = new Literal(new PRV("Sech", child.logvars, child.type), "NA!");
		
		int index = 2;
		for(Literal literal : this.literals)
			if(!literal.name().equals(excluded_prv_name))
				included_literals[index++] = literal;
		
		Arrays.sort(included_literals);
		
		Object[] obj = Helper.join_all(joined_data, included_literals);
		String[] assignments = (String[]) obj[0];
		LogVar[] lv_order = (LogVar[]) obj[1];
		
		double[] values = included_literals[0].eval_all(joined_data.get(included_literals[0].name()), lv_order, assignments);
		for(int i = 1; i < included_literals.length; i++){
			double[] temp = included_literals[i].eval_all(joined_data.get(included_literals[i].name()), lv_order, assignments);
			values = Helper.dot_product(values, temp);
		}
		
		int[] group_on_indexes = Helper.get_lv_indexes(lv_order, fixed_lvs);
		HashMap<String, Double> result = Helper.group_by(assignments, values, lv_order, group_on_indexes);

		for(String assignment : result.keySet())
			result.replace(assignment, result.get(assignment) * this.weight * c);
		return result;
	}
	
	public double calc_c_d(HashMap<String, Double> coming_error, PRV child, double c){
		HashMap<String, HashMap<String, String>> joined_data = new HashMap<String, HashMap<String, String>>();
		joined_data.put(child.name, Helper.doubleHashMap2String(coming_error));
		
		HashMap<String, String> cache_values = new HashMap<String, String>();
		for(String assignment : this.cache.keySet()){
			double new_value = Math.pow(Helper.sech(c * cache.get(assignment)), 2);
			cache_values.put(assignment, new_value + "");
		}
		joined_data.put("Sech", cache_values);
		joined_data.put("Cache", Helper.doubleHashMap2String(new HashMap<String, Double>(this.cache)));
		
		Literal[] included_literals = new Literal[3];
		included_literals[0] = child.lit("NA!");
		included_literals[1] = new Literal(new PRV("Sech", child.logvars, child.type), "NA!");
		included_literals[2] = new Literal(new PRV("Cache", child.logvars, child.type), "NA!");
		
		Object[] obj = Helper.join_all(joined_data, included_literals);
		String[] assignments = (String[]) obj[0];
		LogVar[] lv_order = (LogVar[]) obj[1];
		
		double[] values = included_literals[0].eval_all(joined_data.get(included_literals[0].name()), lv_order, assignments);
		for(int i = 1; i < included_literals.length; i++){
			values = Helper.dot_product(values, included_literals[i].eval_all(joined_data.get(included_literals[i].name()), lv_order, assignments));
		}
		return this.weight * DoubleStream.of(values).sum();
	}
	
	public String my2String(){
		String formula = (literals.length == 0 ? "True" : String.join("*", Arrays.stream(literals).map(lit -> lit.my2String()).collect(Collectors.toList())));
		return formula + " : " + this.weight;
	}
}
