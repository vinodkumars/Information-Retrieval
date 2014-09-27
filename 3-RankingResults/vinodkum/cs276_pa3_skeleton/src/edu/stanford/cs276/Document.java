package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Document {
	public String url = null;
	public String title = null;
	public List<String> headers = null;
	public Map<String, List<Integer>> body_hits = null; // term -> [list of
														// positions]
	public int body_length = 0;
	public int page_rank = 0;
	public Map<String, Integer> anchors = null; // term -> anchor_count

	public Document(String url) {
		this.url = url;
	}

	public Document(Document d) {
		if (d.url != null) {
			this.url = d.url;
		}

		if (d.title != null) {
			this.title = d.title;
		}

		if (d.headers != null) {
			this.headers = new ArrayList<String>();
			this.headers.addAll(d.headers);
		}

		if (d.body_hits != null) {
			this.body_hits = new HashMap<String, List<Integer>>();
			this.body_hits.putAll(d.body_hits);
		}

		this.body_length = d.body_length;
		this.page_rank = d.page_rank;

		if (d.anchors != null) {
			this.anchors = new HashMap<String, Integer>();
			this.anchors.putAll(d.anchors);
		}
	}

	// For debug
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		if (title != null)
			result.append("title: " + title + NEW_LINE);
		if (headers != null)
			result.append("headers: " + headers.toString() + NEW_LINE);
		if (body_hits != null)
			result.append("body_hits: " + body_hits.toString() + NEW_LINE);
		if (body_length != 0)
			result.append("body_length: " + body_length + NEW_LINE);
		if (page_rank != 0)
			result.append("page_rank: " + page_rank + NEW_LINE);
		if (anchors != null)
			result.append("anchors: " + anchors.toString() + NEW_LINE);
		return result.toString();
	}
}
