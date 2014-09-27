package edu.stanford.cs276;

import java.util.List;

import edu.stanford.cs276.util.EditType;

public class UniformCostModel implements EditCostModel {

	private final double edit0Probability = 0.9;
	private final double edit1Probability = 0.05;

	@Override
	public double editProbability(String Q, String R, List<EditType> edits) {
		/*
		 * Your code here
		 */
		double retVal = 0;
		int distance = edits == null ? 0 : edits.size();
		switch (distance) {
		case 0:
			retVal = edit0Probability;
			break;
		case 1:
			retVal = edit1Probability;
			break;
		case 2:
			retVal = edit1Probability * edit1Probability;
			break;
		default:
			retVal = edit1Probability;
			break;
		}
		return Math.log(retVal);
	}
}
