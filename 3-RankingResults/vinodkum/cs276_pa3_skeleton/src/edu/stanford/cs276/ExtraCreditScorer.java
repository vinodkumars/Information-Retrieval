package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.cs276.util.Config;
import edu.stanford.cs276.util.IDF;
import edu.stanford.cs276.util.Pair;
import edu.stanford.cs276.util.PorterStemmer;

public class ExtraCreditScorer extends SmallestWindowScorer {
	PorterStemmer stemmer = new PorterStemmer();
	StringBuilder sb = new StringBuilder();

	double titleWindowWeight = Config.titleWindowWeight;
	double bodyWindowWeight = Config.bodyWindowWeight;
	double anchorWindowWeight = Config.anchorWindowWeight;
	double headerWindowWeight = Config.headerWindowWeight;

	public ExtraCreditScorer(IDF idfData,
			Map<Query, Map<String, Document>> queryDict) {
		super(idfData, queryDict);
	}

	public String StemString(String input) {
		String[] tokens = TokenizeOnSpaces(input);
		sb.delete(0, sb.length());

		for (int i = 0; i < tokens.length; i++) {
			stemmer.setCurrent(tokens[i]);
			stemmer.stem();
			sb.append(stemmer.getCurrent());
			if (i < tokens.length - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public void StemDocument(Document d) {

		// Title
		if (d.title != null) {
			d.title = StemString(d.title);
		}

		// Headers
		if (d.headers != null) {
			List<String> stemmedHeaders = new ArrayList<String>();
			for (String header : d.headers) {
				stemmedHeaders.add(StemString(header));
			}
			d.headers.clear();
			d.headers.addAll(stemmedHeaders);
			stemmedHeaders.clear();
		}

		// Anchors
		if (d.anchors != null) {
			Map<String, Integer> stemmedAnchors = new HashMap<String, Integer>();
			for (Entry<String, Integer> entry : d.anchors.entrySet()) {
				String stemmedKey = StemString(entry.getKey());
				if (stemmedAnchors.containsKey(stemmedKey)) {
					stemmedAnchors.put(stemmedKey,
							stemmedAnchors.get(stemmedKey).intValue()
									+ entry.getValue().intValue());
				} else {
					stemmedAnchors.put(stemmedKey, entry.getValue().intValue());
				}
			}
			d.anchors.clear();
			d.anchors.putAll(stemmedAnchors);
			stemmedAnchors.clear();
		}

		// Body
		if (d.body_hits != null) {
			Map<String, List<Integer>> stemmedBody = new HashMap<String, List<Integer>>();
			for (Entry<String, List<Integer>> entry : d.body_hits.entrySet()) {
				String stemmedKey = StemString(entry.getKey());
				if (stemmedBody.containsKey(stemmedKey)) {
					stemmedBody.get(stemmedKey).addAll(entry.getValue());
					Collections.sort(stemmedBody.get(stemmedKey));
				} else {
					stemmedBody.put(stemmedKey, entry.getValue());
				}
			}
			d.body_hits.clear();
			d.body_hits.putAll(stemmedBody);
			stemmedBody.clear();
		}

		// Url
		if (d.url != null) {
			String[] urlTokens = TokenizeOnNonAlphanumeric(d.url);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < urlTokens.length; i++) {
				sb.append(StemString(urlTokens[i]));
				if (i == urlTokens.length - 1) {
					sb.append(" ");
				}
			}
			d.url = sb.toString();
		}
	}

	public void StemQuery(Query q) {
		List<String> stemmedWords = new ArrayList<String>();
		for (String s : q.queryWords) {
			stemmedWords.add(StemString(s));
		}
		q.queryWords.clear();
		q.queryWords.addAll(stemmedWords);
	}

	public Pair<Integer, String> GetSmallestWindowSizeAndFieldForDoc(
			Document d, Set<String> queryTerms) {
		Pair<Integer, String> min = new Pair<Integer, String>(
				Integer.MAX_VALUE, null);
		int windowSize;

		// Title
		if (d.title != null) {
			ExtractDataFromString(d.title, queryTerms);
			if (!this.data.isEmpty()) {
				if ((windowSize = GetSmallestWindowSizeForData(data)) < min
						.getFirst().intValue()) {
					min.setFirst(windowSize);
					min.setSecond(TFTYPES[titleIndex]);
				}
			}
		}

		// Headers
		if (d.headers != null) {
			for (String header : d.headers) {
				ExtractDataFromString(header, queryTerms);
				if (!this.data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min
							.getFirst().intValue()) {
						min.setFirst(windowSize);
						min.setSecond(TFTYPES[headerIndex]);
					}
				}
			}
		}

		// Anchors
		if (d.anchors != null) {
			for (String anchor : d.anchors.keySet()) {
				ExtractDataFromString(anchor, queryTerms);
				if (!this.data.isEmpty()) {
					if ((windowSize = GetSmallestWindowSizeForData(data)) < min
							.getFirst().intValue()) {
						min.setFirst(windowSize);
						min.setSecond(TFTYPES[anchorIndex]);
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
			if ((windowSize = GetSmallestWindowSizeForData(data)) < min
					.getFirst().intValue()) {
				min.setFirst(windowSize);
				min.setSecond(TFTYPES[bodyIndex]);
			}
		}
		return min;
	}

	@Override
	public double GetBoostedScore(Map<String, Map<String, Double>> tfs,
			Query q, Map<String, Double> tfQuery, Document d) {
		double baseScore = this.getNetScore(tfs, q, tfQuery, d);
		Pair<Integer, String> window = GetSmallestWindowSizeAndFieldForDoc(d,
				tfQuery.keySet());
		double boost = 1;
		if (window.getFirst().intValue() < Integer.MAX_VALUE) {
			double fieldWeight = 1;
			if (window.getSecond().equals(TFTYPES[titleIndex])) {
				fieldWeight = titleWindowWeight;
			} else if (window.getSecond().equals(TFTYPES[bodyIndex])) {
				fieldWeight = bodyWindowWeight;
			} else if (window.getSecond().equals(TFTYPES[anchorIndex])) {
				fieldWeight = anchorWindowWeight;
			} else if (window.getSecond().equals(TFTYPES[headerIndex])) {
				fieldWeight = headerweight;
			}
			boost += ((fieldWeight * this.B * (double) tfQuery.keySet().size()) / (double) window
					.getFirst().intValue());
		}
		return boost * baseScore;
	}

	@Override
	public double getSimScore(Document dOriginal, Query qOriginal) {

		Document d = new Document(dOriginal);
		Query q = new Query(qOriginal);

		StemDocument(d);
		StemQuery(q);

		Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

		this.SublinearScaleDocTermFreqs(tfs);

		this.normalizeTFs(tfs, d, q);

		Map<String, Double> tfQuery = getQueryFreqs(q);

		this.SublinearScale(tfQuery);

		this.IdfWeight(tfQuery);

		return this.GetBoostedScore(tfs, q, tfQuery, d);
	}

	/*
	 * @//TODO : Your code here
	 */
}
