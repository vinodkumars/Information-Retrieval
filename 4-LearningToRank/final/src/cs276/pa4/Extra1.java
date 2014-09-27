package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class Extra1 {

	public Instances extract_train_features(String train_data_file,
			String train_rel_file, IDF idfData) {
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

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute url_w = new Attribute("url_w");
		Attribute title_w = new Attribute("title_w");
		Attribute body_w = new Attribute("body_w");
		Attribute header_w = new Attribute("header_w");
		Attribute anchor_w = new Attribute("anchor_w");

		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);

		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			Map<String, Double> relDataForQuery = relData.get(q.query);
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = TfIdfFeatureExtractor
						.GetFeature(d, q, idfData);
				double[] instance = { relDataForQuery.get(d.url),
						features.get(FieldTypes.URL),
						features.get(FieldTypes.Title),
						features.get(FieldTypes.Body),
						features.get(FieldTypes.Header),
						features.get(FieldTypes.Anchor) };
				Instance inst = new DenseInstance(1.0, instance);
				dataset.add(inst);
			}
		}
		dataset.setClassIndex(0);

		Instances filteredDataSet = null;
		try {
			Standardize standardizeFilter = new Standardize();
			standardizeFilter.setInputFormat(dataset);
			filteredDataSet = Filter.useFilter(dataset, standardizeFilter);
			filteredDataSet.setClassIndex(0);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		return filteredDataSet;
	}

	public svm_model training(Instances dataset) {
		svm_problem prob = new svm_problem();
		prob.l = dataset.size();
		prob.y = new double[dataset.size()];
		prob.x = new svm_node[dataset.size()][];

		for (int i = 0; i < dataset.size(); i++) {
			prob.x[i] = new svm_node[dataset.numAttributes() - 1];
			Instance inst = dataset.get(i);
			for (int j = 1; j < dataset.numAttributes(); j++) {
				svm_node node = new svm_node();
				node.index = j;
				node.value = inst.value(j);
				prob.x[i][j - 1] = node;
			}
			prob.y[i] = inst.value(0);
		}

		svm_parameter param = new svm_parameter();
		param.probability = 1;
		param.svm_type = svm_parameter.EPSILON_SVR;
		param.kernel_type = svm_parameter.LINEAR;

		svm_model model = svm.svm_train(prob, param);
		return model;
	}

	public TestFeatures extract_test_features(String test_data_file, IDF idfData) {
		// TODO Auto-generated method stub
		TestFeatures testFeatures = new TestFeatures();
		testFeatures.index_map = new HashMap<String, Map<String, Integer>>();
		Map<Query, List<Document>> trainData = null;
		try {
			trainData = Util.loadTrainData(test_data_file);
		} catch (Exception ex) {
			System.err.println("Exception while parsing files.");
			ex.printStackTrace();
		}

		Instances dataset = null;

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute url_w = new Attribute("url_w");
		Attribute title_w = new Attribute("title_w");
		Attribute body_w = new Attribute("body_w");
		Attribute header_w = new Attribute("header_w");
		Attribute anchor_w = new Attribute("anchor_w");

		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);

		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		int index = 0;
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			testFeatures.index_map.put(q.query, new HashMap<String, Integer>());
			for (Document d : entry.getValue()) {
				Map<FieldTypes, Double> features = TfIdfFeatureExtractor
						.GetFeature(d, q, idfData);
				double[] instance = { 0.0, features.get(FieldTypes.URL),
						features.get(FieldTypes.Title),
						features.get(FieldTypes.Body),
						features.get(FieldTypes.Header),
						features.get(FieldTypes.Anchor) };
				testFeatures.index_map.get(q.query).put(d.url, index);
				index++;
				Instance inst = new DenseInstance(1.0, instance);
				dataset.add(inst);
			}
		}
		dataset.setClassIndex(0);

		Instances filteredDataSet = null;
		try {
			Standardize standardizeFilter = new Standardize();
			standardizeFilter.setInputFormat(dataset);
			filteredDataSet = Filter.useFilter(dataset, standardizeFilter);
			filteredDataSet.setClassIndex(0);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		testFeatures.features = filteredDataSet;
		return testFeatures;
	}

	public Map<String, List<String>> testing(TestFeatures tf, svm_model model) {
		// TODO Auto-generated method stub

		Map<String, List<String>> rankedResults = new HashMap<String, List<String>>();
		try {
			for (Entry<String, Map<String, Integer>> entry1 : tf.index_map
					.entrySet()) {

				List<Pair<String, Double>> urlAndScores = new ArrayList<Pair<String, Double>>(
						entry1.getValue().size());

				for (Entry<String, Integer> entry2 : entry1.getValue()
						.entrySet()) {
					int numAttr = tf.features.instance(entry2.getValue())
							.numAttributes();
					svm_node nodes[] = new svm_node[numAttr - 1];
					for (int i = 1; i < numAttr; i++) {
						svm_node node = new svm_node();
						node.index = i;
						node.value = tf.features.instance(entry2.getValue())
								.value(i);
						nodes[i - 1] = node;
					}
					double prediction = svm.svm_predict(model, nodes);
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
