package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.stanford.cs276.util.Config;
import edu.stanford.cs276.util.IDF;

public class CosineSimilarityScorer extends AScorer {
	public CosineSimilarityScorer(IDF idfData) {
		super(idfData);
	}

	// /////////////weights///////////////////////////
	double urlweight = Config.urlweight;
	double titleweight = Config.titleweight;
	double bodyweight = Config.bodyweight;
	double headerweight = Config.headerweight;
	double anchorweight = Config.anchorweight;

	double smoothingBodyLength = 500;

	// ////////////////////////////////////////

	public double getNetScore(Map<String, Map<String, Double>> tfs, Query q,
			Map<String, Double> tfQuery, Document d) {
		double score = 0.0;

		/*
		 * @//TODO : Your code here
		 */
		Map<String, Double> weightedTfDoc = GetWeightedTfDoc(tfs);
		for (Entry<String, Double> entry : weightedTfDoc.entrySet()) {
			score += entry.getValue() * tfQuery.get(entry.getKey());
		}

		return score;
	}

	public Map<String, Double> GetWeightedTfDoc(
			Map<String, Map<String, Double>> tfs) {
		Map<String, Double> retVal = new HashMap<String, Double>();
		for (Entry<String, Double> entry : tfs.get(TFTYPES[urlIndex])
				.entrySet()) {
			retVal.put(entry.getKey(), entry.getValue() * urlweight);
		}
		for (Entry<String, Double> entry : tfs.get(TFTYPES[titleIndex])
				.entrySet()) {
			retVal.put(entry.getKey(),
					retVal.get(entry.getKey()) + entry.getValue() * titleweight);
		}
		for (Entry<String, Double> entry : tfs.get(TFTYPES[bodyIndex])
				.entrySet()) {
			retVal.put(entry.getKey(),
					retVal.get(entry.getKey()) + entry.getValue() * bodyweight);
		}
		for (Entry<String, Double> entry : tfs.get(TFTYPES[headerIndex])
				.entrySet()) {
			retVal.put(entry.getKey(),
					retVal.get(entry.getKey()) + entry.getValue()
							* headerweight);
		}
		for (Entry<String, Double> entry : tfs.get(TFTYPES[anchorIndex])
				.entrySet()) {
			retVal.put(entry.getKey(),
					retVal.get(entry.getKey()) + entry.getValue()
							* anchorweight);
		}
		return retVal;
	}

	public void SublinearScale(Map<String, Double> tfQuery) {
		for (Entry<String, Double> mapEntry : tfQuery.entrySet()) {
			if (mapEntry.getValue() != 0) {
				mapEntry.setValue((Math.log(mapEntry.getValue()) + 1));
			}
		}
	}

	public void SublinearScaleDocTermFreqs(Map<String, Map<String, Double>> tfs) {
		for (Map<String, Double> fieldMap : tfs.values()) {
			this.SublinearScale(fieldMap);
		}
	}

	public void IdfWeight(Map<String, Double> tfQuery) {
		for (Entry<String, Double> entry : tfQuery.entrySet()) {
			double idfVal = this.idfData.idfs.containsKey(entry.getKey()) ? this.idfData.idfs
					.get(entry.getKey()) : this.idfData.termnotfound;
			entry.setValue(entry.getValue() * idfVal);
		}
	}

	public void normalizeTFs(Map<String, Map<String, Double>> tfs, Document d,
			Query q) {
		/*
		 * @//TODO : Your code here
		 */
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

	@Override
	public double getSimScore(Document d, Query q) {

		Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

		this.SublinearScaleDocTermFreqs(tfs);

		this.normalizeTFs(tfs, d, q);

		Map<String, Double> tfQuery = getQueryFreqs(q);

		this.SublinearScale(tfQuery);

		this.IdfWeight(tfQuery);

		return getNetScore(tfs, q, tfQuery, d);
	}

}
