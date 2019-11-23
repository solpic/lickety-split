package org.licketysplit.gui;

import java.io.File;
import java.util.Date;
import javax.swing.table.AbstractTableModel;

public class TableModel extends AbstractTableModel {

	private File dir;
	private File applicationData;
	private String[] filenames;
	private String[] columnNames = Field.getNames();
	private Class<?>[] columnClasses = Props.COLUMN_CLASSES;

	// This table model works for any one given directory
	public TableModel(File dir, File applicationData) {
		this.dir = dir;
		this.applicationData = applicationData;
		// Store a list of files in the directory
		this.filenames = dir.list();
	}
    public TableModel(File dir) {
        this.dir = dir;
        // Store a list of files in the directory
        this.filenames = dir.list();
    }

	// Returns a constant columns number for this model
	public int getColumnCount() {
		return Props.COLUMN_CLASSES.length;
	}

	// Returns the number of files in directory
	public int getRowCount() {
		return filenames.length;
	}

	// Returns the name of the given column index
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Class<?> getColumnClass(int col) {
		return columnClasses[col];
	}

	// Returns the value of each cell
	public Object getValueAt(int row, int col) {
		File f = new File(dir, filenames[row]);
		Field field = Field.fromIndex(col);
		switch (field) {
		case NAME:
			return filenames[row];
		case SIZE:
			return f.length();
		case LAST_MODIFIED:
			return new Date(f.lastModified());
		case DIRECTORY:
			return f.isDirectory() ? Boolean.TRUE : Boolean.FALSE;
		case READABLE:
			return f.canRead() ? Boolean.TRUE : Boolean.FALSE;
		case WRITABLE:
			return f.canWrite() ? Boolean.TRUE : Boolean.FALSE;
		default:
			return null;
		}
	}

}
