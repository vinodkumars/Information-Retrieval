package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Query {

	List<String> queryWords = null;

	public Query(String query) {
		queryWords = new ArrayList<String>();
		for (String s : query.trim().split("\\s+")) {
			queryWords.add(s.toLowerCase());
		}
	}

	public Query(Query q) {
		if (q.queryWords != null) {
			this.queryWords = new ArrayList<String>();
			this.queryWords.addAll(q.queryWords);
		}
	}

}
