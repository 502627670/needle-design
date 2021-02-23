package org.needleframe.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewJoin {
	
	private String path;
	private String table;
	private String column;
	private String refTable;
	private String refColumn;
	
	public ViewJoin() {}
}
