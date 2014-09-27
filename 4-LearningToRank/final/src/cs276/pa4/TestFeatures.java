package cs276.pa4;

import weka.core.Instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ciir.umass.edu.learning.DataPoint;

public class TestFeatures {

	/* This is just a sample class to store the result */

	/* Test features */
	Instances features;
	List<DataPoint> datapoints = null;

	/*
	 * Associate query-doc pair to its index within FEATURES instances {query ->
	 * {doc -> index}}
	 * 
	 * For example, you can get the feature for a pair of (query, url) using:
	 * features.get(index_map.get(query).get(url));
	 */
	Map<String, Map<String, Integer>> index_map = null;
	HashMap<String, Map<Pair<String, String>, Integer>> pairs_index_map = null;
}
