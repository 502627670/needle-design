package org.needleframe.core.builder;

import org.needleframe.context.ModuleFactory.MenuFactory;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Menu.MenuItem;
import org.needleframe.core.model.Module;

public class MenuBuilder {
	
	private MenuFactory mf;
	private Menu menu;
	
	public MenuBuilder(Menu menu, MenuFactory mf) {
		this.menu = menu;
		this.mf = mf;
	}
	
	public MenuBuilder name(String name) {
		this.menu.setName(name);
		return this;
	}
	
	public MenuBuilder icon(String icon) {
		this.menu.setIcon(icon);
		return this;
	}
	
	public MenuBuilder uri(String uri) {
		this.menu.setUri(uri);
		return this;
	}
	
	public MenuBuilder sortOrder(int sortOrder) {
		this.menu.setSortOrder(sortOrder);
		return this;
	}
	
	public MenuItemBuilder addItem(String name) {
		MenuItem menuItem = new MenuItem();
		menuItem.setName(name);
		menuItem.setUri("/");
		menuItem.setSortOrder(this.menu.getChildren().size() + 1);
		MenuItemBuilder itemBuilder = new MenuItemBuilder(menuItem, this);
		this.menu.getChildren().add(menuItem);
		return itemBuilder;
	}
	
	public MenuItemBuilder addItem(Class<?> clazz) {
		Module module = mf.getModule(clazz);
		MenuItem menuItem = new MenuItem();
		menuItem.setName(module.getName());
		menuItem.setUri(module.getUri());
		menuItem.setSortOrder(this.menu.getChildren().size() + 1);
		MenuItemBuilder itemBuilder = new MenuItemBuilder(menuItem, this);
		this.menu.getChildren().add(menuItem);
		return itemBuilder;
	}
	
	public MenuFactory and() {
		return mf;
	}
	
	public class MenuItemBuilder {
		private MenuBuilder parent;
		private MenuItem menuItem;
		
		MenuItemBuilder(MenuItem menuItem, MenuBuilder parent) {
			this.menuItem = menuItem;
			this.parent = parent;
		}
		
		public MenuItemBuilder name(String name) {
			this.menuItem.setName(name);
			return this;
		}
		
		public MenuItemBuilder icon(String icon) {
			this.menuItem.setIcon(icon);
			return this;
		}
		
		public MenuItemBuilder uri(String uri) {
			this.menuItem.setUri(uri);
			return this;
		}
		
		public MenuItemBuilder sortOrder(int sortOrder) {
			this.menuItem.setSortOrder(sortOrder);
			return this;
		}
		
		public MenuItemBuilder addItem(Class<?> clazz) {
			return this.parent.addItem(clazz);
		}
		
		public MenuFactory and() {
			return parent.mf;
		}
	}
}
