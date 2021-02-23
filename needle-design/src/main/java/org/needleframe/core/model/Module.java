package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Table;

import org.needleframe.context.ClassPropBuilder;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.needleframe.core.model.ModuleProp.RuleType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Module implements Cloneable {
	
	private String showName;
	
	private String name;
		
	private String table;
	
	private String pk;
	
	private String uri;
	
	private int actionMask = 0;
	
	// 权限分组字段
	private String securityGroup;
	
	// 审计字段
	private String createdBy;
	private String createdDate;
	private String lastModifiedBy;
	private String lastModifiedDate;
	
	// 删除字段
	private String deletedProp;
	
	// 删除的值
	private Object deletedValue;
	
	/** 是否公开数据，如果是那么数据对当前商户的顶级Group下的所有用户公开，只有当模块指定securityGroup时才有效 */
	private boolean publicData = false;
	
	/** 是否开启任务，指定任务子标题对应的属性名  */
	private String taskProp;
	
	private Map<String,ModuleProp> props = new LinkedHashMap<String,ModuleProp>();
	
	/** 存储格式：key=模块名.关联属性名，value=模块名 */
	private Map<String,Module> children = new LinkedHashMap<String,Module>();
		
	private List<Action> actions = new ArrayList<Action>();
	
	// 可搜索属性
	private List<FilterProp> filterProps = new ArrayList<FilterProp>();
	
	// 唯一字段
	private List<String[]> uniqueProps = new ArrayList<String[]>();
	
	// 列表显示的属性
	private List<String> showListProps = new ArrayList<String>();
	
	// 是否引用自身？比如有parent的外键关联属性
	private boolean refSelf = false;
	
	@JsonIgnore
	private BeanWrapper beanWrapper;
	
	@JsonIgnore
	private ClassPropBuilder builder = new ClassPropBuilder();
	
	@JsonIgnore
	private Sort sort;
	
	private Module() {}
	
	private Module(Class<?> clazz) {
		this.name = getName(clazz);
		this.showName = name;
		this.table = this.name;
		Table table = clazz.getAnnotation(Table.class);
		if(table != null) {
			this.table = table.name();
		}
		this.uri = "/" + this.name;
		this.beanWrapper = new BeanWrapperImpl(clazz);
		this.beanWrapper.setAutoGrowNestedPaths(true);
		this.props = this.builder.buildProps(this);
	}
	
	public static Module def(Class<?> clazz) {
		return new Module(clazz);
	}
	
	public static String getName(Class<?> clazz) {
		return clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
	}
	
	public Class<?> getModelClass() {
		return this.beanWrapper.getWrappedClass();
	}
	
	public boolean hasProp(String prop) {
		return props.containsKey(prop);
	}
	
	/** 查找模块的属性，不存在抛出异常信息 */
	public ModuleProp getProp(String propName) {
		ModuleProp moduleProp = props.get(propName);
		if(moduleProp == null) {
			throw new ServiceException("查找模块[" + this.getName() + "]的属性[" + propName + "]不存在");
		}
		return moduleProp;
	}
	
//	public Module addProp(String prop, Class<?> type) {
//		ModuleProp moduleProp = new ModuleProp();
//		moduleProp.module(this)
//			.name(prop)
//			.type(type)
//			.column(prop)
//			.sortOrder(this.props.size() + 10);
//		this.props.put(prop, moduleProp);
//		return this;
//	}
	
	public void addFilterProp(FilterProp filterProp) {
		this.filterProps.add(filterProp);
	}
	
	public Module addAction(Action action) {
		this.actions.add(action);
		return this;
	}
	
	public Module addAct(Act act) {
		this.actionMask |= act.getMask();
		return this;
	}
	
	public Module addCreate() {
		return addAct(Act.CREATE);
	}
	
	public Module noAct(Act act) {
		int actionMask = getActionMask();
		if((actionMask & Act.CREATE.getMask()) == Act.CREATE.getMask()) {
			this.actionMask ^= Act.CREATE.getMask();
		}
		return this;
	}
	
	public boolean isEnableTask() {
		return StringUtils.hasText(taskProp);
	}
	
	public boolean isCreatable() {
		return (this.actionMask & Act.CREATE.getMask()) == Act.CREATE.getMask();
	}
	
	public boolean isUpdateable() {
		return (this.actionMask & Act.UPDATE.getMask()) == Act.UPDATE.getMask();
	}
	
	public boolean isRemoveable() {
		return (this.actionMask & Act.DELETE.getMask()) == Act.DELETE.getMask();
	}
	
	public boolean isImportable() {
		return (this.actionMask & Act.IMPORT.getMask()) == Act.IMPORT.getMask();
	}
	
	public boolean isExportable() {
		return (this.actionMask & Act.EXPORT.getMask()) == Act.EXPORT.getMask();
	}
	
	public void setSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
		if(StringUtils.hasText(securityGroup)) {
			getProp(securityGroup).setSecurityGroup(true);
		}
	}
	
	public Module clone() {
		Module copy = new Module();
		BeanUtils.copyProperties(this, copy);
		
		Map<String,Module> copyOfChildren = new LinkedHashMap<String,Module>();
		children.forEach((key, module) -> {
			copyOfChildren.put(key, module.clone());
		});
		copy.setChildren(copyOfChildren);
				
		Map<String,ModuleProp> copyOfProps = new LinkedHashMap<String,ModuleProp>();
		props.forEach((key, mp) -> {
			copyOfProps.put(key, mp.clone());
		});
		copy.setProps(copyOfProps);
		
		List<Action> copyOfActions = new ArrayList<Action>();
		actions.forEach(action -> {
			copyOfActions.add(action.clone());
		});
		copy.setActions(copyOfActions);
		
		List<FilterProp> copyOfFilterProps = new ArrayList<FilterProp>();
		filterProps.forEach(fp -> {
			copyOfFilterProps.add(fp.clone());
		});
		copy.setFilterProps(copyOfFilterProps);
		
		List<String[]> copyOfUniqueProps = new ArrayList<String[]>();
		uniqueProps.forEach(props -> {
			String[] copyProps = new String[props.length];
			for(int i = 0; i < props.length; i++) {
				copyProps[i] = props[i];
			}
			copyOfUniqueProps.add(copyProps);
		});
		copy.setUniqueProps(copyOfUniqueProps);
		
		List<String> copyOfListProps = new ArrayList<String>();
		showListProps.forEach(prop -> {
			copyOfListProps.add(prop);
		});
		copy.setShowListProps(copyOfListProps);
		
		return copy;
	}
	
	public List<ViewProp> sortShowList(List<ViewProp> viewProps) {
		if(!this.showListProps.isEmpty()) {
			Map<String,ViewProp> viewPropMap = viewProps.stream()
				.collect(Collectors.toMap(ViewProp::getProp, v -> v, (x,y) ->x, LinkedHashMap::new));
			List<ViewProp> newViewProps = new ArrayList<ViewProp>();
			this.showListProps.forEach(prop -> {
				if(hasProp(prop)) {
					ModuleProp mp = getProp(prop);
					RefModule refModule = mp.getRefModule();
					if(refModule != null) {
						prop = String.join(".", prop, refModule.getRefShowProp());
					}
				}
				if(viewPropMap.containsKey(prop)) {
					ViewProp viewProp = viewPropMap.get(prop);
					int ruleType = viewProp.getRuleType();
					if((RuleType.HIDE_LIST.getMask() & ruleType) == RuleType.HIDE_LIST.getMask()) {
						viewProp.setRuleType(ruleType ^ RuleType.HIDE_LIST.getMask());
					}
					newViewProps.add(viewProp);
					viewPropMap.remove(prop);
				}
			});
			newViewProps.addAll(viewPropMap.values());
			return newViewProps;
		}
		return viewProps;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T fromData(Map<String,Object> data) {
		BeanWrapper beanWrapper = new BeanWrapperImpl(this.getModelClass());
		beanWrapper.setAutoGrowNestedPaths(true);
		this.getProps().values().forEach(mp -> {
			String prop = mp.getProp();
			Object value = data.get(prop);
			if(value != null && StringUtils.hasText(value.toString())) {
				Class<?> type = mp.getType();
				RefModule refModule = mp.getRefModule();
				if(refModule != null) {
					prop += "." + refModule.getRefProp();
					type = refModule.getRefPropType();
				}
				if(!type.isAssignableFrom(value.getClass())) {
					value = org.needleframe.utils.BeanUtils.convert(type, value.toString());
				}
				beanWrapper.setPropertyValue(prop, value);
			}
		});
		return (T) beanWrapper.getWrappedInstance();
	}
	
	public Map<String,Object> toData(Object instance) {
		Class<?> clazz = getModelClass();
		Assert.state(clazz.isAssignableFrom(instance.getClass()), "实例不是模块" + this.name + "的实例类型");
		BeanWrapper beanWrapper = new BeanWrapperImpl(instance);
		beanWrapper.setAutoGrowNestedPaths(true);
		Map<String,Object> data = new LinkedHashMap<String,Object>();
		this.getProps().values().forEach(mp -> {
			Object value = beanWrapper.getPropertyValue(mp.getProp());
			data.put(mp.getProp(), value);
		});
		return data;
	}
}
