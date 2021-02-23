package org.needleframe;

import java.util.Collection;

import org.needleframe.context.AppContextBuilder;
import org.needleframe.context.AppContextService;
import org.needleframe.context.AppContextServiceHolder;
import org.needleframe.context.ModuleActionBuilder;
import org.needleframe.context.ModuleContext;
import org.needleframe.context.ModuleFactory;
import org.needleframe.context.ModuleFactory.ActionFactory;
import org.needleframe.context.ModuleFactory.MenuFactory;
import org.needleframe.core.model.Module;
import org.springframework.context.annotation.ComponentScan;

import lombok.Getter;

@ComponentScan
public abstract class AbstractContextService implements AppContextService {
	
	@Getter
	private ModuleFactory moduleFactory = new ModuleFactory();
		
	private ModuleActionBuilder moduleActionBuilder = new ModuleActionBuilder(moduleFactory.getActionFactory());
	
	protected AbstractContextService() {
		AppContextServiceHolder.services.add(this);
		
		defModules(moduleFactory);
		Collection<Module> moduleList = moduleFactory.getModules().values();
		moduleActionBuilder.buildDefaultActions(moduleList);
		
		defActions(moduleFactory.getActionFactory());
		
		defMenus(moduleFactory.getMenuFactory());
	}
	
	public ModuleContext getModuleContext() {
		return AppContextBuilder.mc;
	}
	
	protected abstract void defModules(ModuleFactory mf);
	
	protected void defActions(ActionFactory af) {}
	
	protected void defMenus(MenuFactory mf) {}
}
