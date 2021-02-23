package org.needleframe.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewProp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServiceDispatcherImpl implements ServiceDispatcher {
	
	@Autowired
	private DefaultDataService defaultDataService;
	
	private Map<Class<?>, DataService> services = new ConcurrentHashMap<Class<?>, DataService>();
	
	public void registerService(Class<?> clazz, DataService dataService) {
		services.put(clazz, dataService);
	}
	
	private DataService resolveDataService(Module module) {
		Class<?> clazz = module.getModelClass();
		DataService dataService = services.get(clazz);
		if(dataService == null) {
			dataService = defaultDataService;
		}
		return dataService;
	}
	
	public Map<String, Object> get(Module module, List<ViewProp> viewProps, Serializable id) {
		return resolveDataService(module).get(module, viewProps, id);
	}

	public Page<Map<String, Object>> findPage(Module module, Condition query, Pageable pageable) {
		return resolveDataService(module).findPage(module, query, pageable);
	}

	public List<Map<String, Object>> findList(Module module, Condition query, Pageable pageable) {
		return resolveDataService(module).findList(module, query, pageable);
	}

	public List<ActionData> create(Module module, List<ActionData> dataList) {
		return resolveDataService(module).create(module, dataList);
	}

	public List<Map<String, Object>> update(Module module, List<Map<String, Object>> dataList) {
		return resolveDataService(module).update(module, dataList);
	}

	public void delete(Module module, Serializable[] ids) {
		resolveDataService(module).delete(module, ids);
	}
	
}
