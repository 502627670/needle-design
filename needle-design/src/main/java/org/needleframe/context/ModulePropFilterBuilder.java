package org.needleframe.context;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.needleframe.core.builder.ModuleBuilder;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModulePropFilterBuilder {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ModuleFactory mf;
	
	public ModulePropFilterBuilder(ModuleFactory mf) {
		this.mf = mf;
	}
	
	public void buildModuleFilterProps() {
		mf.modules.forEach((moduleName, module) -> {
			buildModuleFilterProps(module);
			guessDefaultFilterIfNecessary(module);
		});
	}
	
	private void buildModuleFilterProps(Module module) {
		module.getFilterProps().forEach(filterProp -> {  // 支持x.y.z嵌套
			List<ModuleProp> chainProps = mf.buildPropChains(module, filterProp.getPath(), true);
			List<ModuleProp> refMProps = chainProps.stream() 
					.filter(mp -> mp.isCollection()).collect(Collectors.toList());
			if(!refMProps.isEmpty()) {
				throw new ServiceException("模块" + module.getName() + "的搜索属性" + filterProp.getPath() + 
						"错误关联了子集合模块的属性，只允许关联父集合的属性");
			}
			ModuleProp lastProp = chainProps.get(0);
			filterProp.setRootProp(lastProp);
			if(chainProps.size() > 1) {
				ModuleProp prevProp = chainProps.get(chainProps.size() - 2);
				ModuleProp refLeafShowProp = chainProps.get(chainProps.size() - 1);
				String path = filterProp.getPath();
				List<String> propList = Arrays.asList(path.split("\\."));
				long firstUpperCaseNumber = propList.stream()
						.filter(item -> Character.isUpperCase(item.charAt(0)))
						.collect(Collectors.counting());
				String refLeafPropName = prevProp.getRefModule().getRefProp();
				ModuleProp refLeafProp = refLeafShowProp.getModule().getProp(refLeafPropName);
				if((propList.size() - firstUpperCaseNumber) < chainProps.size()) {
					filterProp.setPath(String.join(".", path, prevProp.getRefModule().getRefProp()));
				}
				else {
					int index = path.lastIndexOf(".");
					String prefix = path.substring(0, index);
					String tail = path.substring(index + 1);
					String refLeafShowPropName = prevProp.getRefModule().getRefShowProp();
					if(tail.equals(refLeafPropName)) {
						refLeafShowProp = refLeafShowProp.getModule().getProp(refLeafShowPropName);
					}
					else {
						filterProp.setPath(String.join(".", prefix, refLeafPropName));
					}
				}
				filterProp.setRefLeafProp(refLeafProp);
				filterProp.setRefLeafShowProp(refLeafShowProp);
				lastProp = refLeafShowProp;
			}
			if(filterProp.getValues().isEmpty()) {
				filterProp.setValues(lastProp.getValues());
			}
		});
	}
		
	private void guessDefaultFilterIfNecessary(Module module) {
		if(module.getFilterProps().isEmpty()) {
			List<ModuleProp> mps = module.getProps().values().stream()
					.filter(mp -> !mp.isTransientProp())
					.filter(mp -> !mp.isCollection())
					.filter(mp -> !mp.isPk())
					.filter(mp -> !mp.isAuditProp())
					.filter(mp -> !mp.isSecurityGroup())
					.collect(Collectors.toList());
			
			ModuleProp guessMp = null;
			for(int i = 0; i < mps.size(); i++) {
				ModuleProp mp = mps.get(i);
				if(mp.getProp().indexOf("name") > -1 || mp.getType().equals(String.class)) {
					guessMp = mp;
					break;
				}
			}
			
			if(guessMp == null && !mps.isEmpty()) {
				guessMp = mps.get(0);
			}
			
			if(guessMp != null) {
				logger.info("模块" + module.getName() + "没有指定搜索属性，自动猜测其搜索属性为" + guessMp.getProp());
				new ModuleBuilder(module, mf).prop(guessMp.getProp()).filter().eq();
				this.buildModuleFilterProps(module);
			}
		}
	}
	
}
