package org.needleframe.context;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class ModulePropRefBuilder {
	
	private ModuleFactory mf;
	
	public ModulePropRefBuilder(ModuleFactory mf) {
		this.mf = mf;
	}
	
	public void buildAllPropRefs() {
		mf.modules.forEach((moduleName, module) -> {
			buildModulePropRefs(module);
		});
	}
	
	private void buildModulePropRefs(Module module) {
		module.getProps().values().forEach(moduleProp -> {
			if(moduleProp.isRefProp()) {
				if(moduleProp.isRefParent()) {
					buildPropRefs(module, moduleProp);
				}
				else {
					buildChildrenRefs(module, moduleProp);
				}
			}
		});
	}
	
	private void buildPropRefs(Module module, ModuleProp moduleProp) {
		String prop = moduleProp.getProp();
		Class<?> type = moduleProp.getType();
		String name = Module.getName(type);
	    Module ref = mf.getModule(type);
		if(ref == null) {
			throw new ServiceException("构建模型属性的引用关系失败：模型“" + module.getName() 
					+ "”的属性“" + prop + "”引用了不存在的模型“" + name + "”");
		}
		
		RefModule refModule = moduleProp.getRefModule();
		if(refModule == null) {
			refModule = new RefModule();
			moduleProp.setRefModule(refModule);
		}
		if(!StringUtils.hasText(refModule.getProp())) {
			refModule.setModuleName(module.getName());
			refModule.setProp(moduleProp.getProp());
		}
		refModule.setRefModuleName(ref.getName());
		if(StringUtils.hasText(refModule.getRefProp())) {
			ModuleProp refModuleProp = ref.getProp(refModule.getRefProp());
			refModule.setRefPropType(refModuleProp.getType());
		}
		else {
			ModuleProp refModuleProp = detectJoinColumnProp(module, prop, ref);
			if(refModuleProp == null) {
				refModuleProp = ref.getProp(ref.getPk());
			}
			refModule.setRefProp(refModuleProp.getProp());
			refModule.setRefPropType(refModuleProp.getType());
		}
		if(StringUtils.hasText(refModule.getRefShowProp())) {
			ModuleProp showModuleProp = ref.getProp(refModule.getRefShowProp());
			refModule.setRefShowType(showModuleProp.getType());
		}
		else {
			refModule.setRefShowProp(refModule.getRefProp());
			refModule.setRefShowType(refModule.getRefPropType());
		}
	}
	
	private void buildChildrenRefs(Module module, ModuleProp moduleProp) {
		String prop = moduleProp.getProp();
		BeanWrapper rootWrapper = module.getBeanWrapper();
		try {
			Field field = ReflectionUtils.findField(rootWrapper.getWrappedClass(), f -> f.getName().equals(prop));
			OneToMany oneToMany = field.getAnnotation(OneToMany.class);
			if(oneToMany != null) {
				ParameterizedType genericType = (ParameterizedType) field.getGenericType();
				Type refClass = genericType.getActualTypeArguments()[0];
				Module ref = mf.getModule((Class<?>) refClass);
				if(ref == null) {
					throw new ServiceException("构建模型属性的引用关系失败：模型“" + module.getName() 
							+ "”的属性“" + prop + "”引用了不存在的模型“" + genericType.getTypeName() + "”");
				}
				String mappedBy = oneToMany.mappedBy();
				ModuleProp mappedByProp = ref.getProp(mappedBy);
				
				RefModule refModule = moduleProp.getRefModule();
				if(refModule == null) {
					refModule = new RefModule();
					moduleProp.setRefModule(refModule);
				}
				
				if(!StringUtils.hasText(refModule.getProp())) {
					refModule.setModuleName(module.getName());
					refModule.setProp(module.getPk());
				}
				refModule.setRefModuleName(ref.getName());
				if(StringUtils.hasText(refModule.getRefProp())) {
					ModuleProp refMp = ref.getProp(refModule.getRefProp());
					refModule.setRefPropType(refMp.getType());
				}
				else {
					refModule.setRefProp(mappedByProp.getProp());
					refModule.setRefPropType(mappedByProp.getType());
				}
				if(StringUtils.hasText(refModule.getRefShowProp())) {
					ModuleProp showMp = ref.getProp(refModule.getRefShowProp());
					refModule.setRefShowType(showMp.getType());
				}
				else {
					refModule.setRefShowProp(mappedByProp.getProp());
					refModule.setRefShowType(mappedByProp.getType());
				}
			}
		} catch (Exception e) {}
	}
	
	private ModuleProp detectJoinColumnProp(Module module, String prop, Module ref) {
		ModuleProp refModuleProp = null;
		BeanWrapper rootWrapper = module.getBeanWrapper();
		Field field = ReflectionUtils.findField(rootWrapper.getWrappedClass(), f -> f.getName().equals(prop));
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		if(joinColumn != null) {
			String refColumn = joinColumn.referencedColumnName();
			if(StringUtils.hasText(refColumn)) {
				Optional<ModuleProp> refPropOptional = ref.getProps().values().stream()
					.filter(mp -> mp.getColumn().equals(refColumn)).findFirst();
				if(refPropOptional.isPresent()) {
					refModuleProp = refPropOptional.get();
				}
			}
		}
		return refModuleProp;
	}
	
}
