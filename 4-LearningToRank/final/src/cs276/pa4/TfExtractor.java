package cs276.pa4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfExtractor {

	public static Map<FieldTypes, Map<String, Double>> getDocTermFreqs(
			Document d, Query q) {
		Map<FieldTypes, Map<String, Double>> tfs = new HashMap<FieldTypes, Map<String, Double>>();
		tfs.put(FieldTypes.URL, GetDocUrlTermFreqs(d.url, q));
		tfs.put(FieldTypes.Title, GetDocTitleTermFreqs(d.title, q));
		tfs.put(FieldTypes.Anchor, GetDocAnchorsTermFreqs(d.anchors, q));
		tfs.put(FieldTypes.Body, GetDocBodyTermFreqs(d.body_hits, q));
		tfs.put(FieldTypes.Header, GetDocHeaderTermFreqs(d.headers, q));
		return tfs;
	}

	private static Map<String, Double> GetDocTitleTermFreqs(String title,
			Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (title == null) {
			for (String queryTerm : q.words) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String term : Tokenizer.TokenizeOnSpaces(title)) {
			if (!fieldMap.containsKey(term)) {
				fieldMap.put(term, 1);
			} else {
				fieldMap.put(term, fieldMap.get(term).intValue() + 1);
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.words) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			String key;
			if ((key = StemAndCompare.Compare(fieldMap, queryTerm)) != null) {
				retVal.put(queryTerm, (double) fieldMap.get(key));
			}
		}
		return retVal;
	}

	private static Map<String, Double> GetDocHeaderTermFreqs(
			List<String> header, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (header == null) {
			for (String queryTerm : q.words) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String h : header) {
			for (String term : Tokenizer.TokenizeOnSpaces(h)) {
				if (!fieldMap.containsKey(term)) {
					fieldMap.put(term, 1);
				} else {
					fieldMap.put(term, fieldMap.get(term).intValue() + 1);
				}
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.words) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			String key;
			if ((key = StemAndCompare.Compare(fieldMap, queryTerm)) != null) {
				retVal.put(queryTerm, (double) fieldMap.get(key));
			}
		}
		return retVal;
	}

	private static Map<String, Double> GetDocBodyTermFreqs(
			Map<String, List<Integer>> body, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (body == null) {
			for (String queryTerm : q.words) {
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
		for (String queryTerm : q.words) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			String key;
			if ((key = StemAndCompare.Compare(fieldMap, queryTerm)) != null) {
				retVal.put(queryTerm, (double) fieldMap.get(key));
			}
		}
		return retVal;
	}

	private static Map<String, Double> GetDocAnchorsTermFreqs(
			Map<String, Integer> anchors, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (anchors == null) {
			for (String queryTerm : q.words) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String anchor : anchors.keySet()) {
			for (String term : Tokenizer.TokenizeOnNonAlphanumeric(anchor)) {
				if (!fieldMap.containsKey(term)) {
					fieldMap.put(term, anchors.get(anchor));
				} else {
					fieldMap.put(term,
							fieldMap.get(term).intValue() + anchors.get(anchor));
				}
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.words) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			String key;
			if ((key = StemAndCompare.Compare(fieldMap, queryTerm)) != null) {
				retVal.put(queryTerm, (double) fieldMap.get(key));
			}
		}
		return retVal;
	}

	private static Map<String, Double> GetDocUrlTermFreqs(String url, Query q) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		if (url == null) {
			for (String queryTerm : q.words) {
				retVal.put(queryTerm, (double) 0);
			}
			return retVal;
		}
		Map<String, Integer> fieldMap = new HashMap<String, Integer>();
		// Take counts of different terms in the field
		for (String term : Tokenizer.TokenizeOnNonAlphanumeric(url)) {
			if (!fieldMap.containsKey(term)) {
				fieldMap.put(term, 1);
			} else {
				fieldMap.put(term, fieldMap.get(term).intValue() + 1);
			}
		}
		// Transfer counts of the query terms calculated above
		for (String queryTerm : q.words) {
			if (!retVal.containsKey(queryTerm)) {
				retVal.put(queryTerm, (double) 0);
			}
			String key;
			if ((key = CheckQueryTermInFieldMap(fieldMap, queryTerm)) != null) {
				retVal.put(queryTerm, (double) fieldMap.get(key));
			}
		}
		return retVal;
	}

	public static Map<String, Double> getQueryFreqs(Query q) {
		Map<String, Double> tfQuery = new HashMap<String, Double>();
		for (String term : Tokenizer.TokenizeOnSpaces(q.query)) {
			if (!tfQuery.containsKey(term)) {
				tfQuery.put(term, (double) 1);
			} else {
				tfQuery.put(term, tfQuery.get(term) + 1);
			}
		}
		return tfQuery;
	}

	private static String CheckQueryTermInFieldMap(
			Map<String, Integer> fieldMap, String queryTerm) {
		for (String entry : fieldMap.keySet()) {
			if (entry.contains(queryTerm) || queryTerm.contains(entry)) {
				return entry;
			}
		}
		return null;
	}
}
