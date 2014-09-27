package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.cs276.util.Debug;
import edu.stanford.cs276.util.EditType;
import edu.stanford.cs276.util.EditTypeEnum;
import edu.stanford.cs276.util.Pair;
import edu.stanford.cs276.util.ProbabilityUtility;
import edu.stanford.cs276.util.TermCandidates;

public class RunCorrector {

	public static LanguageModel languageModel;
	public static NoisyChannelModel ncm;

	public static void main(String[] args) throws Exception {

		long startTime = System.currentTimeMillis();

		// Parse input arguments
		String uniformOrEmpirical = null;
		String queryFilePath = null;
		String goldFilePath = null;
		String extra = null;
		BufferedReader goldFileReader = null;
		if (args.length == 2) {
			// Run without extra and comparing to gold
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
		} else if (args.length == 3) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			if (args[2].equals("extra")) {
				extra = args[2];
			} else {
				goldFilePath = args[2];
			}
		} else if (args.length == 4) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			extra = args[2];
			goldFilePath = args[3];
		} else {
			System.err
					.println("Invalid arguments.  Argument count must be 2, 3 or 4"
							+ "./runcorrector <uniform | empirical> <query file> \n"
							+ "./runcorrector <uniform | empirical> <query file> <gold file> \n"
							+ "./runcorrector <uniform | empirical> <query file> <extra> \n"
							+ "./runcorrector <uniform | empirical> <query file> <extra> <gold file> \n"
							+ "SAMPLE: ./runcorrector empirical data/queries.txt \n"
							+ "SAMPLE: ./runcorrector empirical data/queries.txt data/gold.txt \n"
							+ "SAMPLE: ./runcorrector empirical data/queries.txt extra \n"
							+ "SAMPLE: ./runcorrector empirical data/queries.txt extra data/gold.txt \n");
			return;
		}

		if (goldFilePath != null) {
			goldFileReader = new BufferedReader(new FileReader(new File(
					goldFilePath)));
		}

		// Load models from disk
		languageModel = LanguageModel.load();
		ncm = NoisyChannelModel.load();
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(
				new File(queryFilePath)));
		ncm.setProbabilityType(uniformOrEmpirical);
		CandidateGenerator cgen = CandidateGenerator.get();
		if (uniformOrEmpirical.equals("uniform")) {
			Config.mew = 1;
		} else if (uniformOrEmpirical.equals("empirical")) {
			Config.mew = 2;
		}

		Debug debug = new Debug();
		debug.setDev("test");
		BufferedWriter outputWriter = null;
		if (debug.isDev()) {
			Path p = Paths.get(queryFilePath);
			outputWriter = new BufferedWriter(new FileWriter(new File(p
					.getParent().toString() + "\\output.txt")));
		}

		int totalCount = 0;
		int yourCorrectCount = 0;
		String r = null;
		boolean useKGram = false;

		/*
		 * Each line in the file represents one query. We loop over each query
		 * and find the most likely correction
		 */

		// r = "what et is";
		while ((r = queriesFileReader.readLine()) != null) {

			String correctedQuery = "";
			double maxProb = 0;
			double calcProb = 0;
			/*
			 * Your code here
			 */

			if ("extra".equals(extra)) {
				useKGram = true;
			}

			TermCandidates queryCandidates = cgen.getCandidates(r,
					languageModel, useKGram);

			// Finding probability of original
			if (queryCandidates.edit0Candidate != "") {
				calcProb = ProbabilityUtility.GetCandidateScore(
						queryCandidates.edit0Candidate, r, null, languageModel,
						ncm.ecm_);
				correctedQuery = queryCandidates.edit0Candidate;
				maxProb = calcProb;
			}

			// Finding probability of 1-edit candidates
			for (Pair<String, List<EditType>> q : queryCandidates.edit1Candidates) {
				calcProb = ProbabilityUtility.GetCandidateScore(q.getFirst(),
						r, q.getSecond(), languageModel, ncm.ecm_);
				if (correctedQuery == "") {
					correctedQuery = q.getFirst();
					maxProb = calcProb;
				} else if (calcProb > maxProb) {
					correctedQuery = q.getFirst();
					maxProb = calcProb;
				}
			}

			// Finding probability of 2-edit candidates
			for (Pair<String, List<EditType>> q : queryCandidates.edit2Candidates) {
				calcProb = ProbabilityUtility.GetCandidateScore(q.getFirst(),
						r, q.getSecond(), languageModel, ncm.ecm_);
				if (correctedQuery == "") {
					correctedQuery = q.getFirst();
					maxProb = calcProb;
				} else if (calcProb > maxProb) {
					correctedQuery = q.getFirst();
					maxProb = calcProb;
				}
			}

			// If a gold file was provided, compare our correction to the gold
			// correction
			// and output the running accuracy
			if (goldFileReader != null) {
				String goldQuery = goldFileReader.readLine();
				if (goldQuery.equals(correctedQuery)) {
					yourCorrectCount++;
				}
				totalCount++;
			}
			System.out.println(correctedQuery);
			if (debug.isDev()) {
				outputWriter.write(correctedQuery);
				outputWriter.newLine();
			}
		}
		// while loop ends just above
		queriesFileReader.close();
		if (debug.isDev()) {
			outputWriter.close();
		}

		if (debug.isDev()) {
			System.out.println("Correct count: " + yourCorrectCount);
			System.out.println("Total count: " + totalCount);
			System.out
					.println("Accuracy: "
							+ (double) ((double) yourCorrectCount / (double) totalCount));
		}

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		if (debug.isDev()) {
			System.out.println("RUNNING TIME: " + totalTime / 1000
					+ " seconds ");
		}
	}
}
