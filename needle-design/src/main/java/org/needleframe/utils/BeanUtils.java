package org.needleframe.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.needleframe.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class BeanUtils {
	
	static Logger logger = LoggerFactory.getLogger(BeanUtils.class);
	
	public static void setField(String fieldName, Object target, Object value) {
		String methodName = new StringBuilder()
			.append("set")
			.append(fieldName.substring(0, 1).toUpperCase())
			.append(fieldName.substring(1)).toString();
		
		Method method = ReflectionUtils.findMethod(target.getClass(), methodName);
		if(method == null) {
			Field field = ReflectionUtils.findField(target.getClass(), fieldName);
			if(field != null) {
				try {
					field.setAccessible(true);
					ReflectionUtils.setField(field, target, value);
				}
				catch(Exception e) {
					logger.warn("setField(..) => set Field [{}] failed for class[{}]", 
						methodName, target.getClass());
				}
			}
		}
		else {
			try {
				method.invoke(target, value);
			} 
			catch (Exception e) {
				logger.warn("setField(..) => call method [{}] failed for class[{}]", 
					methodName, target.getClass());
			}
		}
	}
	
	public static boolean isCollection(Class<?> clazz) {
		if(clazz == null) {
			return false;
		}
		if(Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convert(Class<?> clazz, String value) {
		Assert.notNull(value, MessageFormat.format("value({0})不能为NULL", value));
		Object result = value;  
		if(ClassUtils.isPrimitiveOrWrapper(clazz)) {
			if(!StringUtils.hasText(value)) {
				return null;
			}
			clazz = ClassUtils.resolvePrimitiveIfNecessary(clazz);
			if(Character.class.isAssignableFrom(clazz)) {
				result = value.charAt(0);
			}
			else {
				Constructor<?> constructor;
				try {
					constructor = clazz.getConstructor(String.class);
					result = org.springframework.beans.BeanUtils.instantiateClass(constructor, value);
				} 
				catch (NoSuchMethodException e) {
					logger.info("convert(..) => 参数{}转换为{}类型失败", value, clazz.getName());
					throw new ServiceException("参数{0}转换为{1}类型失败", value, clazz.getSimpleName()) ;
				} 
				catch (SecurityException e) {
					logger.info("convert(..) => 参数{}转换为{}类型失败", value, clazz.getName());
					throw new ServiceException("参数{0}转换为{1}类型失败", value, clazz.getSimpleName()) ;
				}
			}
		}
		else if(BigDecimal.class.isAssignableFrom(clazz)) {
			result = new BigDecimal(value);
		}
		else if(clazz.isEnum()) {
			Class<Enum> c = (Class<Enum>) clazz;
			result = Enum.valueOf(c, value);
		}
		else if(Time.class.isAssignableFrom(clazz)) {
			result = Time.valueOf(value);
		}
		else if(Timestamp.class.isAssignableFrom(clazz)) {
			result = new Timestamp(DateUtils.parse(value, Locale.getDefault()).getTime());
		}
		else if(Date.class.isAssignableFrom(clazz)) {
			Object dateValue = null;
			if(value.length() >= 19) {
				try {
					value = value.substring(0, 19);
					value = value.replace("T", " ");
					dateValue = DateUtils.parseDate(value, "yyyy-MM-dd HH:mm:ss");
				}
				catch(Exception e) {
					dateValue = DateUtils.parse(value, Locale.getDefault());
				}
			}
			if(dateValue == null) {
				dateValue = DateUtils.parseDate(value, "yyyy-MM-dd");
			}
			result = dateValue;
		}
		else if(AbstractPersistable.class.isAssignableFrom(clazz)) {
			AbstractPersistable entity = null;
			try {
				entity = (AbstractPersistable) clazz.newInstance();
				Field field = ReflectionUtils.findField(clazz, "id");
				field.setAccessible(true);
				ReflectionUtils.setField(field, entity, Long.valueOf(value));
			} 
			catch (InstantiationException e) {
				throw new ServiceException("参数{0}转换为{1}类型失败", value, clazz.getSimpleName()) ;
			} 
			catch (IllegalAccessException e) {
				throw new ServiceException("参数{0}转换为{1}类型失败", value, clazz.getSimpleName()) ;
			}
			return entity;
		}
		return result;
	}
	
	public static Class<?> forName(String className) {
		try {
			return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
		} 
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		catch(LinkageError e) {
			throw new RuntimeException(e);
		}
	}
	
}
