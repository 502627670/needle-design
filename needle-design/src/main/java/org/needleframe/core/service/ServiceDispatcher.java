package org.needleframe.core.service;

public interface ServiceDispatcher extends DataService {
	
	public void registerService(Class<?> clazz, DataService dataService);
	
}
