package edu.stanford.cs276.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CandidateGenerationUtility {

	public static int Minimum(int a, int b, int c) {
		if (a <= b) {
			if (a <= c) {
				return a;
			} else {
				return c;
			}
		} else if (b <= c) {
			return b;
		} else {
			return c;
		}
	}

	public static List<Pair<String, List<EditType>>> GetSplits(String term) {
		List<Pair<String, List<EditType>>> retVal = new ArrayList<Pair<String, List<EditType>>>();
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < term.length() - 1; i++) {
			sb.setLength(0);
			sb.append(term.substring(0, i));
			sb.append(" ");
			sb.append(term.substring(i, term.length()));
			List<EditType> edits = new ArrayList<EditType>();
			edits.add(new EditType(EditTypeEnum.Del, term.charAt(i), ' '));
			retVal.add(new Pair<String, List<EditType>>(sb.toString(), edits));
		}
		return retVal;
	}

	public static List<Pair<String, List<EditType>>> GetCrossProduct(
			String prefix, List<Pair<String, List<EditType>>> l1,
			String suffix1, List<Pair<String, List<EditType>>> l2,
			String suffix2) {
		List<Pair<String, List<EditType>>> retVal = new ArrayList<Pair<String, List<EditType>>>();
		StringBuilder sb = new StringBuilder();
		for (Pair<String, List<EditType>> s1 : l1) {
			for (Pair<String, List<EditType>> s2 : l2) {
				sb.setLength(0);
				if (prefix != "") {
					sb.append(prefix);
				}
				sb.append(s1.getFirst());
				if (suffix1 != "") {
					sb.append(suffix1);
				}
				sb.append(s2.getFirst());
				if (suffix2 != "") {
					sb.append(suffix2);
				}
				List<EditType> edits = new ArrayList<EditType>();
				edits.addAll(s1.getSecond());
				edits.addAll(s2.getSecond());
				retVal.add(new Pair<String, List<EditType>>(sb.toString(),
						edits));
			}
		}
		return retVal;
	}

	public static List<Pair<String, List<EditType>>> GetCrossProduct(
			String prefix, List<Pair<String, List<EditType>>> l1, String suffix) {
		List<Pair<String, List<EditType>>> retVal = new ArrayList<Pair<String, List<EditType>>>();
		StringBuilder sb = new StringBuilder();
		for (Pair<String, List<EditType>> s1 : l1) {

			sb.setLength(0);
			if (prefix != "") {
				sb.append(prefix);
			}
			sb.append(s1.getFirst());
			if (suffix != "") {
				sb.append(suffix);
			}
			retVal.add(new Pair<String, List<EditType>>(sb.toString(), s1
					.getSecond()));
		}
		return retVal;
	}

	public static int FindEditDistance(String s1, String s2) {
		int[][] arr = new int[2][s2.length() + 1];

		for (int j = 0; j < s2.length() + 1; j++) {
			arr[0][j] = j;
		}

		for (int i = 1; i < s1.length() + 1; i++) {
			arr[i % 2][0] = i;
			for (int j = 1; j < s2.length() + 1; j++) {
				arr[i % 2][j] = 0;
			}
			for (int j = 1; j < s2.length() + 1; j++) {
				if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					// no operation required
					arr[i % 2][j] = arr[(i - 1) % 2][j - 1];
				} else if ((i > 1 && j > 1)
						&& (s1.charAt(i - 1) == s2.charAt(j - 2))
						&& (s1.charAt(i - 2) == s2.charAt(j - 1))) {
					// transposition - cost already added in previous step
					arr[i % 2][j] = arr[(i - 1) % 2][j - 1];
				} else {
					// minimum of insertion, deletion and substitution
					arr[i % 2][j] = Minimum(arr[(i - 1) % 2][j - 1] + 1,
							arr[(i - 1) % 2][j] + 1, arr[i % 2][j - 1] + 1);

				}
			}
		}
		return arr[s1.length() % 2][s2.length()];
	}
}
