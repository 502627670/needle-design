package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleProp implements Cloneable {
	public static enum Feature {
		STRING,
		PASSWORD,
		TEXT,
		NUMBER,
		DATE,
		DATETIME,
		TIME,
		SELECT,
		FK,
		FILE,
		IMAGE,
		EDITOR,
	}
	
	public static enum RuleType {
		ID(1 << 0),
		REQUIRED(1 << 1),
		NO_CREATE(1 << 2),
		NO_UPDATE(1 << 3),
		HIDE_LIST(1 << 4),
		HIDE_FORM(1 << 5),
		HIDE_EDIT(1 << 6),
		HIDE_INFO(1 << 7),
		TRANSIENT(1 << 8),
		SHOW_IN_ROW(1 << 9),
		ABSOLUTE_FILE(1 << 10),         // 文件保存为绝对路径，默认是相对路径
		AUDIT_CREATED_BY(1 << 12),
		AUDIT_CREATED_DATE(1 << 13),
		AUDIT_LAST_MODIFIED_BY(1 << 14),
		AUDIT_LAST_MODIFIED_DATE(1 << 15),
		SECURITY_GROUP(1 << 16),
		SYSTEM_FIELD(1 << 17);
		
		private int mask;
		private RuleType(int mask) {
			this.mask = mask;
		}
		
		public int getMask() {
			return mask;
		}
		
		public boolean match(int mask) {
			if((this.mask & mask) == this.mask) {
				return true;
			}
			return false;
		}
	}
	
	private Module module;
	
	private String name;
	
	private String prop;
	
	private Class<?> type;
	
	private String column;
	
	private Integer length = 255;
	
	private String pattern;
	
	private Integer sortOrder = 0;
	
	private Object defaultValue;
	
	// 规则类型
	private int ruleType = 0;
	
	private boolean transientProp = false;
	
	// 是否权限分组字段
	private boolean securityGroup;
	
	// 字段特征
	private Feature feature = Feature.STRING;
	
	private List<Select> values = new ArrayList<Select>();
	
	// 如果是外键字段，那么选择外键实例后，会同步更新本实例的某些字段，则建立外键实例与本实例字段的映射关系
	private Map<String,String> mappings = new LinkedHashMap<String,String>();
	
	private RefModule refModule;
	
	// 值转换
	private Encoder encoder;
	
	// 值解析
	private Decoder decoder;
	
	public ModuleProp module(Module module) {
		this.module = module;
		return this;
	}
	
	public ModuleProp name(String name) {
		this.name = name;
		return this;
	}
	
	public ModuleProp prop(String prop) {
		this.prop = prop;
		return this;
	}
	
	public ModuleProp type(Class<?> type) {
		this.type = type;
		return this;
	}
	
	public ModuleProp column(String column) {
		this.column = column;
		return this;
	}
	
	public ModuleProp length(int length) {
		this.length = length;
		return this;
	}
	
	public ModuleProp pattern(String pattern) {
		this.pattern = pattern;
		return this;
	}
	
	public ModuleProp sortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}
	
	public ModuleProp defaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public ModuleProp transientProp(boolean transientProp) {
		this.transientProp = transientProp;
		return this;
	}
	
	public ModuleProp ruleType(RuleType ruleType) {
		this.ruleType = ruleType.getMask();
		return this;
	}
	
	public ModuleProp addRuleType(RuleType ruleType) {
		this.ruleType = this.ruleType | ruleType.getMask();
		return this;
	}
	
	public ModuleProp securityGroup(boolean securityGroup) {
		this.securityGroup = securityGroup;
		return this;
	}
	
	public ModuleProp feature(Feature feature) {
		this.feature = feature;
		return this;
	}
	
	public ModuleProp values(List<Select> values) {
		this.values = values;
		return this;
	}
	
	public ModuleProp required() {
		this.addRuleType(RuleType.REQUIRED);
		return this;
	}
	
	public Object getValue(Map<String,Object> data) {
		Object value = data.get(prop);
		if(value == null && this.refModule != null) {
			value = data.get(String.join(".", prop, refModule.getRefProp()));
		}
		return value;
	}
	
	public boolean isAuditProp() {
		if(this.ruleType == 0) {
			return false;
		}
		return RuleType.AUDIT_CREATED_BY.match(this.ruleType) || 
			RuleType.AUDIT_CREATED_DATE.match(this.ruleType) || 
			RuleType.AUDIT_LAST_MODIFIED_BY.match(this.ruleType) ||
			RuleType.AUDIT_LAST_MODIFIED_DATE.match(this.ruleType);
	}
	
	public boolean isSystemProp() {
		if(this.ruleType == 0) {
			return false;
		}
		return RuleType.SYSTEM_FIELD.match(this.ruleType);
	}
	
	public boolean isRefProp() {
		return !BeanUtils.isSimpleProperty(type);
	}
	
	public boolean isRefParent() {
		return this.isRefProp() && 
				!Collection.class.isAssignableFrom(type) &&
				!Map.class.isAssignableFrom(type);
	}
	
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(this.type) || 
				Map.class.isAssignableFrom(this.type);
	}
	
	public boolean isNumber() {
		return Number.class.isAssignableFrom(this.type);
	}
	
	public boolean isPk() {
		return this.prop.equals(module.getPk());
	}
	
	public boolean isRefSelf() {
		if(this.refModule == null) {
			return false;
		}
		return this.refModule.isRefSelf();
	}
	
	public boolean equals(ModuleProp other) {
		return this.getModule().getName().equals(other.getModule().getName()) && 
			this.getProp().equals(other.getProp());
	}
	
	public ModuleProp clone() {
		ModuleProp copy = new ModuleProp();
		BeanUtils.copyProperties(this, copy);
		
		List<Select> copyValues = new ArrayList<Select>();
		values.forEach(value -> {
			copyValues.add(value);
		});
		copy.setValues(copyValues);
		
		if(this.refModule != null) {
			RefModule copyOfRefModule = new RefModule();
			BeanUtils.copyProperties(this.refModule, copyOfRefModule);
			copy.setRefModule(copyOfRefModule);
		}
		return copy;
	}
	
	@Getter
	@Setter
	public static class RefModule {
		// User.userRoles => {moduleName:user,prop:id,refModuleName:userRoles,refPk:id,refProp:user_id,...}
		// User.group     => {moduleName:user,prop:group_id,refModuleName:group,refPk:id,refProp:id,...}
		// => module.prop = refModule.refProp
		String moduleName;
		String prop;
		String refModuleName;
		String refProp;
		String refShowProp;
		Class<?> refPropType;
		Class<?> refShowType;
		boolean cascadeDel = false;
		
		public boolean isSimpleProp() {
			return this.refModuleName == null;
		}
		
		public boolean isRefSelf() {
			return this.moduleName.equals(this.refModuleName);
		}
	}
	
	@Getter
	@Setter
	public static class Select {
		String label;
		String value;
		public Select(String value) {
			this(value, value);
		}
		
		public Select(String label, String value) {
			this.label = label;
			this.value = value;
		}
	}
	
	@FunctionalInterface
	public static interface Encoder {
		public Object encode(Object value);
	}
	
	@FunctionalInterface
	public static interface Decoder {
		public Object decode(Object value);
	}
}
