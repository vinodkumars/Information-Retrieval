package edu.stanford.cs276;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.cs276.util.IDF;

public abstract class AScorer {

	IDF idfData;
	static final String[] TFTYPES = { "url", "title", "body", "header",
			"anchor" };
	static final int urlIndex = 0, titleIndex = 1, bodyIndex = 2,
			headerIndex = 3, anchorIndex = 4;
	static final String termNotFound = "termnotfound";

	public AScorer(IDF idfData) {
		this.idfData = idfData;
	}

	// scores each document for each query
	public abstract double getSimScore(Document d, Query q);

	// handle the query vector
	public Map<String, Double> getQueryFreqs(Query q) {
		Map<String, Double> tfQuery = new HashMap<String, Double>();

		/*
		 * @//TODO : Your code here
		 */

		for (String term : q.queryWords) {
			if (!tfQuery.containsKey(term)) {
				tfQuery.put(term, (double) 1);
			} else {
				tfQuery.put(term, tfQuery.get(term) + 1);
			}
		}

		return tfQuery;
	}

	// //////////////////Initialization/Parsing Methods/////////////////////

	/*
	 * @//TODO : Your code here
	 */

	public String[] TokenizeOnSpaces(String s) {
		String[] retVal = s.trim().split("\\s+");
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = retVal[i].toLowerCase();
		}
		return retVal;
	}

	public String[] TokenizeOnNonAlphanumeric(String s) {
		String[] retVal = s.trim().split("\\W+");
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = retVal[i].toLowerCase();
		}
		return retVal;
	}

	// //////////////////////////////////////////////////////

	/*
	 * / Creates the various kinds of term frequences (url, title, body, header,
	 * and anchor) You can override this if you'd like, but it's likely that
	 * your concrete classes will share this implementation
	 */
	public Map<String, Map<String, Double>> getDocTermFreqs(Document d, Query q) {
		// map from tf type -> queryWord -> score
		Map<String, Map<String, Double>> tfs = new HashMap<String, Map<String, Double>>();

		// //////////////////Initialization/////////////////////

		/*
		 * @//TODO : Your code here
		 */

		// //////////////////////////////////////////////////////

		// ////////handle counts//////

		// loop through query terms increasing relevant tfs
		// for (String queryWord : q.queryWords) {
		// /*
		// * @//TODO : Your code here
		// */
		//
		// }

		tfs.put(TFTYPES[urlIndex], this.GetDocUrlTermFreqs(d.url, q));
		tfs.put(TFTYPES[titleIndex], this.GetDocTitleTermFreqs(d.title, q));
		tfs.put(TFTYPES[bodyIndex], this.GetDocBodyTermFreqs(d.body_hits, q));
		tfs.put(TFTYPES[headerIndex], this.GetDocHeaderTermFreqs(d.headers, q));
		tfs.put(TFTYPES[anchorIndex], this.GetDocAnchorsTermFreqs(d.anchors, q));

		return tfs;
	}

	private Map<String, Double> GetDocTitleTermFreqs(String title, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (title == null) {
			for (String queryTerm : q.queryWords) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String term : TokenizeOnSpaces(title)) {
			if (!fieldMap.containsKey(term)) {
				fieldMap.put(term, 1);
			} else {
				fieldMap.put(term, fieldMap.get(term).intValue() + 1);
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.queryWords) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			if (fieldMap.containsKey(queryTerm) && retVal.get(queryTerm) == 0) {
				retVal.put(queryTerm, (double) fieldMap.get(queryTerm));
			}
		}
		return retVal;
	}

	private Map<String, Double> GetDocHeaderTermFreqs(List<String> header,
			Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (header == null) {
			for (String queryTerm : q.queryWords) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String h : header) {
			for (String term : TokenizeOnSpaces(h)) {
				if (!fieldMap.containsKey(term)) {
					fieldMap.put(term, 1);
				} else {
					fieldMap.put(term, fieldMap.get(term).intValue() + 1);
				}
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.queryWords) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			if (fieldMap.containsKey(queryTerm) && retVal.get(queryTerm) == 0) {
				retVal.put(queryTerm, (double) fieldMap.get(queryTerm));
			}
		}
		return retVal;
	}

	private Map<String, Double> GetDocBodyTermFreqs(
			Map<String, List<Integer>> body, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (body == null) {
			for (String queryTerm : q.queryWords) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String term : body.keySet()) {
			if (!fieldMap.containsKey(term)) {
				fieldMap.put(term, body.get(term).size());
			} else {
				fieldMap.put(term,
						fieldMap.get(term).intValue() + body.get(term).size());
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.queryWords) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			if (fieldMap.containsKey(queryTerm) && retVal.get(queryTerm) == 0) {
				retVal.put(queryTerm, (double) fieldMap.get(queryTerm));
			}
		}
		return retVal;
	}

	private Map<String, Double> GetDocAnchorsTermFreqs(
			Map<String, Integer> anchors, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (anchors == null) {
			for (String queryTerm : q.queryWords) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String anchor : anchors.keySet()) {
			for (String term : TokenizeOnNonAlphanumeric(anchor)) {
				if (!fieldMap.containsKey(term)) {
					fieldMap.put(term, anchors.get(anchor));
				} else {
					fieldMap.put(term,
							fieldMap.get(term).intValue() + anchors.get(anchor));
				}
			}
		}

		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.queryWords) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			if (fieldMap.containsKey(queryTerm) && retVal.get(queryTerm) == 0) {
				retVal.put(queryTerm, (double) fieldMap.get(queryTerm));
			}
		}
		return retVal;
	}

	private Map<String, Double> GetDocUrlTermFreqs(String url, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (url == null) {
			for (String queryTerm : q.queryWords) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String term : TokenizeOnNonAlphanumeric(url)) {
			if (!fieldMap.containsKey(term)) {
				fieldMap.put(term, 1);
			} else {
				fieldMap.put(term, fieldMap.get(term).intValue() + 1);
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.queryWords) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			if (fieldMap.containsKey(queryTerm) && retVal.get(queryTerm) == 0) {
				retVal.put(queryTerm, (double) fieldMap.get(queryTerm));
			}
		}
		return retVal;
	}

}
