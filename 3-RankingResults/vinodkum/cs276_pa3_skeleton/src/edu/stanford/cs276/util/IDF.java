package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.Map;

public class IDF implements Serializable {
	public Map<String, Double> idfs = null;
	public double termnotfound;
}
