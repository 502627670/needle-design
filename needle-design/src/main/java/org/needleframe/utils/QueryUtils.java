package org.needleframe.utils;

import java.util.List;

import org.needleframe.core.model.ViewFilter;

public class QueryUtils {
	
	public static String boolFilter(List<ViewFilter> viewFilters, String andOrOr, int startCount) {
		StringBuilder boolBuilder = new StringBuilder();
		if(!viewFilters.isEmpty()) {
			andOrOr = andOrOr.trim();
			boolBuilder.append("(");
			for(int i = 0; i < viewFilters.size(); i++) {
				boolBuilder.append(++startCount);
				if(i < viewFilters.size() - 1) {
					boolBuilder.append(" ").append(andOrOr).append(" ");
				}
			}
			boolBuilder.append(")");
		}
		return boolBuilder.toString().trim();
	}
	
	public static String boolFilter(List<ViewFilter> andFilters, List<ViewFilter> orFilters) {
		StringBuilder boolBuilder = new StringBuilder();
		String boolAnd = andFilter(andFilters);
		String boolOr = boolFilter(orFilters, "or", andFilters.size());
		boolBuilder.append(boolAnd);
		if(boolAnd.length() > 0 && boolOr.length() > 0) {
			boolBuilder.append(" and ");
		}
		boolBuilder.append(boolOr);
		return boolBuilder.toString().trim();
	}
	
	public static String andFilter(List<ViewFilter> viewFilters) {
		return boolFilter(viewFilters, "and", 0);
	}
	
	public static String orFilter(List<ViewFilter> viewFilters) {
		return boolFilter(viewFilters, "or", 0);
	}
	
}
