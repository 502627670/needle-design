package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Menu implements Cloneable, Comparable<Menu> {
	
	private String icon;
	private String name;
	private String uri;
	private Integer sortOrder = 0;
	private List<MenuItem> children = new ArrayList<MenuItem>();
	
	public Integer getSortOrder() {
		return sortOrder == null ? 0 : sortOrder;
	}
	
	@Override
	public int compareTo(Menu o) {
		return this.sortOrder - o.getSortOrder();
	}
	
	public Menu clone() {
		Menu copy = new Menu();
		BeanUtils.copyProperties(this, copy);
		
		List<MenuItem> copyOfChildren = new ArrayList<MenuItem>();
		children.forEach(child -> {
			copyOfChildren.add(child.clone());
		});
		copy.setChildren(copyOfChildren);
		
		return copy;
	}
	
	@Getter
	@Setter
	public static class MenuItem implements Cloneable {
		private String icon;
		private String name;
		private String uri;
		private Integer sortOrder;
		private Menu parent;
		private List<MenuItem> children = new ArrayList<MenuItem>();
		
		public MenuItem clone() {
			MenuItem copy = new MenuItem();
			BeanUtils.copyProperties(this, copy);
			
			List<MenuItem> copyOfChildren = new ArrayList<MenuItem>();
			children.forEach(child -> {
				copyOfChildren.add(child.clone());
			});
			copy.setChildren(copyOfChildren);
			
			return copy;
		}
	}
}
