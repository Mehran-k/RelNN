import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class Helper {
	
	public static double myLog(double x){
		if(x < 0.0001)
			return -10;
		return Math.log(x);
	}
	
	public static double[] filled_double_array(int n, double value){
		double[] temp = new double[n];
		Arrays.fill(temp, value);
		return temp;
	}
	
	public static double sech(double x){
		return 1.0 / Math.cosh(x);
	}
	
	public static double[] dot_product(double[] arr1, double[] arr2){
		double[] result = new double[arr1.length];
		for(int i = 0; i < arr1.length; i++)
			result[i] = arr1[i] * arr2[i];
		return result;
	}
	
	public static double sigmoid(double x){
		return 1.0 / (1 + Math.exp(-x));
	}
	
	public static void printStrArr(String[] s){
		System.out.println(String.join(", ", s));
	}
	
	public static void print2Darray(String[][] s){
		for(int i = 0; i < s.length; i++){
			System.out.println(String.join(", ", s[i]));
		}
	}
	
	public static String[] merge(String[] a, String[] b){
		return (String.join(",", a) + "," + String.join(",", b)).split(",");
	}
	
	public static ArrayList<String> crossProduct(ArrayList<String> al1, ArrayList<String> al2, String append){
		if(al1.size() == 0 || al2.size() == 0)
			return new ArrayList<String>();
		ArrayList<String> temp = new ArrayList<String>(al1.size() * al2.size());
		for(String s1 : al1){
			for(String s2 : al2){
				temp.add(append + "," + s1 + "," + s2);
			}
		}
		return temp;
	}
	
	public static Object[] join(String[] s1, String[] s2, LogVar[] lvs1, LogVar[] lvs2){
		if(lvs1.length <= 1)
			return new Object[]{s2, lvs2};
		
		if(lvs2.length <= 1)
			return new Object[]{s1, lvs1};
		
		if(lvs1.length == 2 && lvs2.length == 2 && lvs1[0].name.equals(lvs2[0].name) && lvs1[1].name.equals(lvs2[1].name))
			return new Object[]{s1, lvs1};
		
		if(lvs1.length == 2 && lvs2.length == 2){
			int index1 = 0, index2 = 0;
			LogVar[] lv_order = new LogVar[]{lvs1[0], lvs1[1], lvs2[1]};
			if(lvs1[0].name.equals(lvs2[1].name)){
				index2 = 1;
				lv_order = new LogVar[]{lvs1[0], lvs1[1], lvs2[0]};
			}else if(lvs1[1].name.equals(lvs2[0].name)){
				index1 = 1;
				lv_order = new LogVar[]{lvs1[1], lvs1[0], lvs2[1]};
			}else if(lvs1[1].name.equals(lvs2[1].name)){
				index1 = 1;
				index2 = 1;
				lv_order = new LogVar[]{lvs1[1], lvs1[0], lvs2[0]};
			}
			
			HashMap<String, ArrayList<String>> temp1 = new HashMap<String, ArrayList<String>>(), temp2 = new HashMap<String, ArrayList<String>>();
			int finalSize = 0;
			for(String s : s1){
				String[] spl = s.split(",");
				if(!temp1.containsKey(spl[index1]))
					temp1.put(spl[index1], new ArrayList<String>(100));
				temp1.get(spl[index1]).add(spl[1-index1]);
			}
			
			for(String s : s2){
				String[] spl = s.split(",");
				if(!temp2.containsKey(spl[index2]))
					temp2.put(spl[index2], new ArrayList<String>(100));
				temp2.get(spl[index2]).add(spl[1-index2]);
				
				finalSize += temp1.getOrDefault(spl[index2], new ArrayList<String>()).size();
			}	
			ArrayList<String> result = new ArrayList<String>(finalSize);
			for(String key : temp1.keySet()){
				result.addAll(crossProduct(temp1.getOrDefault(key, new ArrayList<String>()), temp2.getOrDefault(key, new ArrayList<String>()), key));
			}
			return new Object[]{result.toArray(new String[result.size()]), lv_order};
		}
		if(lvs1.length == 3 && lvs2.length == 2){
			//For now, only consider the case where the 2 logvars are a subset of the 3
			HashMap<String, Boolean> s2Hash = new HashMap<String, Boolean>();
			for(String s : s2)
				s2Hash.put(s, true);
			ArrayList<String> result = new ArrayList<String>();
			
			int index1 = 0, index2 = 1;
			
			if(lvs1[0].name.equals(lvs2[0].name) && lvs1[1].name.equals(lvs2[1].name))
				;
			else if(lvs1[0].name.equals(lvs2[0].name) && lvs1[2].name.equals(lvs2[1].name))
				index2 = 2;
			else if(lvs1[1].name.equals(lvs2[0].name) && lvs1[2].name.equals(lvs2[1].name)){
				index1 = 1; index2 = 2;
			}
			else{
				System.out.println("The code currently does not support your WFs. See Statics.join()!");
				System.exit(1);
			}
			
			for(String s : s1){
				String[] spl = s.split(",");
				if(s2Hash.containsKey(spl[index1] + "," + spl[index2]))
					result.add(s);
			}
			return new Object[]{result.toArray(new String[result.size()]), lvs1};
		}
		System.out.println("The code currently does not support your WFs. See Statics.join()!");
		System.exit(1);
		return null;
	}
	
	public static Object[] join_all(HashMap<String, HashMap<String, String>> data, Literal[] literals){
		Set<String> keys_base = data.get(literals[0].name()).keySet();
		String[] s_base = keys_base.toArray(new String[keys_base.size()]);
		LogVar[] lvs_base = literals[0].logvars();
		for(int i = 1; i < literals.length; i++){
			Set<String> to_append_keys = data.get(literals[i].name()).keySet();
			String[] to_append_s = to_append_keys.toArray(new String[to_append_keys.size()]);
			LogVar[] to_append_lvs = literals[i].logvars();
			Object[] obj = join(s_base, to_append_s, lvs_base, to_append_lvs);
			s_base = (String[]) obj[0];
			lvs_base = (LogVar[]) obj[1];
		}
		return new Object[]{s_base, lvs_base};
	}
	
	public static HashMap<String, Double> group_by(String[] keys, double[] values, LogVar[] lv_order, int[] group_on_indexes){
		HashMap<String, Double> result = new HashMap<String, Double>();
		if(lv_order.length == group_on_indexes.length){
			for(int i = 0; i < keys.length; i++)
				result.put(keys[i], values[i]);
			return result;
		}
		if(lv_order.length == 2 && group_on_indexes.length == 1){
			for(int i = 0; i < keys.length; i++){
				String[] spl = keys[i].split(",");
				String curKey = spl[group_on_indexes[0]];
				double newValue = result.getOrDefault(curKey, 0.0) + values[i];
				result.put(curKey, newValue);
			}
			return result;
		}
		if(lv_order.length == 3 && group_on_indexes.length == 1){
			for(int i = 0; i < keys.length; i++){
				String[] spl = keys[i].split(",");
				String curKey = spl[group_on_indexes[0]];
				double newValue = result.getOrDefault(curKey, 0.0) + values[i];
				result.put(curKey, newValue);
			}
			return result;
		}
		if(lv_order.length == 3 && group_on_indexes.length == 2){
			for(int i = 0; i < keys.length; i++){
				String[] spl = keys[i].split(",");
				String curKey = spl[group_on_indexes[0]] + "," + spl[group_on_indexes[1]];
				double newValue = result.getOrDefault(curKey, 0.0) + values[i];
				result.put(curKey, newValue);
			}
			return result;
		}
		System.out.println("The code currently does not support your WFs. See Helper.group_by()!");
		return null;
	}
	
	public static int[] get_lv_indexes(LogVar[] superset, LogVar[] subset){
		int[] result = new int[subset.length];
		
		for(int i = 0; i < subset.length; i++){
			for(int j = 0; j < superset.length; j++){
				if(subset[i].name.equals(superset[j].name))
					result[i] = j;
			}
		}
		return result;
	}
	
	public static HashMap<String, String> doubleHashMap2String(HashMap<String, Double> hm){
		HashMap<String, String> result = new HashMap<String, String>();
		for(String key : hm.keySet()){
			result.put(key, hm.get(key) + "");
		}
		return result;
	}
	
	public static void printHasMap(HashMap<String, String> hash){
		for(String key : hash.keySet()){
			System.out.println(key + " -> " + hash.get(key));
		}
	}
	
	public static String doubleArr2String(double[] d){
		String result = "";
		for(int i = 0; i < d.length; i++){
			result += d[i];
			if(i < d.length - 1)
				result += ", ";
		}
		return result;
	}
	
	public static double hashMapSum(HashMap<String, Double> h){
		return h.values().stream().mapToDouble(d -> d).sum();
	}
	
	public static double softMax(double d, double lambda){
		if(d >= 0)
			return Math.max(0, d - lambda);
		return Math.min(0, d + lambda);
	}
	
	public static String[] lvNames(LogVar[] lvs){
		String[] result = new String[lvs.length];
		for(int i = 0; i < lvs.length; i++)
			result[i] = lvs[i].name;
		return result;
	}
}
