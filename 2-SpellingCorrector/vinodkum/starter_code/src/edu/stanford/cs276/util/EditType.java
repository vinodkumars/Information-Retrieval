package edu.stanford.cs276.util;

public class EditType {
	public EditTypeEnum type;
	public char x, y;

	public EditType(EditTypeEnum type, char x, char y) {
		this.type = type;
		this.x = x;
		this.y = y;
	}
}
