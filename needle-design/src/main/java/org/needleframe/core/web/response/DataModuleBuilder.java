package org.needleframe.core.web.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.needleframe.core.model.Act;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Action.ActionProp;
import org.needleframe.core.model.Action.ActionStatus;
import org.needleframe.core.model.FilterProp;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.Decoder;
import org.needleframe.core.model.ModuleProp.Encoder;
import org.needleframe.core.model.ModuleProp.Feature;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.needleframe.core.model.ModuleProp.Select;
import org.needleframe.core.model.ViewProp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

public class DataModuleBuilder {
	
	private Module module;
	
	private DataModule dataModule;
	
	public DataModuleBuilder(Module module) {
		this.module = module;
		this.dataModule = new DataModule(this.module);
	}
	
	public DataModule build() {
		return this.build(new ArrayList<ViewProp>());
	}
	
	public DataModule build(List<ViewProp> viewProps) {
		Collection<ModuleProp> props = module.getProps().values();
		List<FilterProp> filterProps = module.getFilterProps();
		List<Action> actions = module.getActions();
		Map<String,Module> children = module.getChildren();
		this.props(props)
			.viewProps(viewProps)
			.filterProps(filterProps)
			.actions(actions)
			.children(children);
		return this.dataModule;
	}
	
	public DataModuleBuilder props(Collection<ModuleProp> moduleProps) {
		List<DataProp> props = new ArrayList<DataProp>();
		moduleProps.stream().filter(mp -> !mp.isCollection()).forEach(mp -> {
			DataProp dataProp = new DataProp();
			dataProp.setName(mp.getName());
			dataProp.setProp(mp.getProp());
			dataProp.setType(mp.getType().getSimpleName());
			dataProp.setLength(mp.getLength());
			dataProp.setRuleType(mp.getRuleType());
			dataProp.setFeature(mp.getFeature());
			dataProp.setValues(mp.getValues());
			dataProp.setDefaultValue(mp.getDefaultValue());
			dataProp.setMappings(mp.getMappings());
			if(mp.isRefProp()) {
				dataProp.setType(null);
				RefModule ref = mp.getRefModule();
				dataProp.setRefModuleName(ref.getRefModuleName());
				dataProp.setRefProp(ref.getRefProp());
				dataProp.setRefType(ref.getRefPropType().getSimpleName());
				dataProp.setShowProp(ref.getRefShowProp());
				dataProp.setShowType(ref.getRefShowType().getSimpleName());
				dataProp.setShowPath(String.join(".", dataProp.getProp(), dataProp.getShowProp()));
			}
			props.add(dataProp);
		});
		this.dataModule.setProps(props);
		return this;
	}
	
	public DataModuleBuilder viewProps(Collection<ViewProp> viewProps) {
		List<DataViewProp> dataViewProps = new ArrayList<DataViewProp>(); 
		viewProps.forEach(vp -> {
			String prop = vp.getProp();
			String name = vp.getName();
			DataViewProp dataProp = new DataViewProp();
			dataProp.setName(name);
			dataProp.setProp(prop);
			dataProp.setType(vp.getType().getSimpleName());
			dataProp.setTypeClass(vp.getType());
			dataProp.setRuleType(vp.getRuleType());
			dataProp.setPattern(vp.getPattern());
			dataProp.setMaxLength(vp.getName().length());
			dataProp.setFeature(vp.getFeature());
			dataProp.setEncoder(vp.getEncoder());
			dataProp.setDecoder(vp.getDecoder());
			dataViewProps.add(dataProp);
		});
		this.dataModule.setViewProps(dataViewProps);
		return this;
	}
	
	public DataModuleBuilder filterProps(Collection<FilterProp> filterProps) {
		List<DataFilter> dataFilters = new ArrayList<DataFilter>();
		filterProps.forEach(fp -> {
			DataFilter dataFilter = new DataFilter();
			ModuleProp mp = fp.getRootProp();
			dataFilter.setName(mp.getName());
			dataFilter.setProp(fp.getPath());
			if(fp.getRefLeafProp() != null) {
				ModuleProp leafProp = fp.getRefLeafProp();
				
				if(module.isRefSelf()) {
					Optional<ModuleProp> selfRefProp = 
						leafProp.getModule().getProps().values().stream()
							.filter(lrmp -> lrmp.isRefParent())
							.filter(lrmp -> lrmp.getRefModule().isRefSelf())
							.findFirst();
					if(selfRefProp.isPresent()) {
						ModuleProp selfMp = selfRefProp.get();
						dataFilter.setSelfModule(selfMp.getModule().getName());
						dataFilter.setSelfPk(selfMp.getModule().getPk());
						dataFilter.setSelfRefProp(selfMp.getProp());
					}
				}
				
				dataFilter.setRefLeafModule(leafProp.getModule().getName());
				dataFilter.setRefLeaf(leafProp.getProp());
				dataFilter.setRefLeafShow(fp.getRefLeafShowProp().getProp());
				mp = fp.getRefLeafShowProp();
			}
			String leafType = Enum.class.isAssignableFrom(mp.getType()) ? 
					String.class.getSimpleName() : mp.getType().getSimpleName();
			dataFilter.setLeafType(leafType);
			dataFilter.setOps(fp.getOps());
			dataFilter.setValues(fp.getValues());
			dataFilter.setFeature(mp.getFeature());
			dataFilters.add(dataFilter);
		});
		this.dataModule.setFilterProps(dataFilters);
		return this;
	}
	
	public DataModuleBuilder children(Map<String,Module> children) {
		List<DataChild> dataChildren = new ArrayList<DataChild>();
		children.forEach((refPropName, child) -> {
			String[] propArray = refPropName.split("\\.");
			refPropName = propArray[propArray.length - 1];
			
			ModuleProp refModuleProp = child.getProp(refPropName);
			DataChild dataChild = new DataChild();
			dataChild.setPk(child.getPk());
			dataChild.setName(child.getName());
			dataChild.setRefProp(refModuleProp.getProp());
			
			child.getProps().values().forEach(mp -> {
				if(!mp.isAuditProp()) {
					DataProp dataProp = new DataProp();
					dataProp.setName(mp.getName());
					dataProp.setProp(mp.getProp());
					dataProp.setShowProp(mp.getProp());
					dataProp.setRefType(mp.getType().getSimpleName());
					dataProp.setShowType(mp.getType().getSimpleName());
					dataProp.setRuleType(mp.getRuleType());
					if(mp.isRefProp()) {
						RefModule ref = mp.getRefModule();
						dataProp.setRefModuleName(ref.getRefModuleName());
						dataProp.setRefProp(ref.getRefProp());
						dataProp.setShowProp(ref.getRefShowProp());
						dataProp.setRefType(ref.getRefPropType().getSimpleName());
						dataProp.setShowType(ref.getRefShowType().getSimpleName());
						if(!mp.getProp().equals(dataChild.getRefProp())) {
							dataChild.getOtherRefProps().add(mp.getProp());
						}
					}
					dataChild.getProps().add(dataProp);
				}
			});
			dataChildren.add(dataChild);
		});
		this.dataModule.setChildren(dataChildren);
		return this;
	}
	
	public DataModuleBuilder actions(Collection<Action> actions) {
		List<DataAction> dataActions = new ArrayList<DataAction>();
		actions.forEach(a -> {
			DataAction dataAction = new DataAction();
			dataAction.setId(a.getIdentity());
			dataAction.setName(a.getName());
			dataAction.setUri(a.getUri());
			dataAction.setBatchable(a.isBatchable());
			dataAction.setAsync(a.isAsync());
			dataAction.setActionStatus(a.getActionStatus());
			dataAction.setAct(a.getAct());
			dataAction.setProps(a.getProps());
			dataAction.setDisableProp(a.getDisableProp());
			dataAction.setDisableValues(a.getDisableValues());
			dataActions.add(dataAction);
		});
		this.dataModule.setActions(dataActions);
		return this;
	}
	
	@Getter
	@Setter
	public static class DataChild {
		private String showName;
		private String name;
		private String pk;
		private String refProp;
		private List<String> otherRefProps = new ArrayList<String>();
		private List<DataProp> props = new ArrayList<DataProp>();
		private List<DataFilter> filterProps = new ArrayList<DataFilter>();
	}
	
	@Getter
	@Setter
	public static class DataFilter {  // Permission.role  或者Permission.roleId.Role.name
		private String name;
		private String prop;          // role             或者roleId
		private String leafType;      // String.class
		
		private String refLeafModule; // role
		private String refLeaf;       // [role.]id
		private String refLeafShow;   // [role.]name
		
		private String selfModule;    // Group
		private String selfPk;        // id
		private String selfRefProp;   // parent
		
		private List<Select> ops = new ArrayList<Select>();
		private List<Select> values = new ArrayList<Select>();
		public Feature feature;
	}

	@Getter
	@Setter
	public static class DataProp {
		private String name;
		private String prop;
		private String type;
		private int length;
		private int ruleType;
		private String pattern;
		
		private String refModuleName;
		private String refProp;
		private String showProp;
		private String refType;
		private String showType;
		private String showPath;
		
		private Feature feature;
		private Object defaultValue;
		private List<Select> values = new ArrayList<Select>();
		private Map<String,String> mappings = new LinkedHashMap<String,String>();
	}
	
	@Getter
	@Setter
	public static class DataViewProp {
		private String name;
		private String prop;   // 支持x.y.z格式
		private String type;
		private String pattern;
		private Feature feature;
		private int ruleType;
		private int maxLength = 0;
		
		@JsonIgnore
		private Class<?> typeClass;
		
		@JsonIgnore
		private Encoder encoder;
		
		@JsonIgnore
		private Decoder decoder;
		
		public void setIfMaxLength(int length) {
			if(length > maxLength) {
				this.maxLength = length;
			}
		}
	}
	
	@Getter
	@Setter
	public static class DataAction {
		private String id;
		private String name;
		private String uri;
		private boolean batchable = false;
		private boolean async = true;
		private ActionStatus actionStatus;
		private Act act;
		private List<ActionProp> props = new ArrayList<ActionProp>();
		private String disableProp;
		private List<Object> disableValues = new ArrayList<Object>();
	}
	
}
