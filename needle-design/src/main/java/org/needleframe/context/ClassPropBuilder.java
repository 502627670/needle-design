package org.needleframe.context;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.Feature;
import org.needleframe.core.model.ModuleProp.RuleType;
import org.needleframe.core.model.ModuleProp.Select;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

public class ClassPropBuilder {
	
	/**
	   *   首先过滤Final属性和static属性；然后取有getter方法的属性；最后只解析普通和级联属性，不解析Map集合属性；
	 * @param module
	 * @return
	 */
	public Map<String,ModuleProp> buildProps(Module module) {
		Map<String,ModuleProp> propsMap = new LinkedHashMap<String,ModuleProp>();
		BeanWrapper beanWrapper = module.getBeanWrapper();
		Class<?> clazz = beanWrapper.getWrappedClass();
		ReflectionUtils.doWithFields(clazz, new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if(!(Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))) {
					if(beanWrapper.isReadableProperty(field.getName())) {
						Class<?> propertyType = field.getType();
//							if(!Map.class.isAssignableFrom(propertyType) && !Collection.class.isAssignableFrom(propertyType)) {
						if(!Map.class.isAssignableFrom(propertyType)) {
							propertyType = ClassUtils.resolvePrimitiveIfNecessary(propertyType);
							if(propertyType.equals(Serializable.class)) {
								propertyType = beanWrapper.getPropertyType(field.getName());
							}
							
							ModuleProp moduleProp = propsMap.computeIfAbsent(field.getName(), k -> new ModuleProp());
							moduleProp.module(module);
							moduleProp.name(field.getName());
							moduleProp.prop(field.getName());
							moduleProp.type(propertyType);
							moduleProp.column(moduleProp.getProp());
							moduleProp.sortOrder(propsMap.size() + 1);
							moduleProp.defaultValue(beanWrapper.getPropertyValue(field.getName()));
							
							if(Boolean.class.equals(propertyType)) {
								List<Select> values = new ArrayList<Select>();
								values.add(new Select("true"));
								values.add(new Select("false"));
								moduleProp.setValues(values);
								moduleProp.feature(Feature.SELECT);
							}
							
							moduleProp.transientProp(false);
							Transient transientProp = field.getAnnotation(Transient.class);
							if(transientProp != null) {
								moduleProp.transientProp(true);
								moduleProp.addRuleType(RuleType.TRANSIENT);
							}
							
							if(Enum.class.isAssignableFrom(propertyType)) {
								@SuppressWarnings("unchecked")
								Class<Enum<?>> clazz = (Class<Enum<?>>) propertyType;
								Enum<?>[] enumConstants = clazz.getEnumConstants();
								List<Select> values = new ArrayList<Select>();
								for(int i = 0; i < enumConstants.length; i++) {
									values.add(new Select(enumConstants[i].toString()));
								}
								moduleProp.values(values);
							}
							
							CreatedBy createdBy = field.getAnnotation(CreatedBy.class);
							if(createdBy != null) {
								module.setCreatedBy(moduleProp.getProp());
								moduleProp.addRuleType(RuleType.AUDIT_CREATED_BY)
									.addRuleType(RuleType.HIDE_LIST)
									.addRuleType(RuleType.HIDE_FORM)
									.addRuleType(RuleType.HIDE_EDIT);
							}
							CreatedDate createdDate = field.getAnnotation(CreatedDate.class);
							if(createdDate != null) {
								moduleProp.feature(Feature.DATETIME);
								module.setCreatedDate(moduleProp.getProp());
								moduleProp.ruleType(RuleType.AUDIT_CREATED_DATE);
								moduleProp.addRuleType(RuleType.AUDIT_CREATED_BY)
									.addRuleType(RuleType.HIDE_LIST)
									.addRuleType(RuleType.HIDE_FORM)
									.addRuleType(RuleType.HIDE_EDIT);
							}
							LastModifiedBy lastModifiedBy = field.getAnnotation(LastModifiedBy.class);
							if(lastModifiedBy != null) {
								module.setLastModifiedBy(moduleProp.getProp());
								moduleProp.ruleType(RuleType.AUDIT_LAST_MODIFIED_BY);
								moduleProp.addRuleType(RuleType.AUDIT_CREATED_BY)
									.addRuleType(RuleType.HIDE_LIST)
									.addRuleType(RuleType.HIDE_FORM)
									.addRuleType(RuleType.HIDE_EDIT);
							}
							LastModifiedDate lastModifiedDate = field.getAnnotation(LastModifiedDate.class);
							if(lastModifiedDate != null) {
								moduleProp.feature(Feature.DATETIME);
								module.setLastModifiedDate(moduleProp.getProp());
								moduleProp.ruleType(RuleType.AUDIT_LAST_MODIFIED_DATE);
								moduleProp.addRuleType(RuleType.AUDIT_CREATED_BY)
									.addRuleType(RuleType.HIDE_LIST)
									.addRuleType(RuleType.HIDE_FORM)
									.addRuleType(RuleType.HIDE_EDIT);
							}
							
							Id id = field.getAnnotation(Id.class);
							if(id != null) {
								module.setPk(field.getName());
								moduleProp.ruleType(RuleType.ID);
								moduleProp.addRuleType(RuleType.HIDE_LIST)
									.addRuleType(RuleType.HIDE_FORM)
									.addRuleType(RuleType.HIDE_EDIT)
									.addRuleType(RuleType.HIDE_INFO);
							}
							
							Column column = field.getAnnotation(Column.class);
							if(column != null) {
								moduleProp.column(column.name());
								moduleProp.length(column.length());
								if(column.length() >= 500) {
									moduleProp.feature(Feature.TEXT);
									moduleProp.addRuleType(RuleType.HIDE_LIST);
								}
							}
							JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
							if(joinColumn != null) {
								moduleProp.column(joinColumn.name());
							}
							
							Transient t = field.getAnnotation(Transient.class);
							if(t != null) {
								moduleProp.transientProp(true);
							}
							
							guessFeature(moduleProp);
						}
					}
				}
			}
		});
		return propsMap;
	}
	
	private void guessFeature(ModuleProp moduleProp) {
		Class<?> type = moduleProp.getType();
		if(Number.class.isAssignableFrom(type)) {
			moduleProp.feature(Feature.NUMBER);
		}
		else if(type.equals(Timestamp.class)) {
			moduleProp.feature(Feature.DATETIME);
		}
		else if(Time.class.equals(type)) {
			moduleProp.feature(Feature.TIME);
		}
		else if(Date.class.isAssignableFrom(type)) {
			moduleProp.feature(Feature.DATE);
		}
		else if(Enum.class.isAssignableFrom(type)) {
			moduleProp.feature(Feature.SELECT);
		}
		else if(!BeanUtils.isSimpleProperty(type)) {
			moduleProp.feature(Feature.FK);
		}
		else if(moduleProp.getProp().equals("password")) {
			moduleProp.feature(Feature.PASSWORD);
		}
	}
	
}
