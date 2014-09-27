package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class PointwiseLearner extends Learner {

	@Override
	public Instances extract_train_features(String train_data_file,
			String train_rel_file, IDF idfData) {

		/*
		 * @TODO: Below is a piece of sample code to show you the basic approach
		 * to construct a Instances object, replace with your implementation.
		 */

		// Parse input files
		Map<Query, List<Document>> trainData = null;
		Map<String, Map<String, Double>> relData = null;
		try {
			trainData = Util.loadTrainData(train_data_file);
			relData = Util.loadRelData(train_rel_file);
		} catch (Exception ex) {
			System.err.println("Exception while parsing files.");
			ex.printStackTrace();
		}

		Instances dataset = null;

		/* Build attributes list */
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("url_w"));
		attributes.add(new Attribute("title_w"));
		attributes.add(new Attribute("body_w"));
		attributes.add(new Attribute("header_w"));
		attributes.add(new Attribute("anchor_w"));
		attributes.add(new Attribute("relevance_score"));
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			Map<String, Double> relDataForQuery = relData.get(q.query);
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = TfIdfFeatureExtractor
						.GetFeature(d, q, idfData);
				double[] instance = { features.get(FieldTypes.URL),
						features.get(FieldTypes.Title),
						features.get(FieldTypes.Body),
						features.get(FieldTypes.Header),
						features.get(FieldTypes.Anchor),
						relDataForQuery.get(d.url) };
				Instance inst = new DenseInstance(1.0, instance);
				dataset.add(inst);
			}
		}

		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);

		// Standardization
		// Instances filteredDataSet = null;
		// try {
		// Standardize filter = new Standardize();
		// filter.setInputFormat(dataset);
		// filteredDataSet = Filter.useFilter(dataset, filter);
		// filteredDataSet.setClassIndex(filteredDataSet.numAttributes() - 1);
		// } catch (Exception ex) {
		// ex.printStackTrace(System.err);
		// }

		return dataset;
	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 */
		LinearRegression model = new LinearRegression();
		try {
			// model.setDebug(true);
			// model.setOutputAdditionalStats(true);

			model.buildClassifier(dataset);

			System.err.println("Coefficients:");
			for (double d : model.coefficients()) {
				System.err.println(d);
			}
			// System.err.println("Options:");
			// for (String s : model.getOptions()) {
			// System.err.println(s);
			// }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;
	}

	@Override
	public TestFeatures extract_test_features(String test_data_file, IDF idfData) {
		/*
		 * @TODO: Your code here
		 */

		TestFeatures testFeatures = new TestFeatures();
		testFeatures.index_map = new HashMap<String, Map<String, Integer>>();

		// Parse input files
		Map<Query, List<Document>> testData = null;
		try {
			testData = Util.loadTrainData(test_data_file);
		} catch (Exception ex) {
			System.err.println("Exception while parsing files.");
			ex.printStackTrace(System.err);
		}

		Instances dataset = null;

		/* Build attributes list */
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("url_w"));
		attributes.add(new Attribute("title_w"));
		attributes.add(new Attribute("body_w"));
		attributes.add(new Attribute("header_w"));
		attributes.add(new Attribute("anchor_w"));
		attributes.add(new Attribute("relevance_score"));
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		int index = 0;
		for (Entry<Query, List<Document>> entry : testData.entrySet()) {
			Query q = entry.getKey();
			testFeatures.index_map.put(q.query, new HashMap<String, Integer>());
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = TfIdfFeatureExtractor
						.GetFeature(d, q, idfData);
				double[] instance = { features.get(FieldTypes.URL),
						features.get(FieldTypes.Title),
						features.get(FieldTypes.Body),
						features.get(FieldTypes.Header),
						features.get(FieldTypes.Anchor), 1.0 };
				testFeatures.index_map.get(q.query).put(d.url, index);
				index++;
				Instance inst = new DenseInstance(1.0, instance);
				dataset.add(inst);
			}
		}

		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);

		// Standardization
		// Instances filteredDataSet = null;
		// try {
		// Standardize filter = new Standardize();
		// filter.setInputFormat(dataset);
		// filteredDataSet = Filter.useFilter(dataset, filter);
		// filteredDataSet.setClassIndex(filteredDataSet.numAttributes() - 1);
		// } catch (Exception ex) {
		// ex.printStackTrace(System.err);
		// }

		testFeatures.features = dataset;
		return testFeatures;
	}

	@Override
	public Map<String, List<String>> testing(TestFeatures tf, Classifier model) {
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
					double prediction = model.classifyInstance(tf.features
							.instance(entry2.getValue()));
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
