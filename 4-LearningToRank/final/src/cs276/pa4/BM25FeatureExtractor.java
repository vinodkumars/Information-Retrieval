package cs276.pa4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BM25FeatureExtractor {

	// /////////////weights///////////////////////////

	private double urlweight = 0.05244944946879171;
	private double titleweight = 0.43146412838900006;
	private double bodyweight = 9.194286210092347;
	private double headerweight = 0.10821525156870385;
	private double anchorweight = 0.027865331148049267;

	// /////bm25 specific weights///////////////
	private double burl = 0.75;
	private double btitle = 1;
	private double bheader = 0.75;
	private double bbody = 0.75;
	private double banchor = 0.5;
	// 1.3
	// private double k1 = Config.k1;
	private double k1 = 1.7;

	Map<Query, List<Document>> queryDict;
	IDF idfData;
	Map<Document, Map<FieldTypes, Double>> lengths;
	Map<FieldTypes, Double> avgLengths;
	Map<Document, Double> pagerankScores;
	Map<String, Double> weightedFieldCombinedTfs;
	int headerFieldCount = 0, anchorFieldCount = 0;

	public BM25FeatureExtractor(IDF idfData,
			Map<Query, List<Document>> queryDict) {
		this.queryDict = queryDict;
		this.idfData = idfData;
		this.calcAverageLengths();
	}

	private void calcAverageLengths() {
		GetDocFieldLengthsAndPageRanks();
		GetAvgFieldLengths();
	}

	private void GetDocFieldLengthsAndPageRanks() {
		lengths = new HashMap<Document, Map<FieldTypes, Double>>();
		pagerankScores = new HashMap<Document, Double>();
		for (List<Document> docList : this.queryDict.values()) {
			for (Document doc : docList) {
				if (!lengths.containsKey(doc)) {
					Map<FieldTypes, Double> fieldLengths = new HashMap<FieldTypes, Double>();

					if (doc.url != null) {
						fieldLengths.put(FieldTypes.URL, (double) Tokenizer
								.TokenizeOnNonAlphanumeric(doc.url).length);
					} else {
						fieldLengths.put(FieldTypes.URL, (double) 0);
					}
					if (doc.title != null) {
						fieldLengths
								.put(FieldTypes.Title, (double) Tokenizer
										.TokenizeOnSpaces(doc.title).length);
					} else {
						fieldLengths.put(FieldTypes.Title, (double) 0);
					}
					if (doc.body_hits != null) {
						fieldLengths.put(FieldTypes.Body,
								(double) doc.body_length);
					} else {
						fieldLengths.put(FieldTypes.Body, (double) 0);
					}

					if (doc.headers != null) {
						int sum = 0;
						this.headerFieldCount += doc.headers.size();
						for (String header : doc.headers) {
							sum += Tokenizer.TokenizeOnSpaces(header).length;
						}
						fieldLengths.put(FieldTypes.Header, (double) sum);
					}
					// else if (doc.url.toLowerCase().endsWith("pdf")) {
					// fieldLengths.put(FieldTypes.Header,
					// fieldLengths.get(FieldTypes.Title));
					// }
					else {
						fieldLengths.put(FieldTypes.Header, (double) 0);
					}

					if (doc.anchors != null) {
						int sum = 0;
						this.anchorFieldCount += doc.anchors.size();
						for (Entry<String, Integer> entry1 : doc.anchors
								.entrySet()) {
							sum += Tokenizer.TokenizeOnSpaces(entry1.getKey()).length
									* entry1.getValue();
						}
						fieldLengths.put(FieldTypes.Anchor, (double) sum);
					} else {
						fieldLengths.put(FieldTypes.Anchor, (double) 0);
					}

					lengths.put(doc, fieldLengths);

					// pagerankScores.put(doc, (double) doc.page_rank);
				}
			}
		}
	}

	private void GetAvgFieldLengths() {
		avgLengths = new HashMap<FieldTypes, Double>();
		avgLengths.put(FieldTypes.URL, (double) 0);
		avgLengths.put(FieldTypes.Body, (double) 0);
		avgLengths.put(FieldTypes.Anchor, (double) 0);
		avgLengths.put(FieldTypes.Header, (double) 0);
		avgLengths.put(FieldTypes.Title, (double) 0);

		for (Map<FieldTypes, Double> map : lengths.values()) {
			for (Entry<FieldTypes, Double> entry : map.entrySet()) {
				avgLengths.put(entry.getKey(), avgLengths.get(entry.getKey())
						+ entry.getValue());
			}
		}

		for (Entry<FieldTypes, Double> entry : avgLengths.entrySet()) {
			int totalCount;
			if (entry.getKey().equals(FieldTypes.Header)) {
				totalCount = this.headerFieldCount;
			} else if (entry.getKey().equals(FieldTypes.Anchor)) {
				totalCount = this.anchorFieldCount;
			} else {
				totalCount = lengths.size();
			}
			avgLengths.put(entry.getKey(), avgLengths.get(entry.getKey())
					/ totalCount);
		}
	}

	private double getNetScore(Document d) {
		double score = 0.0;

		for (Entry<String, Double> entry : weightedFieldCombinedTfs.entrySet()) {
			double idfVal = this.idfData.idfs.containsKey(entry.getKey()) ? this.idfData.idfs
					.get(entry.getKey()) : this.idfData.termnotfound;
			score += (entry.getValue() * idfVal) / (this.k1 + entry.getValue());
		}

		return score;
	}

	private void normalizeTFs(Map<FieldTypes, Map<String, Double>> tfs,
			Document d, Query q) {
		Map<FieldTypes, Double> fieldLengths = lengths.get(d);
		for (Entry<FieldTypes, Map<String, Double>> entry1 : tfs.entrySet()) {
			double b = 0;
			if (entry1.getKey().equals(FieldTypes.URL)) {
				b = this.burl;
			} else if (entry1.getKey().equals(FieldTypes.Title)) {
				b = this.btitle;
			} else if (entry1.getKey().equals(FieldTypes.Header)) {
				b = this.bheader;
			} else if (entry1.getKey().equals(FieldTypes.Anchor)) {
				b = this.banchor;
			} else if (entry1.getKey().equals(FieldTypes.Body)) {
				b = this.bbody;
			}

			for (Entry<String, Double> entry2 : entry1.getValue().entrySet()) {
				if (avgLengths.get(entry1.getKey()) == 0) {
					entry2.setValue((double) 0);
					continue;
				}
				double divVal = 1
						+ b
						* ((fieldLengths.get(entry1.getKey()) / avgLengths
								.get(entry1.getKey())) - 1);
				entry2.setValue(entry2.getValue() / divVal);
			}
		}
	}

	private void GetWeightedFieldCombinedTfs(
			Map<FieldTypes, Map<String, Double>> tfs) {
		weightedFieldCombinedTfs = new HashMap<String, Double>();
		for (Entry<FieldTypes, Map<String, Double>> entry1 : tfs.entrySet()) {
			double weight = 0;
			if (entry1.getKey().equals(FieldTypes.URL)) {
				weight = this.urlweight;
			} else if (entry1.getKey().equals(FieldTypes.Title)) {
				weight = this.titleweight;
			} else if (entry1.getKey().equals(FieldTypes.Header)) {
				weight = this.headerweight;
			} else if (entry1.getKey().equals(FieldTypes.Anchor)) {
				weight = this.anchorweight;
			} else if (entry1.getKey().equals(FieldTypes.Body)) {
				weight = this.bodyweight;
			}

			for (Entry<String, Double> entry2 : entry1.getValue().entrySet()) {
				if (!weightedFieldCombinedTfs.containsKey(entry2.getKey())) {
					weightedFieldCombinedTfs.put(entry2.getKey(), weight
							* entry2.getValue());
				} else {
					weightedFieldCombinedTfs.put(entry2.getKey(),
							weightedFieldCombinedTfs.get(entry2.getKey())
									+ (weight * entry2.getValue()));
				}
			}
		}

	}

	public double GetBM25Score(Document d, Query q) {

		Map<FieldTypes, Map<String, Double>> tfs = TfExtractor.getDocTermFreqs(
				d, q);

		this.normalizeTFs(tfs, d, q);

		this.GetWeightedFieldCombinedTfs(tfs);

		// Map<String, Double> tfQuery = getQueryFreqs(q);

		return getNetScore(d);
	}
}
