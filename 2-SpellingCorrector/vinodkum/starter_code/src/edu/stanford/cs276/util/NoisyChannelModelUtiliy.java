package edu.stanford.cs276.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

enum Direction {
	Right, Down, Diag, None
}

class EditDistUnit {
	int editDist;
	Direction direction;
	char x, y;
	boolean isTranspose;

	public EditDistUnit(char x, char y, int editDist, Direction direction,
			boolean isTranspose) {
		this.x = x;
		this.y = y;
		this.editDist = editDist;
		this.direction = direction;
		this.isTranspose = isTranspose;
	}

	public EditDistUnit(char x, char y, int editDist, Direction direction) {
		this.x = x;
		this.y = y;
		this.editDist = editDist;
		this.direction = direction;
		this.isTranspose = false;
	}

	public EditDistUnit() {
	}
}

public class NoisyChannelModelUtiliy {

	public static EditDistUnit Minimum(EditDistUnit a, EditDistUnit b,
			EditDistUnit c) {
		if (a.editDist <= b.editDist) {
			if (a.editDist <= c.editDist) {
				return new EditDistUnit(' ', ' ', a.editDist + 1,
						Direction.Diag);
			} else {
				return new EditDistUnit(' ', ' ', c.editDist + 1,
						Direction.Right);
			}
		} else if (b.editDist < c.editDist) {
			return new EditDistUnit(' ', ' ', b.editDist + 1, Direction.Down);
		} else {
			return new EditDistUnit(' ', ' ', c.editDist + 1, Direction.Right);
		}
	}

	public static List<EditType> FindEdits(String correct, String wrong) {
		EditDistUnit[][] arr = new EditDistUnit[correct.length() + 1][wrong
				.length() + 1];
		List<EditType> retVal = new ArrayList<EditType>();
		// List<EditDistUnit> editDistUnitsChanged = new
		// LinkedList<EditDistUnit>();

		for (int i = 0; i < correct.length() + 1; i++) {
			char x = i == 0 ? ' ' : correct.charAt(i - 1);
			Direction dir = i == 0 ? Direction.None : Direction.Down;
			arr[i][0] = new EditDistUnit(x, ' ', i, dir);
		}
		for (int j = 1; j < wrong.length() + 1; j++) {
			arr[0][j] = new EditDistUnit(' ', wrong.charAt(j - 1), j,
					Direction.Right);
		}

		for (int i = 1; i < correct.length() + 1; i++) {
			for (int j = 1; j < wrong.length() + 1; j++) {
				if (correct.charAt(i - 1) == wrong.charAt(j - 1)) {
					// no operation required
					arr[i][j] = new EditDistUnit(correct.charAt(i - 1),
							wrong.charAt(j - 1), arr[i - 1][j - 1].editDist,
							Direction.Diag);
				} else if ((i > 1 && j > 1)
						&& (correct.charAt(i - 1) == wrong.charAt(j - 2))
						&& (correct.charAt(i - 2) == wrong.charAt(j - 1))) {
					// transposition - cost already added in previous step
					arr[i][j] = new EditDistUnit(correct.charAt(i - 1),
							wrong.charAt(j - 1), arr[i - 1][j - 1].editDist,
							Direction.Diag, true);
				} else {
					// minimum of insertion, deletion and substitution
					arr[i][j] = Minimum(arr[i - 1][j - 1], arr[i - 1][j],
							arr[i][j - 1]);
					arr[i][j].x = correct.charAt(i - 1);
					arr[i][j].y = wrong.charAt(j - 1);

				}
			}
		}

		int i = correct.length();
		int j = wrong.length();

		if (arr[i][j].editDist > 2) {
			return null;
		}

		// Note to self: Have scope of optimizing. May eliminate the foreach
		// loop

		boolean isTranspose = false;
		while (arr[i][j].direction != Direction.None) {
			switch (arr[i][j].direction) {
			case Diag:
				if (arr[i][j].isTranspose) {
					isTranspose = true;
					// retVal.add(new EditType(EditTypeEnum.Trans, arr[i][j].y,
					// arr[i][j].x));
				} else if (arr[i - 1][j - 1].editDist != arr[i][j].editDist) {
					// editDistUnitsChanged.add(arr[i][j]);
					if (isTranspose) {
						retVal.add(new EditType(EditTypeEnum.Trans,
								arr[i][j].x, arr[i][j].y));
						isTranspose = false;
					} else {
						retVal.add(new EditType(EditTypeEnum.Sub, arr[i][j].x,
								arr[i][j].y));
					}
				}
				i--;
				j--;
				break;
			case Right:
				if (arr[i][j - 1].editDist != arr[i][j].editDist) {
					// editDistUnitsChanged.add(arr[i][j]);
					retVal.add(new EditType(EditTypeEnum.Ins, arr[i][j].x,
							arr[i][j].y));
				}
				j--;
				break;
			case Down:
				if (arr[i - 1][j].editDist != arr[i][j].editDist) {
					// editDistUnitsChanged.add(arr[i][j]);
					retVal.add(new EditType(EditTypeEnum.Del, arr[i][j].y,
							arr[i][j].x));
				}
				i--;
				break;
			default:
				break;
			}
		}

		return retVal;
	}

}
