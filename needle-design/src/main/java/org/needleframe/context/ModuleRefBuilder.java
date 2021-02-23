package org.needleframe.context;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.OneToMany;

import org.needleframe.core.model.Module;
import org.springframework.data.util.ReflectionUtils;

public class ModuleRefBuilder {
	
	private ModuleFactory mf;
	
	public ModuleRefBuilder(ModuleFactory mf) {
		this.mf = mf;
	}
	
	public List<Module> buildRefModules(List<Module> modules) {
		List<Module> results = new ArrayList<Module>();
		modules.forEach(module -> {
			results.addAll(buildRefModules(module));
		});
		return results;
	}
	
	public List<Module> buildRefModules(Module module) {
		List<Module> refModules = new ArrayList<Module>();
		module.getProps().values().stream()
			.filter(mp -> !mp.isTransientProp())
			.filter(mp -> mp.isRefProp()).forEach(mp -> {
			if(mp.isRefParent()) {
				Class<?> parentClass = mp.getType();
				if(!this.mf.containsModule(parentClass)) {
					Module parentModule = this.mf.build(parentClass).getModule();
					refModules.add(parentModule);
				}
			}
			else {
				Class<?> clazz = module.getModelClass();
				String propName = mp.getProp();
				Field field = ReflectionUtils.findField(clazz, f -> f.getName().equals(propName));
				OneToMany oneToMany = field.getAnnotation(OneToMany.class);
				if(oneToMany != null) {
					ParameterizedType genericType = (ParameterizedType) field.getGenericType();
					Class<?> refClass = (Class<?>) genericType.getActualTypeArguments()[0];
					
					if(!this.mf.containsModule(refClass)) {
						Module refModule = this.mf.build(refClass).getModule();
						refModules.add(refModule);
					}
				}
			}
		});
		return refModules;
	}
}
