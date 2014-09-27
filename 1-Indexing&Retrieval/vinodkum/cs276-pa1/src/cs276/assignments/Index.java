package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.LinkedList;

class TermIdDocIdPairComparator implements Comparator<Pair<Integer, Integer>> {

	@Override
	public int compare(Pair<Integer, Integer> arg0, Pair<Integer, Integer> arg1) {
		return arg0.getFirst().intValue() - arg1.getFirst().intValue();
	}

}

public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict = new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue = new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	private static boolean isDebug = false;
	// This is used for debugging purposes
	private static int termIdToFind = 111;

	private static int chunkSize = 30000;

	/*
	 * Write a posting list to the file You should record the file position of
	 * this posting list so that you can read it back during retrieval
	 */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws IOException {
		/*
		 * Your code here
		 */
	}

	private static List<PostingList> ReadChunk(FileChannel fc) {
		List<PostingList> retVal = null;
		PostingList p = null;
		int i = 0;
		while ((i < chunkSize) && ((p = index.readPosting(fc)) != null)) {
			if (retVal == null) {
				retVal = new LinkedList<PostingList>();
			}
			retVal.add(p);
			i++;
		}
		return retVal;
	}

	private static List<PostingList> ReadChunk(FileChannel fc, int termId)
			throws IOException {
		List<PostingList> retVal = null;
		PostingList p = null;
		long lastPos = 0;
		while ((p = index.readPosting(fc)) != null) {
			if (retVal == null) {
				retVal = new LinkedList<PostingList>();
			}
			if (p.getTermId() <= termId) {
				retVal.add(p);
			} else {
				fc.position(lastPos);
				break;
			}
			lastPos = fc.position();
		}
		return retVal;
	}

	private static void writeChunk(FileChannel fc, List<PostingList> chunk,
			boolean isRecordPos) throws IOException {
		for (PostingList p : chunk) {
			if (isRecordPos) {
				if (!postingDict.containsKey(p.getTermId())) {
					Pair<Long, Integer> pair = new Pair<Long, Integer>(
							fc.position(), p.getList().size());
					postingDict.put(p.getTermId(), pair);
				}
			}
			index.writePosting(fc, p);
		}
	}

	private static List<Integer> MergeSortedLists(List<Integer> l1,
			List<Integer> l2) {
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
				result.add(d1.intValue());
				d1 = PopNextDocIdOrNull(itr1);
			} else {
				result.add(d2.intValue());
				d2 = PopNextDocIdOrNull(itr2);
			}
		}
		while (d1 != null) {
			result.add(d1.intValue());
			d1 = PopNextDocIdOrNull(itr1);
		}
		while (d2 != null) {
			result.add(d2.intValue());
			d2 = PopNextDocIdOrNull(itr2);
		}
		return result;
	}

	public static PostingList PopNextPostingListOrNull(Iterator<PostingList> itr) {
		if (itr.hasNext()) {
			return itr.next();
		} else {
			return null;
		}
	}

	public static Integer PopNextDocIdOrNull(Iterator<Integer> itr) {
		if (itr.hasNext()) {
			return itr.next();
		} else {
			return null;
		}
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
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

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();

		/* TermId DocId pairs */
		List<Pair<Integer, Integer>> termIdDocIdPairs = new LinkedList<Pair<Integer, Integer>>();
		/* TermId PostingsList Map */
		Map<Integer, PostingList> termIdPostingsListMap = new TreeMap<Integer, PostingList>();

		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);
			termIdDocIdPairs.clear();
			termIdPostingsListMap.clear();

			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();

			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				++docIdCounter;
				docDict.put(fileName, docIdCounter);

				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						/*
						 * Your code here
						 */
						if (!termDict.containsKey(token)) {
							termDict.put(token, ++wordIdCounter);
						}
						termIdDocIdPairs.add(new Pair<Integer, Integer>(
								termDict.get(token).intValue(), docIdCounter));
					}
				}
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}

			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");

			/*
			 * Your code here
			 */

			// Sorting the TermId DocId pairs
			Collections.sort(termIdDocIdPairs, new TermIdDocIdPairComparator());

			// Creating postings list
			int prevTermId = -1;
			int prevDocId = -1;
			for (Pair<Integer, Integer> tdp : termIdDocIdPairs) {
				if ((tdp.getFirst().intValue() == prevTermId)
						&& (tdp.getSecond().intValue() == prevDocId)) {
					continue;
				} else if (termIdPostingsListMap.containsKey(tdp.getFirst()
						.intValue())) {
					termIdPostingsListMap.get(tdp.getFirst().intValue())
							.getList().add(tdp.getSecond().intValue());
					prevTermId = tdp.getFirst().intValue();
					prevDocId = tdp.getSecond().intValue();
				} else {
					PostingList p = new PostingList(tdp.getFirst().intValue());
					p.getList().add(tdp.getSecond().intValue());
					termIdPostingsListMap.put(tdp.getFirst().intValue(), p);
					prevTermId = tdp.getFirst().intValue();
					prevDocId = tdp.getSecond().intValue();
				}
			}

			if (isDebug) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug", block.getName() + ".txt")));
				for (PostingList p : termIdPostingsListMap.values()) {
					bw.write("TermId: " + p.getTermId() + "\t");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.write("\n");
				}
				bw.close();
			}

			if (isDebug) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug", termIdToFind + ".txt"), true));
				for (PostingList p : termIdPostingsListMap.values()) {
					if (p.getTermId() == termIdToFind) {
						for (Integer d : p.getList()) {
							String docPath = "";
							for (Entry<String, Integer> entry : docDict
									.entrySet()) {
								if (entry.getValue().intValue() == d.intValue()) {
									docPath = entry.getKey();
								}
							}
							bw.append(docPath + "\n");
						}
					}
				}
				bw.close();
			}

			/* Indexing the block */
			for (PostingList p : termIdPostingsListMap.values()) {
				index.writePosting(bfc.getChannel(), p);
			}
			bfc.close();

			if (isDebug) {
				RandomAccessFile raf = new RandomAccessFile(blockFile, "r");
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug\\r", block.getName() + ".txt")));
				PostingList p = null;
				while ((p = index.readPosting(raf.getChannel())) != null) {
					bw.write("TermId: " + p.getTermId() + "\t");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.write("\n");
				}
				bw.close();
				raf.close();
			}
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1) {
				break;
			}
			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			boolean isRecordPos = blockQueue.size() == 0 ? true : false;

			File combfile = new File(output, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

			/*
			 * Your code here
			 */
			int i = 0;
			List<PostingList> chunk1 = null, chunk2 = null, mergeChunk = null;

			/* Reading chunk1 */
			chunk1 = ReadChunk(bf1.getChannel());
			if (isDebug && chunk1 != null) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug\\readChunk", "chunk_" + b1.getName()
								+ "_" + i + ".txt")));
				for (PostingList p : chunk1) {
					bw.write("TermId: " + p.getTermId() + "\t");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.write("\n");
				}
				bw.close();
			}

			/* Reading chunk2 */
			if (chunk1 != null) {
				chunk2 = ReadChunk(bf2.getChannel(),
						chunk1.get(chunk1.size() - 1).getTermId());
			} else {
				chunk2 = ReadChunk(bf2.getChannel());
			}
			if (isDebug && chunk2 != null) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug\\readChunk", "chunk_" + b2.getName()
								+ "_" + i + ".txt")));
				for (PostingList p : chunk2) {
					bw.write("TermId: " + p.getTermId() + "\t");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.write("\n");
				}
				bw.close();
			}

			/* Merging chunk1 & chunk2 */
			mergeChunk = new LinkedList<PostingList>();
			while (chunk1 != null && chunk2 != null) {
				Iterator<PostingList> itr1 = chunk1.iterator();
				Iterator<PostingList> itr2 = chunk2.iterator();
				PostingList p1 = itr1.next();
				PostingList p2 = itr2.next();
				mergeChunk.clear();

				while (p1 != null && p2 != null) {
					if (p1.getTermId() == p2.getTermId()) {
						PostingList p = new PostingList(p1.getTermId(),
								MergeSortedLists(p1.getList(), p2.getList()));
						mergeChunk.add(p);
						p1 = PopNextPostingListOrNull(itr1);
						p2 = PopNextPostingListOrNull(itr2);
					} else if (p1.getTermId() < p2.getTermId()) {
						mergeChunk.add(p1);
						p1 = PopNextPostingListOrNull(itr1);
					} else {
						mergeChunk.add(p2);
						p2 = PopNextPostingListOrNull(itr2);
					}
				}
				while (p1 != null) {
					mergeChunk.add(p1);
					p1 = PopNextPostingListOrNull(itr1);
				}
				while (p2 != null) {
					mergeChunk.add(p2);
					p2 = PopNextPostingListOrNull(itr2);
				}

				/* Writing merged chunk to disk */
				writeChunk(mf.getChannel(), mergeChunk, isRecordPos);
				if (isDebug && mergeChunk != null) {
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							new File(output + "\\Debug\\writeChunk", "chunk_"
									+ combfile.getName() + "_" + i + ".txt")));
					for (PostingList p : mergeChunk) {
						bw.write("TermId: " + p.getTermId() + "\t");
						for (Integer d : p.getList()) {
							bw.write(d.intValue() + " ");
						}
						bw.write("\n");
					}
					bw.close();
				}

				// For debug - file name
				i++;

				/* Reading next chunk1 */
				chunk1.clear();
				chunk1 = ReadChunk(bf1.getChannel());
				if (isDebug && chunk1 != null) {
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							new File(output + "\\Debug\\readChunk", "chunk_"
									+ b1.getName() + "_" + i + ".txt")));
					for (PostingList p : chunk1) {
						bw.write("TermId: " + p.getTermId() + "\t");
						for (Integer d : p.getList()) {
							bw.write(d.intValue() + " ");
						}
						bw.write("\n");
					}
					bw.close();
				}

				/* Reading next chunk2 */
				chunk2.clear();
				if (chunk1 != null) {
					chunk2 = ReadChunk(bf2.getChannel(),
							chunk1.get(chunk1.size() - 1).getTermId());
				} else {
					chunk2 = ReadChunk(bf2.getChannel());
				}
				if (isDebug && chunk2 != null) {
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							new File(output + "\\Debug\\readChunk", "chunk"
									+ b2.getName() + "_" + i + ".txt")));
					for (PostingList p : chunk2) {
						bw.write("TermId: " + p.getTermId() + "\t");
						for (Integer d : p.getList()) {
							bw.write(d.intValue() + " ");
						}
						bw.write("\n");
					}
					bw.close();
				}
			}
			while (chunk1 != null) {
				writeChunk(mf.getChannel(), chunk1, isRecordPos);
				chunk1.clear();
				chunk1 = ReadChunk(bf1.getChannel());
			}
			while (chunk2 != null) {
				writeChunk(mf.getChannel(), chunk2, isRecordPos);
				chunk2.clear();
				chunk2 = ReadChunk(bf2.getChannel());
			}

			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			blockQueue.add(combfile);

			if (isDebug) {
				RandomAccessFile raf = new RandomAccessFile(combfile, "r");
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						output + "\\Debug\\r", combfile.getName() + ".txt")));
				PostingList p = null;
				while ((p = index.readPosting(raf.getChannel())) != null) {
					bw.write("TermId: " + p.getTermId() + "\t");
					for (Integer d : p.getList()) {
						bw.write(d.intValue() + " ");
					}
					bw.write("\n");
				}
				bw.close();
				raf.close();
			}
		}

		File indexFile = blockQueue.removeFirst();
		if (isDebug) {
			RandomAccessFile raf = new RandomAccessFile(indexFile, "r");
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					output + "\\Debug\\r", termIdToFind + ".txt")));
			PostingList p = null;
			while ((p = index.readPosting(raf.getChannel())) != null) {
				if (p.getTermId() == termIdToFind) {
					for (Integer d : p.getList()) {
						String docPath = "";
						for (Entry<String, Integer> entry : docDict.entrySet()) {
							if (entry.getValue().intValue() == d.intValue()) {
								docPath = entry.getKey();
							}
						}
						bw.write(docPath + "\n");
					}
				}
			}
			bw.close();
			raf.close();
		}

		/* Dump constructed index back into file system */
		// File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
