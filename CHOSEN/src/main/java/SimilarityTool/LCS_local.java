package SimilarityTool;

public class LCS_local {

	public static String maxSubstring(String strOne, String strTwo){
		// 参数检查
		if(strOne==null || strTwo == null){
			return null;
		}
		if(strOne.equals("") || strTwo.equals("")){
			return null;
		}
		// 二者中较长的字符串
		String max = "";
		// 二者中较短的字符串
		String min = "";
		if(strOne.length() < strTwo.length()){
			max = strTwo;
			min = strOne;
		} else{
			max = strTwo;
			min = strOne;
		}
		String current = "";
		// 遍历较短的字符串，并依次减少短字符串的字符数量，判断长字符是否包含该子串
		for(int i=0; i<min.length(); i++){
			for(int begin=0, end=min.length()-i; end<=min.length(); begin++, end++){
				current = min.substring(begin, end);
				if(max.contains(current)){
					return current;
				}
			}
		}
		return null;
	}

	public static double MaxRatio(String strOne, String strTwo){
		var result=LCS_local.maxSubstring(strOne,strTwo);
		if(result==null || result.length()==0){
			return 0;
		}
		return (double)result.length()/Math.max(strOne.length(),strTwo.length());
	}

	public static void main(String[] args) {
		String strOne = "normalization";
		String strTwo = "normalize";
		String result = LCS_local.maxSubstring(strOne, strTwo);
		System.out.println(result);
	}

}