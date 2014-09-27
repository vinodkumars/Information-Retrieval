package edu.stanford.cs276.util;

import java.util.List;

import edu.stanford.cs276.Config;
import edu.stanford.cs276.EditCostModel;
import edu.stanford.cs276.LanguageModel;

public class ProbabilityUtility {

	public static double GetProbOfQ(String q, Dictionary oneTermDict,
			Dictionary twoTermDict) {
		double retVal = 0;
		String[] qTerms = q.trim().split("\\s+");

		retVal += Math.log((double) oneTermDict.count(qTerms[0])
				/ (double) oneTermDict.termCount());

		for (int i = 0; i < qTerms.length - 1; i++) {
			double probI = (double) oneTermDict.count(qTerms[i])
					/ (double) oneTermDict.termCount();
			double probIIPlus = (double) twoTermDict.count(qTerms[i] + "|"
					+ qTerms[i + 1])
					/ (double) oneTermDict.count(qTerms[i]);
			retVal += Math.log((Config.lambda * probI) + ((1 - Config.lambda) * probIIPlus));
		}
		return retVal;
	}

	public static double GetCandidateScore(String q, String r, List<EditType> edits,
			LanguageModel lm, EditCostModel ecm) {
		double retVal = 0;
		retVal += ecm.editProbability(q, r, edits);
		retVal += (Config.mew * GetProbOfQ(q, lm.oneTermDict, lm.twoTermDict));
		return retVal;
	}

}
