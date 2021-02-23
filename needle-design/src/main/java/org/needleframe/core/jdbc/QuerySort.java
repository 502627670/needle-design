package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ViewJoin;
import org.springframework.data.domain.Sort.Order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySort {
	
	private Order order;
	
	private String lastColumn;
	
	private List<ViewJoin> viewJoins = new ArrayList<ViewJoin>();
	
	public QuerySort(Order order) {
		this.order = order;
	}
	
	public String getDirection() {
		return order.getDirection().name();
	}
}
