package cs276.pa4;

import java.io.*;

import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.neuralnet.RankNet;

public class Cheat {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// GridSearch(args);
		PagerankSearch(args);
		// K1Search(args);
	}

	private static void K1Search(String[] args) throws IOException {
		double maxDevNdcg = 0;
		double maxTrainNdcg = 0;
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(
						new File(
								"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\output\\cheat\\cheat.txt")));
		bw.write("k1;trainNdcg;devNdcg\n");
		for (Config.k1 = 1.0; Config.k1 <= 2.0; Config.k1 += 0.1) {
			System.out.println(Config.k1);
			Learning2Rank.main(args);
			NdcgMain ndcg = new NdcgMain(
					"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\data\\pa4.rel.train");
			double trainNdcg = ndcg.score("tmp.train.ranked");

			ndcg = new NdcgMain(
					"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\data\\pa4.rel.dev");
			double devNdcg = ndcg
					.score("E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\output\\outputDev.txt");

			if (maxDevNdcg < devNdcg) {
				maxDevNdcg = devNdcg;
				maxTrainNdcg = trainNdcg;
			}

			bw.write(Config.k1 + ";" + trainNdcg + ";" + +devNdcg + "\n");
		}
		bw.close();
		System.out.println();
		System.out.println("Max dev ndcg: " + maxDevNdcg);
		System.out.println("Max train ndcg: " + maxTrainNdcg);
	}

	private static void PagerankSearch(String[] args) throws IOException {
		double maxDevNdcg = 0;
		double maxTrainNdcg = 0;
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(
						new File(
								"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\output\\cheat\\cheat.txt")));
		bw.write("lambdaPrime;trainNdcg;devNdcg\n");
		for (Config.lambdaPrime = 1.5; Config.lambdaPrime <= 2.5; Config.lambdaPrime += 0.1) {
			System.out.println(Config.lambdaPrime);
			Learning2Rank.main(args);
			NdcgMain ndcg = new NdcgMain(
					"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\data\\pa4.rel.train");
			double trainNdcg = ndcg.score("tmp.train.ranked");

			ndcg = new NdcgMain(
					"E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\data\\pa4.rel.dev");
			double devNdcg = ndcg
					.score("E:\\SCPD\\CS276\\PA\\fresh_attempt\\4\\cs276-pa4\\output\\outputDev.txt");

			if (maxDevNdcg < devNdcg) {
				maxDevNdcg = devNdcg;
				maxTrainNdcg = trainNdcg;
			}

			bw.write(Config.lambdaPrime + ";" + trainNdcg + ";" + +devNdcg
					+ "\n");
		}
		bw.close();
		System.out.println();
		System.out.println("Max dev ndcg: " + maxDevNdcg);
		System.out.println("Max train ndcg: " + maxTrainNdcg);
	}

	private static void GridSearch(String[] args) throws IOException {
		double maxDevNdcg = 0;
		double maxTrainNdcg = 0;
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(
						new File(
								"E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\output\\cheat\\cheat.txt")));
		bw.write("cost;gamma;trainNdcg;devNdcg\n");
		for (int i = -3; i <= 3; i++) {
			System.out.println(i);
			for (int j = -7; j <= -1; j++) {
				System.out.println("\t" + j);
				Config.cost = Math.pow(2, i);
				Config.gamma = Math.pow(2, j);

				Learning2Rank.main(args);

				NdcgMain ndcg = new NdcgMain(
						"E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\data\\pa4.rel.train");
				double trainNdcg = ndcg
						.score("E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\output\\outputTrain.txt");

				ndcg = new NdcgMain(
						"E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\data\\pa4.rel.dev");
				double devNdcg = ndcg
						.score("E:\\SCPD\\CS276\\PA\\4\\cs276_pa4\\cs276_pa4\\output\\outputDev.txt");

				if (maxDevNdcg < devNdcg) {
					maxDevNdcg = devNdcg;
					maxTrainNdcg = trainNdcg;
				}

				bw.write(Config.cost + ";" + Config.gamma + ";" + trainNdcg
						+ ";" + +devNdcg + "\n");
			}
		}
		bw.close();
		System.out.println();
		System.out.println("Max dev ndcg: " + maxDevNdcg);
		System.out.println("Max train ndcg: " + maxTrainNdcg);
	}

}
