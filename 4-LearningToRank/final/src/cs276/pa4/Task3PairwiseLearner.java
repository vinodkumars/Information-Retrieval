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

public class Task3PairwiseLearner extends Learner {
	private LibSVM model;
	private boolean useFieldwiseSmallestWindow = true;
	private boolean useSmallestWindow = false;
	private boolean usePageRank = true;
	private boolean useBM25 = true;
	private boolean useUrlDepth = false;

	public Task3PairwiseLearner(boolean isLinearKernel) {
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

	public Task3PairwiseLearner(double C, double gamma, boolean isLinearKernel) {
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
		Attribute titleWindow_w = new Attribute("titleWindow_w");
		Attribute bodyWindow_w = new Attribute("bodyWindow_w");
		Attribute headerWindow_w = new Attribute("headerWindow_w");
		Attribute anchorWindow_w = new Attribute("anchorWindow_w");
		Attribute window_w = new Attribute("window_w");
		Attribute pagerank_w = new Attribute("pagerank_w");
		Attribute bm25_w = new Attribute("bm25_w");
		Attribute urlDepth_w = new Attribute("urlDepth_w");
		Attribute label = new Attribute("label", nominalValues);

		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);
		if (useFieldwiseSmallestWindow) {
			attributes.add(titleWindow_w);
			attributes.add(bodyWindow_w);
			attributes.add(headerWindow_w);
			attributes.add(anchorWindow_w);
		}
		if (useSmallestWindow) {
			attributes.add(window_w);
		}
		if (usePageRank) {
			attributes.add(pagerank_w);
		}
		if (useBM25) {
			attributes.add(bm25_w);
		}
		if (useUrlDepth) {
			attributes.add(urlDepth_w);
		}
		attributes.add(label);
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		BM25FeatureExtractor bm25 = null;
		if (useBM25) {
			bm25 = new BM25FeatureExtractor(idfData, trainData);
		}
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			Map<String, Double> relDataForQuery = relData.get(q.query);
			List<Document> docsForQuery = entry.getValue();
			for (int i = 0; i < docsForQuery.size(); i++) {

				Map<FieldTypes, Double> featuresI = TfIdfFeatureExtractor
						.GetFeature(docsForQuery.get(i), q, idfData);
				double bm25Score = 0;
				double bm25ScoreI = 0;
				if (useBM25) {
					bm25ScoreI = bm25.GetBM25Score(docsForQuery.get(i), q);
				}
				double pagerankScore = 0;
				double pagerankI = 0;
				if (usePageRank) {
					pagerankI = PageRankFeatureExtractor
							.GetPageRank(docsForQuery.get(i));
				}
				Map<FieldTypes, Double> windowFeaturesI = null;
				if (useFieldwiseSmallestWindow) {
					windowFeaturesI = SmallestWindowFeatureExtractor
							.GetSmallestWindowSizeFeatures(docsForQuery.get(i),
									q);
				}
				double smallestWindowScore = 0;
				double smallestWindowI = 0;
				if (useSmallestWindow) {
					smallestWindowI = SmallestWindowFeatureExtractor
							.GetSmallestWindowOfDoc(docsForQuery.get(i), q);
				}
				double urlDepthScore = 0;
				double urlDepthI = 0;
				if (useUrlDepth) {
					urlDepthI = TfIdfFeatureExtractor.GetUrlDepth(docsForQuery
							.get(i));
				}

				for (int j = i + 1; j < docsForQuery.size(); j++) {

					if (relDataForQuery.get(docsForQuery.get(i).url) == relDataForQuery
							.get(docsForQuery.get(j).url)) {
						continue;
					}

					Map<FieldTypes, Double> featuresJ = TfIdfFeatureExtractor
							.GetFeature(docsForQuery.get(j), q, idfData);

					String labelValue1 = relDataForQuery.get(docsForQuery
							.get(i).url) > relDataForQuery.get(docsForQuery
							.get(j).url) ? "+" : "-";

					if (useBM25) {
						double bm25ScoreJ = bm25.GetBM25Score(
								docsForQuery.get(j), q);
						bm25Score = bm25ScoreI - bm25ScoreJ;
					}
					if (usePageRank) {
						double pagerankJ = PageRankFeatureExtractor
								.GetPageRank(docsForQuery.get(j));
						pagerankScore = pagerankI - pagerankJ;
					}
					Map<FieldTypes, Double> windowFeaturesJ = null;
					if (useFieldwiseSmallestWindow) {
						windowFeaturesJ = SmallestWindowFeatureExtractor
								.GetSmallestWindowSizeFeatures(
										docsForQuery.get(i), q);
					}
					if (useSmallestWindow) {
						double smallestWindowJ = SmallestWindowFeatureExtractor
								.GetSmallestWindowOfDoc(docsForQuery.get(j), q);
						smallestWindowScore = smallestWindowI - smallestWindowJ;
					}
					if (useUrlDepth) {
						urlDepthScore = urlDepthI
								- TfIdfFeatureExtractor
										.GetUrlDepth(docsForQuery.get(j));
					}

					Instance inst1 = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							titleWindow_w, bodyWindow_w, headerWindow_w,
							anchorWindow_w, window_w, pagerank_w, bm25_w,
							urlDepth_w, featuresI, featuresJ, bm25Score,
							pagerankScore, windowFeaturesI, windowFeaturesJ,
							smallestWindowScore, urlDepthScore, inst1);
					inst1.setValue(label, labelValue1);
					dataset.add(inst1);

					String labelValue2 = labelValue1.equals("+") ? "-" : "+";
					Instance inst2 = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							titleWindow_w, bodyWindow_w, headerWindow_w,
							anchorWindow_w, window_w, pagerank_w, bm25_w,
							urlDepth_w, featuresJ, featuresI, bm25Score * -1,
							pagerankScore * -1, windowFeaturesJ,
							windowFeaturesI, smallestWindowScore * -1,
							urlDepthScore * -1, inst2);
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
			Attribute titleWindow_w, Attribute bodyWindow_w,
			Attribute headerWindow_w, Attribute anchorWindow_w,
			Attribute window_w, Attribute pagerank_w, Attribute bm25_w,
			Attribute urlDepth, Map<FieldTypes, Double> features1,
			Map<FieldTypes, Double> features2, double bm25Score,
			double pageRankScore, Map<FieldTypes, Double> windowFeaturesI,
			Map<FieldTypes, Double> windowFeaturesJ, double smallestWindow,
			double urlDepthScore, Instance inst) {
		inst.setValue(url_w,
				features1.get(FieldTypes.URL) - features2.get(FieldTypes.URL));
		inst.setValue(
				title_w,
				features1.get(FieldTypes.Title)
						- features2.get(FieldTypes.Title));
		inst.setValue(body_w,
				features1.get(FieldTypes.Body) - features2.get(FieldTypes.Body));
		inst.setValue(
				header_w,
				features1.get(FieldTypes.Header)
						- features2.get(FieldTypes.Header));
		inst.setValue(
				anchor_w,
				features1.get(FieldTypes.Anchor)
						- features2.get(FieldTypes.Anchor));
		if (useBM25) {
			inst.setValue(bm25_w, bm25Score);
		}
		if (usePageRank) {
			inst.setValue(pagerank_w, pageRankScore);
		}
		if (useFieldwiseSmallestWindow) {
			inst.setValue(titleWindow_w, windowFeaturesI.get(FieldTypes.Title)
					- windowFeaturesJ.get(FieldTypes.Title));
			inst.setValue(bodyWindow_w, windowFeaturesI.get(FieldTypes.Body)
					- windowFeaturesJ.get(FieldTypes.Body));
			inst.setValue(
					headerWindow_w,
					windowFeaturesI.get(FieldTypes.Header)
							- windowFeaturesJ.get(FieldTypes.Header));
			inst.setValue(
					anchorWindow_w,
					windowFeaturesI.get(FieldTypes.Anchor)
							- windowFeaturesJ.get(FieldTypes.Anchor));
		}
		if (useSmallestWindow) {
			inst.setValue(window_w, smallestWindow);
		}
		if (useUrlDepth) {
			inst.setValue(urlDepth, urlDepthScore);
		}
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
		Attribute titleWindow_w = new Attribute("titleWindow_w");
		Attribute bodyWindow_w = new Attribute("bodyWindow_w");
		Attribute headerWindow_w = new Attribute("headerWindow_w");
		Attribute anchorWindow_w = new Attribute("anchorWindow_w");
		Attribute window_w = new Attribute("window_w");
		Attribute pagerank_w = new Attribute("pagerank_w");
		Attribute bm25_w = new Attribute("bm25_w");
		Attribute urlDepth_w = new Attribute("urlDepth_w");
		Attribute label = new Attribute("label", nominalValues);

		attributes.add(url_w);
		attributes.add(title_w);
		attributes.add(body_w);
		attributes.add(header_w);
		attributes.add(anchor_w);
		if (useFieldwiseSmallestWindow) {
			attributes.add(titleWindow_w);
			attributes.add(bodyWindow_w);
			attributes.add(headerWindow_w);
			attributes.add(anchorWindow_w);
		}
		if (useSmallestWindow) {
			attributes.add(window_w);
		}
		if (usePageRank) {
			attributes.add(pagerank_w);
		}
		if (useBM25) {
			attributes.add(bm25_w);
		}
		if (useUrlDepth) {
			attributes.add(urlDepth_w);
		}
		attributes.add(label);
		dataset = new Instances("train_dataset", attributes, 0);

		/* Add data */
		BM25FeatureExtractor bm25 = null;
		if (useBM25) {
			bm25 = new BM25FeatureExtractor(idfData, trainData);
		}
		int index = 0;
		for (Entry<Query, List<Document>> entry : trainData.entrySet()) {
			Query q = entry.getKey();
			testFeatures.pairs_index_map.put(q.query,
					new HashMap<Pair<String, String>, Integer>());
			List<Document> docsForQuery = entry.getValue();
			for (int i = 0; i < docsForQuery.size(); i++) {

				Map<FieldTypes, Double> features1 = TfIdfFeatureExtractor
						.GetFeature(docsForQuery.get(i), q, idfData);
				double bm25Score = 0;
				double bm25ScoreI = 0;
				if (useBM25) {
					bm25ScoreI = bm25.GetBM25Score(docsForQuery.get(i), q);
				}
				double pagerankScore = 0;
				double pagerankI = 0;
				if (usePageRank) {
					pagerankI = PageRankFeatureExtractor
							.GetPageRank(docsForQuery.get(i));
				}
				Map<FieldTypes, Double> windowFeaturesI = null;
				if (useFieldwiseSmallestWindow) {
					windowFeaturesI = SmallestWindowFeatureExtractor
							.GetSmallestWindowSizeFeatures(docsForQuery.get(i),
									q);
				}
				double smallestWindowScore = 0;
				double smallestWindowI = 0;
				if (useSmallestWindow) {
					smallestWindowI = SmallestWindowFeatureExtractor
							.GetSmallestWindowOfDoc(docsForQuery.get(i), q);
				}
				double urlDepthScore = 0;
				double urlDepthI = 0;
				if (useUrlDepth) {
					urlDepthI = TfIdfFeatureExtractor.GetUrlDepth(docsForQuery
							.get(i));
				}

				for (int j = i + 1; j < docsForQuery.size(); j++) {

					Map<FieldTypes, Double> features2 = TfIdfFeatureExtractor
							.GetFeature(docsForQuery.get(j), q, idfData);
					if (useBM25) {
						double bm25ScoreJ = bm25.GetBM25Score(
								docsForQuery.get(j), q);
						bm25Score = bm25ScoreI - bm25ScoreJ;
					}
					if (usePageRank) {
						double pagerankJ = PageRankFeatureExtractor
								.GetPageRank(docsForQuery.get(j));
						pagerankScore = pagerankI - pagerankJ;
					}
					Map<FieldTypes, Double> windowFeaturesJ = null;
					if (useFieldwiseSmallestWindow) {
						windowFeaturesJ = SmallestWindowFeatureExtractor
								.GetSmallestWindowSizeFeatures(
										docsForQuery.get(i), q);
					}
					if (useSmallestWindow) {
						double smallestWindowJ = SmallestWindowFeatureExtractor
								.GetSmallestWindowOfDoc(docsForQuery.get(j), q);
						smallestWindowScore = smallestWindowI - smallestWindowJ;
					}
					if (useUrlDepth) {
						urlDepthScore = urlDepthI
								- TfIdfFeatureExtractor
										.GetUrlDepth(docsForQuery.get(j));
					}

					Instance inst = new DenseInstance(attributes.size());
					FormInstance(url_w, title_w, body_w, header_w, anchor_w,
							titleWindow_w, bodyWindow_w, headerWindow_w,
							anchorWindow_w, window_w, pagerank_w, bm25_w,
							urlDepth_w, features1, features2, bm25Score,
							pagerankScore, windowFeaturesI, windowFeaturesJ,
							smallestWindowScore, urlDepthScore, inst);
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
