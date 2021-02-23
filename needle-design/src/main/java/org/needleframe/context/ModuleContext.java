package org.needleframe.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class ModuleContext {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Map<String,Module> modules = new LinkedHashMap<String, Module>();
	
	private Map<String,Action> actions = new LinkedHashMap<String, Action>();
	
	private Map<String,Menu> menus = new LinkedHashMap<String, Menu>();
	
	protected void addModules(Map<String,Module> modules) {
		modules.values().forEach(module -> {
			this.modules.computeIfAbsent(module.getName(), v -> module);
			
			Map<String,ModuleProp> mpMap = module.getProps();
			Map<ModuleProp,String> reserveMap = new LinkedHashMap<ModuleProp,String>();
			mpMap.forEach((key, value) -> reserveMap.put(value, key));
			List<ModuleProp> mpList = new ArrayList<ModuleProp>(mpMap.values());
			Collections.sort(mpList, (x,y) -> x.getSortOrder() - y.getSortOrder());
			Map<String,ModuleProp> resultProps = new LinkedHashMap<String,ModuleProp>();
			mpList.forEach(mp -> resultProps.put(reserveMap.get(mp), mp));
			module.setProps(resultProps);
		});
	}
	
	protected void addActions(Map<String,Action> actions) {
		actions.values().forEach(action -> {
			this.actions.computeIfAbsent(action.getIdentity(), v -> action);
		});
	}
	
	protected void addMenus(Map<String,Menu> menus) {
		menus.values().forEach(menu -> {
			this.menus.computeIfAbsent(menu.getName(), v -> menu);
		});
		
		List<Menu> sortMenus = new ArrayList<Menu>(this.menus.values());
		Collections.sort(sortMenus);
		
		this.menus.clear();
		sortMenus.forEach(menu -> {
			this.menus.put(menu.getName(), menu);
		});
	}
	
	/**
	 * 查找模块，如果不存在，返回空值
	 * @param clazz
	 * @return
	 */
	public Module getModule(Class<?> clazz) {
		return getModule(clazz, false);
	}
	
	public Module getModule(Object entity, boolean required) {
		Class<?> clazz = ClassUtils.getUserClass(entity);
		return getModule(clazz, required);
	}
	
	public Module getModule(Class<?> clazz, boolean required) {
		String name = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
		Module module = modules.get(name);
		if(required && module == null) {
			throw new ServiceException("模块" + name + "在上下文中不存在");
		}
		return module.clone();
	}
	
	/**
	 * 查找模块，如果不存在，返回空值
	 * @param name
	 * @return
	 */
	public Module getModule(String name) {
		return getModule(name, false);
	}
	
	public Module getModule(String name, boolean required) {
		Module module = modules.get(name);
		if(required && module == null) {
			throw new ServiceException("模块" + name + "在上下文中不存在");
		}
		return module.clone();
	}
	
	public Action getAction(String id, boolean required) {
		Action action = actions.get(id);
		if(required && action == null) {
			throw new ServiceException("操作" + id + "在上下文中不存在");
		}
		return action.clone();
	}
	
	public Map<String,Module> getModules() {
		Map<String,Module> copy = new LinkedHashMap<String,Module>();
		
		modules.forEach((key, module) -> {
			copy.put(key, module.clone());
		});
		
		return copy;
	}
	
	public Map<String,Action> getActions() {
		Map<String,Action> copy = new LinkedHashMap<String,Action>();
		
		actions.forEach((key, action) -> {
			copy.put(key, action.clone());
		});
		
		return copy;
	}
	
	public Map<String,Menu> getMenus() {
		Map<String,Menu> copy = new LinkedHashMap<String,Menu>();
		
		menus.forEach((key, menu) -> {
			copy.put(key, menu.clone());
		});
		
		return copy;
	}
	
	/**
	 * prop允许嵌套属性
	 * @param module
	 * @param prop
	 * @return
	 */
	public List<ModuleProp> getModuleProps(Module module, String prop) {
		List<ModuleProp> mps = new ArrayList<ModuleProp>();
		String[] propArray = prop.split("\\.");
		ModuleProp last = module.getProp(propArray[0]);
		mps.add(last.clone());
		for(int i = 1; i < propArray.length; i++) {
			RefModule lastRefModule = last.getRefModule();
			if(lastRefModule == null) {
				break;
			}
			last = getModule(lastRefModule.getRefModuleName()).getProp(propArray[i]);
			mps.add(last.clone());
		}
		return mps;
	}
	
//	public ModuleFactory getModuleFactory() {
//		return mf;
//	}
}
