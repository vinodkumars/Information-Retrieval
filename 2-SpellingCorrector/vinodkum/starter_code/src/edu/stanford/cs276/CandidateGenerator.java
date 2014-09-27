package edu.stanford.cs276;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.EditType;
import edu.stanford.cs276.util.EditTypeEnum;
import edu.stanford.cs276.util.NoisyChannelModelUtiliy;
import edu.stanford.cs276.util.Pair;
import edu.stanford.cs276.util.TermCandidates;
import edu.stanford.cs276.util.CandidateGenerationUtility;

;

public class CandidateGenerator implements Serializable {

	private static CandidateGenerator cg_;

	// Don't use the constructor since this is a Singleton instance
	private CandidateGenerator() {
	}

	public static CandidateGenerator get() throws Exception {
		if (cg_ == null) {
			cg_ = new CandidateGenerator();
		}
		return cg_;
	}

	// Generate all candidates for the target query
	public TermCandidates getCandidates(String query, LanguageModel langModel,
			boolean useKGram) throws Exception {
		TermCandidates queryCandidates = new TermCandidates(query);
		/*
		 * Your code here
		 */

		String[] queryTerms = query.trim().split("\\s+");
		Map<Integer, TermCandidates> termCandidateMap = new TreeMap<Integer, TermCandidates>();
		Map<String, Integer> processedTerms = new TreeMap<String, Integer>();

		int tmp = 0;

		// Get 1-edit and 2-edit candidates for each term in the query
		int i = 0;
		for (String queryTerm : queryTerms) {
			TermCandidates termCandidate = new TermCandidates(queryTerm);
			if (processedTerms.containsKey(queryTerm)) {
				termCandidate.edit1Candidates.addAll(termCandidateMap
						.get(processedTerms.get(queryTerm)).edit1Candidates);
				termCandidate.edit2Candidates.addAll(termCandidateMap
						.get(processedTerms.get(queryTerm)).edit2Candidates);
			} else {
				Collection<String> dictTerms = useKGram == true ? langModel.KGram
						.GetNearestTerms(queryTerm) : langModel.oneTermDict
						.GetTermList();
				for (String dictTerm : dictTerms) {
					switch (CandidateGenerationUtility.FindEditDistance(
							dictTerm, queryTerm)) {
					case 1:
						termCandidate.edit1Candidates
								.add(new Pair<String, List<EditType>>(dictTerm,
										NoisyChannelModelUtiliy.FindEdits(
												dictTerm, queryTerm)));
						break;
					case 2:
						termCandidate.edit2Candidates
								.add(new Pair<String, List<EditType>>(dictTerm,
										NoisyChannelModelUtiliy.FindEdits(
												dictTerm, queryTerm)));
						break;
					default:
						break;
					}

				}
			}

			tmp++;

			for (Pair<String, List<EditType>> split : CandidateGenerationUtility
					.GetSplits(queryTerm)) {
				String[] subSplits = split.getFirst().trim().split("\\s+");
				if (langModel.oneTermDict.isValidTerm(subSplits[0])
						&& langModel.oneTermDict.isValidTerm(subSplits[1])) {
					termCandidate.splitCandidates.add(split);
				}
			}

			tmp++;

			if ((i != queryTerms.length - 1)
					&& (langModel.oneTermDict.isValidTerm(queryTerm
							+ queryTerms[i + 1]))) {
				List<EditType> edits = new ArrayList<EditType>();
				edits.add(new EditType(EditTypeEnum.Ins, queryTerm
						.charAt(queryTerm.length() - 1), ' '));
				termCandidate.joinCandidates
						.add(new Pair<String, List<EditType>>(queryTerm
								+ queryTerms[i + 1], edits));
			}

			termCandidateMap.put(i, termCandidate);
			processedTerms.put(queryTerm, i);
			i++;
		}

		tmp++;

		// Check 0-edit query candidate
		boolean isInDict = true;
		for (String qterm : queryTerms) {
			if (!langModel.oneTermDict.isValidTerm(qterm)) {
				isInDict = false;
				break;
			}
		}
		if (isInDict) {
			queryCandidates.edit0Candidate = query;
		}

		tmp++;

		StringBuilder prefix = new StringBuilder();
		StringBuilder suffix = new StringBuilder();

		// Construct 1-edit query candidates using 1-edit terms and splits
		for (i = 0; i < queryTerms.length; i++) {
			boolean isRestQueryTermsValid = true;
			for (int j = 0; j < queryTerms.length; j++) {
				if (j == i) {
					continue;
				}
				if (!langModel.oneTermDict.isValidTerm(queryTerms[j])) {
					isRestQueryTermsValid = false;
					break;
				}
			}
			if (!isRestQueryTermsValid) {
				continue;
			}
			if (termCandidateMap.get(i).edit1Candidates.isEmpty()
					&& termCandidateMap.get(i).splitCandidates.isEmpty()) {
				continue;
			}

			prefix.setLength(0);
			suffix.setLength(0);
			prefix.append("");
			suffix.append("");

			for (int j = 0; j < i; j++) {
				prefix.append(queryTerms[j]);
				prefix.append(" ");
			}
			for (int j = i + 1; j < queryTerms.length; j++) {
				suffix.append(" ");
				suffix.append(queryTerms[j]);
			}
			queryCandidates.edit1Candidates.addAll(CandidateGenerationUtility
					.GetCrossProduct(prefix.toString(),
							termCandidateMap.get(i).edit1Candidates,
							suffix.toString()));
			queryCandidates.edit1Candidates.addAll(CandidateGenerationUtility
					.GetCrossProduct(prefix.toString(),
							termCandidateMap.get(i).splitCandidates,
							suffix.toString()));
		}

		tmp++;

		// Construct 1-edit query candidates using joints
		for (i = 0; i < queryTerms.length; i++) {
			boolean isRestQueryTermsValid = true;
			for (int j = 0; j < queryTerms.length; j++) {
				if ((j == i) || (j == i + 1)) {
					continue;
				}
				if (!langModel.oneTermDict.isValidTerm(queryTerms[j])) {
					isRestQueryTermsValid = false;
					break;
				}
			}
			if (!isRestQueryTermsValid) {
				continue;
			}
			if (termCandidateMap.get(i).joinCandidates.isEmpty()) {
				continue;
			}

			prefix.setLength(0);
			suffix.setLength(0);
			prefix.append("");
			suffix.append("");

			for (int j = 0; j < i; j++) {
				prefix.append(queryTerms[j]);
				prefix.append(" ");
			}
			for (int j = i + 2; j < queryTerms.length; j++) {
				suffix.append(" ");
				suffix.append(queryTerms[j]);
			}
			queryCandidates.edit1Candidates.addAll(CandidateGenerationUtility
					.GetCrossProduct(prefix.toString(),
							termCandidateMap.get(i).joinCandidates,
							suffix.toString()));
		}

		tmp++;

		// Construct 2-edit query candidates using 2-edit terms
		for (i = 0; i < queryTerms.length; i++) {
			boolean isRestQueryTermsValid = true;
			for (int j = 0; j < queryTerms.length; j++) {
				if (i == j) {
					continue;
				}
				if (!langModel.oneTermDict.isValidTerm(queryTerms[j])) {
					isRestQueryTermsValid = false;
					break;
				}
			}
			if (!isRestQueryTermsValid) {
				continue;
			}
			if (termCandidateMap.get(i).edit2Candidates.isEmpty()) {
				continue;
			}

			prefix.setLength(0);
			suffix.setLength(0);
			prefix.append("");
			suffix.append("");

			for (int j = 0; j < i; j++) {
				prefix.append(queryTerms[j]);
				prefix.append(" ");
			}
			for (int j = i + 1; j < queryTerms.length; j++) {
				suffix.append(" ");
				suffix.append(queryTerms[j]);
			}
			queryCandidates.edit2Candidates.addAll(CandidateGenerationUtility
					.GetCrossProduct(prefix.toString(),
							termCandidateMap.get(i).edit2Candidates,
							suffix.toString()));
		}

		tmp++;

		// Construct 2-edit query candidates using two 1-edit terms
		StringBuilder suffix1 = new StringBuilder();
		StringBuilder suffix2 = new StringBuilder();
		List<Pair<String, List<EditType>>> iEdit1Split = new ArrayList<Pair<String, List<EditType>>>();
		List<Pair<String, List<EditType>>> jEdit1Split = new ArrayList<Pair<String, List<EditType>>>();
		for (i = 0; i < queryTerms.length; i++) {
			iEdit1Split.clear();
			iEdit1Split.addAll(termCandidateMap.get(i).edit1Candidates);
			iEdit1Split.addAll(termCandidateMap.get(i).splitCandidates);
			if (iEdit1Split.isEmpty()) {
				continue;
			}
			for (int j = i + 1; j < queryTerms.length; j++) {
				boolean isRestQueryTermsValid = true;
				for (int k = 0; k < queryTerms.length; k++) {
					if ((k == i) || (k == j)) {
						continue;
					}
					if (!langModel.oneTermDict.isValidTerm(queryTerms[k])) {
						isRestQueryTermsValid = false;
						break;
					}
				}
				if (!isRestQueryTermsValid) {
					continue;
				}

				jEdit1Split.clear();
				jEdit1Split.addAll(termCandidateMap.get(j).edit1Candidates);
				jEdit1Split.addAll(termCandidateMap.get(j).splitCandidates);
				if (jEdit1Split.isEmpty()) {
					continue;
				}

				prefix.setLength(0);
				suffix1.setLength(0);
				suffix2.setLength(0);
				prefix.append("");
				suffix1.append("");
				suffix2.append("");

				for (int k = 0; k < i; k++) {
					prefix.append(queryTerms[k]);
					prefix.append(" ");
				}
				for (int k = i + 1; k < j; k++) {
					suffix1.append(" ");
					suffix1.append(queryTerms[k]);
				}
				suffix1.append(" ");
				for (int k = j + 1; k < queryTerms.length; k++) {
					suffix2.append(" ");
					suffix2.append(queryTerms[k]);
				}

				queryCandidates.edit2Candidates
						.addAll(CandidateGenerationUtility.GetCrossProduct(
								prefix.toString(), iEdit1Split,
								suffix1.toString(), jEdit1Split,
								suffix2.toString()));
			}
		}

		tmp++;

		return queryCandidates;
	}

}
