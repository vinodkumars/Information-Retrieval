package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class PairwiseLearner extends Learner {
	private LibSVM model;

	public PairwiseLearner(boolean isLinearKernel) {
		try {
			model = new LibSVM();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isLinearKernel) {
			model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
					LibSVM.TAGS_KERNELTYPE));
		}
	}

	public PairwiseLearner(double C, double gamma, boolean isLinearKernel) {
		try {
			model = new LibSVM();
		} catch (Exception e) {
			e.printStackTrace();
		}

		model.setCost(C);
		model.setGamma(gamma); // only matter for RBF kernel
		if (isLinearKernel) {
			model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
					LibSVM.TAGS_KERNELTYPE));
		}
	}

	@Override
	public Instances extract_train_features(String train_data_file,
			String train_rel_file, IDF idfData) {
		/*
		 * @TODO: Your code here
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
		List<String> nominalValues = new ArrayList<String>();
		nominalValues.add("+");
		nominalValues.add("-");

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute url_w = new Attribute("url_w");
		Attribute title_w = new Attribute("title_w");
		Attribute body_w = new Attribute("body_w");
		Attribute header_w = new Attribute("header_w");
		Attribute anchor_w = new Attribute("anchor_w");
		Attribute label = new Attribute("label", nominalValues);
		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);
		attributes.add(label);
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			Map<String, Double> relDataForQuery = relData.get(q.query);
			List<Document> docsForQuery = entry.getValue();
			for (int i = 0; i < docsForQuery.size(); i++) {
				Map<FieldTypes, Double> features1 = TfIdfFeatureExtractor
						.GetFeature(docsForQuery.get(i), q, idfData);
				for (int j = i + 1; j < docsForQuery.size(); j++) {

					if (relDataForQuery.get(docsForQuery.get(i).url) == relDataForQuery
							.get(docsForQuery.get(j).url)) {
						continue;
					}
					Map<FieldTypes, Double> features2 = TfIdfFeatureExtractor
							.GetFeature(docsForQuery.get(j), q, idfData);

					String labelValue1 = relDataForQuery.get(docsForQuery
							.get(i).url) > relDataForQuery.get(docsForQuery
							.get(j).url) ? "+" : "-";

					Instance inst1 = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							features1, features2, inst1);
					inst1.setValue(label, labelValue1);
					dataset.add(inst1);

					String labelValue2 = labelValue1.equals("+") ? "-" : "+";
					Instance inst2 = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							features2, features1, inst2);
					inst2.setValue(label, labelValue2);
					dataset.add(inst2);
				}
			}
		}

		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);

		// Standardization
		Instances filteredDataSet = null;
		try {
			// NumericToNominal numToNomFilter = new NumericToNominal();
			// numToNomFilter.setInputFormat(dataset);
			// filteredDataSet = Filter.useFilter(dataset, numToNomFilter);

			Standardize standardizeFilter = new Standardize();
			standardizeFilter.setInputFormat(dataset);
			filteredDataSet = Filter.useFilter(dataset, standardizeFilter);
			filteredDataSet.setClassIndex(filteredDataSet.numAttributes() - 1);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		return filteredDataSet;

	}

	private void FormInstance(Attribute url_w, Attribute title_w,
			Attribute body_w, Attribute header_w, Attribute anchor_w,
			Map<FieldTypes, Double> features1,
			Map<FieldTypes, Double> features2, Instance inst1) {
		inst1.setValue(url_w, features1.get(FieldTypes.URL)
				- features2.get(FieldTypes.URL));
		inst1.setValue(title_w, features1.get(FieldTypes.Title)
				- features2.get(FieldTypes.Title));
		inst1.setValue(body_w, features1.get(FieldTypes.Body)
				- features2.get(FieldTypes.Body));
		inst1.setValue(header_w, features1.get(FieldTypes.Header)
				- features2.get(FieldTypes.Header));
		inst1.setValue(anchor_w, features1.get(FieldTypes.Anchor)
				- features2.get(FieldTypes.Anchor));
	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 */
		try {
			model.buildClassifier(dataset);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.err);
		}
		return model;
	}

	@Override
	public TestFeatures extract_test_features(String test_data_file, IDF idfData) {
		/*
		 * @TODO: Your code here
		 */
		// Parse input files
		TestFeatures testFeatures = new TestFeatures();
		testFeatures.pairs_index_map = new HashMap<String, Map<Pair<String, String>, Integer>>();
		Map<Query, List<Document>> trainData = null;
		try {
			trainData = Util.loadTrainData(test_data_file);
		} catch (Exception ex) {
			System.out.println("Exception while parsing files.");
			ex.printStackTrace();
		}

		Instances dataset = null;

		/* Build attributes list */
		List<String> nominalValues = new ArrayList<String>();
		nominalValues.add("+");
		nominalValues.add("-");

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute url_w = new Attribute("url_w");
		Attribute title_w = new Attribute("title_w");
		Attribute body_w = new Attribute("body_w");
		Attribute header_w = new Attribute("header_w");
		Attribute anchor_w = new Attribute("anchor_w");
		Attribute label = new Attribute("label", nominalValues);
		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);
		attributes.add(label);
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		int index = 0;
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			testFeatures.pairs_index_map.put(q.query,
					new HashMap<Pair<String, String>, Integer>());
			List<Document> docsForQuery = entry.getValue();
			for (int i = 0; i < docsForQuery.size(); i++) {
				Map<FieldTypes, Double> features1 = TfIdfFeatureExtractor
						.GetFeature(docsForQuery.get(i), q, idfData);
				for (int j = i + 1; j < docsForQuery.size(); j++) {
					Map<FieldTypes, Double> features2 = TfIdfFeatureExtractor
							.GetFeature(docsForQuery.get(j), q, idfData);
					Instance inst = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							features1, features2, inst);
					inst.setValue(label, "+");
					dataset.add(inst);
					testFeatures.pairs_index_map.get(q.query).put(
							new Pair<String, String>(docsForQuery.get(i).url,
									docsForQuery.get(j).url), index);
					index++;
				}
			}
		}

		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);

		// Standardization
		Instances filteredDataSet = null;
		try {
			Standardize filter = new Standardize();
			filter.setInputFormat(dataset);
			filteredDataSet = Filter.useFilter(dataset, filter);
			filteredDataSet.setClassIndex(filteredDataSet.numAttributes() - 1);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		testFeatures.features = filteredDataSet;
		return testFeatures;
	}

	@Override
	public Map<String, List<String>> testing(TestFeatures tf, Classifier model) {
		/*
		 * @TODO: Your code here
		 */
		Map<String, List<String>> rankedResults = new HashMap<String, List<String>>();

		try {
			for (String query : tf.pairs_index_map.keySet()) {
				Map<Pair<String, String>, Integer> docPairsForQuery = tf.pairs_index_map
						.get(query);
				final Map<String, Double> docPairsPrediction = new HashMap<String, Double>();

				for (Entry<Pair<String, String>, Integer> entry : docPairsForQuery
						.entrySet()) {
					docPairsPrediction.put(entry.getKey().getFirst() + "|"
							+ entry.getKey().getSecond(), model
							.classifyInstance(tf.features.instance(entry
									.getValue())));
				}

				Set<String> urlSet = new HashSet<String>();
				for (Entry<Pair<String, String>, Integer> entry : docPairsForQuery
						.entrySet()) {
					if (!urlSet.contains(entry.getKey().getFirst())) {
						urlSet.add(entry.getKey().getFirst());
					}
					if (!urlSet.contains(entry.getKey().getSecond())) {
						urlSet.add(entry.getKey().getSecond());
					}
				}
				List<String> urls = new ArrayList<String>();
				for (String s : urlSet) {
					urls.add(s);
				}

				Collections.sort(urls, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						/*
						 * @//TODO : Your code here
						 */
						int retVal = 1;
						try {
							String k1 = o1 + "|" + o2;
							String k2 = o2 + "|" + o1;
							if (docPairsPrediction.containsKey(k1)) {
								retVal = docPairsPrediction.get(k1) == 1.0 ? 1
										: -1;
							} else {
								retVal = docPairsPrediction.get(k2) == 1.0 ? -1
										: 1;
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return retVal;
					}
				});

				rankedResults.put(query, urls);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		return rankedResults;
	}

}
