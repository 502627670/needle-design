package org.needleframe.security.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.context.ModuleContext;
import org.needleframe.context.AppContextService;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Menu.MenuItem;
import org.needleframe.core.model.Module;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource;
import org.needleframe.security.domain.Resource.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {
	
	@Autowired
	private AppContextService moduleContextService;
		
	public List<Resource> getAllMenuResources() {
		ModuleContext mc = moduleContextService.getModuleContext();
		List<Resource> resources = new ArrayList<Resource>();
		mc.getMenus().values().stream().forEach(menu -> {
			Resource parent = new Resource();
			parent.setName(menu.getName());
			parent.setResourceType(ResourceType.MENU);
			parent.setUri(menu.getUri());
			resources.add(parent);
			
			menu.getChildren().forEach(menuItem -> {
				Resource resource = new Resource();
				resource.setName(menuItem.getName());
				resource.setResourceType(ResourceType.MENU);
				resource.setUri(menuItem.getUri());
				parent.getChildren().add(resource);
			});
		});
		return resources;
	}
	
	public List<Resource> getAllActionResources() {
		List<Resource> resources = new ArrayList<Resource>();
		ModuleContext mc = moduleContextService.getModuleContext();
		String noModuleActionsKey = String.valueOf(System.currentTimeMillis());
		Map<String,List<Action>> actionsMap = new LinkedHashMap<String,List<Action>>();
		mc.getActions().values().stream()
			.forEach(action -> {
				 Module module = mc.getModule(action.getModule(), true);
				 String key = module == null ? noModuleActionsKey : module.getName();
				 List<Action> moduleActions = actionsMap.computeIfAbsent(key, v -> new ArrayList<Action>());
				 moduleActions.add(action);
			});
		actionsMap.forEach((moduleName, actionList) -> {
			Module module = mc.getModule(moduleName, false);
			List<Resource> children = resources;
			if(module != null) {
				Resource parent = new Resource();
				parent.setName(module.getName());
				parent.setShowName(module.getShowName());
				parent.setResourceType(ResourceType.ACTION);
				resources.add(parent);
				children = parent.getChildren();
			}
			for(int i = 0; i < actionList.size(); i++) {
				Action action = actionList.get(i);
				Resource resource = new Resource();
				resource.setName(action.getIdentity());
				resource.setShowName(action.getName());
				resource.setResourceType(ResourceType.ACTION);
				resource.setUri(action.getUri());
				children.add(resource);
			}
		});
		return resources;
	}
	
	public List<Resource> findViewResources(List<Permission> permissions) {
		List<Resource> resources = permissions.stream()
				.filter(p -> ResourceType.VIEW.equals(p.getResourceType()))
				.map(p -> {
					Resource resource = new Resource();
					resource.setName(p.getResource());
					resource.setResourceType(ResourceType.VIEW);
					resource.setUri(p.getResourceUri());
					return resource;
				})
				.collect(Collectors.toList());
		return resources;
	}
	
	public List<Resource> findMenuResources(List<Permission> permissions) {
		List<Resource> resources = new ArrayList<Resource>();
		ModuleContext mc = moduleContextService.getModuleContext();
		List<Menu> defaultMenus = new ArrayList<Menu>(mc.getMenus().values());
		Map<String,Permission> permissionMap = permissions.stream()
				.filter(p -> ResourceType.MENU.equals(p.getResourceType()))
				.collect(Collectors.toMap(Permission::getName, v -> v, (x,y) -> x));
		
		defaultMenus.forEach(menu -> {
			List<Resource> children = new ArrayList<Resource>();
			List<MenuItem> menuItems = menu.getChildren();
			for(int i = 0; i < menuItems.size(); i++) {
				MenuItem menuItem = menuItems.get(i);
				if(permissionMap.containsKey(menuItem.getName())) {
					Resource resource = new Resource();
					resource.setName(menuItem.getName());
					resource.setResourceType(ResourceType.MENU);
					resource.setUri(menuItem.getUri());
					children.add(resource);
				}
			}
			
			if(children.size() > 0 || permissionMap.containsKey(menu.getName())) {
				Resource parent = new Resource();
				parent.setName(menu.getName());
				parent.setResourceType(ResourceType.MENU);
				parent.setChildren(children);
				resources.add(parent);
			}
		});
		return resources;
	}
	
	public List<Resource> findActionResources(List<Permission> permissions) {
		List<Resource> resources = new ArrayList<Resource>();
		ModuleContext mc = moduleContextService.getModuleContext();
		Map<String,Permission> permissionMap = permissions.stream()
				.filter(p -> ResourceType.ACTION.equals(p.getResourceType()))
				.collect(Collectors.toMap(Permission::getName, v -> v, (x,y) -> x));
		String noModuleActionsKey = String.valueOf(System.currentTimeMillis());
		Map<String,List<Action>> actionsMap = new LinkedHashMap<String,List<Action>>();
		mc.getActions().values().stream()
			.filter(action -> permissionMap.containsKey(action.getIdentity()))
			.forEach(action -> {
				 Module module = mc.getModule(action.getModule(), true);
				 String key = module == null ? noModuleActionsKey : module.getName();
				 List<Action> moduleActions = actionsMap.computeIfAbsent(key, v -> new ArrayList<Action>());
				 moduleActions.add(action);
			});
		actionsMap.forEach((moduleName, actionList) -> {
			Module module = mc.getModule(moduleName, false);
			List<Resource> children = resources;
			if(module != null) {
				Resource parent = new Resource();
				parent.setName(module.getName());
				parent.setShowName(module.getShowName());
				parent.setResourceType(ResourceType.ACTION);
				resources.add(parent);
				children = parent.getChildren();
			}
			for(int i = 0; i < actionList.size(); i++) {
				Action action = actionList.get(i);
				Resource resource = new Resource();
				resource.setName(action.getIdentity());
				resource.setShowName(action.getName());
				resource.setResourceType(ResourceType.ACTION);
				resource.setUri(action.getUri());
				children.add(resource);
			}
		});
		return resources;
	}
	
	public List<Resource> resolveCheckedMenuResources(List<Resource> resources, List<Permission> permissions) {
		Map<String,Permission> permissionMap = permissions.stream()
				.filter(p -> ResourceType.MENU.equals(p.getResourceType()))
				.collect(Collectors.toMap(Permission::getName, v -> v, (x,y) -> x));
		resources.forEach(resource -> {
			resolveCheckableResources(resource, permissionMap);
		});
		return resources;
	}
	
	public List<Resource> resolveCheckedViewResources(List<Resource> resources, List<Permission> permissions) {
		List<Resource> resourceList = new ArrayList<Resource>();
		permissions.stream()
			.filter(p -> ResourceType.VIEW.equals(p.getResourceType()))
			.forEach(permission -> {
			Resource resource = new Resource();
			resource.setName(permission.getName());
			resource.setResourceType(permission.getResourceType());
			resource.setUri(permission.getResourceUri());
			resource.setChecked(true);
			resourceList.add(resource);
		});
		return resourceList;
	}
	
	public List<Resource> resolveCheckedActionResources(List<Resource> resources, List<Permission> permissions) {
		Map<String,Permission> permissionMap = permissions.stream()
				.filter(p -> ResourceType.ACTION.equals(p.getResourceType()))
				.collect(Collectors.toMap(Permission::getResource, v -> v, (x,y) -> x));
		resources.forEach(resource -> {
			resolveCheckableResources(resource, permissionMap);
		});
		return resources;
	}
	
	private List<Resource> resolveCheckableResources(Resource resource, Map<String,Permission> permissionMap) {
		List<Resource> allResources = new ArrayList<Resource>();
		List<Resource> children = resource.getChildren();
		children.forEach(child -> {
			allResources.addAll(resolveCheckableResources(child, permissionMap));
		});
		if(permissionMap.containsKey(resource.getName())) {
			resource.setChecked(true);
		}
		allResources.add(resource);
		return allResources;
	}
}
