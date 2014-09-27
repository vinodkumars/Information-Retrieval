package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.cs276.util.Config;
import edu.stanford.cs276.util.IDF;

public class BM25Scorer extends AScorer {
	Map<Query, Map<String, Document>> queryDict;

	public BM25Scorer(IDF idfData, Map<Query, Map<String, Document>> queryDict) {
		super(idfData);
		this.queryDict = queryDict;
		this.calcAverageLengths();
	}

	// /////////////weights///////////////////////////
	double urlweight = Config.urlweight;
	double titleweight = Config.titleweight;
	double bodyweight = Config.bodyweight;
	double headerweight = Config.headerweight;
	double anchorweight = Config.anchorweight;

	// /////bm25 specific weights///////////////
	double burl = Config.burl;
	double btitle = Config.btitle;
	double bheader = Config.bheader;
	double bbody = Config.bbody;
	double banchor = Config.banchor;

	double k1 = Config.k1;
	double pageRankLambda = Config.pageRankLambda;
	double pageRankLambdaPrime = Config.pageRankLambdaPrime;
	// ////////////////////////////////////////

	// //////////bm25 data structures--feel free to modify ////////

	Map<Document, Map<String, Double>> lengths;
	Map<String, Double> avgLengths;
	Map<Document, Double> pagerankScores;
	Map<String, Double> weightedFieldCombinedTfs;
	int headerFieldCount = 0, anchorFieldCount = 0;

	// ////////////////////////////////////////

	// sets up average lengths for bm25, also handles pagerank
	public void calcAverageLengths() {
		GetDocFieldLengthsAndPageRanks();
		GetAvgFieldLengths();
		int tmp = 0;
		/*
		 * @//TODO : Your code here
		 */
		// Note to self: Need to confirm what this is
		// normalize avgLengths
		// for (String tfType : this.TFTYPES) {
		// /*
		// * @//TODO : Your code here
		// */
		//
		// }

	}

	public void GetDocFieldLengthsAndPageRanks() {
		lengths = new HashMap<Document, Map<String, Double>>();
		pagerankScores = new HashMap<Document, Double>();
		for (Map<String, Document> map : this.queryDict.values()) {
			for (Entry<String, Document> entry : map.entrySet()) {
				Document doc = entry.getValue();
				if (!lengths.containsKey(doc)) {
					Map<String, Double> fieldLengths = new HashMap<String, Double>();

					if (doc.url != null) {
						fieldLengths
								.put(TFTYPES[urlIndex],
										(double) TokenizeOnNonAlphanumeric(doc.url).length);
					} else {
						fieldLengths.put(TFTYPES[urlIndex], (double) 0);
					}
					if (doc.title != null) {
						fieldLengths.put(TFTYPES[titleIndex],
								(double) TokenizeOnSpaces(doc.title).length);
					} else {
						fieldLengths.put(TFTYPES[titleIndex], (double) 0);
					}
					if (doc.body_hits != null) {
						fieldLengths.put(TFTYPES[bodyIndex],
								(double) doc.body_length);
					} else {
						fieldLengths.put(TFTYPES[bodyIndex], (double) 0);
					}

					if (doc.headers != null) {
						int sum = 0;
						this.headerFieldCount += doc.headers.size();
						for (String header : doc.headers) {
							sum += TokenizeOnSpaces(header).length;
						}
						fieldLengths.put(TFTYPES[headerIndex], (double) sum);
					} else {
						fieldLengths.put(TFTYPES[headerIndex], (double) 0);
					}

					if (doc.anchors != null) {
						int sum = 0;
						this.anchorFieldCount += doc.anchors.size();
						for (Entry<String, Integer> entry1 : doc.anchors
								.entrySet()) {
							sum += TokenizeOnSpaces(entry1.getKey()).length
									* entry1.getValue();
						}
						fieldLengths.put(TFTYPES[anchorIndex], (double) sum);
					} else {
						fieldLengths.put(TFTYPES[anchorIndex], (double) 0);
					}

					lengths.put(doc, fieldLengths);

					// pagerankScores.put(doc, (double) doc.page_rank);
				}
			}
		}
	}

	public void GetAvgFieldLengths() {
		avgLengths = new HashMap<String, Double>();
		avgLengths.put(TFTYPES[urlIndex], (double) 0);
		avgLengths.put(TFTYPES[bodyIndex], (double) 0);
		avgLengths.put(TFTYPES[anchorIndex], (double) 0);
		avgLengths.put(TFTYPES[headerIndex], (double) 0);
		avgLengths.put(TFTYPES[titleIndex], (double) 0);

		for (Map<String, Double> map : lengths.values()) {
			for (Entry<String, Double> entry : map.entrySet()) {
				avgLengths.put(entry.getKey(), avgLengths.get(entry.getKey())
						+ entry.getValue());
			}
		}

		for (Entry<String, Double> entry : avgLengths.entrySet()) {
			int totalCount;
			if (entry.getKey().equals(TFTYPES[headerIndex])) {
				totalCount = this.headerFieldCount;
			} else if (entry.getKey().equals(TFTYPES[anchorIndex])) {
				totalCount = this.anchorFieldCount;
			} else {
				totalCount = lengths.size();
			}
			avgLengths.put(entry.getKey(), avgLengths.get(entry.getKey())
					/ totalCount);
		}
	}

	// //////////////////////////////////

	public double getNetScore(Document d) {
		double score = 0.0;

		/*
		 * @//TODO : Your code here
		 */

		for (Entry<String, Double> entry : weightedFieldCombinedTfs.entrySet()) {
			double idfVal = this.idfData.idfs.containsKey(entry.getKey()) ? this.idfData.idfs
					.get(entry.getKey()) : this.idfData.termnotfound;
			score += (entry.getValue() * idfVal) / (this.k1 + entry.getValue());
		}

		score += this.pageRankLambda
				* Math.log(this.pageRankLambdaPrime + d.page_rank);

		return score;
	}

	// do bm25 normalization
	public void normalizeTFs(Map<String, Map<String, Double>> tfs, Document d,
			Query q) {
		/*
		 * @//TODO : Your code here
		 */
		Map<String, Double> fieldLengths = lengths.get(d);
		for (Entry<String, Map<String, Double>> entry1 : tfs.entrySet()) {
			double b = 0;
			if (entry1.getKey().equals(TFTYPES[urlIndex])) {
				b = this.burl;
			} else if (entry1.getKey().equals(TFTYPES[titleIndex])) {
				b = this.btitle;
			} else if (entry1.getKey().equals(TFTYPES[headerIndex])) {
				b = this.bheader;
			} else if (entry1.getKey().equals(TFTYPES[anchorIndex])) {
				b = this.banchor;
			} else if (entry1.getKey().equals(TFTYPES[bodyIndex])) {
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

	public void GetWeightedFieldCombinedTfs(Map<String, Map<String, Double>> tfs) {
		weightedFieldCombinedTfs = new HashMap<String, Double>();
		for (Entry<String, Map<String, Double>> entry1 : tfs.entrySet()) {
			double weight = 0;
			if (entry1.getKey().equals(TFTYPES[urlIndex])) {
				weight = this.urlweight;
			} else if (entry1.getKey().equals(TFTYPES[titleIndex])) {
				weight = this.titleweight;
			} else if (entry1.getKey().equals(TFTYPES[headerIndex])) {
				weight = this.headerweight;
			} else if (entry1.getKey().equals(TFTYPES[anchorIndex])) {
				weight = this.anchorweight;
			} else if (entry1.getKey().equals(TFTYPES[bodyIndex])) {
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

	@Override
	public double getSimScore(Document d, Query q) {

		Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

		this.normalizeTFs(tfs, d, q);

		this.GetWeightedFieldCombinedTfs(tfs);

		Map<String, Double> tfQuery = getQueryFreqs(q);

		return getNetScore(d);
	}

}
