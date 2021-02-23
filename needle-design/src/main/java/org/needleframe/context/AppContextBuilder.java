package org.needleframe.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Module;
import org.springframework.stereotype.Service;

@Service
public class AppContextBuilder {
	
	public static ModuleContext mc = new ModuleContext();
	
	private ModuleFactory mf = new ModuleFactory();
	private ModuleRefBuilder moduleRefBuilder = new ModuleRefBuilder(mf);
	private ModulePropRefBuilder modulePropRefBuilder = new ModulePropRefBuilder(mf);
	private ModulePropFilterBuilder modulePropFilterBuilder = new ModulePropFilterBuilder(mf);
	private ModuleActionBuilder moduleActionBuilder = new ModuleActionBuilder(mf.getActionFactory());
	
	private Map<String,Module> modules = new LinkedHashMap<String, Module>();
	private Map<String,Action> actions = new LinkedHashMap<String, Action>();
	private Map<String,Menu> menus = new LinkedHashMap<String, Menu>();
	
	@PostConstruct
	public void buildeContext() {
		AppContextServiceHolder.services.forEach(service -> {
			ModuleFactory mf = service.getModuleFactory();
			addModules(mf.getModules());
			addActions(mf.getActions());
			addMenus(mf.getMenus());
		});
		
		mf.modules = this.modules;
		mf.actions = this.actions;
		mf.menus = this.sortMenus();
		
		List<Module> moduleList = new ArrayList<Module>(mf.getModules().values());
		List<Module> refModuleList = moduleRefBuilder.buildRefModules(moduleList);
		moduleActionBuilder.buildDefaultActions(refModuleList);
		
		modulePropRefBuilder.buildAllPropRefs();
		modulePropFilterBuilder.buildModuleFilterProps();
		
		mc.addModules(modules);
		mc.addActions(actions);
		mc.addMenus(menus);
	}
	
	private void addModules(Map<String,Module> modules) {
		modules.values().forEach(module -> {
			if(this.modules.containsKey(module.getName())) {
				throw new ServiceException("模块" + module.getName() + "已存在，不能重复定义");
			}
			this.modules.put(module.getName(), module);
		});
	}
	
	private void addActions(Map<String,Action> actions) {
		actions.values().forEach(action -> {
			if(this.actions.containsKey(action.getIdentity())) {
				throw new ServiceException("操作" + action.getIdentity() + "已存在，不能重复定义");
			}
			this.actions.put(action.getIdentity(), action);
		});
	}
	
	private void addMenus(Map<String,Menu> menus) {
		menus.values().forEach(menu -> {
			if(this.menus.containsKey(menu.getName())) {
				throw new ServiceException("菜单" + menu.getName() + "已存在，不能重复定义");
			}
			this.menus.put(menu.getName(), menu);
		});
	}
	
	private Map<String,Menu> sortMenus() {
		List<Menu> sortMenus = new ArrayList<Menu>(this.menus.values());
		Collections.sort(sortMenus);
		
		this.menus.clear();
		sortMenus.forEach(menu -> {
			this.menus.put(menu.getName(), menu);
		});
		return this.menus;
	}
	
}
