package cs276.pa4;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.neuralnet.RankNet;
import libsvm.svm_model;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class Learning2Rank {

	static boolean isLinearKernel = false;

	public static Classifier train(String train_data_file,
			String train_rel_file, int task, IDF idfData) {
		System.err.println("## Training with feature_file =" + train_data_file
				+ ", rel_file = " + train_rel_file + " ... \n");
		Classifier model = null;
		Learner learner = null;

		if (task == 1) {
			learner = new PointwiseLearner();
		} else if (task == 2) {
			if (isLinearKernel) {
				learner = new PairwiseLearner(isLinearKernel);
			} else {
				learner = new PairwiseLearner(isLinearKernel);
				// learner = new PairwiseLearner(Config.cost, Config.gamma,
				// isLinearKernel);
			}
		} else if (task == 3) {

			/*
			 * @TODO: Your code here, add more features
			 */
			learner = new Task3PairwiseLearner(false);
			// learner = new Task3PairwiseLearner(Config.cost, Config.gamma,
			// isLinearKernel);
			System.err.println("Task 3");

		} else if (task == 4) {
			/*
			 * @TODO: Your code here, extra credit
			 */
			System.err.println("Extra credit");

		}

		/* Step (1): construct your feature matrix here */
		Instances data = learner.extract_train_features(train_data_file,
				train_rel_file, idfData);

		/* Step (2): implement your learning algorithm here */
		model = learner.training(data);

		return model;
	}

	public static Ranker trainForExtra(String train_data_file,
			String train_rel_file, IDF idfData) {
		System.err.println("## Training with feature_file =" + train_data_file
				+ ", rel_file = " + train_rel_file + " ... \n");
		System.err.println("Extra credit");
		Ranker model = null;
		Extra2 learner = new Extra2();

		/* Step (1): construct your feature matrix here */
		List<RankList> data = learner.extract_train_features(train_data_file,
				train_rel_file, idfData);

		/* Step (2): implement your learning algorithm here */
		model = learner.training(data);
		return model;
	}

	public static Map<String, List<String>> test(String test_data_file,
			Classifier model, int task, IDF idfData) {
		System.err.println("## Testing with feature_file=" + test_data_file
				+ " ... \n");
		Map<String, List<String>> ranked_queries = new HashMap<String, List<String>>();
		Learner learner = null;
		if (task == 1) {
			learner = new PointwiseLearner();
		} else if (task == 2) {
			if (isLinearKernel) {
				learner = new PairwiseLearner(isLinearKernel);
			} else {
				learner = new PairwiseLearner(isLinearKernel);
				// learner = new PairwiseLearner(Config.cost, Config.gamma,
				// isLinearKernel);
			}
		} else if (task == 3) {
			/*
			 * @TODO: Your code here, add more features
			 */
			learner = new Task3PairwiseLearner(false);
			System.err.println("Task 3");

		} else if (task == 4) {
			/*
			 * @TODO: Your code here, extra credit
			 */
			System.err.println("Extra credit");

		}

		/* Step (1): construct your test feature matrix here */
		TestFeatures tf = learner
				.extract_test_features(test_data_file, idfData);

		/* Step (2): implement your prediction and ranking code here */
		ranked_queries = learner.testing(tf, model);

		return ranked_queries;
	}

	public static Map<String, List<String>> testForExtra(String test_data_file,
			Ranker model, IDF idfData) {
		System.err.println("## Testing with feature_file=" + test_data_file
				+ " ... \n");
		Map<String, List<String>> ranked_queries = new HashMap<String, List<String>>();
		System.err.println("Extra credit");
		Extra2 learner = new Extra2();

		/* Step (1): construct your test feature matrix here */
		TestFeatures tf = learner
				.extract_test_features(test_data_file, idfData);

		/* Step (2): implement your prediction and ranking code here */
		ranked_queries = learner.testing(tf, model);
		return ranked_queries;
	}

	/* This function output the ranking results in expected format */
	public static void writeRankedResultsToFile(
			Map<String, List<String>> ranked_queries, PrintStream ps) {

		// Note to self: Comment when submitting
		List<Pair<String, List<String>>> rankedCollection = new ArrayList<Pair<String, List<String>>>();
		for (Entry<String, List<String>> entry : ranked_queries.entrySet()) {
			rankedCollection.add(new Pair<String, List<String>>(entry.getKey(),
					entry.getValue()));
		}
		Collections.sort(rankedCollection,
				new Comparator<Pair<String, List<String>>>() {
					@Override
					public int compare(Pair<String, List<String>> arg0,
							Pair<String, List<String>> arg1) {
						// TODO Auto-generated method stub
						return arg0.getFirst().compareToIgnoreCase(
								arg1.getFirst());
					}
				});
		for (Pair<String, List<String>> p : rankedCollection) {
			ps.println("query: " + p.getFirst());

			for (String url : ranked_queries.get(p.getFirst())) {
				ps.println("  url: " + url);
			}
		}

		// for (String query : ranked_queries.keySet()) {
		// ps.println("query: " + query.toString());
		//
		// for (String url : ranked_queries.get(query)) {
		// ps.println("  url: " + url);
		// }
		// }
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 4 && args.length != 5) {
			System.err.println("Input arguments: " + Arrays.toString(args));
			System.err
					.println("Usage: <train_data_file> <train_data_file> <test_data_file> <task> [ranked_out_file]");
			System.err
					.println("  ranked_out_file (optional): output results are written into the specified file. "
							+ "If not, output to stdout.");
			return;
		}

		String train_data_file = args[0];
		String train_rel_file = args[1];
		String test_data_file = args[2];
		int task = Integer.parseInt(args[3]);
		String ranked_out_file = "";
		if (args.length == 5) {
			ranked_out_file = args[4];
		}

		/* Populate idfs */
		String dfFile = "df.txt";
		IDF idfData = null;
		try {
			idfData = Util.loadDFs(dfFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, List<String>> trained_ranked_queries = null;
		Classifier model = null;
		Ranker rankerModel = null;
		if (task != 4) {
			/* Train & test */
			System.err.println("### Running task" + task + "...");
			model = train(train_data_file, train_rel_file, task, idfData);

			/* performance on the training data */
			trained_ranked_queries = test(train_data_file, model, task, idfData);
		} else {
			/* Train & test */
			System.err.println("### Running task" + task + "...");
			rankerModel = trainForExtra(train_data_file, train_rel_file,
					idfData);

			/* performance on the training data */
			trained_ranked_queries = testForExtra(train_data_file, rankerModel,
					idfData);
		}

		// Note to self: Uncomment when submitting
		String trainOutFile = "tmp.train.ranked";
		// String trainOutFile =
		// "E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\output\\outputTrain.txt";

		writeRankedResultsToFile(trained_ranked_queries, new PrintStream(
				new FileOutputStream(trainOutFile)));
		NdcgMain ndcg = new NdcgMain(train_rel_file);
		System.err.println("# Trained NDCG=" + ndcg.score(trainOutFile));
		// Note to self: Uncomment when submitting
		// (new File(trainOutFile)).delete();

		Map<String, List<String>> ranked_queries = null;
		if (task != 4) {
			ranked_queries = test(test_data_file, model, task, idfData);
		} else {
			ranked_queries = testForExtra(test_data_file, rankerModel, idfData);
		}

		/* Output results */
		if (ranked_out_file.equals("")) { /* output to stdout */
			writeRankedResultsToFile(ranked_queries, System.out);
		} else { /* output to file */
			try {
				writeRankedResultsToFile(ranked_queries, new PrintStream(
						new FileOutputStream(ranked_out_file)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
