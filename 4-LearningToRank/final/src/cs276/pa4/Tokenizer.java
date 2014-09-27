package cs276.pa4;

public class Tokenizer {
	public static String[] TokenizeOnSpaces(String s) {
		String[] retVal = s.trim().split("\\s+");
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = retVal[i].toLowerCase();
		}
		return retVal;
	}

	public static String[] TokenizeOnNonAlphanumeric(String s) {
		String[] retVal = s.trim().split("\\W+");
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = retVal[i].toLowerCase();
		}
		return retVal;
	}
}
