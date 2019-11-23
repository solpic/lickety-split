package org.licketysplit.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum Field {

	NAME(0, "name"), SIZE(1, "size"),
	LAST_MODIFIED(2, "last modified"), DIRECTORY(
			3, "directory?"), READABLE(4, "readable?"),
	WRITABLE(5, "writable?");

	private Field(int index, String name) {
		this.index = index;
		this.name = name;
	}

	private int index;
	private String name;

	private static final Map<Integer, Field> COLUMN_INDEX_NAME_MAP = new HashMap<>();
	private static final List<String> NAMES = new ArrayList<>();

	static {
		for (Field c : Field.values()) {
			COLUMN_INDEX_NAME_MAP.put(c.index, c);
			NAMES.add(c.name);
		}
	}

	public static Field fromIndex(int colIndex) {
		Field columnName = COLUMN_INDEX_NAME_MAP.get(colIndex);
		return (columnName != null) ? columnName : null;
	}

	public static String[] getNames() {
		return NAMES.toArray(new String[NAMES.size()]);
	}

}
