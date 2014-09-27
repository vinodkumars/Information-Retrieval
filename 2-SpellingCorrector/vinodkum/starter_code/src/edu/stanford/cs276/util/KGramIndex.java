package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KGramIndex implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<String, List<String>> index;
	private final double jcThresholdLow = 0.2;
	private final double jcThresholdHigh = 0.4;
	private final int split = 6;

	public KGramIndex() {
		index = new HashMap<String, List<String>>();
	}

	private List<String> GetKGrams(String term) {
		List<String> retVal = new ArrayList<String>();
		String term1 = "$" + term + "$";
		for (int i = 0; i < term1.length() - 1; i++) {
			retVal.add(term1.substring(i, i + 2));
		}
		return retVal;
	}

	public void Add(String term) {
		List<String> KGrams = GetKGrams(term);
		for (String KGram : KGrams) {
			if (index.containsKey(KGram)) {
				index.get(KGram).add(term);
			} else {
				List<String> postingList = new ArrayList<String>();
				postingList.add(term);
				index.put(KGram, postingList);
			}
		}
	}

	public List<String> GetNearestTerms(String term) {
		List<String> retVal = new ArrayList<String>();
		List<String> termKGrams = GetKGrams(term);
		Set<String> postingsSet = new HashSet<String>();

		for (String KGram : termKGrams) {
			if (!index.containsKey(KGram)) {
				continue;
			}
			postingsSet.addAll(index.get(KGram));
		}

		for (String posting : postingsSet) {
			if (retVal.contains(posting)) {
				continue;
			}

			List<String> postingKGrams = GetKGrams(posting);
			Set<String> union = new HashSet<>();
			union.addAll(termKGrams);
			union.addAll(postingKGrams);
			Set<String> intersection = new HashSet<>();
			intersection.addAll(termKGrams);
			intersection.retainAll(postingKGrams);

			double jc = (((double) intersection.size()) / ((double) union
					.size()));
			double jcThreshold = union.size() <= split ? jcThresholdLow
					: jcThresholdHigh;
			if (jc >= jcThreshold) {
				if (!retVal.contains(posting)) {
					retVal.add(posting);
				}
			}
		}

		return retVal;
	}
}
