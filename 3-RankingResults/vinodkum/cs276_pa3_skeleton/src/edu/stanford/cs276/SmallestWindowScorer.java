package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.cs276.util.Config;
import edu.stanford.cs276.util.IDF;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
public class SmallestWindowScorer extends CosineSimilarityScorer {

	// ///smallest window specific hyper parameters////////
	double B = Config.B;
	double boostmod = -1;
	List<List<Integer>> data = new ArrayList<List<Integer>>();

	// ////////////////////////////

	public SmallestWindowScorer(IDF idfData,
			Map<Query, Map<String, Document>> queryDict) {
		super(idfData);
		// handleSmallestWindow();
	}

	public void handleSmallestWindow() {
		/*
		 * @//TODO : Your code here
		 */

	}

	public int GetSmallestWindowSizeForDoc(Document d, Set<String> queryTerms) {
		int min = Integer.MAX_VALUE;
		int windowSize;

		// Title
		if (d.title != null) {
			ExtractDataFromString(d.title, queryTerms);
			if (!this.data.isEmpty()) {
				if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
					min = windowSize;
				}
			}
		}

		// Headers
		if (d.headers != null) {
			for (String header : d.headers) {
				ExtractDataFromString(header, queryTerms);
				if (!this.data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
						min = windowSize;
					}
				}
			}
		}

		// Anchors
		if (d.anchors != null) {
			for (String anchor : d.anchors.keySet()) {
				ExtractDataFromString(anchor, queryTerms);
				if (!this.data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
						min = windowSize;
					}
				}
			}
		}

		// Body
		if (d.body_hits != null) {
			this.data.clear();
			boolean isContinue = true;
			for (String queryTerm : queryTerms) {
				if (!d.body_hits.keySet().contains(queryTerm)) {
					isContinue = false;
					break;
				}
			}
			if (!isContinue) {
				return min;
			}
			for (List<Integer> list : d.body_hits.values()) {
				data.add(list);
			}
			if ((windowSize = GetSmallestWindowSizeForData(data)) < min) {
				min = windowSize;
			}
		}
		return min;
	}

	public void ExtractDataFromString(String fieldString, Set<String> queryTerms) {
		clearData();
		String[] fieldTerms = fieldString.split("\\s+");
		for (String queryTerm : queryTerms) {
			List<Integer> pos = new ArrayList<Integer>();
			for (int i = 0; i < fieldTerms.length; i++) {
				if (fieldTerms[i].toLowerCase().equals(queryTerm.toLowerCase())) {
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

	public void clearData() {
		if (this.data.isEmpty()) {
			return;
		}
		for (List<Integer> list : this.data) {
			list.clear();
		}
		this.data.clear();
	}

	public int GetSmallestWindowSizeForData(List<List<Integer>> data) {
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

	public int GetWindowSize(List<Integer> queue) {
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

	public void FindSmallestWindow(List<List<Integer>> data, int currentDepth,
			List<Integer> queue) {
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

	public int FindMinDist(List<Integer> queue, Integer candidate) {
		int min = Integer.MAX_VALUE;
		for (Integer i : queue) {
			int dist = Math.abs(i.intValue() - candidate.intValue());
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}

	public double GetBoostedScore(Map<String, Map<String, Double>> tfs,
			Query q, Map<String, Double> tfQuery, Document d) {
		double baseScore = this.getNetScore(tfs, q, tfQuery, d);
		int windowSize = GetSmallestWindowSizeForDoc(d, tfQuery.keySet());
		double boost = 1;
		if (windowSize < Integer.MAX_VALUE) {
			boost += ((this.B * (double) tfQuery.keySet().size()) / (double) windowSize);
		}
		return boost * baseScore;
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

		this.SublinearScaleDocTermFreqs(tfs);

		this.normalizeTFs(tfs, d, q);

		Map<String, Double> tfQuery = getQueryFreqs(q);

		this.SublinearScale(tfQuery);

		this.IdfWeight(tfQuery);

		return GetBoostedScore(tfs, q, tfQuery, d);
	}

}
