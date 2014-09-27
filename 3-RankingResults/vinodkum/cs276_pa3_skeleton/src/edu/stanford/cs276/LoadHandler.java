package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.cs276.util.IDF;

public class LoadHandler {

	public static Map<Query, Map<String, Document>> loadTrainData(
			String feature_file_name) throws Exception {
		File feature_file = new File(feature_file_name);
		if (!feature_file.exists()) {
			System.err.println("Invalid feature file name: "
					+ feature_file_name);
			return null;
		}

		BufferedReader reader = new BufferedReader(new FileReader(feature_file));
		String line = null, url = null, anchor_text = null;
		Query query = null;

		/* feature dictionary: Query -> (url -> Document) */
		Map<Query, Map<String, Document>> queryDict = new HashMap<Query, Map<String, Document>>();

		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(":", 2);
			String key = tokens[0].trim();
			String value = tokens[1].trim();

			if (key.equals("query")) {
				query = new Query(value);
				queryDict.put(query, new HashMap<String, Document>());
			} else if (key.equals("url")) {
				url = value;
				queryDict.get(query).put(url, new Document(url));
			} else if (key.equals("title")) {
				queryDict.get(query).get(url).title = new String(value);
			} else if (key.equals("header")) {
				if (queryDict.get(query).get(url).headers == null)
					queryDict.get(query).get(url).headers = new ArrayList<String>();
				queryDict.get(query).get(url).headers.add(value);
			} else if (key.equals("body_hits")) {
				if (queryDict.get(query).get(url).body_hits == null)
					queryDict.get(query).get(url).body_hits = new HashMap<String, List<Integer>>();
				String[] temp = value.split(" ", 2);
				String term = temp[0].trim();
				List<Integer> positions_int;

				if (!queryDict.get(query).get(url).body_hits.containsKey(term)) {
					positions_int = new ArrayList<Integer>();
					queryDict.get(query).get(url).body_hits.put(term,
							positions_int);
				} else
					positions_int = queryDict.get(query).get(url).body_hits
							.get(term);

				String[] positions = temp[1].trim().split(" ");
				for (String position : positions)
					positions_int.add(Integer.parseInt(position));

			} else if (key.equals("body_length"))
				queryDict.get(query).get(url).body_length = Integer
						.parseInt(value);
			else if (key.equals("pagerank"))
				queryDict.get(query).get(url).page_rank = Integer
						.parseInt(value);
			else if (key.equals("anchor_text")) {
				anchor_text = value;
				if (queryDict.get(query).get(url).anchors == null)
					queryDict.get(query).get(url).anchors = new HashMap<String, Integer>();
			} else if (key.equals("stanford_anchor_count"))
				queryDict.get(query).get(url).anchors.put(anchor_text,
						Integer.parseInt(value));
		}

		reader.close();

		return queryDict;
	}

	// unserializes from file
	public static IDF loadDFs(String idfFile) {
		IDF idfData = null;
		try {
			FileInputStream fis = new FileInputStream(idfFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			idfData = (IDF) ois.readObject();
			ois.close();
			fis.close();
		} catch (IOException | ClassNotFoundException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return idfData;
	}

	// builds and then serializes from file
	public static IDF buildDFs(String dataDir, String idfFile)
			throws IOException {

		/* Get root directory */
		String root = dataDir;
		File rootdir = new File(root);
		// System.out.println(rootdir.getAbsolutePath());
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return null;
		}

		File[] dirlist = rootdir.listFiles();

		int totalDocCount = 0;

		// counts number of documents in which each term appears
		IDF idfData = new IDF();
		idfData.idfs = new HashMap<String, Double>();

		/*
		 * @//TODO : Your code here --consult pa1 (will be basically a
		 * simplified version)
		 */
		for (File dir : dirlist) {
			// System.out.println(dir.getAbsolutePath());
			for (File doc : dir.listFiles()) {
				++totalDocCount;
				HashSet<String> set = new HashSet<String>();
				BufferedReader reader = new BufferedReader(new FileReader(doc));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						if (!set.contains(token)) {
							set.add(token);
						}
					}
				}
				for (String token : set) {
					if (!idfData.idfs.containsKey(token)) {
						idfData.idfs.put(token, (double) 1);
					} else {
						idfData.idfs.put(token, idfData.idfs.get(token) + 1);
					}
				}
				set.clear();
				reader.close();
			}
		}

		System.out.println(totalDocCount);

		// make idf
		for (String term : idfData.idfs.keySet()) {
			/*
			 * @//TODO : Your code here
			 */
			double idf = Math.log10((double) totalDocCount + 1
					/ idfData.idfs.get(term) + 1);
			idfData.idfs.put(term, idf);
		}
		idfData.termnotfound = Math.log10((double) totalDocCount + 1 / 1);

		// saves to file
		try {
			FileOutputStream fos = new FileOutputStream(idfFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(idfData);
			oos.close();
			fos.close();
		}

		catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return idfData;
	}

}
