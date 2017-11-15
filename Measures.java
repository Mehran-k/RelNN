import java.util.HashMap;

public class Measures {
	double[] targets;
	double[] predictions;
	final String targetValue;
	
	public Measures(HashMap<String, String> targets, HashMap<String, String> predictions, String targetValue){
		int index = 0;
		this.targets = new double[targets.keySet().size()];
		this.predictions = new double[targets.keySet().size()];
		this.targetValue = targetValue;
		
		for(String assignment : targets.keySet()){
			if(targetValue.equals("NA!")){
				this.targets[index] = Double.parseDouble(targets.get(assignment));
			}else{
				this.targets[index] = (targets.get(assignment).equals(targetValue) ? 1.0 : 0.0); 
			}
			this.predictions[index] = Double.parseDouble(predictions.get(assignment));
			index++;
		}
	}
	
	public double accuracy(double boundary){
		double correct = 0.0;
		for(int i = 0; i < targets.length; i++){
			if(targets[i] == 1 && predictions[i] >= boundary)
				correct++;
			else if(targets[i] == 0 && predictions[i] < boundary)
				correct++;
		}
		return correct / targets.length;
	}
	
	public double MAE(){
		double mae = 0.0;
		for(int i = 0; i < targets.length; i++){
			if(targets[i] == 1)
				mae += 1 - predictions[i];
			else if(targets[i] == 0)
				mae += predictions[i];
		}
		return mae / targets.length;
	}
	
	private double MSE_Cont(){
		double mse = 0;
		for(int i = 0; i < targets.length; i++){
			mse += Math.pow(predictions[i] - targets[i], 2);
		}
		return mse / targets.length;
	}
	
	public double MSE(){
		if(this.targetValue.equals("NA!"))
			return MSE_Cont();
		double mse = 0.0;
		for(int i = 0; i < targets.length; i++){
			if(targets[i] == 1)
				mse += Math.pow(1 - predictions[i], 2);
			else if(targets[i] == 0)
				mse += Math.pow(predictions[i], 2);
		}
		return mse / targets.length;
	}
	
	public double ACLL(){
		double acll = 0;
		for(int i = 0; i < targets.length; i++){
			if(targets[i] == 1)
				acll += (Math.log10(predictions[i]) / Math.log10(2.0));
			else if(targets[i] == 0)
				acll += (Math.log10(1 - predictions[i]) / Math.log10(2.0));
		}
		return acll / targets.length;
	}
}
