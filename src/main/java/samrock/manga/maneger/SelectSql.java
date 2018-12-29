package samrock.manga.maneger;

import sam.myutils.Checker;
import sam.sql.JDBCHelper;

public class SelectSql {
	private final StringBuilder sb;
	private final int len;
	
	public SelectSql(String tableName, String whereColumn, String[] columnNames) {
		sb = Checker.isEmpty(columnNames) ? new StringBuilder("SELECT * FROM ").append(tableName) : JDBCHelper.selectSQL(tableName, columnNames);
		sb.append(" WHERE ").append(whereColumn).append('=');
		
		this.len = sb.length();
	}
	
	public String create(int value) {
		sb.append(value).append(';');
		String s = sb.toString();
		sb.setLength(len);
		
		return s;
	}
	
	public String create(int[] values) {
		if(Checker.isEmpty(values))
			throw new IllegalArgumentException();
		
		for (int i : values) 
			sb.append(i).append(',');
		
		sb.setCharAt(sb.length() - 1, ';');
		String s = sb.toString();
		sb.setLength(len);
		
		return s;
	}
}
