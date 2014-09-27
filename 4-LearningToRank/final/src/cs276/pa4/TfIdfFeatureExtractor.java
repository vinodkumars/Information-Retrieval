package cs276.pa4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import stemmer.PorterStemmer;

public class TfIdfFeatureExtractor {

	private static double smoothingBodyLength = 500;

	private static void SublinearScale(Map<String, Double> tfQuery) {
		for (Entry<String, Double> mapEntry : tfQuery.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue(Math.log(mapEntry.getValue()) + 1);
			}
		}
	}

	private static void SublinearScaleDocTermFreqs(
			Map<FieldTypes, Map<String, Double>> tfs) {
		for (Map<String, Double> fieldMap : tfs.values()) {
			SublinearScale(fieldMap);
		}
	}

	private static void normalizeTFs(Map<FieldTypes, Map<String, Double>> tfs,
			Document d) {
		// smoothing + normalization
		for (Map<String, Double> fieldMap : tfs.values()) {
			for (Entry<String, Double> mapEntry : fieldMap.entrySet()) {
				if (mapEntry.getValue() != 0) {
					mapEntry.setValue(mapEntry.getValue()
							/ (double) (d.body_length + smoothingBodyLength));
				}
			}
		}
	}

	private static void normalizeBodyTFs(
			Map<FieldTypes, Map<String, Double>> tfs, Document d) {
		if (d.body_hits == null) {
			return;
		}
		// smoothing + normalization
		Map<String, Double> fieldMap = tfs.get(FieldTypes.Body);
		for (Entry<String, Double> mapEntry : fieldMap.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue(mapEntry.getValue()
						/ (double) (d.body_length + smoothingBodyLength));
			}
		}
	}

	private static void normalizeTitleTFs(
			Map<FieldTypes, Map<String, Double>> tfs, Document d) {
		if (d.title == null) {
			return;
		}
		// smoothing + normalization
		Map<String, Double> fieldMap = tfs.get(FieldTypes.Title);
		int titleLength = Tokenizer.TokenizeOnSpaces(d.title).length;
		for (Entry<String, Double> mapEntry : fieldMap.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue(mapEntry.getValue() / (double) (titleLength));
			}
		}
	}

	private static void normalizeHeaderTFs(
			Map<FieldTypes, Map<String, Double>> tfs, Document d) {
		if (d.headers == null) {
			return;
		}
		// smoothing + normalization
		Map<String, Double> fieldMap = tfs.get(FieldTypes.Header);
		int headerLength = 0;
		for (String h : d.headers) {
			headerLength += Tokenizer.TokenizeOnSpaces(h).length;
		}
		for (Entry<String, Double> mapEntry : fieldMap.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue(mapEntry.getValue() / (double) (headerLength));
			}
		}
	}

	private static void IdfWeight(Map<String, Double> tfQuery, IDF idfData) {
		for (Entry<String, Double> entry : tfQuery.entrySet()) {
			double idfVal = idfData.idfs.containsKey(entry.getKey()) ? idfData.idfs
					.get(entry.getKey()) : idfData.termnotfound;
			entry.setValue(entry.getValue() * idfVal);
		}
	}

	public static double GetUrlDepth(Document d) {
		return d.url.split("/").length;
	}

	public static Map<FieldTypes, Double> GetFeature(Document d, Query q,
			IDF idfData) {
		Map<FieldTypes, Double> features = new HashMap<FieldTypes, Double>();

		Map<FieldTypes, Map<String, Double>> tfs = TfExtractor.getDocTermFreqs(
				d, q);
		SublinearScaleDocTermFreqs(tfs);
		// normalizeTFs(tfs, d);
		normalizeBodyTFs(tfs, d);
		normalizeHeaderTFs(tfs, d);
		normalizeTitleTFs(tfs, d);
		// if (d.url.toLowerCase().endsWith("pdf")) {
		// tfs.put(FieldTypes.Header, tfs.get(FieldTypes.Title));
		// }

		Map<String, Double> tfQuery = TfExtractor.getQueryFreqs(q);
		SublinearScale(tfQuery);
		IdfWeight(tfQuery, idfData);

		features.put(FieldTypes.Anchor, (double) 0);
		features.put(FieldTypes.Body, (double) 0);
		features.put(FieldTypes.Header, (double) 0);
		features.put(FieldTypes.Title, (double) 0);
		features.put(FieldTypes.URL, (double) 0);

		for (Entry<FieldTypes, Map<String, Double>> entry1 : tfs.entrySet()) {
			double score = 0;
			for (Entry<String, Double> entry2 : entry1.getValue().entrySet()) {
				score += entry2.getValue() * tfQuery.get(entry2.getKey());
			}
			features.put(entry1.getKey(), score);
		}

		return features;
	}
}
