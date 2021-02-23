package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewJoin;

import lombok.Getter;
import lombok.Setter;

@Getter
public class QueryFilter {
	
	@Setter
	private ViewFilter viewFilter;
	
	@Setter
	private String lastColumn;
	
	@Setter
	private Object value;
	
	@Setter
	private List<ViewJoin> viewJoins = new ArrayList<ViewJoin>();
	
	public QueryFilter(ViewFilter viewFilter) {
		this.viewFilter = viewFilter;
	}
	
	public Op getOp() {
		return Op.toOp(viewFilter.getOp());
	}
	
	public Object getValue() {
		return value;
	}
	
	public Object value(Object value) {
		this.value = value;
		return this;
	}
	
	public QueryFilter value(Module module, QueryProp prop, List<QueryFilter> filters) {
		this.value = new QuerySubFilter(module, prop, filters);
		return this;
	}
	
	public QueryFilter clone() {
		QueryFilter clone = new QueryFilter(viewFilter);
		clone.setLastColumn(lastColumn);
		clone.setValue(value);
		clone.setViewJoins(viewJoins);
		return clone;
	}
	
	public static class QuerySubFilter {
		Module module;
		QueryProp prop;
		List<QueryFilter> filters;
		
		public QuerySubFilter(Module module, QueryProp prop, List<QueryFilter> filters) {
			this.module = module;
			this.prop = prop;
			this.filters = filters;
		}
	}
	
}

