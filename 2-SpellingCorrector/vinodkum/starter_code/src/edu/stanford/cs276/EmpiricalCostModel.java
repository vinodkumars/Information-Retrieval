package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.EditType;
import edu.stanford.cs276.util.NoisyChannelModelUtiliy;

public class EmpiricalCostModel implements EditCostModel {

	Dictionary delDict = new Dictionary();
	Dictionary insDict = new Dictionary();
	Dictionary subDict = new Dictionary();
	Dictionary transDict = new Dictionary();
	Dictionary unigram = new Dictionary();
	Dictionary bigram = new Dictionary();
	private final double equalProbability = 0.9;

	public EmpiricalCostModel(String editsFile) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(editsFile));
		System.out.println("Constructing edit distance map...");
		String line = null;
		while ((line = input.readLine()) != null) {
			Scanner lineSc = new Scanner(line);
			lineSc.useDelimiter("\t");
			String noisy = lineSc.next();
			String clean = lineSc.next();
			lineSc.close();

			// Determine type of error and record probability
			/*
			 * Your code here
			 */

			if (clean.equals(noisy)) {
				continue;
			}

			for (EditType e : NoisyChannelModelUtiliy.FindEdits(clean, noisy)) {
				switch (e.type) {
				case Del:
					delDict.add(e.x + "|" + e.y);
					break;
				case Ins:
					insDict.add(e.x + "|" + e.y);
					break;
				case Sub:
					subDict.add(e.x + "|" + e.y);
					break;
				case Trans:
					transDict.add(e.x + "|" + e.y);
					break;
				default:
					break;
				}
			}
			unigram.add(" ");
			for (int i = 0; i < clean.length(); i++) {
				unigram.add(clean.charAt(i) + "");
				if (i == 0) {
					bigram.add(" " + "|" + clean.charAt(i));
				} else {
					bigram.add(clean.charAt(i - 1) + "|" + clean.charAt(i));
				}
			}
		}
		input.close();
		System.out.println("Done.");
	}

	// You need to update this to calculate the proper empirical cost
	@Override
	public double editProbability(String Q, String R, List<EditType> edits) {
		/*
		 * Your code here
		 */
		if (Q == R) {
			return Math.log(equalProbability);
		}
		double retVal = 0;
		for (EditType e : edits) {
			double tmp;
			switch (e.type) {
			case Del:
				tmp = (double) (delDict.count(e.x + "|" + e.y) + 1)
						/ (double) (bigram.count(e.x + "|" + e.y) + bigram
								.termCount());
				retVal += Math.log(tmp);
				break;
			case Ins:
				tmp = (double) (insDict.count(e.x + "|" + e.y) + 1)
						/ (double) (unigram.count(e.x + "") + unigram
								.termCount());
				retVal += Math.log(tmp);
				break;
			case Sub:
				tmp = (double) (subDict.count(e.x + "|" + e.y) + 1)
						/ (double) (unigram.count(e.y + "") + unigram
								.termCount());
				retVal += Math.log(tmp);
				break;
			case Trans:
				tmp = (double) (transDict.count(e.x + "|" + e.y) + 1)
						/ (double) (bigram.count(e.x + "|" + e.y) + bigram
								.termCount());
				retVal += Math.log(tmp);
				break;
			default:
				break;
			}
		}

		return retVal;
	}
}
