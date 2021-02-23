package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ViewJoin;
import org.needleframe.core.model.ViewProp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryProp {
	
	private ViewProp viewProp;
	
	private String lastColumn;
	private String showColumn;
	
	private String path;
	private String showPath;
	
	private List<ViewJoin> viewJoins = new ArrayList<ViewJoin>();
	
	public QueryProp(ViewProp viewProp) {
		this.viewProp = viewProp;
		this.path = viewProp.getProp();
	}
	
	public String asName() {
		return path.replaceAll("\\.", "_").toLowerCase();
	}
	
	public String asShowName() {
		return showPath.replaceAll("\\.", "_").toLowerCase();
	}
	
}

