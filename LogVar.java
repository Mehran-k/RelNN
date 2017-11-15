public class LogVar {
	String name;
	String[] individuals;
	String type;
	
	public LogVar(String name, String[] individuals, String type){
		this.name = name;
		this.individuals = individuals;
		this.type = type;
	}
	
	public LogVar(LogVar lv){
		this.name = lv.name;
		this.individuals = lv.individuals;
		this.type = lv.type;
	}
	
	public int psize(){
		return individuals.length;
	}
}
