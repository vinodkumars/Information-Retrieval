package cs276.pa4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RelPrint {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Map<String, Map<String, Double>> relData = null;
		String s1 = "E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\data\\pa4.rel.dev";
		try {
			relData = Util.loadRelData(s1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<Pair<String, List<Pair<String, Double>>>> relDataCollection = new ArrayList<Pair<String, List<Pair<String, Double>>>>();
		for (Entry<String, Map<String, Double>> entry1 : relData.entrySet()) {

			List<Pair<String, Double>> docCollection = new ArrayList<Pair<String, Double>>();
			for (Entry<String, Double> entry2 : entry1.getValue().entrySet()) {
				Pair<String, Double> p = new Pair<String, Double>(
						entry2.getKey(), entry2.getValue());
				docCollection.add(p);
			}
			Collections.sort(docCollection,
					new Comparator<Pair<String, Double>>() {
						@Override
						public int compare(Pair<String, Double> arg0,
								Pair<String, Double> arg1) {
							// TODO Auto-generated method stub
							return arg0.getSecond().doubleValue() >= arg1
									.getSecond().doubleValue() ? -1 : 1;
						}
					});

			Pair<String, List<Pair<String, Double>>> p = new Pair<String, List<Pair<String, Double>>>(
					entry1.getKey(), docCollection);
			relDataCollection.add(p);
		}

		Collections.sort(relDataCollection,
				new Comparator<Pair<String, List<Pair<String, Double>>>>() {
					@Override
					public int compare(
							Pair<String, List<Pair<String, Double>>> arg0,
							Pair<String, List<Pair<String, Double>>> arg1) {
						// TODO Auto-generated method stub
						return arg0.getFirst().compareToIgnoreCase(
								arg1.getFirst());
					}
				});

		BufferedWriter bw = new BufferedWriter(
				new FileWriter(
						new File(
								"E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\output\\sortedRel\\sortedtRel.txt")));
		for (Pair<String, List<Pair<String, Double>>> pair : relDataCollection) {
			bw.write("query: " + pair.getFirst() + "\n");
			for (Pair<String, Double> doc : pair.getSecond()) {
				bw.write("  url: " + doc.getFirst() + "   " + doc.getSecond()
						+ "\n");
			}
		}
		bw.close();
		System.err.println("Done!");
	}
}
