package edu.stanford.cs276.util;

public class Config {
	public static double bodyweight = 1;
	public static double anchorweight = 3;
	public static double headerweight = 1;
	public static double titleweight = 1;
	public static double urlweight = 1;
	
	public static double bbody = 0.75;
	public static double banchor = 0.25;
	public static double bheader = 0.75;
	public static double btitle = 1;
	public static double burl = 0.75;
	
	public static double k1 = 10;
	public static double pageRankLambda = 8;
	public static double pageRankLambdaPrime = 9;
	
	public static double B = 1.25;
	
	public static double titleWindowWeight = 3;
	public static double bodyWindowWeight = 1;
	public static double anchorWindowWeight = 4;
	public static double headerWindowWeight = 3;
}
