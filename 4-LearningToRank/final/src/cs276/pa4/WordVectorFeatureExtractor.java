package cs276.pa4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ejml.simple.SimpleMatrix;

public class WordVectorFeatureExtractor {
	Embedding embedding;

	public WordVectorFeatureExtractor() {
		String wordVectorFile = "data/wordVectors.txt.gz";
		this.embedding = new Embedding(wordVectorFile, false);
	}

	private void IdfWeight(Map<String, Double> tfQuery, IDF idfData) {
		for (Entry<String, Double> entry : tfQuery.entrySet()) {
			double idfVal = idfData.idfs.containsKey(entry.getKey()) ? idfData.idfs
					.get(entry.getKey()) : idfData.termnotfound;
			entry.setValue(entry.getValue() * idfVal);
		}
	}

	private void SublinearScale(Map<String, Double> tfQuery) {
		for (Entry<String, Double> mapEntry : tfQuery.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue(Math.log(mapEntry.getValue()) + 1);
			}
		}
	}

	private void SublinearScaleDocTermFreqs(
			Map<FieldTypes, Map<String, Double>> tfs) {
		for (Map<String, Double> fieldMap : tfs.values()) {
			SublinearScale(fieldMap);
		}
	}

	private Map<FieldTypes, Map<String, Double>> GetDocTFs(Document d) {
		Map<FieldTypes, Map<String, Double>> retVal = new HashMap<FieldTypes, Map<String, Double>>();

		Map<String, Double> fieldMap1 = new HashMap<String, Double>();
		for (String term : Tokenizer.TokenizeOnNonAlphanumeric(d.url)) {
			if (!fieldMap1.containsKey(term)) {
				fieldMap1.put(term, (double) 1);
			} else {
				fieldMap1.put(term, fieldMap1.get(term) + 1);
			}
		}
		retVal.put(FieldTypes.URL, fieldMap1);

		Map<String, Double> fieldMap2 = new HashMap<String, Double>();
		if (d.title != null) {
			for (String term : Tokenizer.TokenizeOnSpaces(d.title)) {
				if (!fieldMap2.containsKey(term)) {
					fieldMap2.put(term, (double) 1);
				} else {
					fieldMap2.put(term, fieldMap2.get(term) + 1);
				}
			}
		}
		retVal.put(FieldTypes.Title, fieldMap2);

		Map<String, Double> fieldMap3 = new HashMap<String, Double>();
		if (d.anchors != null) {
			for (Entry<String, Integer> entry : d.anchors.entrySet()) {
				for (String term : Tokenizer.TokenizeOnSpaces(entry.getKey())) {
					if (!fieldMap3.containsKey(term)) {
						fieldMap3.put(term, (double) entry.getValue());
					} else {
						fieldMap3.put(term,
								fieldMap3.get(term) + entry.getValue());
					}
				}
			}
		}
		retVal.put(FieldTypes.Anchor, fieldMap3);

		Map<String, Double> fieldMap4 = new HashMap<String, Double>();
		if (d.headers != null) {
			for (String header : d.headers) {
				for (String term : Tokenizer.TokenizeOnSpaces(header)) {
					if (!fieldMap4.containsKey(term)) {
						fieldMap4.put(term, (double) 1);
					} else {
						fieldMap4.put(term, fieldMap4.get(term) + 1);
					}
				}
			}
		}
		retVal.put(FieldTypes.Header, fieldMap4);

		Map<String, Double> fieldMap5 = new HashMap<String, Double>();
		if (d.body_hits != null) {
			for (Entry<String, List<Integer>> entry : d.body_hits.entrySet()) {
				for (String term : Tokenizer.TokenizeOnSpaces(entry.getKey())) {
					if (!fieldMap5.containsKey(term)) {
						fieldMap5.put(term, (double) entry.getValue().size());
					} else {
						fieldMap5.put(term, fieldMap5.get(term)
								+ entry.getValue().size());
					}
				}
			}
		}
		retVal.put(FieldTypes.Body, fieldMap5);

		return retVal;
	}

	public Map<FieldTypes, Double> GetFeature(Document d, Query q, IDF idfData) {
		Map<FieldTypes, Double> features = new HashMap<FieldTypes, Double>();

		Map<FieldTypes, Map<String, Double>> tfs = GetDocTFs(d);
		SublinearScaleDocTermFreqs(tfs);

		Map<String, Double> tfQuery = TfExtractor.getQueryFreqs(q);
		SublinearScale(tfQuery);
		IdfWeight(tfQuery, idfData);

		features.put(FieldTypes.Anchor, (double) 0);
		features.put(FieldTypes.Body, (double) 0);
		features.put(FieldTypes.Header, (double) 0);
		features.put(FieldTypes.Title, (double) 0);
		features.put(FieldTypes.URL, (double) 0);

		SimpleMatrix qVec = null;
		for (Entry<String, Double> entry : tfQuery.entrySet()) {
			SimpleMatrix v = this.embedding.get(entry.getKey());
			if (v == null) {
				continue;
			}
			v = v.scale(entry.getValue());
			if (qVec == null) {
				qVec = v;
			} else {
				qVec = qVec.plus(v);
			}
		}

		for (Entry<FieldTypes, Map<String, Double>> entry1 : tfs.entrySet()) {
			SimpleMatrix dVec = null;
			for (Entry<String, Double> entry2 : entry1.getValue().entrySet()) {
				SimpleMatrix v = this.embedding.get(entry2.getKey());
				if (v == null) {
					continue;
				}
				v = v.scale(entry2.getValue());
				if (dVec == null) {
					dVec = v;
				} else {
					dVec = dVec.plus(v);
				}
			}
			if (dVec != null) {
				features.put(entry1.getKey(), dVec.dot(qVec));
			}
		}

		return features;
	}

}
