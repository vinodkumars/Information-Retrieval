package cs276.pa4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SmallestWindowFeatureExtractor {

	private static List<List<Integer>> data = new ArrayList<List<Integer>>();

	public static double GetSmallestWindowOfDoc(Document d, Query q) {
		double retVal = 0;
		Map<FieldTypes, Double> features = GetSmallestWindowSizeFeatures(d, q);
		for (Double entry : features.values()) {
			if (entry > retVal) {
				retVal = entry;
			}
		}
		return retVal;
	}

	public static Map<FieldTypes, Double> GetSmallestWindowSizeFeatures(
			Document d, Query q) {
		int min = Integer.MAX_VALUE;
		int windowSize;
		Map<FieldTypes, Double> features = new HashMap<FieldTypes, Double>();
		features.put(FieldTypes.Anchor, (double) 0);
		features.put(FieldTypes.Body, (double) 0);
		features.put(FieldTypes.Header, (double) 0);
		features.put(FieldTypes.Title, (double) 0);

		// Title
		if (d.title != null) {
			ExtractDataFromString(d.title, q.words);
			if (!data.isEmpty()) {
				min = GetSmallestWindowSizeForData(data);
				if (min != Integer.MAX_VALUE) {
					features.put(FieldTypes.Title, (double) q.words.size()
							/ (double) min);
				}
			}
		}

		// Headers
		min = Integer.MAX_VALUE;
		if (d.headers != null) {
			for (String header : d.headers) {
				ExtractDataFromString(header, q.words);
				if (!data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
						min = windowSize;
					}
				}
			}
			if (min != Integer.MAX_VALUE) {
				features.put(FieldTypes.Header, (double) q.words.size()
						/ (double) min);
			}
		}

		// Anchors
		min = Integer.MAX_VALUE;
		if (d.anchors != null) {
			for (String anchor : d.anchors.keySet()) {
				ExtractDataFromString(anchor, q.words);
				if (!data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
						min = windowSize;
					}
				}
			}
			if (min != Integer.MAX_VALUE) {
				features.put(FieldTypes.Anchor, (double) q.words.size()
						/ (double) min);
			}
		}

		// Body
		if (d.body_hits != null) {
			clearData();
			boolean isContinue = true;
			for (String queryTerm : q.words) {
				if (!d.body_hits.keySet().contains(queryTerm)) {
					isContinue = false;
					break;
				}
			}
			if (!isContinue) {
				return features;
			}
			for (List<Integer> list : d.body_hits.values()) {
				data.add(list);
			}
			min = GetSmallestWindowSizeForData(data);
			if (min != Integer.MAX_VALUE) {
				features.put(FieldTypes.Body, (double) q.words.size()
						/ (double) min);
			}
		}

		for (Entry<FieldTypes, Double> entry : features.entrySet()) {
			entry.setValue(Math.pow(2, 10 * entry.getValue()));
		}

		return features;
	}

	private static void ExtractDataFromString(String fieldString,
			List<String> queryTerms) {
		clearData();
		String[] fieldTerms = Tokenizer.TokenizeOnSpaces(fieldString);
		for (String queryTerm : queryTerms) {
			List<Integer> pos = new ArrayList<Integer>();
			for (int i = 0; i < fieldTerms.length; i++) {
				if (StemAndCompare.StemString(fieldTerms[i].toLowerCase())
						.equals(StemAndCompare.StemString(queryTerm
								.toLowerCase()))) {
					pos.add(i + 1);
				}
			}
			if (pos.isEmpty()) {
				clearData();
				return;
			}
			data.add(pos);
		}
	}

	private static void clearData() {
		if (data.isEmpty()) {
			return;
		}
		for (List<Integer> list : data) {
			list.clear();
		}
		data.clear();
	}

	private static int GetSmallestWindowSizeForData(List<List<Integer>> data) {
		List<Integer> queue = new ArrayList<Integer>();
		int min = Integer.MAX_VALUE;
		for (Integer i : data.get(0)) {
			queue.clear();
			queue.add(i);
			FindSmallestWindow(data, 1, queue);
			int windowSize = GetWindowSize(queue);
			if (windowSize < min) {
				min = windowSize;
			}
		}
		return min;
	}

	private static int GetWindowSize(List<Integer> queue) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Integer i : queue) {
			if (i.intValue() < min) {
				min = i.intValue();
			}
			if (i.intValue() > max) {
				max = i.intValue();
			}
		}
		return max - min + 1;
	}

	private static void FindSmallestWindow(List<List<Integer>> data,
			int currentDepth, List<Integer> queue) {
		/*
		 * @//TODO : Your code here
		 */
		if (currentDepth == data.size()) {
			return;
		}
		int min = Integer.MAX_VALUE;
		Integer toAdd = null;
		for (Integer candidate : data.get(currentDepth)) {
			int dist = FindMinDist(queue, candidate);
			if (dist < min) {
				min = dist;
				toAdd = candidate;
			} else {
				break;
			}
		}
		queue.add(toAdd);
		FindSmallestWindow(data, currentDepth + 1, queue);
	}

	private static int FindMinDist(List<Integer> queue, Integer candidate) {
		int min = Integer.MAX_VALUE;
		for (Integer i : queue) {
			int dist = Math.abs(i.intValue() - candidate.intValue());
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}

}
