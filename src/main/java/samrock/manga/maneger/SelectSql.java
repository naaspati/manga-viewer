package samrock.manga.maneger;

import sam.myutils.Checker;
import sam.sql.JDBCHelper;

public class SelectSql {
	private final StringBuilder sb;
	private final int len;
	private final int selectAll;
	
	public SelectSql(String tableName, String whereColumn, String[] columnNames) {
		sb = Checker.isEmpty(columnNames) ? new StringBuilder("SELECT * FROM ").append(tableName) : JDBCHelper.selectSQL(tableName, columnNames);
		this.selectAll = sb.length(); 
		sb.append(" WHERE ").append(whereColumn);
		
		this.len = sb.length();
	}
	
	public String selectAll() {
		sb.setCharAt(selectAll, ';');
		String s = sb.substring(0, selectAll+1);
		sb.setCharAt(selectAll, ' ');
		
		return s;
	}
	
	public String where_equals(int value) {
		sb.append('=').append(value).append(';');
		String s = sb.toString();
		sb.setLength(len);
		
		return s;
	}
	
	public String where_in(int[] values) {
		if(Checker.isEmpty(values))
			throw new IllegalArgumentException();
		
		sb.append(" IN (");
		
		for (int i : values) 
			sb.append(i).append(',');
		
		sb.setCharAt(sb.length() - 1, ')');
		sb.append(';');
		String s = sb.toString();
		sb.setLength(len);
		
		return s;
	}
}
