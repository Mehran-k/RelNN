import java.util.Comparator;
import java.util.HashMap;

public class Literal implements Comparable<Literal>{//sortable based on the number of logical variables
	PRV prv;
	String value;
	
	public Literal(PRV prv, String value){
		this.prv = prv;
		this.value = value;
	}
	
	public String name(){
		return prv.name;
	}
	
	public LogVar[] logvars(){
		return prv.logvars;
	}
	
	public double[] eval_all(HashMap<String, String> data_hash, LogVar[] allLVs, String[] allAssignments){
		double[] output = new double[allAssignments.length];
		int[] lvIndexes = Helper.get_lv_indexes(allLVs, logvars());
		
		for(int i = 0; i < allAssignments.length; i++){
			String litAssignment = pruneAssignment(allAssignments[i], lvIndexes);
			output[i] = evaluateWithValue(data_hash.getOrDefault(litAssignment, "false"));
		}
		return output;
	}
	
	public double evaluateWithValue(String value){
		if(this.value.equals("NA!"))
			return Double.parseDouble(value);
		double v;
		try {
			v = (value.equals(this.value) ? 1.0 : 0.0);
		} catch (Exception e) {
			System.out.println(this.my2String());
			System.out.println(value);
		}
		return (value.equals(this.value) ? 1.0 : 0.0);	
	}
	
	private String pruneAssignment(String assignment, int[] lvIndexes){
		String[] spl = assignment.split(",");
		String[] result = new String[lvIndexes.length];
		for(int i = 0; i < lvIndexes.length; i++)
			result[i] = spl[lvIndexes[i]];
		return String.join(",", result);
	}
	
	public String my2String(){
		return this.prv.my2String() + (!value.equals("NA!") ? ("=" + this.value) : "");
	}

	@Override
	public int compareTo(Literal arg0) {
		return arg0.logvars().length - this.logvars().length;
	}
}
