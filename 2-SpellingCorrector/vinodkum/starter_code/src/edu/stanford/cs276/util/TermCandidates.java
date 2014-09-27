package edu.stanford.cs276.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TermCandidates {
	public String original;
	public String edit0Candidate;
	public List<Pair<String, List<EditType>>> edit1Candidates;
	public List<Pair<String, List<EditType>>> edit2Candidates;
	public List<Pair<String, List<EditType>>> splitCandidates;
	public List<Pair<String, List<EditType>>> joinCandidates;

	public TermCandidates(String original) {
		this.original = original;
		this.edit0Candidate = "";
		this.edit1Candidates = new ArrayList<Pair<String, List<EditType>>>();
		this.edit2Candidates = new ArrayList<Pair<String, List<EditType>>>();
		this.splitCandidates = new ArrayList<Pair<String, List<EditType>>>();
		this.joinCandidates = new ArrayList<Pair<String, List<EditType>>>();
	}
}
