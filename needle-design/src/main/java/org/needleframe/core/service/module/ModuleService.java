package org.needleframe.core.service.module;

import java.util.List;
import java.util.stream.Collectors;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.model.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModuleService {
	
	@Autowired
	private AppContextService appContextService;
	
	public Module getModule(String name) {
		ModuleContext moduleContext = appContextService.getModuleContext();
		return moduleContext.getModule(name);
	}
	
	public List<Module> findModules() {
		ModuleContext moduleContext = appContextService.getModuleContext();
		return moduleContext.getModules().values().stream()
			.filter(m -> m.isEnableTask())
			.collect(Collectors.toList());
	}
	
}
