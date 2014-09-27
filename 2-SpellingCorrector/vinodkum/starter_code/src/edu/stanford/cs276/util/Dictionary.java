package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;

public class Dictionary implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int termCount;
	private HashMap<String, Integer> map;

	public int termCount() {
		return termCount;
	}

	public Dictionary() {
		termCount = 0;
		map = new HashMap<String, Integer>();
	}
	
	public Set<java.util.Map.Entry<String,Integer>> GetAllEntries()
	{
		return map.entrySet();
	}
	
	public Set<String> GetTermList(){
		return map.keySet();
	}
	
	public boolean isValidTerm(String term){
		return map.containsKey(term);
	}

	public void add(String term) {

		termCount++;
		if (map.containsKey(term)) {
			map.put(term, map.get(term) + 1);
		} else {
			map.put(term, 1);
		}
	}

	public int count(String term) {

		if (map.containsKey(term)) {
			return map.get(term);
		} else {
			return 0;
		}
	}
}
