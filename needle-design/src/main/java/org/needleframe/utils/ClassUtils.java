package org.needleframe.utils;

import org.needleframe.core.exception.ServiceException;

public class ClassUtils {
	
	public static Class<?> forName(String className) {
		try {
			return org.springframework.util.ClassUtils.forName(className, org.springframework.util.ClassUtils.getDefaultClassLoader());
		} 
		catch (ClassNotFoundException e) {
			throw new ServiceException(e);
		}
		catch(LinkageError e) {
			throw new ServiceException(e);
		}
	}
	
}
