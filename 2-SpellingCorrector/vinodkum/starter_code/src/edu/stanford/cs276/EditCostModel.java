package edu.stanford.cs276;

import java.io.Serializable;
import java.util.List;

import edu.stanford.cs276.util.EditType;

public interface EditCostModel extends Serializable {

	public double editProbability(String Q, String R, List<EditType> edits);
}
