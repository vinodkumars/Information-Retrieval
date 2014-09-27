package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.metric.NDCGScorer;

public class Extra2 {

	public List<RankList> extract_train_features(String train_data_file,
			String train_rel_file, IDF idfData) {

		// Parse input files
		Map<Query, List<Document>> trainData = null;
		Map<String, Map<String, Double>> relData = null;
		List<RankList> ranklistList = new ArrayList<RankList>();
		try {
			trainData = Util.loadTrainData(train_data_file);
			relData = Util.loadRelData(train_rel_file);
		} catch (Exception ex) {
			System.err.println("Exception while parsing files.");
			ex.printStackTrace();
		}

		/* Add data */
		StringBuilder sb = new StringBuilder();
		int qid = 0;
		BM25FeatureExtractor bm25 = new BM25FeatureExtractor(idfData, trainData);
		WordVectorFeatureExtractor wordVec = new WordVectorFeatureExtractor();
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			RankList r = new RankList();
			qid++;
			Map<String, Double> relDataForQuery = relData.get(q.query);
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = wordVec.GetFeature(d, q,
						idfData);
				double relOfDocToQuery = relDataForQuery.get(d.url);
				double bm25Score = bm25.GetBM25Score(d, q);
				double pagerank = PageRankFeatureExtractor.GetPageRank(d);
				GetStringForDatapoint(sb, qid, features, bm25Score, pagerank,
						relOfDocToQuery);
				DataPoint dp = new DataPoint(sb.toString());
				r.add(dp);
			}
			ranklistList.add(r);
		}
		return ranklistList;
	}

	private void GetStringForDatapoint(StringBuilder sb, int qid,
			Map<FieldTypes, Double> features, double bm25Score,
			double pagerank, double relOfDocToQuery) {
		sb.setLength(0);
		sb.append(relOfDocToQuery + " " + "qid:" + qid + " ");
		sb.append("1:" + features.get(FieldTypes.URL) + " ");
		sb.append("2:" + features.get(FieldTypes.Title) + " ");
		sb.append("3:" + features.get(FieldTypes.Body) + " ");
		sb.append("4:" + features.get(FieldTypes.Header) + " ");
		sb.append("5:" + features.get(FieldTypes.Anchor) + " ");
		// sb.append("6:" + bm25Score + " ");
		sb.append("6:" + pagerank + " ");
	}

	public Ranker training(List<RankList> ranklistList) {
		/*
		 * @TODO: Your code here
		 */

		int[] features = { 1, 2, 3, 4, 5, 6 };
		RankNet model = new RankNet(ranklistList, features);
		Ranker.verbose = false;
		model.set(new NDCGScorer());
		model.init();
		model.learn();
		return model;
	}

	public TestFeatures extract_test_features(String test_data_file, IDF idfData) {
		/*
		 * @TODO: Your code here
		 */

		TestFeatures testFeatures = new TestFeatures();
		testFeatures.datapoints = new ArrayList<DataPoint>();
		testFeatures.index_map = new HashMap<String, Map<String, Integer>>();
		List<DataPoint> datapoints = new ArrayList<DataPoint>();

		// Parse input files
		Map<Query, List<Document>> testData = null;
		try {
			testData = Util.loadTrainData(test_data_file);
		} catch (Exception ex) {
			System.err.println("Exception while parsing files.");
			ex.printStackTrace(System.err);
		}

		/* Add data */
		int index = 0;
		int qid = 0;
		StringBuilder sb = new StringBuilder();
		BM25FeatureExtractor bm25 = new BM25FeatureExtractor(idfData, testData);
		for (Entry<Query, List<Document>> entry : testData.entrySet()) {
			Query q = entry.getKey();
			qid++;
			testFeatures.index_map.put(q.query, new HashMap<String, Integer>());
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = TfIdfFeatureExtractor
						.GetFeature(d, q, idfData);
				double bm25Score = bm25.GetBM25Score(d, q);
				double pagerank = PageRankFeatureExtractor.GetPageRank(d);
				GetStringForDatapoint(sb, qid, features, bm25Score, pagerank, 0);
				DataPoint dp = new DataPoint(sb.toString());
				datapoints.add(dp);
				testFeatures.index_map.get(q.query).put(d.url, index);
				index++;
			}
		}

		testFeatures.datapoints = datapoints;
		return testFeatures;
	}

	public Map<String, List<String>> testing(TestFeatures tf, Ranker model) {
		/*
		 * @TODO: Your code here
		 */
		Map<String, List<String>> rankedResults = new HashMap<String, List<String>>();

		try {
			for (Entry<String, Map<String, Integer>> entry1 : tf.index_map
					.entrySet()) {

				List<Pair<String, Double>> urlAndScores = new ArrayList<Pair<String, Double>>(
						entry1.getValue().size());

				for (Entry<String, Integer> entry2 : entry1.getValue()
						.entrySet()) {
					double prediction = model.eval(tf.datapoints.get(entry2
							.getValue()));
					urlAndScores.add(new Pair<String, Double>(entry2.getKey(),
							prediction));
				}
				Collections.sort(urlAndScores,
						new Comparator<Pair<String, Double>>() {
							@Override
							public int compare(Pair<String, Double> o1,
									Pair<String, Double> o2) {
								/*
								 * @//TODO : Your code here
								 */
								return o1.getSecond().doubleValue() <= o2
										.getSecond().doubleValue() ? 1 : -1;
							}
						});

				ArrayList<String> rankedUrls = new ArrayList<String>(entry1
						.getValue().size());
				for (Pair<String, Double> entry3 : urlAndScores) {
					rankedUrls.add(entry3.getFirst());
				}
				rankedResults.put(entry1.getKey(), rankedUrls);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		return rankedResults;
	}

}
