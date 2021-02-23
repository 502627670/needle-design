package org.needleframe.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.needleframe.context.ModuleActionBuilder.CreateAction;
import org.needleframe.context.ModuleActionBuilder.DeleteAction;
import org.needleframe.context.ModuleActionBuilder.ExportAction;
import org.needleframe.context.ModuleActionBuilder.ImportAction;
import org.needleframe.context.ModuleActionBuilder.UpdateAction;
import org.needleframe.core.builder.ActionBuilder;
import org.needleframe.core.builder.MenuBuilder;
import org.needleframe.core.builder.ModuleBuilder;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import lombok.Getter;

public class ModuleFactory {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String securityGroup;
	
	@Getter
	protected Map<String,Action> actions = new LinkedHashMap<String, Action>();
	
	@Getter
	protected Map<String,Module> modules = new LinkedHashMap<String, Module>();
	
	@Getter
	protected Map<String,Menu> menus = new LinkedHashMap<String, Menu>();
	
	@Getter
	protected ActionFactory actionFactory = new ActionFactory();
	
	@Getter
	protected MenuFactory menuFactory = new MenuFactory();
	
	public ModuleFactory() {}
	
	public boolean containsModule(Class<?> clazz) {
		return this.containsModule(Module.getName(clazz));
	}
	
	public boolean containsModule(String name) {
		Module module = modules.get(name);
		return module == null ? false : true;
	}
	
	public Module getModule(Class<?> clazz) {
		String name = Module.getName(clazz);
		return this.getModule(name);
	}
	
	public Module getModule(String name) {
		Module module = modules.get(name);
		if(module == null) {
			throw new ServiceException("模块" + name + "在工厂中不存在");
		}
		return module;
	}
	
	public ModuleFactory enableSecurityGroup(String securityGroupProp) {
		this.securityGroup = securityGroupProp;
		return this;
	}
	
	public ModuleBuilder build(Class<?> clazz) {
		String key = Module.getName(clazz);
		if(this.modules.containsKey(key)) {
			Module module = this.modules.get(key);
			return new ModuleBuilder(module, this);
		}
		Module module = Module.def(clazz);
		this.modules.put(key, module);
		logger.info("build(..) => 构建模块“{}”完成", module.getName());
		ModuleBuilder builder = new ModuleBuilder(module, this);
		builder.addCRUD();
		if(StringUtils.hasText(securityGroup)) {
			builder.securityGroup(this.securityGroup);
		}
		return builder;
	}
	
	/**
	 * 获取嵌套属性对应的ModuleProp实例，如果prop最后一个嵌套属性是对象，自动补全对象的主键属性
	 * 比如x.y中y是对象属性，自动补全为x.y.id，其中id是y对象的主键属性
	 * 比如x.y.z => [ModuleProp(x), ModuleProp(y), ModuleProp(x)]
	 * 有两种情况：Permission.roleId.Role.name => [MP(Permission.roleId), MP(Role.name)]
	 *          Permission.role.name        => [MP(Permission.role), MP(Role.name)]
	 *          Permission.role => Permission.role.id   =>  [MP(Permission.role), MP(Role.id)]
	 * @param root
	 * @param path
	 * @return
	 */
	protected List<ModuleProp> buildPropChains(Module root, String path, boolean leafIsShowProp) {
		List<ModuleProp> chains = new ArrayList<ModuleProp>();
		Module prev = root;
		String[] propArray = path.split("\\.");
		for(int i = 0; i < propArray.length; i++) {
			String propName = propArray[i];
			String firstLetter = propName.substring(0, 1);
			if(firstLetter.toUpperCase().equals(firstLetter)) {  // Permission.roleId.Role.name
				String refModuleName = firstLetter.toLowerCase() + propName.substring(1);
				prev = getModule(refModuleName);
				if(i == propArray.length - 1) {
					chains.add(prev.getProp(prev.getPk()));
				}
			}
			else {
				ModuleProp moduleProp = prev.getProp(propName);
				chains.add(moduleProp);
				RefModule propRefModule = moduleProp.getRefModule();
				if(propRefModule != null) {
					prev = getModule(propRefModule.getRefModuleName());
					if(i == propArray.length - 1) {
						String leafProp = leafIsShowProp ? 
								propRefModule.getRefShowProp() : propRefModule.getRefProp();
						chains.add(prev.getProp(leafProp));
					}
				}
			}
		}
		return chains;
	}
	
	public class ActionFactory {
		
		public ActionBuilder build(String identity) {
			Action action = Action.def(identity);
			this.addAction(action);
			ActionBuilder builder = new ActionBuilder(action, this);
			return builder;
		}
		
		public ActionBuilder getAction(String identity) {
			Action action = actions.get(identity);
			if(action == null) {
				throw new ServiceException("操作" + identity + "不存在");
			}
			return new ActionBuilder(action, this);
		}
		
		public ActionBuilder getDefaultCreate(Class<?> clazz) {
			Module module = getModule(clazz);
			Action defaultAction = module.getActions().stream()
				.filter(action -> action instanceof CreateAction)
				.findFirst()
				.orElseThrow(() -> new ServiceException("模块" + module.getName() + "没有定义默认的新建操作"));
			return new ActionBuilder(defaultAction, this);
		}
		
		public ActionBuilder getDefaultUpdate(Class<?> clazz) {
			Module module = getModule(clazz);
			Action defaultAction = module.getActions().stream()
				.filter(action -> action instanceof UpdateAction)
				.findFirst()
				.orElseThrow(() -> new ServiceException("模块" + module.getName() + "没有定义默认的修改操作"));
			return new ActionBuilder(defaultAction, this);
		}
		
		public ActionBuilder getDefaultDelete(Class<?> clazz) {
			Module module = getModule(clazz);
			Action defaultAction = module.getActions().stream()
				.filter(action -> action instanceof DeleteAction)
				.findFirst()
				.orElseThrow(() -> new ServiceException("模块" + module.getName() + "没有定义默认的删除操作"));
			return new ActionBuilder(defaultAction, this);
		}
		
		public Module getModule(Class<?> clazz) {
			return ModuleFactory.this.getModule(clazz);
		}
		
		public Module getModule(String name) {
			return ModuleFactory.this.getModule(name);
		}
		
		public Action getAction(Class<?> clazz, Act act) {
			Module module = getModule(clazz);
			List<Action> actions = module.getActions();
			
			for(int i = 0; i < actions.size(); i++) {
				Action action = actions.get(i);
				if(Act.CREATE.equals(act) && action instanceof CreateAction) {
					return action;
				}
				else if(Act.UPDATE.equals(act) && action instanceof UpdateAction) {
					return action;
				}
				else if(Act.DELETE.equals(act) && action instanceof DeleteAction) {
					return action;
				}
				else if(Act.IMPORT.equals(act) && action instanceof ImportAction) {
					return action;
				}
				else if(Act.CREATE.equals(act) && action instanceof ExportAction) {
					return action;
				}
			}
			throw new ServiceException("模块" + module.getName() + "的基本操作[Act=" + act + "]不存在");
		}
		
		public Action addAction(Action action) {
			if(!actions.containsKey(action.getIdentity())) {
				actions.put(action.getIdentity(), action);
				logger.info("addAction(..) => 新增操作{}完成", action.getIdentity());
			}
			return action;
		}
		
	}
	
	public class MenuFactory {
		public MenuBuilder build(String name) {
			if(menus.containsKey(name)) {
				throw new ServiceException("菜单" + name + "已存在，不允许重复的菜单名");
			}
			Menu menu = new Menu();
			menu.setName(name);
			menu.setUri("/" + name);
			menu.setSortOrder(menus.size() + 1);
			menus.put(name, menu);
			logger.info("build(..) => 构建菜单{}完成", name);
			return new MenuBuilder(menu, this);
		}
		
		public MenuBuilder build(String name, int sortOrder) {
			if(menus.containsKey(name)) {
				throw new ServiceException("菜单" + name + "已存在，不允许重复的菜单名");
			}
			Menu menu = new Menu();
			menu.setName(name);
			menu.setUri("/");
			menu.setSortOrder(sortOrder);
			menus.put(name, menu);
			logger.info("build(..) => 构建菜单{}完成", name);
			return new MenuBuilder(menu, this);
		}
		
		public Menu getMenu(String name) {
			if(!menus.containsKey(name)) {
				throw new ServiceException("菜单" + name + "不存在");
			}
			return menus.get(name);
		}
		
		public Module getModule(Class<?> clazz) {
			return ModuleFactory.this.getModule(clazz);
		}
	}
}
