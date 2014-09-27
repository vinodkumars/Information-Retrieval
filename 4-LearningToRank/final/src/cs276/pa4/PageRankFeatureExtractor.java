package cs276.pa4;

public class PageRankFeatureExtractor {

	private static double lambdaPrime = 2.1;
	// private static double lambdaPrime = Config.lambdaPrime;

	public static double GetPageRank(Document d) {
		return Math.log(lambdaPrime + (double) d.page_rank);
	}
}
