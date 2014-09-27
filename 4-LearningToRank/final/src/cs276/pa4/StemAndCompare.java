package cs276.pa4;

import java.util.Map;

import stemmer.PorterStemmer;

public class StemAndCompare {

	private static PorterStemmer stemmer = new PorterStemmer();

	public static String StemString(String input) {
		stemmer.setCurrent(input);
		stemmer.stem();
		return stemmer.getCurrent();
	}

	public static String Compare(Map<String, Integer> fieldMap, String queryTerm) {
		for (String entry : fieldMap.keySet()) {
			if (StemString(entry).equals(StemString(queryTerm))) {
				return entry;
			}
		}
		return null;
	}
}
