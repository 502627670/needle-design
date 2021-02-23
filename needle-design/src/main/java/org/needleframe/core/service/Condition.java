package org.needleframe.core.service;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.springframework.data.domain.Sort;

import lombok.Data;

@Data
public class Condition {
	
	private List<ViewProp> viewProps = new ArrayList<ViewProp>();
	private List<ViewFilter> viewFilters = new ArrayList<ViewFilter>();
	private Sort sort;
	private String boolFilter;
	
	public Condition(List<ViewProp> viewProps, List<ViewFilter> viewFilters) {
		this.viewProps = viewProps;
		this.viewFilters = viewFilters;
	}
	
	public Condition(List<ViewProp> viewProps, List<ViewFilter> viewFilters, Sort sort) {
		this(viewProps, viewFilters, null, sort);
	}
	
	public Condition(List<ViewProp> viewProps, List<ViewFilter> viewFilters,
			String boolFilter, Sort sort) {
		this.viewProps = viewProps;
		this.viewFilters = viewFilters;
		this.boolFilter = boolFilter;
		this.sort = sort;
	}
	
}
