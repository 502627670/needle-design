package org.needleframe.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.StringUtils;

public class ModelUtils {
	
	static Logger logger = LoggerFactory.getLogger(ModelUtils.class);
	
	public static Long getLong(Map<String,Object> data, String prop) {
		Object obj = data.get(prop);
		if(obj == null) {
			return 0L;
		}
		return Long.valueOf(obj.toString().trim());
	}
	
	public static String getString(Map<String,Object> data, String prop) {
		Object obj = data.get(prop);
		return obj == null ? "" : obj.toString();
	}
	
	public static double getDouble(Map<String,Object> data, String prop) {
		Object obj = data.get(prop);
		if(obj == null) {
			return 0.0;
		}
		return Double.valueOf(obj.toString().trim());
	}
	
	public static Double getDouble(String value) {
		return getDouble(value, 0.0);
	}
	
	public static Double getDouble(String value, double defaultValue) {
		try {
			if(StringUtils.hasText(value)) {
				return new Double(value);
			}
		}
		catch(Exception e) {
			logger.error("getDouble(..) => 转换数据失败：值={}，转换为Double，使用默认值{}替代。异常消息：{}", 
				value, defaultValue, e.getMessage());
		}
		return defaultValue;
	}
	
	public static BigDecimal getBigDecimal(String value) {
		return getBigDecimal(value, BigDecimal.ZERO);
	}
	
	public static BigDecimal getBigDecimal(String value, BigDecimal defaultValue) {
		try {
			if(StringUtils.hasText(value)) {
				return new BigDecimal(value);
			}
		}
		catch(Exception e) {
			logger.error("getBigDecimal(..) => 转换数据失败：值={}，转换为BigDecimal，使用默认值{}替代。异常消息：{}", 
				value, defaultValue, e.getMessage());
		}
		return defaultValue;
	}
	
	public static <T> Page<T> clone(Page<T> page, Pageable pageable, int fetchSize, String... excludeField) {
		List<T> targetList = new ArrayList<T>();
		for(T source : page.getContent()) {
			targetList.add(ModelUtils.clone(source, fetchSize, excludeField));
		}
		return new PageImpl<T>(targetList, pageable, page.getTotalElements());
	}
	
	public static <T> List<T> clone(List<T> entities, String... excludeField) {
		return clone(entities, 1, excludeField);
	}
	
	public static <T> List<T> clone(List<T> entities, int fetchSize, String... excludeField) {
		List<T> targetList = new ArrayList<T>();
		for(T source : entities) {
			targetList.add(ModelUtils.clone(source, fetchSize, excludeField));
		}
		return targetList;
	}
	
	public static <T> Map<String,T> clone(Map<String,T> entities, String... excludeField) {
		return clone(entities, 1, excludeField);
	}
	
	public static <T> Map<String,T> clone(Map<String,T> entities, int fetchSize, String... excludeField) {
		Map<String,T> cloneMap = new ConcurrentHashMap<String,T>();
		for(String key : entities.keySet()) {
			T entity = entities.get(key);
			cloneMap.put(key, ModelUtils.clone(entity, fetchSize, excludeField));
		}
		return cloneMap;
	}
	
	public static <T> T clone(T source, String... excludeField) {
		return clone(source, 1, excludeField);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T clone(T source, int fetchDeepSize, String... excludeField) {
		if(fetchDeepSize <= 0 || source == null) {
			return null;
		}
		List<String> excludeFields = excludeField == null ? new ArrayList<String>() : Arrays.asList(excludeField);
		BeanWrapper sourceBeanWrapper = new BeanWrapperImpl(source);
		Class<?> sourceClass = ClassUtils.getUserClass(source.getClass());
		sourceBeanWrapper.setAutoGrowNestedPaths(true);
		sourceBeanWrapper.setAutoGrowCollectionLimit(3);
		
		BeanWrapper targetBeanWrapper = new BeanWrapperImpl(sourceClass);
		targetBeanWrapper.setAutoGrowNestedPaths(true);
		ReflectionUtils.doWithFields(sourceClass, new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				int modifiers = field.getModifiers();
				String fieldName = field.getName();
				if(!excludeFields.contains(fieldName)) {
					if(!(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))) {
						if(sourceBeanWrapper.isReadableProperty(field.getName())) {
							Object value = sourceBeanWrapper.getPropertyValue(field.getName());
							if(value instanceof Optional) {
								value = ((Optional<?>) value).orElse(null);
							}
							if(value != null) {
								Class<?> valueClass = ClassUtils.getUserClass(value.getClass());
								if(BeanUtils.isSimpleProperty(valueClass)) {
									if(targetBeanWrapper.isWritableProperty(field.getName())) {
										targetBeanWrapper.setPropertyValue(field.getName(), value);
									}
									else {
										Object instance = targetBeanWrapper.getWrappedInstance();
										org.needleframe.utils.BeanUtils.setField(fieldName, instance, value);
									}
								}
								else if(TemporalAccessor.class.isAssignableFrom(valueClass)) {
									if(targetBeanWrapper.isWritableProperty(field.getName())) {
										targetBeanWrapper.setPropertyValue(field.getName(), value);
									}
									else {
										Object instance = targetBeanWrapper.getWrappedInstance();
										org.needleframe.utils.BeanUtils.setField(fieldName, instance, value);
									}
								}
								else if(!Collection.class.isAssignableFrom(valueClass) && !Map.class.isAssignableFrom(valueClass)) {
									Object entity = value;
//									if(fetchSize - 1 == 0) {
//										String propertyPath = String.join(".", field.getName(), "id");
//										if(sourceBeanWrapper.isWritableProperty(propertyPath)) {
//											targetBeanWrapper.setPropertyValue(propertyPath, sourceBeanWrapper.getPropertyValue(propertyPath));
//										}
//									}
									if(fetchDeepSize > 1) {
										Object cloneEntity = ModelUtils.clone(entity, fetchDeepSize - 1);
										if(targetBeanWrapper.isWritableProperty(field.getName())) {
											targetBeanWrapper.setPropertyValue(field.getName(), cloneEntity);
										}
										else {
											Object instance = targetBeanWrapper.getWrappedInstance();
											org.needleframe.utils.BeanUtils.setField(fieldName, instance, cloneEntity);
										}
									}
								}
							}
						}
					}
				}
			}
		});
		return (T) targetBeanWrapper.getWrappedInstance();
		
	}
	
//	public static Map<String,Object> toMap(Object entity) {
//		Assert.notNull(entity, "entity is required");
//		Map<String, Object> map = new LinkedHashMap<String, Object>();
//		try {
//			BeanInfo beanInfo = Introspector.getBeanInfo(entity.getClass());
//			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
//		    for (PropertyDescriptor property : propertyDescriptors) {
//		        String key = property.getName();
//		        if(!key.equals("class")) {
//		            Method getter = property.getReadMethod();
//		            Object value = getter.invoke(entity);
//		            map.put(key, value);
//		        }
//		    }
//		} 
//		catch (IntrospectionException e) {
//			e.printStackTrace();
//		} 
//		catch (IllegalAccessException e) {
//			e.printStackTrace();
//		}
//		catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} 
//		catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//	    return map;
//	}
}
