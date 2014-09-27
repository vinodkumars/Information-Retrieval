package cs276.assignments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	private static boolean isDebug = false;

	static class TermFreqComparator implements Comparator<String> {
		@Override
		public int compare(String arg0, String arg1) {
			return freqDict.get(termDict.get(arg0).intValue()).intValue()
					- freqDict.get(termDict.get(arg1).intValue()).intValue();
		}
	}

	/*
	 * Write a posting list with a given termID from the file You should seek to
	 * the file position of this specific posting list and read it back.
	 */
	private static PostingList readPosting(FileChannel fc, int termId)
			throws IOException {
		/*
		 * Your code here
		 */
		fc.position(posDict.get(termId).longValue());
		PostingList p = index.readPosting(fc);
		return p;
	}

	public static Integer PopNextDocIdOrNull(Iterator<Integer> itr) {
		if (itr.hasNext()) {
			return itr.next();
		} else {
			return null;
		}
	}

	public static List<Integer> intersect(List<Integer> l1, List<Integer> l2) {
		List<Integer> result = new ArrayList<Integer>();
		Iterator<Integer> itr1 = l1.iterator();
		Iterator<Integer> itr2 = l2.iterator();
		Integer d1 = PopNextDocIdOrNull(itr1);
		Integer d2 = PopNextDocIdOrNull(itr2);

		while (d1 != null && d2 != null) {
			if (d1.intValue() == d2.intValue()) {
				result.add(d1.intValue());
				d1 = PopNextDocIdOrNull(itr1);
				d2 = PopNextDocIdOrNull(itr2);
			} else if (d1.intValue() < d2.intValue()) {
				d1 = PopNextDocIdOrNull(itr1);
			} else {
				d2 = PopNextDocIdOrNull(itr2);
			}
		}
		return result;
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		/* For each query */
		while ((line = br.readLine()) != null) {
			/*
			 * Your code here
			 */

			// Removing Duplicate terms
			HashSet<String> hs = new HashSet<String>();
			List<Integer> result = null;
			List<String> inputTerms = new ArrayList<String>();
			boolean invalidTermFound = false;
			String resultFileName = "";

			hs.addAll(Arrays.asList(line.trim().split("\\s+")));
			inputTerms.addAll(hs);

			// Checking for invalid terms
			for (String term : inputTerms) {
				if (!termDict.containsKey(term)) {
					System.out.println("no results found");
					invalidTermFound = true;
					break;
				}
			}
			if (invalidTermFound) {
				continue;
			}

			// Sorting by frequency
			Collections.sort(inputTerms, new TermFreqComparator());

			for (String term : inputTerms) {

				PostingList p = readPosting(indexFile.getChannel(),
						termDict.get(term));
				if (p == null) {
					System.out.println("no results found");
					break;
				}

				if (result == null) {
					result = p.getList();
				} else {
					result = intersect(result, p.getList());
				}

				if (isDebug) {
					BufferedWriter bw = new BufferedWriter(
							new FileWriter(
									new File(
											"E:\\SCPD\\CS276\\PA\\1Version2\\cs276-pa1\\task1\\output\\debug\\queryOut",
											term + ".txt")));
					bw.write("TermId: " + p.getTermId() + " ");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.close();
				}
				if (isDebug) {
					resultFileName += term + "1";
					BufferedWriter bw = new BufferedWriter(
							new FileWriter(
									new File(
											"E:\\SCPD\\CS276\\PA\\1Version2\\cs276-pa1\\task1\\output\\debug\\queryOut",
											resultFileName + ".txt")));
					for (Integer d : result) {
						bw.write(d.intValue() + " ");
					}
					bw.close();
				}
			}

			List<String> output = new LinkedList<String>();
			if (result.isEmpty() || result == null) {
				System.out.println("no results found");
			} else {
				for (Integer d : result) {
					output.add(docDict.get(d));
				}
				Collections.sort(output);
				for (String s : output) {
					System.out.println(s);
				}

			}

			if (isDebug) {
				BufferedWriter bw = new BufferedWriter(
						new FileWriter(
								new File(
										"E:\\SCPD\\CS276\\PA\\1Version2\\cs276-pa1\\task2\\output\\debug\\queryOut",
										"result.txt")));
				if (result.isEmpty() || result == null) {
					bw.write("no results found");
				} else {
					for (String s : output) {
						bw.write(s + "\n");
					}
				}
				bw.close();
			}

		}
		br.close();
		indexFile.close();
	}
}
