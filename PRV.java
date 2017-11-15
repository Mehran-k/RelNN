import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class PRV {
	String name;
	LogVar[] logvars;
	String type; //observed_input, unobserved_input, hidden
	
	public PRV(String name, LogVar[] logvars, String type){
		this.name = name;
		this.logvars = logvars;
		this.type = type;
	}
	
	public Literal lit(String value){
		return new Literal(this, value);
	}
	
	public HashMap<String, String> randomValues(){ //This function is only used for unobserved inputs. Currently, assuming only unary unobserved inputs.
		HashMap<String, String> values = new HashMap<String, String>();
		if(logvars.length == 1){
			for(String individual : logvars[0].individuals)
				if(GlobalParams.debugMode)
					values.put(individual, "-1");
				else
					values.put(individual, (Math.random() * 2 - 1) + "");
		}
		else{
			System.out.println("This version of the code does not support non-unary unobserved inputs!\nSee PRV->random_values().");
			System.exit(1);
		}
		return values;
	}
	
	public String my2String(){
		return (name + "(" + String.join(",", Arrays.stream(logvars).map(lv -> lv.name).collect(Collectors.toList())) + ")");  
	}
}
