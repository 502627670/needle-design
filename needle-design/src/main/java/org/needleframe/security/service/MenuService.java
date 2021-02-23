package org.needleframe.security.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.context.ModuleContext;
import org.needleframe.context.AppContextService;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Menu.MenuItem;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
	
	@Autowired
	private AppContextService moduleContextService;
	
	public List<Menu> getAllMenus() {
		ModuleContext mc = moduleContextService.getModuleContext();
		List<Menu> menus = new ArrayList<Menu>(mc.getMenus().values());
		return menus;
	}
	
	public List<Menu> findAccessableMenus(List<Permission> permissions) {
		ModuleContext mc = moduleContextService.getModuleContext();
		List<Menu> menus = new ArrayList<Menu>(mc.getMenus().values());
		Map<String,Permission> permissionMap = permissions.stream()
				.filter(p -> p.getResourceType().equals(ResourceType.MENU))
				.collect(Collectors.toMap(Permission::getName, v -> v, (x, y) -> x));
		
		List<Menu> resultMenus = new ArrayList<Menu>();
		menus.forEach(menu -> {
			if(menu.getChildren().isEmpty()) {
				if(permissionMap.containsKey(menu.getName())) {
					resultMenus.add(menu);
				}
			}
			else {
				List<MenuItem> accessable = new ArrayList<MenuItem>();
				menu.getChildren().forEach(menuItem -> {
					if(menuItem.getChildren().isEmpty()) {
						if(permissionMap.containsKey(menuItem.getName())) {
							accessable.add(menuItem);
						}
					}
					else {
						List<MenuItem> accessChildren = getAccessableChildren(menuItem, permissionMap);
						if(!accessChildren.isEmpty()) {
							accessable.add(menuItem);
						}
					}
				});
				menu.setChildren(accessable);
				if(!menu.getChildren().isEmpty()) {
					resultMenus.add(menu);
				}
			}
		});
		return resultMenus;
	}
	
	private List<MenuItem> getAccessableChildren(MenuItem menuItem, Map<String,Permission> permissionMap) {
		List<MenuItem> accessable = new ArrayList<MenuItem>();
		menuItem.getChildren().forEach(child -> {
			List<MenuItem> accessChildren = getAccessableChildren(child, permissionMap);
			if(accessChildren.isEmpty()) {
				if(permissionMap.containsKey(child.getName())) {
					accessable.add(child);
				}
			}
			else {
				accessable.add(child);
			}
		});
		return accessable;
	}
	
}
